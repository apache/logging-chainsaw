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
package org.apache.log4j.xml;

import junit.framework.TestCase;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.Vector;
import java.net.URL;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;

/**
 * Tests for XMLDecoder.
 *
 */
public class XMLDecoderTest extends TestCase {


  /**
   * Constructor for XMLDecoderTest.
   * @param arg0 test name.
   */
  public XMLDecoderTest(String arg0) {
    super(arg0);
  }

  public String getStringFromResource(final String resourceName,
                                      final int maxSize) throws Exception {
      InputStream is = XMLDecoderTest.class.getResourceAsStream(resourceName);
      if (is == null) {
          throw new FileNotFoundException(resourceName);
      }
      InputStreamReader reader = new InputStreamReader(is, "UTF-8");
      CharBuffer cb = CharBuffer.allocate(maxSize);

      while (reader.read(cb) != -1);

      cb.flip();
      return cb.toString();
  }

    public void testDecodeEventsString1() throws Exception {
        String xmlStr = getStringFromResource("xmlLayout.1.xml", 10000);
        XMLDecoder decoder = new XMLDecoder();
        Vector<ChainsawLoggingEvent> events = decoder.decodeEvents(xmlStr);
        assertEquals(17, events.size());
    }

  public void testDecodeEventsString2() throws Exception {
      String xmlStr = getStringFromResource("xsltLayout.1.xml", 10000);
      XMLDecoder decoder = new XMLDecoder();
      Vector<ChainsawLoggingEvent> events = decoder.decodeEvents(xmlStr);
      assertEquals(15, events.size());
  }

    public void testDecodeEventsURL1() throws Exception {
        URL resource = XMLDecoderTest.class.getResource("xmlLayout.1.xml");
        XMLDecoder decoder = new XMLDecoder();
        Vector<ChainsawLoggingEvent> events = decoder.decode(resource);
        assertEquals(17, events.size());
    }

    public void testDecodeEventsURL2() throws Exception {
        URL resource = XMLDecoderTest.class.getResource("xsltLayout.1.xml");
        XMLDecoder decoder = new XMLDecoder();
        Vector<ChainsawLoggingEvent> events = decoder.decode(resource);
        assertEquals(15, events.size());
    }

}
