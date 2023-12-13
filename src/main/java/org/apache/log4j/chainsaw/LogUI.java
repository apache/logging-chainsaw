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
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.components.elements.SmallButton;
import org.apache.log4j.chainsaw.components.elements.TabIconHandler;
import org.apache.log4j.chainsaw.components.logpanel.LogPanel;
import org.apache.log4j.chainsaw.components.tabbedpane.ChainsawTabbedPane;
import org.apache.log4j.chainsaw.components.tutorial.TutorialFrame;
import org.apache.log4j.chainsaw.components.welcome.WelcomePanel;
import org.apache.log4j.chainsaw.dnd.FileDnDTarget;
import org.apache.log4j.chainsaw.help.HelpManager;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LineIconFactory;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverFactory;
import org.apache.log4j.chainsaw.receivers.ReceiversPanel;
import org.apache.log4j.chainsaw.zeroconf.ZeroConfPlugin;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;


/**
 * The main entry point for Chainsaw, this class represents the first frame
 * that is used to display a Welcome panel, and any other panels that are
 * generated because Logging Events are streamed via a Receiver, or other
 * mechanism.
 * <p>
 * NOTE: Some of Chainsaw's application initialization should be performed prior
 * to activating receivers and the logging framework used to perform self-logging.
 * <p>
 * DELAY as much as possible the logging framework initialization process,
 * currently initialized by the creation of a ChainsawAppenderHandler.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 * @author Paul Smith  &lt;psmith@apache.org&gt;
 */
public class LogUI extends JFrame {
    private static Logger logger = LogManager.getLogger(LogUI.class);

    private static final String MAIN_WINDOW_HEIGHT = "main.window.height";
    private static final String MAIN_WINDOW_WIDTH = "main.window.width";
    private static final String MAIN_WINDOW_Y = "main.window.y";
    private static final String MAIN_WINDOW_X = "main.window.x";

    private static final double DEFAULT_MAIN_RECEIVER_SPLIT_LOCATION = 0.85d;

    /* Panels / Views */
    private final JFrame preferencesFrame = new JFrame();
    private ReceiversPanel receiversPanel;
    private WelcomePanel welcomePanel;
    private ChainsawTabbedPane tabbedPane;
    private ChainsawAbout aboutBox;
    public TutorialFrame tutorialFrame;
    private JSplitPane mainReceiverSplitPane;
    private final List<LogPanel> identifierPanels = new ArrayList<>();


    private JToolBar toolbar;
    private ChainsawToolBarAndMenus chainsawToolBarAndMenus;
    private ChainsawStatusBar statusBar;
    private ApplicationPreferenceModelPanel applicationPreferenceModelPanel;
    private final List<String> filterableColumns = new ArrayList<>();
    private final Map<String, Component> panelMap = new HashMap<>();
    public ChainsawAppender chainsawAppender;
    private SettingsManager settingsManager;
    private double lastMainReceiverSplitLocation = DEFAULT_MAIN_RECEIVER_SPLIT_LOCATION;
    private int dividerSize;
    public int cyclicBufferSize;
    private List<ChainsawReceiver> receivers = new ArrayList<>();
    private List<ReceiverEventListener> receiverListeners = new ArrayList<>();
    private ZeroConfPlugin zeroConf = new ZeroConfPlugin(settingsManager);

    /**
     * Clients can register a ShutdownListener to be notified when the user has
     * requested Chainsaw to exit.
     */
    private EventListenerList shutdownListenerList = new EventListenerList();

    //map of tab names to rulecolorizers
    private Map<String, RuleColorizer> allColorizers = new HashMap<>();
    private RuleColorizer globalRuleColorizer = new RuleColorizer(settingsManager, true);
    private AbstractConfiguration configuration;

    private ShutdownManager shutdownManager;

    /**
     * Constructor which builds up all the visual elements of the frame including
     * the Menu bar
     */
    public LogUI(SettingsManager settingsManager) {
        super("Chainsaw");

        this.settingsManager = settingsManager;
        this.configuration = settingsManager.getGlobalConfiguration();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        globalRuleColorizer.setConfiguration(configuration);
        globalRuleColorizer.loadColorSettings();

        if (ChainsawIcons.WINDOW_ICON != null) {
            setIconImage(new ImageIcon(ChainsawIcons.WINDOW_ICON).getImage());
        }

        shutdownManager = new ShutdownManager(this, configuration, receivers, shutdownListenerList);
    }


