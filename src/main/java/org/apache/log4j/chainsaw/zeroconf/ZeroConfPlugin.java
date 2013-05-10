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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.LogFilePatternLayoutBuilder;
import org.apache.log4j.chainsaw.SmallButton;
import org.apache.log4j.chainsaw.help.HelpManager;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.plugins.GUIPluginSkeleton;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.vfs.VFSLogFilePatternReceiver;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.net.MulticastReceiver;
import org.apache.log4j.net.SocketHubReceiver;
import org.apache.log4j.net.SocketReceiver;
import org.apache.log4j.net.UDPReceiver;
import org.apache.log4j.net.XMLSocketReceiver;
import org.apache.log4j.net.ZeroConfSupport;
import org.apache.log4j.plugins.Plugin;
import org.apache.log4j.plugins.PluginEvent;
import org.apache.log4j.plugins.PluginListener;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LoggerRepositoryEx;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * This plugin is designed to detect specific Zeroconf zones (Rendevouz/Bonjour,
 * whatever people are calling it) and allow the user to double click on
 * 'devices' to try and connect to them with no configuration needed.
 * 
 * TODO need to handle
 * NON-log4j devices that may be broadcast in the interested zones 
 * TODO add the
 * default Zone, and the list of user-specified zones to a preferenceModel
 * 
 * @author psmith
 * 
 */
public class ZeroConfPlugin extends GUIPluginSkeleton {

    private static final Logger LOG = Logger.getLogger(ZeroConfPlugin.class);

    private ZeroConfDeviceModel discoveredDevices = new ZeroConfDeviceModel();

    private JTable deviceTable = new JTable(discoveredDevices);
    
    private final JScrollPane scrollPane = new JScrollPane(deviceTable);

    private ZeroConfPreferenceModel preferenceModel;
    
    private Map serviceInfoToReceiveMap = new HashMap();

    private JMenu connectToMenu = new JMenu("Connect to");
    private JMenuItem helpItem = new JMenuItem(new AbstractAction("Learn more about ZeroConf...",
            ChainsawIcons.ICON_HELP) {

        public void actionPerformed(ActionEvent e) {
            HelpManager.getInstance()
                    .showHelpForClass(ZeroConfPlugin.class);
        }
    });  
    
    private JMenuItem nothingToConnectTo = new JMenuItem("No devices discovered");
    private static final String MULTICAST_APPENDER_SERVICE_NAME = "_log4j_xml_mcast_appender.local.";
    private static final String UDP_APPENDER_SERVICE_NAME = "_log4j_xml_udp_appender.local.";
    private static final String XML_SOCKET_APPENDER_SERVICE_NAME = "_log4j_xml_tcpconnect_appender.local.";
    private static final String SOCKET_APPENDER_SERVICE_NAME = "_log4j_obj_tcpconnect_appender.local.";
    private static final String SOCKETHUB_APPENDER_SERVICE_NAME = "_log4j_obj_tcpaccept_appender.local.";
    private static final String TCP_APPENDER_SERVICE_NAME = "_log4j._tcp.local.";
    private static final String NEW_UDP_APPENDER_SERVICE_NAME = "_log4j._udp.local.";

    private JmDNS jmDNS;

    public ZeroConfPlugin() {
        setName("Zeroconf");
        deviceTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
    }

    public void shutdown() {
        if (jmDNS != null) {
            try {
                jmDNS.close();
            } catch (Exception e) {
                LOG.error("Unable to close JMDNS", e);
            }
        }
        save();
    }

    private void save() {
        File fileLocation = getPreferenceFileLocation();
        XStream stream = new XStream(new DomDriver());
        try {
            stream.toXML(preferenceModel, new FileWriter(fileLocation));
        } catch (Exception e) {
            LOG.error("Failed to save ZeroConfPlugin configuration file",e);
        }
    }

    private File getPreferenceFileLocation() {
        return new File(SettingsManager.getInstance().getSettingsDirectory(), "zeroconfprefs.xml");
    }

