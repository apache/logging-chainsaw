package org.apache.log4j.chainsaw.zeroconf;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ZeroConfPreferenceModel {

    private List monitoredZones = new ArrayList();
    private Set autoConnectDevices = new HashSet();
    
    private transient PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
//   TODO expose addPropertyChangeListener
    
    public void addAutoConnectDevice(String deviceName) {
//        TODO  fire property changes
        autoConnectDevices.add(deviceName);
    }
    
    public void addMonitoredZone(String zone) {
//        TODO fire property change events
        monitoredZones.add(zone);
    }

    public Set getAutoConnectDevices() {
        return autoConnectDevices;
    }

    public void setAutoConnectDevices(Set autoConnectDevices) {
        this.autoConnectDevices = autoConnectDevices;
    }

    public List getMonitoredZones() {
        return monitoredZones;
    }

    public void setMonitoredZones(List monitoredZones) {
        this.monitoredZones = monitoredZones;
    }

    public void removeAutoConnectDevice(String device) {
        autoConnectDevices.remove(device);
    }
    
    
}
