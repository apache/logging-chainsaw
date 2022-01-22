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

import org.apache.log4j.*;
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.dnd.FileDnDTarget;
import org.apache.log4j.chainsaw.help.HelpManager;
import org.apache.log4j.chainsaw.help.Tutorial;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LineIconFactory;
import org.apache.log4j.chainsaw.messages.MessageCenter;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.plugins.PluginClassLoaderFactory;
import org.apache.log4j.chainsaw.prefs.*;
import org.apache.log4j.chainsaw.receivers.ReceiversHelper;
import org.apache.log4j.chainsaw.receivers.ReceiversPanel;
import org.apache.log4j.chainsaw.vfs.VFSLogFilePatternReceiver;
import org.apache.log4j.net.SocketNodeEventListener;
import org.apache.log4j.plugins.*;
import org.apache.log4j.rewrite.PropertyRewritePolicy;
import org.apache.log4j.rewrite.RewriteAppender;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.Decoder;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggerRepositoryEx;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.xml.XMLDecoder;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.util.*;
import java.util.List;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.logging.log4j.core.LoggerContext;


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
public class LogUI extends JFrame implements ChainsawViewer, SettingsListener {
    private static final String MAIN_WINDOW_HEIGHT = "main.window.height";
    private static final String MAIN_WINDOW_WIDTH = "main.window.width";
    private static final String MAIN_WINDOW_Y = "main.window.y";
    private static final String MAIN_WINDOW_X = "main.window.x";
    private static ChainsawSplash splash;
    private static final double DEFAULT_MAIN_RECEIVER_SPLIT_LOCATION = 0.85d;
    private final JFrame preferencesFrame = new JFrame();
    private boolean noReceiversDefined;
    private ReceiversPanel receiversPanel;
    private ChainsawTabbedPane tabbedPane;
    private JToolBar toolbar;
    private ChainsawStatusBar statusBar;
    private ApplicationPreferenceModel applicationPreferenceModel;
    private ApplicationPreferenceModelPanel applicationPreferenceModelPanel;
    private final Map tableModelMap = new HashMap();
    private final Map tableMap = new HashMap();
    private final List<String> filterableColumns = new ArrayList<>();
    private final Map<String, Component> panelMap = new HashMap<>();
    private ChainsawAppender chainsawAppender;
    private ChainsawToolBarAndMenus tbms;
    private ChainsawAbout aboutBox;
    private final SettingsManager sm = SettingsManager.getInstance();
    private final JFrame tutorialFrame = new JFrame("Chainsaw Tutorial");
    private JSplitPane mainReceiverSplitPane;
    private double lastMainReceiverSplitLocation = DEFAULT_MAIN_RECEIVER_SPLIT_LOCATION;
    private final List<LogPanel> identifierPanels = new ArrayList<>();
    private int dividerSize;
    private int cyclicBufferSize;
    private static String configurationURLAppArg;
    private List<ChainsawReceiver> m_receivers = new ArrayList<>();
    private List<ReceiverEventListener> m_receiverListeners = new ArrayList<>();

    private static org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();

    /**
     * Set to true, if and only if the GUI has completed it's full
     * initialization. Any logging events that come in must wait until this is
     * true, and if it is false, should wait on the initializationLock object
     * until notified.
     */
    private boolean isGUIFullyInitialized = false;
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

    private static final Object repositorySelectorGuard = new Object();
    private static final LoggerRepositoryExImpl repositoryExImpl = new LoggerRepositoryExImpl(LogManager.getLoggerRepository());

    private PluginRegistry pluginRegistry;
    //map of tab names to rulecolorizers
    private Map<String, RuleColorizer> allColorizers = new HashMap<>();
    private ReceiverConfigurationPanel receiverConfigurationPanel = new ReceiverConfigurationPanel();

    /**
     * Constructor which builds up all the visual elements of the frame including
     * the Menu bar
     */
    public LogUI() {
        super("Chainsaw");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        if (ChainsawIcons.WINDOW_ICON != null) {
            setIconImage(new ImageIcon(ChainsawIcons.WINDOW_ICON).getImage());
        }
    }

    private static final void showSplash(Frame owner) {
        splash = new ChainsawSplash(owner);
        SwingHelper.centerOnScreen(splash);
        splash.setVisible(true);
    }

