package org.apache.log4j.net;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import junit.framework.TestCase;

/**
 * Some test methods to validate that the ZeroConf stuff works as expected/advertised
 * 
 * @author psmith
 *
 */
public class ZeroConfSocketHubAppenderTest extends TestCase {

    private static final int DEFAULT_TIMEOUT_FOR_ZEROCONF_EVENTS_TO_APPEAR = 2000;

    /**
     * This does a simple test, as a test harness, to make sure the Appender can be created
     * and that it can shutdown appropriately.  in older versions of JmDNS a non-daemon thread
     * could hold the JVM open preventing it from shutting down.
     * 
     * @see com.strangeberry.jmdns.tools.Main for a ZeroConf Network browser in Swing allowing you to see the broadcasts
     * 
     * @throws Exception
     */
    public void testSimpleTest() throws Exception {
        JmDNS jmdns = Zeroconf4log4j.getInstance();
        
        final ModifiableBoolean addedFlag = new ModifiableBoolean();
        final ModifiableBoolean removedFlag = new ModifiableBoolean();
        
        /**
         * This is just a test to make sure I'm not stupid.
         */
        assertTrue(!addedFlag.isSet());
        assertTrue(!removedFlag.isSet());
        
        jmdns.addServiceListener(ZeroConfSocketHubAppender.DEFAULT_ZEROCONF_ZONE, new ServiceListener() {

            public void serviceAdded(ServiceEvent event) {
                addedFlag.setValue(true);
               
            }

            public void serviceRemoved(ServiceEvent event) {
                removedFlag.setValue(true);
            }

            public void serviceResolved(ServiceEvent event) {
                
            }});
        ZeroConfSocketHubAppender appender = new ZeroConfSocketHubAppender();
        appender.setName("SimpleTest");
        appender.activateOptions();
        
        Thread.sleep(DEFAULT_TIMEOUT_FOR_ZEROCONF_EVENTS_TO_APPEAR);
        
        assertTrue("Should have detected the addition", addedFlag.isSet());
        
        appender.close();
        Zeroconf4log4j.shutdown();
        
        Thread.sleep(DEFAULT_TIMEOUT_FOR_ZEROCONF_EVENTS_TO_APPEAR);
        
    }
}
