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
package org.apache.log4j.chainsaw.layout;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.log4j.chainsaw.logevents.LocationInfo;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * This layout is used for formatting HTML text for use inside
 * the Chainsaw Event Detail Panel, and the tooltip used
 * when mouse-over on a particular log event row.
 * <p>
 * It relies an an internal PatternLayout to accomplish this, but ensures HTML characters
 * from any LoggingEvent are escaped first.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class EventDetailLayout {

    private String m_conversionPattern;
    private DateTimeFormatter m_dateFormat;

    public EventDetailLayout() {
        m_dateFormat = DateTimeFormatter.ISO_LOCAL_TIME;
    }

    public void setConversionPattern(String conversionPattern) {
        m_conversionPattern = conversionPattern;
    }

    public String getConversionPattern() {
        return m_conversionPattern;
    }

    public void setDateformat(DateTimeFormatter dateFormat) {
        m_dateFormat = dateFormat;
    }

    public DateTimeFormatter getDateformat() {
        return m_dateFormat;
    }

    //  /* (non-Javadoc)
    //   * @see org.apache.log4j.Layout#format(java.io.Writer, org.apache.log4j.spi.LoggingEvent)
    //   */
    //  public void format(Writer output, LoggingEvent event)
    //    throws IOException {
    //    boolean pastFirst = false;
    //    output.write("<html><body><table cellspacing=0 cellpadding=0>");
    //
    //    List columnNames = ChainsawColumns.getColumnsNames();
    //
    //    Vector v = ChainsawAppenderHandler.convert(event);
    //
    //    /**
    //     * we need to add the ID property from the event
    //     */
    //    v.add(event.getProperty(ChainsawConstants.LOG4J_ID_KEY));
    //
    //    //             ListIterator iter = displayFilter.getDetailColumns().listIterator();
    //    Iterator iter = columnNames.iterator();
    //    String column = null;
    //    int index = -1;
    //
    //    while (iter.hasNext()) {
    //      column = (String) iter.next();
    //      index = columnNames.indexOf(column);
    //
    //      if (index > -1) {
    //        if (pastFirst) {
    //          output.write("</td></tr>");
    //        }
    //
    //        output.write("<tr><td valign=\"top\"><b>");
    //        output.write(column);
    //        output.write(": </b></td><td>");
    //
    //
    //        if (index<v.size()) {
    //			Object o = v.get(index);
    //
    //			if (o != null) {
    //				output.write(escape(o.toString()));
    //			} else {
    //				output.write("{null}");
    //			}
    //
    //		}else {
    ////            output.write("Invalid column " + column + " (index=" + index + ")");
    //        }
    //
    //        pastFirst = true;
    //      }
    //    }
    //
    //    output.write("</table></body></html>");
    //  }

    /**
     * Escape &lt;, &gt; &amp; and &quot; as their entities. It is very
     * dumb about &amp; handling.
     *
     * @param aStr the String to escape.
     * @return the escaped String
     */
    private static String escape(String string) {
        if (string == null) {
            return "";
        }

        final StringBuilder buf = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            switch (c) {
                case '<':
                    buf.append("&lt;");

                    break;

                case '>':
                    buf.append("&gt;");

                    break;

                case '\"':
                    buf.append("&quot;");

                    break;

                case '&':
                    buf.append("&amp;");

                    break;

                default:
                    buf.append(c);

                    break;
            }
        }

        return buf.toString();
    }

    /**
     * Takes a source event and copies it into a new LoggingEvent object
     * and ensuring all the internal elements of the event are HTML safe
     *
     * @param event
     * @return new LoggingEvent
     */
    private static ChainsawLoggingEvent copyForHTML(final ChainsawLoggingEvent event) {
        ChainsawLoggingEventBuilder build = new ChainsawLoggingEventBuilder();
        build.copyFromEvent(event);

        build.setMessage(escape(event.m_message));
        LocationInfo li = event.m_locationInfo;
        if (li != null) {
            li = new LocationInfo(escape(li.fileName), escape(li.className), escape(li.methodName), li.lineNumber);
            build.setLocationInfo(li);
        }

        return build.create();
    }

    /**
     * @param event
     * @return
     */
    //    private static Hashtable<String, String> formatProperties(LoggingEvent event) {
    //        Set keySet = event.getPropertyKeySet();
    //        Hashtable<String, String> hashTable = new Hashtable<>();
    //
    //        for (Object key : keySet) {
    //            Object value = event.getProperty(key.toString());
    //            hashTable.put(escape(key.toString()), escape(value.toString()));
    //        }
    //
    //        return hashTable;
    //    }

    /* (non-Javadoc)
     * @see org.apache.log4j.Layout#ignoresThrowable()
     */
    public boolean ignoresThrowable() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.spi.OptionHandler#activateOptions()
     */
    public void activateOptions() {}

    /* (non-Javadoc)
     * @see org.apache.log4j.Layout#format(java.io.Writer, org.apache.log4j.spi.LoggingEvent)
     */
    public String format(final ChainsawLoggingEvent event) {
        ChainsawLoggingEvent newEvent = copyForHTML(event);

        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("level", event.m_level.toString());
        valuesMap.put("logger", event.m_logger);
        valuesMap.put("time", event.m_timestamp.atZone(ZoneId.systemDefault()).format(m_dateFormat));
        valuesMap.put("millisdelta", event.getProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE));
        valuesMap.put("thread", event.m_threadName);
        valuesMap.put("message", event.m_message);
        valuesMap.put("marker", "");
        valuesMap.put("throwable", "");
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        String resolvedString = sub.replace(m_conversionPattern);

        return resolvedString;
        //        synchronized (patternLayout) {
        //            return patternLayout.format(newEvent);
        //        }
    }
}
