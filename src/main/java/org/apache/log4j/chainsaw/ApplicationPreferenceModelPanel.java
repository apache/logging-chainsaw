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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.osx.OSXIntegration;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;


/**
 * A panel used by the user to modify any application-wide preferences.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ApplicationPreferenceModelPanel extends AbstractPreferencePanel {
    private ApplicationPreferenceModel committedPreferenceModel;
    private ApplicationPreferenceModel uncommittedPreferenceModel =
        new ApplicationPreferenceModel();
    private JTextField identifierExpression;
    private JTextField toolTipDisplayMillis;
    private JTextField cyclicBufferSize;
    private JComboBox<String> configurationURL;
    private final Logger logger;
    private GeneralAllPrefPanel generalAllPrefPanel;

    ApplicationPreferenceModelPanel(ApplicationPreferenceModel model) {
        this.committedPreferenceModel = model;
        logger = LogManager.getLogger(ApplicationPreferenceModelPanel.class);
        initComponents();
        getOkButton().addActionListener(
            e -> {
                uncommittedPreferenceModel.setConfigurationURL((String) configurationURL.getSelectedItem());
                uncommittedPreferenceModel.setIdentifierExpression(
                    identifierExpression.getText());
                try {
                    int millis = Integer.parseInt(toolTipDisplayMillis.getText());
                    if (millis >= 0) {
                        uncommittedPreferenceModel.setToolTipDisplayMillis(millis);
                    }
                } catch (NumberFormatException nfe) {
                }
                try {
                    int bufferSize = Integer.parseInt(cyclicBufferSize.getText());
                    if (bufferSize >= 0) {
                        uncommittedPreferenceModel.setCyclicBufferSize(bufferSize);
                    }
                } catch (NumberFormatException nfe) {
                }
                committedPreferenceModel.apply(uncommittedPreferenceModel);
                hidePanel();
            });

        getCancelButton().addActionListener(
            e -> {
                uncommittedPreferenceModel.apply(committedPreferenceModel);
                hidePanel();
            });
    }


    public static void main(String[] args) {
        JFrame f = new JFrame("App Preferences Panel Test Bed");
        ApplicationPreferenceModel model = new ApplicationPreferenceModel();
        ApplicationPreferenceModelPanel panel =
            new ApplicationPreferenceModelPanel(model);
        f.getContentPane().add(panel);

        model.addPropertyChangeListener(System.out::println);
        panel.setOkCancelActionListener(
            e -> System.exit(1));

        f.setSize(640, 480);
        f.setVisible(true);
    }

    /**
     * Ensures this panels DISPLAYED model is in sync with
     * the model initially passed to the constructor.
     */
    public void updateModel() {
        this.uncommittedPreferenceModel.apply(committedPreferenceModel);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.AbstractPreferencePanel#createTreeModel()
     */
    protected TreeModel createTreeModel() {
        final DefaultMutableTreeNode rootNode =
            new DefaultMutableTreeNode("Preferences");
        DefaultTreeModel model = new DefaultTreeModel(rootNode);

        generalAllPrefPanel = new GeneralAllPrefPanel();
        DefaultMutableTreeNode general =
            new DefaultMutableTreeNode(generalAllPrefPanel);

        DefaultMutableTreeNode visuals =
            new DefaultMutableTreeNode(new VisualsPrefPanel());

        rootNode.add(general);
        rootNode.add(visuals);

        return model;
    }

    public class VisualsPrefPanel extends BasicPrefPanel {
        private final JRadioButton topPlacement = new JRadioButton(" Top         ");
        private final JRadioButton bottomPlacement = new JRadioButton(" Bottom         ");
        private final JCheckBox statusBar = new JCheckBox(" Show Status bar ");
        private final JCheckBox toolBar = new JCheckBox(" Show Toolbar ");
        private final JCheckBox receivers = new JCheckBox(" Show Receivers ");
        private UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
        private final ButtonGroup lookAndFeelGroup = new ButtonGroup();

        private VisualsPrefPanel() {
            super("Visuals");

            //Nimbus has major issues with colors in tables..just remove it from the list..
            //only use this if nimbus was found..
            UIManager.LookAndFeelInfo[] newLookAndFeels = new UIManager.LookAndFeelInfo[lookAndFeels.length];
            boolean useNewLookAndFeels = false;
            int j = 0;
            for (UIManager.LookAndFeelInfo lookAndFeel : lookAndFeels) {
                if (lookAndFeel.getClassName().toLowerCase(Locale.ENGLISH).contains("nimbus")) {
                    useNewLookAndFeels = true;
                } else {
                    newLookAndFeels[j++] = lookAndFeel;
                }
            }
            if (useNewLookAndFeels) {
                UIManager.LookAndFeelInfo[] replacedLookAndFeels = new UIManager.LookAndFeelInfo[lookAndFeels.length - 1];
                System.arraycopy(newLookAndFeels, 0, replacedLookAndFeels, 0, newLookAndFeels.length - 1);
                lookAndFeels = replacedLookAndFeels;
            }

            setupComponents();
            setupListeners();
            setupInitialValues();
        }

        /**
         *
         */
        private void setupListeners() {
            topPlacement.addActionListener(
                e -> uncommittedPreferenceModel.setTabPlacement(SwingConstants.TOP));
            bottomPlacement.addActionListener(
                e -> uncommittedPreferenceModel.setTabPlacement(SwingConstants.BOTTOM));

            statusBar.addActionListener(
                e -> uncommittedPreferenceModel.setStatusBar(statusBar.isSelected()));

            toolBar.addActionListener(
                e -> uncommittedPreferenceModel.setToolbar(toolBar.isSelected()));

            receivers.addActionListener(
                e -> uncommittedPreferenceModel.setReceivers(receivers.isSelected()));

            uncommittedPreferenceModel.addPropertyChangeListener(
                "tabPlacement",
                evt -> {
                    int value = (Integer) evt.getNewValue();

                    configureTabPlacement(value);
                });

            uncommittedPreferenceModel.addPropertyChangeListener(
                "statusBar",
                evt -> statusBar.setSelected(
                    (Boolean) evt.getNewValue()));

            uncommittedPreferenceModel.addPropertyChangeListener(
                "toolbar",
                evt -> toolBar.setSelected((Boolean) evt.getNewValue()));

            uncommittedPreferenceModel.addPropertyChangeListener(
                "receivers",
                evt -> receivers.setSelected(
                    (Boolean) evt.getNewValue()));

            uncommittedPreferenceModel.addPropertyChangeListener(
                "lookAndFeelClassName",
                evt -> {
                    String lf = evt.getNewValue().toString();

                    Enumeration enumeration = lookAndFeelGroup.getElements();

                    while (enumeration.hasMoreElements()) {
                        JRadioButton button = (JRadioButton) enumeration.nextElement();

                        if (button.getName() != null && button.getName().equals(lf)) {
                            button.setSelected(true);

                            break;
                        }
                    }
                });
        }

        /**
         *
         */
        private void setupComponents() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JPanel tabPlacementBox = new JPanel();
            tabPlacementBox.setLayout(new BoxLayout(tabPlacementBox, BoxLayout.Y_AXIS));

            tabPlacementBox.setBorder(BorderFactory.createTitledBorder(" Tab Placement "));

            ButtonGroup tabPlacementGroup = new ButtonGroup();

            tabPlacementGroup.add(topPlacement);
            tabPlacementGroup.add(bottomPlacement);

            tabPlacementBox.add(topPlacement);
            tabPlacementBox.add(bottomPlacement);

            /**
             * If we're OSX, we're 'not allowed' to change the tab placement...
             */
            if (OSXIntegration.IS_OSX) {
                tabPlacementBox.setEnabled(false);
                topPlacement.setEnabled(false);
                bottomPlacement.setEnabled(false);
            }

            add(tabPlacementBox);
            add(statusBar);
            add(receivers);
            add(toolBar);

            JPanel lfPanel = new JPanel();
            lfPanel.setLayout(new BoxLayout(lfPanel, BoxLayout.Y_AXIS));
            lfPanel.setBorder(BorderFactory.createTitledBorder(" Look & Feel "));

            for (final UIManager.LookAndFeelInfo lfInfo : lookAndFeels) {
                final JRadioButton lfItem = new JRadioButton(" " + lfInfo.getName() + " ");
                lfItem.setName(lfInfo.getClassName());
                lfItem.addActionListener(
                    e -> uncommittedPreferenceModel.setLookAndFeelClassName(
                        lfInfo.getClassName()));
                lookAndFeelGroup.add(lfItem);
                lfPanel.add(lfItem);
            }

            try {
                final Class gtkLF =
                    Class.forName("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                final JRadioButton lfIGTK = new JRadioButton(" GTK+ 2.0 ");
                lfIGTK.addActionListener(
                    e -> uncommittedPreferenceModel.setLookAndFeelClassName(
                        gtkLF.getName()));
                lookAndFeelGroup.add(lfIGTK);
                lfPanel.add(lfIGTK);
            } catch (Exception e) {
                logger.debug("Can't find new GTK L&F, might be Windows, or <JDK1.4.2");
            }

            add(lfPanel);

            add(
                new JLabel(
                    "<html>Look and Feel change will apply the next time you start Chainsaw.<br>" +
                        "If this value is not set, the default L&F of your system is used.</html>"));
        }

        private void configureTabPlacement(int value) {
            switch (value) {
                case SwingConstants.TOP:
                    topPlacement.setSelected(true);

                    break;

                case SwingConstants.BOTTOM:
                    bottomPlacement.setSelected(true);

                    break;

                default:
                    break;
            }
        }

        private void setupInitialValues() {
            statusBar.setSelected(uncommittedPreferenceModel.isStatusBar());
            receivers.setSelected(uncommittedPreferenceModel.isReceivers());
            toolBar.setSelected(uncommittedPreferenceModel.isToolbar());
            configureTabPlacement(uncommittedPreferenceModel.getTabPlacement());
            Enumeration e = lookAndFeelGroup.getElements();
            while (e.hasMoreElements()) {
                JRadioButton radioButton = (JRadioButton) e.nextElement();
                if (radioButton.getText().equals(uncommittedPreferenceModel.getLookAndFeelClassName())) {
                    radioButton.setSelected(true);
                    break;
                }
            }
        }
    }

    /**
     * @author psmith
     */
    public class GeneralAllPrefPanel extends BasicPrefPanel {
        private final JCheckBox showNoReceiverWarning =
            new JCheckBox(" Prompt me on startup if there are no Receivers defined ");
        private final JCheckBox showSplash = new JCheckBox(" Show Splash screen at startup ");
        private final JSlider responsiveSlider =
            new JSlider(SwingConstants.HORIZONTAL, 1, 4, 2);
        private final JCheckBox confirmExit = new JCheckBox(" Confirm Exit ");
        Dictionary<Integer, JLabel> sliderLabelMap = new Hashtable<>();

        private final JCheckBox okToRemoveSecurityManager = new JCheckBox(" Ok to remove SecurityManager ");

        public GeneralAllPrefPanel() {
            super("General");

            GeneralAllPrefPanel.this.initComponents();
        }

        private void initComponents() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            configurationURL = new JComboBox(); //new JComboBox<>(new DefaultComboBoxModel<>(committedPreferenceModel.getConfigurationURLs()));
            configurationURL.setEditable(true);
            configurationURL.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            configurationURL.setPreferredSize(new Dimension(375, 13));

            identifierExpression = new JTextField(30);
            toolTipDisplayMillis = new JTextField(8);
            cyclicBufferSize = new JTextField(8);
            Box p = new Box(BoxLayout.X_AXIS);

            p.add(showNoReceiverWarning);
            p.add(Box.createHorizontalGlue());

            confirmExit.setToolTipText("If set, you prompt to confirm Chainsaw exit");
            okToRemoveSecurityManager.setToolTipText("You will need to tick this to be able to load Receivers/Plugins that require external dependancies.");
            setupInitialValues();
            setupListeners();

            initSliderComponent();
            add(responsiveSlider);

            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));

            p1.add(new JLabel(" Tab name/event routing expression "));
            p1.add(Box.createHorizontalStrut(5));
            p1.add(identifierExpression);
            add(p1);
            add(p);

            Box p2 = new Box(BoxLayout.X_AXIS);
            p2.add(confirmExit);
            p2.add(Box.createHorizontalGlue());

            Box p3 = new Box(BoxLayout.X_AXIS);
            p3.add(showSplash);
            p3.add(Box.createHorizontalGlue());

            Box ok4 = new Box(BoxLayout.X_AXIS);
            ok4.add(okToRemoveSecurityManager);
            ok4.add(Box.createHorizontalGlue());

            add(p2);
            add(p3);
            add(ok4);

            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT));

            p4.add(new JLabel(" ToolTip Display (millis) "));
            p4.add(Box.createHorizontalStrut(5));
            p4.add(toolTipDisplayMillis);
            add(p4);

            JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT));

            p5.add(new JLabel(" Cyclic buffer size "));
            p5.add(Box.createHorizontalStrut(5));
            p5.add(cyclicBufferSize);
            p5.add(Box.createHorizontalStrut(5));
            p5.add(new JLabel(" (effective on restart) "));
            add(p5);

            Box p6 = new Box(BoxLayout.Y_AXIS);

            Box configURLPanel = new Box(BoxLayout.X_AXIS);
            JLabel configLabel = new JLabel(" Auto Config URL ");
            configURLPanel.add(configLabel);
            configURLPanel.add(Box.createHorizontalStrut(5));

            configURLPanel.add(configurationURL);
            configURLPanel.add(Box.createHorizontalGlue());

            p6.add(configURLPanel);

            JButton browseButton = new JButton(" Open File... ");
            browseButton.addActionListener(e -> browseForConfiguration());
            Box browsePanel = new Box(BoxLayout.X_AXIS);
            browsePanel.add(Box.createHorizontalGlue());
            browsePanel.add(browseButton);
            p6.add(Box.createVerticalStrut(5));
            p6.add(browsePanel);
            p6.add(Box.createVerticalGlue());
            add(p6);

            configurationURL.setToolTipText("A complete and valid URL identifying the location of a valid log4 xml configuration file to auto-configure Receivers and other Plugins");
            configurationURL.setInputVerifier(new InputVerifier() {

                public boolean verify(JComponent input) {
                    try {
                        String selectedItem = (String) configurationURL.getSelectedItem();
                        if (selectedItem != null && !(selectedItem.trim().equals(""))) {
                            new URL(selectedItem);
                        }
                    } catch (Exception e) {
                        return false;
                    }
                    return true;
                }
            });
