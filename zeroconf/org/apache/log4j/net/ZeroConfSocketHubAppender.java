package org.apache.log4j.net;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;


/**
 * A sub-class of SocketHubAppender that broadcasts its configuration via Zeroconf.
 * 
 * This allows Zeroconf aware applications such as Chainsaw to be able to detect them, and automatically configure
 * themselves to be able to connect to them.
 * 
 * @author psmith
 *
 */
public class ZeroConfSocketHubAppender extends SocketHubAppender {

    public static final String DEFAULT_ZEROCONF_ZONE="_log4j._tcp.local.";
    private String zeroConfZone = DEFAULT_ZEROCONF_ZONE;
    
    private String zeroConfDeviceName = "SocketHubAppender";
    
    public void activateOptions() {
        super.activateOptions();
        
        try {
            JmDNS jmDNS = Zeroconf4log4j.getInstance();
            ServiceInfo info = new ServiceInfo(zeroConfZone, zeroConfDeviceName, getPort(), "SocketHubAppender on port " + getPort() );
            getLogger().info("Registering this SocketHubAppender as :" + info);
            jmDNS.registerService(info);
        } catch (IOException e) {
            getLogger().error("Failed to instantiate JmDNS to broadcast via ZeroConf, will now operate in simple SocketHubAppender mode");
        }
        
    }

    /**
     * Sets the name of this appender as it would appear in a ZeroConf browser.
     * @see #setZeroConfDeviceName(String)
     * @return String deviceName
     */
    public String getZeroConfDeviceName() {
        return zeroConfDeviceName;
    }




    /**
     * Configures the name/label of this appender so that it will appear nicely in a ZeroConf browser, the default
     * being "SocketHubAppender"
     * @param zeroConfDeviceName
     */
    public void setZeroConfDeviceName(String zeroConfDeviceName) {
        this.zeroConfDeviceName = zeroConfDeviceName;
    }




    /**
     * Returns the ZeroConf domain that will be used to register this 'device'.
     * 
     * @return String ZeroConf zone
     */
    public String getZeroConfZone() {
        return zeroConfZone;
    }


    /**
     * Sets the ZeroConf zone to register this device under, BE CAREFUL with this value
     * as ZeroConf has some weird naming conventions, it should start with an "_" and end in a ".",
     * if you're not sure about this value might I suggest that you leave it at the default value
     * which is specified in {@link #DEFAULT_ZEROCONF_ZONE }.
     * 
     * This method does NO(0, zero, pun not intended) checks on this value.
     * 
     * @param zeroConfZone
     */
    public void setZeroConfZone(String zeroConfZone) {
//        TODO work out a sane checking mechanism that verifies the value is a correct ZeroConf zone
        this.zeroConfZone = zeroConfZone;
    }
}
