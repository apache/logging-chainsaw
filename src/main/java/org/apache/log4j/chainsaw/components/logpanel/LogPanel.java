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

package org.apache.log4j.chainsaw.components.logpanel;

import org.apache.log4j.chainsaw.*;
import org.apache.log4j.chainsaw.color.ColorPanel;
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.components.elements.sorttable.JSortTable;
import org.apache.log4j.chainsaw.components.elements.SmallButton;
import org.apache.log4j.chainsaw.components.elements.SmallToggleButton;
import org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameTreePanel;
import org.apache.log4j.chainsaw.filter.FilterModel;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LineIconFactory;
import org.apache.log4j.chainsaw.layout.DefaultLayoutFactory;
import org.apache.log4j.chainsaw.layout.EventDetailLayout;
import org.apache.log4j.chainsaw.layout.LayoutEditorPane;
import org.apache.log4j.chainsaw.prefs.Profileable;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.ColorRule;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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
 * </ul>
 *
 * @author Scott Deboy (sdeboy at apache.org)
 * @author Paul Smith (psmith at apache.org)
 * @author Stephen Pain
 * @author Isuru Suriarachchi
 * @see org.apache.log4j.chainsaw.color.ColorPanel
 * @see org.apache.log4j.rule.ExpressionRule
 * @see org.apache.log4j.spi.LoggingEventFieldResolver
 */
public class LogPanel extends DockablePanel implements ChainsawEventBatchListener {
    private static final DateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat(Constants.TIMESTAMP_RULE_FORMAT);
    private final String identifier;
    private final ChainsawStatusBar statusBar;
    private final JFrame logPanelPreferencesFrame = new JFrame();
    private ColorPanel colorPanel;
    private final JFrame colorFrame = new JFrame();
    private final JFrame undockedFrame;
    private final DockablePanel externalPanel;
    private final Action dockingAction;
    private final JToolBar undockedToolbar;
    private JSortTable table = null;
    private TableColorizingRenderer renderer = null;
    private EventContainer tableModel = null;
    private final JEditorPane detail;
    private final JSplitPane lowerPanel;
    private final DetailPaneUpdater detailPaneUpdater;
    private final JPanel detailPanel = new JPanel(new BorderLayout());
    private final JSplitPane nameTreeAndMainPanelSplit;
    private final LoggerNameTreePanel logTreePanel;
    private final LogPanelPreferenceModel logPanelPreferenceModel;
    private final ApplicationPreferenceModel applicationPreferenceModel;
    private final LogPanelPreferencePanel logPanelPreferencePanel;
    private final FilterModel filterModel = new FilterModel();
    private RuleColorizer currentColorizer;
    private final RuleColorizer globalColorizer;
    private final RuleMediator tableRuleMediator = new RuleMediator(false);
    private final RuleMediator searchRuleMediator = new RuleMediator(true);
    private final EventDetailLayout detailLayout = new EventDetailLayout();
    private Point currentPoint;
    private JTable currentTable;
    private Rule findRule;
    private String currentFindRuleText;
    private Rule findMarkerRule;
    private final int dividerSize;
    private int previousLastIndex = -1;
    private final Logger logger = LogManager.getLogger();
    private AutoFilterComboBox filterCombo;
    private AutoFilterComboBox findCombo;
    private JScrollPane eventsPane;
    private int currentSearchMatchCount;
    private Rule clearTableExpressionRule;
    private EventContainer searchModel;
    private JSortTable searchTable = null;
    private TableColorizingRenderer searchRenderer;
    private ToggleToolTips mainToggleToolTips;
    private ToggleToolTips searchToggleToolTips;
    private JScrollPane detailPane;
    private JScrollPane searchPane;
    //only one tableCellEditor, shared by both tables
    private TableCellEditor markerCellEditor;
    private JToolBar detailToolbar;
    private boolean searchResultsDisplayed;
    private ColorizedEventAndSearchMatchThumbnail colorizedEventAndSearchMatchThumbnail;
    private EventTimeDeltaMatchThumbnail eventTimeDeltaMatchThumbnail;
    private ChainsawReceiver receiver;
    private Map<String, RuleColorizer> allColorizers;

    /**
     * Creates a new LogPanel object.  If a LogPanel with this identifier has
     * been loaded previously, reload settings saved on last exit.
     *
     * @param settingsManager
     * @param statusBar       shared status bar, provided by main application
     * @param identifier      used to load and save settings
     */
    public LogPanel(SettingsManager settingsManager,
                    ApplicationPreferenceModel applicationPreferenceModel,
                    final ChainsawStatusBar statusBar,
                    final String identifier,
                    Map<String, RuleColorizer> allColorizers,
                    RuleColorizer globalRuleColorizer) {

        this.identifier = identifier;
        this.applicationPreferenceModel = applicationPreferenceModel;
        logPanelPreferenceModel = new LogPanelPreferenceModel(settingsManager.getSettingsForReceiverTab(identifier));
        this.statusBar = statusBar;
        this.currentColorizer = globalRuleColorizer;
        this.globalColorizer = globalRuleColorizer;
        this.allColorizers = allColorizers;
        logger.debug("creating logpanel for {}", identifier);

        logPanelPreferencePanel = new LogPanelPreferencePanel(settingsManager, identifier, logPanelPreferenceModel);

        setLayout(new BorderLayout());

        String prototypeValue = "1231231231231231231231";

        filterCombo = new AutoFilterComboBox();
        findCombo = new AutoFilterComboBox();

        filterCombo.setPrototypeDisplayValue(prototypeValue);
        buildCombo(filterCombo, true, findCombo.model);

        findCombo.setPrototypeDisplayValue(prototypeValue);
        buildCombo(findCombo, false, filterCombo.model);

        ColumnNameKeywordMapper columnNameKeywordMapper = new ColumnNameKeywordMapper();

        logPanelPreferencesFrame.setTitle("'" + identifier + "' Log Panel Preferences");
        logPanelPreferencesFrame.setIconImage(
            ((ImageIcon) ChainsawIcons.ICON_PREFERENCES).getImage());
        logPanelPreferencesFrame.getContentPane().add(new JScrollPane(logPanelPreferencePanel));

        logPanelPreferencesFrame.setSize(740, 520);

        logPanelPreferencePanel.setOkCancelActionListener(
            e -> logPanelPreferencesFrame.setVisible(false));

        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action closeLogPanelPreferencesFrameAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                logPanelPreferencesFrame.setVisible(false);
            }
        };
        logPanelPreferencesFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "ESCAPE");
        logPanelPreferencesFrame.getRootPane().
            getActionMap().put("ESCAPE", closeLogPanelPreferencesFrameAction);


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

        logPanelPreferenceModel.addEventListener(
            evt -> {
                if (evt.getPropertyName().equals(LogPanelPreferenceModel.SCROLL_TO_BOTTOM)) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    if (value) {
                        scrollToBottom();
                    }
                }
            });
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
                        logPanelPreferenceModel.setDateFormatPattern("ISO8601");
                    }
                });
        final JRadioButtonMenuItem simpleTimeButton =
            new JRadioButtonMenuItem(
                new AbstractAction("Use simple time") {
                    public void actionPerformed(ActionEvent e) {
                        logPanelPreferenceModel.setDateFormatPattern("HH:mm:ss");
                    }
                });

        ButtonGroup dfBG = new ButtonGroup();
        dfBG.add(isoButton);
        dfBG.add(simpleTimeButton);
        simpleTimeButton.setSelected(true);
        dateFormatChangePopup.add(isoButton);
        dateFormatChangePopup.add(simpleTimeButton);

        final JCheckBoxMenuItem menuItemLoggerTree =
            new JCheckBoxMenuItem("Show Logger Tree");
        menuItemLoggerTree.addActionListener(
            e -> logPanelPreferenceModel.setLogTreePanelVisible(menuItemLoggerTree.isSelected()));
        menuItemLoggerTree.setIcon(new ImageIcon(ChainsawIcons.WINDOW_ICON));

        final JCheckBoxMenuItem menuItemToggleDetails =
            new JCheckBoxMenuItem("Show Detail Pane");
        menuItemToggleDetails.addActionListener(
            e -> logPanelPreferenceModel.setDetailPaneVisible(menuItemToggleDetails.isSelected()));

        menuItemToggleDetails.setIcon(new ImageIcon(ChainsawIcons.INFO));

        /*
         * add preferencemodel listeners
         */
        logPanelPreferenceModel.addEventListener(
                evt -> {
                    if (evt.getPropertyName().equals(LogPanelPreferenceModel.WRAP_MSG)) {
                        boolean wrap = (Boolean) evt.getPropertyValue();
                        renderer.setWrapMessage(wrap);
                        table.tableChanged(new TableModelEvent(tableModel));
                        searchRenderer.setWrapMessage(wrap);
                        searchTable.tableChanged(new TableModelEvent(searchModel));
                    }
                });

        logPanelPreferenceModel.addEventListener(
                evt -> {
                    if (evt.getPropertyName().equals(LogPanelPreferenceModel.SEARCH_RESULTS_VISIBLE)) {
                        boolean displaySearchResultsInDetailsIfAvailable = (Boolean) evt.getPropertyValue();
                        if (displaySearchResultsInDetailsIfAvailable) {
                            showSearchResults();
                        } else {
                            hideSearchResults();
                        }
                    }
                });

        logPanelPreferenceModel.addEventListener(
                evt -> {
                    if (evt.getPropertyName().equals(LogPanelPreferenceModel.HIGHLIGHT_SEARCH_MATCH_TEXT)) {
                        boolean highlightText = (Boolean) evt.getPropertyValue();
                        renderer.setHighlightSearchMatchText(highlightText);
                        table.tableChanged(new TableModelEvent(tableModel));
                        searchRenderer.setHighlightSearchMatchText(highlightText);
                        searchTable.tableChanged(new TableModelEvent(searchModel));
                    }
                });

        logPanelPreferenceModel.addEventListener(
                evt -> {
                    if (evt.getPropertyName().equals(LogPanelPreferenceModel.DETAIL_PANE_VISIBLE)) {
                        boolean detailPaneVisible = (Boolean) evt.getPropertyValue();
                        if (detailPaneVisible) {
                            showDetailPane();
                        } else {
                            //don't hide the detail pane if search results are being displayed
                            if (!searchResultsDisplayed) {
                                hideDetailPane();
                            }
                        }
                    }
                });

        logPanelPreferenceModel.addEventListener(
                evt -> {
                    if (evt.getPropertyName().equals(LogPanelPreferenceModel.LOG_TREE_PANEL_VISIBLE)) {
                        boolean newValue = (Boolean) evt.getPropertyValue();
                        if (newValue) {
                            showLogTreePanel();
                        } else {
                            hideLogTreePanel();
                        }
                    }
                });

        logPanelPreferenceModel.addEventListener(
                evt -> {
                    if (evt.getPropertyName().equals(LogPanelPreferenceModel.TOOL_TIPS_VISIBLE)) {
                        boolean toolTips = (Boolean) evt.getPropertyValue();
                        renderer.setToolTipsVisible(toolTips);
                        searchRenderer.setToolTipsVisible(toolTips);
                        searchToggleToolTips.setSelected(toolTips);
                        mainToggleToolTips.setSelected(toolTips);
                    }
                });

        logPanelPreferenceModel.addEventListener(
            evt -> {
            if (evt.getPropertyName().equals(LogPanelPreferenceModel.DATE_FORMAT_TIME_ZONE)) {
                isoButton.setSelected(logPanelPreferenceModel.isUseISO8601Format());
                simpleTimeButton.setSelected(!logPanelPreferenceModel.isUseISO8601Format() && !logPanelPreferenceModel.isCustomDateFormat());

                if (logPanelPreferenceModel.getDateFormatTimeZone() != null) {
                    renderer.setTimeZone(logPanelPreferenceModel.getDateFormatTimeZone());
                    searchRenderer.setTimeZone(logPanelPreferenceModel.getDateFormatTimeZone());
                }

                if (logPanelPreferenceModel.isUseISO8601Format()) {
                    renderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
                    searchRenderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
                } else {
                    try {
                        renderer.setDateFormatter(new SimpleDateFormat(logPanelPreferenceModel.getDateFormatPattern()));
                    } catch (IllegalArgumentException iae) {
                        logPanelPreferenceModel.setDefaultDatePatternFormat();
                        renderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
                    }
                    try {
                        searchRenderer.setDateFormatter(new SimpleDateFormat(logPanelPreferenceModel.getDateFormatPattern()));
                    } catch (IllegalArgumentException iae) {
                        logPanelPreferenceModel.setDefaultDatePatternFormat();
                        searchRenderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
                    }
                }

                table.tableChanged(new TableModelEvent(tableModel));
                searchTable.tableChanged(new TableModelEvent(searchModel));
            }
        });

        logPanelPreferenceModel.addEventListener(
            evt -> {
                if (evt.getPropertyName().equals(LogPanelPreferenceModel.CLEAR_TABLE_EXPRESSION)) {
                    String expression = evt.getPropertyValue().toString();
                    try {
                        clearTableExpressionRule = ExpressionRule.getRule(expression);
                        logger.info("clearTableExpressionRule set to: " + expression);
                    } catch (Exception e) {
                        logger.info("clearTableExpressionRule invalid - ignoring: " + expression);
                        clearTableExpressionRule = null;
                    }
                }
            });

        logPanelPreferenceModel.addEventListener(evt -> {
                if (!evt.isBeforeUpdate() && evt.getPropertyName().equals(LogPanelPreferenceModel.DATE_FORMAT_PATTERN) ||
                    evt.getPropertyName().equals(LogPanelPreferenceModel.DATE_FORMAT_TIME_ZONE)) {

            isoButton.setSelected(logPanelPreferenceModel.isUseISO8601Format());
            simpleTimeButton.setSelected(!logPanelPreferenceModel.isUseISO8601Format() && !logPanelPreferenceModel.isCustomDateFormat());

            if (logPanelPreferenceModel.getDateFormatTimeZone() != null && !logPanelPreferenceModel.getDateFormatTimeZone().isEmpty()) {
                renderer.setTimeZone(logPanelPreferenceModel.getDateFormatTimeZone());
                searchRenderer.setTimeZone(logPanelPreferenceModel.getDateFormatTimeZone());
            }

            if (logPanelPreferenceModel.isUseISO8601Format()) {
                renderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
                searchRenderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
            } else {
                try {
                    renderer.setDateFormatter(new SimpleDateFormat(logPanelPreferenceModel.getDateFormatPattern()));
                } catch (IllegalArgumentException iae) {
                    logPanelPreferenceModel.setDefaultDatePatternFormat();
                    renderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
                }
                try {
                    searchRenderer.setDateFormatter(new SimpleDateFormat(logPanelPreferenceModel.getDateFormatPattern()));
                } catch (IllegalArgumentException iae) {
                    logPanelPreferenceModel.setDefaultDatePatternFormat();
                    searchRenderer.setDateFormatter(new SimpleDateFormat(Constants.ISO8601_PATTERN));
                }
            }

            table.tableChanged(new TableModelEvent(tableModel));
            searchTable.tableChanged(new TableModelEvent(searchModel));
        }});

        logPanelPreferenceModel.addEventListener(
            evt -> {
                if (evt.getPropertyName().equals(LogPanelPreferenceModel.LOGGER_PRECISION)) {
                    renderer.setLoggerPrecision(logPanelPreferenceModel.getLoggerPrecision());
                    table.tableChanged(new TableModelEvent(tableModel));

                    searchRenderer.setLoggerPrecision(logPanelPreferenceModel.getLoggerPrecision());
                    searchTable.tableChanged(new TableModelEvent(searchModel));
                }
            });

        logPanelPreferenceModel.addEventListener(
            evt -> {
                if (evt.getPropertyName().equals(LogPanelPreferenceModel.LOG_TREE_PANEL_VISIBLE)) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    menuItemLoggerTree.setSelected(value);
                }
            });

        logPanelPreferenceModel.addEventListener(
            evt -> {
                if (evt.getPropertyName().equals(LogPanelPreferenceModel.DETAIL_PANE_VISIBLE)) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    menuItemToggleDetails.setSelected(value);
                }
            });

