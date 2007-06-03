package org.apache.log4j.net;

import javax.jmdns.JmDNS;

/**
 * This singleton holds the single instance of the JmDNS instance that is used to broadcast
 * Appender related information via ZeroConf.  Once referenced, a single JmDNS instance is created
 * and held.  To ensure your JVM exits cleanly you should ensure that you call the {@link #shutdown() } method
 * to broadcast the disappearance of your devices, and cleanup sockets.  (alternatively you can call the close() 
 * method on the JmDNS instead, totally up to you...)
 * 
 * See http://jmdns.sf.net for more information about JmDNS and ZeroConf.
 * 
 * @author psmith
 *
 */
public class Zeroconf4log4j {

    private static final JmDNS instance;

    static {
        try {
            instance = new JmDNS();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize JmDNS");
        }
    }

    /**
     * Returns the current instance of the JmDNS being used by log4j.
     * 
     * @throws IllegalStateException if JmDNS was not correctly initialized.
     * 
     * @return
     */
    public static JmDNS getInstance() {
        checkState();
        return instance;
    }

    private static void checkState() {
        if (instance == null) {
            throw new IllegalStateException(
                    "JmDNS did not initialize correctly");
        }
    }
    
    /**
     * Ensures JmDNS cleanly broadcasts 'goodbye' and closes any sockets, and (more imporantly)
     * ensures some Threads exit so your JVM can exit.
     *
     */
    public static void shutdown() {
        checkState();
        instance.close();
    }
}
