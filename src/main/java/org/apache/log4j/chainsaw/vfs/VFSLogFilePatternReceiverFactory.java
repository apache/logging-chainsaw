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
package org.apache.log4j.chainsaw.vfs;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverFactory;
import org.apache.log4j.varia.LogFilePatternReceiver;

/**
 *
 */
public class VFSLogFilePatternReceiverFactory implements ChainsawReceiverFactory {

    @Override
    public ChainsawReceiver create() {
        return new VFSLogFilePatternReceiver();
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() throws IntrospectionException {
        return new PropertyDescriptor[] {
            new PropertyDescriptor("name", LogFilePatternReceiver.class),
            new PropertyDescriptor("fileURL", LogFilePatternReceiver.class),
            new PropertyDescriptor("appendNonMatches", LogFilePatternReceiver.class),
            new PropertyDescriptor("filterExpression", LogFilePatternReceiver.class),
            new PropertyDescriptor("tailing", LogFilePatternReceiver.class),
            new PropertyDescriptor("logFormat", LogFilePatternReceiver.class),
            new PropertyDescriptor("group", LogFilePatternReceiver.class),
            new PropertyDescriptor("timestampFormat", LogFilePatternReceiver.class),
            new PropertyDescriptor("waitMillis", LogFilePatternReceiver.class),
        };
    }

    @Override
    public String getReceiverName() {
        return "VFSLogFilePatternReceiver";
    }

    @Override
    public String getReceiverDocumentation() {
        return "<html>VFSLogFilePatternReceiver can parse and tail log files, converting entries into\n"
                + "LoggingEvents.  If the file doesn't exist when the receiver is initialized, the\n"
                + "receiver will look for the file once every 10 seconds.\n"
                + "<p>\n"
                + "This receiver relies on java.util.regex features to perform the parsing of text in the\n"
                + "log file, however the only regular expression field explicitly supported is\n"
                + " * a glob-style wildcard used to ignore fields in the log file if needed.  All other\n"
                + "fields are parsed by using the supplied keywords.\n"
                + "<p>\n"
                + "<b>Features:</b><br>\n"
                + "- specify the URL of the log file to be processed<br>\n"
                + "- specify the timestamp format in the file (if one exists, using patterns from {@link java.text.SimpleDateFormat})<br>\n"
                + "- specify the pattern (logFormat) used in the log file using keywords, a wildcard character (*) and fixed text<br>\n"
                + "- 'tail' the file (allows the contents of the file to be continually read and new events processed)<br>\n"
                + "- supports the parsing of multi-line messages and exceptions\n"
                + "- 'hostname' property set to URL host (or 'file' if not available)\n"
                + "- 'application' property set to URL path (or value of fileURL if not available)\n"
                + "- 'group' property can be set to associate multiple log file receivers\n"
                + "<p>\n"
                + "<b>Keywords:</b><br>\n"
                + "TIMESTAMP<br>\n"
                + "LOGGER<br>\n"
                + "LEVEL<br>\n"
                + "THREAD<br>\n"
                + "CLASS<br>\n"
                + "FILE<br>\n"
                + "LINE<br>\n"
                + "METHOD<br>\n"
                + "RELATIVETIME<br>\n"
                + "MESSAGE<br>\n"
                + "NDC<br>\n"
                + "PROP(key)<br>\n"
                + "(NL)<br>\n"
                + "<p>\n"
                + "(NL) represents a new line embedded in the log format, supporting log formats whose fields span multiple lines\n"
                + "<p>\n"
                + "Use a * to ignore portions of the log format that should be ignored\n"
                + "<p>\n"
                + "Example:<br>\n"
                + "If your file's patternlayout is this:<br>\n"
                + "<b>%d %-5p [%t] %C{2} (%F:%L) - %m%n</b>\n"
                + "<p>\n"
                + "specify this as the log format:<br>\n"
                + "<b>TIMESTAMP LEVEL [THREAD] CLASS (FILE:LINE) - MESSAGE</b>\n"
                + "<p>\n"
                + "To define a PROPERTY field, use PROP(key)\n"
                + "<p>\n"
                + "Example:<br>\n"
                + "If you used the RELATIVETIME pattern layout character in the file,\n"
                + "you can use PROP(RELATIVETIME) in the logFormat definition to assign\n"
                + "the RELATIVETIME field as a property on the event.\n"
                + "<p>\n"
                + "If your file's patternlayout is this:<br>\n"
                + "<b>%r [%t] %-5p %c %x - %m%n</b>\n"
                + "<p>\n"
                + "specify this as the log format:<br>\n"
                + "<b>PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE</b>\n"
                + "<p>\n"
                + "Note the * - it can be used to ignore a single word or sequence of words in the log file\n"
                + "(in order for the wildcard to ignore a sequence of words, the text being ignored must be\n"
                + "followed by some delimiter, like '-' or '[') - ndc is being ignored in the following example.\n"
                + "<p>\n"
                + "Assign a filterExpression in order to only process events which match a filter.\n"
                + "If a filterExpression is not assigned, all events are processed.\n"
                + "<p>\n"
                + "<b>Limitations:</b><br>\n"
                + "- no support for the single-line version of throwable supported by patternlayout<br>\n"
                + "(this version of throwable will be included as the last line of the message)<br>\n"
                + "- the relativetime patternLayout character must be set as a property: PROP(RELATIVETIME)<br>\n"
                + "- messages should appear as the last field of the logFormat because the variability in message content<br>\n"
                + "- exceptions are converted if the exception stack trace (other than the first line of the exception)<br>\n"
                + "is stored in the log file with a tab followed by the word 'at' as the first characters in the line<br>\n"
                + "- tailing may fail if the file rolls over.\n"
                + "<p>\n"
                + "<b>Example receiver configuration settings</b> (add these as params, specifying a LogFilePatternReceiver 'plugin'):<br>\n"
                + "param: \"timestampFormat\" value=\"yyyy-MM-d HH:mm:ss,SSS\"<br>\n"
                + "param: \"logFormat\" value=\"PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE\"<br>\n"
                + "param: \"fileURL\" value=\"file:///c:/events.log\"<br>\n"
                + "param: \"tailing\" value=\"true\"\n"
                + "<p>\n"
                + "This configuration will be able to process these sample events:<br>\n"
                + "710    [       Thread-0] DEBUG                   first.logger first - &lt;test&gt;   &lt;test2&gt;something here&lt;/test2&gt;   &lt;test3 blah=something/&gt;   &lt;test4&gt;       &lt;test5&gt;something else&lt;/test5&gt;   &lt;/test4&gt;&lt;/test&gt;<br>\n"
                + "880    [       Thread-2] DEBUG                   first.logger third - &lt;test&gt;   &lt;test2&gt;something here&lt;/test2&gt;   &lt;test3 blah=something/&gt;   &lt;test4&gt;       &lt;test5&gt;something else&lt;/test5&gt;   &lt;/test4&gt;&lt;/test&gt;<br>\n"
                + "880    [       Thread-0] INFO                    first.logger first - infomsg-0<br>\n"
                + "java.lang.Exception: someexception-first<br>\n"
                + "at Generator2.run(Generator2.java:102)<br>";
    }
}
