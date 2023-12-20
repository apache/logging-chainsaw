/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.log4j.chainsaw;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LogFilePatternLayoutBuilder {

    public static String getTimeStampFormat(String patternLayout) {
        int basicIndex = patternLayout.indexOf("%d");
        if (basicIndex < 0) {
            return null;
        }

        int index = patternLayout.indexOf("%d{");
        //%d - default
        if (index < 0) {
            return "yyyy-MM-dd HH:mm:ss,SSS";
        }

        int length = patternLayout.substring(index).indexOf("}");
        String timestampFormat = patternLayout.substring(index + "%d{".length(), index + length);
        if (timestampFormat.equals("ABSOLUTE")) {
            return "HH:mm:ss,SSS";
        }
        if (timestampFormat.equals("ISO8601")) {
            return "yyyy-MM-dd HH:mm:ss,SSS";
        }
        if (timestampFormat.equals("DATE")) {
            return "dd MMM yyyy HH:mm:ss,SSS";
        }
        return timestampFormat;
    }

    public static Map<String, Map<String, String>> getAppenderConfiguration(File file) {
        try {
            return getXMLFileAppenderConfiguration(file);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            //ignore
        }
        try {
            return getPropertiesFileAppenderConfiguration(file);
        } catch (Exception e) {
            //ignore
        }
        //don't return null
        return new HashMap<>();
    }

    public static Map<String, Map<String, String>> getPropertiesFileAppenderConfiguration(File propertyFile) throws IOException {
        Map<String, Map<String, String>> result = new HashMap<>();
        String appenderPrefix = "log4j.appender";
        Properties props = new Properties();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(propertyFile);
            props.load(inputStream);
            Enumeration propertyNames = props.propertyNames();
            Map<String, String> appenders = new HashMap<>();
            while (propertyNames.hasMoreElements()) {
                String propertyName = propertyNames.nextElement().toString();
                if (propertyName.startsWith(appenderPrefix)) {
                    String value = propertyName.substring(appenderPrefix.length() + 1);
                    if (!value.contains(".")) {
                        //no sub-values - this entry is the appender name & class
                        appenders.put(value, props.getProperty(propertyName).trim());
                    }
                }
            }
            for (Object o : appenders.entrySet()) {
                Map.Entry appenderEntry = (Map.Entry) o;
                String appenderName = appenderEntry.getKey().toString();
                String appenderClassName = appenderEntry.getValue().toString();
                if (appenderClassName.toLowerCase(Locale.ENGLISH).endsWith("fileappender")) {
                    String layout = props.getProperty(appenderPrefix + "." + appenderName + ".layout");
                    if (layout != null && layout.trim().equals("org.apache.log4j.PatternLayout")) {
                        String conversion = props.getProperty(appenderPrefix + "." + appenderName + ".layout.ConversionPattern");
                        String file = props.getProperty(appenderPrefix + "." + appenderName + ".File");
                        if (conversion != null && file != null) {
                            Map<String, String> entry = new HashMap<>();
                            entry.put("file", file.trim());
                            entry.put("conversion", conversion.trim());
                            result.put(appenderName, entry);
                        }
                    }
                }
            }
          /*
          example:
          log4j.appender.R=org.apache.log4j.RollingFileAppender
          log4j.appender.R.File=${catalina.base}/logs/tomcat.log
          log4j.appender.R.MaxFileSize=10MB
          log4j.appender.R.MaxBackupIndex=10
          log4j.appender.R.layout=org.apache.log4j.PatternLayout
          log4j.appender.R.layout.ConversionPattern=%d - %p %t %c - %m%n
           */
        } catch (IOException ioe) {
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        //don't return null
        return result;
    }

    @SuppressFBWarnings // TODO: loading files like this is dangerous - at least in web. see if we can do better
    private static Map<String, Map<String, String>> getXMLFileAppenderConfiguration(File file) throws IOException, ParserConfigurationException, SAXException {
        Map<String, Map<String, String>> result = new HashMap<>();
        try (InputStream stream = file.toURI().toURL().openStream()) {
            InputSource src = new InputSource(stream);
            src.setSystemId(file.toURI().toURL().toString());
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();

//            docBuilder.setErrorHandler(new SAXErrorHandler());
//            docBuilder.setEntityResolver(new Log4jEntityResolver());
            Document doc = docBuilder.parse(src);
            NodeList appenders = doc.getElementsByTagName("appender");
            for (int i = 0; i < appenders.getLength(); i++) {
                Node appender = appenders.item(i);
                NamedNodeMap appenderAttributes = appender.getAttributes();
//        Class appenderClass = Class.forName(map.getNamedItem("class").getNodeValue());
                Node appenderClass = appenderAttributes.getNamedItem("class");
                if (appenderAttributes.getNamedItem("name") != null && appenderClass != null && appenderClass.getNodeValue() != null) {
                    //all log4j fileappenders end in fileappender..if a custom fileappender also ends in fileappender and uses the same dom nodes to be loaded,
                    //try to parse the nodes as well
                    if (appenderClass.getNodeValue().toLowerCase(Locale.ENGLISH).endsWith("fileappender")) {
                        String appenderName = appenderAttributes.getNamedItem("name").getNodeValue();
                        //subclass of FileAppender - add it
                        Map<String, String> entry = new HashMap<>();
                        NodeList appenderChildren = appender.getChildNodes();
                        for (int j = 0; j < appenderChildren.getLength(); j++) {
                            Node appenderChild = appenderChildren.item(j);
                            if (appenderChild.getNodeName().equals("param") && appenderChild.hasAttributes()) {
                                Node fileNameNode = appenderChild.getAttributes().getNamedItem("name");
                                if (fileNameNode != null && fileNameNode.getNodeValue().equalsIgnoreCase("file")) {
                                    Node fileValueNode = appenderChild.getAttributes().getNamedItem("value");
                                    if (fileValueNode != null) {
                                        entry.put("file", fileValueNode.getNodeValue());
                                    }
                                }
                            }
                            if (appenderChild.getNodeName().equalsIgnoreCase("layout") && appenderChild.hasAttributes()) {
                                NamedNodeMap layoutAttributes = appenderChild.getAttributes();
                                Node layoutNode = layoutAttributes.getNamedItem("class");
                                if (layoutNode != null && layoutNode.getNodeValue() != null && layoutNode.getNodeValue().equalsIgnoreCase("org.apache.log4j.PatternLayout")) {
                                    NodeList layoutChildren = appenderChild.getChildNodes();
                                    for (int k = 0; k < layoutChildren.getLength(); k++) {
                                        Node layoutChild = layoutChildren.item(k);
                                        if (layoutChild.getNodeName().equals("param") && layoutChild.hasAttributes()) {
                                            Node layoutName = layoutChild.getAttributes().getNamedItem("name");
                                            if (layoutName != null && layoutName.getNodeValue() != null && layoutName.getNodeValue().equalsIgnoreCase("conversionpattern")) {
                                                Node conversionValue = layoutChild.getAttributes().getNamedItem("value");
                                                if (conversionValue != null) {
                                                    entry.put("conversion", conversionValue.getNodeValue());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        result.put(appenderName, entry);
                    }
                }
            }
        }
//        logger.info("getXMLFileAppenderConfiguration for file: " + file + ", result: " + result);
        return result;
    }
}
