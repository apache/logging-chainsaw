package org.apache.log4j.chainsaw.components.tutorial;

import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.ChainsawStatusBar;
import org.apache.log4j.chainsaw.Generator;
import org.apache.log4j.chainsaw.JTextComponentFormatter;
import org.apache.log4j.chainsaw.LogUI;
import org.apache.log4j.chainsaw.ReceiverEventListener;
import org.apache.log4j.chainsaw.components.elements.SmallButton;
import org.apache.log4j.chainsaw.components.elements.SmallToggleButton;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;

public class TutorialFrame {
    public static final String LABEL_TUTORIAL_STARTED = "TutorialStarted";

    private final Logger logger = LogManager.getLogger(TutorialFrame.class);

    private List<ChainsawReceiver> receivers;
    private List<ReceiverEventListener> receiverEventListeners;
    private LogUI logUI;
    private final JFrame tutorialFrame = new JFrame("Chainsaw Tutorial");
    public TutorialFrame(List<ChainsawReceiver> receivers, List<ReceiverEventListener> receiverEventListeners, LogUI logUI) {
        this.receivers = receivers;
        this.receiverEventListeners = receiverEventListeners;
        this.logUI = logUI;
    }

    public void createTutorialFrame(ChainsawStatusBar statusBar) {
        Container container = tutorialFrame.getContentPane();
        container.setLayout(new BorderLayout());

        final JEditorPane tutorialArea = new JEditorPane();
        tutorialArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        tutorialArea.setEditable(false);

        try {
            tutorialArea.setPage(ChainsawConstants.TUTORIAL_URL);
            JTextComponentFormatter.applySystemFontAndSize(tutorialArea);
            container.add(new JScrollPane(tutorialArea), BorderLayout.CENTER);
        } catch (Exception e) {
            logger.error("Can't load tutorial", e);
            statusBar.setMessage("Can't load tutorial");
        }

        tutorialFrame.setIconImage(new ImageIcon(ChainsawIcons.HELP).getImage());
        tutorialFrame.setSize(new Dimension(640, 480));

        final Action startTutorial = createStartTutorialAction();
        final Action stopTutorial = createStopTutorialAction(startTutorial);
        final JToolBar tutorialToolbar = createTutorialToolbar(startTutorial, stopTutorial);

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
                            statusBar.setMessage("Failed to change URL for tutorial");
                            logger.error(
                                "Failed to change the URL for the Tutorial", e1);
                        }
                    }
                }
            });
    }

    private Action createStopTutorialAction(Action startTutorial) {
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
                                for( ChainsawReceiver rx : receivers){
                                    if( rx instanceof Generator){
                                        rx.shutdown();
                                    }
                                }
                            }).start();
                        setEnabled(false);
                        startTutorial.putValue(LABEL_TUTORIAL_STARTED, Boolean.FALSE);
                    }
                }
            };

        stopTutorial.putValue(
            Action.SHORT_DESCRIPTION,
            "Removes all of the Tutorials Generator Receivers, leaving all other Receivers untouched");

        stopTutorial.setEnabled(false);
        return stopTutorial;
    }

    private Action createStartTutorialAction() {
        final Action startTutorial =
            new AbstractAction(
                "Start Tutorial", new ImageIcon(ChainsawIcons.ICON_RESUME_RECEIVER)) {
                public void actionPerformed(ActionEvent e) {
                    if (
                        JOptionPane.showConfirmDialog(
                            null,
                            "This will start 3 \"Generator\" receivers for use in the Tutorial.  Is that ok?",
                            "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        // Create and start generators
                        Generator[] generators = {
                            new Generator("Generator 1"),
                            new Generator("Generator 2"),
                            new Generator("Generator 3"),
                        };

                        for( Generator gen : generators ){
                            logUI.addReceiver(gen);
                            gen.start();
                        }

                        putValue(LABEL_TUTORIAL_STARTED, Boolean.TRUE);
                    } else {
                        putValue(LABEL_TUTORIAL_STARTED, Boolean.FALSE);
                    }
                }
            };
        startTutorial.putValue(
            Action.SHORT_DESCRIPTION,
            "Begins the Tutorial, starting up some Generator Receivers so you can see Chainsaw in action");
        return startTutorial;
    }

    private JToolBar createTutorialToolbar(Action startTutorial, Action stopTutorial) {
        final SmallToggleButton startButton = new SmallToggleButton(startTutorial);
        PropertyChangeListener pcl =
            evt -> {
                stopTutorial.setEnabled(
                    startTutorial.getValue(LABEL_TUTORIAL_STARTED).equals(Boolean.TRUE));
                startButton.setSelected(stopTutorial.isEnabled());
            };

        startTutorial.addPropertyChangeListener(pcl);
        stopTutorial.addPropertyChangeListener(pcl);

        receiverEventListeners.add(new ReceiverEventListener() {
            @Override
            public void receiverAdded(ChainsawReceiver rx) {}

            @Override
            public void receiverRemoved(ChainsawReceiver rx1) {
                int count = 0;
                for( ChainsawReceiver rx : receivers){
                    if( rx instanceof Generator ){
                        count++;
                    }
                }

                if (count == 0) {
                    startTutorial.putValue(LABEL_TUTORIAL_STARTED, Boolean.FALSE);
                }
            }
        });

        final SmallButton stopButton = new SmallButton(stopTutorial);

        final JToolBar tutorialToolbar = new JToolBar();
        tutorialToolbar.setFloatable(false);
        tutorialToolbar.add(startButton);
        tutorialToolbar.add(stopButton);
        return tutorialToolbar;
    }
}