    public void activateOptions() {
        setLayout(new BorderLayout());
        jmDNS = (JmDNS) ZeroConfSupport.getJMDNSInstance();

        registerServiceListenersForAppenders();

        deviceTable.addMouseListener(new ConnectorMouseListener());

        
        JToolBar toolbar = new JToolBar();
        SmallButton helpButton = new SmallButton(helpItem.getAction());
        helpButton.setText(helpItem.getText());
        toolbar.add(helpButton);
        toolbar.setFloatable(false);
        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        injectMenu();
        
        ((LoggerRepositoryEx)LogManager.getLoggerRepository()).getPluginRegistry().addPluginListener(new PluginListener() {

            public void pluginStarted(PluginEvent e) {
                
            }

            public void pluginStopped(PluginEvent e) {
                Plugin plugin = e.getPlugin();
                synchronized(serviceInfoToReceiveMap) {
                    for (Iterator iter = serviceInfoToReceiveMap.entrySet().iterator(); iter.hasNext();) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        if(entry.getValue() == plugin) {
                                iter.remove();
                        }
                    }
                }
//                 need to make sure that the menu item tracking this item has it's icon and enabled state updade
                discoveredDevices.fireTableDataChanged();
            }});

        File fileLocation = getPreferenceFileLocation();
        XStream stream = new XStream(new DomDriver());
        if (fileLocation.exists()) {
            try {
                this.preferenceModel = (ZeroConfPreferenceModel) stream
                        .fromXML(new FileReader(fileLocation));
            } catch (Exception e) {
                LOG.error("Failed to load ZeroConfPlugin configuration file",e);
            }
        }else {
            this.preferenceModel = new ZeroConfPreferenceModel();
        }
        discoveredDevices.setZeroConfPreferenceModel(preferenceModel);
        discoveredDevices.setZeroConfPluginParent(this);
    }

    private void registerServiceListenersForAppenders()
    {
        Set serviceNames = new HashSet();
        serviceNames.add(MULTICAST_APPENDER_SERVICE_NAME);
        serviceNames.add(SOCKET_APPENDER_SERVICE_NAME);
        serviceNames.add(SOCKETHUB_APPENDER_SERVICE_NAME);
        serviceNames.add(UDP_APPENDER_SERVICE_NAME);
        serviceNames.add(XML_SOCKET_APPENDER_SERVICE_NAME);
        serviceNames.add(TCP_APPENDER_SERVICE_NAME);
        serviceNames.add(NEW_UDP_APPENDER_SERVICE_NAME);

        for (Iterator iter = serviceNames.iterator(); iter.hasNext();) {
            String serviceName = iter.next().toString();
            jmDNS.addServiceListener(
                    serviceName,
                    new ZeroConfServiceListener());

            jmDNS.addServiceListener(serviceName, discoveredDevices);
        }

        //now add each appender constant
    }

    /**
     * Attempts to find a JFrame container as a parent,and addse a "Connect to" menu
     *
     */
    private void injectMenu() {
        
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if(frame == null) {
            LOG.info("Could not locate parent JFrame to add menu to");
        }else {
            JMenuBar menuBar = frame.getJMenuBar();
            if(menuBar==null ) {
                menuBar = new JMenuBar();
                frame.setJMenuBar(menuBar);
            }
            insertToLeftOfHelp(menuBar, connectToMenu);
            connectToMenu.add(nothingToConnectTo);
            
            discoveredDevices.addTableModelListener(new TableModelListener (){

                public void tableChanged(TableModelEvent e) {
                    if(discoveredDevices.getRowCount()==0) {
                        connectToMenu.add(nothingToConnectTo,0);
                    }else if(discoveredDevices.getRowCount()>0) {
                        connectToMenu.remove(nothingToConnectTo);
                    }
                    
                }});
            
            nothingToConnectTo.setEnabled(false);

            connectToMenu.addSeparator();
            connectToMenu.add(helpItem);
        }
    }

    /**
     * Hack method to locate the JMenu that is the Help menu, and inserts the new menu
     * just to the left of it.
     * @param menuBar
     * @param item
     */
    private void insertToLeftOfHelp(JMenuBar menuBar, JMenu item) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if(menu.getText().equalsIgnoreCase("help")) {
                menuBar.add(item, i-1);
            }
        }
        LOG.warn("menu '" + item.getText() + "' was NOT added because the 'Help' menu could not be located");
    }

    /**
     * When a device is discovered, we create a menu item for it so it can be connected to via that
     * GUI mechanism, and also if the device is one of the auto-connect devices then a background thread
     * is created to connect the device.
     * @param info
     */
    private void deviceDiscovered(final ServiceInfo info) {
        final String name = info.getName();
//        TODO currently adding ALL devices to autoConnectlist
//        preferenceModel.addAutoConnectDevice(name);
        
        
        JMenuItem connectToDeviceMenuItem = new JMenuItem(new AbstractAction(info.getName()) {

            public void actionPerformed(ActionEvent e) {
                connectTo(info);
            }});
        
        if(discoveredDevices.getRowCount()>0) {
            Component[] menuComponents = connectToMenu.getMenuComponents();
            boolean located = false;
            for (int i = 0; i < menuComponents.length; i++) {
                Component c = menuComponents[i];
                if (!(c instanceof JPopupMenu.Separator)) {
                    JMenuItem item = (JMenuItem) menuComponents[i];
                    if (item.getText().compareToIgnoreCase(name) < 0) {
                        connectToMenu.insert(connectToDeviceMenuItem, i);
                        located = true;
                        break;
                    }
                }
            }
            if(!located) {
                connectToMenu.insert(connectToDeviceMenuItem,0);
            }
        }else {
            connectToMenu.insert(connectToDeviceMenuItem,0);
        }
//         if the device name is one of the autoconnect devices, then connect immediately
        if (preferenceModel != null && preferenceModel.getAutoConnectDevices() != null && preferenceModel.getAutoConnectDevices().contains(name)) {
            new Thread(new Runnable() {

                public void run() {
                    LOG.info("Auto-connecting to " + name);
                    connectTo(info);
                }
            }).start();
        }
    }
    
    /**
     * When a device is removed or disappears we need to remove any JMenu item associated with it.
     * @param name
     */
    private void deviceRemoved(String name) {
        Component[] menuComponents = connectToMenu.getMenuComponents();
        for (int i = 0; i < menuComponents.length; i++) {
            Component c = menuComponents[i];
            if (!(c instanceof JPopupMenu.Separator)) {
                JMenuItem item = (JMenuItem) menuComponents[i];
                if (item.getText().compareToIgnoreCase(name) == 0) {
                    connectToMenu.remove(item);
                    break;
                }
            }
        }
    }
        
    /**
     * Listens out on the JmDNS/ZeroConf network for new devices that appear
     * and adds/removes these device information from the list/model.
     *
     */
    private class ZeroConfServiceListener implements ServiceListener {

        public void serviceAdded(final ServiceEvent event) {
            LOG.info("Service Added: " + event);
            /**
             * it's not very clear whether we should do the resolving in a
             * background thread or not.. All it says is to NOT do it in the AWT
             * thread, so I'm thinking it probably should be a background thread
             */
            Runnable runnable = new Runnable() {
                public void run() {
                    ZeroConfPlugin.this.jmDNS.requestServiceInfo(event
                            .getType(), event.getName());
                }
            };
            Thread thread = new Thread(runnable,
                    "ChainsawZeroConfRequestResolutionThread");
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        public void serviceRemoved(ServiceEvent event) {
            LOG.info("Service Removed: " + event);
            deviceRemoved(event.getName());
        }

        public void serviceResolved(ServiceEvent event) {
            LOG.info("Service Resolved: " + event);
            deviceDiscovered(event.getInfo());
        }

    }


    /**
     * When the user double clicks on a row, then the device is connected to,
     * the only exception is when clicking in the check box column for auto connect.
     */
    private class ConnectorMouseListener extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = deviceTable.rowAtPoint(e.getPoint());
                if(deviceTable.columnAtPoint(e.getPoint())==2) {
                    return;
                }
                ServiceInfo info = discoveredDevices.getServiceInfoAtRow(row);
                
                if (!isConnectedTo(info)) {
                    connectTo(info);
                } else {
                    disconnectFrom(info);
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            /**
             * This methodh handles when the user clicks the
             * auto-connect
             */
//            int index = listBox.locationToIndex(e.getPoint());
//
//            if (index != -1) {
////                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), )
//                Component c = SwingUtilities.getDeepestComponentAt(ZeroConfPlugin.this, e.getX(), e.getY());
//                if (c instanceof JCheckBox) {
//                    ServiceInfo info = (ServiceInfo) listBox.getModel()
//                            .getElementAt(index);
//                    String name = info.getName();
//                    if (preferenceModel.getAutoConnectDevices().contains(name)) {
//                        preferenceModel.removeAutoConnectDevice(name);
//                    } else {
//                        preferenceModel.addAutoConnectDevice(name);
//                    }
//                    discoveredDevices.fireContentsChanged();
//                    repaint();
//                }
//            }
        }
    }

    private void disconnectFrom(ServiceInfo info) {
        if(!isConnectedTo(info)) {
            return; // not connected, who cares
        }
        Plugin plugin;
        synchronized (serviceInfoToReceiveMap) {
            plugin = (Plugin) serviceInfoToReceiveMap.get(info);
        }
        ((LoggerRepositoryEx)LogManager.getLoggerRepository()).getPluginRegistry().stopPlugin(plugin.getName());
        
        JMenuItem item = locateMatchingMenuItem(info.getName());
        if (item!=null) {
            item.setIcon(null);
            item.setEnabled(true);
        }
    }
    /**
     * returns true if the serviceInfo record already has a matching connected receiver
     * @param info
     * @return
     */
    boolean isConnectedTo(ServiceInfo info) {
        return serviceInfoToReceiveMap.containsKey(info);
    }
    /**
     * Starts a receiver to the appender referenced within the ServiceInfo
     * @param info
     */
    private void connectTo(ServiceInfo info) {
        LOG.info("Connection request for " + info);
        //Chainsaw can construct receivers from discovered appenders
        Receiver receiver = getReceiver(info);
        //if null, unable to resolve the service name..no-op
        if (receiver == null) {
            return;
        }
        ((LoggerRepositoryEx)LogManager.getLoggerRepository()).getPluginRegistry().addPlugin(receiver);
        receiver.activateOptions();
        LOG.info("Receiver '" + receiver.getName() + "' has been started");
        
        // ServiceInfo obeys equals() and hashCode() contracts, so this should be safe.
        synchronized (serviceInfoToReceiveMap) {
            serviceInfoToReceiveMap.put(info, receiver);
        }
        
//         this instance of the menu item needs to be disabled, and have an icon added
        JMenuItem item = locateMatchingMenuItem(info.getName());
        if (item!=null) {
            item.setIcon(new ImageIcon(ChainsawIcons.ANIM_NET_CONNECT));
            item.setEnabled(false);
        }
//        // now notify the list model has changed, it needs redrawing of the receiver icon now it's connected
//        discoveredDevices.fireContentsChanged();
    }

    private Receiver getReceiver(ServiceInfo info) {
        String zone = info.getType();
        int port = info.getPort();
        String hostAddress = info.getHostAddress();
        String name = info.getName();
        String decoderClass = info.getPropertyString("decoder");

        if (NEW_UDP_APPENDER_SERVICE_NAME.equals(zone))
        {
            UDPReceiver receiver = new UDPReceiver();
            receiver.setPort(port);
            receiver.setName(name + "-receiver");
            return receiver;
        }
        //FileAppender or socketappender
        //TODO: add more checks (actual layout format, etc)
        if (TCP_APPENDER_SERVICE_NAME.equals(zone)) {
            //CHECK content type
            //application/octet-stream = SocketReceiver
            //text/plain = VFSLogFilePatternReceiver (if structured=false)
            String contentType = info.getPropertyString("contentType").toLowerCase();
            //won't work with log4j2, as Chainsaw depends on log4j1.x
            if ("application/octet-stream".equals(contentType))
            {
                SocketReceiver receiver = new SocketReceiver();
                receiver.setPort(port);
                receiver.setName(name + "-receiver");
                return receiver;
            }
            //this will work - regular text log files are fine
            if ("text/plain".equals(contentType))
            {
                VFSLogFilePatternReceiver receiver = new VFSLogFilePatternReceiver();
                receiver.setAppendNonMatches(true);
                receiver.setFileURL(info.getPropertyString("fileURI"));
                receiver.setLogFormat(LogFilePatternLayoutBuilder.getLogFormatFromPatternLayout(info.getPropertyString("format")));
                receiver.setTimestampFormat(LogFilePatternLayoutBuilder.getTimeStampFormat(info.getPropertyString("format")));
                receiver.setName(name + "-receiver");
                receiver.setTailing(true);
                return receiver;
            }
        }

        //MulticastAppender
        if (MULTICAST_APPENDER_SERVICE_NAME.equals(zone)) {
            MulticastReceiver receiver = new MulticastReceiver();
            //this needs to be a multicast address, not the host address, so we need to use a property
            receiver.setAddress(info.getPropertyString("multicastAddress"));
            receiver.setPort(port);
            receiver.setName(name + "-receiver");
            if (decoderClass != null && !decoderClass.equals("")) {
                receiver.setDecoder(decoderClass);
            }

            return receiver;
        }
        //UDPAppender
        if (UDP_APPENDER_SERVICE_NAME.equals(zone)) {
            UDPReceiver receiver = new UDPReceiver();
            receiver.setPort(port);
            receiver.setName(name + "-receiver");
            if (decoderClass != null && !decoderClass.equals("")) {
                receiver.setDecoder(decoderClass);
            }
            return receiver;
        }

        //non-log4j XML-based socketappender
        if (XML_SOCKET_APPENDER_SERVICE_NAME.equals(zone)) {
            XMLSocketReceiver receiver = new XMLSocketReceiver();
            receiver.setPort(port);
            receiver.setName(name + "-receiver");
            if (decoderClass != null && !decoderClass.equals("")) {
                receiver.setDecoder(decoderClass);
            }
            return receiver;
        }

        //SocketAppender
        if (SOCKET_APPENDER_SERVICE_NAME.equals(zone)) {
            SocketReceiver receiver = new SocketReceiver();
            receiver.setPort(port);
            receiver.setName(name + "-receiver");
            return receiver;
        }

        //SocketHubAppender
        if (SOCKETHUB_APPENDER_SERVICE_NAME.equals(zone)) {
            SocketHubReceiver receiver = new SocketHubReceiver();
            receiver.setHost(hostAddress);
            receiver.setPort(port);
            receiver.setName(name + "-receiver");
            return receiver;
        }
        //not recognized
        LogLog.debug("Unable to find receiver for appender with service name: " + zone);
        return null;
    }

    /**
     * Finds the matching JMenuItem based on name, may return null if there is no match.
     * 
     * @param name
     * @return
     */
    private JMenuItem locateMatchingMenuItem(String name) {
        Component[] menuComponents = connectToMenu.getMenuComponents();
        for (int i = 0; i < menuComponents.length; i++) {
            Component c = menuComponents[i];
            if (!(c instanceof JPopupMenu.Separator)) {
                JMenuItem item = (JMenuItem) menuComponents[i];
                if (item.getText().compareToIgnoreCase(name) == 0) {
                    return item;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws InterruptedException {

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();

        final ZeroConfPlugin plugin = new ZeroConfPlugin();


        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(plugin, BorderLayout.CENTER);

        // needs to be activated after being added to the JFrame for Menu injection to work
        plugin.activateOptions();

        frame.pack();
        frame.setVisible(true);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                plugin.shutdown();
            }
        });
        Runtime.getRuntime().addShutdownHook(thread);
    }

}
