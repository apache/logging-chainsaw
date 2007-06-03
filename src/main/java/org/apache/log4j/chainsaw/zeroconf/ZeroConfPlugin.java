package org.apache.log4j.chainsaw.zeroconf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.SmallButton;
import org.apache.log4j.chainsaw.help.HelpManager;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.plugins.GUIPluginSkeleton;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.net.SocketHubReceiver;
import org.apache.log4j.net.ZeroConfSocketHubAppender;
import org.apache.log4j.net.Zeroconf4log4j;
import org.apache.log4j.plugins.Plugin;
import org.apache.log4j.plugins.PluginEvent;
import org.apache.log4j.plugins.PluginListener;
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
 * To run this in trial mode, first run {@link ZeroConfSocketHubAppenderTestBed}, then
 * run this class' main(..) method.
 * 
 * @author psmith
 * 
 */
public class ZeroConfPlugin extends GUIPluginSkeleton {

    private static final Logger LOG = Logger.getLogger(ZeroConfPlugin.class);

    private static final Icon DEVICE_DISCOVERED_ICON = new ImageIcon(ChainsawIcons.ANIM_RADIO_TOWER);

    private ZeroConfDeviceModel discoveredDevices = new ZeroConfDeviceModel();

    private JTable deviceTable = new JTable(discoveredDevices);
    
    private final JScrollPane scrollPane = new JScrollPane(deviceTable);

    private JmDNS jmDNS;

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
    
    public ZeroConfPlugin() {
        setName("Zeroconf");
    }

    public void shutdown() {
        Zeroconf4log4j.shutdown();
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
        jmDNS = Zeroconf4log4j.getInstance();

        jmDNS.addServiceListener(
                ZeroConfSocketHubAppender.DEFAULT_ZEROCONF_ZONE,
                new ZeroConfServiceListener());

        jmDNS.addServiceListener(ZeroConfSocketHubAppender.DEFAULT_ZEROCONF_ZONE, discoveredDevices);
        
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
    
    /**
     * Sets the icon of this parent container (a JTabbedPane, we hope
     *
     */
    private void setIconIfNeeded() {
        Container container = this.getParent();
        if(container instanceof JTabbedPane) {
            JTabbedPane tabbedPane = (JTabbedPane) container;
            Icon icon = discoveredDevices.getRowCount()==0?null:DEVICE_DISCOVERED_ICON;
            tabbedPane.setIconAt(tabbedPane.indexOfTab(getName()), icon);
        }else {
            LOG.warn("Parent is not a TabbedPane, not setting icon: " + container.getClass().getName());
        }
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
        if (preferenceModel.getAutoConnectDevices().contains(name)) {
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
        int port = info.getPort();
        String hostAddress = info.getHostAddress();
       
//        TODO handle different receivers than just SocketHubReceiver
        SocketHubReceiver receiver = new SocketHubReceiver();
        receiver.setHost(hostAddress);
        receiver.setPort(port);
        receiver.setName(info.getName());
        
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
