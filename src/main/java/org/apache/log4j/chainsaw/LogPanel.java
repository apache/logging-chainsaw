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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.Document;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.chainsaw.color.ColorPanel;
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.filter.FilterModel;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LineIconFactory;
import org.apache.log4j.chainsaw.layout.DefaultLayoutFactory;
import org.apache.log4j.chainsaw.layout.EventDetailLayout;
import org.apache.log4j.chainsaw.layout.LayoutEditorPane;
import org.apache.log4j.chainsaw.messages.MessageCenter;
import org.apache.log4j.chainsaw.prefs.LoadSettingsEvent;
import org.apache.log4j.chainsaw.prefs.Profileable;
import org.apache.log4j.chainsaw.prefs.SaveSettingsEvent;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.xstream.TableColumnConverter;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.LoggingEventFieldResolver;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * A LogPanel provides a view to a collection of LoggingEvents.<br>
 * <br>
 * As events are received, the keywords in the 'tab identifier' application
 * preference  are replaced with the values from the received event.  The
 * main application uses  this expression to route received LoggingEvents to
 * individual LogPanels which  match each event's resolved expression.<br>
 * <br>
 * The LogPanel's capabilities can be broken up into four areas:<br>
 * <ul><li> toolbar - provides 'find' and 'refine focus' features
 * <li> logger tree - displays a tree of the logger hierarchy, which can be used
 * to filter the display
 * <li> table - displays the events which pass the filtering rules
 * <li>detail panel - displays information about the currently selected event
 * </ul>
 * Here is a complete list of LogPanel's capabilities:<br>
 * <ul><li>display selected LoggingEvent row number and total LoggingEvent count
 * <li>pause or unpause reception of LoggingEvents
 * <li>configure, load and save column settings (displayed columns, order, width)
 * <li>configure, load and save color rules
 * filter displayed LoggingEvents based on the logger tree settings
 * <li>filter displayed LoggingEvents based on a 'refine focus' expression
 * (evaluates only those LoggingEvents which pass the logger tree filter
 * <li>colorize LoggingEvents based on expressions
 * <li>hide, show and configure the detail pane and tooltip
 * <li>configure the formatting of the logger, level and timestamp fields
 * <li>dock or undock
 * <li>table displays first line of exception, but when cell is clicked, a
 * popup opens to display the full stack trace
 * <li>find
 * <li>scroll to bottom
 * <li>sort
 * <li>provide a context menu which can be used to build color or display expressions
 * <li>hide or show the logger tree
 * <li>toggle the container storing the LoggingEvents to use either a
 * CyclicBuffer (defaults to max size of 5000,  but configurable  through
 * CHAINSAW_CAPACITY system property) or ArrayList (no max size)
 * <li>use the mouse context menu to 'best-fit' columns, define display
 * expression filters based on mouse location and access other capabilities
 *</ul>
 *
 *@see org.apache.log4j.chainsaw.color.ColorPanel
 *@see org.apache.log4j.rule.ExpressionRule
 *@see org.apache.log4j.spi.LoggingEventFieldResolver
 *
 *@author Scott Deboy (sdeboy at apache.org)
 *@author Paul Smith (psmith at apache.org)
 *@author Stephen Pain
 *@author Isuru Suriarachchi
 *
 */
public class LogPanel extends DockablePanel implements EventBatchListener,
  Profileable {
  private static final double DEFAULT_DETAIL_SPLIT_LOCATION = 0.56d;
  private static final double DEFAULT_LOG_TREE_SPLIT_LOCATION = 0.2d;
  private final String identifier;
  private final ChainsawStatusBar statusBar;
  private final JFrame preferencesFrame = new JFrame();
  private ColorPanel colorPanel;
  private final JFrame colorFrame = new JFrame();
  private final JFrame undockedFrame;
  private final DockablePanel externalPanel;
  private final Action dockingAction;
  private final JToolBar undockedToolbar;
  private final JSortTable table;
  private final TableColorizingRenderer renderer;
  private final EventContainer tableModel;
  private final ThrowableRenderPanel throwableRenderPanel;
  private final JEditorPane detail;
  private final JSplitPane lowerPanel;
  private final DetailPaneUpdater detailPaneUpdater;
  private final JPanel detailPanel = new JPanel(new BorderLayout());
  private final JSplitPane nameTreeAndMainPanelSplit;
  private final LoggerNameTreePanel logTreePanel;
  private final LogPanelPreferenceModel preferenceModel = new LogPanelPreferenceModel();
  private final LogPanelPreferencePanel preferencesPanel = new LogPanelPreferencePanel(preferenceModel);
  private final FilterModel filterModel = new FilterModel();
  private final RuleColorizer colorizer = new RuleColorizer();
  private final RuleMediator ruleMediator = new RuleMediator();
  private final EventDetailLayout detailLayout = new EventDetailLayout();
  private double lastDetailPanelSplitLocation = DEFAULT_DETAIL_SPLIT_LOCATION;
  private double lastLogTreePanelSplitLocation = DEFAULT_LOG_TREE_SPLIT_LOCATION;
  private Point currentPoint;
  private boolean paused = false;
  private Rule findRule;
  private String currentFindRuleText;
  private Rule findMarkerRule;
  private final JPanel findPanel;
  private JTextField findField;
  private final int dividerSize;
  static final String TABLE_COLUMN_ORDER = "table.columns.order";
  static final String TABLE_COLUMN_WIDTHS = "table.columns.widths";
  static final String COLORS_EXTENSION = ".colors";
  private static final int LOG_PANEL_SERIALIZATION_VERSION_NUMBER = 2; //increment when format changes
  private int previousLastIndex = -1;
  private final DateFormat timestampExpressionFormat = new SimpleDateFormat(Constants.TIMESTAMP_RULE_FORMAT);
  private final Logger logger = LogManager.getLogger(LogPanel.class);
  private static final Color INVALID_EXPRESSION_BACKGROUND = new Color(251, 186, 186);
  private TableCellEditor markerCellEditor;
  private AutoFilterComboBox filterCombo;
  private JScrollPane eventsPane;

    /**
   * Creates a new LogPanel object.  If a LogPanel with this identifier has
   * been loaded previously, reload settings saved on last exit.
   *
   * @param statusBar shared status bar, provided by main application
   * @param identifier used to load and save settings
   */
  public LogPanel(final ChainsawStatusBar statusBar, final String identifier, int cyclicBufferSize, Map allColorizers) {
    this.identifier = identifier;
    this.statusBar = statusBar;
    logger.debug("creating logpanel for " + identifier);

    setLayout(new BorderLayout());
    findPanel = new JPanel();
    findPanel.setLayout(new BoxLayout(findPanel, BoxLayout.X_AXIS));
    Dimension findPanelSize = new Dimension(310, 30);
    findPanel.setPreferredSize(findPanelSize);
    findPanel.setMaximumSize(findPanelSize);
    findPanel.setMinimumSize(findPanelSize);

    final Map columnNameKeywordMap = new HashMap();
    columnNameKeywordMap.put(
      ChainsawConstants.CLASS_COL_NAME, LoggingEventFieldResolver.CLASS_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.FILE_COL_NAME, LoggingEventFieldResolver.FILE_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.LEVEL_COL_NAME, LoggingEventFieldResolver.LEVEL_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.LINE_COL_NAME, LoggingEventFieldResolver.LINE_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.LOGGER_COL_NAME, LoggingEventFieldResolver.LOGGER_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.NDC_COL_NAME, LoggingEventFieldResolver.NDC_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.MESSAGE_COL_NAME, LoggingEventFieldResolver.MSG_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.THREAD_COL_NAME, LoggingEventFieldResolver.THREAD_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.THROWABLE_COL_NAME,
      LoggingEventFieldResolver.EXCEPTION_FIELD);
    columnNameKeywordMap.put(
      ChainsawConstants.TIMESTAMP_COL_NAME,
      LoggingEventFieldResolver.TIMESTAMP_FIELD);

    preferencesFrame.setTitle("'" + identifier + "' Log Panel Preferences");
    preferencesFrame.setIconImage(
      ((ImageIcon) ChainsawIcons.ICON_PREFERENCES).getImage());
    preferencesFrame.getContentPane().add(preferencesPanel);

    preferencesFrame.setSize(640, 480);

    preferencesPanel.setOkCancelActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          preferencesFrame.setVisible(false);
        }
      });

    setDetailPaneConversionPattern(
      DefaultLayoutFactory.getDefaultPatternLayout());
      detailLayout.setConversionPattern(
      DefaultLayoutFactory.getDefaultPatternLayout());

    undockedFrame = new JFrame(identifier);
    undockedFrame.setDefaultCloseOperation(
      WindowConstants.DO_NOTHING_ON_CLOSE);

    if (ChainsawIcons.UNDOCKED_ICON != null) {
      undockedFrame.setIconImage(
        new ImageIcon(ChainsawIcons.UNDOCKED_ICON).getImage());
    }

    externalPanel = new DockablePanel();
    externalPanel.setLayout(new BorderLayout());

    undockedFrame.addWindowListener(
      new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          dock();
        }
      });

    undockedToolbar = createDockwindowToolbar();
    externalPanel.add(undockedToolbar, BorderLayout.NORTH);
    undockedFrame.getContentPane().add(externalPanel);
    undockedFrame.setSize(new Dimension(1024, 768));
    undockedFrame.pack();

    /*
     * Menus on which the preferencemodels rely
     */

    /**
     * Setup a popup menu triggered for Timestamp column to allow time stamp
     * format changes
     */
    final JPopupMenu dateFormatChangePopup = new JPopupMenu();
    final JRadioButtonMenuItem isoButton =
      new JRadioButtonMenuItem(
        new AbstractAction("Use ISO8601Format") {
          public void actionPerformed(ActionEvent e) {
            preferenceModel.setDateFormatPattern("ISO8601");
          }
        });
    final JRadioButtonMenuItem simpleTimeButton =
      new JRadioButtonMenuItem(
        new AbstractAction("Use simple time") {
          public void actionPerformed(ActionEvent e) {
            preferenceModel.setDateFormatPattern("HH:mm:ss");
          }
        });

    ButtonGroup dfBG = new ButtonGroup();
    dfBG.add(isoButton);
    dfBG.add(simpleTimeButton);
    simpleTimeButton.setSelected(true);
    dateFormatChangePopup.add(isoButton);
    dateFormatChangePopup.add(simpleTimeButton);

    final JCheckBoxMenuItem menuItemToggleToolTips =
      new JCheckBoxMenuItem("Show ToolTips");
    menuItemToggleToolTips.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          preferenceModel.setToolTips(menuItemToggleToolTips.isSelected());
        }
      });
    menuItemToggleToolTips.setIcon(new ImageIcon(ChainsawIcons.TOOL_TIP));

    final JCheckBoxMenuItem menuItemLoggerTree =
      new JCheckBoxMenuItem("Show Logger Tree panel");
    menuItemLoggerTree.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          preferenceModel.setLogTreePanelVisible(
            menuItemLoggerTree.isSelected());
        }
      });
    menuItemLoggerTree.setIcon(new ImageIcon(ChainsawIcons.WINDOW_ICON));

    final JMenuItem menuItemScrollToTop = new JMenuItem("Scroll to top");
    menuItemScrollToTop.addActionListener(
      new ActionListener() {
          public void actionPerformed(ActionEvent evt)
          {
              scrollToTop();
          }
      });
    final JCheckBoxMenuItem menuItemScrollBottom =
      new JCheckBoxMenuItem("Scroll to bottom");
    menuItemScrollBottom.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          preferenceModel.setScrollToBottom(menuItemScrollBottom.isSelected());
        }
      });
    menuItemScrollBottom.setSelected(isScrollToBottom());

    menuItemScrollBottom.setIcon(
      new ImageIcon(ChainsawIcons.SCROLL_TO_BOTTOM));

    final JCheckBoxMenuItem menuItemToggleDetails =
      new JCheckBoxMenuItem("Show Detail Pane");
    menuItemToggleDetails.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          preferenceModel.setDetailPaneVisible(
            menuItemToggleDetails.isSelected());
        }
      });

    menuItemToggleDetails.setIcon(new ImageIcon(ChainsawIcons.INFO));

    /*
     * add preferencemodel listeners
     */
    preferenceModel.addPropertyChangeListener(
      "levelIcons",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          renderer.setLevelUseIcons(
            ((Boolean) evt.getNewValue()).booleanValue());
          table.tableChanged(new TableModelEvent(tableModel));
        }
      });

    preferenceModel.addPropertyChangeListener(
      "detailPaneVisible",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          boolean newValue = ((Boolean) evt.getNewValue()).booleanValue();

          if (newValue) {
            showDetailPane();
          } else {
            hideDetailPane();
          }
        }
      });

    preferenceModel.addPropertyChangeListener(
      "logTreePanelVisible",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          boolean newValue = ((Boolean) evt.getNewValue()).booleanValue();

          if (newValue) {
            showLogTreePanel();
          } else {
            hideLogTreePanel();
          }
        }
      });
    
    preferenceModel.addPropertyChangeListener(
      "toolTips",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          renderer.setToolTipsVisible(
            ((Boolean) evt.getNewValue()).booleanValue());
        }
      });

    preferenceModel.addPropertyChangeListener(
      "visibleColumns",
      new PropertyChangeListener() {
    	public void propertyChange(PropertyChangeEvent evt) {
    		//remove all columns and re-add visible
            TableColumnModel columnModel = table.getColumnModel();
            while (columnModel.getColumnCount() > 0) {
                columnModel.removeColumn(columnModel.getColumn(0)); 
    		}
            for (Iterator iter = preferenceModel.getVisibleColumnOrder().iterator();iter.hasNext();) {
    			TableColumn c = (TableColumn)iter.next();
    			columnModel.addColumn(c);
    		}
    	}
      });

    PropertyChangeListener datePrefsChangeListener =
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          LogPanelPreferenceModel model =
            (LogPanelPreferenceModel) evt.getSource();

          isoButton.setSelected(model.isUseISO8601Format());
          simpleTimeButton.setSelected(
            !model.isUseISO8601Format() && !model.isCustomDateFormat());

          if (model.getTimeZone() != null) {
            renderer.setTimeZone(model.getTimeZone());
          }
          
          if (model.isUseISO8601Format()) {
            renderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
          } else {
      		try {
            renderer.setDateFormatter(
              new SimpleDateFormat(model.getDateFormatPattern()));
            		} catch (IllegalArgumentException iae) {
            			model.setDefaultDatePatternFormat();
                        renderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
            		}
          }

          table.tableChanged(new TableModelEvent(tableModel));
        }
      };

    preferenceModel.addPropertyChangeListener(
      "dateFormatPattern", datePrefsChangeListener);
    preferenceModel.addPropertyChangeListener("dateFormatTimeZone", datePrefsChangeListener);

    preferenceModel.addPropertyChangeListener(
      "loggerPrecision",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          LogPanelPreferenceModel model =
            (LogPanelPreferenceModel) evt.getSource();

          renderer.setLoggerPrecision(model.getLoggerPrecision());

          table.tableChanged(new TableModelEvent(tableModel));
        }
      });

    preferenceModel.addPropertyChangeListener(
      "toolTips",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          boolean value = ((Boolean) evt.getNewValue()).booleanValue();
          menuItemToggleToolTips.setSelected(value);
        }
      });

    preferenceModel.addPropertyChangeListener(
      "logTreePanelVisible",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          boolean value = ((Boolean) evt.getNewValue()).booleanValue();
          menuItemLoggerTree.setSelected(value);
        }
      });

    preferenceModel.addPropertyChangeListener(
      "scrollToBottom",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          boolean value = ((Boolean) evt.getNewValue()).booleanValue();
          menuItemScrollBottom.setSelected(value);
          if (value) {
            scrollToBottom();
          }
        }
      });

    preferenceModel.addPropertyChangeListener(
      "detailPaneVisible",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          boolean value = ((Boolean) evt.getNewValue()).booleanValue();
          menuItemToggleDetails.setSelected(value);
        }
      });

    /*
     *End of preferenceModel listeners
     */
    tableModel = new ChainsawCyclicBufferTableModel(cyclicBufferSize, colorizer);
    table = new JSortTable(tableModel);

    //we've mapped f2, shift f2 and ctrl-f2 to marker-related actions, unmap them from the table
    table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F2"), "none");
    table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.SHIFT_MASK), "none");
    table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.CTRL_MASK), "none");
    table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK), "none");

    //we're also mapping ctrl-a to scroll-to-top, unmap from the table
    table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK), "none");
        
    //add a listener to update the 'refine focus'
    tableModel.addNewKeyListener(new NewKeyListener() {
		public void newKeyAdded(NewKeyEvent e) {
            columnNameKeywordMap.put(e.getKey(), "PROP." + e.getKey());
		}
    });

    /*
     * Set the Display rule to use the mediator, the model will add itself as
     * a property change listener and update itself when the rule changes.
     */
    tableModel.setDisplayRule(ruleMediator);

    tableModel.addEventCountListener(
      new EventCountListener() {
        public void eventCountChanged(int currentCount, int totalCount) {
          if (LogPanel.this.isVisible()) {
            statusBar.setSelectedLine(
              table.getSelectedRow() + 1, currentCount, totalCount);
          }
        }
      });

    tableModel.addEventCountListener(
      new EventCountListener() {
        final NumberFormat formatter = NumberFormat.getPercentInstance();
        boolean warning75 = false;
        boolean warning100 = false;

        public void eventCountChanged(int currentCount, int totalCount) {
          if (tableModel.isCyclic()) {
            double percent =
              ((double) totalCount) / ((ChainsawCyclicBufferTableModel) tableModel)
              .getMaxSize();
            String msg = null;

            if ((percent > 0.75) && (percent < 1.0) && !warning75) {
              msg =
                "Warning :: " + formatter.format(percent) + " of the '"
                + getIdentifier() + "' buffer has been used";
              warning75 = true;
            } else if ((percent >= 1.0) && !warning100) {
              msg =
                "Warning :: " + formatter.format(percent) + " of the '"
                + getIdentifier()
                + "' buffer has been used.  Older events are being discarded.";
              warning100 = true;
            }

            if (msg != null) {
              MessageCenter.getInstance().getLogger().info(msg);
            }
          }
        }
      });

    /*
     * Logger tree panel
     *
     */
    LogPanelLoggerTreeModel logTreeModel = new LogPanelLoggerTreeModel();
    logTreePanel = new LoggerNameTreePanel(logTreeModel, preferenceModel);
    logTreePanel.addPropertyChangeListener("searchExpression", new PropertyChangeListener()
    {
        public void propertyChange(PropertyChangeEvent evt)
        {
            findField.setText(evt.getNewValue().toString());
            findNext();
        }
    });
      
    tableModel.addLoggerNameListener(logTreeModel);

    /**
     * Set the LoggerRule to be the LoggerTreePanel, as this visual component
     * is a rule itself, and the RuleMediator will automatically listen when
     * it's rule state changes.
     */
    ruleMediator.setLoggerRule(logTreePanel);
    colorizer.setLoggerRule(logTreePanel.getLoggerColorRule());

    /*
     * Color rule frame and panel
     */
    colorFrame.setTitle("'" + identifier + "' Color Filter");
    colorFrame.setIconImage(
      ((ImageIcon) ChainsawIcons.ICON_PREFERENCES).getImage());

    allColorizers.put(identifier, colorizer);
    colorPanel = new ColorPanel(colorizer, filterModel, allColorizers);

    colorFrame.getContentPane().add(colorPanel);

    colorPanel.setCloseActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          colorFrame.setVisible(false);
        }
      });

    colorizer.addPropertyChangeListener(
      "colorrule",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          if (table != null) {
            table.repaint();
          }
        }
      });

    /*
     * Table definition.  Actual construction is above (next to tablemodel)
     */
    table.setRowHeight(20);
    table.setShowGrid(false);

    table.getColumnModel().addColumnModelListener(
      new ChainsawTableColumnModelListener());

    table.setAutoCreateColumnsFromModel(false);

    table.addMouseMotionListener(new TableColumnDetailMouseListener());
    table.addMouseListener(new TableMarkerListener());

    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    //set valueisadjusting if holding down a key - don't process setdetail events
    table.addKeyListener(
      new KeyListener() {
        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
          synchronized (detail) {
            table.getSelectionModel().setValueIsAdjusting(true);
            detail.notify();
          }
        }

        public void keyReleased(KeyEvent e) {
          synchronized (detail) {
            table.getSelectionModel().setValueIsAdjusting(false);
            detail.notify();
          }
        }
      });

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent evt) {
            if (((evt.getFirstIndex() == evt.getLastIndex())
                && (evt.getFirstIndex() > 0) && previousLastIndex != -1) || (evt.getValueIsAdjusting())) {
              return;
            }
            boolean lastIndexOnLastRow = (evt.getLastIndex() == (table.getRowCount() - 1));
            boolean lastIndexSame = (previousLastIndex == evt.getLastIndex());

            /*
             * when scroll-to-bottom is active, here is what events look like:
             * rowcount-1: 227, last: 227, previous last: 191..first: 191
             *
             * when the user has unselected the bottom row, here is what the events look like:
             * rowcount-1: 227, last: 227, previous last: 227..first: 222
             *
             * note: previouslast is set after it is evaluated in the bypass scroll check
            */
           //System.out.println("rowcount: " + (table.getRowCount() - 1) + ", last: " + evt.getLastIndex() +", previous last: " + previousLastIndex + "..first: " + evt.getFirstIndex() + ", isadjusting: " + evt.getValueIsAdjusting());

            boolean disableScrollToBottom = (lastIndexOnLastRow && lastIndexSame && previousLastIndex != evt.getFirstIndex());
            if (disableScrollToBottom && isScrollToBottom() && table.getRowCount() > 0) {
              preferenceModel.setScrollToBottom(false);
            }
            previousLastIndex = evt.getLastIndex();
          }
        }
    );

    table.getSelectionModel().addListSelectionListener(
      new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent evt) {
          if (((evt.getFirstIndex() == evt.getLastIndex())
              && (evt.getFirstIndex() > 0) && previousLastIndex != -1) || (evt.getValueIsAdjusting())) {
            return;
          }

          final ListSelectionModel lsm = (ListSelectionModel) evt.getSource();

          if (lsm.isSelectionEmpty()) {
            if (isVisible()) {
              statusBar.setNothingSelected();
            }

            if (detail.getDocument().getDefaultRootElement() != null) {
              detailPaneUpdater.setSelectedRow(-1);
            }
          } else {
            if (table.getSelectedRow() > -1) {
              int selectedRow = table.getSelectedRow();

              if (isVisible()) {
                updateStatusBar();
              }

              try {
                if (tableModel.getRowCount() >= selectedRow) {
                  detailPaneUpdater.setSelectedRow(table.getSelectedRow());
                } else {
                  detailPaneUpdater.setSelectedRow(-1);
                }
              } catch (Exception e) {
                e.printStackTrace();
                detailPaneUpdater.setSelectedRow(-1);
              }
            }
          }
        }
      });

    renderer = new TableColorizingRenderer(colorizer);
    renderer.setToolTipsVisible(preferenceModel.isToolTips());

    table.setDefaultRenderer(Object.class, renderer);

    /*
     * Throwable popup
     */
    throwableRenderPanel = new ThrowableRenderPanel();
    markerCellEditor = new MarkerCellEditor();

    final JDialog detailDialog = new JDialog((JFrame) null, true);
    Container container = detailDialog.getContentPane();
    final JTextArea detailArea = new JTextArea(10, 40);
    detailArea.setEditable(false);
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    container.add(new JScrollPane(detailArea));

    detailDialog.pack();

    throwableRenderPanel.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Object o = table.getValueAt(
            table.getSelectedRow(), table.getSelectedColumn());
          if (o == null) {
            //no row selected - ignore
          	logger.debug("no row selected - unable to display throwable popup");
            return;
          }
          detailDialog.setTitle(
            table.getColumnName(table.getSelectedColumn()) + " detail...");

          if (o instanceof String[]) {
            StringBuffer buf = new StringBuffer();
            String[] ti = (String[]) o;
            buf.append(ti[0]).append("\n");

            for (int i = 1; i < ti.length; i++) {
              buf.append(ti[i]).append("\n    ");
            }

            detailArea.setText(buf.toString());
          } else {
            detailArea.setText(o.toString());
          }

          detailDialog.setLocation(lowerPanel.getLocationOnScreen());
          SwingHelper.invokeOnEDT(new Runnable() {
              public void run() {
                detailDialog.setVisible(true);
              }
            });
        }
      });

    /*
     * We listen for new Key's coming in so we can get them automatically
     * added as columns
     */
    tableModel.addNewKeyListener(
      new NewKeyListener() {
        public void newKeyAdded(final NewKeyEvent e) {
        	SwingHelper.invokeOnEDT(new Runnable() {
        		public void run() {
           // don't add the column if we already know about it, this could be if we've seen it before and saved the column preferences
            //this may throw an illegalargexception - ignore it because we need to add only if not already added
        	//if the column is already added, don't add again
        	
        	try {
        	if(table.getColumn(e.getKey())!=null){
                return;
            } 
        	} catch (IllegalArgumentException iae) {}
          TableColumn col = new TableColumn(e.getNewModelIndex());
          col.setHeaderValue(e.getKey());

          if (preferenceModel.addColumn(col)) {
        	  table.addColumn(col);
        	  preferenceModel.setColumnVisible(e.getKey().toString(), true);
          }
        		}
        	});
        }
      });

    tableModel.addPropertyChangeListener(
      "cyclic",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent arg0) {
          if (tableModel.isCyclic()) {
            MessageCenter.getInstance().getLogger().warn(
              "Changed to Cyclic Mode. Maximum # events kept: "
              + tableModel.getMaxSize());
          } else {
            MessageCenter.getInstance().getLogger().warn(
              "Changed to Unlimited Mode. Warning, you may run out of memory.");
          }
        }
      });

    table.getTableHeader().addMouseListener(
      new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          checkEvent(e);
        }

        public void mousePressed(MouseEvent e) {
          checkEvent(e);
        }

        public void mouseReleased(MouseEvent e) {
          checkEvent(e);
        }

        private void checkEvent(MouseEvent e) {
          if (e.isPopupTrigger()) {
            TableColumnModel colModel = table.getColumnModel();
            int index = colModel.getColumnIndexAtX(e.getX());
            int modelIndex = colModel.getColumn(index).getModelIndex();

            if ((modelIndex + 1) == ChainsawColumns.INDEX_TIMESTAMP_COL_NAME) {
              dateFormatChangePopup.show(e.getComponent(), e.getX(), e.getY());
            }
          }
        }
      });

    /*
     * Upper panel definition
     */
    JPanel upperPanel = new JPanel(new BorderLayout());
    upperPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));

    final JLabel filterLabel = new JLabel("Refine focus on: ");
    filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));
    filterLabel.setDisplayedMnemonic('k');

    JPanel upperLeftPanel = new JPanel();
    upperLeftPanel.setLayout(new BoxLayout(upperLeftPanel, BoxLayout.X_AXIS));
    upperLeftPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
    upperLeftPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

    upperLeftPanel.add(filterLabel);

    //add (hopefully useful) default filters
    Vector filterExpressionVector = new Vector();
    filterExpressionVector.add("LEVEL == TRACE");
    filterExpressionVector.add("LEVEL >= DEBUG");
    filterExpressionVector.add("LEVEL >= INFO");
    filterExpressionVector.add("LEVEL >= WARN");
    filterExpressionVector.add("LEVEL >= ERROR");
    filterExpressionVector.add("LEVEL == FATAL");
    
    filterCombo = new AutoFilterComboBox(filterExpressionVector);
    filterCombo.setSelectedIndex(-1);
    final JTextField filterText =(JTextField) filterCombo.getEditor().getEditorComponent();
    filterText.getDocument().addDocumentListener(new DelayedFilterTextDocumentListener(filterText));
    filterText.setToolTipText("Enter an expression, press enter to add to list");
    filterText.addKeyListener(new ExpressionRuleContext(filterModel, filterText));

    if (filterCombo.getEditor().getEditorComponent() instanceof JTextField) {
      filterCombo.addActionListener(
        new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("comboBoxEdited")) {
              try {
                //verify the expression is valid
                  Object item = filterCombo.getSelectedItem();
                  if (item != null && !item.toString().trim().equals("")) {
                    ExpressionRule.getRule(item.toString());
                    //add entry as first row of the combo box
                    filterCombo.insertItemAt(item, 0);
                  }
                //valid expression, reset background color in case we were previously an invalid expression
                filterText.setBackground(UIManager.getColor("TextField.background"));
              } catch (IllegalArgumentException iae) {
                  //don't add expressions that aren't valid
                  //invalid expression, change background of the field
                  filterText.setToolTipText(iae.getMessage());
                  filterText.setBackground(INVALID_EXPRESSION_BACKGROUND);
                return;
              }
            }
          }
        });
    }
      upperPanel.add(filterCombo, BorderLayout.CENTER);
      filterLabel.setLabelFor(filterCombo);

    upperPanel.add(upperLeftPanel, BorderLayout.WEST);

    JPanel upperRightPanel =
      new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    //Adding a button to clear filter expressions which are currently remembered by Chainsaw...
    final JButton clearButton = new JButton("Clear expression");
    clearButton.setToolTipText("Click here to remove the selected expression from the list");
    clearButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent e){
                	Object selectedItem = filterCombo.getSelectedItem();
                    if (e.getSource() == clearButton && selectedItem != null && !selectedItem.toString().trim().equals("")){
                        //don't just remove the entry from the store, clear the refine focus field
                        filterText.setText(null);
                        int index = filterCombo.getSelectedIndex();
                        filterCombo.setSelectedIndex(-1);
                        filterCombo.removeItemAt(index);
                    }
                }
            }
    );

    upperRightPanel.add(clearButton);

    upperPanel.add(upperRightPanel, BorderLayout.EAST);

    /*
     * Detail pane definition
     */
    detail = new JEditorPane(ChainsawConstants.DETAIL_CONTENT_TYPE, "");
    detail.setEditable(false);

    detailPaneUpdater = new DetailPaneUpdater();

    //if the panel gets focus, update the detail pane
    addFocusListener(new FocusListener() {

        public void focusGained(FocusEvent e) {
            detailPaneUpdater.updateDetailPane();
        }

        public void focusLost(FocusEvent e) {
            
        }
    });
    findMarkerRule = ExpressionRule.getRule("prop." + ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE + " exists");
        
    tableModel.addTableModelListener(new TableModelListener() {
		public void tableChanged(TableModelEvent e) {
			detailPaneUpdater.setSelectedRow(table.getSelectedRow());
		}
    });
    
    addPropertyChangeListener(
      "detailPaneConversionPattern", detailPaneUpdater);

    final JScrollPane detailPane = new JScrollPane(detail);

    detailPane.setPreferredSize(new Dimension(900, 50));

    detailPanel.add(detailPane, BorderLayout.CENTER);

    JPanel eventsAndStatusPanel = new JPanel(new BorderLayout());

    eventsPane = new JScrollPane(table);

    eventsAndStatusPanel.add(eventsPane, BorderLayout.CENTER);
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
    JPanel thumbNailPanel = new EventMatchThumbnail();

    //set thumbnail width to be a bit narrower than scrollbar width
    thumbNailPanel.setPreferredSize(new Dimension(((Integer)UIManager.get("ScrollBar.width")).intValue() -4, -1));
    rightPanel.add(thumbNailPanel);
    eventsAndStatusPanel.add(rightPanel, BorderLayout.EAST);

    final JPanel statusLabelPanel = new JPanel();
    statusLabelPanel.setLayout(new BorderLayout());

    statusLabelPanel.add(upperPanel, BorderLayout.CENTER);
    eventsAndStatusPanel.add(statusLabelPanel, BorderLayout.NORTH);

    lowerPanel =
      new JSplitPane(
        JSplitPane.VERTICAL_SPLIT, eventsAndStatusPanel, detailPanel);

    dividerSize = lowerPanel.getDividerSize();
    lowerPanel.setDividerLocation(-1);

    lowerPanel.setResizeWeight(1.0);
    lowerPanel.setBorder(null);
    lowerPanel.setContinuousLayout(true);

    if (preferenceModel.isDetailPaneVisible()) {
      showDetailPane();
    } else {
      hideDetailPane();
    }
    
    /*
     * Detail panel layout editor
     */
    final JToolBar detailToolbar = new JToolBar(SwingConstants.HORIZONTAL);
    detailToolbar.setFloatable(false);

    final LayoutEditorPane layoutEditorPane = new LayoutEditorPane();
    final JDialog layoutEditorDialog =
      new JDialog((JFrame) null, "Pattern Editor");
    layoutEditorDialog.getContentPane().add(layoutEditorPane);
    layoutEditorDialog.setSize(640, 480);

    layoutEditorPane.addCancelActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          layoutEditorDialog.setVisible(false);
        }
      });

    layoutEditorPane.addOkActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setDetailPaneConversionPattern(
            layoutEditorPane.getConversionPattern());
          layoutEditorDialog.setVisible(false);
        }
      });

    Action copyToRefineFocusAction = new AbstractAction("Set 'refine focus' field") {
        public void actionPerformed(ActionEvent e) {
            String selectedText = detail.getSelectedText();
            if (selectedText == null || selectedText.equals("")) {
                //no-op empty searches
                return;
            }
            filterText.setText("msg ~= '" + selectedText + "'");
        }
    };

    Action copyToSearchAction = new AbstractAction("Find next") {
        public void actionPerformed(ActionEvent e) {
            String selectedText = detail.getSelectedText();
            if (selectedText == null || selectedText.equals("")) {
                //no-op empty searches
                return;
            }
            findField.setText("msg ~= '" + selectedText + "'");
            findNext();
        }
    };

    Action editDetailAction =
      new AbstractAction(
        "Edit...", new ImageIcon(ChainsawIcons.ICON_EDIT_RECEIVER)) {
        public void actionPerformed(ActionEvent e) {
          layoutEditorPane.setConversionPattern(
            getDetailPaneConversionPattern());

          Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
          Point p =
            new Point(
              ((int) ((size.getWidth() / 2)
              - (layoutEditorDialog.getSize().getWidth() / 2))),
              ((int) ((size.getHeight() / 2)
              - (layoutEditorDialog.getSize().getHeight() / 2))));
          layoutEditorDialog.setLocation(p);

          layoutEditorDialog.setVisible(true);
        }
      };

    editDetailAction.putValue(
      Action.SHORT_DESCRIPTION,
      "opens a Dialog window to Edit the Pattern Layout text");

    final SmallButton editDetailButton = new SmallButton(editDetailAction);
    editDetailButton.setText(null);
    detailToolbar.add(Box.createHorizontalGlue());
    detailToolbar.add(editDetailButton);
    detailToolbar.addSeparator();
    detailToolbar.add(Box.createHorizontalStrut(5));

    Action closeDetailAction =
      new AbstractAction(null, LineIconFactory.createCloseIcon()) {
        public void actionPerformed(ActionEvent arg0) {
          preferenceModel.setDetailPaneVisible(false);
        }
      };

    closeDetailAction.putValue(
      Action.SHORT_DESCRIPTION, "Hides the Detail Panel");

    SmallButton closeDetailButton = new SmallButton(closeDetailAction);
    detailToolbar.add(closeDetailButton);

    detailPanel.add(detailToolbar, BorderLayout.NORTH);

    JPopupMenu editDetailPopupMenu = new JPopupMenu();

    editDetailPopupMenu.add(copyToRefineFocusAction);
    editDetailPopupMenu.add(copyToSearchAction);
    editDetailPopupMenu.addSeparator();

    editDetailPopupMenu.add(editDetailAction);
    editDetailPopupMenu.addSeparator();

    final ButtonGroup layoutGroup = new ButtonGroup();

    JRadioButtonMenuItem defaultLayoutRadio =
      new JRadioButtonMenuItem(
        new AbstractAction("Set to Default Layout") {
          public void actionPerformed(ActionEvent e) {
            setDetailPaneConversionPattern(
              DefaultLayoutFactory.getDefaultPatternLayout());
          }
        });

        JRadioButtonMenuItem fullLayoutRadio =
          new JRadioButtonMenuItem(
            new AbstractAction("Set to Full Layout") {
              public void actionPerformed(ActionEvent e) {
                setDetailPaneConversionPattern(
                  DefaultLayoutFactory.getFullPatternLayout());
              }
            });

    editDetailPopupMenu.add(defaultLayoutRadio);
    editDetailPopupMenu.add(fullLayoutRadio);

    layoutGroup.add(defaultLayoutRadio);
    layoutGroup.add(fullLayoutRadio);
    defaultLayoutRadio.setSelected(true);

    JRadioButtonMenuItem tccLayoutRadio =
      new JRadioButtonMenuItem(
        new AbstractAction("Set to TCCLayout") {
          public void actionPerformed(ActionEvent e) {
            setDetailPaneConversionPattern(
              PatternLayout.TTCC_CONVERSION_PATTERN);
          }
        });
    editDetailPopupMenu.add(tccLayoutRadio);
    layoutGroup.add(tccLayoutRadio);

    PopupListener editDetailPopupListener =
      new PopupListener(editDetailPopupMenu);
    detail.addMouseListener(editDetailPopupListener);

    /*
     * Logger tree splitpane definition
     */
    nameTreeAndMainPanelSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logTreePanel, lowerPanel);
    
    nameTreeAndMainPanelSplit.setToolTipText("Still under development....");
    nameTreeAndMainPanelSplit.setDividerLocation(-1);

    add(nameTreeAndMainPanelSplit, BorderLayout.CENTER);

    if (isLogTreeVisible()) {
        showLogTreePanel();
    } else {
        hideLogTreePanel();
    }

    /*
     * Other menu items
     */
    final JMenuItem menuItemBestFit = new JMenuItem("Best fit column");
    menuItemBestFit.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          if (currentPoint != null) {
            int column = table.columnAtPoint(currentPoint);
            int maxWidth = getMaxColumnWidth(column);
            table.getColumnModel().getColumn(column).setPreferredWidth(
              maxWidth);
          }
        }
      });

    JMenuItem menuItemColorPanel = new JMenuItem("LogPanel Color Filter...");
    menuItemColorPanel.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          showColorPreferences();
        }
      });
    menuItemColorPanel.setIcon(ChainsawIcons.ICON_PREFERENCES);

    JMenuItem menuItemLogPanelPreferences =
      new JMenuItem("LogPanel Preferences...");
    menuItemLogPanelPreferences.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          showPreferences();
        }
      });
    menuItemLogPanelPreferences.setIcon(ChainsawIcons.ICON_PREFERENCES);

    final JMenuItem menuItemFocusOn =
      new JMenuItem("Set 'refine focus' field");
    menuItemFocusOn.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          if (currentPoint != null) {
            String operator = "==";
            int column = table.columnAtPoint(currentPoint);
            int row = table.rowAtPoint(currentPoint);
            String colName = table.getColumnName(column).toUpperCase();
            String value = "";

            if (colName.equalsIgnoreCase(ChainsawConstants.TIMESTAMP_COL_NAME)) {
            	value = timestampExpressionFormat.format(new Date(table.getValueAt(row, column).toString()));
            } else {
              Object o = table.getValueAt(row, column);

              if (o != null) {
                if (o instanceof String[]) {
                  value = ((String[]) o)[0];
                  operator = "~=";
                } else {
                  value = o.toString();
                }
              }
            }

            if (columnNameKeywordMap.containsKey(colName)) {
              filterText.setText(
                columnNameKeywordMap.get(colName).toString() + " " + operator
                + " '" + value + "'");
            }
          }
        }
      });

    final JMenuItem menuDefineAddCustomFilter =
      new JMenuItem("Add to 'refine focus' field");
    menuDefineAddCustomFilter.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          if (currentPoint != null) {
            String operator = "==";
            int column = table.columnAtPoint(currentPoint);
            int row = table.rowAtPoint(currentPoint);
            String colName = table.getColumnName(column).toUpperCase();
            String value = "";

            if (colName.equalsIgnoreCase(ChainsawConstants.TIMESTAMP_COL_NAME)) {
              JComponent comp =
                (JComponent) table.getCellRenderer(row, column);

              if (comp instanceof JLabel) {
                value = ((JLabel) comp).getText();
              }
            } else {
              Object o = table.getValueAt(row, column).toString();

              if (o instanceof String[]) {
                value = ((String[]) o)[0];
                operator = "~=";
              } else {
                value = o.toString();
              }
            }

            if (columnNameKeywordMap.containsKey(colName)) {
              filterText.setText(
                filterText.getText() + " && "
                + columnNameKeywordMap.get(colName).toString() + " "
                + operator + " '" + value + "'");
            }
          }
        }
      });

    final JPopupMenu p = new JPopupMenu();

    final Action clearFocusAction =
      new AbstractAction("Clear 'refine focus' field") {
        public void actionPerformed(ActionEvent e) {
          filterText.setText(null);
          ruleMediator.setRefinementRule(null);
        }
      };

    final JMenuItem menuItemToggleDock = new JMenuItem("Undock/dock");

    dockingAction =
      new AbstractAction("Undock") {
          public void actionPerformed(ActionEvent evt) {
            if (isDocked()) {
              undock();
            } else {
              dock();
            }
          }
        };
    dockingAction.putValue(
      Action.SMALL_ICON, new ImageIcon(ChainsawIcons.UNDOCK));
    menuItemToggleDock.setAction(dockingAction);

    /*
     * Popup definition
     */
    p.add(menuItemFocusOn);
    p.add(menuDefineAddCustomFilter);
    p.add(clearFocusAction);

    p.add(new JSeparator());

        final JMenuItem menuItemSearch =
      new JMenuItem("Find next");
    menuItemSearch.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          if (currentPoint != null) {
            String operator = "~=";
            int column = table.columnAtPoint(currentPoint);
            int row = table.rowAtPoint(currentPoint);
            String colName = table.getColumnName(column).toUpperCase();
            String value = "";

            if (colName.equalsIgnoreCase(ChainsawConstants.TIMESTAMP_COL_NAME)) {
            	value = timestampExpressionFormat.format(new Date(table.getValueAt(row, column).toString()));
            } else {
              Object o = table.getValueAt(row, column);

              if (o != null) {
                if (o instanceof String[]) {
                  value = ((String[]) o)[0];
                  operator = "~=";
                } else {
                  value = o.toString();
                }
              }
            }

            if (columnNameKeywordMap.containsKey(colName)) {
              findField.setText(
                columnNameKeywordMap.get(colName).toString() + " " + operator
                + " '" + value + "'");
              findNext();
            }
          }
        }
      });

      final Action clearSearchAction =
        new AbstractAction("Clear find next") {
          public void actionPerformed(ActionEvent e) {
            findField.setText(null);
            updateRule(null);
          }
        };

    p.add(menuItemSearch);
    p.add(clearSearchAction);
      
    p.add(new JSeparator());

    p.add(menuItemBestFit);

    p.add(new JSeparator());

    final JMenuItem menuItemDisplayNormalTimes =
      new JMenuItem("Hide relative times");
    menuItemDisplayNormalTimes.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (currentPoint != null) {
            renderer.setUseNormalTimes();
            tableModel.reFilter();
            menuItemDisplayNormalTimes.setEnabled(false);
          }
        }
    });

    final JMenuItem menuItemDisplayRelativeTimesToRowUnderCursor =
      new JMenuItem("Show times relative to this row");
    menuItemDisplayRelativeTimesToRowUnderCursor.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            if (currentPoint != null) {
              int row = table.rowAtPoint(currentPoint);
              LoggingEvent event = tableModel.getRow(row);
              renderer.setUseRelativeTimes(event.getTimeStamp());
              tableModel.reFilter();
              menuItemDisplayNormalTimes.setEnabled(true);
            }
        }
      });

    menuItemDisplayNormalTimes.setEnabled(false);
    p.add(menuItemDisplayRelativeTimesToRowUnderCursor);
    p.add(menuItemDisplayNormalTimes);
    p.add(new JSeparator());

    p.add(menuItemToggleDetails);
    p.add(menuItemLoggerTree);
    p.add(menuItemToggleToolTips);

    p.add(new JSeparator());

    p.add(menuItemScrollToTop);
    p.add(menuItemScrollBottom);
    p.add(new JSeparator());

    p.add(menuItemToggleDock);

    p.add(new JSeparator());

    p.add(menuItemColorPanel);
    p.add(menuItemLogPanelPreferences);

    final PopupListener popupListener = new PopupListener(p);

    eventsPane.addMouseListener(popupListener);
    table.addMouseListener(popupListener);
  }
  
  /**
   * Accessor
   *
   * @return scrollToBottom
   *
   */
  public boolean isScrollToBottom() {
  	return preferenceModel.isScrollToBottom();
  }

  /**
   * Mutator
   *
   */
  public void toggleScrollToBottom() {
  	preferenceModel.setScrollToBottom(!preferenceModel.isScrollToBottom());
  }
  
  private void scrollToBottom() {
    table.scrollToRow(tableModel.getRowCount() - 1);
  }

  public void scrollToTop()
  {
    if (tableModel.getRowCount() > 1) {
        table.scrollToRow(0);
    }
  }

  /**
   * Accessor
   *
   * @return namespace
   *
   * @see Profileable
   */
  public String getNamespace() {
    return getIdentifier();
  }

  /**
   * Accessor
   *
   * @return identifier
   *
   * @see EventBatchListener
   */
  public String getInterestedIdentifier() {
    return getIdentifier();
  }

  /**
   * Process events associated with the identifier.  Currently assumes it only
   * receives events which share this LogPanel's identifier
   *
   * @param ident identifier shared by events
   * @param events list of LoggingEvent objects
   */
  public void receiveEventBatch(String ident, final List events) {

    SwingHelper.invokeOnEDT(new Runnable() {
      public void run() {
        /*
        * if this panel is paused, we totally ignore events
        */
        if (isPaused()) {
          return;
        }
        final int selectedRow = table.getSelectedRow();
        final int startingRow = table.getRowCount();
        final ExtendedLoggingEvent selectedEvent;
        if (selectedRow >= 0) {
          selectedEvent = tableModel.getRow(selectedRow);
        } else {
          selectedEvent = null;
        }

        boolean rowAdded = false;

        int addedRowCount = 0;
        for (Iterator iter = events.iterator(); iter.hasNext();) {
          ExtendedLoggingEvent event = new ExtendedLoggingEvent((LoggingEvent) iter.next());

          updateOtherModels(event);

          boolean isCurrentRowAdded = tableModel.isAddRow(event);
          if (isCurrentRowAdded) {
              addedRowCount++;
          }
          rowAdded = rowAdded || isCurrentRowAdded;
        }
        //fire after adding all events
        if (rowAdded) {
          tableModel.fireTableEvent(startingRow, startingRow + addedRowCount, addedRowCount);
        }

        //tell the model to notify the count listeners
        tableModel.notifyCountListeners();

        if (rowAdded) {
          if (tableModel.isSortEnabled()) {
            tableModel.sort();
          }
          
          //always update detail pane (since we may be using a cyclic buffer which is full)
          detailPaneUpdater.setSelectedRow(table.getSelectedRow());
        }

        if (isScrollToBottom()) {
          scrollToBottom();
        } else if (selectedEvent != null) {
          final int newIndex = tableModel.getRowIndex(selectedEvent);
          if (newIndex >= 0) {
            // Don't scroll, just maintain selection...
            table.setRowSelectionInterval(newIndex, newIndex);
          }
        }
      }
    });
  }

  /**
   * Load settings from the panel preference model
   *
   * @param event
   *
   * @see LogPanelPreferenceModel
   */
  public void loadSettings(LoadSettingsEvent event) {

    File xmlFile = new File(SettingsManager.getInstance()
                .getSettingsDirectory(), URLEncoder.encode(identifier) + ".xml");

    if (xmlFile.exists()) {
        XStream stream = buildXStreamForLogPanelPreference();
        ObjectInputStream in = null;
        try {
            FileReader r = new FileReader(xmlFile);
            in = stream.createObjectInputStream(r);
            LogPanelPreferenceModel storedPrefs = (LogPanelPreferenceModel)in.readObject();
            int lowerPanelDividerLocation = in.readInt();
            int treeDividerLocation = in.readInt();
            String conversionPattern = in.readObject().toString();
            Point p = (Point)in.readObject();
            Dimension d = (Dimension)in.readObject();
            //this version number is checked to identify whether there is a Vector comming next
            int versionNumber = 0;
            try {
                versionNumber = in.readInt();
            } catch (EOFException eof){
            }

            Vector savedVector;
            //read the vector only if the version number is greater than 0. higher version numbers can be
            //used in the future to save more data structures
            if (versionNumber > 0) {
                savedVector = (Vector) in.readObject();
                for(int i = 0 ; i < savedVector.size() ; i++){
                    Object item = savedVector.get(i);
                    //insert each row at index zero (so last row in vector will be row zero)
                    filterCombo.insertItemAt(item, 0);
                }
                if (versionNumber > 1) {
                    //update prefModel columns to include defaults
                    int index = 0;
                    String columnOrder = event.getSetting(TABLE_COLUMN_ORDER);
                    StringTokenizer tok = new StringTokenizer(columnOrder, ",");
                    while (tok.hasMoreElements()) {
                      String element = tok.nextElement().toString().trim().toUpperCase();
                      TableColumn column = new TableColumn(index++);
                      column.setHeaderValue(element);
                      preferenceModel.addColumn(column);
                    }

                    TableColumnModel columnModel = table.getColumnModel();
                    //remove previous columns
                    while (columnModel.getColumnCount() > 0) {
                        columnModel.removeColumn(columnModel.getColumn(0));
                    }
                    //add visible column order columns
                    for (Iterator iter = preferenceModel.getVisibleColumnOrder().iterator();iter.hasNext();) {
                        TableColumn col = (TableColumn)iter.next();
                        columnModel.addColumn(col);
                    }
                    preferenceModel.apply(storedPrefs);
                } else {
                    loadDefaultColumnSettings(event);
                }
                //may be panel configs that don't have these values
                lowerPanel.setDividerLocation(lowerPanelDividerLocation);
                nameTreeAndMainPanelSplit.setDividerLocation(treeDividerLocation);
                detailLayout.setConversionPattern(conversionPattern);
                if (p.x != 0 && p.y != 0) {
                    undockedFrame.setLocation(p.x, p.y);
                    undockedFrame.setSize(d);
                } else {
                    undockedFrame.setLocation(0, 0);
                    undockedFrame.setSize(new Dimension(1024, 768));
                }
            } else {
                loadDefaultColumnSettings(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadDefaultColumnSettings(event);
            // TODO need to log this..
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {}
            }
        }
    } else {
        loadDefaultColumnSettings(event);
    }

    logTreePanel.ignore(preferenceModel.getHiddenLoggers());

    //first attempt to load encoded file
    File f2 =
      new File(
        SettingsManager.getInstance().getSettingsDirectory(), URLEncoder.encode(identifier) + COLORS_EXTENSION);

    if (f2.exists()) {
        loadColorSettings(f2);
    } else {
        f2 =
            new File(
              SettingsManager.getInstance().getSettingsDirectory(), identifier + COLORS_EXTENSION);
    }
  }

  /**
   * Save preferences to the panel preference model
   *
   * @param event
   *
   * @see LogPanelPreferenceModel
   */
  public void saveSettings(SaveSettingsEvent event) {
      File xmlFile = new File(SettingsManager.getInstance()
              .getSettingsDirectory(), URLEncoder.encode(identifier) + ".xml");

    preferenceModel.setHiddenLoggers(new HashSet(logTreePanel.getHiddenSet()));
    List visibleOrder = new ArrayList();
    Enumeration cols = table.getColumnModel().getColumns();
    while (cols.hasMoreElements()) {
    	TableColumn c = (TableColumn)cols.nextElement();
    	visibleOrder.add(c);
    }
    preferenceModel.setVisibleColumnOrder(visibleOrder);
    
    XStream stream = buildXStreamForLogPanelPreference();
    ObjectOutputStream s = null;
    try {
    	FileWriter w = new FileWriter(xmlFile);
    	s = stream.createObjectOutputStream(w);
    	s.writeObject(preferenceModel);
        s.writeInt(lowerPanel.getDividerLocation());
    	s.writeInt(nameTreeAndMainPanelSplit.getDividerLocation());
    	s.writeObject(detailLayout.getConversionPattern());
    	s.writeObject(undockedFrame.getLocation());
    	s.writeObject(undockedFrame.getSize());
        //this is a version number written to the file to identify that there is a Vector serialized after this
        s.writeInt(LOG_PANEL_SERIALIZATION_VERSION_NUMBER);
        //don't write filterexpressionvector, write the combobox's model's backing vector
        s.writeObject(filterCombo.getModelData());
    } catch (Exception ex) {
        ex.printStackTrace();
        // TODO need to log this..
    } finally {
    	if (s != null) {
    		try {
    			s.close();
    		} catch (IOException ioe) {}
    	}
    }

//    TODO colour settings need to be saved
    saveColorSettings();
  }

    private XStream buildXStreamForLogPanelPreference() {
        XStream stream = new XStream(new DomDriver());
        stream.registerConverter(new TableColumnConverter());
        return stream;
    }

  /**
     * Display the panel preferences frame
     */
  void showPreferences() {
    preferencesFrame.setVisible(true);
  }

  /**
   * Display the color rule frame
   */
  void showColorPreferences() {
    colorPanel.loadLogPanelColorizers();
    colorFrame.pack();
    colorFrame.setVisible(true);
  }

  /**
   * Toggle panel preference for detail visibility on or off
   */
  void toggleDetailVisible() {
    preferenceModel.setDetailPaneVisible(
      !preferenceModel.isDetailPaneVisible());
  }

  /**
   * Accessor
   *
   * @return detail visibility flag
   */
  boolean isDetailVisible() {
    return preferenceModel.isDetailPaneVisible();
  }

  /**
   * Toggle panel preference for logger tree visibility on or off
   */
  void toggleLogTreeVisible() {
    preferenceModel.setLogTreePanelVisible(
      !preferenceModel.isLogTreePanelVisible());
  }

  /**
   * Accessor
   *
   * @return logger tree visibility flag
   */
  boolean isLogTreeVisible() {
    return preferenceModel.isLogTreePanelVisible();
  }

  /**
   * Return all events
   *
   * @return list of LoggingEvents
   */
  List getEvents() {
    return tableModel.getAllEvents();
  }

  /**
   * Return the events that are visible with the current filter applied
   *
   * @return list of LoggingEvents
   */
  List getFilteredEvents() {
  	return tableModel.getFilteredEvents();  
  }
  
  List getMatchingEvents(Rule rule) {
    return tableModel.getMatchingEvents(rule);
  }

  /**
   * Remove all events
   */
  void clearEvents() {
    clearModel();
  }

  /**
   * Accessor
   *
   * @return identifier
   */
  String getIdentifier() {
    return identifier;
  }

  /**
   * Undocks this DockablePanel by removing the panel from the LogUI window
   * and placing it inside it's own JFrame.
   */
  void undock() {
  	int row = table.getSelectedRow();
    setDocked(false);
    externalPanel.removeAll();
    findPanel.removeAll();

    JLabel searchLabel = new JLabel("Search:");
    searchLabel.setDisplayedMnemonic('j');

    searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD));
    findPanel.add(searchLabel);
    findPanel.add(Box.createHorizontalStrut(3));

    findPanel.add(findField);
    findPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    searchLabel.setLabelFor(findField);

    externalPanel.add(undockedToolbar, BorderLayout.NORTH);
    externalPanel.add(nameTreeAndMainPanelSplit, BorderLayout.CENTER);
    externalPanel.setDocked(false);
    undockedFrame.pack();

    undockedFrame.setVisible(true);
    dockingAction.putValue(Action.NAME, "Dock");
    dockingAction.putValue(Action.SMALL_ICON, ChainsawIcons.ICON_DOCK);
    if (row > -1) {
    	table.scrollToRow(row);
    }
  }

  /**
   * Add an eventCountListener
   *
   * @param l
   */
  void addEventCountListener(EventCountListener l) {
    tableModel.addEventCountListener(l);
  }

  /**
   * Accessor
   *
   * @return paused flag
   */
  boolean isPaused() {
    return paused;
  }

  /**
   * Modifies the Paused property and notifies the listeners
   *
   * @param paused
   */
  void setPaused(boolean paused) {
    boolean oldValue = this.paused;
    this.paused = paused;
    firePropertyChange("paused", oldValue, paused);
  }

  /**
   * Change the selected event on the log panel
   *
   * @param eventNumber
   */
  void setSelectedEvent(int eventNumber) {
      int row = tableModel.find(ExpressionRule.getRule("prop.log4jid == " + eventNumber), 0, true);
      table.scrollTo(row, 0);
  }

  /**
   * Add a preference propertyChangeListener
   *
   * @param listener
   */
  void addPreferencePropertyChangeListener(PropertyChangeListener listener) {
    preferenceModel.addPropertyChangeListener(listener);
  }

  /**
   * Toggle the LoggingEvent container from either managing a cyclic buffer of
   * events or an ArrayList of events
   */
  void toggleCyclic() {
    tableModel.setCyclic(!tableModel.isCyclic());
  }

  /**
   * Accessor
   *
   * @return flag answering if LoggingEvent container is a cyclic buffer
   */
  boolean isCyclic() {
    return tableModel.isCyclic();
  }

  public void updateRule(String ruleText) {
    if ((ruleText == null) || (ruleText.trim().equals(""))) {
      findRule = null;
      tableModel.updateEventsWithFindRule(null);
      colorizer.setFindRule(null);
      //reset background color in case we were previously an invalid expression
      findField.setBackground(UIManager.getColor("TextField.background"));
      findField.setToolTipText(
        "Enter expression - right click or ctrl-space for menu");
    } else {
      //only turn off scrolltobottom when finding something (find not empty)
      preferenceModel.setScrollToBottom(false);
      if(ruleText.equals(currentFindRuleText)) {
          //don't update events if rule hasn't changed (we're finding next/previous)
          return;
      }
      currentFindRuleText = ruleText;
      try {
        findField.setToolTipText(
          "Enter expression - right click or ctrl-space for menu");
        findRule = ExpressionRule.getRule(ruleText);
        tableModel.updateEventsWithFindRule(findRule);
        colorizer.setFindRule(findRule);
        //valid expression, reset background color in case we were previously an invalid expression
        findField.setBackground(UIManager.getColor("TextField.background"));
      } catch (IllegalArgumentException re) {
        findField.setToolTipText(re.getMessage());
        findField.setBackground(INVALID_EXPRESSION_BACKGROUND);
        colorizer.setFindRule(null);
        tableModel.updateEventsWithFindRule(null);
      }
    }
  }

  /**
   * Display the detail pane, using the last known divider location
   */
  private void showDetailPane() {
    lowerPanel.setDividerSize(dividerSize);
    lowerPanel.setDividerLocation(lastDetailPanelSplitLocation);
    detailPanel.setVisible(true);
    lowerPanel.repaint();
  }

  /**
   * Hide the detail pane, holding the current divider location for later use
   */
  private void hideDetailPane() {
    int currentSize = lowerPanel.getHeight() - lowerPanel.getDividerSize();

    if (currentSize > 0) {
      lastDetailPanelSplitLocation =
        (double) lowerPanel.getDividerLocation() / currentSize;
     }

    lowerPanel.setDividerSize(0);
    detailPanel.setVisible(false);
    lowerPanel.repaint();
  }

  /**
   * Display the log tree pane, using the last known divider location
   */
  private void showLogTreePanel() {
    nameTreeAndMainPanelSplit.setDividerSize(dividerSize);
    nameTreeAndMainPanelSplit.setDividerLocation(
      lastLogTreePanelSplitLocation);
    logTreePanel.setVisible(true);
    nameTreeAndMainPanelSplit.repaint();
  }

  /**
   * Hide the log tree pane, holding the current divider location for later use
   */
  private void hideLogTreePanel() {
    //subtract one to make sizes match
    int currentSize = nameTreeAndMainPanelSplit.getWidth() - nameTreeAndMainPanelSplit.getDividerSize() - 1;

    if (currentSize > 0) {
      lastLogTreePanelSplitLocation =
        (double) nameTreeAndMainPanelSplit.getDividerLocation() / currentSize;
    }
    nameTreeAndMainPanelSplit.setDividerSize(0);
    logTreePanel.setVisible(false);
    nameTreeAndMainPanelSplit.repaint();
  }

  /**
   * Return a toolbar used by the undocked LogPanel's frame
   *
   * @return toolbar
   */
  private JToolBar createDockwindowToolbar() {
    final JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);

    final Action dockPauseAction =
      new AbstractAction("Pause") {
        public void actionPerformed(ActionEvent evt) {
          setPaused(!isPaused());
        }
      };

    dockPauseAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
    dockPauseAction.putValue(
      Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("F12"));
    dockPauseAction.putValue(
      Action.SHORT_DESCRIPTION,
      "Halts the display, while still allowing events to stream in the background");
    dockPauseAction.putValue(
      Action.SMALL_ICON, new ImageIcon(ChainsawIcons.PAUSE));

    final SmallToggleButton dockPauseButton =
      new SmallToggleButton(dockPauseAction);
    dockPauseButton.setText("");

    dockPauseButton.getModel().setSelected(isPaused());

    addPropertyChangeListener(
      "paused",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          dockPauseButton.getModel().setSelected(isPaused());
        }
      });
    toolbar.add(dockPauseButton);

    Action dockShowPrefsAction =
      new AbstractAction("") {
        public void actionPerformed(ActionEvent arg0) {
          showPreferences();
        }
      };

    dockShowPrefsAction.putValue(
      Action.SHORT_DESCRIPTION, "Define preferences...");
    dockShowPrefsAction.putValue(
      Action.SMALL_ICON, ChainsawIcons.ICON_PREFERENCES);

    toolbar.add(new SmallButton(dockShowPrefsAction));

    Action dockToggleLogTreeAction =
      new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          toggleLogTreeVisible();
        }
      };

      dockToggleLogTreeAction.putValue(Action.SHORT_DESCRIPTION, "Toggles the Logger Tree Pane");
      dockToggleLogTreeAction.putValue("enabled", Boolean.TRUE);
      dockToggleLogTreeAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
      dockToggleLogTreeAction.putValue(
        Action.ACCELERATOR_KEY,
        KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_MASK));
      dockToggleLogTreeAction.putValue(
        Action.SMALL_ICON, new ImageIcon(ChainsawIcons.WINDOW_ICON));

    final SmallToggleButton toggleLogTreeButton =
      new SmallToggleButton(dockToggleLogTreeAction);
    preferenceModel.addPropertyChangeListener("logTreePanelVisible", new PropertyChangeListener() {
    	public void propertyChange(PropertyChangeEvent evt) {
    	    toggleLogTreeButton.setSelected(preferenceModel.isLogTreePanelVisible());    		
    	}
    });
    		
    toggleLogTreeButton.setSelected(isLogTreeVisible());
    toolbar.add(toggleLogTreeButton);
    toolbar.addSeparator();

    final Action undockedClearAction =
      new AbstractAction("Clear") {
        public void actionPerformed(ActionEvent arg0) {
          clearModel();
        }
      };

    undockedClearAction.putValue(
      Action.SMALL_ICON, new ImageIcon(ChainsawIcons.DELETE));
    undockedClearAction.putValue(
      Action.SHORT_DESCRIPTION, "Removes all the events from the current view");

    final SmallButton dockClearButton = new SmallButton(undockedClearAction);
    dockClearButton.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, InputEvent.CTRL_MASK),
      undockedClearAction.getValue(Action.NAME));
    dockClearButton.getActionMap().put(
      undockedClearAction.getValue(Action.NAME), undockedClearAction);

    dockClearButton.setText("");
    toolbar.add(dockClearButton);
    toolbar.addSeparator();

    Action dockToggleScrollToBottomAction =
        new AbstractAction("Toggles Scroll to Bottom") {
          public void actionPerformed(ActionEvent e) {
            toggleScrollToBottom();
          }
        };

        dockToggleScrollToBottomAction.putValue(Action.SHORT_DESCRIPTION, "Toggles Scroll to Bottom");
        dockToggleScrollToBottomAction.putValue("enabled", Boolean.TRUE);
        dockToggleScrollToBottomAction.putValue(
          Action.SMALL_ICON, new ImageIcon(ChainsawIcons.SCROLL_TO_BOTTOM));

      final SmallToggleButton toggleScrollToBottomButton =
        new SmallToggleButton(dockToggleScrollToBottomAction);
      preferenceModel.addPropertyChangeListener("scrollToBottom", new PropertyChangeListener() {
      	public void propertyChange(PropertyChangeEvent evt) {
      	    toggleScrollToBottomButton.setSelected(isScrollToBottom());    		
      	}
      });

      toggleScrollToBottomButton.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
  	      KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK),
  	      dockToggleScrollToBottomAction.getValue(Action.NAME));
  	    toggleScrollToBottomButton.getActionMap().put(
  	      dockToggleScrollToBottomAction.getValue(Action.NAME), dockToggleScrollToBottomAction);
      
      toggleScrollToBottomButton.setSelected(isScrollToBottom());
      toggleScrollToBottomButton.setText("");
      toolbar.add(toggleScrollToBottomButton);
      toolbar.addSeparator();
    
    findField = new JTextField();
    Dimension findSize = new Dimension(260, 22);
    findField.setPreferredSize(findSize);
    findField.setMaximumSize(findSize);
    findField.setMinimumSize(findSize);
    findPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
    findField.setAlignmentY(Component.CENTER_ALIGNMENT);

    findField.addKeyListener(
      new ExpressionRuleContext(filterModel, findField));

    final Action undockedFindNextAction =
      new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          findNext();
        }
      };

    undockedFindNextAction.putValue(Action.NAME, "Find next");
    undockedFindNextAction.putValue(
      Action.SHORT_DESCRIPTION,
      "Find the next occurrence of the rule from the current row");
    undockedFindNextAction.putValue(
      Action.SMALL_ICON, new ImageIcon(ChainsawIcons.DOWN));

    SmallButton undockedFindNextButton =
      new SmallButton(undockedFindNextAction);

    undockedFindNextButton.setAction(undockedFindNextAction);
    undockedFindNextButton.setText("");
    undockedFindNextButton.getActionMap().put(
      undockedFindNextAction.getValue(Action.NAME), undockedFindNextAction);
    undockedFindNextButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      KeyStroke.getKeyStroke("F3"),
      undockedFindNextAction.getValue(Action.NAME));

    final Action undockedFindPreviousAction =
      new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          findPrevious();
        }
      };

    undockedFindPreviousAction.putValue(Action.NAME, "Find previous");
    undockedFindPreviousAction.putValue(
      Action.SHORT_DESCRIPTION,
      "Find the previous occurrence of the rule from the current row");
    undockedFindPreviousAction.putValue(
      Action.SMALL_ICON, new ImageIcon(ChainsawIcons.UP));

    SmallButton undockedFindPreviousButton =
      new SmallButton(undockedFindPreviousAction);

    undockedFindPreviousButton.setAction(undockedFindPreviousAction);
    undockedFindPreviousButton.setText("");
    undockedFindPreviousButton.getActionMap().put(
      undockedFindPreviousAction.getValue(Action.NAME),
      undockedFindPreviousAction);
    undockedFindPreviousButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK),
      undockedFindPreviousAction.getValue(Action.NAME));

    Dimension findPanelSize = new Dimension(310, 30);
    findPanel.setPreferredSize(findPanelSize);
    findPanel.setMaximumSize(findPanelSize);
    findPanel.setMinimumSize(findPanelSize);
    findField.setPreferredSize(findSize);
    findField.setMaximumSize(findSize);
    findField.setMinimumSize(findSize);
    findPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
    findField.setAlignmentY(Component.CENTER_ALIGNMENT);
    
    toolbar.add(findPanel);
    toolbar.addSeparator(new Dimension(7, 5));
      
    toolbar.add(undockedFindNextButton);
    toolbar.add(undockedFindPreviousButton);

    toolbar.addSeparator();

    Action redockAction =
      new AbstractAction("", ChainsawIcons.ICON_DOCK) {
        public void actionPerformed(ActionEvent arg0) {
          dock();
        }
      };

    redockAction.putValue(
      Action.SHORT_DESCRIPTION,
      "Docks this window back with the main Chainsaw window");

    SmallButton redockButton = new SmallButton(redockAction);
    toolbar.add(redockButton);

    return toolbar;
  }

  /**
   * Update the status bar with current selected row and row count
   */
  private void updateStatusBar() {
    SwingHelper.invokeOnEDT(
      new Runnable() {
        public void run() {
          statusBar.setSelectedLine(
            table.getSelectedRow() + 1, tableModel.getRowCount(),
            tableModel.size());
        }
      });
  }

  /**
   * Update the detail pane layout text
   *
   * @param conversionPattern layout text
   */
  private void setDetailPaneConversionPattern(String conversionPattern) {
    String oldPattern = getDetailPaneConversionPattern();
    (detailLayout).setConversionPattern(conversionPattern);
    firePropertyChange(
      "detailPaneConversionPattern", oldPattern,
      getDetailPaneConversionPattern());
  }

  /**
   * Accessor
   *
   * @return conversionPattern layout text
   */
  private String getDetailPaneConversionPattern() {
    return (detailLayout).getConversionPattern();
  }

  /**
   * Reset the LoggingEvent container, detail panel and status bar
   */
  private void clearModel() {
    previousLastIndex = -1;
    tableModel.clearModel();

    synchronized (detail) {
      detailPaneUpdater.setSelectedRow(-1);
      detail.notify();
    }

    statusBar.setNothingSelected();
  }

  public void findNextColorizedEvent() {
    int nextRow = tableModel.findColoredRow(table.getSelectedRow() + 1, true);        
    if (nextRow > -1) {
      table.scrollToRow(nextRow);
    }
  }

  public void findPreviousColorizedEvent() {
    int previousRow = tableModel.findColoredRow(table.getSelectedRow() - 1, false);
    if (previousRow > -1) {
      table.scrollToRow(previousRow);
    }
  }

  /**
   * Finds the next row matching the current find rule, and ensures it is made
   * visible
   *
   */
  public void findNext() {
    updateRule(findField.getText());

    if (findRule != null) {
      try {
        int nextRow = tableModel.find(findRule, table.getSelectedRow() + 1, true);

        if (nextRow > -1) {
          table.scrollToRow(nextRow);
          findField.setToolTipText("Enter an expression");
        }
      } catch (IllegalArgumentException iae) {
        findField.setToolTipText(iae.getMessage());
        colorizer.setFindRule(null);
      }
    }
  }

  /**
   * Finds the previous row matching the current find rule, and ensures it is made
   * visible
   *
   */
  public void findPrevious() {
    updateRule(findField.getText());

    if (findRule != null) {
      try {
        final int previousRow =
          tableModel.find(findRule, table.getSelectedRow() - 1, false);

        if (previousRow > -1) {
          table.scrollToRow(previousRow);
          findField.setToolTipText("Enter an expression");
        }
      } catch (IllegalArgumentException iae) {
        findField.setToolTipText(iae.getMessage());
      }
    }
  }

  /**
   * Docks this DockablePanel by hiding the JFrame and placing the Panel back
   * inside the LogUI window.
   */
  private void dock() {
  	
  	int row = table.getSelectedRow();
    setDocked(true);
    undockedFrame.setVisible(false);
    removeAll();

    add(nameTreeAndMainPanelSplit, BorderLayout.CENTER);
    externalPanel.setDocked(true);
    dockingAction.putValue(Action.NAME, "Undock");
    dockingAction.putValue(Action.SMALL_ICON, ChainsawIcons.ICON_UNDOCK);
    if (row > -1) {
    	table.scrollToRow(row);
    }
  }

  /**
   * Save panel color settings
   */
  private void saveColorSettings() {
    ObjectOutputStream o = null;

    try {
      File f = new File(SettingsManager.getInstance().getSettingsDirectory(), 
      		URLEncoder.encode(getIdentifier() + COLORS_EXTENSION));
      logger.debug("writing colors to file: " + f);
      
      o = new ObjectOutputStream(
          new BufferedOutputStream(new FileOutputStream(f)));

      o.writeObject(colorizer.getRules());
      o.flush();
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      try {
        if (o != null) {
          o.close();
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  /**
   * Load default column settings if no settings exist for this identifier
   *
   * @param event
   */
  private void loadDefaultColumnSettings(LoadSettingsEvent event) {
    String columnOrder = event.getSetting(TABLE_COLUMN_ORDER);

    TableColumnModel columnModel = table.getColumnModel();

    Map columnNameMap = new HashMap();

    for (int i = 0; i < columnModel.getColumnCount(); i++) {
      columnNameMap.put(table.getColumnName(i).toUpperCase(), columnModel.getColumn(i));
    }

    int index = 0;
    StringTokenizer tok = new StringTokenizer(columnOrder, ",");
    List sortedColumnList = new ArrayList();

    /*
       remove all columns from the table that exist in the model
       and add in the correct order to a new arraylist
       (may be a subset of possible columns)
     **/
    while (tok.hasMoreElements()) {
      String element = tok.nextElement().toString().trim().toUpperCase();
      TableColumn column = (TableColumn) columnNameMap.get(element);

      if (column != null) {
        sortedColumnList.add(column);
        table.removeColumn(column);
      }
    }
    preferenceModel.setDetailPaneVisible(event.asBoolean("detailPaneVisible"));
    preferenceModel.setLogTreePanelVisible(event.asBoolean("logTreePanelVisible"));
    //re-add columns to the table in the order provided from the list
    for (Iterator iter = sortedColumnList.iterator(); iter.hasNext();) {
      TableColumn element = (TableColumn) iter.next();
      if (preferenceModel.addColumn(element)) {
          table.addColumn(element);
    	  preferenceModel.setColumnVisible(element.getHeaderValue().toString(), true);
      }
    }

    String columnWidths = event.getSetting(TABLE_COLUMN_WIDTHS);

    tok = new StringTokenizer(columnWidths, ",");
    index = 0;

    while (tok.hasMoreElements()) {
      String element = (String) tok.nextElement();

      try {
        int width = Integer.parseInt(element);

        if (index > (columnModel.getColumnCount() - 1)) {
          logger.warn(
            "loadsettings - failed attempt to set width for index " + index
            + ", width " + element);
        } else {
          columnModel.getColumn(index).setPreferredWidth(width);
        }

        index++;
      } catch (NumberFormatException e) {
        logger.error("Error decoding a Table width", e);
      }
    }
    undockedFrame.setSize(getSize());
    undockedFrame.setLocation(getBounds().x, getBounds().y);

      repaint();
    }

  public JTextField getFindTextField() {
    return findField;
  }

  /**
   * Load panel color settings
   */
  private void loadColorSettings(File f) {
    if (f.exists()) {
      ObjectInputStream s = null;

      try {
        s = new ObjectInputStream(
            new BufferedInputStream(new FileInputStream(f)));

        Map map = (Map) s.readObject();
        colorizer.setRules(map);
      } catch (EOFException eof) { //end of file - ignore..
      }catch (IOException ioe) {
        ioe.printStackTrace();
        //unable to load file - delete it
        f.delete();
      } catch (ClassNotFoundException cnfe) {
        cnfe.printStackTrace();
      } finally {
        if (s != null) {
          try {
            s.close();
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Iterate over all values in the column and return the longest width
   *
   * @param index column index
   *
   * @return longest width - relies on FontMetrics.stringWidth for calculation
   */
  private int getMaxColumnWidth(int index) {
    FontMetrics metrics = getGraphics().getFontMetrics();
    int longestWidth =
      metrics.stringWidth("  " + table.getColumnName(index) + "  ")
      + (2 * table.getColumnModel().getColumnMargin());

    for (int i = 0, j = tableModel.getRowCount(); i < j; i++) {
      Component c =
        renderer.getTableCellRendererComponent(
          table, table.getValueAt(i, index), false, false, i, index);

      if (c instanceof JLabel) {
        longestWidth =
          Math.max(longestWidth, metrics.stringWidth(((JLabel) c).getText()));
      }
    }

    return longestWidth + 5;
  }

  /**
   * ensures the Entry map of all the unque logger names etc, that is used for
   * the Filter panel is updated with any new information from the event
   *
   * @param event
   */
  private void updateOtherModels(LoggingEvent event) {

    /*
     * EventContainer is a LoggerNameModel imp, use that for notifing
     */
    tableModel.addLoggerName(event.getLoggerName());

    filterModel.processNewLoggingEvent(event);
  }

    public void findNextMarker() {
      final int nextRow = tableModel.find(findMarkerRule, table.getSelectedRow() + 1, true);

      if (nextRow > -1) {
        table.scrollToRow(nextRow);
      }
    }

    public void findPreviousMarker() {
        final int previousRow = tableModel.find(findMarkerRule, table.getSelectedRow() - 1, false);

        if (previousRow > -1) {
          table.scrollToRow(previousRow);
        }
    }

    public void clearAllMarkers() {
        tableModel.removePropertyFromEvents(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE);
    }

    public void toggleMarker() {
        int row = table.getSelectedRow();
        if (row != -1) {
          LoggingEvent event = tableModel.getRow(row);
          Object marker = event.getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE);
          if (marker == null) {
              event.setProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE, "set");
          } else {
              event.removeProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE);
          }
          //if marker -was- null, it no longer is (may need to add the column)
          tableModel.fireRowUpdated(row, (marker == null));
        }
    }

    /**
   * This class receives notification when the Refine focus text field is
   * updated, where a backgrounh thread periodically wakes up and checks if
   * they have stopped typing yet. This ensures that the filtering of the
   * model is not done for every single character typed.
   *
   * @author Paul Smith psmith
   */
  private final class DelayedFilterTextDocumentListener
    implements DocumentListener {
    private static final long CHECK_PERIOD = 1000;
    private final JTextField filterText;
    private long lastTimeStamp = System.currentTimeMillis();
    private final Thread delayThread;
    private final String defaultToolTip;
    private String lastFilterText = null;

    private DelayedFilterTextDocumentListener(final JTextField filterText) {
      super();
      this.filterText = filterText;
      this.defaultToolTip = filterText.getToolTipText();

      this.delayThread =
        new Thread(
          new Runnable() {
            public void run() {
              while (true) {
                try {
                  Thread.sleep(CHECK_PERIOD);
                } catch (InterruptedException e) {
                }

                if (
                  (System.currentTimeMillis() - lastTimeStamp) < CHECK_PERIOD) {
                  // They typed something since the last check. we ignor
                  // this for a sample period
                  //                logger.debug("Typed something since the last check");
                } else if (
                  (System.currentTimeMillis() - lastTimeStamp) < (2 * CHECK_PERIOD)) {
                  // they stopped typing recently, but have stopped for at least
                  // 1 sample period. lets apply the filter
                  //                logger.debug("Typed something recently applying filter");
                  if (filterText != null && (!(filterText.getText().equals(lastFilterText)))) {
                    lastFilterText = filterText.getText();
                    setFilter();
                  }
                } else {
                  // they stopped typing a while ago, let's forget about it
                  //                logger.debug(
                  //                  "They stoppped typing a while ago, assuming filter has been applied");
                }
              }
            }
          });

      delayThread.setPriority(Thread.MIN_PRIORITY);
      delayThread.start();
    }

    /**
     * Update timestamp
     *
     * @param e
     */
    public void insertUpdate(DocumentEvent e) {
      notifyChange();
    }

    /**
     * Update timestamp
     *
     * @param e
     */
    public void removeUpdate(DocumentEvent e) {
      notifyChange();
    }

    /**
     * Update timestamp
     *
     * @param e
     */
    public void changedUpdate(DocumentEvent e) {
      notifyChange();
    }

    /**
     * Update timestamp
     */
    private void notifyChange() {
      this.lastTimeStamp = System.currentTimeMillis();
    }

    /**
     * Update refinement rule based on the entered expression.
     */
    private void setFilter() {
      if (filterText.getText().trim().equals("")) {
        //reset background color in case we were previously an invalid expression
        filterText.setBackground(UIManager.getColor("TextField.background"));
        ruleMediator.setRefinementRule(null);
        filterText.setToolTipText(defaultToolTip);
      } else {
        try {
          ruleMediator.setRefinementRule(
            ExpressionRule.getRule(filterText.getText()));
          filterText.setToolTipText(defaultToolTip);
          //valid expression, reset background color in case we were previously an invalid expression
          filterText.setBackground(UIManager.getColor("TextField.background"));
        } catch (IllegalArgumentException iae) {
          //invalid expression, change background of the field
          filterText.setToolTipText(iae.getMessage());
          filterText.setBackground(INVALID_EXPRESSION_BACKGROUND);
        }
      }
    }
  }

  private final class TableMarkerListener extends MouseAdapter {
      public void mouseClicked(MouseEvent evt) {
          if (evt.getClickCount() == 2) {
              int row = table.rowAtPoint(evt.getPoint());
              if (row != -1) {
                ExtendedLoggingEvent event = tableModel.getRow(row);
                Object marker = event.getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE);
                if (marker == null) {
                    event.setProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE, "set");
                } else {
                    event.removeProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE);
                }
                //if marker -was- null, it no longer is (may need to add the column)
                tableModel.fireRowUpdated(row, (marker == null));
              }
          }
      }
  }

  /**
   * Update active tooltip
   */
  private final class TableColumnDetailMouseListener extends MouseMotionAdapter {
    private int currentRow = -1;

    private TableColumnDetailMouseListener() {
    }

    /**
     * Update tooltip based on mouse position
     *
     * @param evt
     */
    public void mouseMoved(MouseEvent evt) {
      currentPoint = evt.getPoint();

      if (preferenceModel.isToolTips()) {
        int row = table.rowAtPoint(evt.getPoint());

        if ((row == currentRow) || (row == -1)) {
          return;
        }

        currentRow = row;

        ExtendedLoggingEvent event = tableModel.getRow(currentRow);

        if (event != null) {
          StringBuffer buf = new StringBuffer();
          buf.append(detailLayout.getHeader())
             .append(detailLayout.format(event)).append(
            detailLayout.getFooter());
          table.setToolTipText(buf.toString());
        }
      } else {
        table.setToolTipText(null);
      }
    }
  }

  //if columnmoved or columnremoved callback received, re-apply table's sort index based
  //sort column name
  private class ChainsawTableColumnModelListener
    implements TableColumnModelListener {
    private ChainsawTableColumnModelListener() {
    }

    /**
     * If a new column was added to the display and that column was the exception column,
     * set the cell editor to the throwablerenderer
     *
     * @param e
     */
    public void columnAdded(TableColumnModelEvent e) {
      Enumeration enumeration = table.getColumnModel().getColumns();

      while (enumeration.hasMoreElements()) {
        TableColumn column = (TableColumn) enumeration.nextElement();

        if (
          (column.getModelIndex() + 1) == ChainsawColumns.INDEX_THROWABLE_COL_NAME) {
          column.setCellEditor(throwableRenderPanel);
        }
        if (column.getHeaderValue().toString().toLowerCase().equals(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE)) {
          column.setCellEditor(markerCellEditor);
        }
      }
    }

    /**
     * Update sorted column
     *
     * @param e
     */
    public void columnRemoved(TableColumnModelEvent e) {
      table.updateSortedColumn();
    }

    /**
     * Update sorted column
     *
     * @param e
     */
    public void columnMoved(TableColumnModelEvent e) {
      table.updateSortedColumn();
    }

    /**
     * Ignore margin changed
     *
     * @param e
     */
    public void columnMarginChanged(ChangeEvent e) {
    }

    /**
     * Ignore selection changed
     *
     * @param e
     */
    public void columnSelectionChanged(ListSelectionEvent e) {
    }
  }

  /**
   * Thread that periodically checks if the selected row has changed, and if
   * it was, updates the Detail Panel with the detailed Logging information
   */
  private class DetailPaneUpdater implements PropertyChangeListener {
    private int selectedRow = -1;
    int lastRow = -1;
    private DetailPaneUpdater() {
    }

    /**
     * Update detail pane to display information about the LoggingEvent at index row
     *
     * @param row
     */
    private void setSelectedRow(int row) {
      selectedRow = row;
      updateDetailPane();
    }

    /**
     * Update detail pane
     */
    private void updateDetailPane() {
            /*
             * Don't bother doing anything if it's not visible. Note: the isVisible() method on
             * Component is not really accurate here because when the button to toggle display of
             * the detail pane is triggered it still appears as 'visible' for some reason.
             */
      if (!preferenceModel.isDetailPaneVisible()) {
        return;
      }

	      ExtendedLoggingEvent event = null;
	      if (selectedRow != -1 && (lastRow != selectedRow)) {
	        event = tableModel.getRow(selectedRow);
	
	        if (event != null) {
	          final StringBuffer buf = new StringBuffer();
	          buf.append(detailLayout.getHeader())
	             .append(detailLayout.format(event)).append(
	            detailLayout.getFooter());
	          if (buf.length() > 0) {
		          	try {
		          		final Document doc = detail.getEditorKit().createDefaultDocument();
		          		detail.getEditorKit().read(new StringReader(buf.toString()), doc, 0);
				      	SwingHelper.invokeOnEDT(new Runnable() {
				      		public void run() {
				      			detail.setDocument(doc);
				      			detail.setCaretPosition(0);
                                lastRow = selectedRow;
				      		}
				      	});
		          	} catch (Exception e) {}
	      		}
	        }
	      }
	
	      if (event == null && (lastRow != selectedRow)) {
          	try {
          		final Document doc = detail.getEditorKit().createDefaultDocument();
          		detail.getEditorKit().read(new StringReader("<html>Nothing selected</html>"), doc, 0);
		      	SwingHelper.invokeOnEDT(new Runnable() {
		      		public void run() {
		      			detail.setDocument(doc);
		      			detail.setCaretPosition(0);
                        lastRow = selectedRow;
		      		}
		      	});
          	} catch (Exception e) {}
  		}
    }

    /**
     * Update detail pane layout if it's changed
     *
     * @param arg0
     */
    public void propertyChange(PropertyChangeEvent arg0) {
      SwingUtilities.invokeLater(
        new Runnable() {
          public void run() {
            updateDetailPane();
          }
        });
    }
  }

    private class MarkerCellEditor implements TableCellEditor {
        JTextField textField = new JTextField();
        Set cellEditorListeners = new HashSet();
        private ExtendedLoggingEvent currentEvent;

        public Object getCellEditorValue()
        {
            return textField.getText();
        }

        public boolean isCellEditable(EventObject anEvent)
        {
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent)
        {
            textField.selectAll();
            return true;
        }

        public boolean stopCellEditing()
        {
            if (textField.getText().trim().equals("")) {
                currentEvent.removeProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE);
            } else {
                currentEvent.setProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE, textField.getText());
            }
            ChangeEvent event = new ChangeEvent(table);
            for (Iterator iter = cellEditorListeners.iterator();iter.hasNext();) {
                ((CellEditorListener)iter.next()).editingStopped(event);
            }
            return true;
        }

        public void cancelCellEditing()
        {
           ChangeEvent event = new ChangeEvent(table);
           for (Iterator iter = cellEditorListeners.iterator();iter.hasNext();) {
               ((CellEditorListener)iter.next()).editingCanceled(event);
           }
        }

        public void addCellEditorListener(CellEditorListener l)
        {
            cellEditorListeners.add(l);
        }

        public void removeCellEditorListener(CellEditorListener l)
        {
            cellEditorListeners.remove(l);
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
        {
            currentEvent = tableModel.getRow(row);
            textField.setText(currentEvent.getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE));
            textField.selectAll();
            return textField;
        }
    }

    private class EventMatchThumbnail extends JPanel {
        private List all = new ArrayList();
        private List findMatches = new ArrayList();

        private final int maxEventHeight = 6;

        public EventMatchThumbnail() {
            configureColors();
            colorizer.addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent evt)
                {
                    configureColors();
                }
            });
            addMouseListener(new MouseAdapter(){
                public void mouseClicked(MouseEvent e)
                {
                    int rowCount = table.getRowCount();

                    Point offsets = getScrollBarOffsets();
                    int topOffset = offsets.x;
                    int bottomOffset = offsets.y;

                    //'effective' height of this component is scrollpane height - top/bottom offsets
                    int height = eventsPane.getHeight() - topOffset - bottomOffset;

                    int clickLocation = e.getPoint().y;

                    //remove top offset from click location but avoid going negative
                    clickLocation = Math.max(clickLocation - topOffset, 0);

                    //don't let clicklocation exceed height
                    if (clickLocation >= height) {
                        clickLocation = height;
                    }

//                    System.out.println("clicked y pos: " + e.getPoint().y + ", relative: " + clickLocation);
                    float ratio = (float)clickLocation / height;
                    int rowToSelect = Math.round(rowCount * ratio);
//                    System.out.println("rowCount: " + rowCount + ", height: " + height + ", clickLocation: " + clickLocation + ", ratio: " + ratio + ", rowToSelect: " + rowToSelect);
                    EventWrapper event = getClosestRow(rowToSelect);
//                    System.out.println("rowToSelect: " + rowToSelect + ", closestRow: " + event.loggingEvent.getProperty("log4jid"));
                    if (event != null) {
                        int id = new Integer(event.loggingEvent.getProperty("log4jid")).intValue();
                        setSelectedEvent(id);
                    }
                }
            });

            tableModel.addTableModelListener(new TableModelListener(){
                public void tableChanged(TableModelEvent e) {
                    int firstRow = e.getFirstRow();
                    int lastRow = e.getLastRow();
                    if (lastRow == Integer.MAX_VALUE) {
                        lastRow = table.getRowCount() -1; //zero-indexed rows
                    }

                    List displayedEvents = tableModel.getFilteredEvents();
                    if (e.getType() == TableModelEvent.INSERT) {
//                        System.out.println("insert - current warnings: " + warnings.size() + ", errors: " + errors.size() + ", first row: " + firstRow + ", last row: " + lastRow);
                        for (int i=firstRow;i<lastRow;i++) {
                            ExtendedLoggingEvent event = (ExtendedLoggingEvent)displayedEvents.get(i);
                            EventWrapper wrapper = new EventWrapper(i, event);
                            if (event.isSearchMatch()) {
                                findMatches.add(wrapper);
//                                System.out.println("added warning: " + i + " - " + event.getLevel());
                            }
                            if (!wrapper.loggingEvent.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)) {
                                //add to this one
                                all.add(wrapper);
                            }
//                                System.out.println("added error: " + i + " - " + event.getLevel());
                        }
//                        System.out.println("insert- new warnings: " + warnings + ", errors: " + errors);

                        //run evaluation on rows & add to list
                    } else if (e.getType() == TableModelEvent.DELETE) {
                        //find each eventwrapper with an id in the deleted range and remove it...
//                        System.out.println("delete- current warnings: " + warnings.size() + ", errors: " + errors.size() + ", first row: " + firstRow + ", last row: " + lastRow + ", displayed event count: " + displayedEvents.size() );
                        for (Iterator iter = findMatches.iterator();iter.hasNext();) {
                            EventWrapper wrapper = (EventWrapper)iter.next();
                            if ((wrapper.rowNum >= firstRow) && (wrapper.rowNum <= lastRow)) {
//                                System.out.println("deleting find: " + wrapper);
                                iter.remove();
                            }
                        }
                        for (Iterator iter = all.iterator();iter.hasNext();) {
                            EventWrapper wrapper = (EventWrapper)iter.next();
                            if ((wrapper.rowNum >= firstRow) && (wrapper.rowNum <= lastRow)) {
//                                System.out.println("deleting error: " + wrapper);
                                iter.remove();
                            }
                        }
//                        System.out.println("delete- new warnings: " + warnings.size() + ", errors: " + errors.size());

                        //remove any matching rows
                    } else if (e.getType() == TableModelEvent.UPDATE) {
//                        System.out.println("update - about to delete old warnings in range: " + firstRow + " to " + lastRow + ", current warnings: " + warnings.size() + ", errors: " + errors.size());
                        //find each eventwrapper with an id in the deleted range and remove it...
                        for (Iterator iter = findMatches.iterator();iter.hasNext();) {
                            EventWrapper wrapper = (EventWrapper)iter.next();
                            if ((wrapper.rowNum >= firstRow) && (wrapper.rowNum <= lastRow)) {
//                                System.out.println("update - deleting warning: " + wrapper);
                                iter.remove();
                            }
                        }
                        for (Iterator iter = all.iterator();iter.hasNext();) {
                            EventWrapper wrapper = (EventWrapper)iter.next();
                            if ((wrapper.rowNum >= firstRow) && (wrapper.rowNum <= lastRow)) {
//                                System.out.println("update - deleting error: " + wrapper);
                                iter.remove();
                            }
                        }
//                        System.out.println("update - after deleting old warnings in range: " + firstRow + " to " + lastRow + ", new warnings: " + warnings.size() + ", errors: " + errors.size());
                        //NOTE: for update, we need to do i<= lastRow
                        for (int i=firstRow;i<=lastRow;i++) {
                            ExtendedLoggingEvent event = (ExtendedLoggingEvent)displayedEvents.get(i);
                            EventWrapper wrapper = new EventWrapper(i, event);
//                                System.out.println("update - adding error: " + i + ", event: " + event.getMessage());
                            //only add event to thumbnail if there is a color
                            if (!wrapper.loggingEvent.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)) {
                                all.add(wrapper);
                            }

                            if (event.isSearchMatch()) {
//                                System.out.println("update - adding marker: " + i + ", event: " + event.getMessage());
                                findMatches.add(wrapper);
                            }
                        }
                        //clear everything if we got an event w/-1
                        if (firstRow < 0 || lastRow < 0) {
                            all.clear();
                            findMatches.clear();
                        }
//                        System.out.println("update - new warnings: " + warnings.size() + ", errors: " + errors.size());
                    }
                    repaint();
                }
            });
        }

        private EventWrapper getClosestRow(int rowToSelect) {
            EventWrapper closestRow = null;
            int rowDelta = Integer.MAX_VALUE;
            for (Iterator iter = findMatches.iterator();iter.hasNext();) {
                EventWrapper event = (EventWrapper) iter.next();
                int newRowDelta = Math.abs(rowToSelect - event.rowNum);
                if (newRowDelta < rowDelta) {
                    closestRow = event;
                    rowDelta = newRowDelta;
                }
            }
            for (Iterator iter = all.iterator();iter.hasNext();) {
                EventWrapper event = (EventWrapper) iter.next();
                int newRowDelta = Math.abs(rowToSelect - event.rowNum);
                if (newRowDelta < rowDelta) {
                    closestRow = event;
                    rowDelta = newRowDelta;
                }
            }
            return closestRow;
        }

        private void configureColors() {
            findMatches.clear();
            all.clear();

            int i=0;
            for (Iterator iter = tableModel.getFilteredEvents().iterator();iter.hasNext();) {
                ExtendedLoggingEvent extendedLoggingEvent = (ExtendedLoggingEvent) iter.next();
                extendedLoggingEvent.updateColorRuleColors(colorizer.getBackgroundColor(extendedLoggingEvent), colorizer.getForegroundColor(extendedLoggingEvent));
                EventWrapper wrapper = new EventWrapper(i, extendedLoggingEvent);
                if (extendedLoggingEvent.isSearchMatch()) {
                    findMatches.add(wrapper);
                }
                i++;
                //only add if there is a color defined
                if (!wrapper.loggingEvent.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)) {
                    all.add(wrapper);
                }
            }
            repaint();
        }

        /**
         * Return a point representing top and bottom offsets
         * Top offset is held by Point.x, bottom offset is held by Point.y
         *
         * @return point representing top and bottom offsets (x and y values of Point)
         */
        public Point getScrollBarOffsets() {
            int scrollBarOffset = ((Integer)UIManager.get("ScrollBar.width")).intValue();
            //calculate topOffset and bottomOffset (topOffset is tableheader size + optional vertical scrollbar thumb height
            //bottom offset is optional vertical scrollbar thumb height + optional horizontal scrollbar thumb height
            int topOffset = table.getTableHeader().getMinimumSize().height;
            int bottomOffset = 0;
            if (eventsPane.getVerticalScrollBar().isVisible()) {
                topOffset += scrollBarOffset;
                bottomOffset += scrollBarOffset;
            }
            if (eventsPane.getHorizontalScrollBar().isVisible()) {
                bottomOffset += scrollBarOffset;
            }
            return new Point(topOffset, bottomOffset);
        }

        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Point topAndBottomOffset = getScrollBarOffsets();
            int topOffset = topAndBottomOffset.x;
            int bottomOffset = topAndBottomOffset.y;

            int rowCount = table.getRowCount();
            if (rowCount == 0) {
                return;
            }
            //use event pane height as reference height - max component height will be extended by event height if
            // last row is rendered, so subtract here
            int height = eventsPane.getHeight() - topOffset - bottomOffset;
            int maxHeight = Math.min(maxEventHeight, (height / rowCount));
            int minHeight = Math.max(1, maxHeight);
            int componentHeight = height - minHeight;
            int eventHeight = minHeight;

            for (Iterator iter = all.iterator();iter.hasNext();) {
                EventWrapper wrapper = (EventWrapper)iter.next();
                if (!wrapper.loggingEvent.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)) {
                    float ratio = (wrapper.rowNum / (float)rowCount);
    //                System.out.println("error - ratio: " + ratio + ", component height: " + componentHeight);
                    int verticalLocation = (int) (componentHeight * ratio) + topOffset;
                    drawEvent(wrapper.loggingEvent.getColorRuleBackground(), verticalLocation, eventHeight, g, false);
    //                System.out.println("painting error - rownum: " + wrapper.rowNum + ", location: " + verticalLocation + ", height: " + eventHeight + ", component height: " + componentHeight + ", row count: " + rowCount);
                }
            }

            for (Iterator iter = findMatches.iterator();iter.hasNext();) {
                EventWrapper wrapper = (EventWrapper)iter.next();
                float ratio = (wrapper.rowNum / (float)rowCount);
//                System.out.println("warning - ratio: " + ratio + ", component height: " + componentHeight);
                int verticalLocation = (int) (componentHeight * ratio) + topOffset;
                drawEvent(wrapper.loggingEvent.getBackground(), verticalLocation, eventHeight, g, true);
//                System.out.println("painting warning - rownum: " + wrapper.rowNum + ", location: " + verticalLocation + ", height: " + eventHeight + ", component height: " + componentHeight + ", row count: " + rowCount);
            }
        }

        private void drawEvent(Color newColor, int verticalLocation, int eventHeight, Graphics g, boolean drawHalfWidth) {
//            System.out.println("painting: - color: " + newColor + ", verticalLocation: " + verticalLocation + ", eventHeight: " + eventHeight);
            int x = 1;
            int width = getWidth() - (x * 2);
            if (drawHalfWidth) {
                width = (width/2);
            }
            //center drawing at vertical location 
            int y = verticalLocation + (eventHeight / 2);
            Color oldColor = g.getColor();
            g.setColor(newColor);
            g.fillRect(x, y, width, eventHeight);
            if (eventHeight >= 3) {
                g.setColor(newColor.darker());
                g.drawRect(x, y, width, eventHeight);
            }
            g.setColor(oldColor);
        }

        class EventWrapper {
            int rowNum;
            ExtendedLoggingEvent loggingEvent;
            public EventWrapper(int rowNum, ExtendedLoggingEvent loggingEvent) {
                this.rowNum = rowNum;
                this.loggingEvent = loggingEvent;
            }

            public String toString()
            {
                return "event - rownum: " + rowNum + ", level: " + loggingEvent.getLevel();
            }

            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (o == null || getClass() != o.getClass())
                {
                    return false;
                }

                EventWrapper that = (EventWrapper) o;

                if (loggingEvent != null ? !loggingEvent.equals(that.loggingEvent) : that.loggingEvent != null)
                {
                    return false;
                }

                return true;
            }

            public int hashCode()
            {
                return loggingEvent != null ? loggingEvent.hashCode() : 0;
            }
        }
    }

    static class AutoFilterComboBox extends JComboBox {
        private boolean bypassFiltering;
        private List allEntries = new ArrayList();
        private List displayedEntries = new ArrayList();
        private AutoFilterComboBoxModel model = new AutoFilterComboBoxModel();
        //editor component
        private final JTextField textField = new JTextField();
        private String lastTextToMatch;

        public AutoFilterComboBox(Collection entries) {
            if (entries != null) {
                for (Iterator iter=entries.iterator();iter.hasNext();) {
                    Object nextObject = iter.next();
                    model.addElement(nextObject);
                }
            }
            setModel(model);
            setEditor(new AutoFilterEditor());
            ((JTextField)getEditor().getEditorComponent()).getDocument().addDocumentListener(new AutoFilterDocumentListener());
            setEditable(true);
            addPopupMenuListener(new PopupMenuListenerImpl());
        }

        public Vector getModelData() {
            //reverse the model order, because it will be un-reversed when we reload it from saved settings
            Vector vector = new Vector();
            for (Iterator iter = allEntries.iterator();iter.hasNext();) {
                vector.insertElementAt(iter.next(), 0);
            }
            return vector;
        }

        private void refilter() {
            //only refilter if we're not bypassing filtering AND the text has changed since the last call to refilter
            String textToMatch = getEditor().getItem().toString();
            if (bypassFiltering || (lastTextToMatch != null && lastTextToMatch.equals(textToMatch))) {
                return;
            }
            lastTextToMatch = textToMatch;
            displayedEntries.clear();
            bypassFiltering = true;
                model.removeAllElements();
                List entriesCopy = new ArrayList(allEntries);
                for (Iterator iter = entriesCopy.iterator();iter.hasNext();) {
                    String thisEntry = iter.next().toString();
                    if (thisEntry.toLowerCase().contains(textToMatch.toLowerCase())) {
                        displayedEntries.add(thisEntry);
                        model.addElement(thisEntry);
                    }
                }
                bypassFiltering = false;
                //TODO: on no-match, don't filter at all (show the popup?)
                if (displayedEntries.size() > 0 && !textToMatch.equals("")) {
                    showPopup();
                } else {
                    hidePopup();
                }
        }

        class AutoFilterEditor implements ComboBoxEditor {
            public Component getEditorComponent() {
                return textField;
            }

            public void setItem(Object item) {
                if (bypassFiltering) {
                    return;
                }
                bypassFiltering = true;
                if (item == null) {
                    textField.setText("");
                } else {
                    textField.setText(item.toString());
                }
                bypassFiltering = false;
            }

            public Object getItem() {
                return textField.getText();
            }

            public void selectAll() {
                textField.selectAll();
            }

            public void addActionListener(ActionListener listener) {
                textField.addActionListener(listener);
            }

            public void removeActionListener(ActionListener listener) {
                textField.removeActionListener(listener);
            }
        }

        class AutoFilterDocumentListener implements DocumentListener {
            public void insertUpdate(DocumentEvent e) {
                refilter();
            }

            public void removeUpdate(DocumentEvent e) {
                refilter();
            }

            public void changedUpdate(DocumentEvent e) {
                refilter();
            }
        }

        class AutoFilterComboBoxModel extends AbstractListModel implements MutableComboBoxModel {
            private Object selectedItem;

            public void addElement(Object obj) {
                //assuming add is to displayed list...add to full list (only if not a dup)
                if (allEntries.contains(obj)) {
                    return;
                }
                bypassFiltering = true;
                allEntries.add(obj);
                displayedEntries.add(obj);
                fireIntervalAdded(this, displayedEntries.size() - 1, displayedEntries.size() -1);
                bypassFiltering = false;
            }

            public void removeElement(Object obj) {
                int index = displayedEntries.indexOf(obj);
                if (index != -1) {
                    removeElementAt(index);
                }
            }

            public void insertElementAt(Object obj, int index) {
                //assuming add is to displayed list...add to full list (only if not a dup)
                if (allEntries.contains(obj)) {
                    return;
                }
                bypassFiltering = true;
                displayedEntries.add(index, obj);
                allEntries.add(index, obj);
                fireIntervalAdded(this, index, index);
                bypassFiltering = false;
                refilter();
            }

            public void removeElementAt(int index) {
                bypassFiltering = true;
                //assuming removal is from displayed list..remove from full list
                Object obj = displayedEntries.get(index);
                allEntries.remove(obj);
                fireContentsChanged(this, 0, displayedEntries.size());
                bypassFiltering = false;
                refilter();
            }

            public void setSelectedItem(Object item) {
                if ((selectedItem != null && !selectedItem.equals(item)) || selectedItem == null && item != null) {
                    selectedItem = item;
                    fireContentsChanged(this, -1, -1);
                }
            }

            public Object getSelectedItem() {
                return selectedItem;
            }

            public int getSize() {
                return displayedEntries.size();
            }

            public Object getElementAt(int index) {
                if (index >= 0 && index < displayedEntries.size()) {
                    return displayedEntries.get(index);
                }
                return null;
            }


            public void removeAllElements() {
                bypassFiltering = true;
                displayedEntries.clear();
                fireContentsChanged(this, 0, displayedEntries.size());
                bypassFiltering = false;
            }

            public void showAllElements() {
                bypassFiltering = true;
                displayedEntries.clear();
                displayedEntries.addAll(allEntries);
                fireContentsChanged(this, 0, displayedEntries.size());
                bypassFiltering = false;
            }
        }

        private class PopupMenuListenerImpl implements PopupMenuListener {
            private boolean willBecomeVisible = false;

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                if (!willBecomeVisible) {
                    //we already have a match but we're showing the popup - unfilter
                    if (displayedEntries.contains(textField.getText())) {
                        model.showAllElements();
                    }

                    //workaround for bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4743225
                    //the height of the popup after updating entries in this listener was not updated..
                    JComboBox list = (JComboBox) e.getSource();
                    willBecomeVisible = true; // the flag is needed to prevent a loop
                    try {
                        list.getUI().setPopupVisible(list, true);
                    } finally {
                        willBecomeVisible = false;
                    }
                }
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                //no-op
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
                //no-op
            }
        }
    }
}
