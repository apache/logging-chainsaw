package org.apache.log4j.net;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 
 * A test bed class to configure and launch a ZeroConfSocketHubAppender and stream
 * some LoggingEvents to it so that one can test Chainsaw
 * 
 * @author psmith
 *
 */
public class ZeroConfSocketHubAppenderTestBed {

    public static void main(String[] args) throws Exception {
        ZeroConfSocketHubAppender appender = new ZeroConfSocketHubAppender();
        appender.activateOptions();
        Logger LOG = LogManager.getRootLogger();
        LOG.addAppender(appender);
        
        while(true) {
            LOG.info("TestBedEvent: " + System.currentTimeMillis());
            Thread.sleep(250);
        }
        
        
    }
}
