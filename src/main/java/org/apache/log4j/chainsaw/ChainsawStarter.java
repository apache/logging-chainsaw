package org.apache.log4j.chainsaw;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.chainsaw.components.splash.SplashViewer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Locale;

public class ChainsawStarter {
    private static final Logger logger = LogManager.getLogger(ChainsawStarter.class);

    /**
     * Starts Chainsaw by attaching a new instance to the Log4J main root Logger
     * via a ChainsawAppender, and activates itself
     */
    public static void main(String[] args) {
        if (OSXIntegration.IS_OSX) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        SettingsManager settingsManager = new SettingsManager();

        AbstractConfiguration configuration = settingsManager.getGlobalConfiguration();
        EventQueue.invokeLater(() -> {
            String lookAndFeelClassName = configuration.getString("lookAndFeelClassName");
            if (lookAndFeelClassName == null || lookAndFeelClassName.trim().isEmpty()) {
                String osName = System.getProperty("os.name");
                if (osName.toLowerCase(Locale.ENGLISH).startsWith("mac")) {
                    //no need to assign look and feel
                } else if (osName.toLowerCase(Locale.ENGLISH).startsWith("windows")) {
                    lookAndFeelClassName = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
                    configuration.setProperty("lookAndFeelClassName",lookAndFeelClassName);
                } else if (osName.toLowerCase(Locale.ENGLISH).startsWith("linux")) {
                    lookAndFeelClassName = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
                    configuration.setProperty("lookAndFeelClassName",lookAndFeelClassName);
                }
            }

            if (lookAndFeelClassName != null && !(lookAndFeelClassName.trim().isEmpty())) {
                try{
                    UIManager.setLookAndFeel(lookAndFeelClassName);
                } catch (Exception ex){
                    logger.error(ex);
                }
            }else{
                try{
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex){
                    logger.error(ex);
                }
            }
            createChainsawGUI(settingsManager);
        });
    }

    /**
     * Creates, activates, and then shows the Chainsaw GUI, optionally showing
     * the splash screen, and using the passed shutdown action when the user
     * requests to exit the application (if null, then Chainsaw will exit the vm)
     */
    public static void createChainsawGUI(SettingsManager settingsManager) {
        SplashViewer splashViewer = new SplashViewer();

        AbstractConfiguration configuration = settingsManager.getGlobalConfiguration();
        if (configuration.getBoolean("okToRemoveSecurityManager", false)) {
            System.setSecurityManager(null);
            // this SHOULD set the Policy/Permission stuff for any
            // code loaded from our custom classloader.
            // crossing fingers...
            Policy.setPolicy(new Policy() {
                @Override
                public PermissionCollection getPermissions(CodeSource codesource) {
                    Permissions perms = new Permissions();
                    perms.add(new AllPermission());
                    return (perms);
                }
            });
        }

        final LogUI logUI = new LogUI(settingsManager);
        final LoggerContext ctx = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        logUI.chainsawAppender = ctx.getConfiguration().getAppender("chainsaw");

        if (configuration.getBoolean("slowSplash", true)) {
            splashViewer.showSplash(logUI);
        }
        logUI.cyclicBufferSize = configuration.getInt("cyclicBufferSize", 50000);


        /**
         * TODO until we work out how JoranConfigurator might be able to have
         * configurable class loader, if at all.  For now we temporarily replace the
         * TCCL so that Plugins that need access to resources in
         * the Plugins directory can find them (this is particularly
         * important for the Web start version of Chainsaw
         */
        //configuration initialized here

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            logger.error("Uncaught exception in thread {}", t.getName(), e);
        });

        EventQueue.invokeLater(() -> {
            logUI.activateViewer();
            splashViewer.removeSplash();
        });
        EventQueue.invokeLater(logUI::buildChainsawLogPanel);

        logger.info("SecurityManager is now: {}", System.getSecurityManager());

    }
}
