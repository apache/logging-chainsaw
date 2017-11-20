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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.HashMap;
import java.util.Map;


/**
 * This layout is used for formatting HTML text for use inside
 * the Chainsaw Event Detail Panel, and the tooltip used
 * when mouse-over on a particular log event row.
 *
 * It relies an an internal PatternLayout to accomplish this, but ensures HTML characters
 * from any LogEvent are escaped first.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class EventDetailLayout {
  private PatternLayout patternLayout;

  public synchronized void setConversionPattern(String conversionPattern) {
    patternLayout = PatternLayout.newBuilder().withPattern(conversionPattern).build();
  }

  public synchronized String getConversionPattern() {
    return patternLayout.getConversionPattern();
  }

  public String getFooter() {
    return "";
  }

  public String getHeader() {
    return "";
  }

  /**
    * Escape &lt;, &gt; &amp; and &quot; as their entities. It is very
    * dumb about &amp; handling.
    * @param string the String to escape.
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
      }
    }

    return buf.toString();
  }

  /**
   * Takes a source event and copies it into a new LogEvent object
   * and ensuring all the internal elements of the event are HTML safe
   * @param event
   * @return new LogEvent
   */
  private static LogEvent copyForHTML(LogEvent event) {
    String msg = escape(event.getMessage().getFormattedMessage());
    StackTraceElement li = null;
    if (event.getSource() != null) {
        li = formatLocationInfo(event);
    }
    Map<String, String> properties = formatProperties(event);

    return Log4jLogEvent.newBuilder()
            .setLoggerFqcn(event.getLoggerFqcn())
            .setLoggerName(event.getLoggerName())
            .setTimeMillis(event.getTimeMillis())
            .setNanoTime(event.getNanoTime())
            .setLevel(event.getLevel())
            .setMessage(new SimpleMessage(msg))
            .setThreadName(event.getThreadName())
            .setThreadId(event.getThreadId())
            .setThreadPriority(event.getThreadPriority())
            .setThrownProxy(event.getThrownProxy())
            .setContextStack(event.getContextStack())
            .setContextMap(properties)
            .setSource(li)
            .setMarker(event.getMarker())
            .build();
  }

  private static StackTraceElement formatLocationInfo(LogEvent event) {
    StackTraceElement info = event.getSource();
    return new StackTraceElement(
      escape(info.getFileName()), escape(info.getClassName()),
      escape(info.getMethodName()), info.getLineNumber());
  }

  private static Map<String, String> formatProperties(LogEvent event) {
    Map<String, String> hashTable = new HashMap<>();
    event.getContextData().forEach(new TriConsumer<String, String, Map<String, String>>() {
      @Override
      public void accept(String key, String value, Map<String, String> state) {
        state.put(escape(key), escape(value));
      }
    }, hashTable);
    return hashTable;
  }

  public String format(final LogEvent event) {
      LogEvent newEvent = copyForHTML(event);
      /*
       * Layouts are not thread-safe, but are normally
       * protected by the fact that their Appender is thread-safe.
       * 
       * But here in Chainsaw there is no such guarantees.
       */ 
      synchronized (this) {
          return patternLayout.toSerializable(newEvent);
      }
  }
}
