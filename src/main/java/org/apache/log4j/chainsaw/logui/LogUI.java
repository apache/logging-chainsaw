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
package org.apache.log4j.chainsaw.logui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.log4j.chainsaw.*;
import org.apache.log4j.chainsaw.components.about.ChainsawAbout;
import org.apache.log4j.chainsaw.components.elements.SmallButton;
import org.apache.log4j.chainsaw.components.logpanel.LogPanel;
import org.apache.log4j.chainsaw.components.tabbedpane.ChainsawTabbedPane;
import org.apache.log4j.chainsaw.components.tutorial.TutorialFrame;
import org.apache.log4j.chainsaw.components.welcome.WelcomePanel;
import org.apache.log4j.chainsaw.dnd.FileDnDTarget;
import org.apache.log4j.chainsaw.help.HelpManager;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverFactory;
import org.apache.log4j.chainsaw.zeroconf.ZeroConfPlugin;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    /* Panels / Views */
    private final JFrame preferencesFrame = new JFrame();
    private WelcomePanel welcomePanel;
    private ChainsawTabbedPane tabbedPane;
    private ChainsawAbout aboutBox;
    public TutorialFrame tutorialFrame;
    private final List<LogPanel> identifierPanels = new ArrayList<>();

    private JToolBar toolbar;
    private ChainsawToolBarAndMenus chainsawToolBarAndMenus;
    private ChainsawStatusBar statusBar;
    private ApplicationPreferenceModelPanel applicationPreferenceModelPanel;
    private final Map<String, Component> panelMap = new HashMap<>();
    public ChainsawAppender chainsawAppender;
    private SettingsManager settingsManager;
    private final List<ChainsawReceiver> receivers = new ArrayList<>();
    private final List<ReceiverEventListener> receiverListeners = new ArrayList<>();
    private final ZeroConfPlugin zeroConf = new ZeroConfPlugin();

    /**
     * Clients can register a ShutdownListener to be notified when the user has
     * requested Chainsaw to exit.
     */
    private final EventListenerList shutdownListenerList = new EventListenerList();

    private final AbstractConfiguration configuration;

    private final ShutdownManager shutdownManager;
    private LogUIPanelBuilder logUIPanelBuilder;
    private final ApplicationPreferenceModel applicationPreferenceModel;

    /**
     * Constructor which builds up all the visual elements of the frame including
     * the Menu bar
     */
    public LogUI(SettingsManager settingsManager) {
        super("Chainsaw");

        this.settingsManager = settingsManager;
        this.configuration = settingsManager.getGlobalConfiguration();
        applicationPreferenceModel = new ApplicationPreferenceModel(settingsManager.getGlobalConfiguration());

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        if (ChainsawIcons.WINDOW_ICON != null) {
            setIconImage(new ImageIcon(ChainsawIcons.WINDOW_ICON).getImage());
        }

        shutdownManager = new ShutdownManager(this, configuration, receivers, shutdownListenerList);
    }

    /**
     * Initialises the menu's and toolbars, but does not actually create any of
     * the main panel components.
     */
    private void initGUI() {
        setupHelpSystem();
        statusBar = new ChainsawStatusBar(this, applicationPreferenceModel);

        setStatusBarVisible(applicationPreferenceModel.isStatusBarVisible());

        chainsawToolBarAndMenus = new ChainsawToolBarAndMenus(this, applicationPreferenceModel);
        toolbar = chainsawToolBarAndMenus.getToolbar();
        setJMenuBar(chainsawToolBarAndMenus.getMenubar());

        tabbedPane = new ChainsawTabbedPane();

        createDragAndDrop();

        applicationPreferenceModelPanel =
                new ApplicationPreferenceModelPanel(settingsManager, applicationPreferenceModel);

        applicationPreferenceModelPanel.setOkCancelActionListener(e -> preferencesFrame.setVisible(false));

        KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        Action closeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                preferencesFrame.setVisible(false);
            }
        };
        preferencesFrame
                .getRootPane()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(escape, "ESCAPE");
        preferencesFrame.getRootPane().getActionMap().put("ESCAPE", closeAction);

        logUIPanelBuilder = new LogUIPanelBuilder(
                tabbedPane,
                identifierPanels,
                chainsawToolBarAndMenus,
                panelMap,
                settingsManager,
                applicationPreferenceModel,
                statusBar,
                zeroConf);

        OSXIntegration.init(this);
    }

    /**
     * This adds Drag & Drop capability to Chainsaw
     */
    private void createDragAndDrop() {
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
    }

    /**
     * Initialises the Help system and the WelcomePanel
     */
    private void setupHelpSystem() {
        welcomePanel = new WelcomePanel();

        tutorialFrame = new TutorialFrame(receivers, receiverListeners, this, statusBar);

        JToolBar tb = welcomePanel.getToolbar();
        JButton help = new SmallButton.Builder()
                .iconUrl(ChainsawIcons.HELP)
                .action(tutorialFrame::setupTutorial)
                .shortDescription("Tutorial")
                .build();

        tb.add(help);

        tb.addSeparator();

        JButton exampleButton = new SmallButton.Builder()
                .iconUrl(ChainsawIcons.HELP)
                .action(() -> HelpManager.getInstance().setHelpURL(ChainsawConstants.EXAMPLE_CONFIG_URL))
                .name("View example Receiver configuration")
                .shortDescription("Displays an example Log4j configuration file with several Receivers defined.")
                .build();

        tb.add(exampleButton);
        tb.add(Box.createHorizontalGlue());

        /*
         * Setup a listener on the HelpURL property and automatically change the WelcomePages URL
         * to it.
         */
        HelpManager.getInstance().addPropertyChangeListener("helpURL", evt -> {
            URL newURL = (URL) evt.getNewValue();

            if (newURL != null) {
                welcomePanel.setURL(newURL);
                ensureWelcomePanelVisible();
            }
        });
    }

    /**
     * ensure that the Welcome Panel is made visible
     */
    private void ensureWelcomePanelVisible() {
        if (!tabbedPane.containsWelcomePanel()) {
            addWelcomePanel();
        }

        tabbedPane.setSelectedComponent(welcomePanel);
    }

    /**
     * Given the load event, configures the size/location of the main window etc. etc.
     */
    private void loadSettings() {
        AbstractConfiguration config = settingsManager.getGlobalConfiguration();
        setLocation(config.getInt(LogUI.MAIN_WINDOW_X, 0), config.getInt(LogUI.MAIN_WINDOW_Y, 0));

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
    }

    /**
     * Activates itself as a viewer by configuring Size, and location of itself,
     * and configures the default Tabbed Pane elements with the correct layout,
     * table columns, and sets itself viewable.
     */
    public void activateViewer() {
        initGUI();
        loadSettings();

        JPanel panePanel = new JPanel();
        panePanel.setLayout(new BorderLayout(2, 2));

        getContentPane().setLayout(new BorderLayout());

        tabbedPane.addChangeListener(chainsawToolBarAndMenus);
        tabbedPane.addChangeListener(e -> {
            LogPanel thisLogPanel = getCurrentLogPanel();
            if (thisLogPanel != null) {
                thisLogPanel.updateStatusBar();
            }
        });

        LogUiKeyStrokeCreator.createKeyStrokeRight(tabbedPane);
        LogUiKeyStrokeCreator.createKeyStrokeLeft(tabbedPane);
        LogUiKeyStrokeCreator.createKeyStrokeGotoLine(tabbedPane, this);

        MouseAdapter mouseAdapter = createMouseAdapter();
        tabbedPane.addMouseListener(mouseAdapter);

        panePanel.add(tabbedPane);
        addWelcomePanel();

        getContentPane().add(toolbar, BorderLayout.NORTH);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        createReceiversPanel(panePanel);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                exit();
            }
        });
        preferencesFrame.setTitle("'Application-wide Preferences");
        preferencesFrame.setIconImage(((ImageIcon) ChainsawIcons.ICON_PREFERENCES).getImage());
        preferencesFrame.getContentPane().add(applicationPreferenceModelPanel);

        preferencesFrame.setSize(750, 520);

        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        preferencesFrame.setLocation(new Point(
                (screenDimension.width / 2) - (preferencesFrame.getSize().width / 2),
                (screenDimension.height / 2) - (preferencesFrame.getSize().height / 2)));

        pack();

        JPopupMenu tabPopup = new JPopupMenu();

        Action hideCurrentTabAction = ceateHideCurrentTabAction();
        Action hideOtherTabsAction = createHideOtherTabsAction();
        Action showHiddenTabsAction = createShowHiddenTabsAction();

        tabPopup.add(hideCurrentTabAction);
        tabPopup.add(hideOtherTabsAction);
        tabPopup.addSeparator();
        tabPopup.add(showHiddenTabsAction);

        final PopupListener tabPopupListener = new PopupListener(tabPopup);
        tabbedPane.addMouseListener(tabPopupListener);

        int tooltipDisplayMillis = configuration.getInt("tooltipDisplayMillis", 4000);
        ToolTipManager.sharedInstance().setDismissDelay(tooltipDisplayMillis);

        boolean showToolbar = configuration.getBoolean("toolbar", true);
        toolbar.setVisible(showToolbar);

        setVisible(true);

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

    private void createReceiversPanel(JPanel panePanel) {
        LogUiReceiversPanel logUiReceiversPanel = new LogUiReceiversPanel(
                settingsManager, applicationPreferenceModel, receivers, this, statusBar, panePanel);
        getContentPane().add(logUiReceiversPanel.getMainReceiverSplitPane(), BorderLayout.CENTER);
    }

    public void createCustomExpressionLogPanel(String ident) {
        // collect events matching the rule from all of the tabs
        try {
            List<ChainsawLoggingEvent> list = new ArrayList<>();
            Rule rule = ExpressionRule.getRule(ident);

            for (LogPanel identifierPanel : identifierPanels) {
                for (LoggingEventWrapper e : identifierPanel.getMatchingEvents(rule)) {
                    list.add(e.getLoggingEvent());
                }
            }

            logUIPanelBuilder.buildLogPanel(true, ident, null);
        } catch (IllegalArgumentException iae) {
            statusBar.setMessage("Unable to add tab using expression: " + ident + ", reason: " + iae.getMessage());
        }
    }

    public void buildChainsawLogPanel() {
        if (chainsawAppender != null) {
            logUIPanelBuilder.buildLogPanel(false, "Chainsaw", chainsawAppender.getReceiver());
        } else {
            logUIPanelBuilder.buildLogPanel(false, "Chainsaw", null);
        }

    }

    public void addWelcomePanel() {
        tabbedPane.insertTab(
                ChainsawTabbedPane.WELCOME_TAB, new ImageIcon(ChainsawIcons.ABOUT), welcomePanel, "Welcome/Help", 0);
        tabbedPane.setSelectedComponent(welcomePanel);
        panelMap.put(ChainsawTabbedPane.WELCOME_TAB, welcomePanel);
    }

    public void removeWelcomePanel() {
        EventQueue.invokeLater(() -> {
            if (tabbedPane.containsWelcomePanel()) {
                tabbedPane.remove(tabbedPane.getComponentAt(tabbedPane.indexOfTab(ChainsawTabbedPane.WELCOME_TAB)));
            }
        });
    }

    public void showAboutBox() {
        if (aboutBox == null) {
            aboutBox = new ChainsawAbout(this);
        } else {
            aboutBox.calculateDimentions();
        }

        aboutBox.setVisible(true);
    }

    void displayPanel(String panelName, boolean display) {
        Component p = panelMap.get(panelName);

        int index = tabbedPane.indexOfTab(panelName);

        if ((index == -1) && display) {
            tabbedPane.addTab(panelName, p);
        }

        if ((index > -1) && !display) {
            tabbedPane.removeTabAt(index);
        }
    }

    public void showApplicationPreferences() {
        preferencesFrame.setVisible(true);
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
     * Exits the application, ensuring Settings are saved.
     */
    public boolean exit() {
        for (ChainsawReceiver rx : receivers) {
            settingsManager.saveSettingsForReceiver(rx);
        }

        for (Component comp : panelMap.values()) {
            if (comp instanceof LogPanel) {
                ((LogPanel) comp).saveSettings();
            }
        }

        settingsManager.saveAllSettings();
        return shutdownManager.shutdown();
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

    /**
     * Returns the currently selected LogPanel, if there is one, otherwise null
     *
     * @return current log panel
     */
    public LogPanel getCurrentLogPanel() {
        Component selectedTab = tabbedPane.getSelectedComponent();
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
        SwingUtilities.invokeLater(() -> statusBar.setVisible(visible));
    }

    public boolean isStatusBarVisible() {
        return statusBar.isVisible();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getActiveTabName() {
        int index = tabbedPane.getSelectedIndex();

        if (index == -1) {
            return null;
        } else {
            return tabbedPane.getTitleAt(index);
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

    public void addReceiver(ChainsawReceiver rx) {
        receivers.add(rx);
        logUIPanelBuilder.buildLogPanel(false, rx.getName(), rx);

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

    /** @deprecated */
    public ChainsawTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public ChainsawStatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * We listen for double clicks, and auto-undock currently selected Tab if
     * the mouse event location matches the currently selected tab
     */
    private MouseAdapter createMouseAdapter() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                if ((e.getClickCount() > 1) && ((e.getModifiers() & InputEvent.BUTTON1_MASK) > 0)) {
                    int tabIndex = tabbedPane.getSelectedIndex();

                    if ((tabIndex != -1) && (tabIndex == tabbedPane.getSelectedIndex())) {
                        LogPanel logPanel = getCurrentLogPanel();

                        if (logPanel != null) {
                            logPanel.undock();
                        }
                    }
                }
            }
        };
    }

    private Action ceateHideCurrentTabAction() {
        return new AbstractAction("Hide") {
            public void actionPerformed(ActionEvent e) {
                Component selectedComp = tabbedPane.getSelectedComponent();
                if (selectedComp instanceof LogPanel) {
                    displayPanel(getCurrentLogPanel().getIdentifier(), false);
                    chainsawToolBarAndMenus.stateChange();
                } else {
                    tabbedPane.remove(selectedComp);
                }
            }
        };
    }

    private Action createHideOtherTabsAction() {
        return new AbstractAction("Hide Others") {
            public void actionPerformed(ActionEvent e) {
                Component selectedComp = tabbedPane.getSelectedComponent();
                String currentName;
                if (selectedComp instanceof LogPanel) {
                    currentName = getCurrentLogPanel().getIdentifier();
                } else if (selectedComp instanceof WelcomePanel) {
                    currentName = ChainsawTabbedPane.WELCOME_TAB;
                } else {
                    currentName = ChainsawTabbedPane.ZEROCONF;
                }

                int count = tabbedPane.getTabCount();
                int index = 0;

                for (int i = 0; i < count; i++) {
                    String name = tabbedPane.getTitleAt(index);

                    if (panelMap.containsKey(name) && !name.equals(currentName)) {
                        displayPanel(name, false);
                        chainsawToolBarAndMenus.stateChange();
                    } else {
                        index++;
                    }
                }
            }
        };
    }

    private Action createShowHiddenTabsAction() {
        return new AbstractAction("Show All Hidden") {
            public void actionPerformed(ActionEvent e) {

                for (Map.Entry<String, Boolean> entry : getPanels().entrySet()) {
                    Boolean docked = entry.getValue();
                    if (Boolean.TRUE.equals(docked)) {
                        String identifier = entry.getKey();
                        int count = tabbedPane.getTabCount();
                        boolean found = false;

                        for (int i = 0; i < count; i++) {
                            String name = tabbedPane.getTitleAt(i);

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
    }
}
