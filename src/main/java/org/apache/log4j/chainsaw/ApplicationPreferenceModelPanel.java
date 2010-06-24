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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.osx.OSXIntegration;


/**
 * A panel used by the user to modify any application-wide preferences.
 *
 * @author Paul Smith <psmith@apache.org>
 *
 */
public class ApplicationPreferenceModelPanel extends AbstractPreferencePanel {
  private ApplicationPreferenceModel committedPreferenceModel;
  private ApplicationPreferenceModel uncommittedPreferenceModel =
    new ApplicationPreferenceModel();
  private JTextField identifierExpression;
  private JTextField toolTipDisplayMillis;
  private JTextField cyclicBufferSize;    
  private JComboBox configurationURL;
  private final Logger logger;
  private GeneralAllPrefPanel generalAllPrefPanel;

    ApplicationPreferenceModelPanel(ApplicationPreferenceModel model) {
    this.committedPreferenceModel = model;
    logger = LogManager.getLogger(ApplicationPreferenceModelPanel.class);
    initComponents();
    getOkButton().addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          uncommittedPreferenceModel.setConfigurationURL((String)configurationURL.getSelectedItem());
          uncommittedPreferenceModel.setIdentifierExpression(
            identifierExpression.getText());
            try {
                int millis = Integer.parseInt(toolTipDisplayMillis.getText());
                if (millis >= 0) {
                    uncommittedPreferenceModel.setToolTipDisplayMillis(millis);
                }
            } catch (NumberFormatException nfe) {}
            try {
                int bufferSize = Integer.parseInt(cyclicBufferSize.getText());
                if (bufferSize >= 0) {
                    uncommittedPreferenceModel.setCyclicBufferSize(bufferSize);
                }
            } catch (NumberFormatException nfe) {}
          committedPreferenceModel.apply(uncommittedPreferenceModel);
          hidePanel();
        }
      });

    getCancelButton().addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          uncommittedPreferenceModel.apply(committedPreferenceModel);
          hidePanel();
        }
      });
  }


