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
