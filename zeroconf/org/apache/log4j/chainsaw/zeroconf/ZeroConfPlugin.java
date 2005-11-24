package org.apache.log4j.chainsaw.zeroconf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.plugins.GUIPluginSkeleton;
import org.apache.log4j.net.ZeroConfSocketHubAppender;
import org.apache.log4j.net.ZeroConfSocketHubAppenderTestBed;
import org.apache.log4j.net.Zeroconf4log4j;
import org.apache.log4j.xml.Log4jEntityResolver;

/**
 * This plugin is designed to detect specific Zeroconf zones (Rendevouz/Bonjour,
 * whatever people are calling it) and allow the user to double click on
 * 'devices' to try and connect to them with no configuration needed.
 * 
 * TODO add autoConnect visuals, and save it in a model TODO need to handle
 * NON-log4j devices that may be broadcast in the interested zones TODO add the
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

    private DefaultListModel discoveredDevices = new DefaultListModel();

    private final JList listBox = new JList(discoveredDevices);

    private final JScrollPane scrollPane = new JScrollPane(listBox);

    private JmDNS jmDNS;

    public ZeroConfPlugin() {
        setName("Zeroconf");
    }

    public void shutdown() {
        Zeroconf4log4j.shutdown();
    }

    public void activateOptions() {
        setLayout(new BorderLayout());
        jmDNS = Zeroconf4log4j.getInstance();

        jmDNS.addServiceListener(
                ZeroConfSocketHubAppender.DEFAULT_ZEROCONF_ZONE,
                new ZeroConfServiceListener());

        listBox.setCellRenderer(new ServiceInfoListCellRenderer());
        listBox.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listBox.setVisibleRowCount(-1);
        listBox.addMouseListener(new ConnectorMouseListener());
        add(scrollPane, BorderLayout.CENTER);

    }

    private void deviceDiscovered(ServiceInfo info) {
        String name = info.getName();
        for (int i = 0; i < discoveredDevices.getSize(); i++) {
            if (name.compareToIgnoreCase(((ServiceInfo) discoveredDevices
                    .elementAt(i)).getName()) < 0) {
                discoveredDevices.insertElementAt(name, i);
                return;
            }
        }
        discoveredDevices.addElement(info);
    }

    private class ZeroConfServiceListener implements ServiceListener {

        public void serviceAdded(final ServiceEvent event) {
            LOG.info("Service Added: " + event);
            /**
             * TODO it's not very clear whether we should do the resolving in a
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
        }

        public void serviceResolved(ServiceEvent event) {
            LOG.info("Service Resolved: " + event);
            deviceDiscovered(event.getInfo());
        }

    }

    private static class ServiceInfoListCellRenderer implements
            ListCellRenderer {

        private JPanel panel = new JPanel(new BorderLayout(15, 15));

        private JLabel iconLabel = new JLabel(new ImageIcon(
                ChainsawIcons.ANIM_RADIO_TOWER));

        private JLabel nameLabel = new JLabel();

        private JLabel detailLabel = new JLabel();

        private JCheckBox autoConnect = new JCheckBox();

        private ServiceInfoListCellRenderer() {
            Font font = nameLabel.getFont();
            font = font.deriveFont(font.getSize() + 6);
            nameLabel.setFont(font);
            panel.setLayout(new BorderLayout());
            panel.add(iconLabel, BorderLayout.WEST);

            // TODO scale the icon a bit more, it's a touch small

            JPanel centerPanel = new JPanel(new BorderLayout(3, 3));

            centerPanel.add(nameLabel, BorderLayout.CENTER);
            centerPanel.add(detailLabel, BorderLayout.SOUTH);
            panel.add(centerPanel, BorderLayout.CENTER);
            // TODO add autoconnect label

            // TODO border etching?
            panel.setBorder(BorderFactory.createEtchedBorder());

        }

        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                panel.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                panel.setForeground(list.getForeground());
            }
            ServiceInfo info = (ServiceInfo) value;
            nameLabel.setText(info.getName());
            detailLabel.setText(info.getHostAddress() + ":" + info.getPort());
            return panel;
        }

    }

    private class ConnectorMouseListener extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int index = listBox.locationToIndex(e.getPoint());
                ListModel dlm = discoveredDevices;
                ServiceInfo info = (ServiceInfo) dlm.getElementAt(index);
                listBox.ensureIndexIsVisible(index);
                LOG.info("Connection fequest for " + info);
//                TODO actually do the connection, and 'remember' the connection so we don't do it again.
            }
        }

    }

    public static void main(String[] args) throws InterruptedException {

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();

        final ZeroConfPlugin plugin = new ZeroConfPlugin();

        plugin.activateOptions();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(plugin, BorderLayout.CENTER);

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
