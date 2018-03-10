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

package org.apache.log4j.chainsaw.layout;

import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;


/**
 * This layout is used for formatting HTML text for use inside
 * the Chainsaw Event Detail Panel, and the tooltip used
 * when mouse-over on a particular log event row.
 *
 * It relies an an internal PatternLayout to accomplish this, but ensures HTML characters
 * from any LoggingEvent are escaped first.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class EventDetailLayout extends Layout {
  private EnhancedPatternLayout patternLayout = new EnhancedPatternLayout();

  public EventDetailLayout() {
  }

  public void setConversionPattern(String conversionPattern) {
    patternLayout.setConversionPattern(conversionPattern);
    patternLayout.activateOptions();
  }

  public String getConversionPattern() {
    return patternLayout.getConversionPattern();
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.Layout#getFooter()
   */
  public String getFooter() {
    return "";
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.Layout#getHeader()
   */
  public String getHeader() {
    return "";
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
   * @param event
   * @return new LoggingEvent
   */
  private static LoggingEvent copyForHTML(LoggingEvent event) {
    Logger logger = Logger.getLogger(event.getLoggerName());
    String threadName = event.getThreadName();
    Object msg = escape(event.getMessage().toString());
    String ndc = event.getNDC();
//    Hashtable mdc = formatMDC(event);
    LocationInfo li = null;
    if (event.locationInformationExists()) {
        li = formatLocationInfo(event);
    }
    Hashtable properties = formatProperties(event);
    LoggingEvent copy = new LoggingEvent(null,
	   logger, event.getTimeStamp(),
	   event.getLevel(),
	   msg,
	   threadName,
	   event.getThrowableInformation(),
	   ndc,
	   li,
	   properties);
    
    return copy;
  }

//  /**
//  * @param event
//  * @return
//  */
//  private static Hashtable formatMDC(LoggingEvent event) {
//    Set keySet = event.getMDCKeySet();
//    Hashtable hashTable = new Hashtable();
//
//    for (Iterator iter = keySet.iterator(); iter.hasNext();) {
//      Object key = (Object) iter.next();
//      Object value = event.getMDC(key.toString());
//      hashTable.put(escape(key.toString()), escape(value.toString()));
//    }
//
//    return hashTable;
//  }

  /**
   * @param event
   * @return
   */
  private static LocationInfo formatLocationInfo(LoggingEvent event) {
    LocationInfo info = event.getLocationInformation();
    LocationInfo newInfo =
      new LocationInfo(
        escape(info.getFileName()), escape(info.getClassName()),
        escape(info.getMethodName()), escape(info.getLineNumber()));

    return newInfo;
  }

  /**
   * @param event
   * @return
   */
  private static Hashtable formatProperties(LoggingEvent event) {
    Set keySet = event.getPropertyKeySet();
    Hashtable hashTable = new Hashtable();

      for (Object key : keySet) {
          Object value = event.getProperty(key.toString());
          hashTable.put(escape(key.toString()), escape(value.toString()));
      }

    return hashTable;
  }

  /* (non-Javadoc)
     * @see org.apache.log4j.Layout#ignoresThrowable()
     */
  public boolean ignoresThrowable() {
    return false;
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.spi.OptionHandler#activateOptions()
   */
  public void activateOptions() {
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.Layout#format(java.io.Writer, org.apache.log4j.spi.LoggingEvent)
   */
  public String format(final LoggingEvent event) {
      LoggingEvent newEvent =  copyForHTML(event);
      /**
       * Layouts are not thread-safe, but are normally
       * protected by the fact that their Appender is thread-safe.
       * 
       * But here in Chainsaw there is no such guarantees.
       */ 
      synchronized(patternLayout) {
          return patternLayout.format(newEvent);
      }
  }
}