    private static final void removeSplash() {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
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
     * Removes the registered ShutdownListener so that the listener will not be
     * notified on a shutdown.
     *
     * @param l
     */
    public void removeShutdownListener(ShutdownListener l) {
        shutdownListenerList.remove(ShutdownListener.class, l);
    }

    /**
     * Starts Chainsaw by attaching a new instance to the Log4J main root Logger
     * via a ChainsawAppender, and activates itself
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            configurationURLAppArg = args[0];
        }

        if (OSXIntegration.IS_OSX) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        LogManager.setRepositorySelector(() -> repositoryExImpl, repositorySelectorGuard);

        final ApplicationPreferenceModel model = new ApplicationPreferenceModel();

        SettingsManager.getInstance().configure(new ApplicationPreferenceModelSaver(model));
        //if a configuration URL param was provided, set the configuration URL field to null
        if (configurationURLAppArg != null) {
            model.setBypassConfigurationURL(configurationURLAppArg);
        }

        EventQueue.invokeLater(() -> {
            String lookAndFeelClassName = model.getLookAndFeelClassName();
            if (lookAndFeelClassName == null || lookAndFeelClassName.trim().equals("")) {
                String osName = System.getProperty("os.name");
                if (osName.toLowerCase(Locale.ENGLISH).startsWith("mac")) {
                    //no need to assign look and feel
                } else if (osName.toLowerCase(Locale.ENGLISH).startsWith("windows")) {
                    lookAndFeelClassName = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
                    model.setLookAndFeelClassName(lookAndFeelClassName);
                } else if (osName.toLowerCase(Locale.ENGLISH).startsWith("linux")) {
                    lookAndFeelClassName = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
                    model.setLookAndFeelClassName(lookAndFeelClassName);
                }
            }

            if (lookAndFeelClassName != null && !(lookAndFeelClassName.trim().equals(""))) {
                loadLookAndFeelUsingPluginClassLoader(lookAndFeelClassName);
            }
            createChainsawGUI(model, null);
        });
    }

    /**
     * Creates, activates, and then shows the Chainsaw GUI, optionally showing
     * the splash screen, and using the passed shutdown action when the user
     * requests to exit the application (if null, then Chainsaw will exit the vm)
     *
     * @param model
     * @param newShutdownAction DOCUMENT ME!
     */
    public static void createChainsawGUI(
        ApplicationPreferenceModel model, Action newShutdownAction) {

        if (model.isOkToRemoveSecurityManager()) {
            MessageCenter
                .getInstance()
                .addMessage(
                    "User has authorised removal of Java Security Manager via preferences");
            System.setSecurityManager(null);
            // this SHOULD set the Policy/Permission stuff for any
            // code loaded from our custom classloader.  
            // crossing fingers...
            Policy.setPolicy(new Policy() {

                public void refresh() {
                }

                public PermissionCollection getPermissions(CodeSource codesource) {
                    Permissions perms = new Permissions();
                    perms.add(new AllPermission());
                    return (perms);
                }
            });
        }

        final LogUI logUI = new LogUI();
        logUI.applicationPreferenceModel = model;

        if (model.isShowSplash()) {
            showSplash(logUI);
        }
        logUI.cyclicBufferSize = model.getCyclicBufferSize();
        logUI.pluginRegistry = repositoryExImpl.getPluginRegistry();

        final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        logUI.chainsawAppender = ctx.getConfiguration().getAppender("chainsaw");

        /**
         * TODO until we work out how JoranConfigurator might be able to have
         * configurable class loader, if at all.  For now we temporarily replace the
         * TCCL so that Plugins that need access to resources in
         * the Plugins directory can find them (this is particularly
         * important for the Web start version of Chainsaw
         */
        //configuration initialized here

        //set hostname, application and group properties which will cause Chainsaw and other apache-generated
        //logging events to route (by default) to a tab named 'chainsaw-log'
        PropertyRewritePolicy policy = new PropertyRewritePolicy();
        policy.setProperties("hostname=chainsaw,application=log,group=chainsaw");

        RewriteAppender rewriteAppender = new RewriteAppender();
        rewriteAppender.setRewritePolicy(policy);

        Enumeration appenders = Logger.getLogger("org.apache").getAllAppenders();
        if (!appenders.hasMoreElements()) {
            appenders = Logger.getRootLogger().getAllAppenders();
        }
        while (appenders.hasMoreElements()) {
            Appender nextAppender = (Appender) appenders.nextElement();
            rewriteAppender.addAppender(nextAppender);
        }
        Logger.getLogger("org.apache").removeAllAppenders();
        Logger.getLogger("org.apache").addAppender(rewriteAppender);
        Logger.getLogger("org.apache").setAdditivity(false);

        //commons-vfs uses httpclient for http filesystem support, route this to the chainsaw-log tab as well
        appenders = Logger.getLogger("httpclient").getAllAppenders();
        if (!appenders.hasMoreElements()) {
            appenders = Logger.getRootLogger().getAllAppenders();
        }
        while (appenders.hasMoreElements()) {
            Appender nextAppender = (Appender) appenders.nextElement();
            rewriteAppender.addAppender(nextAppender);
        }
        Logger.getLogger("httpclient").removeAllAppenders();
        Logger.getLogger("httpclient").addAppender(rewriteAppender);
        Logger.getLogger("httpclient").setAdditivity(false);

        //set the commons.vfs.cache logger to info, since it can contain password information
        Logger.getLogger("org.apache.commons.vfs.cache").setLevel(Level.INFO);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            logger.error("Uncaught exception in thread " + t, e);
        });

        String config = configurationURLAppArg;
        if (config != null) {
            logger.info("Command-line configuration arg provided (overriding auto-configuration URL) - using: " + config);
        } else {
            config = model.getConfigurationURL();
        }

        if (config != null && (!config.trim().equals(""))) {
            config = config.trim();
            try {
                URL configURL = new URL(config);
                logger.info("Using '" + config + "' for auto-configuration");
                logUI.loadConfigurationUsingPluginClassLoader(configURL);
            } catch (MalformedURLException e) {
                logger.error("Initial configuration - failed to convert config string to url", e);
            } catch (IOException e) {
                logger.error("Unable to access auto-configuration URL: " + config);
            }
        }

        //register a listener to load the configuration when it changes (avoid having to restart Chainsaw when applying a new configuration)
        //this doesn't remove receivers from receivers panel, it just triggers DOMConfigurator.configure.
        model.addPropertyChangeListener("configurationURL", evt -> {
            String newConfiguration = evt.getNewValue().toString();
            if (newConfiguration != null && !(newConfiguration.trim().equals(""))) {
                newConfiguration = newConfiguration.trim();
                try {
                    logger.info("loading updated configuration: " + newConfiguration);
                    URL newConfigurationURL = new URL(newConfiguration);
                    File file = new File(newConfigurationURL.toURI());
                    if (file.exists()) {
                        logUI.loadConfigurationUsingPluginClassLoader(newConfigurationURL);
                    } else {
                        logger.info("Updated configuration but file does not exist");
                    }
                } catch (MalformedURLException | URISyntaxException e) {
                    logger.error("Updated configuration - failed to convert config string to URL", e);
                }
            }
        });

        LogManager.getRootLogger().setLevel(Level.TRACE);
        EventQueue.invokeLater(logUI::activateViewer);
        EventQueue.invokeLater(logUI::buildChainsawLogPanel);

        logger.info("SecurityManager is now: " + System.getSecurityManager());

