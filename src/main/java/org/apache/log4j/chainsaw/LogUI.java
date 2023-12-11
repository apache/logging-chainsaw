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
    private final JFrame preferencesFrame = new JFrame();
    private boolean noReceiversDefined;
    private ReceiversPanel receiversPanel;
    private ChainsawTabbedPane tabbedPane;
    private JToolBar toolbar;
    private ChainsawStatusBar statusBar;
    private ApplicationPreferenceModel applicationPreferenceModel;
    private ApplicationPreferenceModelPanel applicationPreferenceModelPanel;
    private final List<String> filterableColumns = new ArrayList<>();
    private final Map<String, Component> panelMap = new HashMap<>();
    public ChainsawAppender chainsawAppender;
    private ChainsawToolBarAndMenus chainsawToolBarAndMenus;
    private ChainsawAbout aboutBox;
    private SettingsManager settingsManager;
    public TutorialFrame tutorialFrame;
    private JSplitPane mainReceiverSplitPane;
    private double lastMainReceiverSplitLocation = DEFAULT_MAIN_RECEIVER_SPLIT_LOCATION;
    private final List<LogPanel> identifierPanels = new ArrayList<>();
    private int dividerSize;
    public int cyclicBufferSize;
    private List<ChainsawReceiver> receivers = new ArrayList<>();
    private List<ReceiverEventListener> receiverListeners = new ArrayList<>();
    private ZeroConfPlugin zeroConf = new ZeroConfPlugin(settingsManager);


    private final Object initializationLock = new Object();

    /**
     * The shutdownAction is called when the user requests to exit Chainsaw, and
     * by default this exits the VM, but a developer may replace this action with
     * something that better suits their needs
     */
    private Action shutdownAction = null;

    /**
     * Clients can register a ShutdownListener to be notified when the user has
     * requested Chainsaw to exit.
     */
    private EventListenerList shutdownListenerList = new EventListenerList();
    private WelcomePanel welcomePanel;

    //map of tab names to rulecolorizers
    private Map<String, RuleColorizer> allColorizers = new HashMap<>();
    private RuleColorizer globalRuleColorizer = new RuleColorizer(settingsManager, true);
    private ReceiverConfigurationPanel receiverConfigurationPanel = new ReceiverConfigurationPanel(settingsManager);
    private AbstractConfiguration configuration;

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
    }

    /**
     * Registers a ShutdownListener with this calss so that it can be notified
     * when the user has requested that Chainsaw exit.
     *
     * @param l
     */
    public void addShutdownListener(ShutdownListener l) {
        shutdownListenerList.add(ShutdownListener.class, l);
    }

    /**
     * Initialises the menu's and toolbars, but does not actually create any of
     * the main panel components.
     */
    private void initGUI() {

        setupHelpSystem();
        statusBar = new ChainsawStatusBar(this);
        setupReceiverPanel();

        setToolBarAndMenus(new ChainsawToolBarAndMenus(this, configuration));
        toolbar = getToolBarAndMenus().getToolbar();
        setJMenuBar(getToolBarAndMenus().getMenubar());

        setTabbedPane(new ChainsawTabbedPane());

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
//        receiversPanel.addPropertyChangeListener(
//            "visible",
//            evt -> getApplicationPreferenceModel().setReceivers(
//                (Boolean) evt.getNewValue()));
    }

    /**
     * Initialises the Help system and the WelcomePanel
     */
    private void setupHelpSystem() {
        welcomePanel = new WelcomePanel();

        tutorialFrame = new TutorialFrame(receivers, receiverListeners, this, statusBar) ;

        JToolBar tb = welcomePanel.getToolbar();
        JButton help = new SmallButton.Builder().iconUrl(ChainsawIcons.HELP)
            .action(tutorialFrame::setupTutorial).shortDescription("Tutorial").build();

        tb.add(help);

        tb.addSeparator();

        JButton exampleButton = new SmallButton.Builder().iconUrl(ChainsawIcons.HELP)
            .action(
                ()-> HelpManager.getInstance().setHelpURL(ChainsawConstants.EXAMPLE_CONFIG_URL)
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

        getToolBarAndMenus().stateChange();
        RuleColorizer colorizer = new RuleColorizer(settingsManager);
        allColorizers.put(ChainsawConstants.DEFAULT_COLOR_RULE_NAME, colorizer);
    }

    public void buildChainsawLogPanel(){
        List<ChainsawLoggingEvent> events = new ArrayList<>();
        buildLogPanel(false, "Chainsaw", events, chainsawAppender.getReceiver());
    }

    /**
     * Activates itself as a viewer by configuring Size, and location of itself,
     * and configures the default Tabbed Pane elements with the correct layout,
     * table columns, and sets itself viewable.
     */
    public void activateViewer() {
        initGUI();
        loadSettings();

        noReceiversDefined = receivers.isEmpty();

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

        getTabbedPane().addChangeListener(getToolBarAndMenus());
        getTabbedPane().addChangeListener(e -> {
            LogPanel thisLogPanel = getCurrentLogPanel();
            if (thisLogPanel != null) {
                thisLogPanel.updateStatusBar();
            }
        });

        KeyStroke ksRight =
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke ksLeft =
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        KeyStroke ksGotoLine =
            KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

        getTabbedPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            ksRight, "MoveRight");
        getTabbedPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            ksLeft, "MoveLeft");
        getTabbedPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            ksGotoLine, "GotoLine");

        Action moveRight =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int temp = getTabbedPane().getSelectedIndex();
                    ++temp;

                    if (temp != getTabbedPane().getTabCount()) {
                        getTabbedPane().setSelectedTab(temp);
                    }
                }
            };

        Action moveLeft =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int temp = getTabbedPane().getSelectedIndex();
                    --temp;

                    if (temp > -1) {
                        getTabbedPane().setSelectedTab(temp);
                    }
                }
            };

        Action gotoLine =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    String inputLine = JOptionPane.showInputDialog(LogUI.this, "Enter the line number to go:", "Goto Line", JOptionPane.PLAIN_MESSAGE);
                    try {
                        int lineNumber = Integer.parseInt(inputLine);
                        int row = getCurrentLogPanel().setSelectedEvent(lineNumber);
                        if (row == -1) {
                            JOptionPane.showMessageDialog(LogUI.this, "You have entered an invalid line number", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(LogUI.this, "You have entered an invalid line number", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };


        getTabbedPane().getActionMap().put("MoveRight", moveRight);
        getTabbedPane().getActionMap().put("MoveLeft", moveLeft);
        getTabbedPane().getActionMap().put("GotoLine", gotoLine);

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
                            getPanelMap().containsKey(name) && !name.equals(currentName)) {
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

//        this.handler.addPropertyChangeListener(
//            "dataRate",
//            evt -> {
//                double dataRate = (Double) evt.getNewValue();
//                statusBar.setDataRate(dataRate);
//            });

//        getSettingsManager().addSettingsListener(this);
//        getSettingsManager().addSettingsListener(MRUFileListPreferenceSaver.getInstance());
//        getSettingsManager().addSettingsListener(receiversPanel);
//        try {
//            //if an uncaught exception is thrown, allow the UI to continue to load
//            getSettingsManager().loadSettings();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        //app preferences have already been loaded (and configuration url possibly set to blank if being overridden)
        //but we need a listener so the settings will be saved on exit (added after loadsettings was called)
//        getSettingsManager().addSettingsListener(new ApplicationPreferenceModelSaver(applicationPreferenceModel));

        setVisible(true);

        if (configuration.getBoolean("showReceivers", false)) {
            showReceiverPanel();
        } else {
            hideReceiverPanel();
        }

        synchronized (initializationLock) {
            initializationLock.notifyAll();
        }

        if (
            noReceiversDefined
                && configuration.getBoolean("showNoReceiverWarning", true)) {
            SwingHelper.invokeOnEDT(this::showReceiverConfigurationPanel);
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
//        applicationPreferenceModel.addPropertyChangeListener(
//            "identifierExpression",
//            evt -> handler.setIdentifierExpression(evt.getNewValue().toString()));
//        handler.setIdentifierExpression(applicationPreferenceModel.getIdentifierExpression());

        int tooltipDisplayMillis = configuration.getInt("tooltipDisplayMillis", 4000);
//        applicationPreferenceModel.addPropertyChangeListener(
//            "toolTipDisplayMillis",
//            evt -> ToolTipManager.sharedInstance().setDismissDelay(
//                (Integer) evt.getNewValue()));
        ToolTipManager.sharedInstance().setDismissDelay(
            tooltipDisplayMillis);

//        applicationPreferenceModel.addPropertyChangeListener(
//            "responsiveness",
//            evt -> {
//                int value = (Integer) evt.getNewValue();
//                handler.setQueueInterval((value * 1000) - 750);
//            });
//        handler.setQueueInterval((applicationPreferenceModel.getResponsiveness() * 1000) - 750);

//        applicationPreferenceModel.addPropertyChangeListener(
//            "tabPlacement",
//            evt -> SwingUtilities.invokeLater(
//                () -> {
//                    int placement = (Integer) evt.getNewValue();
//
//                    switch (placement) {
//                        case SwingConstants.TOP:
//                        case SwingConstants.BOTTOM:
//                            tabbedPane.setTabPlacement(placement);
//
//                            break;
//
//                        default:
//                            break;
//                    }
//                }));
//
        configuration.addEventListener(ConfigurationEvent.SET_PROPERTY,
            evt -> {
                if( evt.getPropertyName().equals( "statusBar" ) ){
                    boolean value = (Boolean) evt.getPropertyValue();
                    statusBar.setVisible(value);
                }
            });
        boolean showStatusBar = configuration.getBoolean("statusBar", true);
        setStatusBarVisible(showStatusBar);

        configuration.addEventListener(ConfigurationEvent.SET_PROPERTY,
            evt -> {
                if( evt.getPropertyName().equals( "showReceivers" ) ){
                    boolean value = (Boolean) evt.getPropertyValue();
                    if( value ){
                        showReceiverPanel();
                    }else{
                        hideReceiverPanel();
                    }
                }
            });
        boolean showReceivers = configuration.getBoolean("showReceivers", false);
        setStatusBarVisible(showStatusBar);
        if( showReceivers ){
            showReceiverPanel();
        }else{
            hideReceiverPanel();
        }
//
//        applicationPreferenceModel.addPropertyChangeListener(
//            "receivers",
//            evt -> {
//                boolean value = (Boolean) evt.getNewValue();
//
//                if (value) {
//                    showReceiverPanel();
//                } else {
//                    hideReceiverPanel();
//                }
//            });
////    if (applicationPreferenceModel.isReceivers()) {
////      showReceiverPanel();
////    } else {
////      hideReceiverPanel();
////    }
//
//
        configuration.addEventListener(ConfigurationEvent.SET_PROPERTY,
            evt -> {
                if( evt.getPropertyName().equals( "toolbar" ) ){
                    boolean value = (Boolean) evt.getPropertyValue();
                    toolbar.setVisible(value);
                }
            });
        boolean showToolbar = configuration.getBoolean("toolbar", true);
        toolbar.setVisible(showToolbar);

    }

    /**
     * Displays a dialog which will provide options for selecting a configuration
     */
    private void showReceiverConfigurationPanel() {
//        SwingUtilities.invokeLater(
//            () -> {
//                final JDialog dialog = new JDialog(LogUI.this, true);
//                dialog.setTitle("Load events into Chainsaw");
//                dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
//
//                dialog.setResizable(false);
//
//                receiverConfigurationPanel.setCompletionActionListener(
//                    e -> {
//                        dialog.setVisible(false);
//
//                        if (receiverConfigurationPanel.getModel().isCancelled()) {
//                            return;
//                        }
//                        applicationPreferenceModel.setShowNoReceiverWarning(!receiverConfigurationPanel.isDontWarnMeAgain());
//                        //remove existing plugins
//                        List<Plugin> plugins = pluginRegistry.getPlugins();
//                        for (Object plugin1 : plugins) {
//                            Plugin plugin = (Plugin) plugin1;
//                            //don't stop ZeroConfPlugin if it is registered
//                            if (!plugin.getName().toLowerCase(Locale.ENGLISH).contains("zeroconf")) {
//                                pluginRegistry.stopPlugin(plugin.getName());
//                            }
//                        }
//                        URL configURL = null;
//
//                        if (receiverConfigurationPanel.getModel().isNetworkReceiverMode()) {
//                            int port = receiverConfigurationPanel.getModel().getNetworkReceiverPort();
//
//                            try {
//                                Class<? extends Receiver> receiverClass = receiverConfigurationPanel.getModel().getNetworkReceiverClass();
//                                Receiver networkReceiver = receiverClass.newInstance();
//                                networkReceiver.setName(receiverClass.getSimpleName() + "-" + port);
//
//                                Method portMethod =
//                                    networkReceiver.getClass().getMethod(
//                                        "setPort", int.class);
//                                portMethod.invoke(
//                                    networkReceiver, port);
//
//                                networkReceiver.setThreshold(Level.TRACE);
//
//                                pluginRegistry.addPlugin(networkReceiver);
//                                networkReceiver.activateOptions();
//                                receiversPanel.updateReceiverTreeInDispatchThread();
//                            } catch (Exception e3) {
//                                logger.error(
//                                    "Error creating Receiver", e3);
//                                statusBar.setMessage(
//                                    "An error occurred creating your Receiver");
//                            }
//                        } else if (receiverConfigurationPanel.getModel().isLog4jConfig()) {
//                            File log4jConfigFile = receiverConfigurationPanel.getModel().getLog4jConfigFile();
//                            if (log4jConfigFile != null) {
//                                try {
//                                    Map<String, Map<String, String>> entries = LogFilePatternLayoutBuilder.getAppenderConfiguration(log4jConfigFile);
//                                    for (Object o : entries.entrySet()) {
//                                        try {
//                                            Map.Entry entry = (Map.Entry) o;
//                                            String name = (String) entry.getKey();
//                                            Map values = (Map) entry.getValue();
//                                            //values: conversion, file
//                                            String conversionPattern = values.get("conversion").toString();
//                                            File file = new File(values.get("file").toString());
//                                            URL fileURL = file.toURI().toURL();
//                                            String timestampFormat = LogFilePatternLayoutBuilder.getTimeStampFormat(conversionPattern);
//                                            String receiverPattern = LogFilePatternLayoutBuilder.getLogFormatFromPatternLayout(conversionPattern);
//                                            VFSLogFilePatternReceiver fileReceiver = new VFSLogFilePatternReceiver();
//                                            fileReceiver.setName(name);
//                                            fileReceiver.setAutoReconnect(true);
//                                            fileReceiver.setContainer(LogUI.this);
//                                            fileReceiver.setAppendNonMatches(true);
//                                            fileReceiver.setFileURL(fileURL.toURI().toString());
//                                            fileReceiver.setTailing(true);
//                                            fileReceiver.setLogFormat(receiverPattern);
//                                            fileReceiver.setTimestampFormat(timestampFormat);
////                                            fileReceiver.setThreshold(Level.TRACE);
////                                            pluginRegistry.addPlugin(fileReceiver);
//                                            fileReceiver.activateOptions();
//                                            receiversPanel.updateReceiverTreeInDispatchThread();
//                                        } catch (URISyntaxException e1) {
//                                            e1.printStackTrace();
//                                        }
//                                    }
//                                } catch (IOException e1) {
//                                    e1.printStackTrace();
//                                }
//                            }
//                        } else if (receiverConfigurationPanel.getModel().isLoadConfig()) {
//                            configURL = receiverConfigurationPanel.getModel().getConfigToLoad();
//                        } else if (receiverConfigurationPanel.getModel().isLogFileReceiverConfig()) {
//                            try {
//                                URL fileURL = receiverConfigurationPanel.getModel().getLogFileURL();
//                                if (fileURL != null) {
//                                    VFSLogFilePatternReceiver fileReceiver = new VFSLogFilePatternReceiver();
//                                    fileReceiver.setName(fileURL.getFile());
//                                    fileReceiver.setAutoReconnect(true);
//                                    fileReceiver.setContainer(LogUI.this);
//                                    fileReceiver.setAppendNonMatches(true);
//                                    fileReceiver.setFileURL(fileURL.toURI().toString());
//                                    fileReceiver.setTailing(true);
//                                    if (receiverConfigurationPanel.getModel().isPatternLayoutLogFormat()) {
//                                        fileReceiver.setLogFormat(LogFilePatternLayoutBuilder.getLogFormatFromPatternLayout(receiverConfigurationPanel.getModel().getLogFormat()));
//                                    } else {
//                                        fileReceiver.setLogFormat(receiverConfigurationPanel.getModel().getLogFormat());
//                                    }
//                                    fileReceiver.setTimestampFormat(receiverConfigurationPanel.getModel().getLogFormatTimestampFormat());
////                                    fileReceiver.setThreshold(Level.TRACE);
////
////                                    pluginRegistry.addPlugin(fileReceiver);
//                                    fileReceiver.activateOptions();
//                                    receiversPanel.updateReceiverTreeInDispatchThread();
//                                }
//                            } catch (Exception e2) {
//                                logger.error(
//                                    "Error creating Receiver", e2);
//                                statusBar.setMessage(
//                                    "An error occurred creating your Receiver");
//                            }
//                        }
//                        if (configURL == null && receiverConfigurationPanel.isDontWarnMeAgain()) {
//                            //use the saved config file as the config URL if defined
//                            if (receiverConfigurationPanel.getModel().getSaveConfigFile() != null) {
//                                try {
//                                    configURL = receiverConfigurationPanel.getModel().getSaveConfigFile().toURI().toURL();
//                                } catch (MalformedURLException e1) {
//                                    e1.printStackTrace();
//                                }
//                            } else {
//                                //no saved config defined but don't warn me is checked - use default config
//                                configURL = receiverConfigurationPanel.getModel().getDefaultConfigFileURL();
//                            }
//                        }
//                        if (configURL != null) {
////                            MessageCenter.getInstance().getLogger().debug(
////                                "Initialiazing Log4j with " + configURL.toExternalForm());
//                            final URL finalURL = configURL;
//                            new Thread(
//                                () -> {
//                                    if (receiverConfigurationPanel.isDontWarnMeAgain()) {
//                                        applicationPreferenceModel.setConfigurationURL(finalURL.toExternalForm());
//                                    } else {
//                                        try {
//                                            if (new File(finalURL.toURI()).exists()) {
//                                                loadConfigurationUsingPluginClassLoader(finalURL);
//                                            }
//                                        } catch (URISyntaxException e12) {
//                                            //ignore
//                                        }
//                                    }
//
//                                    receiversPanel.updateReceiverTreeInDispatchThread();
//                                }).start();
//                        }
//                        File saveConfigFile = receiverConfigurationPanel.getModel().getSaveConfigFile();
//                        if (saveConfigFile != null) {
//                            saveReceiversToFile(saveConfigFile);
//                        }
//                    });
//
//                receiverConfigurationPanel.setDialog(dialog);
//                dialog.getContentPane().add(receiverConfigurationPanel);
//
//                dialog.pack();
//
//                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//                dialog.setLocation(
//                    (screenSize.width / 2) - (dialog.getWidth() / 2),
//                    (screenSize.height / 2) - (dialog.getHeight() / 2));
//
//                dialog.setVisible(true);
//            });
    }

    /**
     * Exits the application, ensuring Settings are saved.
     */
    public boolean exit() {
        for(ChainsawReceiver rx : receivers){
            settingsManager.saveSettingsForReceiver(rx);
        }

        settingsManager.saveAllSettings();

        return shutdown();
    }

    void addWelcomePanel() {
        getTabbedPane().insertTab(
            ChainsawTabbedPane.WELCOME_TAB, new ImageIcon(ChainsawIcons.ABOUT), welcomePanel,
            "Welcome/Help", 0);
        getTabbedPane().setSelectedComponent(welcomePanel);
        getPanelMap().put(ChainsawTabbedPane.WELCOME_TAB, welcomePanel);
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
        // applicationPreferenceModelPanel.updateModel();
        preferencesFrame.setVisible(true);
    }

    public void loadReceiver() {
        Runnable r = () -> {
            JFileChooser jfc = new JFileChooser(SettingsManager.getSettingsDirectory());
            int returnVal = jfc.showOpenDialog(this);
            if(returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }

            logger.debug("Load file {}", jfc.getSelectedFile());

            // Create the receiver
            String fileToLoad = jfc.getSelectedFile().getName();
            String receiverName = fileToLoad.split( "-" )[0];
            AbstractConfiguration config = settingsManager.getSettingsForReceiverTab(receiverName);
            String typeToLoad = config.getString("receiver.type");
            ServiceLoader<ChainsawReceiverFactory> sl = ServiceLoader.load(ChainsawReceiverFactory.class);

            for( ChainsawReceiverFactory crFactory : sl ){
                if(crFactory.getReceiverName().equals(typeToLoad)){
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
        Set<Map.Entry<String, Component>> panelSet = getPanelMap().entrySet();

        for (Map.Entry<String, Component> panel : panelSet) {
            Component component = panel.getValue();
            boolean value = !(component instanceof LogPanel) || ((DockablePanel) panel.getValue()).isDocked();
            result.put(panel.getKey(), value);
        }

        return result;
    }

    void displayPanel(String panelName, boolean display) {
        Component p = getPanelMap().get(panelName);

        int index = getTabbedPane().indexOfTab(panelName);

        if ((index == -1) && display) {
            getTabbedPane().addTab(panelName, p);
        }

        if ((index > -1) && !display) {
            getTabbedPane().removeTabAt(index);
        }
    }


    /**
     * Shutsdown by ensuring the Appender gets a chance to close.
     */
    public boolean shutdown() {
        boolean confirmExit = configuration.getBoolean("confirmExit", true);
        if (confirmExit) {
            if (
                JOptionPane.showConfirmDialog(
                    LogUI.this, "Are you sure you want to exit Chainsaw?",
                    "Confirm Exit", JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE) != JOptionPane.YES_OPTION) {
                return false;
            }

        }

        final JWindow progressWindow = new JWindow();
        final ProgressPanel panel = new ProgressPanel(1, 3, "Shutting down");
        progressWindow.getContentPane().add(panel);
        progressWindow.pack();

        Point p = new Point(getLocation());
        p.move((int) getSize().getWidth() >> 1, (int) getSize().getHeight() >> 1);
        progressWindow.setLocation(p);
        progressWindow.setVisible(true);

        Runnable runnable =
            () -> {
                try {
                    int progress = 1;
                    final int delay = 25;

                    panel.setProgress(progress++);

                    Thread.sleep(delay);

                    for( ChainsawReceiver rx : receivers){
                        rx.shutdown();
                    }
                    panel.setProgress(progress++);

                    Thread.sleep(delay);

                    panel.setProgress(progress++);
                    Thread.sleep(delay);
                } catch (Exception e) {
                    logger.error(e,e);
                }

                fireShutdownEvent();
                performShutdownAction();
                progressWindow.setVisible(false);
            };

        if (OSXIntegration.IS_OSX) {
            /*
             * or OSX we do it in the current thread because otherwise returning
             * will exit the process before it's had a chance to save things
             *
             */
            runnable.run();
        } else {
            new Thread(runnable).start();
        }
        return true;
    }

    /**
     * Ensures all the registered ShutdownListeners are notified.
     */
    private void fireShutdownEvent() {
        ShutdownListener[] listeners =
            shutdownListenerList.getListeners(
                ShutdownListener.class);

        for (ShutdownListener listener : listeners) {
            listener.shuttingDown();
        }
    }

    /**
     * Configures LogUI's with an action to execute when the user requests to
     * exit the application, the default action is to exit the VM. This Action is
     * called AFTER all the ShutdownListeners have been notified
     *
     * @param shutdownAction
     */
    public final void setShutdownAction(Action shutdownAction) {
        this.shutdownAction = shutdownAction;
    }

    /**
     * Using the current thread, calls the registed Shutdown action's
     * actionPerformed(...) method.
     */
    private void performShutdownAction() {
        logger.debug(
            "Calling the shutdown Action. Goodbye!");

        shutdownAction.actionPerformed(
            new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutting Down"));
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
    public Map<String, Component> getPanelMap() {
        return panelMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param tbms          DOCUMENT ME!
     */
    public void setToolBarAndMenus(ChainsawToolBarAndMenus tbms) {
        this.chainsawToolBarAndMenus = tbms;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public ChainsawToolBarAndMenus getToolBarAndMenus() {
        return chainsawToolBarAndMenus;
    }

    /**
     * DOCUMENT ME!
     *
     * @param tabbedPane DOCUMENT ME!
     */
    public void setTabbedPane(ChainsawTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public ChainsawTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * @return Returns the applicationPreferenceModel.
     */
    public final ApplicationPreferenceModel getApplicationPreferenceModel() {
        return applicationPreferenceModel;
    }


    private void buildLogPanel(
        boolean customExpression, final String ident, final List<ChainsawLoggingEvent> events, final ChainsawReceiver rx)
        throws IllegalArgumentException {
        final LogPanel thisPanel = new LogPanel(settingsManager, getStatusBar(), ident, cyclicBufferSize, allColorizers, applicationPreferenceModel, globalRuleColorizer);

        if( !customExpression && rx != null ){
            thisPanel.setReceiver(rx);
        }

        /*
         * Now add the panel as a batch listener so it can handle it's own
         * batchs
         */
        if (customExpression) {
//            handler.addCustomEventBatchListener(ident, thisPanel);
        } else {
            identifierPanels.add(thisPanel);
//            handler.addEventBatchListener(thisPanel);
        }

        TabIconHandler iconHandler = new TabIconHandler(ident);
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
                    getPanelMap().put(logPanel.getIdentifier(), logPanel);
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
        getPanelMap().put(ident, thisPanel);

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

            buildLogPanel(true, ident, list, null);
        } catch (IllegalArgumentException iae) {
            statusBar.setMessage(
                "Unable to add tab using expression: " + ident + ", reason: "
                    + iae.getMessage());
        }
    }

    private class TabIconHandler implements EventCountListener, ChangeListener {
        //the tabIconHandler is associated with a new tab, and a new tab always
        //shows the 'new events' icon
        private boolean newEvents = true;
        private boolean seenEvents = false;
        private final String ident;
        ImageIcon NEW_EVENTS = new ImageIcon(ChainsawIcons.ANIM_RADIO_TOWER);
        ImageIcon HAS_EVENTS = new ImageIcon(ChainsawIcons.INFO);
        Icon SELECTED = LineIconFactory.createBlankIcon();

        public TabIconHandler(String identifier) {
            ident = identifier;

            new Thread(
                () -> {
                    while (true) {
                        //if this tab is active, remove the icon
                        //don't process undocked tabs
                        if (getTabbedPane().indexOfTab(ident) > -1 &&
                            getTabbedPane().getSelectedIndex() == getTabbedPane()
                                .indexOfTab(ident)) {
                            getTabbedPane().setIconAt(
                                getTabbedPane().indexOfTab(ident), SELECTED);
                            newEvents = false;
                            seenEvents = true;
                        } else if (getTabbedPane().indexOfTab(ident) > -1) {
                            if (newEvents) {
                                getTabbedPane().setIconAt(
                                    getTabbedPane().indexOfTab(ident), NEW_EVENTS);
                                newEvents = false;
                                seenEvents = false;
                            } else if (!seenEvents) {
                                getTabbedPane().setIconAt(
                                    getTabbedPane().indexOfTab(ident), HAS_EVENTS);
                            }
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                        }
                    }
                }).start();
        }

        /**
         * DOCUMENT ME!
         *
         * @param currentCount DOCUMENT ME!
         * @param totalCount   DOCUMENT ME!
         */
        public void eventCountChanged(int currentCount, int totalCount) {
            newEvents = true;
        }

        public void stateChanged(ChangeEvent event) {
            if (
                getTabbedPane().indexOfTab(ident) > -1 && getTabbedPane().indexOfTab(ident) == getTabbedPane().getSelectedIndex()) {
                getTabbedPane().setIconAt(getTabbedPane().indexOfTab(ident), SELECTED);
            }
        }
    }

    public void addReceiver(ChainsawReceiver rx){
        receivers.add(rx);
        List<ChainsawLoggingEvent> list = new ArrayList<>();
        buildLogPanel(false, rx.getName(), list, rx);
        
        for(ReceiverEventListener listen : receiverListeners){
            listen.receiverAdded(rx);
        }
    }
    
    public void removeReceiver(ChainsawReceiver rx){
        if( !receivers.remove(rx) ){
            return;
        }
        
        for(ReceiverEventListener listen : receiverListeners){
            listen.receiverRemoved(rx);
        }
    }
    
    public void addReceiverEventListener(ReceiverEventListener listener){
        receiverListeners.add(listener);
    }
    
    public void removeReceiverEventListener(ReceiverEventListener listener){
        receiverListeners.remove(listener);
    }
    
    public List<ChainsawReceiver> getAllReceivers(){
        return receivers;
    }


}
