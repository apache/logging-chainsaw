package org.apache.log4j.chainsaw;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.log4j.chainsaw.osx.OSXIntegration;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ShutdownManager {
    private static Logger logger = LogManager.getLogger(ShutdownManager.class);

    private JFrame logUI;
    private AbstractConfiguration configuration;
    private List<ChainsawReceiver> receivers;
    private EventListenerList shutdownListenerList;

    /**
     * The shutdownAction is called when the user requests to exit Chainsaw, and
     * by default this exits the VM, but a developer may replace this action with
     * something that better suits their needs
     */
    private Action shutdownAction = null;

    public ShutdownManager(JFrame logUI, AbstractConfiguration configuration, List<ChainsawReceiver> receivers, EventListenerList shutdownListenerList) {
        this.logUI = logUI;
        this.configuration = configuration;
        this.receivers = receivers;
        this.shutdownListenerList = shutdownListenerList;

        // TODO unnecessary
        this.setShutdownAction( new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * Shutsdown by ensuring the Appender gets a chance to close.
     */
    public boolean shutdown() {
        boolean confirmExit = configuration.getBoolean("confirmExit", true);
        if (confirmExit) {
            if (
                JOptionPane.showConfirmDialog(
                    logUI, "Are you sure you want to exit Chainsaw?",
                    "Confirm Exit", JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE) != JOptionPane.YES_OPTION) {
                return false;
            }

        }

        final JWindow progressWindow = new JWindow();
        final ProgressPanel panel = new ProgressPanel(1, 3, "Shutting down");
        progressWindow.getContentPane().add(panel);
        progressWindow.pack();

        Point p = new Point(logUI.getLocation());
        p.move((int) logUI.getSize().getWidth() >> 1, (int) logUI.getSize().getHeight() >> 1);
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

}
