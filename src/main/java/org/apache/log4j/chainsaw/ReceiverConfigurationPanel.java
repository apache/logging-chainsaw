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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.net.SocketReceiver;
import org.apache.log4j.net.UDPReceiver;


/**
 * A panel providing receiver configuration options
 *
 * @author Paul Smith
 */
class ReceiverConfigurationPanel extends JPanel {
    private final Logger logger = LogManager.getLogger(ReceiverConfigurationPanel.class);

    private final PanelModel panelModel = new PanelModel();

    //network receiver widgets
    private JComboBox networkReceiverPortComboBox;
    private JComboBox networkReceiverClassNameComboBox;
    private DefaultComboBoxModel networkReceiverClassNameComboBoxModel;
    private DefaultComboBoxModel networkReceiverPortComboBoxModel;

    //logfile receiver widgets
    private JButton browseLogFileButton;
    private JComboBox logFileFormatTypeComboBox;

    private JTextField logFileFormatTextField;
    private JComboBox logFileFormatTimestampFormatComboBox;
    private JTextField logFileURLTextField;
    private DefaultComboBoxModel logFileFormatTimestampFormatComboBoxModel;

    //use existing configuration widgets
    private JButton browseForAnExistingConfigurationButton;
    private DefaultComboBoxModel existingConfigurationComboBoxModel;
    private JComboBox existingConfigurationComboBox;

    //don't warn again widgets
    private JCheckBox dontwarnIfNoReceiver;

    //ok button
    private JButton okButton;

    //radiobutton widgets
    private JRadioButton logFileReceiverRadioButton;
    private JRadioButton networkReceiverRadioButton;
    private JRadioButton doNothingRadioButton;
    private JRadioButton useExistingConfigurationRadioButton;
    private JRadioButton useAutoSavedConfigRadioButton;
    private ButtonGroup buttonGroup;

