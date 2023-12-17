package org.apache.log4j.chainsaw;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.receivers.ReceiversPanel;

import javax.swing.*;
import java.util.List;

public class LogUiReceiversPanel {

    private ReceiversPanel receiversPanel;
    private JSplitPane mainReceiverSplitPane;

    private int dividerSize;
    private static final double DEFAULT_MAIN_RECEIVER_SPLIT_LOCATION = 0.85d;
    private double lastMainReceiverSplitLocation = DEFAULT_MAIN_RECEIVER_SPLIT_LOCATION;
    public LogUiReceiversPanel(SettingsManager settingsManager, List<ChainsawReceiver> receivers, LogUI logUI, ChainsawStatusBar statusBar, JPanel panePanel) {
        receiversPanel = new ReceiversPanel(settingsManager, receivers, logUI, statusBar);
        mainReceiverSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panePanel, receiversPanel);
        mainReceiverSplitPane.setContinuousLayout(true);
        dividerSize = mainReceiverSplitPane.getDividerSize();
        mainReceiverSplitPane.setDividerLocation(-1);
        mainReceiverSplitPane.setResizeWeight(1.0);

        AbstractConfiguration configuration = settingsManager.getGlobalConfiguration();
        boolean showReceivers = configuration.getBoolean("showReceivers", false);

        configuration.addEventListener(ConfigurationEvent.SET_PROPERTY,
            evt -> {
                if (evt.getPropertyName().equals("showReceivers")) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    if (value) {
                        showReceiverPanel();
                    } else {
                        hideReceiverPanel();
                    }
                }
            });

        if (showReceivers) {
            showReceiverPanel();
        } else {
            hideReceiverPanel();
        }
    }


    public JSplitPane getMainReceiverSplitPane() {
        return mainReceiverSplitPane;
    }

    /**
     * Display the log tree pane, using the last known divider location
     */
    public void showReceiverPanel() {
        mainReceiverSplitPane.setDividerSize(dividerSize);
        mainReceiverSplitPane.setDividerLocation(lastMainReceiverSplitLocation);
        receiversPanel.setVisible(true);
        mainReceiverSplitPane.repaint();
    }

    /**
     * Hide the log tree pane, holding the current divider location for later use
     */
    public void hideReceiverPanel() {
        //subtract one to make sizes match
        int currentSize = mainReceiverSplitPane.getWidth() - mainReceiverSplitPane.getDividerSize();
        if (mainReceiverSplitPane.getDividerLocation() > -1) {
            if (!(((mainReceiverSplitPane.getDividerLocation() + 1) == currentSize)
                || ((mainReceiverSplitPane.getDividerLocation() - 1) == 0))) {
                lastMainReceiverSplitLocation = ((double) mainReceiverSplitPane
                    .getDividerLocation() / currentSize);
            }
        }
        mainReceiverSplitPane.setDividerSize(0);
        receiversPanel.setVisible(false);
        mainReceiverSplitPane.repaint();
    }
}
