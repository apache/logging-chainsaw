package org.apache.log4j.net;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.log4j.Level;


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
    
    private Object logger;
    private Method logInfoMethod;
    private Method logErrorMethod;
    
    public ZeroConfSocketHubAppender() {
        setName("SocketHubAppender");
        try {
            Method getLoggerMethod = this.getClass().getMethod("getLogger", new Class[0]);
            logger = getLoggerMethod.invoke(this, new Object[0]);
            logInfoMethod = logger.getClass().getMethod("info", new Class[] {Object.class});
            logErrorMethod = logger.getClass().getMethod("error", new Class[] {Object.class});
        }catch(Exception e) {
            // we're not in log4j1.3 land
        }
    }
    public void activateOptions() {
        super.activateOptions();
        
        try {
            JmDNS jmDNS = Zeroconf4log4j.getInstance();
            ServiceInfo info = buildServiceInfo();
            logWithlog4j12Compatibility(Level.INFO,"Registering this SocketHubAppender as :" + info);
            jmDNS.registerService(info);
        } catch (IOException e) {
            logWithlog4j12Compatibility(Level.ERROR,"Failed to instantiate JmDNS to broadcast via ZeroConf, will now operate in simple SocketHubAppender mode");
        }
    }
    private ServiceInfo buildServiceInfo() {
        return new ServiceInfo(zeroConfZone, getName(), getPort(), "SocketHubAppender on port " + getPort() );
    }
    
    private void logWithlog4j12Compatibility(Level level, String message) {
        if(logger!=null && logInfoMethod!=null & logErrorMethod!=null) {
            try {
                switch (level.toInt()) {
                case Level.INFO_INT:
                    logInfoMethod.invoke(logger, new Object[] { message });
                    break;
                case Level.ERROR_INT:
                    logInfoMethod.invoke(logger, new Object[] { message });
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
    public synchronized void close() {
        super.close();
        try {
            JmDNS jmDNS = Zeroconf4log4j.getInstance();
            ServiceInfo info = buildServiceInfo();
            logWithlog4j12Compatibility(Level.INFO,"Deregistering this SocketHubAppender (" + info + ")");
            jmDNS.unregisterService(info);
        } catch (Exception e) {
            logWithlog4j12Compatibility(Level.ERROR,"Failed to instantiate JmDNS to broadcast via ZeroConf, will now operate in simple SocketHubAppender mode");
        }
    }
    
    
}
