/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.xml;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.log4j.chainsaw.logevents.LocationInfo;
import org.apache.log4j.spi.Decoder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Decodes JDK 1.4's java.util.logging package events
 * delivered via XML (using the logger.dtd).
 *
 * @author Scott Deboy (sdeboy@apache.org)
 * @author Paul Smith (psmith@apache.org)
 */
public class UtilLoggingXMLDecoder implements Decoder {
    // NOTE: xml section is only handed on first delivery of events
    // on this first delivery of events, there is no end tag for the log element
    /**
     * Document prolog.
     */
    private static final String BEGIN_PART = "<log>";
    /**
     * Document close.
     */
    private static final String END_PART = "</log>";
    /**
     * Document builder.
     */
    private DocumentBuilder docBuilder;
    /**
     * Additional properties.
     */
    private Map additionalProperties = new HashMap();
    /**
     * Partial event.
     */
    private String partialEvent;
    /**
     * Record end.
     */
    private static final String RECORD_END = "</record>";
    /**
     * Owner.
     */
    private Component owner = null;

    private ChainsawLoggingEventBuilder builder = new ChainsawLoggingEventBuilder();

    private static final String ENCODING = "UTF-8";

    /**
     * Create new instance.
     */
    public UtilLoggingXMLDecoder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            docBuilder = dbf.newDocumentBuilder();
            docBuilder.setEntityResolver(new UtilLoggingEntityResolver());
        } catch (ParserConfigurationException pce) {
            System.err.println("Unable to get document builder");
        }
    }

    /**
     * Sets an additionalProperty map, where each Key/Value pair is
     * automatically added to each LoggingEvent as it is decoded.
     * <p>
     * This is useful, say, to include the source file name of the Logging events
     *
     * @param properties additional properties
     */
    public void setAdditionalProperties(final Map properties) {
        this.additionalProperties = properties;
    }

    /**
     * Converts the LoggingEvent data in XML string format into an actual
     * XML Document class instance.
     *
     * @param data XML fragment
     * @return dom document
     */
    @SuppressFBWarnings // applied security practices
    private Document parse(final String data) {
        if (docBuilder == null || data == null) {
            return null;
        }

        Document document = null;

        try {
            // we change the system ID to a valid URI so that Crimson won't
            // complain. Indeed, "log4j.dtd" alone is not a valid URI which
            // causes Crimson to barf. The Log4jEntityResolver only cares
            // about the "log4j.dtd" ending.

            /**
             * resetting the length of the StringBuffer is dangerous, particularly
             * on some JDK 1.4 impls, there's a known Bug that causes a memory leak
             */
            StringBuilder buf = new StringBuilder(1024);

            if (!data.startsWith("<?xml")) {
                buf.append(BEGIN_PART);
            }

            buf.append(data);

            if (!data.endsWith(END_PART)) {
                buf.append(END_PART);
            }

            InputSource inputSource = new InputSource(new StringReader(buf.toString()));
            document = docBuilder.parse(inputSource);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return document;
    }

    /**
     * Decodes a File into a Vector of LoggingEvents.
     *
     * @param url the url of a file containing events to decode
     * @return Vector of LoggingEvents
     * @throws IOException if IO error during processing.
     */
    @SuppressFBWarnings // TODO: loading files like this is dangerous - at least in web. see if we can do better
    public Vector<ChainsawLoggingEvent> decode(final URL url) throws IOException {
        LineNumberReader reader;
        boolean isZipFile = url.getPath().toLowerCase().endsWith(".zip");
        InputStream inputStream;
        if (isZipFile) {
            inputStream = new ZipInputStream(url.openStream());
            // move stream to next entry so we can read it
            ((ZipInputStream) inputStream).getNextEntry();
        } else {
            inputStream = url.openStream();
        }
        if (owner != null) {
            reader = new LineNumberReader(new InputStreamReader(
                    new ProgressMonitorInputStream(owner, "Loading " + url, inputStream), ENCODING));
        } else {
            reader = new LineNumberReader(new InputStreamReader(inputStream, ENCODING));
        }
        Vector<ChainsawLoggingEvent> v = new Vector<>();

        String line;
        Vector<ChainsawLoggingEvent> events;
        try {
            while ((line = reader.readLine()) != null) {
                StringBuilder buffer = new StringBuilder(line);
                for (int i = 0; i < 1000; i++) {
                    buffer.append(reader.readLine()).append("\n");
                }
                events = decodeEvents(buffer.toString());
                if (events != null) {
                    v.addAll(events);
                }
            }
        } finally {
            partialEvent = null;
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return v;
    }

    /**
     * Decodes a String representing a number of events into a
     * Vector of LoggingEvents.
     *
     * @param document to decode events from
     * @return Vector of LoggingEvents
     */
    public Vector<ChainsawLoggingEvent> decodeEvents(final String document) {

        if (document != null) {

            if (document.trim().isEmpty()) {
                return null;
            }

            String newDoc;
            String newPartialEvent = null;
            // separate the string into the last portion ending with </record>
            // (which will be processed) and the partial event which
            // will be combined and processed in the next section

            // if the document does not contain a record end,
            // append it to the partial event string
            if (document.lastIndexOf(RECORD_END) == -1) {
                partialEvent = partialEvent + document;
                return null;
            }

            if (document.lastIndexOf(RECORD_END) + RECORD_END.length() < document.length()) {
                newDoc = document.substring(0, document.lastIndexOf(RECORD_END) + RECORD_END.length());
                newPartialEvent = document.substring(document.lastIndexOf(RECORD_END) + RECORD_END.length());
            } else {
                newDoc = document;
            }
            if (partialEvent != null) {
                newDoc = partialEvent + newDoc;
            }
            partialEvent = newPartialEvent;

            Document doc = parse(newDoc);
            if (doc == null) {
                return null;
            }
            return decodeEvents(doc);
        }
        return null;
    }

    /**
     * Converts the string data into an XML Document, and then soaks out the
     * relevant bits to form a new LoggingEvent instance which can be used
     * by any Log4j element locally.
     *
     * @param data XML fragment
     * @return a single LoggingEvent or null
     */
    public ChainsawLoggingEvent decode(final String data) {
        Document document = parse(data);

        if (document == null) {
            return null;
        }

        Vector<ChainsawLoggingEvent> events = decodeEvents(document);

        if (events.size() > 0) {
            return events.firstElement();
        }

        return null;
    }

    /**
     * Given a Document, converts the XML into a Vector of LoggingEvents.
     *
     * @param document XML document
     * @return Vector of LoggingEvents
     */
    private Vector<ChainsawLoggingEvent> decodeEvents(final Document document) {
        Vector<ChainsawLoggingEvent> events = new Vector<>();

        NodeList eventList = document.getElementsByTagName("record");

        for (int eventIndex = 0; eventIndex < eventList.getLength(); eventIndex++) {
            Node eventNode = eventList.item(eventIndex);

            String logger = null;
            long timeStamp = 0L;
            String level = null;
            String threadName = null;
            String message = null;
            String ndc = null;
            String className = null;
            String methodName = null;
            String fileName = null;
            String lineNumber = "0"; // TODO this is not working
            Hashtable properties = new Hashtable();

            // format of date: 2003-05-04T11:04:52
            // ignore date or set as a property? using millis in constructor instead
            NodeList list = eventNode.getChildNodes();
            int listLength = list.getLength();

            if (listLength == 0) {
                continue;
            }

            for (int y = 0; y < listLength; y++) {
                String tagName = list.item(y).getNodeName();

                if (tagName.equalsIgnoreCase("logger")) {
                    logger = getCData(list.item(y));
                }

                if (tagName.equalsIgnoreCase("millis")) {
                    timeStamp = Long.parseLong(getCData(list.item(y)));
                }

                if (tagName.equalsIgnoreCase("level")) {
                    level = getCData(list.item(y));
                }

                if (tagName.equalsIgnoreCase("thread")) {
                    threadName = getCData(list.item(y));
                }

                if (tagName.equalsIgnoreCase("sequence")) {
                    properties.put("log4jid", getCData(list.item(y)));
                }

                if (tagName.equalsIgnoreCase("message")) {
                    message = getCData(list.item(y));
                }

                if (tagName.equalsIgnoreCase("class")) {
                    className = getCData(list.item(y));
                }

                if (tagName.equalsIgnoreCase("method")) {
                    methodName = getCData(list.item(y));
                }

                if (tagName.equalsIgnoreCase("exception")) {
                    ArrayList<String> exceptionList = new ArrayList<>();
                    NodeList exList = list.item(y).getChildNodes();
                    int exlistLength = exList.getLength();

                    for (int i2 = 0; i2 < exlistLength; i2++) {
                        Node exNode = exList.item(i2);
                        String exName = exList.item(i2).getNodeName();

                        if (exName.equalsIgnoreCase("message")) {
                            exceptionList.add(getCData(exList.item(i2)));
                        }

                        if (exName.equalsIgnoreCase("frame")) {
                            NodeList exList2 = exNode.getChildNodes();
                            int exlist2Length = exList2.getLength();

                            for (int i3 = 0; i3 < exlist2Length; i3++) {
                                exceptionList.add(getCData(exList2.item(i3)) + "\n");
                            }
                        }
                    }
                }
            }

            /**
             * We add all the additional properties to the properties
             * hashtable. Override properties that already exist
             */
            if (additionalProperties.size() > 0) {
                if (properties == null) {
                    properties = new Hashtable(additionalProperties);
                }
                for (Object o : additionalProperties.entrySet()) {
                    Map.Entry e = (Map.Entry) o;
                    properties.put(e.getKey(), e.getValue());
                }
            }

            LocationInfo info;
            if ((fileName != null) || (className != null) || (methodName != null) || (lineNumber != null)) {
                info = new LocationInfo(fileName, className, methodName, Integer.parseInt(lineNumber));
            } else {
                info = null;
            }

            //            ThrowableInformation throwableInfo = null;
            //            if (exception != null) {
            //                throwableInfo = new ThrowableInformation(exception);
            //            }

            builder.clear();
            builder.setLogger(logger)
                    .setTimestamp(Instant.ofEpochMilli(timeStamp))
                    .setLevelFromString(level)
                    .setMessage(message)
                    .setThreadName(threadName)
                    .setMDC(properties)
                    .setNDC(ndc)
                    .setLocationInfo(info);

            events.add(builder.create());
        }
        return events;
    }

    /**
     * Get contents of CDATASection.
     *
     * @param n CDATASection
     * @return text content of all text or CDATA children of node.
     */
    private String getCData(final Node n) {
        StringBuilder buf = new StringBuilder();
        NodeList nl = n.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node innerNode = nl.item(x);

            if ((innerNode.getNodeType() == Node.TEXT_NODE) || (innerNode.getNodeType() == Node.CDATA_SECTION_NODE)) {
                buf.append(innerNode.getNodeValue());
            }
        }

        return buf.toString();
    }
}
