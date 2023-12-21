/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
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

import java.awt.*;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A panel used by the user to modify any application-wide preferences.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ApplicationPreferenceModelPanel extends AbstractPreferencePanel {
    private static final Logger logger = LogManager.getLogger(ApplicationPreferenceModelPanel.class);
    private final ApplicationPreferenceModel applicationPreferenceModel;

    private JTextField toolTipDisplayMillis;
    private JTextField cyclicBufferSize;
    private GeneralAllPrefPanel generalAllPrefPanel;

    public ApplicationPreferenceModelPanel(
            SettingsManager settingsManager, ApplicationPreferenceModel applicationPreferenceModel) {
        this.applicationPreferenceModel = applicationPreferenceModel;
        initComponents();
        getOkButton().addActionListener(e -> {
            settingsManager.saveGlobalSettings();
            hidePanel();
        });

        getCancelButton().addActionListener(e -> hidePanel());
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.AbstractPreferencePanel#createTreeModel()
     */
    protected TreeModel createTreeModel() {
        final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Preferences");
        DefaultTreeModel model = new DefaultTreeModel(rootNode);

        generalAllPrefPanel = new GeneralAllPrefPanel();
        DefaultMutableTreeNode general = new DefaultMutableTreeNode(generalAllPrefPanel);

        DefaultMutableTreeNode visuals = new DefaultMutableTreeNode(new VisualsPrefPanel());

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

            // Nimbus has major issues with colors in tables..just remove it from the list..
            // only use this if nimbus was found..
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
                UIManager.LookAndFeelInfo[] replacedLookAndFeels =
                        new UIManager.LookAndFeelInfo[lookAndFeels.length - 1];
                System.arraycopy(newLookAndFeels, 0, replacedLookAndFeels, 0, newLookAndFeels.length - 1);
                lookAndFeels = replacedLookAndFeels;
            }

            setupComponents();
            setupListeners();
            updateModel();
        }

        /**
         *
         */
        private void setupListeners() {
            topPlacement.addActionListener(e -> applicationPreferenceModel.setTabPlacement(SwingConstants.TOP));
            bottomPlacement.addActionListener(e -> applicationPreferenceModel.setTabPlacement(SwingConstants.BOTTOM));

            statusBar.addActionListener(e -> applicationPreferenceModel.setStatusBarVisible(statusBar.isSelected()));

            toolBar.addActionListener(e -> applicationPreferenceModel.setToolbarVisible(toolBar.isSelected()));

            receivers.addActionListener(e -> applicationPreferenceModel.setReceiversVisible(receivers.isSelected()));

            //            uncommittedPreferenceModel.addPropertyChangeListener(
            //                "tabPlacement",
            //                evt -> {
            //                    int value = (Integer) evt.getNewValue();
            //
            //                    configureTabPlacement(value);
            //                });
            //
            //            uncommittedPreferenceModel.addPropertyChangeListener(
            //                "statusBar",
            //                evt -> statusBar.setSelected(
            //                    (Boolean) evt.getNewValue()));
            //
            //            uncommittedPreferenceModel.addPropertyChangeListener(
            //                "toolbar",
            //                evt -> toolBar.setSelected((Boolean) evt.getNewValue()));
            //
            //            uncommittedPreferenceModel.addPropertyChangeListener(
            //                "receivers",
            //                evt -> receivers.setSelected(
            //                    (Boolean) evt.getNewValue()));
            //
            //            uncommittedPreferenceModel.addPropertyChangeListener(
            //                "lookAndFeelClassName",
            //                evt -> {
            //                    String lf = evt.getNewValue().toString();
            //
            //                    Enumeration enumeration = lookAndFeelGroup.getElements();
            //
            //                    while (enumeration.hasMoreElements()) {
            //                        JRadioButton button = (JRadioButton) enumeration.nextElement();
            //
            //                        if (button.getName() != null && button.getName().equals(lf)) {
            //                            button.setSelected(true);
            //
            //                            break;
            //                        }
            //                    }
            //                });
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
                        e -> applicationPreferenceModel.setLookAndFeelClassName(lfInfo.getClassName()));
                lookAndFeelGroup.add(lfItem);
                lfPanel.add(lfItem);
            }

            try {
                final Class gtkLF = Class.forName("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                final JRadioButton lfIGTK = new JRadioButton(" GTK+ 2.0 ");
                lfIGTK.addActionListener(e -> applicationPreferenceModel.setLookAndFeelClassName(gtkLF.getName()));
                lookAndFeelGroup.add(lfIGTK);
                lfPanel.add(lfIGTK);
            } catch (Exception e) {
                logger.debug("Can't find new GTK L&F, might be Windows, or <JDK1.4.2");
            }

            add(lfPanel);

            add(new JLabel("<html>Look and Feel change will apply the next time you start Chainsaw.<br>"
                    + "If this value is not set, the default L&F of your system is used.</html>"));
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

        public void updateModel() {
            statusBar.setSelected(applicationPreferenceModel.isStatusBarVisible());
            receivers.setSelected(applicationPreferenceModel.isReceiversVisible());
            toolBar.setSelected(applicationPreferenceModel.isToolbarVisible());
            configureTabPlacement(applicationPreferenceModel.getTabPlacement());
            Enumeration e = lookAndFeelGroup.getElements();
            while (e.hasMoreElements()) {
                JRadioButton radioButton = (JRadioButton) e.nextElement();
                if (radioButton.getText().equals(applicationPreferenceModel.getLookAndFeelClassName())) {
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
        private final JCheckBox showSplash = new JCheckBox(" Show Splash screen at startup ");
        private final JSlider responsiveSlider = new JSlider(SwingConstants.HORIZONTAL, 1, 4, 2);
        private final JCheckBox confirmExit = new JCheckBox(" Confirm Exit ");
        Dictionary<Integer, JLabel> sliderLabelMap = new Hashtable<>();

        public GeneralAllPrefPanel() {
            super("General");

            GeneralAllPrefPanel.this.initComponents();
        }

        private void initComponents() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            toolTipDisplayMillis = new JTextField(8);
            cyclicBufferSize = new JTextField(8);
            Box p = new Box(BoxLayout.X_AXIS);

            p.add(Box.createHorizontalGlue());

            confirmExit.setToolTipText("If set, you prompt to confirm Chainsaw exit");
            setupInitialValues();
            setupListeners();

            initSliderComponent();
            add(responsiveSlider);

            Box p2 = new Box(BoxLayout.X_AXIS);
            p2.add(confirmExit);
            p2.add(Box.createHorizontalGlue());

            Box p3 = new Box(BoxLayout.X_AXIS);
            p3.add(showSplash);
            p3.add(Box.createHorizontalGlue());

            Box ok4 = new Box(BoxLayout.X_AXIS);
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
        }

        private void initSliderComponent() {
            responsiveSlider.setToolTipText(
                    "Adjust to set the responsiveness of the app.  How often the view is updated.");
            responsiveSlider.setSnapToTicks(true);
            responsiveSlider.setLabelTable(sliderLabelMap);
            responsiveSlider.setPaintLabels(true);
            responsiveSlider.setPaintTrack(true);

            responsiveSlider.setBorder(BorderFactory.createTitledBorder(" Responsiveness "));
        }

        private void setupListeners() {
            applicationPreferenceModel.addEventListener(evt -> {
                if (evt.getPropertyName().equals(ApplicationPreferenceModel.SHOW_SPLASH)) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    showSplash.setSelected(value);
                }
            });

            applicationPreferenceModel.addEventListener(evt -> {
                if (evt.getPropertyName().equals(ApplicationPreferenceModel.RESPONSIVENESS)) {
                    int value = (Integer) evt.getPropertyValue();
                    if (value >= 1000) {
                        int newValue = (value - 750) / 1000;
                        logger.debug("Adjusting old Responsiveness value from {} to {}", value, newValue);
                        value = newValue;
                    }

                    responsiveSlider.setValue(value);
                }
            });

            applicationPreferenceModel.addEventListener(evt -> {
                if (evt.getPropertyName().equals(ApplicationPreferenceModel.TOOL_TIP_DISPLAY_MILLIS)) {
                    toolTipDisplayMillis.setText(evt.getPropertyValue().toString());
                }
            });

            applicationPreferenceModel.addEventListener(evt -> {
                if (evt.getPropertyName().equals(ApplicationPreferenceModel.CYCLIC_BUFFER_SIZE)) {
                    cyclicBufferSize.setText(evt.getPropertyValue().toString());
                }
            });

            applicationPreferenceModel.addEventListener(evt -> {
                if (evt.getPropertyName().equals(ApplicationPreferenceModel.CONFIRM_EXIT)) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    confirmExit.setSelected(value);
                }
            });

            showSplash.addActionListener(e -> applicationPreferenceModel.setShowSplash(showSplash.isSelected()));
            responsiveSlider.getModel().addChangeListener(e -> {
                if (responsiveSlider.getValueIsAdjusting()) {
                    /**
                     * We'll wait until it stops.
                     */
                } else {
                    int value = responsiveSlider.getValue();

                    if (value == 0) {
                        value = 1;
                    }

                    logger.debug("Adjust responsiveness to {}", value);
                    applicationPreferenceModel.setResponsiveness(value);
                }
            });

            confirmExit.addActionListener(e -> applicationPreferenceModel.setConfirmExit(confirmExit.isSelected()));
        }

        private void setupInitialValues() {
            sliderLabelMap.put(1, new JLabel(" Fastest "));
            sliderLabelMap.put(2, new JLabel(" Fast "));
            sliderLabelMap.put(3, new JLabel(" Medium "));
            sliderLabelMap.put(4, new JLabel(" Slow "));

            confirmExit.setSelected(applicationPreferenceModel.isConfirmExit());
            showSplash.setSelected(applicationPreferenceModel.isShowSplash());
            toolTipDisplayMillis.setText(String.valueOf(applicationPreferenceModel.getToolTipDisplayMillis()));
            cyclicBufferSize.setText(String.valueOf(applicationPreferenceModel.getCyclicBufferSize()));
        }
    }
}