//        applicationPreferenceModel.addPropertyChangeListener("searchColor", new PropertyChangeListener() {
//            public void propertyChange(PropertyChangeEvent evt) {
//                if (table != null) {
//                    table.repaint();
//                }
//                if (searchTable != null) {
//                    searchTable.repaint();
//                }
//            }
//        });
//
//        applicationPreferenceModel.addPropertyChangeListener("alternatingColor", new PropertyChangeListener() {
//            public void propertyChange(PropertyChangeEvent evt) {
//                if (table != null) {
//                    table.repaint();
//                }
//                if (searchTable != null) {
//                    searchTable.repaint();
//                }
//            }
//        });

        /*
         *End of preferenceModel listeners
         */

        int cyclicBufferSize = applicationPreferenceModel.getCyclicBufferSize();
        tableModel = new ChainsawCyclicBufferTableModel(cyclicBufferSize, currentColorizer, "main");
        table = new JSortTable(tableModel);

        markerCellEditor = new MarkerCellEditor();
        table.setName("main");
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);

        searchModel = new ChainsawCyclicBufferTableModel(cyclicBufferSize, currentColorizer, "search");
        searchTable = new JSortTable(searchModel);

        searchTable.setName("search");
        searchTable.setColumnSelectionAllowed(false);
        searchTable.setRowSelectionAllowed(true);

        //we've mapped f2, shift f2 and ctrl-f2 to marker-related actions, unmap them from the table
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F2"), "none");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.SHIFT_MASK), "none");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK), "none");

        //we're also mapping ctrl-a to scroll-to-top, unmap from the table
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

        searchTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F2"), "none");
        searchTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.SHIFT_MASK), "none");
        searchTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");
        searchTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK), "none");

        //we're also mapping ctrl-a to scroll-to-top, unmap from the table
        searchTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

        //add a listener to update the 'refine focus'
        tableModel.addNewKeyListener(e -> columnNameKeywordMapper.put(e.getKey().toString(), "PROP." + e.getKey()));

        /*
         * Set the Display rule to use the mediator, the model will add itself as
         * a property change listener and update itself when the rule changes.
         */
        tableModel.setRuleMediator(tableRuleMediator);
        searchModel.setRuleMediator(searchRuleMediator);

        tableModel.addEventCountListener(
            (currentCount, totalCount) -> {
                if (LogPanel.this.isVisible()) {
                    statusBar.setSelectedLine(
                        table.getSelectedRow() + 1, currentCount, totalCount, getIdentifier());
                }
            });

        tableModel.addEventCountListener(
            new EventCountListener() {
                final NumberFormat formatter = NumberFormat.getPercentInstance();
                boolean warning75 = false;
                boolean warning100 = false;

                public void eventCountChanged(int currentCount, int totalCount) {
                    if (logPanelPreferenceModel.isCyclic()) {
                        double percent =
                            ((double) totalCount) / tableModel.getMaxSize();
                        String msg;
                        boolean wasWarning = warning75 || warning100;
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
                        } else {
                            //clear msg
                            msg = "";
                            warning75 = false;
                            warning100 = false;
                        }

                        if (msg != null && wasWarning) {
                            statusBar.setMessage(msg);
                        }
                    }
                }
            });

        /*
         * Logger tree panel
         *
         */

        logTreePanel = createLoggerNameTreePanel();

        /**
         * Set the LoggerRule to be the LoggerTreePanel, as this visual component
         * is a rule itself, and the RuleMediator will automatically listen when
         * it's rule state changes.
         */
        tableRuleMediator.setLoggerRule(logTreePanel.getLoggerVisibilityRule());
        searchRuleMediator.setLoggerRule(logTreePanel.getLoggerVisibilityRule());

        currentColorizer.setLoggerRule(logTreePanel.getLoggerColorRule());

        /*
         * Color rule frame and panel
         */
        colorFrame.setTitle("'" + identifier + "' color settings");
        colorFrame.setIconImage(
            ((ImageIcon) ChainsawIcons.ICON_PREFERENCES).getImage());

        allColorizers.put(identifier, currentColorizer);
        colorPanel = new ColorPanel(settingsManager, globalColorizer, filterModel, allColorizers, this);

        colorFrame.getContentPane().add(colorPanel);

        Action closeColorPanelAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                colorPanel.hidePanel();
            }
        };
        colorFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "ESCAPE");
        colorFrame.getRootPane().
            getActionMap().put("ESCAPE", closeColorPanelAction);

        colorPanel.setCloseActionListener(
            e -> colorFrame.setVisible(false));

        currentColorizer.addPropertyChangeListener(
            "colorrule",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    for (Object o : tableModel.getAllEvents()) {
                        LoggingEventWrapper loggingEventWrapper = (LoggingEventWrapper) o;
                        loggingEventWrapper.updateColorRuleColors(currentColorizer.getBackgroundColor(loggingEventWrapper.getLoggingEvent()), currentColorizer.getForegroundColor(loggingEventWrapper.getLoggingEvent()));
                    }
//          no need to update searchmodel events since tablemodel and searchmodel share all events, and color rules aren't different between the two
//          if that changes, un-do the color syncing in loggingeventwrapper & re-enable this code
//
//          for (Iterator iter = searchModel.getAllEvents().iterator();iter.hasNext();) {
//             LoggingEventWrapper loggingEventWrapper = (LoggingEventWrapper)iter.next();
//             loggingEventWrapper.updateColorRuleColors(colorizer.getBackgroundColor(loggingEventWrapper.getLoggingEvent()), colorizer.getForegroundColor(loggingEventWrapper.getLoggingEvent()));
//           }
                    colorizedEventAndSearchMatchThumbnail.configureColors();
                    lowerPanel.revalidate();
                    lowerPanel.repaint();

                    searchTable.revalidate();
                    searchTable.repaint();
                }
            });

        /*
         * Table definition.  Actual construction is above (next to tablemodel)
         */
        table.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
        table.setRowMargin(0);
        table.getColumnModel().setColumnMargin(0);
        table.setShowGrid(false);
        table.getColumnModel().addColumnModelListener(new ChainsawTableColumnModelListener(table));
        table.setAutoCreateColumnsFromModel(false);
        table.addMouseMotionListener(new TableColumnDetailMouseListener(table, tableModel));
        table.addMouseListener(new TableMarkerListener(table, tableModel, searchModel));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        searchTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
        searchTable.setRowMargin(0);
        searchTable.getColumnModel().setColumnMargin(0);
        searchTable.setShowGrid(false);
        searchTable.getColumnModel().addColumnModelListener(new ChainsawTableColumnModelListener(searchTable));
        searchTable.setAutoCreateColumnsFromModel(false);
        searchTable.addMouseMotionListener(new TableColumnDetailMouseListener(searchTable, searchModel));
        searchTable.addMouseListener(new TableMarkerListener(searchTable, searchModel, tableModel));
        searchTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);


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
        searchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(evt -> {
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
                    logPanelPreferenceModel.setScrollToBottom(false);
                }
                previousLastIndex = evt.getLastIndex();
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

        renderer = new TableColorizingRenderer(settingsManager, currentColorizer, tableModel, logPanelPreferenceModel, applicationPreferenceModel, true);
        renderer.setToolTipsVisible(logPanelPreferenceModel.isToolTipsVisible());

        table.setDefaultRenderer(Object.class, renderer);

        searchRenderer = new TableColorizingRenderer(settingsManager, currentColorizer, searchModel, logPanelPreferenceModel, applicationPreferenceModel, false);
        searchRenderer.setToolTipsVisible(logPanelPreferenceModel.isToolTipsVisible());

        searchTable.setDefaultRenderer(Object.class, searchRenderer);

        /*
         * Throwable popup
         */
        table.addMouseListener(new ThrowableDisplayMouseAdapter(table, tableModel));
        searchTable.addMouseListener(new ThrowableDisplayMouseAdapter(searchTable, searchModel));

        //select a row in the main table when a row in the search table is selected
        searchTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                LoggingEventWrapper loggingEventWrapper = searchModel.getRow(searchTable.getSelectedRow());
                if (loggingEventWrapper != null) {
                    int id = Integer.parseInt(loggingEventWrapper.getLoggingEvent().getProperty("log4jid"));
                    //preserve the table's viewble column
                    setSelectedEvent(id);
                }
            }
        });

        /*
         * We listen for new Key's coming in so we can get them automatically
         * added as columns
         */
        tableModel.addNewKeyListener(
            e -> SwingHelper.invokeOnEDT(() -> {
// don't add the column if we already know about it, this could be if we've seen it before and saved the column preferences
//this may throw an illegalargexception - ignore it because we need to add only if not already added
                //if the column is already added, don't add again

                try {
                    if (table.getColumn(e.getKey()) != null) {
                        return;
                    }
//no need to check search table - we use the same columns
                } catch (IllegalArgumentException iae) {
                }
                TableColumn col = new TableColumn(e.getNewModelIndex());
                col.setHeaderValue(e.getKey());

                if (logPanelPreferenceModel.addColumn(col)) {
                    if (logPanelPreferenceModel.isColumnVisible(col)) {
                        table.addColumn(col);
                        searchTable.addColumn(col);
                        logPanelPreferenceModel.setColumnVisible(e.getKey().toString(), true);
                    }
                }
            }));

        //if the table is refiltered, try to reselect the last selected row
        //refilter with a newValue of TRUE means refiltering is about to begin
        //refilter with a newValue of FALSE means refiltering is complete
        //assuming notification is called on the EDT so we can in the current EDT call update the scroll & selection
        tableModel.addPropertyChangeListener("refilter", new PropertyChangeListener() {
            private LoggingEventWrapper currentEvent;

            public void propertyChange(PropertyChangeEvent evt) {
                //if new value is true, filtering is about to begin
                //if new value is false, filtering is complete
                if (evt.getNewValue().equals(Boolean.TRUE)) {
                    int currentRow = table.getSelectedRow();
                    if (currentRow > -1) {
                        currentEvent = tableModel.getRow(currentRow);
                    }
                } else {
                    if (currentEvent != null) {
                        table.scrollToRow(tableModel.getRowIndex(currentEvent));
                    }
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

        final JTextField filterText = (JTextField) filterCombo.getEditor().getEditorComponent();
        final JTextField findText = (JTextField) findCombo.getEditor().getEditorComponent();

        JPanel upperPanel = createUpperPanel(filterText, findText);

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
        findMarkerRule = ExpressionRule.getRule("prop." + ChainsawConstants.LOG4J_MARKER_COL_NAME + " exists");

        tableModel.addTableModelListener(e -> {
            int currentRow = table.getSelectedRow();
            if (e.getFirstRow() <= currentRow && e.getLastRow() >= currentRow) {
//current row has changed - update
                detailPaneUpdater.setAndUpdateSelectedRow(table.getSelectedRow());
            }
        });
        addPropertyChangeListener("detailPaneConversionPattern", detailPaneUpdater);
        addPropertyChangeListener("detailPaneDatetimeFormat", detailPaneUpdater);

        searchPane = new JScrollPane(searchTable);
        searchPane.getVerticalScrollBar().setUnitIncrement(ChainsawConstants.DEFAULT_ROW_HEIGHT * 2);
        searchPane.setPreferredSize(new Dimension(900, 50));

        //default detail panel to contain detail panel - if searchResultsVisible is true, when a search if triggered, update detail pane to contain search results
        detailPane = new JScrollPane(detail);
        detailPane.setPreferredSize(new Dimension(900, 50));

        detailPanel.add(detailPane, BorderLayout.CENTER);

        JPanel eventsAndStatusPanel = new JPanel(new BorderLayout());

        eventsPane = new JScrollPane(table);
        eventsPane.getVerticalScrollBar().setUnitIncrement(ChainsawConstants.DEFAULT_ROW_HEIGHT * 2);

        eventsAndStatusPanel.add(eventsPane, BorderLayout.CENTER);

        Integer scrollBarWidth = (Integer) UIManager.get("ScrollBar.width");
        Integer height = (Integer) UIManager.get("ScrollBar.width");

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        JPanel rightThumbNailPanel = new JPanel();
        rightThumbNailPanel.setLayout(new BoxLayout(rightThumbNailPanel, BoxLayout.Y_AXIS));
        rightThumbNailPanel.add(Box.createVerticalStrut(height));
        colorizedEventAndSearchMatchThumbnail = new ColorizedEventAndSearchMatchThumbnail();
        rightThumbNailPanel.add(colorizedEventAndSearchMatchThumbnail);
        rightThumbNailPanel.add(Box.createVerticalStrut(height));
        rightPanel.add(rightThumbNailPanel);
        //set thumbnail width to be a bit narrower than scrollbar width
        rightThumbNailPanel.setPreferredSize(new Dimension(scrollBarWidth - 4, -1));
        eventsAndStatusPanel.add(rightPanel, BorderLayout.EAST);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JPanel leftThumbNailPanel = new JPanel();
        leftThumbNailPanel.setLayout(new BoxLayout(leftThumbNailPanel, BoxLayout.Y_AXIS));
        leftThumbNailPanel.add(Box.createVerticalStrut(height));
        eventTimeDeltaMatchThumbnail = new EventTimeDeltaMatchThumbnail();
        leftThumbNailPanel.add(eventTimeDeltaMatchThumbnail);
        leftThumbNailPanel.add(Box.createVerticalStrut(height));
        leftPanel.add(leftThumbNailPanel);

        //set thumbnail width to be a bit narrower than scrollbar width
        leftThumbNailPanel.setPreferredSize(new Dimension(scrollBarWidth - 4, -1));
        eventsAndStatusPanel.add(leftPanel, BorderLayout.WEST);

        final JPanel statusLabelPanel = new JPanel();
        statusLabelPanel.setLayout(new BorderLayout());

        statusLabelPanel.add(upperPanel, BorderLayout.CENTER);
        eventsAndStatusPanel.add(statusLabelPanel, BorderLayout.NORTH);

        /*
         * Detail panel layout editor
         */
        detailToolbar = new JToolBar(SwingConstants.HORIZONTAL);
        detailToolbar.setFloatable(false);

        final LayoutEditorPane layoutEditorPane = new LayoutEditorPane();
        final JDialog layoutEditorDialog =
            new JDialog((JFrame) null, "Pattern Editor");
        layoutEditorDialog.getContentPane().add(layoutEditorPane);
        layoutEditorDialog.setSize(640, 480);

        layoutEditorPane.addCancelActionListener(
            e -> layoutEditorDialog.setVisible(false));

        layoutEditorPane.addOkActionListener(
            e -> {
                setDetailPaneConversionPattern(
                    layoutEditorPane.getConversionPattern());
                setDetailPaneDatetimeFormat(layoutEditorPane.getDatetimeFormatter());
                layoutEditorDialog.setVisible(false);
            });

        Action copyToRefineFocusAction = new AbstractAction("Set 'refine focus' field") {
            public void actionPerformed(ActionEvent e) {
                String selectedText = detail.getSelectedText();
                if (selectedText == null || selectedText.isEmpty()) {
                    //no-op empty searches
                    return;
                }
                filterText.setText("msg ~= '" + selectedText + "'");
            }
        };

        Action copyToSearchAction = new AbstractAction("Find next") {
            public void actionPerformed(ActionEvent e) {
                String selectedText = detail.getSelectedText();
                if (selectedText == null || selectedText.isEmpty()) {
                    //no-op empty searches
                    return;
                }
                findCombo.setSelectedItem("msg ~= '" + selectedText + "'");
                findNext();
            }
        };

        Action editDetailAction =
            new AbstractAction(
                "Edit...", new ImageIcon(ChainsawIcons.ICON_EDIT_RECEIVER)) {
                public void actionPerformed(ActionEvent e) {
                    layoutEditorPane.setConversionPattern(
                        getDetailPaneConversionPattern());
                    layoutEditorPane.setDatetimeFormatter(getDetailPaneDatetimeFormat());

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
                    hideDetailPane();
                }
            };

        closeDetailAction.putValue(
            Action.SHORT_DESCRIPTION, "Hides the Detail Panel");

        SmallButton closeDetailButton = new SmallButton(closeDetailAction);
        detailToolbar.add(closeDetailButton);

        detailPanel.add(detailToolbar, BorderLayout.NORTH);

        lowerPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, eventsAndStatusPanel, detailPanel);

        dividerSize = lowerPanel.getDividerSize();
        lowerPanel.setDividerLocation(logPanelPreferenceModel.getLowerPanelDividerLocation());

        lowerPanel.setResizeWeight(1.0);
        lowerPanel.setBorder(null);
        lowerPanel.setContinuousLayout(true);

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
//                        setDetailPaneConversionPattern(
//                            PatternLayout.TTCC_CONVERSION_PATTERN);
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
        nameTreeAndMainPanelSplit.setDividerLocation(-1);
        nameTreeAndMainPanelSplit.setContinuousLayout(true);

        add(nameTreeAndMainPanelSplit, BorderLayout.CENTER);

        if (isLogTreeVisible()) {
            showLogTreePanel();
        } else {
            hideLogTreePanel();
        }

        /*
         * Other menu items
         */
        class BestFit extends JMenuItem {
            public BestFit() {
                super("Best fit column");
                addActionListener(
                    evt -> {
                        if (currentPoint != null) {
                            int column = currentTable.columnAtPoint(currentPoint);
                            int maxWidth = getMaxColumnWidth(column);
                            currentTable.getColumnModel().getColumn(column).setPreferredWidth(
                                maxWidth);
                        }
                    });
            }
        }

        class ColorPanel extends JMenuItem {
            public ColorPanel() {
                super("Color settings...");
                setIcon(ChainsawIcons.ICON_PREFERENCES);
                addActionListener(
                    evt -> showColorPreferences());
            }
        }

        class LogPanelPreferences extends JMenuItem {
            public LogPanelPreferences() {
                super("Tab Preferences...");
                setIcon(ChainsawIcons.ICON_PREFERENCES);
                addActionListener(
                    evt -> showPreferences());
            }
        }

        class FocusOn extends JMenuItem {
            public FocusOn() {
                super("Set 'refine focus' field to value under pointer");
                addActionListener(
                    evt -> {
                        if (currentPoint != null) {
                            String operator = "==";
                            int column = currentTable.columnAtPoint(currentPoint);
                            int row = currentTable.rowAtPoint(currentPoint);
                            String colName = currentTable.getColumnName(column).toUpperCase();
                            String value = getValueOf(row, column);

                            if (columnNameKeywordMapper.contains(colName)) {
                                filterText.setText(
                                    columnNameKeywordMapper.get(colName) + " " + operator
                                        + " '" + value + "'");
                            }
                        }
                    });
            }
        }

        class DefineAddCustomFilter extends JMenuItem {
            public DefineAddCustomFilter() {
                super("Add value under pointer to 'refine focus' field");
                addActionListener(
                    evt -> {
                        if (currentPoint != null) {
                            String operator = "==";
                            int column = currentTable.columnAtPoint(currentPoint);
                            int row = currentTable.rowAtPoint(currentPoint);
                            String value = getValueOf(row, column);
                            String colName = currentTable.getColumnName(column).toUpperCase();

                            if (columnNameKeywordMapper.contains(colName)) {
                                filterText.setText(
                                    filterText.getText() + " && "
                                        + columnNameKeywordMapper.get(colName) + " "
                                        + operator + " '" + value + "'");
                            }

                        }
                    });
            }
        }

        class DefineAddCustomFind extends JMenuItem {
            public DefineAddCustomFind() {
                super("Add value under pointer to 'find' field");
                addActionListener(
                    evt -> {
                        if (currentPoint != null) {
                            String operator = "==";
                            int column = currentTable.columnAtPoint(currentPoint);
                            int row = currentTable.rowAtPoint(currentPoint);
                            String value = getValueOf(row, column);
                            String colName = currentTable.getColumnName(column).toUpperCase();

                            if (columnNameKeywordMapper.contains(colName)) {
                                findCombo.setSelectedItem(
                                    findText.getText() + " && "
                                        + columnNameKeywordMapper.get(colName) + " "
                                        + operator + " '" + value + "'");
                                findNext();
                            }
                        }
                    });
            }
        }

        class BuildColorRule extends JMenuItem {
            public BuildColorRule() {
                super("Define color rule for value under pointer");
                addActionListener(
                    evt -> {
                        if (currentPoint != null) {
                            String operator = "==";
                            int column = currentTable.columnAtPoint(currentPoint);
                            int row = currentTable.rowAtPoint(currentPoint);
                            String colName = currentTable.getColumnName(column).toUpperCase();
                            String value = getValueOf(row, column);

                            if (columnNameKeywordMapper.contains(colName)) {
                                Color c = JColorChooser.showDialog(getRootPane(), "Choose a color", Color.red);
                                if (c != null) {
                                    String expression = columnNameKeywordMapper.get(colName) + " " + operator + " '" + value + "'";
                                    currentColorizer.addRule(new ColorRule(expression,
                                        ExpressionRule.getRule(expression), c, ChainsawConstants.COLOR_DEFAULT_FOREGROUND));
                                }
                            }
                        }
                    });
            }
        }

        final JPopupMenu mainPopup = new JPopupMenu();
        final JPopupMenu searchPopup = new JPopupMenu();

        class ClearFocus extends AbstractAction {
            public ClearFocus() {
                super("Clear 'refine focus' field");
            }

            public void actionPerformed(ActionEvent e) {
                filterText.setText(null);
                tableRuleMediator.setFilterRule(null);
                searchRuleMediator.setFilterRule(null);
            }
        }

        class CopySelection extends AbstractAction {
            public CopySelection() {
                super("Copy selection to clipboard");
            }

            public void actionPerformed(ActionEvent e) {
                if (currentTable == null) {
                    return;
                }
                int start = currentTable.getSelectionModel().getMinSelectionIndex();
                int end = currentTable.getSelectionModel().getMaxSelectionIndex();
                StringBuilder result = new StringBuilder();
                for (int row = start; row < end + 1; row++) {
                    for (int column = 0; column < currentTable.getColumnCount(); column++) {
                        result.append(getValueOf(row, column));
                        if (column != (currentTable.getColumnCount() - 1)) {
                            result.append(" - ");
                        }
                    }
                    result.append(System.getProperty("line.separator"));
                }
                StringSelection selection = new StringSelection(result.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, null);
            }
        }

        class CopyField extends AbstractAction {
            public CopyField() {
                super("Copy value under pointer to clipboard");
            }

            public void actionPerformed(ActionEvent e) {
                if (currentPoint != null && currentTable != null) {
                    int column = currentTable.columnAtPoint(currentPoint);
                    int row = currentTable.rowAtPoint(currentPoint);
                    String value = getValueOf(row, column);
                    StringSelection selection = new StringSelection(value);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, null);
                }
            }
        }
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
        mainPopup.add(new FocusOn());
        searchPopup.add(new FocusOn());
        mainPopup.add(new DefineAddCustomFilter());
        searchPopup.add(new DefineAddCustomFilter());
        mainPopup.add(new ClearFocus());
        searchPopup.add(new ClearFocus());

        mainPopup.add(new JSeparator());
        searchPopup.add(new JSeparator());

        class Search extends JMenuItem {
            public Search() {
                super("Find value under pointer");

                addActionListener(
                    evt -> {
                        if (currentPoint != null) {
                            String operator = "==";
                            int column = currentTable.columnAtPoint(currentPoint);
                            int row = currentTable.rowAtPoint(currentPoint);
                            String colName = currentTable.getColumnName(column).toUpperCase();
                            String value = getValueOf(row, column);
                            if (columnNameKeywordMapper.contains(colName)) {
                                findCombo.setSelectedItem(
                                    columnNameKeywordMapper.get(colName) + " " + operator
                                        + " '" + value + "'");
                                findNext();
                            }
                        }
                    });
            }
        }

        class ClearSearch extends AbstractAction {
            public ClearSearch() {
                super("Clear find field");
            }

            public void actionPerformed(ActionEvent e) {
                findCombo.setSelectedItem(null);
                updateFindRule(null);
            }
        }

        mainPopup.add(new Search());
        searchPopup.add(new Search());
        mainPopup.add(new DefineAddCustomFind());
        searchPopup.add(new DefineAddCustomFind());
        mainPopup.add(new ClearSearch());
        searchPopup.add(new ClearSearch());

        mainPopup.add(new JSeparator());
        searchPopup.add(new JSeparator());

        class DisplayNormalTimes extends JMenuItem {
            public DisplayNormalTimes() {
                super("Hide relative times");
                addActionListener(
                    e -> {
                        if (currentPoint != null) {
                            ((TableColorizingRenderer) currentTable.getDefaultRenderer(Object.class)).setUseNormalTimes();
                            ((ChainsawCyclicBufferTableModel) currentTable.getModel()).reFilter();
                            setEnabled(true);
                        }
                    });
            }
        }

        class DisplayRelativeTimesToRowUnderCursor extends JMenuItem {
            public DisplayRelativeTimesToRowUnderCursor() {
                super("Show times relative to this event");
                addActionListener(
                    e -> {
                        if (currentPoint != null) {
                            int row = currentTable.rowAtPoint(currentPoint);
                            ChainsawCyclicBufferTableModel cyclicBufferTableModel = (ChainsawCyclicBufferTableModel) currentTable.getModel();
                            LoggingEventWrapper loggingEventWrapper = cyclicBufferTableModel.getRow(row);
                            if (loggingEventWrapper != null) {
                                ((TableColorizingRenderer) currentTable.getDefaultRenderer(Object.class)).setUseRelativeTimes(loggingEventWrapper.getLoggingEvent().m_timestamp.atZone(ZoneId.systemDefault()));
                                cyclicBufferTableModel.reFilter();
                            }
                            setEnabled(true);
                        }
                    });
            }
        }

        class DisplayRelativeTimesToPreviousRow extends JMenuItem {
            public DisplayRelativeTimesToPreviousRow() {
                super("Show times relative to previous rows");
                addActionListener(
                    e -> {
                        if (currentPoint != null) {
                            ((TableColorizingRenderer) currentTable.getDefaultRenderer(Object.class)).setUseRelativeTimesToPreviousRow();
                            ((ChainsawCyclicBufferTableModel) currentTable.getModel()).reFilter();
                            setEnabled(true);
                        }
                    });
            }
        }

        mainPopup.add(new DisplayRelativeTimesToRowUnderCursor());
        searchPopup.add(new DisplayRelativeTimesToRowUnderCursor());
        mainPopup.add(new DisplayRelativeTimesToPreviousRow());
        searchPopup.add(new DisplayRelativeTimesToPreviousRow());
        mainPopup.add(new DisplayNormalTimes());
        searchPopup.add(new DisplayNormalTimes());
        mainPopup.add(new JSeparator());
        searchPopup.add(new JSeparator());

        mainPopup.add(new BuildColorRule());
        searchPopup.add(new BuildColorRule());
        mainPopup.add(new JSeparator());
        searchPopup.add(new JSeparator());
        mainPopup.add(new CopyField());
        mainPopup.add(new CopySelection());
        searchPopup.add(new CopyField());
        searchPopup.add(new CopySelection());
        mainPopup.add(new JSeparator());
        searchPopup.add(new JSeparator());

        mainPopup.add(menuItemToggleDetails);
        mainPopup.add(menuItemLoggerTree);
        mainToggleToolTips = new ToggleToolTips();
        searchToggleToolTips = new ToggleToolTips();
        mainPopup.add(mainToggleToolTips);
        searchPopup.add(searchToggleToolTips);

        mainPopup.add(new JSeparator());

        mainPopup.add(menuItemToggleDock);

        mainPopup.add(new BestFit());
        searchPopup.add(new BestFit());

        mainPopup.add(new JSeparator());

        mainPopup.add(new ColorPanel());
        searchPopup.add(new ColorPanel());
        mainPopup.add(new LogPanelPreferences());
        searchPopup.add(new LogPanelPreferences());

        final PopupListener mainTablePopupListener = new PopupListener(mainPopup);
        eventsPane.addMouseListener(mainTablePopupListener);
        table.addMouseListener(mainTablePopupListener);

        table.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mousePressed(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mouseReleased(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mouseEntered(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mouseExited(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            private void checkMultiSelect(MouseEvent mouseEvent) {
                if (mouseEvent.isAltDown()) {
                    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                } else {
                    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                }
            }
        });


        searchTable.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mousePressed(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mouseReleased(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mouseEntered(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            public void mouseExited(MouseEvent mouseEvent) {
                checkMultiSelect(mouseEvent);
            }

            private void checkMultiSelect(MouseEvent mouseEvent) {
                if (mouseEvent.isAltDown()) {
                    searchTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                } else {
                    searchTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                }
            }
        });


        final PopupListener searchTablePopupListener = new PopupListener(searchPopup);
        searchPane.addMouseListener(searchTablePopupListener);
        searchTable.addMouseListener(searchTablePopupListener);

        loadSettings();
        loadColumnSettings();
    }

    public void saveSettings() {
        if (isDetailVisible()) {
            logPanelPreferenceModel.setLowerPanelDividerLocation(lowerPanel.getDividerLocation());
        }
        logPanelPreferenceModel.setDetailPaneVisible(isDetailVisible());
        if (isLogTreeVisible()) {
            logPanelPreferenceModel.setLogTreeDividerLocation(nameTreeAndMainPanelSplit.getDividerLocation());
        }
        logPanelPreferenceModel.setLogTreePanelVisible(isLogTreeVisible());
    }

    private LoggerNameTreePanel createLoggerNameTreePanel() {
        final LoggerNameTreePanel logTreePanel;
        LogPanelLoggerTreeModel logTreeModel = new LogPanelLoggerTreeModel();
        logTreePanel = new LoggerNameTreePanel(logTreeModel, logPanelPreferenceModel, this, currentColorizer, filterModel);
        logTreePanel.getLoggerVisibilityRule().addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("searchExpression")) {
                findCombo.setSelectedItem(evt.getNewValue().toString());
                findNext();
            }
        });

        tableModel.addLoggerNameListener(logTreeModel);
        tableModel.addLoggerNameListener(logTreePanel);
        return logTreePanel;
    }

    private JPanel createUpperPanel(JTextField filterText, JTextField findText) {
        /*
         * Upper panel definition
         */
        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new BoxLayout(upperPanel, BoxLayout.X_AXIS));
        upperPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 0));

        final JLabel filterLabel = new JLabel("Refine focus on: ");
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));

        upperPanel.add(filterLabel);
        upperPanel.add(Box.createHorizontalStrut(3));
        upperPanel.add(filterCombo);
        upperPanel.add(Box.createHorizontalStrut(3));

        //Adding a button to clear filter expressions which are currently remembered by Chainsaw...
        final JButton removeFilterButton = new JButton(" Remove ");

        removeFilterButton.setToolTipText("Click here to remove the selected expression from the list");
        removeFilterButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    Object selectedItem = filterCombo.getSelectedItem();
                    if (e.getSource() == removeFilterButton && selectedItem != null && !selectedItem.toString().trim().isEmpty()) {
                        //don't just remove the entry from the store, clear the field
                        int index = filterCombo.getSelectedIndex();
                        filterText.setText(null);
                        filterCombo.setSelectedIndex(-1);
                        filterCombo.removeItemAt(index);
                        if (!(findCombo.getSelectedItem() != null && findCombo.getSelectedItem().equals(selectedItem))) {
                            //now remove the entry from the other model
                            ((AutoFilterComboBox.AutoFilterComboBoxModel) findCombo.getModel()).removeElement(selectedItem);
                        }
                    }
                }
            }
        );
        upperPanel.add(removeFilterButton);
        //add some space between refine focus and search sections of the panel
        upperPanel.add(Box.createHorizontalStrut(25));

        final JLabel findLabel = new JLabel("Find: ");
        findLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));

        upperPanel.add(findLabel);
        upperPanel.add(Box.createHorizontalStrut(3));

        upperPanel.add(findCombo);
        upperPanel.add(Box.createHorizontalStrut(3));

        //add up & down search
        upperPanel.add(ElementFactory.createFindNextButton(this::findNext));
        upperPanel.add(ElementFactory.createFindPreviousButton(this::findPrevious));

        upperPanel.add(Box.createHorizontalStrut(3));

        //Adding a button to clear filter expressions which are currently remembered by Chainsaw...
        final JButton removeFindButton = new JButton(" Remove ");
        removeFindButton.setToolTipText("Click here to remove the selected expression from the list");
        removeFindButton.addActionListener(
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    Object selectedItem = findCombo.getSelectedItem();
                    if (e.getSource() == removeFindButton && selectedItem != null && !selectedItem.toString().trim().isEmpty()) {
                        //don't just remove the entry from the store, clear the field
                        int index = findCombo.getSelectedIndex();
                        findText.setText(null);
                        findCombo.setSelectedIndex(-1);
                        findCombo.removeItemAt(index);
                        if (!(filterCombo.getSelectedItem() != null && filterCombo.getSelectedItem().equals(selectedItem))) {
                            //now remove the entry from the other model if it wasn't selected
                            ((AutoFilterComboBox.AutoFilterComboBoxModel) filterCombo.getModel()).removeElement(selectedItem);
                        }
                    }
                }
            }
        );
        upperPanel.add(removeFindButton);

        //define search and refine focus selection and clear actions
        Action findFocusAction = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                findCombo.requestFocus();
            }
        };

        Action filterFocusAction = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                filterCombo.requestFocus();
            }
        };

        Action findClearAction = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                findCombo.setSelectedIndex(-1);
                findNext();
            }
        };

        Action filterClearAction = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                setRefineFocusText("");
                filterCombo.refilter();
            }
        };

        //now add them to the action and input maps for the logpanel
        KeyStroke ksFindFocus =
            KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke ksFilterFocus =
            KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke ksFindClear =
            KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke ksFilterClear =
            KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksFindFocus, "FindFocus");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksFilterFocus, "FilterFocus");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksFindClear, "FindClear");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksFilterClear, "FilterClear");

        getActionMap().put("FindFocus", findFocusAction);
        getActionMap().put("FilterFocus", filterFocusAction);
        getActionMap().put("FindClear", findClearAction);
        getActionMap().put("FilterClear", filterClearAction);

        return upperPanel;
    }


    private String getValueOf(int row, int column) {
        if (currentTable == null) {
            return "";
        }

        Object o = currentTable.getValueAt(row, column);

        if (o instanceof Date) {
            return TIMESTAMP_DATE_FORMAT.format((Date) o);
        }

        if (o instanceof String) {
            return (String) o;
        }

        if (o instanceof org.apache.log4j.chainsaw.logevents.Level) {
            return o.toString();
        }

        if (o instanceof String[]) {
            StringBuilder value = new StringBuilder();
            //exception - build message + throwable
            String[] ti = (String[]) o;
            if (ti.length > 0 && (!(ti.length == 1 && ti[0].isEmpty()))) {
                LoggingEventWrapper loggingEventWrapper = ((ChainsawCyclicBufferTableModel) (currentTable.getModel())).getRow(row);
                value = new StringBuilder(loggingEventWrapper.getLoggingEvent().m_message);
                for (int i = 0; i < ((String[]) o).length; i++) {
                    value.append('\n').append(((String[]) o)[i]);
                }
            }
            return value.toString();
        }
        return "";
    }

    private void buildCombo(final AutoFilterComboBox combo, boolean isFiltering, final AutoFilterComboBox.AutoFilterComboBoxModel otherModel) {
        //add (hopefully useful) default filters
        combo.addItem("LEVEL == TRACE");
        combo.addItem("LEVEL >= DEBUG");
        combo.addItem("LEVEL >= INFO");
        combo.addItem("LEVEL >= WARN");
        combo.addItem("LEVEL >= ERROR");
        combo.addItem("LEVEL == FATAL");

        final JTextField filterText = (JTextField) combo.getEditor().getEditorComponent();
        if (isFiltering) {
            filterText.getDocument().addDocumentListener(new DelayedTextDocumentListener(filterText));
        }
        filterText.setToolTipText("Enter an expression - right click or ctrl-space for menu - press enter to add to list");
        filterText.addKeyListener(new ExpressionRuleContext(filterModel, filterText));

        if (combo.getEditor().getEditorComponent() instanceof JTextField) {
            combo.addActionListener(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (e.getActionCommand().equals("comboBoxEdited")) {
                            try {
                                //verify the expression is valid
                                Object item = combo.getSelectedItem();
                                if (item != null && !item.toString().trim().isEmpty()) {
                                    ExpressionRule.getRule(item.toString());
                                    //add entry as first row of the combo box
                                    combo.insertItemAt(item, 0);
                                    otherModel.insertElementAt(item, 0);
                                }
                                //valid expression, reset background color in case we were previously an invalid expression
                                filterText.setBackground(UIManager.getColor("TextField.background"));
                            } catch (IllegalArgumentException iae) {
                                //don't add expressions that aren't valid
                                //invalid expression, change background of the field
                                filterText.setToolTipText(iae.getMessage());
                                filterText.setBackground(ChainsawConstants.INVALID_EXPRESSION_BACKGROUND);
                            }
                        }
                    }
                });
        }
    }

    /**
     * Accessor
     *
     * @return scrollToBottom
     */
    public boolean isScrollToBottom() {
        return logPanelPreferenceModel.isScrollToBottom();
    }

    public void setRefineFocusText(String refineFocusText) {
        final JTextField filterText = (JTextField) filterCombo.getEditor().getEditorComponent();
        filterText.setText(refineFocusText);
    }

    public String getRefineFocusText() {
        final JTextField filterText = (JTextField) filterCombo.getEditor().getEditorComponent();
        return filterText.getText();
    }

    /**
     * Mutator
     */
    public void toggleScrollToBottom() {
        logPanelPreferenceModel.setScrollToBottom(!logPanelPreferenceModel.isScrollToBottom());
    }

    private void scrollToBottom() {
        //run this in an invokeLater block to ensure this action is enqueued to the end of the EDT
        EventQueue.invokeLater(() -> {
            int scrollRow = tableModel.getRowCount() - 1;
            table.scrollToRow(scrollRow);
        });
    }

    public void scrollToTop() {
        EventQueue.invokeLater(() -> {
            if (tableModel.getRowCount() > 1) {
                table.scrollToRow(0);
            }
        });
    }

    /**
     * Accessor
     *
     * @return namespace
     * @see Profileable
     */
    public String getNamespace() {
        return getIdentifier();
    }

    /**
     * Accessor
     *
     * @return identifier
     */
    public String getInterestedIdentifier() {
        return getIdentifier();
    }

    /**
     * Load settings from the panel preference model
     */
    private void loadSettings() {
        logger.info("Loading settings for panel with identifier {}", identifier);

        //TODO: Add column support
        boolean isCyclic = logPanelPreferenceModel.isCyclic();
        tableModel.setCyclic(isCyclic);
        searchModel.setCyclic(isCyclic);
        lowerPanel.setDividerLocation(logPanelPreferenceModel.getLowerPanelDividerLocation());
        nameTreeAndMainPanelSplit.setDividerLocation(logPanelPreferenceModel.getLogTreeDividerLocation());
        detailLayout.setConversionPattern(logPanelPreferenceModel.getConversionPattern());
        undockedFrame.setLocation(0, 0);
        undockedFrame.setSize(new Dimension(1024, 768));

        logTreePanel.ignore(logPanelPreferenceModel.getHiddenLoggers());
        logTreePanel.setHiddenExpression(logPanelPreferenceModel.getHiddenExpression());
        logTreePanel.setAlwaysDisplayExpression(logPanelPreferenceModel.getAlwaysDisplayExpression());
        String clearTableExpression = logPanelPreferenceModel.getClearTableExpression();
        if (clearTableExpression != null && clearTableExpression.length() > 1) {
            try {
                clearTableExpressionRule = ExpressionRule.getRule(clearTableExpression);
            } catch (Exception e) {
                clearTableExpressionRule = null;
            }
        }

        setRuleColorizer(new RuleColorizer());

//        if (xmlFile.exists()) {
//            XStream stream = buildXStreamForLogPanelPreference();
//            ObjectInputStream in = null;
//            try {
//                logger.info("configuration for panel exists: " + xmlFile + " - " + identifier + ", loading");
//                FileReader r = new FileReader(xmlFile);
//                in = stream.createObjectInputStream(r);
//                LogPanelPreferenceModel storedPrefs = (LogPanelPreferenceModel) in.readObject();
//                lowerPanelDividerLocation = in.readInt();
//                int treeDividerLocation = in.readInt();
//                String conversionPattern = in.readObject().toString();
//                //this version number is checked to identify whether there is a Vector comming next
//                int versionNumber = 0;
//                try {
//                    versionNumber = in.readInt();
//                } catch (EOFException eof) {
//                }
//
//                Vector savedVector;
//                //read the vector only if the version number is greater than 0. higher version numbers can be
//                //used in the future to save more data structures
//                if (versionNumber > 0) {
//                    savedVector = (Vector) in.readObject();
//                    for (Object item : savedVector) {
//                        //insert each row at index zero (so last row in vector will be row zero)
//                        filterCombo.insertItemAt(item, 0);
//                        findCombo.insertItemAt(item, 0);
//                    }
//                    if (versionNumber > 1) {
//                        //update prefModel columns to include defaults
//                        int index = 0;
//                        String columnOrder = event.getSetting(TABLE_COLUMN_ORDER);
//                        StringTokenizer tok = new StringTokenizer(columnOrder, ",");
//                        while (tok.hasMoreElements()) {
//                            String element = tok.nextElement().toString().trim().toUpperCase();
//                            TableColumn column = new TableColumn(index++);
//                            column.setHeaderValue(element);
//                            preferenceModel.addColumn(column);
//                        }
//
//                        TableColumnModel columnModel = table.getColumnModel();
//                        //remove previous columns
//                        while (columnModel.getColumnCount() > 0) {
//                            columnModel.removeColumn(columnModel.getColumn(0));
//                        }
//                        //add visible column order columns
//                        for (Object o1 : preferenceModel.getVisibleColumnOrder()) {
//                            TableColumn col = (TableColumn) o1;
//                            columnModel.addColumn(col);
//                        }
//
//                        TableColumnModel searchColumnModel = searchTable.getColumnModel();
//                        //remove previous columns
//                        while (searchColumnModel.getColumnCount() > 0) {
//                            searchColumnModel.removeColumn(searchColumnModel.getColumn(0));
//                        }
//                        //add visible column order columns
//                        for (Object o : preferenceModel.getVisibleColumnOrder()) {
//                            TableColumn col = (TableColumn) o;
//                            searchColumnModel.addColumn(col);
//                        }
//
//                        preferenceModel.apply(storedPrefs);
//                    } else {
//                        loadDefaultColumnSettings(event);
//                    }
//                    //ensure tablemodel cyclic flag is updated
//                    //may be panel configs that don't have these values
//                    tableModel.setCyclic(preferenceModel.isCyclic());
//                    searchModel.setCyclic(preferenceModel.isCyclic());
//                    lowerPanel.setDividerLocation(lowerPanelDividerLocation);
//                    nameTreeAndMainPanelSplit.setDividerLocation(treeDividerLocation);
//                    detailLayout.setConversionPattern(conversionPattern);
//                    undockedFrame.setLocation(0, 0);
//                    undockedFrame.setSize(new Dimension(1024, 768));
//                } else {
//                    loadDefaultColumnSettings(event);
//                }
//            } catch (Exception e) {
//                logger.info("unable to load configuration for panel: " + xmlFile + " - " + identifier + " - using default settings", e);
//                loadDefaultColumnSettings(event);
//                // TODO need to log this..
//            } finally {
//                if (in != null) {
//                    try {
//                        in.close();
//                    } catch (IOException ioe) {
//                    }
//                }
//            }
//        } else {
//            //not setting lower panel divider location here - will do that after the UI is visible
//            loadDefaultColumnSettings(event);
//        }
//        //ensure tablemodel cyclic flag is updated
//        tableModel.setCyclic(preferenceModel.isCyclic());
//        searchModel.setCyclic(preferenceModel.isCyclic());
//        logTreePanel.ignore(preferenceModel.getHiddenLoggers());
//        logTreePanel.setHiddenExpression(preferenceModel.getHiddenExpression());
//        logTreePanel.setAlwaysDisplayExpression(preferenceModel.getAlwaysDisplayExpression());
//        if (preferenceModel.getClearTableExpression() != null) {
//            try {
//                clearTableExpressionRule = ExpressionRule.getRule(preferenceModel.getClearTableExpression());
//            } catch (Exception e) {
//                clearTableExpressionRule = null;
//            }
//        }
//
//        //attempt to load color settings - no need to URL encode the identifier
//        colorizer.loadColorSettings(identifier);
    }

    public void setRuleColorizer(RuleColorizer newRuleColorizer) {
        if (newRuleColorizer == currentColorizer) {
            return;
        }

        currentColorizer = newRuleColorizer;
        allColorizers.put(identifier, currentColorizer);
    }

    public RuleColorizer getCurrentRuleColorizer() {
        return currentColorizer;
    }

    /**
     * Display the panel preferences frame
     */
    public void showPreferences() {
        //don't pack this frame
        centerAndSetVisible(logPanelPreferencesFrame);
    }

    public static void centerAndSetVisible(Window window) {
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        window.setLocation(new Point((screenDimension.width / 2) - (window.getSize().width / 2),
            (screenDimension.height / 2) - (window.getSize().height / 2)));
        window.setVisible(true);
    }

    /**
     * Display the color rule frame
     */
    public void showColorPreferences() {
        colorPanel.componentChanged();
        colorFrame.pack();
        centerAndSetVisible(colorFrame);
    }

    /**
     * Toggle panel preference for detail visibility on or off
     */
    public void toggleDetailVisible() {
        boolean visible = logPanelPreferenceModel.isDetailPaneVisible();
        logPanelPreferenceModel.setDetailPaneVisible(!visible);
    }

    /**
     * Accessor
     *
     * @return detail visibility flag
     */
    public boolean isDetailVisible() {
        return logPanelPreferenceModel.isDetailPaneVisible();
    }

    boolean isSearchResultsVisible() {
        return logPanelPreferenceModel.isSearchResultsVisible();
    }

    /**
     * Toggle panel preference for logger tree visibility on or off
     */
    public void toggleLogTreeVisible() {
        boolean visible = logPanelPreferenceModel.isLogTreePanelVisible();
        logPanelPreferenceModel.setLogTreePanelVisible(!visible);
    }

    /**
     * Accessor
     *
     * @return logger tree visibility flag
     */
    public boolean isLogTreeVisible() {
        return logPanelPreferenceModel.isLogTreePanelVisible();
    }

    /**
     * Return all events
     *
     * @return list of LoggingEvents
     */
    List<LoggingEventWrapper> getEvents() {
        return tableModel.getAllEvents();
    }

    /**
     * Return the events that are visible with the current filter applied
     *
     * @return list of LoggingEvents
     */
    public List<LoggingEventWrapper> getFilteredEvents() {
        return tableModel.getFilteredEvents();
    }

    public List<LoggingEventWrapper> getMatchingEvents(Rule rule) {
        return tableModel.getMatchingEvents(rule);
    }

    /**
     * Remove all events
     */
    public void clearEvents() {
        clearModel();
    }

    /**
     * Accessor
     *
     * @return identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Undocks this DockablePanel by removing the panel from the LogUI window
     * and placing it inside it's own JFrame.
     */
    public void undock() {
        final int row = table.getSelectedRow();
        setDocked(false);
        externalPanel.removeAll();

        externalPanel.add(undockedToolbar, BorderLayout.NORTH);
        externalPanel.add(nameTreeAndMainPanelSplit, BorderLayout.CENTER);
        externalPanel.setDocked(false);
        undockedFrame.pack();

        undockedFrame.setVisible(true);
        dockingAction.putValue(Action.NAME, "Dock");
        dockingAction.putValue(Action.SMALL_ICON, ChainsawIcons.ICON_DOCK);
        if (row > -1) {
            EventQueue.invokeLater(() -> table.scrollToRow(row));
        }
    }

    /**
     * Add an eventCountListener
     *
     * @param l
     */
    public void addEventCountListener(EventCountListener l) {
        tableModel.addEventCountListener(l);
    }

    /**
     * Accessor
     *
     * @return paused flag
     */
    public boolean isPaused() {
        return logPanelPreferenceModel.isPaused();
    }

    /**
     * Modifies the Paused property and notifies the listeners
     *
     * @param paused
     */
    public void setPaused(boolean paused) {
        logPanelPreferenceModel.setPaused(paused);
    }

    /**
     * Change the selected event on the log panel.  Will cause scrollToBottom to be turned off.
     *
     * @param eventNumber
     * @return row number or -1 if row with log4jid property with that number was not found
     */
    public int setSelectedEvent(int eventNumber) {
        int row = tableModel.locate(ExpressionRule.getRule("prop.log4jid == " + eventNumber), 0, true);
        if (row > -1) {
            logPanelPreferenceModel.setScrollToBottom(false);

            table.scrollToRow(row);
        }
        return row;
    }

    /**
     * Toggle the LoggingEvent container from either managing a cyclic buffer of
     * events or an ArrayList of events
     */
    public void toggleCyclic() {
        boolean toggledCyclic = !logPanelPreferenceModel.isCyclic();

        logPanelPreferenceModel.setCyclic(toggledCyclic);
        tableModel.setCyclic(toggledCyclic);
        searchModel.setCyclic(toggledCyclic);
    }

    /**
     * Accessor
     *
     * @return flag answering if LoggingEvent container is a cyclic buffer
     */
    public boolean isCyclic() {
        return logPanelPreferenceModel.isCyclic();
    }

    public void updateFindRule(String ruleText) {
        if ((ruleText == null) || (ruleText.trim().isEmpty())) {
            findRule = null;
            tableModel.updateEventsWithFindRule(null);
            currentColorizer.setFindRule(null);
            tableRuleMediator.setFindRule(null);
            searchRuleMediator.setFindRule(null);
            //reset background color in case we were previously an invalid expression
            findCombo.setBackground(UIManager.getColor("TextField.background"));
            findCombo.setToolTipText(
                "Enter an expression - right click or ctrl-space for menu - press enter to add to list");
            currentSearchMatchCount = 0;
            currentFindRuleText = null;
            statusBar.setSearchMatchCount(currentSearchMatchCount, getIdentifier());
            //if the preference to show search results is enabled, the find rule is now null - hide search results
            if (isSearchResultsVisible()) {
                hideSearchResults();
            }
        } else {
            //only turn off scrolltobottom when finding something (find not empty)
            logPanelPreferenceModel.setScrollToBottom(false);
            if (ruleText.equals(currentFindRuleText)) {
                //don't update events if rule hasn't changed (we're finding next/previous)
                return;
            }
            currentFindRuleText = ruleText;
            try {
                final JTextField findText = (JTextField) findCombo.getEditor().getEditorComponent();
                findText.setToolTipText(
                    "Enter an expression - right click or ctrl-space for menu - press enter to add to list");
                findRule = ExpressionRule.getRule(ruleText);
                currentSearchMatchCount = tableModel.updateEventsWithFindRule(findRule);
                searchModel.updateEventsWithFindRule(findRule);
                currentColorizer.setFindRule(findRule);
                tableRuleMediator.setFindRule(findRule);
                searchRuleMediator.setFindRule(findRule);
                //valid expression, reset background color in case we were previously an invalid expression
                findText.setBackground(UIManager.getColor("TextField.background"));
                statusBar.setSearchMatchCount(currentSearchMatchCount, getIdentifier());
                if (isSearchResultsVisible()) {
                    showSearchResults();
                }
            } catch (IllegalArgumentException re) {
                findRule = null;
                final JTextField findText = (JTextField) findCombo.getEditor().getEditorComponent();
                findText.setToolTipText(re.getMessage());
                findText.setBackground(ChainsawConstants.INVALID_EXPRESSION_BACKGROUND);
                currentColorizer.setFindRule(null);
                tableRuleMediator.setFindRule(null);
                searchRuleMediator.setFindRule(null);
                tableModel.updateEventsWithFindRule(null);
                searchModel.updateEventsWithFindRule(null);
                currentSearchMatchCount = 0;
                statusBar.setSearchMatchCount(currentSearchMatchCount, getIdentifier());
                //if the preference to show search results is enabled, the find rule is now null - hide search results
                if (isSearchResultsVisible()) {
                    hideSearchResults();
                }
            }
        }
    }

    private void hideSearchResults() {
        if (searchResultsDisplayed) {
            detailPanel.removeAll();
            JPanel leftSpacePanel = new JPanel();
            Integer scrollBarWidth = (Integer) UIManager.get("ScrollBar.width");
            leftSpacePanel.setPreferredSize(new Dimension(scrollBarWidth - 4, -1));

            JPanel rightSpacePanel = new JPanel();
            rightSpacePanel.setPreferredSize(new Dimension(scrollBarWidth - 4, -1));

            detailPanel.add(detailToolbar, BorderLayout.NORTH);
            detailPanel.add(detailPane, BorderLayout.CENTER);

            detailPanel.add(leftSpacePanel, BorderLayout.WEST);
            detailPanel.add(rightSpacePanel, BorderLayout.EAST);

            detailPanel.revalidate();
            detailPanel.repaint();
            //if the detail visible pref is not enabled, hide the detail pane
            searchResultsDisplayed = false;
            //hide if pref is not enabled
            if (!isDetailVisible()) {
                hideDetailPane();
            }
        }
    }

    private void showSearchResults() {
        if (isSearchResultsVisible() && !searchResultsDisplayed && findRule != null) {
            //if pref is set, always update detail panel to contain search results
            detailPanel.removeAll();
            detailPanel.add(searchPane, BorderLayout.CENTER);
            Integer scrollBarWidth = (Integer) UIManager.get("ScrollBar.width");
            JPanel leftSpacePanel = new JPanel();
            leftSpacePanel.setPreferredSize(new Dimension(scrollBarWidth - 4, -1));
            JPanel rightSpacePanel = new JPanel();
            rightSpacePanel.setPreferredSize(new Dimension(scrollBarWidth - 4, -1));
            detailPanel.add(leftSpacePanel, BorderLayout.WEST);
            detailPanel.add(rightSpacePanel, BorderLayout.EAST);
            detailPanel.revalidate();
            detailPanel.repaint();
            //if the detail visible pref is not enabled, show the detail pane
            searchResultsDisplayed = true;
            //show if pref is not enabled
            if (!isDetailVisible()) {
                showDetailPane();
            }
        }
    }

    /**
     * Display the detail pane, using the last known divider location
     */
    private void showDetailPane() {
        lowerPanel.setDividerSize(dividerSize);
        lowerPanel.setDividerLocation(logPanelPreferenceModel.getLowerPanelDividerLocation());
        detailPanel.setVisible(true);
        detailPanel.repaint();
        lowerPanel.repaint();
        logPanelPreferenceModel.setLowerPanelDividerLocation(lowerPanel.getDividerLocation());
    }

    /**
     * Hide the detail pane, holding the current divider location for later use
     */
    private void hideDetailPane() {
        //may be called not currently visible on initial setup to ensure panel is not visible..only update divider location if hiding when currently visible
        lowerPanel.setDividerSize(0);
        detailPanel.setVisible(false);
        detailPanel.repaint();
        lowerPanel.repaint();
    }

    /**
     * Display the log tree pane, using the last known divider location
     */
    private void showLogTreePanel() {
        nameTreeAndMainPanelSplit.setDividerSize(dividerSize);
        nameTreeAndMainPanelSplit.setDividerLocation(logPanelPreferenceModel.getLogTreeDividerLocation());
        logTreePanel.setVisible(true);
        nameTreeAndMainPanelSplit.repaint();
        logPanelPreferenceModel.setLogTreeDividerLocation(nameTreeAndMainPanelSplit.getDividerLocation());
    }

    /**
     * Hide the log tree pane, holding the current divider location for later use
     */
    private void hideLogTreePanel() {
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

        dockPauseAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_P);
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

        logPanelPreferenceModel.addEventListener(
            evt -> {
            if (evt.getPropertyName().equals(LogPanelPreferenceModel.PAUSED)) {
                dockPauseButton.getModel().setSelected((boolean)evt.getPropertyValue());
            }
        });

        toolbar.add(dockPauseButton);

        SmallButton showDockButton =
            new SmallButton.Builder()
                .action(this::showPreferences)
                .shortDescription("Define preferences...")
                .smallIconUrl(ChainsawIcons.PREFERENCES)
                .build();

        toolbar.add(showDockButton);

        Action dockToggleLogTreeAction =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    toggleLogTreeVisible();
                }
            };

        dockToggleLogTreeAction.putValue(Action.SHORT_DESCRIPTION, "Toggles the Logger Tree Pane");
        dockToggleLogTreeAction.putValue("enabled", Boolean.TRUE);
        dockToggleLogTreeAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_T);
        dockToggleLogTreeAction.putValue(
            Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        dockToggleLogTreeAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.WINDOW_ICON));

        final SmallToggleButton toggleLogTreeButton =
            new SmallToggleButton(dockToggleLogTreeAction);
        logPanelPreferenceModel.addEventListener(
            evt -> {
            if (evt.getPropertyName().equals(LogPanelPreferenceModel.LOG_TREE_PANEL_VISIBLE)) {
                toggleLogTreeButton.setSelected((Boolean)evt.getPropertyValue());
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
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
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

        logPanelPreferenceModel.addEventListener(
            evt -> {
            if (evt.getPropertyName().equals(LogPanelPreferenceModel.SCROLL_TO_BOTTOM)) {
                toggleScrollToBottomButton.setSelected((Boolean)evt.getPropertyValue());
            }
        });

        toggleScrollToBottomButton.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
            dockToggleScrollToBottomAction.getValue(Action.NAME));
        toggleScrollToBottomButton.getActionMap().put(
            dockToggleScrollToBottomAction.getValue(Action.NAME), dockToggleScrollToBottomAction);

        toggleScrollToBottomButton.setSelected(isScrollToBottom());
        toggleScrollToBottomButton.setText("");
        toolbar.add(toggleScrollToBottomButton);
        toolbar.addSeparator();

        findCombo.addActionListener(e -> {
            //comboboxchanged event received when text is modified in the field..when enter is pressed, it's comboboxedited
            if (e.getActionCommand().equalsIgnoreCase("comboBoxEdited")) {
                findNext();
            }
        });
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
    public void updateStatusBar() {
        SwingHelper.invokeOnEDT(
            () -> {
                statusBar.setSelectedLine(
                    table.getSelectedRow() + 1, tableModel.getRowCount(),
                    tableModel.size(), getIdentifier());
                statusBar.setSearchMatchCount(currentSearchMatchCount, getIdentifier());
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

    private void setDetailPaneDatetimeFormat(DateTimeFormatter datetimeFormat) {
        DateTimeFormatter oldFormat = getDetailPaneDatetimeFormat();
        detailLayout.setDateformat(datetimeFormat);
        firePropertyChange(
            "detailPaneDatetimeFormat", oldFormat,
            getDetailPaneDatetimeFormat());
    }

    private DateTimeFormatter getDetailPaneDatetimeFormat() {
        return detailLayout.getDateformat();
    }

    /**
     * Reset the LoggingEvent container, detail panel and status bar
     */
    private void clearModel() {
        previousLastIndex = -1;
        tableModel.clearModel();
        searchModel.clearModel();

        synchronized (detail) {
            detailPaneUpdater.setSelectedRow(-1);
            detail.notify();
        }

        statusBar.setNothingSelected();
    }

    public void findNextColorizedEvent() {
        EventQueue.invokeLater(() -> {
            final int nextRow = tableModel.findColoredRow(table.getSelectedRow() + 1, true);
            if (nextRow > -1) {
                table.scrollToRow(nextRow);
            }
        });
    }

    public void findPreviousColorizedEvent() {
        EventQueue.invokeLater(() -> {
            final int previousRow = tableModel.findColoredRow(table.getSelectedRow() - 1, false);
            if (previousRow > -1) {
                table.scrollToRow(previousRow);
            }
        });
    }

    /**
     * Finds the next row matching the current find rule, and ensures it is made
     * visible
     */
    public void findNext() {
        Object item = findCombo.getSelectedItem();
        updateFindRule(item == null ? null : item.toString());

        if (findRule != null) {
            EventQueue.invokeLater(() -> {
                final JTextField findText = (JTextField) findCombo.getEditor().getEditorComponent();
                try {
                    int filteredEventsSize = getFilteredEvents().size();
                    int startRow = table.getSelectedRow() + 1;
                    if (startRow > filteredEventsSize - 1) {
                        startRow = 0;
                    }
                    //no selected row would return -1, so we'd start at row zero
                    final int nextRow = tableModel.locate(findRule, startRow, true);

                    if (nextRow > -1) {
                        table.scrollToRow(nextRow);
                        findText.setToolTipText("Enter an expression - right click or ctrl-space for menu - press enter to add to list");
                    }
                    findText.setBackground(UIManager.getColor("TextField.background"));
                } catch (IllegalArgumentException iae) {
                    findText.setToolTipText(iae.getMessage());
                    findText.setBackground(ChainsawConstants.INVALID_EXPRESSION_BACKGROUND);
                    currentColorizer.setFindRule(null);
                    tableRuleMediator.setFindRule(null);
                    searchRuleMediator.setFindRule(null);
                }
            });
        }
    }

    /**
     * Finds the previous row matching the current find rule, and ensures it is made
     * visible
     */
    public void findPrevious() {
        Object item = findCombo.getSelectedItem();
        updateFindRule(item == null ? null : item.toString());

        if (findRule != null) {
            EventQueue.invokeLater(() -> {
                final JTextField findText = (JTextField) findCombo.getEditor().getEditorComponent();
                try {
                    int startRow = table.getSelectedRow() - 1;
                    int filteredEventsSize = getFilteredEvents().size();
                    if (startRow < 0) {
                        startRow = filteredEventsSize - 1;
                    }
                    final int previousRow = tableModel.locate(findRule, startRow, false);

                    if (previousRow > -1) {
                        table.scrollToRow(previousRow);
                        findCombo.setToolTipText("Enter an expression - right click or ctrl-space for menu - press enter to add to list");
                    }
                    findText.setBackground(UIManager.getColor("TextField.background"));
                } catch (IllegalArgumentException iae) {
                    findText.setToolTipText(iae.getMessage());
                    findText.setBackground(ChainsawConstants.INVALID_EXPRESSION_BACKGROUND);
                }
            });
        }
    }

    /**
     * Docks this DockablePanel by hiding the JFrame and placing the Panel back
     * inside the LogUI window.
     */
    private void dock() {

        final int row = table.getSelectedRow();
        setDocked(true);
        undockedFrame.setVisible(false);
        removeAll();

        add(nameTreeAndMainPanelSplit, BorderLayout.CENTER);
        externalPanel.setDocked(true);
        dockingAction.putValue(Action.NAME, "Undock");
        dockingAction.putValue(Action.SMALL_ICON, ChainsawIcons.ICON_UNDOCK);
        if (row > -1) {
            EventQueue.invokeLater(() -> table.scrollToRow(row));
        }
    }

    /**
     * Load default column settings if no settings exist for this identifier
     *
     */
    private void loadColumnSettings() {
        TableColumnModel columnModel = table.getColumnModel();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        Map<String, TableColumn> columnNameMap = new HashMap<>();

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn thisColumn = columnModel.getColumn(i);
            if (thisColumn.getHeaderValue().toString().equalsIgnoreCase(ChainsawConstants.LOG4J_MARKER_COL_NAME)) {
                thisColumn.setCellEditor(markerCellEditor);
            }

            columnNameMap.put(table.getColumnName(i).toUpperCase(), thisColumn);
        }

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn thisColumn = columnModel.getColumn(i);
            columnNameMap.put(table.getColumnName(i).toUpperCase(), thisColumn);
        }

        while (table.getColumnCount() > 0) {
            table.removeColumn(columnModel.getColumn(0));
        }

        Map<String, Integer> columnNamesAndWidths = logPanelPreferenceModel.getAllColumnNamesAndWidths();
        for (String tableColumnName : logPanelPreferenceModel.getVisibleOrderedColumnNames()) {
            TableColumn column = columnNameMap.get(tableColumnName);
            if (column != null) {
                Integer preferredWidth = columnNamesAndWidths.get(tableColumnName);
                if (null != preferredWidth) {
                    column.setPreferredWidth(preferredWidth);
                }
                table.addColumn(column);
            }
        }

        undockedFrame.setSize(getSize());
        undockedFrame.setLocation(getBounds().x, getBounds().y);

        table.validate();
    }

    /**
     * Iterate over all values in the column and return the longest width
     *
     * @param index column index
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

    private String getToolTipTextForEvent(LoggingEventWrapper loggingEventWrapper) {
        return detailLayout.format(loggingEventWrapper.getLoggingEvent());
    }

    /**
     * ensures the Entry map of all the unque logger names etc, that is used for
     * the Filter panel is updated with any new information from the event
     *
     * @param event
     */
    private void updateOtherModels(ChainsawLoggingEvent event) {

        /*
         * EventContainer is a LoggerNameModel imp, use that for notifing
         */
        tableModel.addLoggerName(event.m_logger);

        filterModel.processNewLoggingEvent(event);
    }

    public void findNextMarker() {
        EventQueue.invokeLater(() -> {
            int startRow = table.getSelectedRow() + 1;
            int filteredEventsSize = getFilteredEvents().size();
            if (startRow > filteredEventsSize - 1) {
                startRow = 0;
            }
            final int nextRow = tableModel.locate(findMarkerRule, startRow, true);

            if (nextRow > -1) {
                table.scrollToRow(nextRow);
            }
        });
    }

    public void findPreviousMarker() {
        EventQueue.invokeLater(() -> {
            int startRow = table.getSelectedRow() - 1;
            int filteredEventsSize = getFilteredEvents().size();
            if (startRow < 0) {
                startRow = filteredEventsSize - 1;
            }
            final int previousRow = tableModel.locate(findMarkerRule, startRow, false);

            if (previousRow > -1) {
                table.scrollToRow(previousRow);
            }
        });
    }

    public void clearAllMarkers() {
        //this will get the properties to be removed from both tables..but
        tableModel.removePropertyFromEvents(ChainsawConstants.LOG4J_MARKER_COL_NAME);
    }

    public void toggleMarker() {
        int row = table.getSelectedRow();
        if (row != -1) {
            LoggingEventWrapper loggingEventWrapper = tableModel.getRow(row);
            if (loggingEventWrapper != null) {
                Object marker = loggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME);
                if (marker == null) {
                    loggingEventWrapper.setProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME, "set");
                } else {
                    loggingEventWrapper.removeProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME);
                }
                //if marker -was- null, it no longer is (may need to add the column)
                tableModel.fireRowUpdated(row, (marker == null));
            }
        }
    }

    public void layoutComponents() {
        if (logPanelPreferenceModel.isDetailPaneVisible()) {
            showDetailPane();
        } else {
            hideDetailPane();
        }
    }

    public void setFindText(String findText) {
        findCombo.setSelectedItem(findText);
        findNext();
    }

    public String getFindText() {
        Object selectedItem = findCombo.getSelectedItem();
        if (selectedItem == null) {
            return "";
        }
        return selectedItem.toString();
    }

    public void setReceiver(ChainsawReceiver rx) {
        receiver = rx;
        receiver.addPropertyChangeListener(((pce) -> {
            if (pce.getPropertyName().equals("name")) {
//                this.identifier = pce.getNewValue();
            }
        }));
        receiver.addChainsawEventBatchListener(this);
    }

    public void receiveChainsawEventBatch(List<ChainsawLoggingEvent> events) {
        SwingHelper.invokeOnEDT(() -> {
            /*
             * if this panel is paused, we totally ignore events
             */
            if (isPaused()) {
                return;
            }
            final int selectedRow = table.getSelectedRow();
            final int startingRow = table.getRowCount();
            final LoggingEventWrapper selectedEvent;
            if (selectedRow >= 0) {
                selectedEvent = tableModel.getRow(selectedRow);
            } else {
                selectedEvent = null;
            }

            final int startingSearchRow = searchTable.getRowCount();

            boolean rowAdded = false;
            boolean searchRowAdded = false;

            int addedRowCount = 0;
            int searchAddedRowCount = 0;

            for (ChainsawLoggingEvent event1 : events) {
                //create two separate loggingEventWrappers (main table and search table), as they have different info on display state
                LoggingEventWrapper loggingEventWrapper1 = new LoggingEventWrapper(event1);
                //if the clearTableExpressionRule is not null, evaluate & clear the table if it matches
                if (clearTableExpressionRule != null && clearTableExpressionRule.evaluate(event1, null)) {
                    logger.info("clear table expression matched - clearing table - matching event msg - " + event1.m_message);
                    clearEvents();
                }

                updateOtherModels(event1);
                boolean isCurrentRowAdded = tableModel.isAddRow(loggingEventWrapper1);
                if (isCurrentRowAdded) {
                    addedRowCount++;
                }
                rowAdded = rowAdded || isCurrentRowAdded;

                //create a new loggingEventWrapper via copy constructor to ensure same IDs
                LoggingEventWrapper loggingEventWrapper2 = new LoggingEventWrapper(loggingEventWrapper1);
                boolean isSearchCurrentRowAdded = searchModel.isAddRow(loggingEventWrapper2);
                if (isSearchCurrentRowAdded) {
                    searchAddedRowCount++;
                }
                searchRowAdded = searchRowAdded || isSearchCurrentRowAdded;
            }
            //fire after adding all events
            if (rowAdded) {
                tableModel.fireTableEvent(startingRow, startingRow + addedRowCount, addedRowCount);
            }
            if (searchRowAdded) {
                searchModel.fireTableEvent(startingSearchRow, startingSearchRow + searchAddedRowCount, searchAddedRowCount);
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

            if (searchRowAdded) {
                if (searchModel.isSortEnabled()) {
                    searchModel.sort();
                }
            }

            if (!isScrollToBottom() && selectedEvent != null) {
                final int newIndex = tableModel.getRowIndex(selectedEvent);
                if (newIndex >= 0) {
                    // Don't scroll, just maintain selection...
                    table.setRowSelectionInterval(newIndex, newIndex);
                }
            }
        });
    }

    /**
     * This class receives notification when the Refine focus or find field is
     * updated, where a background thread periodically wakes up and checks if
     * they have stopped typing yet. This ensures that the filtering of the
     * model is not done for every single character typed.
     *
     * @author Paul Smith psmith
     */
    private final class DelayedTextDocumentListener
        implements DocumentListener {
        private static final long CHECK_PERIOD = 1000;
        private final JTextField textField;
        private long lastTimeStamp = System.currentTimeMillis();
        private final Thread delayThread;
        private final String defaultToolTip;
        private String lastText = "";

        private DelayedTextDocumentListener(final JTextField textFeld) {
            super();
            this.textField = textFeld;
            this.defaultToolTip = textFeld.getToolTipText();

            this.delayThread =
                new Thread(
                    () -> {
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
                                if (!(textFeld.getText().trim().equals(lastText.trim()))) {
                                    lastText = textFeld.getText();
                                    EventQueue.invokeLater(DelayedTextDocumentListener.this::setFilter);
                                }
                            } else {
                                // they stopped typing a while ago, let's forget about it
                                //                logger.debug(
                                //                  "They stoppped typing a while ago, assuming filter has been applied");
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
            if (textField.getText().trim().isEmpty()) {
                //reset background color in case we were previously an invalid expression
                textField.setBackground(UIManager.getColor("TextField.background"));
                tableRuleMediator.setFilterRule(null);
                searchRuleMediator.setFilterRule(null);
                textField.setToolTipText(defaultToolTip);
                if (findRule != null) {
                    currentSearchMatchCount = tableModel.getSearchMatchCount();
                    statusBar.setSearchMatchCount(currentSearchMatchCount, getIdentifier());
                }
            } else {
                try {
                    tableRuleMediator.setFilterRule(ExpressionRule.getRule(textField.getText()));
                    searchRuleMediator.setFilterRule(ExpressionRule.getRule(textField.getText()));
                    textField.setToolTipText(defaultToolTip);
                    if (findRule != null) {
                        currentSearchMatchCount = tableModel.getSearchMatchCount();
                        statusBar.setSearchMatchCount(currentSearchMatchCount, getIdentifier());
                    }
                    //valid expression, reset background color in case we were previously an invalid expression
                    textField.setBackground(UIManager.getColor("TextField.background"));
                } catch (IllegalArgumentException iae) {
                    //invalid expression, change background of the field
                    textField.setToolTipText(iae.getMessage());
                    textField.setBackground(ChainsawConstants.INVALID_EXPRESSION_BACKGROUND);
                    if (findRule != null) {
                        currentSearchMatchCount = tableModel.getSearchMatchCount();
                        statusBar.setSearchMatchCount(currentSearchMatchCount, getIdentifier());
                    }
                }
            }
        }
    }

    private final class TableMarkerListener extends MouseAdapter {
        private JTable markerTable;
        private EventContainer markerEventContainer;
        private EventContainer otherMarkerEventContainer;

        private TableMarkerListener(JTable markerTable, EventContainer markerEventContainer, EventContainer otherMarkerEventContainer) {
            this.markerTable = markerTable;
            this.markerEventContainer = markerEventContainer;
            this.otherMarkerEventContainer = otherMarkerEventContainer;
        }

        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                int row = markerTable.rowAtPoint(evt.getPoint());
                if (row != -1) {
                    LoggingEventWrapper loggingEventWrapper = markerEventContainer.getRow(row);
                    if (loggingEventWrapper != null) {
                        Object marker = loggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME);
                        if (marker == null) {
                            loggingEventWrapper.setProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME, "set");
                        } else {
                            loggingEventWrapper.removeProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME);
                        }
                        //if marker -was- null, it no longer is (may need to add the column)
                        markerEventContainer.fireRowUpdated(row, (marker == null));
                        otherMarkerEventContainer.fireRowUpdated(otherMarkerEventContainer.getRowIndex(loggingEventWrapper), (marker == null));
                    }
                }
            }
        }
    }

    /**
     * Update active tooltip
     */
    private final class TableColumnDetailMouseListener extends MouseMotionAdapter {
        private int currentRow = -1;
        private JTable detailTable;
        private EventContainer detailEventContainer;

        private TableColumnDetailMouseListener(JTable detailTable, EventContainer detailEventContainer) {
            this.detailTable = detailTable;
            this.detailEventContainer = detailEventContainer;
        }

        /**
         * Update tooltip based on mouse position
         *
         * @param evt
         */
        public void mouseMoved(MouseEvent evt) {
            currentPoint = evt.getPoint();
            currentTable = detailTable;

            if (logPanelPreferenceModel.isToolTipsVisible()) {
                int row = detailTable.rowAtPoint(evt.getPoint());

                if ((row == currentRow) || (row == -1)) {
                    return;
                }

                currentRow = row;

                LoggingEventWrapper event = detailEventContainer.getRow(currentRow);

                if (event != null) {
                    String toolTipText = getToolTipTextForEvent(event);
                    detailTable.setToolTipText(toolTipText);
                }
            } else {
                detailTable.setToolTipText(null);
            }
        }
    }

    //if columnmoved or columnremoved callback received, re-apply table's sort index based
    //sort column name
    private class ChainsawTableColumnModelListener implements TableColumnModelListener {
        private JSortTable modelListenerTable;

        private ChainsawTableColumnModelListener(JSortTable modelListenerTable) {
            this.modelListenerTable = modelListenerTable;
        }

        public void columnAdded(TableColumnModelEvent e) {
            //no-op
        }

        /**
         * Update sorted column
         *
         * @param e
         */
        public void columnRemoved(TableColumnModelEvent e) {
            modelListenerTable.updateSortedColumn();
        }

        /**
         * Update sorted column
         *
         * @param e
         */
        public void columnMoved(TableColumnModelEvent e) {
            modelListenerTable.updateSortedColumn();
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

        private void setAndUpdateSelectedRow(int row) {
            selectedRow = row;
            updateDetailPane(true);
        }

        private void updateDetailPane() {
            updateDetailPane(false);
        }

        /**
         * Update detail pane
         */
        private void updateDetailPane(boolean force) {
            /*
             * Don't bother doing anything if it's not visible. Note: the isVisible() method on
             * Component is not really accurate here because when the button to toggle display of
             * the detail pane is triggered it still appears as 'visible' for some reason.
             */
            if (!logPanelPreferenceModel.isDetailPaneVisible()) {
                return;
            }

            LoggingEventWrapper loggingEventWrapper = null;
            if (force || (selectedRow != -1 && (lastRow != selectedRow))) {
                loggingEventWrapper = tableModel.getRow(selectedRow);

                if (loggingEventWrapper != null) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(detailLayout.format(loggingEventWrapper.getLoggingEvent()));
                    if (buf.length() > 0) {
                        try {
                            final Document doc = detail.getEditorKit().createDefaultDocument();
                            detail.getEditorKit().read(new StringReader(buf.toString()), doc, 0);

                            SwingHelper.invokeOnEDT(() -> {
                                detail.setDocument(doc);
                                JTextComponentFormatter.applySystemFontAndSize(detail);
                                detail.setCaretPosition(0);
                                lastRow = selectedRow;
                            });
                        } catch (Exception e) {
                        }
                    }
                }
            }

            if (loggingEventWrapper == null && (lastRow != selectedRow)) {
                try {
                    final Document doc = detail.getEditorKit().createDefaultDocument();
                    detail.getEditorKit().read(new StringReader("<html>Nothing selected</html>"), doc, 0);
                    SwingHelper.invokeOnEDT(() -> {
                        detail.setDocument(doc);
                        JTextComponentFormatter.applySystemFontAndSize(detail);
                        detail.setCaretPosition(0);
                        lastRow = selectedRow;
                    });
                } catch (Exception e) {
                }
            }
        }

        /**
         * Update detail pane layout if it's changed
         *
         * @param arg0
         */
        public void propertyChange(PropertyChangeEvent arg0) {
            SwingUtilities.invokeLater(
                () -> updateDetailPane(true));
        }
    }

    private class ThrowableDisplayMouseAdapter extends MouseAdapter {
        private JTable throwableTable;
        private EventContainer throwableEventContainer;
        final JDialog detailDialog;
        final JEditorPane detailArea;

        public ThrowableDisplayMouseAdapter(JTable throwableTable, EventContainer throwableEventContainer) {
            this.throwableTable = throwableTable;
            this.throwableEventContainer = throwableEventContainer;

            detailDialog = new JDialog((JFrame) null, true);
            Container container = detailDialog.getContentPane();
            detailArea = new JEditorPane();
            JTextComponentFormatter.applySystemFontAndSize(detailArea);
            detailArea.setEditable(false);
            Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
            detailArea.setPreferredSize(new Dimension(screenDimension.width / 2, screenDimension.height / 2));
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.add(new JScrollPane(detailArea));

            detailDialog.pack();
        }

        public void mouseClicked(MouseEvent e) {
/*
            TableColumn column = throwableTable.getColumnModel().getColumn(throwableTable.columnAtPoint(e.getPoint()));
            if (!column.getHeaderValue().toString().toUpperCase().equals(ChainsawColumns.getColumnName(ChainsawColumns.INDEX_THROWABLE_COL_NAME))) {
                return;
            }

            LoggingEventWrapper loggingEventWrapper = throwableEventContainer.getRow(throwableTable.getSelectedRow());
*/

            //throwable string representation may be a length-one empty array
//            String[] ti = loggingEventWrapper.getLoggingEvent().getThrowableStrRep();
//            if (ti != null && ti.length > 0 && (!(ti.length == 1 && ti[0].isEmpty()))) {
//                detailDialog.setTitle(throwableTable.getColumnName(throwableTable.getSelectedColumn()) + " detail...");
//                StringBuilder buf = new StringBuilder();
//                buf.append(loggingEventWrapper.getLoggingEvent().getMessage());
//                buf.append("\n");
//                for (String aTi : ti) {
//                    buf.append(aTi).append("\n    ");
//                }
//
//                detailArea.setText(buf.toString());
//                SwingHelper.invokeOnEDT(() -> centerAndSetVisible(detailDialog));
//            }
        }
    }

    private class MarkerCellEditor implements TableCellEditor {
        JTable currentTableMarkerCell;
        JTextField textField = new JTextField();
        Set<CellEditorListener> cellEditorListeners = new HashSet<>();
        private LoggingEventWrapper currentLoggingEventWrapper;
        private final Object mutex = new Object();
        int currentRowHeight = 0;

        public Object getCellEditorValue() {
            return textField.getText();
        }

        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            textField.selectAll();
            return true;
        }

        public boolean stopCellEditing() {
            if (textField.getText().trim().isEmpty()) {
                currentLoggingEventWrapper.removeProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME);
            } else {
                currentLoggingEventWrapper.setProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME, textField.getText());
            }
            //row should always exist in the main table if it is being edited
            tableModel.fireRowUpdated(tableModel.getRowIndex(currentLoggingEventWrapper), true);
            int index = searchModel.getRowIndex(currentLoggingEventWrapper);
            if (index > -1) {
                searchModel.fireRowUpdated(index, true);
            }

            ChangeEvent event = new ChangeEvent(currentTableMarkerCell);
            Set<CellEditorListener> cellEditorListenersCopy;
            synchronized (mutex) {
                cellEditorListenersCopy = new HashSet<>(cellEditorListeners);
            }

            for (Object aCellEditorListenersCopy : cellEditorListenersCopy) {
                ((CellEditorListener) aCellEditorListenersCopy).editingStopped(event);
            }
            currentTableMarkerCell.setRowHeight(currentRowHeight);
            currentLoggingEventWrapper = null;
            currentTableMarkerCell = null;

            return true;
        }

        public void cancelCellEditing() {
            Set<CellEditorListener> cellEditorListenersCopy;
            synchronized (mutex) {
                cellEditorListenersCopy = new HashSet<>(cellEditorListeners);
            }

            ChangeEvent event = new ChangeEvent(currentTableMarkerCell);
            for (Object aCellEditorListenersCopy : cellEditorListenersCopy) {
                ((CellEditorListener) aCellEditorListenersCopy).editingCanceled(event);
            }
            currentTableMarkerCell.setRowHeight(currentRowHeight);
            currentLoggingEventWrapper = null;
            currentTableMarkerCell = null;
        }

        public void addCellEditorListener(CellEditorListener l) {
            synchronized (mutex) {
                cellEditorListeners.add(l);
            }
        }

        public void removeCellEditorListener(CellEditorListener l) {
            synchronized (mutex) {
                cellEditorListeners.remove(l);
            }
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentTableMarkerCell = table;
            currentLoggingEventWrapper = ((EventContainer) table.getModel()).getRow(row);
            if (currentLoggingEventWrapper != null) {
                textField.setText(currentLoggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME));
                textField.selectAll();
            } else {
                textField.setText("");
            }
            currentRowHeight = table.getRowHeight(row);
            table.setRowHeight(row, textField.getPreferredSize().height);
            return textField;
        }
    }

    private class EventTimeDeltaMatchThumbnail extends AbstractEventMatchThumbnail {
        public EventTimeDeltaMatchThumbnail() {
            super();
            initializeLists();
        }

        boolean primaryMatches(ThumbnailLoggingEventWrapper wrapper) {
            String millisDelta = wrapper.loggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE);
            if (millisDelta != null && !millisDelta.trim().isEmpty()) {
                long millisDeltaLong = Long.parseLong(millisDelta);
                //arbitrary
                return millisDeltaLong >= 1000;
            }
            return false;
        }

        boolean secondaryMatches(ThumbnailLoggingEventWrapper wrapper) {
            //secondary is not used
            return false;
        }

        private void initializeLists() {
            secondaryList.clear();
            primaryList.clear();

            int i = 0;
            for (Object o : tableModel.getFilteredEvents()) {
                LoggingEventWrapper loggingEventWrapper = (LoggingEventWrapper) o;
                ThumbnailLoggingEventWrapper wrapper = new ThumbnailLoggingEventWrapper(i, loggingEventWrapper);
                i++;
                //only add if there is a color defined
                if (primaryMatches(wrapper)) {
                    primaryList.add(wrapper);
                }
            }
            revalidate();
            repaint();
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int rowCount = table.getRowCount();
            if (rowCount == 0) {
                return;
            }
            //use event pane height as reference height - max component height will be extended by event height if
            // last row is rendered, so subtract here
            int height = eventsPane.getHeight();
            int maxHeight = Math.min(maxEventHeight, (height / rowCount));
            int minHeight = Math.max(1, maxHeight);
            int componentHeight = height - minHeight;
            int eventHeight = minHeight;

            //draw all events
            for (Object aPrimaryList : primaryList) {
                ThumbnailLoggingEventWrapper wrapper = (ThumbnailLoggingEventWrapper) aPrimaryList;
                if (primaryMatches(wrapper)) {
                    float ratio = (wrapper.rowNum / (float) rowCount);
                    //                System.out.println("error - ratio: " + ratio + ", component height: " + componentHeight);
                    int verticalLocation = (int) (componentHeight * ratio);

                    int startX = 1;
                    int width = getWidth() - (startX * 2);
                    //max out at 50, min 2...
                    String millisDelta = wrapper.loggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE);
                    long millisDeltaLong = Long.parseLong(millisDelta);
                    long delta = Math.min(ChainsawConstants.MILLIS_DELTA_RENDERING_HEIGHT_MAX, Math.max(0, (long) (millisDeltaLong * ChainsawConstants.MILLIS_DELTA_RENDERING_FACTOR)));
                    float widthMaxMillisDeltaRenderRatio = ((float) width / ChainsawConstants.MILLIS_DELTA_RENDERING_HEIGHT_MAX);
                    int widthToUse = Math.max(2, (int) (delta * widthMaxMillisDeltaRenderRatio));
                    eventHeight = Math.min(maxEventHeight, eventHeight + 3);
//                            eventHeight = maxEventHeight;
//                    drawEvent(applicationPreferenceModel.getDeltaColor(), (verticalLocation - eventHeight + 1), eventHeight, g, startX, widthToUse);
                    //                System.out.println("painting error - rownum: " + wrapper.rowNum + ", location: " + verticalLocation + ", height: " + eventHeight + ", component height: " + componentHeight + ", row count: " + rowCount);
                }
            }
        }
    }

    //a listener receiving color updates needs to call configureColors on this class
    private class ColorizedEventAndSearchMatchThumbnail extends AbstractEventMatchThumbnail {
        public ColorizedEventAndSearchMatchThumbnail() {
            super();
            configureColors();
        }

        boolean primaryMatches(ThumbnailLoggingEventWrapper wrapper) {
            return !wrapper.loggingEventWrapper.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND);
        }

        boolean secondaryMatches(ThumbnailLoggingEventWrapper wrapper) {
            return wrapper.loggingEventWrapper.isSearchMatch();
        }

        private void configureColors() {
            secondaryList.clear();
            primaryList.clear();

            int i = 0;
            for (Object o : tableModel.getFilteredEvents()) {
                LoggingEventWrapper loggingEventWrapper = (LoggingEventWrapper) o;
                ThumbnailLoggingEventWrapper wrapper = new ThumbnailLoggingEventWrapper(i, loggingEventWrapper);
                if (secondaryMatches(wrapper)) {
                    secondaryList.add(wrapper);
                }
                i++;
                //only add if there is a color defined
                if (primaryMatches(wrapper)) {
                    primaryList.add(wrapper);
                }
            }
            revalidate();
            repaint();
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int rowCount = table.getRowCount();
            if (rowCount == 0) {
                return;
            }
            //use event pane height as reference height - max component height will be extended by event height if
            // last row is rendered, so subtract here
            int height = eventsPane.getHeight();
            int maxHeight = Math.min(maxEventHeight, (height / rowCount));
            int minHeight = Math.max(1, maxHeight);
            int componentHeight = height - minHeight;
            int eventHeight = minHeight;

            //draw all non error/warning/marker events
            for (Object aPrimaryList1 : primaryList) {
                ThumbnailLoggingEventWrapper wrapper = (ThumbnailLoggingEventWrapper) aPrimaryList1;
                if (!wrapper.loggingEventWrapper.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)) {
                    if (wrapper.loggingEventWrapper.getLoggingEvent().m_level.ordinal() < Level.WARN.ordinal() && wrapper.loggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME) == null) {
                        float ratio = (wrapper.rowNum / (float) rowCount);
                        //                System.out.println("error - ratio: " + ratio + ", component height: " + componentHeight);
                        int verticalLocation = (int) (componentHeight * ratio);

                        int startX = 1;
                        int width = getWidth() - (startX * 2);

                        drawEvent(wrapper.loggingEventWrapper.getColorRuleBackground(), verticalLocation, eventHeight, g, startX, width);
                        //                System.out.println("painting error - rownum: " + wrapper.rowNum + ", location: " + verticalLocation + ", height: " + eventHeight + ", component height: " + componentHeight + ", row count: " + rowCount);
                    }
                }
            }

            //draw warnings, error, fatal & markers last (full width)
            for (Object aPrimaryList : primaryList) {
                ThumbnailLoggingEventWrapper wrapper = (ThumbnailLoggingEventWrapper) aPrimaryList;
                if (!wrapper.loggingEventWrapper.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)) {
                    if (wrapper.loggingEventWrapper.getLoggingEvent().m_level.ordinal() >= Level.WARN.ordinal() || wrapper.loggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME) != null) {
                        float ratio = (wrapper.rowNum / (float) rowCount);
                        //                System.out.println("error - ratio: " + ratio + ", component height: " + componentHeight);
                        int verticalLocation = (int) (componentHeight * ratio);

                        int startX = 1;
                        int width = getWidth() - (startX * 2);
                        //narrow the color a bit if level is less than warn
                        //make warnings, errors a little taller

                        eventHeight = Math.min(maxEventHeight, eventHeight + 3);
//                            eventHeight = maxEventHeight;

                        drawEvent(wrapper.loggingEventWrapper.getColorRuleBackground(), (verticalLocation - eventHeight + 1), eventHeight, g, startX, width);
                        //                System.out.println("painting error - rownum: " + wrapper.rowNum + ", location: " + verticalLocation + ", height: " + eventHeight + ", component height: " + componentHeight + ", row count: " + rowCount);
                    }
                }
            }

            for (Object aSecondaryList : secondaryList) {
                ThumbnailLoggingEventWrapper wrapper = (ThumbnailLoggingEventWrapper) aSecondaryList;
                float ratio = (wrapper.rowNum / (float) rowCount);
//                System.out.println("warning - ratio: " + ratio + ", component height: " + componentHeight);
                int verticalLocation = (int) (componentHeight * ratio);

                int startX = 1;
                int width = getWidth() - (startX * 2);
                width = (width / 2);

                //use black for search indicator in the 'gutter'
                drawEvent(Color.BLACK, verticalLocation, eventHeight, g, startX, width);
//                System.out.println("painting warning - rownum: " + wrapper.rowNum + ", location: " + verticalLocation + ", height: " + eventHeight + ", component height: " + componentHeight + ", row count: " + rowCount);
            }
        }
    }

    abstract class AbstractEventMatchThumbnail extends JPanel {
        protected List<ThumbnailLoggingEventWrapper> primaryList = new ArrayList<>();
        protected List<ThumbnailLoggingEventWrapper> secondaryList = new ArrayList<>();
        protected final int maxEventHeight = 6;

        AbstractEventMatchThumbnail() {
            super();
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    if (logPanelPreferenceModel.isThumbnailBarToolTipsVisible()) {
                        int yPosition = e.getPoint().y;
                        ThumbnailLoggingEventWrapper event = getEventWrapperAtPosition(yPosition);
                        if (event != null) {
                            setToolTipText(getToolTipTextForEvent(event.loggingEventWrapper));
                        }
                    } else {
                        setToolTipText(null);
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int yPosition = e.getPoint().y;
                    ThumbnailLoggingEventWrapper event = getEventWrapperAtPosition(yPosition);
//                    System.out.println("rowToSelect: " + rowToSelect + ", closestRow: " + event.loggingEvent.getProperty("log4jid"));
                    if (event != null) {
                        int id = Integer.parseInt(event.loggingEventWrapper.getLoggingEvent().getProperty("log4jid"));
                        setSelectedEvent(id);
                    }
                }
            });

            tableModel.addTableModelListener(e -> {
                int firstRow = e.getFirstRow();
                //lastRow may be Integer.MAX_VALUE..if so, set lastRow to rowcount - 1 (so rowcount may be negative here, which will bypass for loops below)
                int lastRow = Math.min(e.getLastRow(), table.getRowCount() - 1);
                //clear everything if we got an event w/-1 for first or last row
                if (firstRow < 0 || lastRow < 0) {
                    primaryList.clear();
                    secondaryList.clear();
                }

//                    System.out.println("lastRow: " + lastRow + ", first row: " + firstRow + ", original last row: " + e.getLastRow() + ", type: " + e.getType());

                List displayedEvents = tableModel.getFilteredEvents();
                if (e.getType() == TableModelEvent.INSERT) {
//                        System.out.println("insert - current warnings: " + warnings.size() + ", errors: " + errors.size() + ", first row: " + firstRow + ", last row: " + lastRow);
                    for (int i = firstRow; i < lastRow; i++) {
                        LoggingEventWrapper event = (LoggingEventWrapper) displayedEvents.get(i);
                        ThumbnailLoggingEventWrapper wrapper = new ThumbnailLoggingEventWrapper(i, event);
                        if (secondaryMatches(wrapper)) {
                            secondaryList.add(wrapper);
//                                System.out.println("added warning: " + i + " - " + event.getLevel());
                        }
                        if (primaryMatches(wrapper)) {
                            //add to this one
                            primaryList.add(wrapper);
                        }
//                                System.out.println("added error: " + i + " - " + event.getLevel());
                    }
//                        System.out.println("insert- new warnings: " + warnings + ", errors: " + errors);

                    //run evaluation on rows & add to list
                } else if (e.getType() == TableModelEvent.DELETE) {
                    //find each eventwrapper with an id in the deleted range and remove it...
//                        System.out.println("delete- current warnings: " + warnings.size() + ", errors: " + errors.size() + ", first row: " + firstRow + ", last row: " + lastRow + ", displayed event count: " + displayedEvents.size() );
                    for (Iterator<ThumbnailLoggingEventWrapper> iter = secondaryList.iterator(); iter.hasNext(); ) {
                        ThumbnailLoggingEventWrapper wrapper = iter.next();
                        if ((wrapper.rowNum >= firstRow) && (wrapper.rowNum <= lastRow)) {
//                                System.out.println("deleting find: " + wrapper);
                            iter.remove();
                        }
                    }
                    for (Iterator<ThumbnailLoggingEventWrapper> iter = primaryList.iterator(); iter.hasNext(); ) {
                        ThumbnailLoggingEventWrapper wrapper = iter.next();
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
                    for (Iterator<ThumbnailLoggingEventWrapper> iter = secondaryList.iterator(); iter.hasNext(); ) {
                        ThumbnailLoggingEventWrapper wrapper = iter.next();
                        if ((wrapper.rowNum >= firstRow) && (wrapper.rowNum <= lastRow)) {
//                                System.out.println("update - deleting warning: " + wrapper);
                            iter.remove();
                        }
                    }
                    for (Iterator<ThumbnailLoggingEventWrapper> iter = primaryList.iterator(); iter.hasNext(); ) {
                        ThumbnailLoggingEventWrapper wrapper = iter.next();
                        if ((wrapper.rowNum >= firstRow) && (wrapper.rowNum <= lastRow)) {
//                                System.out.println("update - deleting error: " + wrapper);
                            iter.remove();
                        }
                    }
//                        System.out.println("update - after deleting old warnings in range: " + firstRow + " to " + lastRow + ", new warnings: " + warnings.size() + ", errors: " + errors.size());
                    //NOTE: for update, we need to do i<= lastRow
                    for (int i = firstRow; i <= lastRow; i++) {
                        LoggingEventWrapper event = (LoggingEventWrapper) displayedEvents.get(i);
                        ThumbnailLoggingEventWrapper wrapper = new ThumbnailLoggingEventWrapper(i, event);
//                                System.out.println("update - adding error: " + i + ", event: " + event.getMessage());
                        //only add event to thumbnail if there is a color
                        if (primaryMatches(wrapper)) {
                            //!wrapper.loggingEvent.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)
                            primaryList.add(wrapper);
                        } else {
                            primaryList.remove(wrapper);
                        }

                        if (secondaryMatches(wrapper)) {
                            //event.isSearchMatch())
//                                System.out.println("update - adding marker: " + i + ", event: " + event.getMessage());
                            secondaryList.add(wrapper);
                        } else {
                            secondaryList.remove(wrapper);
                        }
                    }
//                        System.out.println("update - new warnings: " + warnings.size() + ", errors: " + errors.size());
                }
                revalidate();
                repaint();
                //run this in an invokeLater block to ensure this action is enqueued to the end of the EDT
                EventQueue.invokeLater(() -> {
                    if (isScrollToBottom()) {
                        scrollToBottom();
                    }
                });
            });
        }

        abstract boolean primaryMatches(ThumbnailLoggingEventWrapper wrapper);

        abstract boolean secondaryMatches(ThumbnailLoggingEventWrapper wrapper);

        /**
         * Get event wrapper - may be null
         *
         * @param yPosition
         * @return event wrapper or null
         */
        protected ThumbnailLoggingEventWrapper getEventWrapperAtPosition(int yPosition) {
            int rowCount = table.getRowCount();

            //'effective' height of this component is scrollpane height
            int height = eventsPane.getHeight();

            yPosition = Math.max(yPosition, 0);

            //don't let clicklocation exceed height
            if (yPosition >= height) {
                yPosition = height;
            }

            //                    System.out.println("clicked y pos: " + e.getPoint().y + ", relative: " + clickLocation);
            float ratio = (float) yPosition / height;
            int rowToSelect = Math.round(rowCount * ratio);
            //                    System.out.println("rowCount: " + rowCount + ", height: " + height + ", clickLocation: " + clickLocation + ", ratio: " + ratio + ", rowToSelect: " + rowToSelect);
            ThumbnailLoggingEventWrapper event = getClosestRow(rowToSelect);
            return event;
        }

        private ThumbnailLoggingEventWrapper getClosestRow(int rowToSelect) {
            ThumbnailLoggingEventWrapper closestRow = null;
            int rowDelta = Integer.MAX_VALUE;
            for (Object aSecondaryList : secondaryList) {
                ThumbnailLoggingEventWrapper event = (ThumbnailLoggingEventWrapper) aSecondaryList;
                int newRowDelta = Math.abs(rowToSelect - event.rowNum);
                if (newRowDelta < rowDelta) {
                    closestRow = event;
                    rowDelta = newRowDelta;
                }
            }
            for (Object aPrimaryList : primaryList) {
                ThumbnailLoggingEventWrapper event = (ThumbnailLoggingEventWrapper) aPrimaryList;
                int newRowDelta = Math.abs(rowToSelect - event.rowNum);
                if (newRowDelta < rowDelta) {
                    closestRow = event;
                    rowDelta = newRowDelta;
                }
            }
            return closestRow;
        }

        public Point getToolTipLocation(MouseEvent event) {
            //shift tooltip down so the the pointer doesn't cover up events below the current mouse location
            return new Point(event.getX(), event.getY() + 30);
        }

        protected void drawEvent(Color newColor, int verticalLocation, int eventHeight, Graphics g, int x, int width) {
            //            System.out.println("painting: - color: " + newColor + ", verticalLocation: " + verticalLocation + ", eventHeight: " + eventHeight);
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
    }

    class ThumbnailLoggingEventWrapper {
        int rowNum;
        LoggingEventWrapper loggingEventWrapper;

        public ThumbnailLoggingEventWrapper(int rowNum, LoggingEventWrapper loggingEventWrapper) {
            this.rowNum = rowNum;
            this.loggingEventWrapper = loggingEventWrapper;
        }

        public String toString() {
            return "event - rownum: " + rowNum + ", level: " + loggingEventWrapper.getLoggingEvent().m_level;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ThumbnailLoggingEventWrapper that = (ThumbnailLoggingEventWrapper) o;

            return loggingEventWrapper != null ? loggingEventWrapper.equals(that.loggingEventWrapper) : that.loggingEventWrapper == null;
        }

        public int hashCode() {
            return loggingEventWrapper != null ? loggingEventWrapper.hashCode() : 0;
        }
    }


    class ToggleToolTips extends JCheckBoxMenuItem {
        public ToggleToolTips() {
            super("Show ToolTips", new ImageIcon(ChainsawIcons.TOOL_TIP));
            addActionListener(
                evt -> logPanelPreferenceModel.setToolTipsVisible(isSelected()));
        }
    }
}