//            String configToDisplay = committedPreferenceModel.getBypassConfigurationURL() != null ? committedPreferenceModel.getBypassConfigurationURL() : committedPreferenceModel.getConfigurationURL();
//            configurationURL.setSelectedItem(configToDisplay);
        }

        public void browseForConfiguration() {
            String defaultPath = ".";
            if (configurationURL.getItemCount() > 0) {
                Object selectedItem = configurationURL.getSelectedItem();
                if (selectedItem != null) {
                    File currentConfigurationPath = new File(selectedItem.toString()).getParentFile();
                    if (currentConfigurationPath != null) {
                        defaultPath = currentConfigurationPath.getPath();
                        //FileDialog will not navigate to this location unless we remove the prefixing protocol and slash
                        //at least on winxp
                        if (defaultPath.toLowerCase(Locale.ENGLISH).startsWith("file:\\")) {
                            defaultPath = defaultPath.substring("file:\\".length());
                        }
                    }
                }
            }
            File selectedFile = SwingHelper.promptForFile(this, defaultPath, "Select a Chainsaw configuration file", true);
            if (selectedFile != null) {
                try {
                    String newConfigurationFile = selectedFile.toURI().toURL().toExternalForm();
                    if (!committedPreferenceModel.getConfigurationURLs().contains(newConfigurationFile)) {
                        configurationURL.addItem(newConfigurationFile);
                    }
                    configurationURL.setSelectedItem(newConfigurationFile);
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
        }

        private void initSliderComponent() {
            responsiveSlider.setToolTipText(
                "Adjust to set the responsiveness of the app.  How often the view is updated.");
            responsiveSlider.setSnapToTicks(true);
            responsiveSlider.setLabelTable(sliderLabelMap);
            responsiveSlider.setPaintLabels(true);
            responsiveSlider.setPaintTrack(true);

            responsiveSlider.setBorder(BorderFactory.createTitledBorder(" Responsiveness "));

            //            responsiveSlider.setAlignmentY(0);
            //            responsiveSlider.setAlignmentX(0);
        }

        private void setupListeners() {
            uncommittedPreferenceModel.addPropertyChangeListener(
                "showNoReceiverWarning",
                evt -> showNoReceiverWarning.setSelected(
                    (Boolean) evt.getNewValue()));

            uncommittedPreferenceModel.addPropertyChangeListener("showSplash", evt -> {
                boolean value = (Boolean) evt.getNewValue();
                showSplash.setSelected(value);
            });

            uncommittedPreferenceModel.addPropertyChangeListener("okToRemoveSecurityManager", evt -> {
                boolean newValue = (Boolean) evt.getNewValue();
                if (newValue) {
                    okToRemoveSecurityManager.setSelected(newValue);
                } else {
                    okToRemoveSecurityManager.setSelected(false);
                }

            });


            uncommittedPreferenceModel.addPropertyChangeListener(
                "identifierExpression",
                evt -> identifierExpression.setText(evt.getNewValue().toString()));

            uncommittedPreferenceModel.addPropertyChangeListener(
                "responsiveness",
                evt -> {
                    int value = (Integer) evt.getNewValue();

                    if (value >= 1000) {
                        int newValue = (value - 750) / 1000;
                        logger.debug(
                            "Adjusting old Responsiveness value from " + value + " to "
                                + newValue);
                        value = newValue;
                    }

                    responsiveSlider.setValue(value);
                });

            uncommittedPreferenceModel.addPropertyChangeListener(
                "toolTipDisplayMillis",
                evt -> toolTipDisplayMillis.setText(evt.getNewValue().toString()));

            uncommittedPreferenceModel.addPropertyChangeListener(
                "cyclicBufferSize",
                evt -> cyclicBufferSize.setText(evt.getNewValue().toString()));

            showNoReceiverWarning.addActionListener(
                e -> uncommittedPreferenceModel.setShowNoReceiverWarning(
                    showNoReceiverWarning.isSelected()));

            showSplash.addActionListener(e -> uncommittedPreferenceModel.setShowSplash(showSplash.isSelected()));

            okToRemoveSecurityManager.addActionListener(e -> {

                if (okToRemoveSecurityManager.isSelected() && JOptionPane.showConfirmDialog(okToRemoveSecurityManager, "By ticking this option, you are authorizing Chainsaw to remove Java's Security Manager.\n\n" +
                    "This is required under Java Web Start so that it can access Jars/classes locally.  Without this, Receivers like JMSReceiver + DBReceiver that require" +
                    " specific driver jars will NOT be able to be run.  \n\n" +
                    "By ticking this, you are saying that this is ok.", "Please Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    uncommittedPreferenceModel.setOkToRemoveSecurityManager(true);
                } else {
                    uncommittedPreferenceModel.setOkToRemoveSecurityManager(false);
                }

            });


            responsiveSlider.getModel().addChangeListener(
                e -> {
                    if (responsiveSlider.getValueIsAdjusting()) {
                        /**
                         * We'll wait until it stops.
                         */
                    } else {
                        int value = responsiveSlider.getValue();

                        if (value == 0) {
                            value = 1;
                        }

                        logger.debug("Adjust responsiveness to " + value);
                        uncommittedPreferenceModel.setResponsiveness(value);
                    }
                });

            uncommittedPreferenceModel.addPropertyChangeListener(
                "confirmExit",
                evt -> {
                    boolean value = (Boolean) evt.getNewValue();
                    confirmExit.setSelected(value);
                });

            uncommittedPreferenceModel.addPropertyChangeListener("configurationURL", evt -> {
                String value = evt.getNewValue().toString();
                configurationURL.setSelectedItem(value);
            });
            confirmExit.addActionListener(
                e -> uncommittedPreferenceModel.setConfirmExit(
                    confirmExit.isSelected()));
        }

        private void setupInitialValues() {
            sliderLabelMap.put(1, new JLabel(" Fastest "));
            sliderLabelMap.put(2, new JLabel(" Fast "));
            sliderLabelMap.put(3, new JLabel(" Medium "));
            sliderLabelMap.put(4, new JLabel(" Slow "));

            //
            showNoReceiverWarning.setSelected(
                uncommittedPreferenceModel.isShowNoReceiverWarning());
            identifierExpression.setText(
                uncommittedPreferenceModel.getIdentifierExpression());

            confirmExit.setSelected(uncommittedPreferenceModel.isConfirmExit());
            okToRemoveSecurityManager.setSelected(uncommittedPreferenceModel.isOkToRemoveSecurityManager());
            showNoReceiverWarning.setSelected(uncommittedPreferenceModel.isShowNoReceiverWarning());
            showSplash.setSelected(uncommittedPreferenceModel.isShowSplash());
            identifierExpression.setText(uncommittedPreferenceModel.getIdentifierExpression());
            toolTipDisplayMillis.setText(uncommittedPreferenceModel.getToolTipDisplayMillis() + "");
            cyclicBufferSize.setText(uncommittedPreferenceModel.getCyclicBufferSize() + "");
            configurationURL.setSelectedItem(uncommittedPreferenceModel.getConfigurationURL());
        }
    }
}