        if (newShutdownAction != null) {
            logUI.setShutdownAction(newShutdownAction);
        } else {
            logUI.setShutdownAction(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        System.exit(0);
                    }
                });
        }
    }

    /**
     * Allow Chainsaw v2 to be ran in-process (configured as a ChainsawAppender)
     * NOTE: Closing Chainsaw will NOT stop the application generating the events.
     *
     * @param appender
     */
    public void activateViewer(ChainsawAppender appender) {

        if (OSXIntegration.IS_OSX) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        LogManager.setRepositorySelector(() -> repositoryExImpl, repositorySelectorGuard);

        //if Chainsaw is launched as an appender, ensure the root logger level is TRACE
        LogManager.getRootLogger().setLevel(Level.TRACE);

        final ApplicationPreferenceModel model = new ApplicationPreferenceModel();
        SettingsManager.getInstance().configure(new ApplicationPreferenceModelSaver(model));

        cyclicBufferSize = model.getCyclicBufferSize();

        setShutdownAction(
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                }
            });

        applicationPreferenceModel = new ApplicationPreferenceModel();

        SettingsManager.getInstance().configure(new ApplicationPreferenceModelSaver(model));

        EventQueue.invokeLater(() -> {
            loadLookAndFeelUsingPluginClassLoader(model.getLookAndFeelClassName());
            createChainsawGUI(model, null);
            getApplicationPreferenceModel().apply(model);
            activateViewer();
        });
    }

    /**
     * Initialises the menu's and toolbars, but does not actually create any of
     * the main panel components.
     */
    private void initGUI() {

        setupHelpSystem();
        statusBar = new ChainsawStatusBar(this);
        setupReceiverPanel();

        setToolBarAndMenus(new ChainsawToolBarAndMenus(this));
        toolbar = getToolBarAndMenus().getToolbar();
        setJMenuBar(getToolBarAndMenus().getMenubar());

        setTabbedPane(new ChainsawTabbedPane());
        getSettingsManager().addSettingsListener(getTabbedPane());
        getSettingsManager().configure(getTabbedPane());

        /**
         * This adds Drag & Drop capability to Chainsaw
         */
        FileDnDTarget dnDTarget = new FileDnDTarget(tabbedPane);
        dnDTarget.addPropertyChangeListener("fileList", evt -> {
            final List fileList = (List) evt.getNewValue();

            Thread thread = new Thread(() -> {
                logger.debug("Loading files: " + fileList);
                for (Object aFileList : fileList) {
                    File file = (File) aFileList;
                    final Decoder decoder = new XMLDecoder();
                    try {
                        getStatusBar().setMessage("Loading " + file.getAbsolutePath() + "...");
//                        FileLoadAction.importURL(handler, decoder, file
//                            .getName(), file.toURI().toURL());
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

        applicationPreferenceModelPanel = new ApplicationPreferenceModelPanel(applicationPreferenceModel);

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

    private void initPlugins(PluginRegistry pluginRegistry) {
        pluginRegistry.addPluginListener(
            new PluginListener() {
                public void pluginStarted(PluginEvent e) {
                    if (e.getPlugin() instanceof JComponent) {
                        JComponent plugin = (JComponent) e.getPlugin();
                        getTabbedPane().addANewTab(plugin.getName(), plugin, null, null);
                        getPanelMap().put(plugin.getName(), plugin);
                    }
                }

                public void pluginStopped(PluginEvent e) {
                    //TODO remove the plugin from the gui
                }
            });

        // TODO this should all be in a config file
//    ChainsawCentral cc = new ChainsawCentral();
//    pluginRegistry.addPlugin(cc);
//    cc.activateOptions();

        try {
            Class<? extends Plugin> pluginClass = Class.forName("org.apache.log4j.chainsaw.zeroconf.ZeroConfPlugin").asSubclass(Plugin.class);
            Plugin plugin = pluginClass.newInstance();
            pluginRegistry.addPlugin(plugin);
            plugin.activateOptions();
            MessageCenter.getInstance().getLogger().info("Looks like ZeroConf is available... WooHoo!");
        } catch (Throwable e) {
            MessageCenter.getInstance().getLogger().error("Doesn't look like ZeroConf is available", e);
        }
    }

    private void setupReceiverPanel() {
        receiversPanel = new ReceiversPanel(this);
        receiversPanel.addPropertyChangeListener(
            "visible",
            evt -> getApplicationPreferenceModel().setReceivers(
                (Boolean) evt.getNewValue()));
    }

    /**
     * Initialises the Help system and the WelcomePanel
     */
    private void setupHelpSystem() {
        welcomePanel = new WelcomePanel();

        JToolBar tb = welcomePanel.getToolbar();


        tb.add(
            new SmallButton(
                new AbstractAction("Tutorial", new ImageIcon(ChainsawIcons.HELP)) {
                    public void actionPerformed(ActionEvent e) {
                        setupTutorial();
                    }
                }));
        tb.addSeparator();

        final Action exampleConfigAction =
            new AbstractAction("View example Receiver configuration") {
                public void actionPerformed(ActionEvent e) {
                    HelpManager.getInstance().setHelpURL(
                        ChainsawConstants.EXAMPLE_CONFIG_URL);
                }
            };

        exampleConfigAction.putValue(
            Action.SHORT_DESCRIPTION,
            "Displays an example Log4j configuration file with several Receivers defined.");

        JButton exampleButton = new SmallButton(exampleConfigAction);
        tb.add(exampleButton);

        tb.add(Box.createHorizontalGlue());

        /**
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
     *
     * @param event DOCUMENT ME!
     */
    public void loadSettings(LoadSettingsEvent event) {
        setLocation(
            event.asInt(LogUI.MAIN_WINDOW_X), event.asInt(LogUI.MAIN_WINDOW_Y));
        int width = event.asInt(LogUI.MAIN_WINDOW_WIDTH);
        int height = event.asInt(LogUI.MAIN_WINDOW_HEIGHT);
        if (width == -1 && height == -1) {
            width = Toolkit.getDefaultToolkit().getScreenSize().width;
            height = Toolkit.getDefaultToolkit().getScreenSize().height;
            setSize(width, height);
            setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        } else {
            setSize(width, height);
        }

        getToolBarAndMenus().stateChange();
        RuleColorizer colorizer = new RuleColorizer();
        colorizer.loadColorSettings(ChainsawConstants.DEFAULT_COLOR_RULE_NAME);
        allColorizers.put(ChainsawConstants.DEFAULT_COLOR_RULE_NAME, colorizer);
        if (event.getSetting("SavedConfig.logFormat") != null) {
            receiverConfigurationPanel.getModel().setLogFormat(event.getSetting("SavedConfig.logFormat"));
        }
    }

    /**
     * Ensures the location/size of the main window is stored with the settings
     *
     * @param event DOCUMENT ME!
     */
    public void saveSettings(SaveSettingsEvent event) {
        event.saveSetting(LogUI.MAIN_WINDOW_X, (int) getLocation().getX());
        event.saveSetting(LogUI.MAIN_WINDOW_Y, (int) getLocation().getY());

        event.saveSetting(LogUI.MAIN_WINDOW_WIDTH, getWidth());
        event.saveSetting(LogUI.MAIN_WINDOW_HEIGHT, getHeight());
        RuleColorizer colorizer = allColorizers.get(ChainsawConstants.DEFAULT_COLOR_RULE_NAME);
        colorizer.saveColorSettings(ChainsawConstants.DEFAULT_COLOR_RULE_NAME);
        if (receiverConfigurationPanel.getModel().getLogFormat() != null) {
            event.saveSetting("SavedConfig.logFormat", receiverConfigurationPanel.getModel().getLogFormat());
        }
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
        LoggerRepository repo = LogManager.getLoggerRepository();
        if (repo instanceof LoggerRepositoryEx) {
            this.pluginRegistry = ((LoggerRepositoryEx) repo).getPluginRegistry();
        }
        initGUI();

        initPrefModelListeners();

        /**
         * We add a simple appender to the MessageCenter logger
         * so that each message is displayed in the Status bar
         */
//        MessageCenter.getInstance().getLogger().addAppender(
//            new AppenderSkeleton() {
//                protected void append(LoggingEvent event) {
//                    getStatusBar().setMessage(event.getMessage().toString());
//                }
//
//                public void close() {
//                }
//
//                public boolean requiresLayout() {
//                    return false;
//                }
//            });


        initSocketConnectionListener();

        if (pluginRegistry.getPlugins(Receiver.class).size() == 0) {
            noReceiversDefined = true;
        }

        getFilterableColumns().add(ChainsawConstants.LEVEL_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.LOGGER_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.THREAD_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.NDC_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.PROPERTIES_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.CLASS_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.METHOD_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.FILE_COL_NAME);
        getFilterableColumns().add(ChainsawConstants.NONE_COL_NAME);

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
        dividerSize = mainReceiverSplitPane.getDividerSize();
        mainReceiverSplitPane.setDividerLocation(-1);

        getContentPane().add(mainReceiverSplitPane, BorderLayout.CENTER);

        /**
         * We need to make sure that all the internal GUI components have been added to the
         * JFrame so that any plugns that get activated during initPlugins(...) method
         * have access to inject menus
         */
        initPlugins(pluginRegistry);

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
                        tbms.stateChange();
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
                            getPanelMap().keySet().contains(name)
                                && !name.equals(currentName)) {
                            displayPanel(name, false);
                            tbms.stateChange();
                        } else {
                            index++;
                        }
                    }
                }
            };

        Action showHiddenTabsAction =
            new AbstractAction("Show All Hidden") {
                public void actionPerformed(ActionEvent e) {
                    for (Object o : getPanels().entrySet()) {
                        Map.Entry entry = (Map.Entry) o;
                        Boolean docked = (Boolean) entry.getValue();
                        if (docked) {
                            String identifier = (String) entry.getKey();
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
                                tbms.stateChange();
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

//        this.handler.addPropertyChangeListener(
//            "dataRate",
//            evt -> {
//                double dataRate = (Double) evt.getNewValue();
//                statusBar.setDataRate(dataRate);
//            });

        getSettingsManager().addSettingsListener(this);
        getSettingsManager().addSettingsListener(MRUFileListPreferenceSaver.getInstance());
        getSettingsManager().addSettingsListener(receiversPanel);
        try {
            //if an uncaught exception is thrown, allow the UI to continue to load
            getSettingsManager().loadSettings();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //app preferences have already been loaded (and configuration url possibly set to blank if being overridden)
        //but we need a listener so the settings will be saved on exit (added after loadsettings was called)
        getSettingsManager().addSettingsListener(new ApplicationPreferenceModelSaver(applicationPreferenceModel));

        setVisible(true);

        if (applicationPreferenceModel.isReceivers()) {
            showReceiverPanel();
        } else {
            hideReceiverPanel();
        }

        removeSplash();

        synchronized (initializationLock) {
            isGUIFullyInitialized = true;
            initializationLock.notifyAll();
        }

        if (
            noReceiversDefined
                && applicationPreferenceModel.isShowNoReceiverWarning()) {
            SwingHelper.invokeOnEDT(this::showReceiverConfigurationPanel);
        }

        Container container = tutorialFrame.getContentPane();
        final JEditorPane tutorialArea = new JEditorPane();
        tutorialArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        tutorialArea.setEditable(false);
        container.setLayout(new BorderLayout());

        try {
            tutorialArea.setPage(ChainsawConstants.TUTORIAL_URL);
            JTextComponentFormatter.applySystemFontAndSize(tutorialArea);

            container.add(new JScrollPane(tutorialArea), BorderLayout.CENTER);
        } catch (Exception e) {
            MessageCenter.getInstance().getLogger().error(
                "Error occurred loading the Tutorial", e);
        }

        tutorialFrame.setIconImage(new ImageIcon(ChainsawIcons.HELP).getImage());
        tutorialFrame.setSize(new Dimension(640, 480));

        final Action startTutorial =
            new AbstractAction(
                "Start Tutorial", new ImageIcon(ChainsawIcons.ICON_RESUME_RECEIVER)) {
                public void actionPerformed(ActionEvent e) {
                    if (
                        JOptionPane.showConfirmDialog(
                            null,
                            "This will start 3 \"Generator\" receivers for use in the Tutorial.  Is that ok?",
                            "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        new Thread(new Tutorial()).start();
                        putValue("TutorialStarted", Boolean.TRUE);
                    } else {
                        putValue("TutorialStarted", Boolean.FALSE);
                    }
                }
            };

        final Action stopTutorial =
            new AbstractAction(
                "Stop Tutorial", new ImageIcon(ChainsawIcons.ICON_STOP_RECEIVER)) {
                public void actionPerformed(ActionEvent e) {
                    if (
                        JOptionPane.showConfirmDialog(
                            null,
                            "This will stop all of the \"Generator\" receivers used in the Tutorial, but leave any other Receiver untouched.  Is that ok?",
                            "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        new Thread(
                            () -> {
                                LoggerRepository repo1 = LogManager.getLoggerRepository();
                                if (repo1 instanceof LoggerRepositoryEx) {
                                    PluginRegistry pluginRegistry = ((LoggerRepositoryEx) repo1).getPluginRegistry();
                                    List list = pluginRegistry.getPlugins(Generator.class);

                                    for (Object aList : list) {
                                        Plugin plugin = (Plugin) aList;
                                        pluginRegistry.stopPlugin(plugin.getName());
                                    }
                                }
                            }).start();
                        setEnabled(false);
                        startTutorial.putValue("TutorialStarted", Boolean.FALSE);
                    }
                }
            };

        stopTutorial.putValue(
            Action.SHORT_DESCRIPTION,
            "Removes all of the Tutorials Generator Receivers, leaving all other Receivers untouched");
        startTutorial.putValue(
            Action.SHORT_DESCRIPTION,
            "Begins the Tutorial, starting up some Generator Receivers so you can see Chainsaw in action");
        stopTutorial.setEnabled(false);

        final SmallToggleButton startButton = new SmallToggleButton(startTutorial);
        PropertyChangeListener pcl =
            evt -> {
                stopTutorial.setEnabled(
                    startTutorial.getValue("TutorialStarted").equals(Boolean.TRUE));
                startButton.setSelected(stopTutorial.isEnabled());
            };

        startTutorial.addPropertyChangeListener(pcl);
        stopTutorial.addPropertyChangeListener(pcl);

        pluginRegistry.addPluginListener(
            new PluginListener() {
                public void pluginStarted(PluginEvent e) {
                }

                public void pluginStopped(PluginEvent e) {
                    List list = pluginRegistry.getPlugins(Generator.class);

                    if (list.size() == 0) {
                        startTutorial.putValue("TutorialStarted", Boolean.FALSE);
                    }
                }
            });

        final SmallButton stopButton = new SmallButton(stopTutorial);

        final JToolBar tutorialToolbar = new JToolBar();
        tutorialToolbar.setFloatable(false);
        tutorialToolbar.add(startButton);
        tutorialToolbar.add(stopButton);
        container.add(tutorialToolbar, BorderLayout.NORTH);
        tutorialArea.addHyperlinkListener(
            e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e.getDescription().equals("StartTutorial")) {
                        startTutorial.actionPerformed(null);
                    } else if (e.getDescription().equals("StopTutorial")) {
                        stopTutorial.actionPerformed(null);
                    } else {
                        try {
                            tutorialArea.setPage(e.getURL());
                        } catch (IOException e1) {
                            MessageCenter.getInstance().getLogger().error(
                                "Failed to change the URL for the Tutorial", e1);
                        }
                    }
                }
            });

        /**
         * loads the saved tab settings and if there are hidden tabs,
         * hide those tabs out of currently loaded tabs..
         */

        if (!getTabbedPane().tabSetting.isWelcome()) {
            displayPanel(ChainsawTabbedPane.WELCOME_TAB, false);
        }
        if (!getTabbedPane().tabSetting.isZeroconf()) {
            displayPanel(ChainsawTabbedPane.ZEROCONF, false);
        }
        tbms.stateChange();

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

    private void initSocketConnectionListener() {
        final SocketNodeEventListener socketListener =
            new SocketNodeEventListener() {
                public void socketOpened(String remoteInfo) {
                    statusBar.remoteConnectionReceived(remoteInfo);
                }

                public void socketClosedEvent(Exception e) {
                    MessageCenter.getInstance().getLogger().info(
                        "Connection lost! :: " + e.getMessage());
                }
            };

        PluginListener pluginListener =
            new PluginListener() {
                public void pluginStarted(PluginEvent e) {
                    MessageCenter.getInstance().getLogger().info(
                        e.getPlugin().getName() + " started!");

                    Method method = getAddListenerMethod(e.getPlugin());

                    if (method != null) {
                        try {
                            method.invoke(e.getPlugin(), socketListener);
                        } catch (Exception ex) {
                            MessageCenter.getInstance().getLogger().error(
                                "Failed to add a SocketNodeEventListener", ex);
                        }
                    }
                }

                Method getRemoveListenerMethod(Plugin p) {
                    try {
                        return p.getClass().getMethod(
                            "removeSocketNodeEventListener",
                            SocketNodeEventListener.class);
                    } catch (Exception e) {
                        return null;
                    }
                }

                Method getAddListenerMethod(Plugin p) {
                    try {
                        return p.getClass().getMethod(
                            "addSocketNodeEventListener",
                            SocketNodeEventListener.class);
                    } catch (Exception e) {
                        return null;
                    }
                }

                public void pluginStopped(PluginEvent e) {
                    Method method = getRemoveListenerMethod(e.getPlugin());

                    if (method != null) {
                        try {
                            method.invoke(e.getPlugin(), socketListener);
                        } catch (Exception ex) {
                            MessageCenter.getInstance().getLogger().error(
                                "Failed to remove SocketNodeEventListener", ex);
                        }
                    }

                    MessageCenter.getInstance().getLogger().info(
                        e.getPlugin().getName() + " stopped!");
                }
            };

        pluginRegistry.addPluginListener(pluginListener);
    }

    private void initPrefModelListeners() {
//        applicationPreferenceModel.addPropertyChangeListener(
//            "identifierExpression",
//            evt -> handler.setIdentifierExpression(evt.getNewValue().toString()));
//        handler.setIdentifierExpression(applicationPreferenceModel.getIdentifierExpression());


        applicationPreferenceModel.addPropertyChangeListener(
            "toolTipDisplayMillis",
            evt -> ToolTipManager.sharedInstance().setDismissDelay(
                (Integer) evt.getNewValue()));
        ToolTipManager.sharedInstance().setDismissDelay(
            applicationPreferenceModel.getToolTipDisplayMillis());

//        applicationPreferenceModel.addPropertyChangeListener(
//            "responsiveness",
//            evt -> {
//                int value = (Integer) evt.getNewValue();
//                handler.setQueueInterval((value * 1000) - 750);
//            });
//        handler.setQueueInterval((applicationPreferenceModel.getResponsiveness() * 1000) - 750);

        applicationPreferenceModel.addPropertyChangeListener(
            "tabPlacement",
            evt -> SwingUtilities.invokeLater(
                () -> {
                    int placement = (Integer) evt.getNewValue();

                    switch (placement) {
                        case SwingConstants.TOP:
                        case SwingConstants.BOTTOM:
                            tabbedPane.setTabPlacement(placement);

                            break;

                        default:
                            break;
                    }
                }));

        applicationPreferenceModel.addPropertyChangeListener(
            "statusBar",
            evt -> {
                boolean value = (Boolean) evt.getNewValue();
                setStatusBarVisible(value);
            });
        setStatusBarVisible(applicationPreferenceModel.isStatusBar());

        applicationPreferenceModel.addPropertyChangeListener(
            "receivers",
            evt -> {
                boolean value = (Boolean) evt.getNewValue();

                if (value) {
                    showReceiverPanel();
                } else {
                    hideReceiverPanel();
                }
            });
//    if (applicationPreferenceModel.isReceivers()) {
//      showReceiverPanel();
//    } else {
//      hideReceiverPanel();
//    }


        applicationPreferenceModel.addPropertyChangeListener(
            "toolbar",
            evt -> {
                boolean value = (Boolean) evt.getNewValue();
                toolbar.setVisible(value);
            });
        toolbar.setVisible(applicationPreferenceModel.isToolbar());

    }

    /**
     * Displays a dialog which will provide options for selecting a configuration
     */
    private void showReceiverConfigurationPanel() {
        SwingUtilities.invokeLater(
            () -> {
                final JDialog dialog = new JDialog(LogUI.this, true);
                dialog.setTitle("Load events into Chainsaw");
                dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

                dialog.setResizable(false);

                receiverConfigurationPanel.setCompletionActionListener(
                    e -> {
                        dialog.setVisible(false);

                        if (receiverConfigurationPanel.getModel().isCancelled()) {
                            return;
                        }
                        applicationPreferenceModel.setShowNoReceiverWarning(!receiverConfigurationPanel.isDontWarnMeAgain());
                        //remove existing plugins
                        List<Plugin> plugins = pluginRegistry.getPlugins();
                        for (Object plugin1 : plugins) {
                            Plugin plugin = (Plugin) plugin1;
                            //don't stop ZeroConfPlugin if it is registered
                            if (!plugin.getName().toLowerCase(Locale.ENGLISH).contains("zeroconf")) {
                                pluginRegistry.stopPlugin(plugin.getName());
                            }
                        }
                        URL configURL = null;

                        if (receiverConfigurationPanel.getModel().isNetworkReceiverMode()) {
                            int port = receiverConfigurationPanel.getModel().getNetworkReceiverPort();

                            try {
                                Class<? extends Receiver> receiverClass = receiverConfigurationPanel.getModel().getNetworkReceiverClass();
                                Receiver networkReceiver = receiverClass.newInstance();
                                networkReceiver.setName(receiverClass.getSimpleName() + "-" + port);

                                Method portMethod =
                                    networkReceiver.getClass().getMethod(
                                        "setPort", int.class);
                                portMethod.invoke(
                                    networkReceiver, port);

                                networkReceiver.setThreshold(Level.TRACE);

                                pluginRegistry.addPlugin(networkReceiver);
                                networkReceiver.activateOptions();
                                receiversPanel.updateReceiverTreeInDispatchThread();
                            } catch (Exception e3) {
                                MessageCenter.getInstance().getLogger().error(
                                    "Error creating Receiver", e3);
                                MessageCenter.getInstance().getLogger().info(
                                    "An error occurred creating your Receiver");
                            }
                        } else if (receiverConfigurationPanel.getModel().isLog4jConfig()) {
                            File log4jConfigFile = receiverConfigurationPanel.getModel().getLog4jConfigFile();
                            if (log4jConfigFile != null) {
                                try {
                                    Map<String, Map<String, String>> entries = LogFilePatternLayoutBuilder.getAppenderConfiguration(log4jConfigFile);
                                    for (Object o : entries.entrySet()) {
                                        try {
                                            Map.Entry entry = (Map.Entry) o;
                                            String name = (String) entry.getKey();
                                            Map values = (Map) entry.getValue();
                                            //values: conversion, file
                                            String conversionPattern = values.get("conversion").toString();
                                            File file = new File(values.get("file").toString());
                                            URL fileURL = file.toURI().toURL();
                                            String timestampFormat = LogFilePatternLayoutBuilder.getTimeStampFormat(conversionPattern);
                                            String receiverPattern = LogFilePatternLayoutBuilder.getLogFormatFromPatternLayout(conversionPattern);
                                            VFSLogFilePatternReceiver fileReceiver = new VFSLogFilePatternReceiver();
                                            fileReceiver.setName(name);
                                            fileReceiver.setAutoReconnect(true);
                                            fileReceiver.setContainer(LogUI.this);
                                            fileReceiver.setAppendNonMatches(true);
                                            fileReceiver.setFileURL(fileURL.toURI().toString());
                                            fileReceiver.setTailing(true);
                                            fileReceiver.setLogFormat(receiverPattern);
                                            fileReceiver.setTimestampFormat(timestampFormat);
                                            fileReceiver.setThreshold(Level.TRACE);
                                            pluginRegistry.addPlugin(fileReceiver);
                                            fileReceiver.activateOptions();
                                            receiversPanel.updateReceiverTreeInDispatchThread();
                                        } catch (URISyntaxException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        } else if (receiverConfigurationPanel.getModel().isLoadConfig()) {
                            configURL = receiverConfigurationPanel.getModel().getConfigToLoad();
                        } else if (receiverConfigurationPanel.getModel().isLogFileReceiverConfig()) {
                            try {
                                URL fileURL = receiverConfigurationPanel.getModel().getLogFileURL();
                                if (fileURL != null) {
                                    VFSLogFilePatternReceiver fileReceiver = new VFSLogFilePatternReceiver();
                                    fileReceiver.setName(fileURL.getFile());
                                    fileReceiver.setAutoReconnect(true);
                                    fileReceiver.setContainer(LogUI.this);
                                    fileReceiver.setAppendNonMatches(true);
                                    fileReceiver.setFileURL(fileURL.toURI().toString());
                                    fileReceiver.setTailing(true);
                                    if (receiverConfigurationPanel.getModel().isPatternLayoutLogFormat()) {
                                        fileReceiver.setLogFormat(LogFilePatternLayoutBuilder.getLogFormatFromPatternLayout(receiverConfigurationPanel.getModel().getLogFormat()));
                                    } else {
                                        fileReceiver.setLogFormat(receiverConfigurationPanel.getModel().getLogFormat());
                                    }
                                    fileReceiver.setTimestampFormat(receiverConfigurationPanel.getModel().getLogFormatTimestampFormat());
                                    fileReceiver.setThreshold(Level.TRACE);

                                    pluginRegistry.addPlugin(fileReceiver);
                                    fileReceiver.activateOptions();
                                    receiversPanel.updateReceiverTreeInDispatchThread();
                                }
                            } catch (Exception e2) {
                                MessageCenter.getInstance().getLogger().error(
                                    "Error creating Receiver", e2);
                                MessageCenter.getInstance().getLogger().info(
                                    "An error occurred creating your Receiver");
                            }
                        }
                        if (configURL == null && receiverConfigurationPanel.isDontWarnMeAgain()) {
                            //use the saved config file as the config URL if defined
                            if (receiverConfigurationPanel.getModel().getSaveConfigFile() != null) {
                                try {
                                    configURL = receiverConfigurationPanel.getModel().getSaveConfigFile().toURI().toURL();
                                } catch (MalformedURLException e1) {
                                    e1.printStackTrace();
                                }
                            } else {
                                //no saved config defined but don't warn me is checked - use default config
                                configURL = receiverConfigurationPanel.getModel().getDefaultConfigFileURL();
                            }
                        }
                        if (configURL != null) {
                            MessageCenter.getInstance().getLogger().debug(
                                "Initialiazing Log4j with " + configURL.toExternalForm());
                            final URL finalURL = configURL;
                            new Thread(
                                () -> {
                                    if (receiverConfigurationPanel.isDontWarnMeAgain()) {
                                        applicationPreferenceModel.setConfigurationURL(finalURL.toExternalForm());
                                    } else {
                                        try {
                                            if (new File(finalURL.toURI()).exists()) {
                                                loadConfigurationUsingPluginClassLoader(finalURL);
                                            }
                                        } catch (URISyntaxException e12) {
                                            //ignore
                                        }
                                    }

                                    receiversPanel.updateReceiverTreeInDispatchThread();
                                }).start();
                        }
                        File saveConfigFile = receiverConfigurationPanel.getModel().getSaveConfigFile();
                        if (saveConfigFile != null) {
                            ReceiversHelper.getInstance().saveReceiverConfiguration(saveConfigFile);
                        }
                    });

                receiverConfigurationPanel.setDialog(dialog);
                dialog.getContentPane().add(receiverConfigurationPanel);

                dialog.pack();

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                dialog.setLocation(
                    (screenSize.width / 2) - (dialog.getWidth() / 2),
                    (screenSize.height / 2) - (dialog.getHeight() / 2));

                dialog.setVisible(true);
            });
    }

    /**
     * Exits the application, ensuring Settings are saved.
     */
    public boolean exit() {
        getSettingsManager().saveSettings();

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
        applicationPreferenceModelPanel.updateModel();
        preferencesFrame.setVisible(true);
    }

    public void showReceiverConfiguration() {
        showReceiverConfigurationPanel();
    }

    public void showAboutBox() {
        if (aboutBox == null) {
            aboutBox = new ChainsawAbout(this);
        }

        aboutBox.setVisible(true);
    }

    Map getPanels() {
        Map m = new HashMap();
        Set<Map.Entry<String, Component>> panelSet = getPanelMap().entrySet();

        for (Object aPanelSet : panelSet) {
            Map.Entry entry = (Map.Entry) aPanelSet;
            Object o = entry.getValue();
            boolean valueToSend;
            valueToSend = !(o instanceof LogPanel) || ((DockablePanel) entry.getValue()).isDocked();
            m.put(entry.getKey(), valueToSend);
        }

        return m;
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
        if (getApplicationPreferenceModel().isConfirmExit()) {
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

                    pluginRegistry.stopAllPlugins();
                    panel.setProgress(progress++);

                    Thread.sleep(delay);

                    panel.setProgress(progress++);
                    Thread.sleep(delay);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                fireShutdownEvent();
                performShutdownAction();
                progressWindow.setVisible(false);
            };

        if (OSXIntegration.IS_OSX) {
            /**
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
        MessageCenter.getInstance().getLogger().debug(
            "Calling the shutdown Action. Goodbye!");

        shutdownAction.actionPerformed(
            new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutting Down"));
    }

    /**
     * Returns the currently selected LogPanel, if there is one, otherwise null
     *
     * @return current log panel
     */
    LogPanel getCurrentLogPanel() {
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
        MessageCenter.getInstance().getLogger().debug(
            "Setting StatusBar to " + visible);
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
     * Causes the Welcome Panel to become visible, and shows the URL specified as
     * it's contents
     *
     * @param url for content to show
     */
    public void showHelp(URL url) {
        ensureWelcomePanelVisible();
        //    TODO ensure the Welcome Panel is the selected tab
        getWelcomePanel().setURL(url);
    }

    /**
     * DOCUMENT ME!
     *
     * @return welcome panel
     */
    private WelcomePanel getWelcomePanel() {
        return welcomePanel;
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

    //  public Map getLevelMap() {
    //    return levelMap;
    //  }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public SettingsManager getSettingsManager() {
        return sm;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public List<String> getFilterableColumns() {
        return filterableColumns;
    }

    /**
     * DOCUMENT ME!
     *
     * @param tbms DOCUMENT ME!
     */
    public void setToolBarAndMenus(ChainsawToolBarAndMenus tbms) {
        this.tbms = tbms;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public ChainsawToolBarAndMenus getToolBarAndMenus() {
        return tbms;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Map getTableMap() {
        return tableMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Map getTableModelMap() {
        return tableModelMap;
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

    /**
     * DOCUMENT ME!
     */
    public void setupTutorial() {
        SwingUtilities.invokeLater(
            () -> {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                setLocation(0, getLocation().y);

                double chainsawwidth = 0.7;
                double tutorialwidth = 1 - chainsawwidth;
                setSize((int) (screen.width * chainsawwidth), getSize().height);
                invalidate();
                validate();

                Dimension size = getSize();
                Point loc = getLocation();
                tutorialFrame.setSize(
                    (int) (screen.width * tutorialwidth), size.height);
                tutorialFrame.setLocation(loc.x + size.width, loc.y);
                tutorialFrame.setVisible(true);
            });
    }

    private void buildLogPanel(
        boolean customExpression, final String ident, final List<ChainsawLoggingEvent> events, final ChainsawReceiver rx)
        throws IllegalArgumentException {
        final LogPanel thisPanel = new LogPanel(getStatusBar(), ident, cyclicBufferSize, allColorizers, applicationPreferenceModel);

        getSettingsManager().addSettingsListener(thisPanel);
        getSettingsManager().configure(thisPanel);

        if( !customExpression && rx != null ){
            thisPanel.setReceiver(rx);
        }

        /**
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
            evt -> tbms.stateChange();

        thisPanel.addPropertyChangeListener(toolbarMenuUpdateListener);
        thisPanel.addPreferencePropertyChangeListener(toolbarMenuUpdateListener);

        thisPanel.addPropertyChangeListener(
            "docked",
            evt -> {
                LogPanel logPanel = (LogPanel) evt.getSource();

                if (logPanel.isDocked()) {
                    getPanelMap().put(logPanel.getIdentifier(), logPanel);
                    getTabbedPane().addANewTab(
                        logPanel.getIdentifier(), logPanel, null);
                    getTabbedPane().setSelectedTab(getTabbedPane().indexOfTab(logPanel.getIdentifier()));
                } else {
                    getTabbedPane().remove(logPanel);
                }
            });

        logger.debug("adding logpanel to tabbed pane: " + ident);

        //NOTE: tab addition is a very fragile process - if you modify this code,
        //verify the frames in the individual log panels initialize to their
        //correct sizes
        getTabbedPane().add(ident, thisPanel);
        getPanelMap().put(ident, thisPanel);

        /**
         * Let the new LogPanel receive this batch
         */

        SwingUtilities.invokeLater(
            () -> {
                getTabbedPane().addANewTab(
                    ident, thisPanel, new ImageIcon(ChainsawIcons.ANIM_RADIO_TOWER));
                thisPanel.layoutComponents();
//                thisPanel.receiveEventBatch(ident, events);
                if (!getTabbedPane().tabSetting.isChainsawLog()) {
                    displayPanel("chainsaw-log", false);
                }
            });

        String msg = "added tab " + ident;
        MessageCenter.getInstance().getLogger().debug(msg);
    }

    public void createCustomExpressionLogPanel(String ident) {
        //collect events matching the rule from all of the tabs
        try {
            List<ChainsawLoggingEvent> list = new ArrayList<>();
            Rule rule = ExpressionRule.getRule(ident);

            for (Object identifierPanel : identifierPanels) {
                LogPanel panel = (LogPanel) identifierPanel;

                for (Object o : panel.getMatchingEvents(rule)) {
                    LoggingEventWrapper e = (LoggingEventWrapper) o;
                    list.add(e.getLoggingEvent());
                }
            }

            buildLogPanel(true, ident, list, null);
        } catch (IllegalArgumentException iae) {
            MessageCenter.getInstance().getLogger().info(
                "Unable to add tab using expression: " + ident + ", reason: "
                    + iae.getMessage());
        }
    }

    /**
     * Loads the log4j configuration file specified by the url, using
     * the PluginClassLoader instance as a TCCL, but only replacing it temporarily, with the original
     * TCCL being restored in a finally block to ensure consitency.
     *
     * @param url
     */
    private void loadConfigurationUsingPluginClassLoader(final URL url) {
        ClassLoader classLoader = PluginClassLoaderFactory.getInstance().getClassLoader();
        ClassLoader previousTCCL = Thread.currentThread().getContextClassLoader();

        if (url != null) {
            try {
                // we temporarily swap the TCCL so that plugins can find resources
                Thread.currentThread().setContextClassLoader(classLoader);
                try {
                    DOMConfigurator.configure(url);
                } catch (Exception e) {
                    logger.warn("Unable to load configuration URL: " + url, e);
                }
            } finally {
                // now switch it back...
                Thread.currentThread().setContextClassLoader(previousTCCL);
            }
        }
    }

    private static void loadLookAndFeelUsingPluginClassLoader(String lookAndFeelClassName) {
        ClassLoader classLoader = PluginClassLoaderFactory.getInstance().getClassLoader();
        ClassLoader previousTCCL = Thread.currentThread().getContextClassLoader();
        try {
            // we temporarily swap the TCCL so that plugins can find resources
            Thread.currentThread().setContextClassLoader(classLoader);
            UIManager.setLookAndFeel(lookAndFeelClassName);
            UIManager.getLookAndFeelDefaults().put("ClassLoader", classLoader);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // now switch it back...
            Thread.currentThread().setContextClassLoader(previousTCCL);
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
        m_receivers.add(rx);
        List<ChainsawLoggingEvent> list = new ArrayList<>();
        buildLogPanel(false, rx.getName(), list, rx);
        
        for(ReceiverEventListener listen : m_receiverListeners){
            listen.receiverAdded(rx);
        }
    }
    
    public void removeReceiver(ChainsawReceiver rx){
        if( !m_receivers.remove(rx) ){
            return;
        }
        
        for(ReceiverEventListener listen : m_receiverListeners){
            listen.receiverRemoved(rx);
        }
    }
    
    public void addReceiverEventListener(ReceiverEventListener listener){
        m_receiverListeners.add(listener);
    }
    
    public void removeReceiverEventListener(ReceiverEventListener listener){
        m_receiverListeners.remove(listener);
    }
    
    public List<ChainsawReceiver> getAllReceivers(){
        return m_receivers;
    }
}
