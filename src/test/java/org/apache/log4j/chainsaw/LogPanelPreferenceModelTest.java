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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import junit.framework.TestCase;

public class LogPanelPreferenceModelTest extends TestCase {

    public void testLogPanelPreferenceModelSerialization() throws Exception {
        LogPanelPreferenceModel model = new LogPanelPreferenceModel();
        
        
        /**
         * First modify the model from it's default state so we know we're actually storing
         * something 'different' for deserialization tests
         */
        
        model.setLevelIcons(!model.isLevelIcons());
        model.setWrapMessage(!model.isWrapMessage());
        model.setDateFormatPattern("yyyyDDmm");
        model.setLoggerPrecision("FATAL");
        model.setLogTreePanelVisible(!model.isLogTreePanelVisible());
        model.setScrollToBottom(model.isScrollToBottom());
        model.setToolTips(!model.isToolTips());
        
        XStream stream = new XStream(new DomDriver());
        String xml = stream.toXML(model);
//        System.out.println(xml);
        
        LogPanelPreferenceModel restored = (LogPanelPreferenceModel) stream.fromXML(xml);
        
        assertEquals(model.isLevelIcons(), restored.isLevelIcons());
        assertEquals(model.getDateFormatPattern(), restored.getDateFormatPattern());
        assertEquals(model.getLoggerPrecision(), restored.getLoggerPrecision());
        assertEquals(model.isLogTreePanelVisible(), restored.isLogTreePanelVisible());
        assertEquals(model.isScrollToBottom(), restored.isScrollToBottom());
        assertEquals(model.isToolTips(), restored.isToolTips());
        
        
        
    }
}
