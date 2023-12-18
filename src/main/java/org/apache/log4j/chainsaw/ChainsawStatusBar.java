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

package org.apache.log4j.chainsaw;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.logui.LogUI;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.text.NumberFormat;


/**
 * A general purpose status bar for all Frame windows
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ChainsawStatusBar extends JPanel {
    private static final int DELAY_PERIOD = 5000;
    private static final String DEFAULT_MSG = "Welcome to Chainsaw v2!";
    private final JLabel statusMsg = new JLabel(DEFAULT_MSG);
    private final JLabel searchMatchLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel pausedLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel lineSelectionLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel eventCountLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel receivedEventLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel receivedConnectionlabel = new JLabel("", SwingConstants.CENTER);
    private volatile long lastReceivedConnection = System.currentTimeMillis();
    private final Thread connectionThread;
    private final Icon pausedIcon = new ImageIcon(ChainsawIcons.PAUSE);
    private final Icon netConnectIcon =
        new ImageIcon(ChainsawIcons.ANIM_NET_CONNECT);
    private final NumberFormat nf = NumberFormat.getNumberInstance();
    private final Border statusBarComponentBorder =
        BorderFactory.createLineBorder(statusMsg.getBackground().darker());
    private final LogUI logUI;

    public ChainsawStatusBar(LogUI logUI, ApplicationPreferenceModel applicationPreferenceModel) {
        setLayout(new GridBagLayout());
        this.logUI = logUI;

        applicationPreferenceModel.addEventListener(
            evt -> {
                if (evt.getPropertyName().equals(ApplicationPreferenceModel.STATUS_BAR_VISIBLE)) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    setVisible(value);
                }
            });

        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        nf.setGroupingUsed(false);

        JPanel statusMsgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        statusMsgPanel.add(statusMsg);
        statusMsgPanel.setBorder(statusBarComponentBorder);

        pausedLabel.setBorder(statusBarComponentBorder);
        pausedLabel.setMinimumSize(
            new Dimension(pausedIcon.getIconWidth(), pausedIcon.getIconHeight()));

        pausedLabel.setToolTipText(
            "Shows whether the current Log panel is paused or not");

        receivedEventLabel.setBorder(statusBarComponentBorder);
        receivedEventLabel.setToolTipText(
            "Indicates whether Chainsaw is receiving events, and how fast it is processing them");
        receivedEventLabel.setMinimumSize(
            new Dimension(
                receivedEventLabel.getFontMetrics(receivedEventLabel.getFont())
                    .stringWidth("99999999999.9/s") + 5,
                (int) receivedEventLabel.getPreferredSize().getHeight()));

        eventCountLabel.setBorder(statusBarComponentBorder);
        eventCountLabel.setToolTipText("<# viewable events>:<# total events>");
        eventCountLabel.setMinimumSize(
            new Dimension(
                eventCountLabel.getFontMetrics(eventCountLabel.getFont())
                    .stringWidth("Filtered/Total: 999999999999:999999999999") + 5,
                (int) eventCountLabel.getPreferredSize().getHeight()));

        searchMatchLabel.setBorder(statusBarComponentBorder);
        searchMatchLabel.setToolTipText("<# viewable events>:<# total events>");
        searchMatchLabel.setMinimumSize(
            new Dimension(
                searchMatchLabel.getFontMetrics(eventCountLabel.getFont()).stringWidth("Find matches: 999999999999") + 5,
                (int) searchMatchLabel.getPreferredSize().getHeight()));

        receivedConnectionlabel.setBorder(statusBarComponentBorder);
        receivedConnectionlabel.setToolTipText(
            "Indicates whether Chainsaw has received a remote connection");
        receivedConnectionlabel.setMinimumSize(
            new Dimension(
                netConnectIcon.getIconWidth() + 4,
                (int) receivedConnectionlabel.getPreferredSize().getHeight()));

        lineSelectionLabel.setBorder(statusBarComponentBorder);
        lineSelectionLabel.setMinimumSize(
            new Dimension(
                lineSelectionLabel.getFontMetrics(lineSelectionLabel.getFont())
                    .stringWidth("999999999"),
                (int) lineSelectionLabel.getPreferredSize().getHeight()));
        lineSelectionLabel.setToolTipText(
            "The current line # selected");

        JComponent[] toFix =
            new JComponent[]{
                searchMatchLabel, eventCountLabel,
                receivedConnectionlabel, lineSelectionLabel, receivedEventLabel,
                pausedLabel
            };

        for (JComponent aToFix : toFix) {
            aToFix.setPreferredSize(aToFix.getMinimumSize());
            aToFix.setMaximumSize(aToFix.getMinimumSize());
        }

        statusMsg.setMinimumSize(pausedLabel.getPreferredSize());
        statusMsg.setToolTipText("Shows messages from Chainsaw");

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.ipadx = 2;
        c.ipady = 2;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;

        add(statusMsgPanel, c);

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 1;
        add(receivedConnectionlabel, c);

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 2;
        add(lineSelectionLabel, c);

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 3;
        add(searchMatchLabel, c);

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 4;
        add(eventCountLabel, c);

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 5;
        add(receivedEventLabel, c);

        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridx = 6;

        add(pausedLabel, c);

        connectionThread =
            new Thread(
                () -> {
                    while (true) {
                        try {
                            Thread.sleep(DELAY_PERIOD);
                        } catch (InterruptedException e) {
                        }

                        Icon icon = null;

                        if (
                            (System.currentTimeMillis() - lastReceivedConnection) < DELAY_PERIOD) {
                            icon = netConnectIcon;
                        }

                        final Icon theIcon = icon;
                        SwingUtilities.invokeLater(
                            () -> receivedConnectionlabel.setIcon(theIcon));
                    }
                });
        connectionThread.start();
    }

    void setDataRate(final double dataRate) {
        SwingUtilities.invokeLater(
            () -> receivedEventLabel.setText(nf.format(dataRate) + "/s"));
    }

    /**
     * Indicates a new connection has been established between
     * Chainsaw and some remote host
     *
     * @param source
     */
    void remoteConnectionReceived(String source) {
        lastReceivedConnection = System.currentTimeMillis();
        setMessage("Connection received from " + source);
        connectionThread.interrupt();

        //    TODO and maybe play a sound?
    }

    /**
     * Called when the paused state of the LogPanel has been updated
     *
     * @param isPaused
     * @param tabName
     */
    void setPaused(final boolean isPaused, String tabName) {
        if (tabName.equals(logUI.getActiveTabName())) {
            Runnable runnable =
                () -> {
                    pausedLabel.setIcon(isPaused ? pausedIcon : null);
                    pausedLabel.setToolTipText(
                        isPaused ? "This Log panel is currently paused"
                            : "This Log panel is not paused");
                };
            SwingUtilities.invokeLater(runnable);
        }
    }

    public void setSelectedLine(
        final int selectedLine, final int lineCount, final int total, String tabName) {
        if (tabName.equals(logUI.getActiveTabName())) {
            SwingUtilities.invokeLater(
                () -> {
                    lineSelectionLabel.setText(selectedLine + "");
                    eventCountLabel.setText("Filtered/Total: " + lineCount + ":" + total);
                });
        }
    }

    public void setSearchMatchCount(int searchMatchCount, String tabName) {
        if (tabName.equals(logUI.getActiveTabName())) {
            if (searchMatchCount == 0) {
                searchMatchLabel.setText("");
            } else {
                searchMatchLabel.setText("Find matches: " + searchMatchCount);
            }
        }
    }

    public void setNothingSelected() {
        SwingUtilities.invokeLater(
            () -> lineSelectionLabel.setText(""));
    }

    void clear() {
        setMessage(DEFAULT_MSG);
        setNothingSelected();
        SwingUtilities.invokeLater(
            () -> receivedEventLabel.setText(""));
    }

    public void setMessage(final String msg) {
        SwingUtilities.invokeLater(
            () -> statusMsg.setText(" " + msg));
    }
}
