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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.pattern.ClassNamePatternConverter;
import org.apache.log4j.pattern.FileLocationPatternConverter;
import org.apache.log4j.pattern.FullLocationPatternConverter;
import org.apache.log4j.pattern.LevelPatternConverter;
import org.apache.log4j.pattern.LineLocationPatternConverter;
import org.apache.log4j.pattern.LineSeparatorPatternConverter;
import org.apache.log4j.pattern.LiteralPatternConverter;
import org.apache.log4j.pattern.LoggerPatternConverter;
import org.apache.log4j.pattern.LoggingEventPatternConverter;
import org.apache.log4j.pattern.MessagePatternConverter;
import org.apache.log4j.pattern.MethodLocationPatternConverter;
import org.apache.log4j.pattern.NDCPatternConverter;
import org.apache.log4j.pattern.PatternParser;
import org.apache.log4j.pattern.PropertiesPatternConverter;
import org.apache.log4j.pattern.RelativeTimePatternConverter;
import org.apache.log4j.pattern.SequenceNumberPatternConverter;
import org.apache.log4j.pattern.ThreadPatternConverter;

public class LogFilePatternLayoutBuilder
{
    public static String getLogFormatFromPatternLayout(String patternLayout) {
        String input = OptionConverter.convertSpecialChars(patternLayout);
        List converters = new ArrayList();
        List fields = new ArrayList();
        Map converterRegistry = null;

        PatternParser.parse(input, converters, fields, converterRegistry, PatternParser.getPatternLayoutRules());
        return getFormatFromConverters(converters);
    }

    private static String getFormatFromConverters(List converters) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator iter = converters.iterator();iter.hasNext();) {
            LoggingEventPatternConverter converter = (LoggingEventPatternConverter)iter.next();
            if (converter instanceof MessagePatternConverter) {
                buffer.append("MESSAGE");
            } else if (converter instanceof LoggerPatternConverter) {
                buffer.append("LOGGER");
            } else if (converter instanceof ClassNamePatternConverter) {
                buffer.append("CLASS");
            } else if (converter instanceof RelativeTimePatternConverter) {
                buffer.append("PROP(RELATIVETIME)");
            } else if (converter instanceof ThreadPatternConverter) {
                buffer.append("THREAD");
            } else if (converter instanceof NDCPatternConverter) {
                buffer.append("NDC");
            } else if (converter instanceof LiteralPatternConverter) {
                LiteralPatternConverter literal = (LiteralPatternConverter)converter;
                //format shouldn't normally take a null, but we're getting a literal, so passing in the buffer will work
                literal.format(null, buffer);
            } else if (converter instanceof SequenceNumberPatternConverter) {
                buffer.append("PROP(log4jid)");
            } else if (converter instanceof LevelPatternConverter) {
                buffer.append("LEVEL");
            } else if (converter instanceof MethodLocationPatternConverter) {
                buffer.append("METHOD");
            } else if (converter instanceof FullLocationPatternConverter) {
                buffer.append("PROP(locationInfo)");
            } else if (converter instanceof LineLocationPatternConverter) {
                buffer.append("LINE");
            } else if (converter instanceof FileLocationPatternConverter) {
                buffer.append("FILE");
            } else if (converter instanceof PropertiesPatternConverter) {
                PropertiesPatternConverter propertiesConverter = (PropertiesPatternConverter) converter;
//                String option = propertiesConverter.getOption();
//                if (option != null && option.length() > 0) {
//                    buffer.append("PROP(" + option + ")");
//                } else {
                    buffer.append("PROP(PROPERTIES)");
//                }
            } else if (converter instanceof LineSeparatorPatternConverter) {
                //done
            }
        }
        return buffer.toString();
    }
}