    /**
     * Registers a ShutdownListener with this class so that it can be notified
     * when the user has requested that Chainsaw exit.
     *
     * @param listener the listener to add
     */
    public void addShutdownListener(ShutdownListener listener) {
        shutdownListenerList.add(ShutdownListener.class, listener);
    }

    /**
     * Initialises the menu's and toolbars, but does not actually create any of
     * the main panel components.
     */
    private void initGUI() {

        setupHelpSystem();
        statusBar = new ChainsawStatusBar(this);
        setupReceiverPanel();

        this.chainsawToolBarAndMenus = new ChainsawToolBarAndMenus(this, configuration);
        toolbar = chainsawToolBarAndMenus.getToolbar();
        setJMenuBar(chainsawToolBarAndMenus.getMenubar());

        tabbedPane = new ChainsawTabbedPane();

        /**
         * This adds Drag & Drop capability to Chainsaw
         */
        FileDnDTarget dnDTarget = new FileDnDTarget(tabbedPane);
        dnDTarget.addPropertyChangeListener("fileList", evt -> {
            final List fileList = (List) evt.getNewValue();

            Thread thread = new Thread(() -> {
                logger.debug("Loading files: {}", fileList);
                for (Object aFileList : fileList) {
                    File file = (File) aFileList;
                    try {
                        getStatusBar().setMessage("Loading " + file.getAbsolutePath() + "...");
                    } catch (Exception e) {
                        String errorMsg = "Failed to import a file";
                        logger.error(errorMsg, e);
                        getStatusBar().setMessage(errorMsg);
                    }
                }
            });

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();

        });

        applicationPreferenceModelPanel = new ApplicationPreferenceModelPanel(settingsManager);

        applicationPreferenceModelPanel.setOkCancelActionListener(
            e -> preferencesFrame.setVisible(false));
        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action closeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                preferencesFrame.setVisible(false);
            }
        };
        preferencesFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "ESCAPE");
        preferencesFrame.getRootPane().
            getActionMap().put("ESCAPE", closeAction);

        OSXIntegration.init(this);

    }

    private void setupReceiverPanel() {
        receiversPanel = new ReceiversPanel(settingsManager, receivers, this, statusBar);
    }

    /**
     * Initialises the Help system and the WelcomePanel
     */
    private void setupHelpSystem() {
        welcomePanel = new WelcomePanel();

        tutorialFrame = new TutorialFrame(receivers, receiverListeners, this, statusBar);

        JToolBar tb = welcomePanel.getToolbar();
        JButton help = new SmallButton.Builder().iconUrl(ChainsawIcons.HELP)
            .action(tutorialFrame::setupTutorial).shortDescription("Tutorial").build();

        tb.add(help);

        tb.addSeparator();

        JButton exampleButton = new SmallButton.Builder().iconUrl(ChainsawIcons.HELP)
            .action(
                () -> HelpManager.getInstance().setHelpURL(ChainsawConstants.EXAMPLE_CONFIG_URL)
            )
            .name("View example Receiver configuration")
            .shortDescription("Displays an example Log4j configuration file with several Receivers defined.")
            .build();


        tb.add(exampleButton);

        tb.add(Box.createHorizontalGlue());

        /*
         * Setup a listener on the HelpURL property and automatically change the WelcomePages URL
         * to it.
         */
        HelpManager.getInstance().addPropertyChangeListener(
            "helpURL",
            evt -> {
                URL newURL = (URL) evt.getNewValue();

                if (newURL != null) {
                    welcomePanel.setURL(newURL);
                    ensureWelcomePanelVisible();
                }
            });
    }

    private void ensureWelcomePanelVisible() {
        // ensure that the Welcome Panel is made visible
        if (!getTabbedPane().containsWelcomePanel()) {
            addWelcomePanel();
        }
        getTabbedPane().setSelectedComponent(welcomePanel);
    }

    /**
     * Given the load event, configures the size/location of the main window etc
     * etc.
     */
    private void loadSettings() {
        AbstractConfiguration config = settingsManager.getGlobalConfiguration();
        setLocation(
            config.getInt(LogUI.MAIN_WINDOW_X, 0), config.getInt(LogUI.MAIN_WINDOW_Y, 0));
        int width = config.getInt(LogUI.MAIN_WINDOW_WIDTH, -1);
        int height = config.getInt(LogUI.MAIN_WINDOW_HEIGHT, -1);
        if (width == -1 && height == -1) {
            width = Toolkit.getDefaultToolkit().getScreenSize().width;
            height = Toolkit.getDefaultToolkit().getScreenSize().height;
            setSize(width, height);
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        } else {
            setSize(width, height);
        }

        chainsawToolBarAndMenus.stateChange();
        RuleColorizer colorizer = new RuleColorizer(settingsManager);
        allColorizers.put(ChainsawConstants.DEFAULT_COLOR_RULE_NAME, colorizer);
    }

    public void buildChainsawLogPanel() {
        buildLogPanel(false, "Chainsaw", chainsawAppender.getReceiver());
    }

    /**
     * Activates itself as a viewer by configuring Size, and location of itself,
     * and configures the default Tabbed Pane elements with the correct layout,
     * table columns, and sets itself viewable.
     */
    public void activateViewer() {
        initGUI();
        loadSettings();

        filterableColumns.add(ChainsawConstants.LEVEL_COL_NAME);
        filterableColumns.add(ChainsawConstants.LOGGER_COL_NAME);
        filterableColumns.add(ChainsawConstants.THREAD_COL_NAME);
        filterableColumns.add(ChainsawConstants.NDC_COL_NAME);
        filterableColumns.add(ChainsawConstants.PROPERTIES_COL_NAME);
        filterableColumns.add(ChainsawConstants.CLASS_COL_NAME);
        filterableColumns.add(ChainsawConstants.METHOD_COL_NAME);
        filterableColumns.add(ChainsawConstants.FILE_COL_NAME);
        filterableColumns.add(ChainsawConstants.NONE_COL_NAME);

        JPanel panePanel = new JPanel();
        panePanel.setLayout(new BorderLayout(2, 2));

        getContentPane().setLayout(new BorderLayout());

        getTabbedPane().addChangeListener(chainsawToolBarAndMenus);
        getTabbedPane().addChangeListener(e -> {
            LogPanel thisLogPanel = getCurrentLogPanel();
            if (thisLogPanel != null) {
                thisLogPanel.updateStatusBar();
            }
        });

        LogUiKeyStrokeCreator.createKeyStrokeRight(tabbedPane);
        LogUiKeyStrokeCreator.createKeyStrokeLeft(tabbedPane);
        LogUiKeyStrokeCreator.createKeyStrokeGotoLine(tabbedPane, this);

        /**
         * We listen for double clicks, and auto-undock currently selected Tab if
         * the mouse event location matches the currently selected tab
         */
        getTabbedPane().addMouseListener(
            new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);

                    if (
                        (e.getClickCount() > 1)
                            && ((e.getModifiers() & InputEvent.BUTTON1_MASK) > 0)) {
                        int tabIndex = getTabbedPane().getSelectedIndex();

                        if (
                            (tabIndex != -1)
                                && (tabIndex == getTabbedPane().getSelectedIndex())) {
                            LogPanel logPanel = getCurrentLogPanel();

                            if (logPanel != null) {
                                logPanel.undock();
                            }
                        }
                    }
                }
            });

        panePanel.add(getTabbedPane());
        addWelcomePanel();

        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        mainReceiverSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panePanel, receiversPanel);
        mainReceiverSplitPane.setContinuousLayout(true);
        dividerSize = mainReceiverSplitPane.getDividerSize();
        mainReceiverSplitPane.setDividerLocation(-1);

        getContentPane().add(mainReceiverSplitPane, BorderLayout.CENTER);

        mainReceiverSplitPane.setResizeWeight(1.0);
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    exit();
                }
            });
        preferencesFrame.setTitle("'Application-wide Preferences");
        preferencesFrame.setIconImage(
            ((ImageIcon) ChainsawIcons.ICON_PREFERENCES).getImage());
        preferencesFrame.getContentPane().add(applicationPreferenceModelPanel);

        preferencesFrame.setSize(750, 520);

        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        preferencesFrame.setLocation(
            new Point(
                (screenDimension.width / 2) - (preferencesFrame.getSize().width / 2),
                (screenDimension.height / 2) - (preferencesFrame.getSize().height / 2)));

        pack();

        final JPopupMenu tabPopup = new JPopupMenu();
        final Action hideCurrentTabAction =
            new AbstractAction("Hide") {
                public void actionPerformed(ActionEvent e) {
                    Component selectedComp = getTabbedPane().getSelectedComponent();
                    if (selectedComp instanceof LogPanel) {
                        displayPanel(getCurrentLogPanel().getIdentifier(), false);
                        chainsawToolBarAndMenus.stateChange();
                    } else {
                        getTabbedPane().remove(selectedComp);
                    }
                }
            };

        final Action hideOtherTabsAction =
            new AbstractAction("Hide Others") {
                public void actionPerformed(ActionEvent e) {
                    Component selectedComp = getTabbedPane().getSelectedComponent();
                    String currentName;
                    if (selectedComp instanceof LogPanel) {
                        currentName = getCurrentLogPanel().getIdentifier();
                    } else if (selectedComp instanceof WelcomePanel) {
                        currentName = ChainsawTabbedPane.WELCOME_TAB;
                    } else {
                        currentName = ChainsawTabbedPane.ZEROCONF;
                    }

                    int count = getTabbedPane().getTabCount();
                    int index = 0;

                    for (int i = 0; i < count; i++) {
                        String name = getTabbedPane().getTitleAt(index);

                        if (
                            panelMap.containsKey(name) && !name.equals(currentName)) {
                            displayPanel(name, false);
                            chainsawToolBarAndMenus.stateChange();
                        } else {
                            index++;
                        }
                    }
                }
            };

        Action showHiddenTabsAction =
            new AbstractAction("Show All Hidden") {
                public void actionPerformed(ActionEvent e) {

                    for (Map.Entry<String, Boolean> entry : getPanels().entrySet()) {
                        Boolean docked = entry.getValue();
                        if (Boolean.TRUE.equals(docked)) {
                            String identifier = entry.getKey();
                            int count = getTabbedPane().getTabCount();
                            boolean found = false;

                            for (int i = 0; i < count; i++) {
                                String name = getTabbedPane().getTitleAt(i);

                                if (name.equals(identifier)) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                displayPanel(identifier, true);
                                chainsawToolBarAndMenus.stateChange();
                            }
                        }
                    }
                }
            };

        tabPopup.add(hideCurrentTabAction);
        tabPopup.add(hideOtherTabsAction);
        tabPopup.addSeparator();
        tabPopup.add(showHiddenTabsAction);

        final PopupListener tabPopupListener = new PopupListener(tabPopup);
        getTabbedPane().addMouseListener(tabPopupListener);

        initPrefModelListeners();
        setVisible(true);

        if (configuration.getBoolean("showReceivers", false)) {
            showReceiverPanel();
        } else {
            hideReceiverPanel();
        }

        /*
         * loads the saved tab settings and if there are hidden tabs,
         * hide those tabs out of currently loaded tabs..
         */

        if (!configuration.getBoolean("displayWelcomeTab", true)) {
            displayPanel(ChainsawTabbedPane.WELCOME_TAB, false);
        }
        if (!configuration.getBoolean("displayZeroconfTab", true)) {
            displayPanel(ChainsawTabbedPane.ZEROCONF, false);
        }
        chainsawToolBarAndMenus.stateChange();
    }

    /**
     * Display the log tree pane, using the last known divider location
     */
    private void showReceiverPanel() {
        mainReceiverSplitPane.setDividerSize(dividerSize);
        mainReceiverSplitPane.setDividerLocation(lastMainReceiverSplitLocation);
        receiversPanel.setVisible(true);
        mainReceiverSplitPane.repaint();
    }

    /**
     * Hide the log tree pane, holding the current divider location for later use
     */
    private void hideReceiverPanel() {
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

    private void initPrefModelListeners() {
        int tooltipDisplayMillis = configuration.getInt("tooltipDisplayMillis", 4000);

        ToolTipManager.sharedInstance().setDismissDelay(tooltipDisplayMillis);

        configuration.addEventListener(ConfigurationEvent.SET_PROPERTY,
            evt -> {
                if (evt.getPropertyName().equals("statusBar")) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    statusBar.setVisible(value);
                }
            });
        boolean showStatusBar = configuration.getBoolean("statusBar", true);
        setStatusBarVisible(showStatusBar);

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
        boolean showReceivers = configuration.getBoolean("showReceivers", false);
        setStatusBarVisible(showStatusBar);
        if (showReceivers) {
            showReceiverPanel();
        } else {
            hideReceiverPanel();
        }

        configuration.addEventListener(ConfigurationEvent.SET_PROPERTY,
            evt -> {
                if (evt.getPropertyName().equals("toolbar")) {
                    boolean value = (Boolean) evt.getPropertyValue();
                    toolbar.setVisible(value);
                }
            });
        boolean showToolbar = configuration.getBoolean("toolbar", true);
        toolbar.setVisible(showToolbar);

    }

    /**
     * Exits the application, ensuring Settings are saved.
     */
    public boolean exit() {
        for (ChainsawReceiver rx : receivers) {
            settingsManager.saveSettingsForReceiver(rx);
        }

        settingsManager.saveAllSettings();

        return shutdownManager.shutdown();
    }

    void addWelcomePanel() {
        getTabbedPane().insertTab(
            ChainsawTabbedPane.WELCOME_TAB, new ImageIcon(ChainsawIcons.ABOUT), welcomePanel,
            "Welcome/Help", 0);
        getTabbedPane().setSelectedComponent(welcomePanel);
        panelMap.put(ChainsawTabbedPane.WELCOME_TAB, welcomePanel);
    }

    void removeWelcomePanel() {
        EventQueue.invokeLater(() -> {
            if (getTabbedPane().containsWelcomePanel()) {
                getTabbedPane().remove(
                    getTabbedPane().getComponentAt(getTabbedPane().indexOfTab(ChainsawTabbedPane.WELCOME_TAB)));
            }
        });
    }

    ChainsawStatusBar getStatusBar() {
        return statusBar;
    }

    public void showApplicationPreferences() {
        preferencesFrame.setVisible(true);
    }

    public void loadReceiver() {
        Runnable r = () -> {
            JFileChooser jfc = new JFileChooser(SettingsManager.getSettingsDirectory());
            int returnVal = jfc.showOpenDialog(this);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }

            logger.debug("Load file {}", jfc.getSelectedFile());

            // Create the receiver
            String fileToLoad = jfc.getSelectedFile().getName();
            String receiverName = fileToLoad.split("-")[0];
            AbstractConfiguration config = settingsManager.getSettingsForReceiverTab(receiverName);
            String typeToLoad = config.getString("receiver.type");
            ServiceLoader<ChainsawReceiverFactory> sl = ServiceLoader.load(ChainsawReceiverFactory.class);

            for (ChainsawReceiverFactory crFactory : sl) {
                if (crFactory.getReceiverName().equals(typeToLoad)) {
                    ChainsawReceiver rx = crFactory.create();
                    rx.setName(receiverName);
                    settingsManager.loadSettingsForReceiver(rx);
                    addReceiver(rx);

                    rx.start();
                }
            }
        };

        SwingUtilities.invokeLater(r);
    }

    public void showAboutBox() {
        if (aboutBox == null) {
            aboutBox = new ChainsawAbout(this);
        }

        aboutBox.setVisible(true);
    }

    Map<String, Boolean> getPanels() {
        Map<String, Boolean> result = new HashMap<>();
        Set<Map.Entry<String, Component>> panelSet = panelMap.entrySet();

        for (Map.Entry<String, Component> panel : panelSet) {
            Component component = panel.getValue();
            boolean value = !(component instanceof LogPanel) || ((DockablePanel) panel.getValue()).isDocked();
            result.put(panel.getKey(), value);
        }

        return result;
    }

    void displayPanel(String panelName, boolean display) {
        Component p = panelMap.get(panelName);

        int index = getTabbedPane().indexOfTab(panelName);

        if ((index == -1) && display) {
            getTabbedPane().addTab(panelName, p);
        }

        if ((index > -1) && !display) {
            getTabbedPane().removeTabAt(index);
        }
    }

    /**
     * Returns the currently selected LogPanel, if there is one, otherwise null
     *
     * @return current log panel
     */
    public LogPanel getCurrentLogPanel() {
        Component selectedTab = getTabbedPane().getSelectedComponent();

        if (selectedTab instanceof LogPanel) {
            return (LogPanel) selectedTab;
        }

        return null;
    }

    /**
     * @param visible
     */
    private void setStatusBarVisible(final boolean visible) {
        logger.debug("Setting StatusBar to {}", visible);
        SwingUtilities.invokeLater(
            () -> statusBar.setVisible(visible));
    }

    boolean isStatusBarVisible() {
        return statusBar.isVisible();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getActiveTabName() {
        int index = getTabbedPane().getSelectedIndex();

        if (index == -1) {
            return null;
        } else {
            return getTabbedPane().getTitleAt(index);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return log tree panel visible flag
     */
    public boolean isLogTreePanelVisible() {
        return getCurrentLogPanel() != null && getCurrentLogPanel().isLogTreeVisible();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public ChainsawTabbedPane getTabbedPane() {
        return tabbedPane;
    }


    private void buildLogPanel(boolean customExpression, final String ident, final ChainsawReceiver rx)
        throws IllegalArgumentException {
        final LogPanel thisPanel = new LogPanel(settingsManager, getStatusBar(), ident, cyclicBufferSize, allColorizers, globalRuleColorizer);

        if (!customExpression && rx != null) {
            thisPanel.setReceiver(rx);
        }

        /*
         * Now add the panel as a batch listener so it can handle it's own
         * batchs
         */
        if (!customExpression) {
            identifierPanels.add(thisPanel);
        }

        TabIconHandler iconHandler = new TabIconHandler(ident, tabbedPane);
        thisPanel.addEventCountListener(iconHandler);

        tabbedPane.addChangeListener(iconHandler);

        PropertyChangeListener toolbarMenuUpdateListener =
            evt -> chainsawToolBarAndMenus.stateChange();

        thisPanel.addPropertyChangeListener(toolbarMenuUpdateListener);
        thisPanel.addPreferencePropertyChangeListener(toolbarMenuUpdateListener);

        thisPanel.addPropertyChangeListener(
            "docked",
            evt -> {
                LogPanel logPanel = (LogPanel) evt.getSource();

                if (logPanel.isDocked()) {
                    panelMap.put(logPanel.getIdentifier(), logPanel);
                    getTabbedPane().addANewTab(
                        logPanel.getIdentifier(), logPanel, null,
                        true);
                    getTabbedPane().setSelectedTab(getTabbedPane().indexOfTab(logPanel.getIdentifier()));
                } else {
                    getTabbedPane().remove(logPanel);
                }
            });

        logger.debug("adding logpanel to tabbed pane: {}", ident);

        //NOTE: tab addition is a very fragile process - if you modify this code,
        //verify the frames in the individual log panels initialize to their
        //correct sizes
        getTabbedPane().add(ident, thisPanel);
        panelMap.put(ident, thisPanel);

        /*
         * Let the new LogPanel receive this batch
         */

        SwingUtilities.invokeLater(
            () -> {
                getTabbedPane().addANewTab(
                    ident,
                    thisPanel,
                    new ImageIcon(ChainsawIcons.ANIM_RADIO_TOWER),
                    false);
                thisPanel.layoutComponents();

                getTabbedPane().addANewTab(ChainsawTabbedPane.ZEROCONF,
                    zeroConf,
                    null,
                    false);
            });

        logger.debug("added tab {}", ident);
    }

    public void createCustomExpressionLogPanel(String ident) {
        //collect events matching the rule from all of the tabs
        try {
            List<ChainsawLoggingEvent> list = new ArrayList<>();
            Rule rule = ExpressionRule.getRule(ident);

            for (LogPanel identifierPanel : identifierPanels) {
                for (LoggingEventWrapper e : identifierPanel.getMatchingEvents(rule)) {
                    list.add(e.getLoggingEvent());
                }
            }

            buildLogPanel(true, ident, null);
        } catch (IllegalArgumentException iae) {
            statusBar.setMessage(
                "Unable to add tab using expression: " + ident + ", reason: "
                    + iae.getMessage());
        }
    }



    public void addReceiver(ChainsawReceiver rx) {
        receivers.add(rx);
        buildLogPanel(false, rx.getName(), rx);

        for (ReceiverEventListener listen : receiverListeners) {
            listen.receiverAdded(rx);
        }
    }

    public void addReceiverEventListener(ReceiverEventListener listener) {
        receiverListeners.add(listener);
    }

    public List<ChainsawReceiver> getAllReceivers() {
        return receivers;
    }
}
