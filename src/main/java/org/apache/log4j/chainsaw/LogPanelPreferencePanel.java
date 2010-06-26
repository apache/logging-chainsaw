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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


/**
 * GUI panel used to manipulate the PreferenceModel for a Log Panel
 *
 * @author Paul Smith
 */
public class LogPanelPreferencePanel extends AbstractPreferencePanel
{
  //~ Instance fields =========================================================

  private final LogPanelPreferenceModel preferenceModel;
  private final ModifiableListModel columnListModel = new ModifiableListModel();
  private static final Logger logger = LogManager.getLogger(LogPanelPreferencePanel.class);

  //~ Constructors ============================================================

  public LogPanelPreferencePanel(LogPanelPreferenceModel model)
  {
    preferenceModel = model;
    initComponents();

    getOkButton().addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          hidePanel();
        }
      });

    getCancelButton().addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          hidePanel();
        }
      });
    }

  //~ Methods =================================================================

  /**
   * DOCUMENT ME!
   *
   * @param args DOCUMENT ME!
   */
  public static void main(String[] args)
  {
    JFrame f = new JFrame("Preferences Panel Test Bed");
    LogPanelPreferenceModel model = new LogPanelPreferenceModel();
    LogPanelPreferencePanel panel = new LogPanelPreferencePanel(model);
    f.getContentPane().add(panel);

    model.addPropertyChangeListener(new PropertyChangeListener()
      {
        public void propertyChange(PropertyChangeEvent evt)
        {
          logger.warn(evt.toString());
        }
      });
    panel.setOkCancelActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          System.exit(1);
        }
      });

    f.setSize(640, 480);
    f.setVisible(true);
  }

  protected TreeModel createTreeModel()
  {
    final DefaultMutableTreeNode rootNode =
      new DefaultMutableTreeNode("Preferences");
    DefaultTreeModel model = new DefaultTreeModel(rootNode);

    DefaultMutableTreeNode visuals =
      new DefaultMutableTreeNode(new VisualsPrefPanel());
    DefaultMutableTreeNode formatting =
      new DefaultMutableTreeNode(new FormattingPanel());
    DefaultMutableTreeNode columns =
      new DefaultMutableTreeNode(new ColumnSelectorPanel());

    rootNode.add(visuals);
    rootNode.add(formatting);
    rootNode.add(columns);

    return model;
  }

  //~ Inner Classes ===========================================================

  /**
   * Allows the user to choose which columns to display.
   *
   * @author Paul Smith
   *
   */
  public class ColumnSelectorPanel extends BasicPrefPanel
  {
    //~ Constructors ==========================================================

    ColumnSelectorPanel()
    {
      super("Columns");
      initComponents();
    }

    //~ Methods ===============================================================

    private void initComponents()
    {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      Box columnBox = new Box(BoxLayout.Y_AXIS);

      //		columnBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Displayed Columns"));
      final JList columnList = new JList();
      columnList.setVisibleRowCount(17);

      for (
        Iterator iter = preferenceModel.getColumns().iterator();
          iter.hasNext();)
      {
          TableColumn col = (TableColumn)iter.next();
          Enumeration enumeration = columnListModel.elements();
          boolean found = false;
          while (enumeration.hasMoreElements()) {
              TableColumn thisCol = (TableColumn) enumeration.nextElement();
              if (thisCol.getHeaderValue().equals(col.getHeaderValue())) {
                  found = true;
              }
          }
            if (!found) {
              columnListModel.addElement(col);
            }
      }

      columnList.setModel(columnListModel);

      CheckListCellRenderer cellRenderer = new CheckListCellRenderer()
        {
          protected boolean isSelected(Object value)
          {
            return LogPanelPreferencePanel.this.preferenceModel.isColumnVisible((TableColumn)
              value);
          }
        };

      columnList.addMouseListener(new MouseAdapter()
        {
          public void mouseClicked(MouseEvent e)
          {
            if (
              (e.getClickCount() == 1)
                && ((e.getModifiers() & InputEvent.BUTTON1_MASK) > 0))
            {
              int i = columnList.locationToIndex(e.getPoint());

              if (i >= 0)
              {
                Object column = columnListModel.get(i);
                preferenceModel.toggleColumn(((TableColumn)column));
              }
            }
          }
        });
      columnList.setCellRenderer(cellRenderer);
      columnBox.add(new JScrollPane(columnList));

      add(columnBox);
      add(Box.createVerticalGlue());
    }
  }

  /**
   * Provides preference gui's for all the Formatting options
   * available for the columns etc.
   */
  private class FormattingPanel extends BasicPrefPanel
  {
    //~ Instance fields =======================================================

    private JTextField customFormatText = new JTextField("", 10);
    private JTextField loggerPrecision = new JTextField(10);
    private JRadioButton rdCustom = new JRadioButton("Custom Format ");
    private final JRadioButton rdISO =
      new JRadioButton(
        "<html><b>Fast</b> ISO 8601 format (yyyy-MM-dd HH:mm:ss) </html>");
    private final JTextField timeZone = new JTextField(10);
    private final JRadioButton rdLevelIcons = new JRadioButton("Icons ");
    private final JRadioButton rdLevelText = new JRadioButton("Text ");
    private final JCheckBox wrapMessage = new JCheckBox("Wrap message field (display multi-line rows) ");
    private final JCheckBox highlightSearchMatchText = new JCheckBox("Highlight search match text ");
    private JRadioButton rdLast;

    //~ Constructors ==========================================================

    private FormattingPanel()
    {
      super("Formatting");
      this.initComponents();
      setupListeners();
    }

    //~ Methods ===============================================================

    private void initComponents()
    {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      JPanel dateFormatPanel = new JPanel();
      dateFormatPanel.setLayout(new BoxLayout(dateFormatPanel, BoxLayout.Y_AXIS));
      dateFormatPanel.setBorder(
        BorderFactory.createTitledBorder(
          BorderFactory.createEtchedBorder(), "Timestamp"));

      ButtonGroup bgDateFormat = new ButtonGroup();

      rdISO.setSelected(preferenceModel.isUseISO8601Format());

      rdISO.setHorizontalTextPosition(SwingConstants.RIGHT);
      rdISO.setAlignmentX(Component.LEFT_ALIGNMENT);      

      bgDateFormat.add(rdISO);
      dateFormatPanel.add(rdISO);

      for (
        Iterator iter = LogPanelPreferenceModel.DATE_FORMATS.iterator();
          iter.hasNext();)
      {
        final String format = (String) iter.next();
        final JRadioButton rdFormat = new JRadioButton(format);
        rdFormat.setHorizontalTextPosition(SwingConstants.RIGHT);
        rdFormat.setAlignmentX(Component.LEFT_ALIGNMENT);      

        rdFormat.addActionListener(new ActionListener()
          {
            public void actionPerformed(ActionEvent e)
            {
              preferenceModel.setDateFormatPattern(format);
              customFormatText.setEnabled(rdCustom.isSelected());
              rdLast = rdFormat;
            }
          });
        //update based on external changes to dateformatpattern (column context
        //menu)
        preferenceModel.addPropertyChangeListener(
          "dateFormatPattern", new PropertyChangeListener()
          {
            public void propertyChange(PropertyChangeEvent evt)
            {
              rdFormat.setSelected(
                preferenceModel.getDateFormatPattern().equals(format));
              rdLast = rdFormat;
            }
          });

        dateFormatPanel.add(rdFormat);
        bgDateFormat.add(rdFormat);
      }

      customFormatText.setPreferredSize(new Dimension(100, 20));
      customFormatText.setMaximumSize(customFormatText.getPreferredSize());
      customFormatText.setMinimumSize(customFormatText.getPreferredSize());
      customFormatText.setEnabled(false);

      bgDateFormat.add(rdCustom);
      rdCustom.setSelected(preferenceModel.isCustomDateFormat());

      // add a custom date format
      if (preferenceModel.isCustomDateFormat())
      {
        customFormatText.setText(preferenceModel.getDateFormatPattern());
        customFormatText.setEnabled(true);
      }

      JPanel customPanel = new JPanel();
      customPanel.setLayout(new BoxLayout(customPanel, BoxLayout.X_AXIS));
      customPanel.add(rdCustom);
      customPanel.add(customFormatText);
      customPanel.setAlignmentX(Component.LEFT_ALIGNMENT);      

      dateFormatPanel.add(customPanel);
      dateFormatPanel.add(Box.createVerticalStrut(5));

      JLabel dateFormatLabel = new JLabel("Time zone of events (or blank for local time zone");
      dateFormatPanel.add(dateFormatLabel);

      timeZone.setMaximumSize(timeZone.getPreferredSize());
      dateFormatPanel.add(Box.createVerticalStrut(5));
      dateFormatPanel.add(timeZone);
      
      add(dateFormatPanel);

      JPanel levelFormatPanel = new JPanel();
      levelFormatPanel.setLayout(
        new BoxLayout(levelFormatPanel, BoxLayout.Y_AXIS));
      levelFormatPanel.setBorder(
        BorderFactory.createTitledBorder(
          BorderFactory.createEtchedBorder(), "Level"));
      levelFormatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

      ButtonGroup bgLevel = new ButtonGroup();
      bgLevel.add(rdLevelIcons);
      bgLevel.add(rdLevelText);

      rdLevelIcons.setSelected(preferenceModel.isLevelIcons());

      levelFormatPanel.add(rdLevelIcons);
      levelFormatPanel.add(rdLevelText);

      add(levelFormatPanel);
      add(wrapMessage);
      add(highlightSearchMatchText);

      JPanel loggerFormatPanel = new JPanel();
      loggerFormatPanel.setLayout(
        new BoxLayout(loggerFormatPanel, BoxLayout.Y_AXIS));
      loggerFormatPanel.setBorder(
        BorderFactory.createTitledBorder(
          BorderFactory.createEtchedBorder(), "Logger"));
      loggerFormatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

      loggerFormatPanel.add(Box.createVerticalStrut(3));

      final JLabel precisionLabel =
        new JLabel("Number of package levels to hide (or blank to display full logger)");

      loggerFormatPanel.add(precisionLabel);
      loggerFormatPanel.add(Box.createVerticalStrut(5));

      loggerPrecision.setMaximumSize(loggerPrecision.getPreferredSize());
      loggerFormatPanel.add(loggerPrecision);

      add(loggerFormatPanel);
    }

    /*
     * Restore text fields to current model values
     */
    private void reset() {

    	if (preferenceModel.isCustomDateFormat()) {
    		customFormatText.setText(preferenceModel.getDateFormatPattern());
    	} else {
    		if (rdLast != null) {
    			rdLast.setSelected(true);
    		}
    		customFormatText.setEnabled(false);
    	}
    	
    	loggerPrecision.setText(preferenceModel.getLoggerPrecision());
    	timeZone.setText(preferenceModel.getTimeZone());
    }

    /*
     * Commit text fields to model
     */
    private void commit() {
    	if (rdCustom.isSelected()) {
    		preferenceModel.setDateFormatPattern(customFormatText.getText());
    	}
    	preferenceModel.setLoggerPrecision(loggerPrecision.getText());
    	preferenceModel.setTimeZone(timeZone.getText());
    }

    /**
     * DOCUMENT ME!
    */
    private void setupListeners()
    {
      getOkButton().addActionListener(new ActionListener() {
    	  public void actionPerformed(ActionEvent evt) {
    		  commit();
    	  }
      });
      
      getCancelButton().addActionListener(new ActionListener() {
    	  public void actionPerformed(ActionEvent evt) {
    		  reset();
    	  }
      });
    	  
    	  rdCustom.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            customFormatText.setEnabled(rdCustom.isSelected());
            customFormatText.grabFocus();
          }
        });

      //a second?? listener for dateformatpattern
      preferenceModel.addPropertyChangeListener(
        "dateFormatPattern", new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent evt)
          {
            /**
             * we need to make sure we are not reacting to the user typing, so only do this
             * if the text box is not the same as the model
             */
            if (
              preferenceModel.isCustomDateFormat()
                && !customFormatText.getText().equals(
                  evt.getNewValue().toString()))
            {
              customFormatText.setText(preferenceModel.getDateFormatPattern());
              rdCustom.setSelected(true);
              customFormatText.setEnabled(true);
            }
            else
            {
              rdCustom.setSelected(false);
            }
          }
        });

      rdISO.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            preferenceModel.setDateFormatPattern("ISO8601");
            customFormatText.setEnabled(rdCustom.isSelected());
            rdLast = rdISO;
          }
        });
      preferenceModel.addPropertyChangeListener(
        "dateFormatPattern", new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent evt)
          {
            rdISO.setSelected(preferenceModel.isUseISO8601Format());
            rdLast = rdISO;
          }
        });
      preferenceModel.addPropertyChangeListener(
          "dateFormatTimeZone", new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent evt) {
                  timeZone.setText(preferenceModel.getTimeZone());
              }
          }
      );

      ActionListener levelIconListener = new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            preferenceModel.setLevelIcons(rdLevelIcons.isSelected());
          }
        };

      ActionListener wrapMessageListener = new ActionListener()
      {
          public void actionPerformed(ActionEvent e)
          {
              preferenceModel.setWrapMessage(wrapMessage.isSelected());
          }
      };

      wrapMessage.addActionListener(wrapMessageListener);

      ActionListener highlightSearchMatchTextListener = new ActionListener()
      {
          public void actionPerformed(ActionEvent e)
          {
              preferenceModel.setHighlightSearchMatchText(highlightSearchMatchText.isSelected());
          }
      };

      highlightSearchMatchText.addActionListener(highlightSearchMatchTextListener);

      rdLevelIcons.addActionListener(levelIconListener);
      rdLevelText.addActionListener(levelIconListener);

      preferenceModel.addPropertyChangeListener(
        "levelIcons", new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent evt)
          {
            boolean value = ((Boolean) evt.getNewValue()).booleanValue();
            rdLevelIcons.setSelected(value);
            rdLevelText.setSelected(!value);
          }
        });
        
        preferenceModel.addPropertyChangeListener(
          "wrapMessage", new PropertyChangeListener()
          {
            public void propertyChange(PropertyChangeEvent evt)
            {
              boolean value = ((Boolean) evt.getNewValue()).booleanValue();
              wrapMessage.setSelected(value);
            }
          });

        preferenceModel.addPropertyChangeListener(
          "highlightSearchMatchText", new PropertyChangeListener()
          {
            public void propertyChange(PropertyChangeEvent evt)
            {
              boolean value = ((Boolean) evt.getNewValue()).booleanValue();
              highlightSearchMatchText.setSelected(value);
            }
          });

    }
  }

  /**
   * DOCUMENT ME!
   *
   * @author $author$
   * @version $Revision$, $Date$
   *
   * @author psmith
   *
   */
  private class VisualsPrefPanel extends BasicPrefPanel
  {
    //~ Instance fields =======================================================

    private final JCheckBox detailPanelVisible =
      new JCheckBox("Show Event Detail panel");

    private final JCheckBox loggerTreePanel =
      new JCheckBox("Show Logger Tree");
    private final JCheckBox scrollToBottom =
      new JCheckBox("Scroll to bottom (view tracks with new events)");
    private final JCheckBox toolTips =
      new JCheckBox("Show Event Detail Tooltips");
    private final JCheckBox thumbnailBarToolTips =
      new JCheckBox("Show Thumbnail Bar Tooltips");
    private final JEditorPane clearTableExpression = new JEditorPane();

    //~ Constructors ==========================================================

    /**
     * Creates a new VisualsPrefPanel object.
    */
    private VisualsPrefPanel()
    {
      super("Visuals");
      initPanelComponents();
      setupListeners();
    }

    //~ Methods ===============================================================

    /**
     * DOCUMENT ME!
    */
    private void initPanelComponents()
    {
      JTextComponentFormatter.applySystemFontAndSize(clearTableExpression);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      toolTips.setAlignmentX(Component.LEFT_ALIGNMENT);
      thumbnailBarToolTips.setAlignmentX(Component.LEFT_ALIGNMENT);
      detailPanelVisible.setAlignmentX(Component.LEFT_ALIGNMENT);
      loggerTreePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      scrollToBottom.setAlignmentX(Component.LEFT_ALIGNMENT);
      add(toolTips);
      add(thumbnailBarToolTips);
      add(detailPanelVisible);
      add(loggerTreePanel);
      add(scrollToBottom);
      JPanel clearPanel = new JPanel(new BorderLayout());
      clearPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      clearPanel.add(new JLabel("Clear all events if expression matches"), BorderLayout.NORTH);
      clearTableExpression.setText(preferenceModel.getClearTableExpression());
      clearTableExpression.setPreferredSize(new Dimension(300, 50));
      JPanel clearTableScrollPanel = new JPanel(new BorderLayout());
      clearTableScrollPanel.add(new JScrollPane(clearTableExpression), BorderLayout.NORTH);
      clearPanel.add(clearTableScrollPanel, BorderLayout.CENTER);
      add(clearPanel);

      toolTips.setSelected(preferenceModel.isToolTips());
      thumbnailBarToolTips.setSelected(preferenceModel.isThumbnailBarToolTips());
      detailPanelVisible.setSelected(preferenceModel.isDetailPaneVisible());
      loggerTreePanel.setSelected(preferenceModel.isLogTreePanelVisible());
    }

    /**
     * DOCUMENT ME!
    */
    private void setupListeners()
    {
      toolTips.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            preferenceModel.setToolTips(toolTips.isSelected());
          }
        });

      thumbnailBarToolTips.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              preferenceModel.setThumbnailBarToolTips(thumbnailBarToolTips.isSelected());
          }
      });

      getOkButton().addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              preferenceModel.setClearTableExpression(clearTableExpression.getText().trim());
          }
      });

      preferenceModel.addPropertyChangeListener(
        "toolTips", new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent evt)
          {
            boolean value = ((Boolean) evt.getNewValue()).booleanValue();
            toolTips.setSelected(value);
          }
        });

    preferenceModel.addPropertyChangeListener(
      "thumbnailBarToolTips", new PropertyChangeListener()
      {
        public void propertyChange(PropertyChangeEvent evt)
        {
          boolean value = ((Boolean) evt.getNewValue()).booleanValue();
          thumbnailBarToolTips.setSelected(value);
        }
      });

      detailPanelVisible.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            preferenceModel.setDetailPaneVisible(detailPanelVisible.isSelected());
          }
        });

      preferenceModel.addPropertyChangeListener(
        "detailPaneVisible", new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent evt)
          {
            boolean value = ((Boolean) evt.getNewValue()).booleanValue();
            detailPanelVisible.setSelected(value);
          }
        });

      scrollToBottom.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            preferenceModel.setScrollToBottom(scrollToBottom.isSelected());
          }
        });

      preferenceModel.addPropertyChangeListener(
        "scrollToBottom", new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent evt)
          {
            boolean value = ((Boolean) evt.getNewValue()).booleanValue();
            scrollToBottom.setSelected(value);
          }
        });

      loggerTreePanel.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            preferenceModel.setLogTreePanelVisible(loggerTreePanel.isSelected());
          }
        });

      preferenceModel.addPropertyChangeListener(
        "logTreePanelVisible", new PropertyChangeListener()
        {
          public void propertyChange(PropertyChangeEvent evt)
          {
            boolean value = ((Boolean) evt.getNewValue()).booleanValue();
            loggerTreePanel.setSelected(value);
          }
        });

      preferenceModel.addPropertyChangeListener("columns", new PropertyChangeListener() {
        	public void propertyChange(PropertyChangeEvent evt) {
      	  List cols = (List)evt.getNewValue();
            for (
          	        Iterator iter = cols.iterator();
          	          iter.hasNext();)
          	      {
          	        TableColumn col = (TableColumn) iter.next();
                    Enumeration enumeration = columnListModel.elements();
                    boolean found = false;
                    while (enumeration.hasMoreElements()) {
                        TableColumn thisCol = (TableColumn) enumeration.nextElement();
                        if (thisCol.getHeaderValue().equals(col.getHeaderValue())) {
                            found = true;
                        }
                    }
          	        if (!found) {
          	        	columnListModel.addElement(col);
          	            columnListModel.fireContentsChanged();
          	        }
          	      }
        }});

        preferenceModel.addPropertyChangeListener(
                "visibleColumns", new PropertyChangeListener()
                {
                  public void propertyChange(PropertyChangeEvent evt)
                  {
                    columnListModel.fireContentsChanged();
                  }
                });

        preferenceModel.addPropertyChangeListener("clearTableExpression", new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent evt) {
                clearTableExpression.setText(((LogPanelPreferenceModel)evt.getSource()).getClearTableExpression());
            }
        });
    }
  }
}
