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
package org.apache.log4j.chainsaw.zeroconf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ZeroConfPreferenceModel {

    private List<String> monitoredZones = new ArrayList<>();
    private Set<String> autoConnectDevices = new HashSet<>();
    
//   TODO expose addPropertyChangeListener
    
    public void addAutoConnectDevice(String deviceName) {
//        TODO  fire property changes
        autoConnectDevices.add(deviceName);
    }
    
    public void addMonitoredZone(String zone) {
//        TODO fire property change events
        monitoredZones.add(zone);
    }

    public Set<String> getAutoConnectDevices() {
        return autoConnectDevices;
    }

    public void setAutoConnectDevices(Set<String> autoConnectDevices) {
        this.autoConnectDevices = autoConnectDevices;
    }

    public List<String> getMonitoredZones() {
        return monitoredZones;
    }

    public void setMonitoredZones(List<String> monitoredZones) {
        this.monitoredZones = monitoredZones;
    }

    public void removeAutoConnectDevice(String device) {
        autoConnectDevices.remove(device);
    }
    
    
}
