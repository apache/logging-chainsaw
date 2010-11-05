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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.pattern.ClassNamePatternConverter;
import org.apache.log4j.pattern.DatePatternConverter;
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
import org.apache.log4j.xml.Log4jEntityResolver;
import org.apache.log4j.xml.SAXErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
  
    private static String getFormatFromConverters(List converters) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator iter = converters.iterator();iter.hasNext();) {
            LoggingEventPatternConverter converter = (LoggingEventPatternConverter)iter.next();
            if (converter instanceof DatePatternConverter) {
              buffer.append("TIMESTAMP");
            } else if (converter instanceof MessagePatternConverter) {
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

  public static Map getAppenderConfiguration(File file) throws IOException, ParserConfigurationException, SAXException {
    InputStream stream = file.toURI().toURL().openStream();
    Map result = new HashMap();
    try {
      InputSource src = new InputSource(stream);
      src.setSystemId(file.toURI().toURL().toString());
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbf.newDocumentBuilder();

      docBuilder.setErrorHandler(new SAXErrorHandler());
      docBuilder.setEntityResolver(new Log4jEntityResolver());
      Document doc = docBuilder.parse(src);
      NodeList appenders = doc.getElementsByTagName("appender");
      for (int i = 0; i < appenders.getLength(); i++) {
        Node appender = appenders.item(i);
        NamedNodeMap appenderAttributes = appender.getAttributes();
//        Class appenderClass = Class.forName(map.getNamedItem("class").getNodeValue());
        Node appenderClass = appenderAttributes.getNamedItem("class");
        try {
          if (appenderAttributes.getNamedItem("name") != null && appenderClass != null && appenderClass.getNodeValue() != null) {
            //load the class or try to support custom subclasses that we don't have available in the classpath but end in 'FileAppender'
            if (appenderClass.getNodeValue().toLowerCase().endsWith("fileappender") || FileAppender.class.isAssignableFrom(Class.forName(appenderClass.getNodeValue()))) {
              String appenderName = appenderAttributes.getNamedItem("name").getNodeValue();
              //subclass of FileAppender - add it
              Map entry = new HashMap();
              NodeList appenderChildren = appender.getChildNodes();
              for (int j = 0; j < appenderChildren.getLength(); j++) {
                Node appenderChild = appenderChildren.item(j);
                if (appenderChild.getNodeName().equals("param") && appenderChild.hasAttributes()) {
                  Node fileNameNode = appenderChild.getAttributes().getNamedItem("name");
                  if (fileNameNode != null && fileNameNode.getNodeValue().equals("file")) {
                    Node fileValueNode = appenderChild.getAttributes().getNamedItem("value");
                    if (fileValueNode != null) {
                      entry.put("file", fileValueNode.getNodeValue());
                    }
                  }
                }
                if (appenderChild.getNodeName().equals("layout") && appenderChild.hasAttributes()) {
                  NamedNodeMap layoutAttributes = appenderChild.getAttributes();
                  Node layoutNode = layoutAttributes.getNamedItem("class");
                  if (layoutNode != null && layoutNode.getNodeValue() != null && layoutNode.getNodeValue().equals("org.apache.log4j.PatternLayout")) {
                    NodeList layoutChildren = appenderChild.getChildNodes();
                    for (int k = 0; k < layoutChildren.getLength(); k++) {
                      Node layoutChild = layoutChildren.item(k);
                      if (layoutChild.getNodeName().equals("param") && layoutChild.hasAttributes()) {
                        Node layoutName = layoutChild.getAttributes().getNamedItem("name");
                        if (layoutName != null && layoutName.getNodeValue() != null && layoutName.getNodeValue().equals("ConversionPattern")) {
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
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } finally {
      stream.close();
    }
    return result;
  }
}