public static void main(String[] args) {
    JFrame f = new JFrame("App Preferences Panel Test Bed");
    ApplicationPreferenceModel model = new ApplicationPreferenceModel();
    ApplicationPreferenceModelPanel panel =
      new ApplicationPreferenceModelPanel(model);
    f.getContentPane().add(panel);

    model.addPropertyChangeListener(
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          System.out.println(evt);
        }
      });
    panel.setOkCancelActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.exit(1);
        }
      });

    f.setSize(640, 480);
    f.setVisible(true);
  }

  /**
   * Ensures this panels DISPLAYED model is in sync with
   * the model initially passed to the constructor.
   *
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

    public void browseForConfiguration() {
        generalAllPrefPanel.browseForConfiguration();
    }

    public class VisualsPrefPanel extends BasicPrefPanel {
    private final JRadioButton topPlacement = new JRadioButton("Top");
    private final JRadioButton bottomPlacement = new JRadioButton("Bottom");
    private final JCheckBox statusBar = new JCheckBox("Show Status bar");
    private final JCheckBox toolBar = new JCheckBox("Show Toolbar");
    private final JCheckBox receivers = new JCheckBox("Show Receivers");
    private UIManager.LookAndFeelInfo[] lookAndFeels =
      UIManager.getInstalledLookAndFeels();
    private final ButtonGroup lookAndFeelGroup = new ButtonGroup();

    private VisualsPrefPanel() {
      super("Visuals");
      setupComponents();
      setupListeners();
      setupInitialValues();
    }

    /**
     *
     */
    private void setupListeners() {
      topPlacement.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            uncommittedPreferenceModel.setTabPlacement(SwingConstants.TOP);
          }
        });
      bottomPlacement.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            uncommittedPreferenceModel.setTabPlacement(SwingConstants.BOTTOM);
          }
        });

      statusBar.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            uncommittedPreferenceModel.setStatusBar(statusBar.isSelected());
          }
        });

      toolBar.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            uncommittedPreferenceModel.setToolbar(toolBar.isSelected());
          }
        });

      receivers.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            uncommittedPreferenceModel.setReceivers(receivers.isSelected());
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener(
        "tabPlacement",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            int value = ((Integer) evt.getNewValue()).intValue();

            configureTabPlacement(value);
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener(
        "statusBar",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            statusBar.setSelected(
              ((Boolean) evt.getNewValue()).booleanValue());
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener(
        "toolbar",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            toolBar.setSelected(((Boolean) evt.getNewValue()).booleanValue());
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener(
        "receivers",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            receivers.setSelected(
              ((Boolean) evt.getNewValue()).booleanValue());
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener(
        "lookAndFeelClassName",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            String lf = evt.getNewValue().toString();

            Enumeration enumeration = lookAndFeelGroup.getElements();

            while (enumeration.hasMoreElements()) {
              JRadioButton button = (JRadioButton) enumeration.nextElement();

              if (button.getName()!=null && button.getName().equals(lf)) {
                button.setSelected(true);

                break;
              }
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
      tabPlacementBox.setLayout(
        new BoxLayout(tabPlacementBox, BoxLayout.Y_AXIS));

      tabPlacementBox.setBorder(
        BorderFactory.createTitledBorder(
          BorderFactory.createEtchedBorder(), "Tab Placement"));

      ButtonGroup tabPlacementGroup = new ButtonGroup();

      tabPlacementGroup.add(topPlacement);
      tabPlacementGroup.add(bottomPlacement);

      tabPlacementBox.add(topPlacement);
      tabPlacementBox.add(bottomPlacement);

      /** 
       * If we're OSX, we're 'not allowed' to change the tab placement... 
       */
      if(OSXIntegration.IS_OSX) {
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
      lfPanel.setBorder(
        BorderFactory.createTitledBorder(
          BorderFactory.createEtchedBorder(), "Look & Feel"));

      for (int i = 0; i < lookAndFeels.length; i++) {
        final UIManager.LookAndFeelInfo lfInfo = lookAndFeels[i];
        final JRadioButton lfItem = new JRadioButton(lfInfo.getName());
        lfItem.setName(lfInfo.getClassName());
        lfItem.addActionListener(
          new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              uncommittedPreferenceModel.setLookAndFeelClassName(
                lfInfo.getClassName());
            }
          });
        lookAndFeelGroup.add(lfItem);
        lfPanel.add(lfItem);
      }

      try {
        final Class gtkLF =
          Class.forName("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        final JRadioButton lfIGTK = new JRadioButton("GTK+ 2.0");
        lfIGTK.addActionListener(
          new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              uncommittedPreferenceModel.setLookAndFeelClassName(
                gtkLF.getName());
            }
          });
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
        while(e.hasMoreElements()) {
            JRadioButton radioButton = (JRadioButton)e.nextElement();
            if(radioButton.getText().equals(uncommittedPreferenceModel.getLookAndFeelClassName())) {
                radioButton.setSelected(true);
                break;
            }
        }
    }
  }

  /**
   * @author psmith
   *
   */
  public class GeneralAllPrefPanel extends BasicPrefPanel {
    private final JCheckBox showNoReceiverWarning =
      new JCheckBox("Prompt me on startup if there are no Receivers defined");
    private final JCheckBox showSplash = new JCheckBox("Show Splash screen at startup");
    private final JSlider responsiveSlider =
      new JSlider(SwingConstants.HORIZONTAL, 1, 4, 2);
    private final JCheckBox confirmExit = new JCheckBox("Confirm Exit");
    Dictionary sliderLabelMap = new Hashtable();
    
    private final JCheckBox okToRemoveSecurityManager = new JCheckBox("Ok to remove SecurityManager");

    public GeneralAllPrefPanel() {
      super("General");

      GeneralAllPrefPanel.this.initComponents();
    }

    private void initComponents() {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      configurationURL = new JComboBox(new DefaultComboBoxModel(committedPreferenceModel.getConfigurationURLs()));
      configurationURL.setEditable(true);
      configurationURL.setPrototypeDisplayValue("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
      configurationURL.setPreferredSize(new Dimension(375, 13));

      identifierExpression = new JTextField(30);
      toolTipDisplayMillis = new JTextField(8);
      cyclicBufferSize = new JTextField(8);
      Box p = new Box(BoxLayout.X_AXIS);
                                                           
      p.add(showNoReceiverWarning);
      p.add(Box.createHorizontalGlue());

      confirmExit.setToolTipText("Is set, you will be prompted to confirm the exit Chainsaw");
      okToRemoveSecurityManager.setToolTipText("You will need to tick this to be able to load Receivers/Plugins that require external dependancies.");
      setupInitialValues();
      setupListeners();

      initSliderComponent();
      add(responsiveSlider);

      JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));

      p1.add(new JLabel("Tab name/event routing expression"));
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

      p4.add(new JLabel("ToolTip Display (millis)"));
      p4.add(Box.createHorizontalStrut(5));
      p4.add(toolTipDisplayMillis);
      add(p4);

      JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT));

      p5.add(new JLabel("Cyclic buffer size"));
      p5.add(Box.createHorizontalStrut(5));
      p5.add(cyclicBufferSize);
      p5.add(Box.createHorizontalStrut(5));
      p5.add(new JLabel("Cyclic buffer size change will take effect on Chainsaw restart"));
      add(p5);

      Box p6 = new Box(BoxLayout.Y_AXIS);

      Box configURLPanel = new Box(BoxLayout.X_AXIS);
      JLabel configLabel = new JLabel("Auto Config URL");
      configURLPanel.add(configLabel);
      configURLPanel.add(Box.createHorizontalStrut(5));

      configURLPanel.add(configurationURL);
      configURLPanel.add(Box.createHorizontalGlue());

      p6.add(configURLPanel);

      JButton browseButton = new JButton(" Browse ");
      browseButton.addActionListener(new ActionListener()
      {
          public void actionPerformed(ActionEvent e)
          {
              browseForConfiguration();
          }
      });
      Box browsePanel = new Box(BoxLayout.X_AXIS);
      browsePanel.add(Box.createHorizontalGlue());
      browsePanel.add(browseButton);
      p6.add(Box.createVerticalStrut(5));
      p6.add(browsePanel);
      p6.add(Box.createVerticalGlue());
      add(p6);

      configurationURL.setToolTipText("A complete and valid URL identifying the location of a valid log4 xml configuration file to auto-configure Receivers and other Plugins");
      configurationURL.setInputVerifier(new InputVerifier() {

        public boolean verify(JComponent input)
        {
            try {
                String selectedItem = (String)configurationURL.getSelectedItem();
                if (selectedItem != null && !(selectedItem.trim().equals(""))) {
                    new URL(selectedItem);
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }});
        String configToDisplay = committedPreferenceModel.getBypassConfigurationURL() != null?committedPreferenceModel.getBypassConfigurationURL():committedPreferenceModel.getConfigurationURL();
        configurationURL.setSelectedItem(configToDisplay);
    }

    public void browseForConfiguration() {
          String defaultPath = ".";
          if (configurationURL.getItemCount() > 0) {
              Object selectedItem = configurationURL.getSelectedItem();
              if (selectedItem != null) {
                  File currentConfigurationPath = new File(selectedItem.toString()).getParentFile();
                  if (currentConfigurationPath != null) {
                      defaultPath = currentConfigurationPath.getPath();
                      //JFileChooser constructor will not navigate to this location unless we remove the prefixing protocol and slash
                      //at least on winxp
                      if (defaultPath.toLowerCase().startsWith("file:\\")) {
                          defaultPath = defaultPath.substring("file:\\".length());
                      }
                  }
              }
          }

          JFileChooser chooser = new JFileChooser(defaultPath);
          int result = chooser.showOpenDialog(ApplicationPreferenceModelPanel.this);
          if (JFileChooser.APPROVE_OPTION == result) {
              File f = chooser.getSelectedFile();
              try
              {
                  String newConfigurationFile = f.toURI().toURL().toExternalForm();
                  if (!committedPreferenceModel.getConfigurationURLs().contains(newConfigurationFile)) {
                    configurationURL.addItem(newConfigurationFile);
                  }
                  configurationURL.setSelectedItem(newConfigurationFile);
              }
              catch (MalformedURLException e1)
              {
                  e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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

      responsiveSlider.setBorder(
        BorderFactory.createTitledBorder(
          BorderFactory.createEtchedBorder(), "Responsiveness"));

      //            responsiveSlider.setAlignmentY(0);
      //            responsiveSlider.setAlignmentX(0);
    }

    private void setupListeners() {
      uncommittedPreferenceModel.addPropertyChangeListener(
        "showNoReceiverWarning",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            showNoReceiverWarning.setSelected(
              ((Boolean) evt.getNewValue()).booleanValue());
          }
        });
      
      uncommittedPreferenceModel.addPropertyChangeListener("showSplash", new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
          boolean value = ((Boolean)evt.getNewValue()).booleanValue();
          showSplash.setSelected(value);
        }});
      
      uncommittedPreferenceModel.addPropertyChangeListener("okToRemoveSecurityManager", new PropertyChangeListener() {

		public void propertyChange(PropertyChangeEvent evt) {
            boolean newValue = ((Boolean) evt.getNewValue()).booleanValue();
            if(newValue) {
            okToRemoveSecurityManager.setSelected(newValue);
            }else {
                okToRemoveSecurityManager.setSelected(false);
            }
            
		}});
      
      
      uncommittedPreferenceModel.addPropertyChangeListener(
        "identifierExpression",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            identifierExpression.setText(evt.getNewValue().toString());
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener(
        "responsiveness",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            int value = ((Integer) evt.getNewValue()).intValue();

            if (value >= 1000) {
              int newValue = (value - 750) / 1000;
              logger.debug(
                "Adjusting old Responsiveness value from " + value + " to "
                + newValue);
              value = newValue;
            }

            responsiveSlider.setValue(value);
          }
        });

        uncommittedPreferenceModel.addPropertyChangeListener(
          "toolTipDisplayMillis",
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
              toolTipDisplayMillis.setText(evt.getNewValue().toString());
            }
          });

        uncommittedPreferenceModel.addPropertyChangeListener(
          "cyclicBufferSize",
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
              cyclicBufferSize.setText(evt.getNewValue().toString());
            }
          });

      showNoReceiverWarning.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            uncommittedPreferenceModel.setShowNoReceiverWarning(
              showNoReceiverWarning.isSelected());
          }
        });

      showSplash.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          uncommittedPreferenceModel.setShowSplash(showSplash.isSelected());
        }});
      
      okToRemoveSecurityManager.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            
          if(okToRemoveSecurityManager.isSelected() && JOptionPane.showConfirmDialog(okToRemoveSecurityManager, "By ticking this option, you are authorizing Chainsaw to remove Java's Security Manager.\n\n" +
                    "This is required under Java Web Start so that it can access Jars/classes locally.  Without this, Receivers like JMSReceiver + DBReceiver that require" +
                    " specific driver jars will NOT be able to be run.  \n\n" +
                    "By ticking this, you are saying that this is ok.", "Please Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          	uncommittedPreferenceModel.setOkToRemoveSecurityManager(true);
          }else {
            uncommittedPreferenceModel.setOkToRemoveSecurityManager(false);
          }
                
        }});
      
      
      responsiveSlider.getModel().addChangeListener(
        new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
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
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener(
        "confirmExit",
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            boolean value = ((Boolean) evt.getNewValue()).booleanValue();
            confirmExit.setSelected(value);
          }
        });

      uncommittedPreferenceModel.addPropertyChangeListener("configurationURL", new PropertyChangeListener() {

          public void propertyChange(PropertyChangeEvent evt) {
            String value = evt.getNewValue().toString();
            configurationURL.setSelectedItem(value);
          }});
      confirmExit.addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            uncommittedPreferenceModel.setConfirmExit(
              confirmExit.isSelected());
          }
        });
    }

    private void setupInitialValues() {
      sliderLabelMap.put(new Integer(1), new JLabel("Fastest"));
      sliderLabelMap.put(new Integer(2), new JLabel("Fast"));
      sliderLabelMap.put(new Integer(3), new JLabel("Medium"));
      sliderLabelMap.put(new Integer(4), new JLabel("Slow"));

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
      toolTipDisplayMillis.setText(uncommittedPreferenceModel.getToolTipDisplayMillis()+"");
      cyclicBufferSize.setText(uncommittedPreferenceModel.getCyclicBufferSize() + "");
      configurationURL.setSelectedItem(uncommittedPreferenceModel.getConfigurationURL());
    }
  }
}