    ReceiverConfigurationPanel() {
        JPanel topDescriptionPanel = buildTopDescriptionPanel();
        JPanel networkReceiverPanel = buildNetworkReceiverPanel();
        JPanel logFileReceiverPanel = buildLogFileReceiverPanel();
        JPanel useExistingConfigurationPanel = buildUseExistingConfigurationPanel();
        JPanel dontWarnAndOKPanel = buildDontWarnAndOKPanel();
        JPanel bottomDescriptionPanel = buildBottomDescriptionPanel();

        buttonGroup = new ButtonGroup();

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        add(topDescriptionPanel, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        logFileReceiverRadioButton = new JRadioButton("Load events from a regular log file (and continue to tail the file)");
        buttonGroup.add(logFileReceiverRadioButton);
        add(logFileReceiverRadioButton, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 20, 20, 0);
        add(logFileReceiverPanel, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        networkReceiverRadioButton = new JRadioButton("Receive events from the network");
        buttonGroup.add(networkReceiverRadioButton);
        add(networkReceiverRadioButton, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 20, 20, 0);
        add(networkReceiverPanel, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.fill = GridBagConstraints.HORIZONTAL;
        useExistingConfigurationRadioButton = new JRadioButton("Use an existing Chainsaw configuration file...");
        buttonGroup.add(useExistingConfigurationRadioButton);
        add(useExistingConfigurationRadioButton, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 20, 20, 0);
        add(useExistingConfigurationPanel, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 20, 0);
        useAutoSavedConfigRadioButton = new JRadioButton("Use last used/auto-saved configuration from $HOME/.chainsaw/receiver-config.xml");
        buttonGroup.add(useAutoSavedConfigRadioButton);
        add(useAutoSavedConfigRadioButton, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 9;
        c.fill = GridBagConstraints.HORIZONTAL;
        doNothingRadioButton = new JRadioButton("I'm fine thanks, don't worry");
        buttonGroup.add(doNothingRadioButton);
        add(doNothingRadioButton, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 10;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 20, 20, 0);
        add(dontWarnAndOKPanel, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 11;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(bottomDescriptionPanel, c);

        /**
         * This listener activates/deactivates certain controls based on the current
         * state of the options
         */
        ActionListener al = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateEnabledState((Component)e.getSource());
                }
            };

        logFileReceiverRadioButton.addActionListener(al);
        networkReceiverRadioButton.addActionListener(al);
        doNothingRadioButton.addActionListener(al);
        useExistingConfigurationRadioButton.addActionListener(al);
        useAutoSavedConfigRadioButton.addActionListener(al);

        //set 'do nothing' as default
        buttonGroup.setSelected(doNothingRadioButton.getModel(), true);
        updateEnabledState(doNothingRadioButton);
    }

    private JPanel buildTopDescriptionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        JTextPane descriptionTextPane = new JTextPane();

        StyledDocument doc = descriptionTextPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        
        descriptionTextPane.setText("Define an event source or load a configuration file");
        descriptionTextPane.setEditable(false);
        descriptionTextPane.setOpaque(false);
        descriptionTextPane.setFont(getFont());
        panel.add(descriptionTextPane, c);
        return panel;
    }

    private JPanel buildDontWarnAndOKPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.LINE_END;
        dontwarnIfNoReceiver = new JCheckBox("Don't show this again");
        panel.add(dontwarnIfNoReceiver, c);

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        okButton = new JButton(" OK ");
        panel.add(okButton, c);

        return panel;
    }

    private JPanel buildBottomDescriptionPanel() {
        JTextPane descriptionTextPane = new JTextPane();
        StyledDocument doc = descriptionTextPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        descriptionTextPane.setText("The active configuration is auto-saved on exit to $HOME/.chainsaw/receiver-config.xml\n\nAn example configuration file is available from the Welcome tab");
        descriptionTextPane.setEditable(false);
        descriptionTextPane.setOpaque(false);
        descriptionTextPane.setFont(getFont());

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;

        panel.add(descriptionTextPane, c);

        return panel;
    }

    private JPanel buildNetworkReceiverPanel() {
        networkReceiverPortComboBoxModel = new DefaultComboBoxModel();
        networkReceiverPortComboBoxModel.addElement("4445");
        networkReceiverPortComboBoxModel.addElement("4560");

        networkReceiverPortComboBox = new JComboBox(networkReceiverPortComboBoxModel);
        networkReceiverPortComboBox.setEditable(true);
        networkReceiverPortComboBox.setOpaque(false);

        networkReceiverClassNameComboBoxModel = new DefaultComboBoxModel();
        networkReceiverClassNameComboBoxModel.addElement(SocketReceiver.class);
        networkReceiverClassNameComboBoxModel.addElement(UDPReceiver.class);

        networkReceiverClassNameComboBox = new JComboBox(networkReceiverClassNameComboBoxModel);

        networkReceiverClassNameComboBox.setEditable(false);
        networkReceiverClassNameComboBox.setOpaque(false);

        networkReceiverClassNameComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {

                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof Class) {
                    Class receiverClass = (Class) value;
                    JLabel cellLabel = (JLabel) component;
                    String shortenedName = receiverClass.getName().substring(receiverClass.getName().lastIndexOf('.') + 1);
                    cellLabel.setText(shortenedName);
                }

                return component;
            }
        });

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 5);
        panel.add(networkReceiverClassNameComboBox, c);

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(networkReceiverPortComboBox, c);

        return panel;
    }

    private JPanel buildLogFileReceiverPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        browseLogFileButton = new JButton(new AbstractAction(" Find a log file ") {
            public void actionPerformed(ActionEvent e) {
                try {

                    URL url = browseLogFile();
                    if (url != null) {
                        String item = url.toURI().toString();
                        logFileURLTextField.setText(item);
                    }
                } catch (Exception ex) {
                    logger.error(
                        "Error browsing for log file", ex);
                }
            }
        });

        browseLogFileButton.setToolTipText("Shows a File Open dialog to allow you to find a log file");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(browseLogFileButton, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(0, 0, 5, 5);
        panel.add(new JLabel(" Log file URL "), c);
        logFileURLTextField = new JTextField();
        logFileURLTextField.setEditable(true);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(logFileURLTextField, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(0, 0, 5, 5);
        panel.add(new JLabel(" Log file format type "), c);

        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        comboBoxModel.addElement("PatternLayout format");
        comboBoxModel.addElement("LogFilePatternReceiver LogFormat");

        logFileFormatTypeComboBox = new JComboBox(comboBoxModel);
        logFileFormatTypeComboBox.setOpaque(false);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(logFileFormatTypeComboBox, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(0, 5, 5, 5);
        panel.add(new JLabel(" Log file format "), c);

        logFileFormatTextField = new JTextField();
        logFileFormatTextField.setEditable(true);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(logFileFormatTextField, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.insets = new Insets(0, 5, 5, 5);
        panel.add(new JLabel(" Log file timestamp format "), c);

        logFileFormatTimestampFormatComboBoxModel = new DefaultComboBoxModel();
        seedLogFileFormatTimestampComboBoxModel();
        logFileFormatTimestampFormatComboBox = new JComboBox(logFileFormatTimestampFormatComboBoxModel);
        logFileFormatTimestampFormatComboBox.setEditable(true);
        logFileFormatTimestampFormatComboBox.setOpaque(false);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(logFileFormatTimestampFormatComboBox, c);

        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss,SSS");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth=5;
        c.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel(" Timestamps parsed using Java's SimpleDateFormat - use yyyy.MM.dd HH:mm:ss,SSS to parse " +  dateFormat.format(new Date()) + " "), c);
        return panel;
    }

    private void seedLogFileFormatTimestampComboBoxModel()
    {
        logFileFormatTimestampFormatComboBoxModel.addElement("yyyy-MM-dd HH:mm:ss,SSS");
        logFileFormatTimestampFormatComboBoxModel.addElement("yyyyMMdd HH:mm:ss.SSS");
        logFileFormatTimestampFormatComboBoxModel.addElement("yyyy/MM/dd HH:mm:ss");
        logFileFormatTimestampFormatComboBoxModel.addElement("dd MMM yyyy HH:mm:ss,SSS");
        logFileFormatTimestampFormatComboBoxModel.addElement("HH:mm:ss,SSS");
        logFileFormatTimestampFormatComboBoxModel.addElement("yyyy-MM-ddTHH:mm");
        logFileFormatTimestampFormatComboBoxModel.addElement("yyyy-MM-ddTHH:mm:ss.SSS");
    }

    private JPanel buildUseExistingConfigurationPanel() {
        existingConfigurationComboBoxModel = new DefaultComboBoxModel();

        existingConfigurationComboBox = new JComboBox(existingConfigurationComboBoxModel);
        existingConfigurationComboBox.setOpaque(false);
        existingConfigurationComboBox.setToolTipText("Previously loaded configurations can be chosen here");
        existingConfigurationComboBox.setEditable(true);

        existingConfigurationComboBox.getEditor().getEditorComponent().addFocusListener(
            new FocusListener() {
                public void focusGained(FocusEvent e) {
                    selectAll();
                }

                private void selectAll() {
                    existingConfigurationComboBox.getEditor().selectAll();
                }

                public void focusLost(FocusEvent e) {
                }
            });

        browseForAnExistingConfigurationButton = new JButton(new AbstractAction(" Find an existing configuration ") {
                    public void actionPerformed(ActionEvent e) {
                        try {

                            URL url = browseConfig();

                            if (url != null) {
                                getModel().configUrl = url;
                                existingConfigurationComboBoxModel.addElement(url);
                                existingConfigurationComboBox.getModel().setSelectedItem(
                                    url);
                            }
                        } catch (Exception ex) {
                            logger.error(
                                "Error browsing for Configuration file", ex);
                        }
                    }
                });

        browseForAnExistingConfigurationButton.setToolTipText(
            "Shows a File Open dialog to allow you to find a configuration file");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(browseForAnExistingConfigurationButton, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 5, 0, 5);
        panel.add(new JLabel(" Configuration file URL "), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(existingConfigurationComboBox, c);


        return panel;
    }

    /**
     * Returns the current Model/state of the chosen options by the user.
     * @return model
     */
    PanelModel getModel() {
        return panelModel;
    }

    /**
     * Clients of this panel can configure the ActionListener to be used
     * when the user presses the OK button, so they can read
     * back this Panel's model top determine what to do.
     * @param actionListener listener which will be notified that ok was selected
     */
    void setOkActionListener(ActionListener actionListener) {
        okButton.addActionListener(actionListener);
    }

    private void updateEnabledState(Component component) {
        existingConfigurationComboBox.setEnabled(component == useExistingConfigurationRadioButton);
        browseForAnExistingConfigurationButton.setEnabled(component == useExistingConfigurationRadioButton);
        networkReceiverPortComboBox.setEnabled(component == networkReceiverRadioButton);
        networkReceiverClassNameComboBox.setEnabled(component == networkReceiverRadioButton);
        browseLogFileButton.setEnabled(component == logFileReceiverRadioButton);
        logFileURLTextField.setEnabled(component == logFileReceiverRadioButton);
        logFileFormatTypeComboBox.setEnabled(component == logFileReceiverRadioButton);
        logFileFormatTextField.setEnabled(component == logFileReceiverRadioButton);
        logFileFormatTimestampFormatComboBox.setEnabled(component == logFileReceiverRadioButton);
    }

    /**
     * Returns the URL chosen by the user for a Configuration file
     * or null if they cancelled.
     */
    private URL browseConfig() throws MalformedURLException {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Use an existing configuration file...");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {

                    return f.isDirectory() ||
                    f.getName().endsWith(".properties") ||
                    f.getName().endsWith(".xml");
                }

                public String getDescription() {

                    return "Log4j Configuration file";
                }
            });

        chooser.showOpenDialog(this);

        File selectedFile = chooser.getSelectedFile();

        if (selectedFile == null) {

            return null;
        }

        if (!selectedFile.exists() || !selectedFile.canRead()) {

            return null;
        }

        return chooser.getSelectedFile().toURI().toURL();
    }

    /**
     * Returns the URL chosen by the user for a Configuration file
     * or null if they cancelled.
     */
    private URL browseLogFile() throws MalformedURLException {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Browse for a log file...");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.showOpenDialog(this);

        File selectedFile = chooser.getSelectedFile();

        if (selectedFile == null) {
            return null;
        }

        if (!selectedFile.exists() || !selectedFile.canRead()) {
            return null;
        }

        return chooser.getSelectedFile().toURI().toURL();
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame();
        frame.getContentPane().add(new ReceiverConfigurationPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * @return Returns the dontWarnMeAgain.
     */
    public final boolean isDontWarnMeAgain() {

        return dontwarnIfNoReceiver.isSelected();
    }

    /**
     * This class represents the model of the chosen options the user
     * has configured.
     *
     */
    class PanelModel {

        private URL configUrl;
        private File file;

        public PanelModel(){
            file = new File(SettingsManager.getInstance().getSettingsDirectory(), "receiver-config.xml");
        }

        boolean isNetworkReceiverMode() {

            return networkReceiverRadioButton.isSelected();
        }

        int getNetworkReceiverPort() {

            return Integer.parseInt(networkReceiverPortComboBoxModel.getSelectedItem().toString());
        }

        Class getNetworkReceiverClass() {

            return (Class) networkReceiverClassNameComboBoxModel.getSelectedItem();
        }

        boolean isLoadConfig() {

            return useExistingConfigurationRadioButton.isSelected();
        }

        boolean isLoadSavedConfigs() {

            return useAutoSavedConfigRadioButton.isSelected();
        }

        boolean isLogFileReceiverConfig() {
            return logFileReceiverRadioButton.isSelected();
        }

        public Object[] getRememberedConfigs() {

            Object[] urls = new Object[existingConfigurationComboBoxModel.getSize()];

            for (int i = 0; i < existingConfigurationComboBoxModel.getSize(); i++) {
                urls[i] = existingConfigurationComboBoxModel.getElementAt(i);
            }

            return urls;
        }

        public void setRememberedConfigs(final Object[] configs) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        existingConfigurationComboBoxModel.removeAllElements();

                        for (int i = 0; i < configs.length; i++) {
                            existingConfigurationComboBoxModel.addElement(configs[i]);
                        }
                    }
                });
        }

        URL getConfigToLoad() {

            return configUrl;
        }

        URL getSavedConfigToLoad() {
            try {
                if (file.exists()){
                    return file.toURL();
                } else {
                    logger.debug("No configuration file found");
                }
            } catch (MalformedURLException e) {
                logger.error("Error loading saved configurations by Chainsaw", e);
            }
            return null;
        }

        URL getLogFileURL() {
            try
            {
                Object item = logFileURLTextField.getText();
                if (item != null) {
                    return new URL(item.toString());
                }
            }
            catch (MalformedURLException e)
            {
                logger.error("Error retrieving log file URL", e);
            }
            return null;
        }

        String getLogFormat() {
            Object item = logFileFormatTextField.getText();
            if (item != null) {
                return item.toString();
            }
            return null;
        }

        boolean isPatternLayoutLogFormat() {
            Object item = logFileFormatTypeComboBox.getSelectedItem();
            return item != null && item.toString().toLowerCase().contains("patternlayout");
        }

        String getLogFormatTimestampFormat() {
            Object item = logFileFormatTimestampFormatComboBox.getSelectedItem();
            if (item != null) {
                return item.toString();
            }

            return null;
        }

        public void setRememberedLayouts(Object[] layouts)
        {
            //TODO: implement
            //add an entry to the logformat, formattype and timestamp fields

        }

        public Object[] getRememberedLayouts()
        {
            //TODO: implement
            return new Object[0];
        }
    }
}
