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
import java.util.Iterator;
import java.util.List;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.chainsaw.icons.ChainsawIcons;

public class ZeroConfDeviceModel extends AbstractTableModel implements ServiceListener {

    private List deviceList = new ArrayList();
    private ZeroConfPreferenceModel zeroConfPreferenceModel;
    private transient ZeroConfPlugin plugin;
    
    public ZeroConfDeviceModel() {
    }
    
    public int getRowCount() {
        return deviceList.size();
    }

    public int getColumnCount() {
        return 4;
    }

    public ServiceInfo getServiceInfoAtRow(int row) {
        return (ServiceInfo) deviceList.get(row);
    }
    public Object getValueAt(int rowIndex, int columnIndex) {
        ServiceInfo info = (ServiceInfo) deviceList.get(rowIndex);
        if(info == null) {
            return "";
        }
        switch(columnIndex) {
        case 0:
                return getAutoConnectHandle(info);
        case 1:
                return info.getAddress().getHostName() + ":" + info.getPort();
        case 2:
                return zeroConfPreferenceModel.getAutoConnectDevices().contains(getAutoConnectHandle(info))?Boolean.TRUE:Boolean.FALSE;
        case 3:
                return plugin.isConnectedTo(info)?"Connected":"Not Connected";
//                return plugin.isConnectedTo(info)?new ImageIcon(ChainsawIcons.ANIM_NET_CONNECT):new ImageIcon();
            default:
                    return "";
        }
    }

    private String getAutoConnectHandle(ServiceInfo info) {
        return info.getName();
    }

    public void serviceAdded(ServiceEvent event) {
    }

    public void serviceRemoved(ServiceEvent event) {
        for (Iterator iter = deviceList.iterator(); iter.hasNext();) {
            ServiceInfo info = (ServiceInfo) iter.next();
            if(info.getName().equals(event.getName())) {
                iter.remove();
            }
        }
        fireTableDataChanged();
    }

    public void serviceResolved(ServiceEvent event) {
        deviceList.add(event.getInfo());
        fireTableDataChanged();
    }

    public void setZeroConfPreferenceModel(
            ZeroConfPreferenceModel zeroConfPreferenceModel) {
        this.zeroConfPreferenceModel = zeroConfPreferenceModel;
    }

    public String getColumnName(int column) {
        switch(column) {
        case 0:
                return "ZeroConf name";
        case 1:
                return "Address:Port";
        case 2:
                return "Auto-connect";
        case 3:
                return "Connection Status";
            default:
                    return "";
        }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 2;
    }

    public Class getColumnClass(int columnIndex) {
        switch(columnIndex) {
        case 2:
               return Boolean.class;
        default:
            return super.getColumnClass(columnIndex);
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if(columnIndex!=2 || !(aValue instanceof Boolean))  {
            return;
        }
        boolean autoConnect = ((Boolean)aValue).booleanValue();
        Object device = this.deviceList.get(rowIndex);
        String autoConnectHandle = getAutoConnectHandle((ServiceInfo) device);
        if(autoConnect) {
            zeroConfPreferenceModel.getAutoConnectDevices().add(autoConnectHandle);
        }else {
            zeroConfPreferenceModel.getAutoConnectDevices().remove(autoConnectHandle);
        }
        fireTableDataChanged();
    }
    
    void setZeroConfPluginParent(ZeroConfPlugin parent) {
        this.plugin = parent;
    }


}
