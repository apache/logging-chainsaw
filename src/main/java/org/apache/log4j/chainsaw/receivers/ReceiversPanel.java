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

package org.apache.log4j.chainsaw.receivers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.log4j.chainsaw.PopupListener;
import org.apache.log4j.chainsaw.SmallButton;
import org.apache.log4j.chainsaw.help.HelpManager;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LevelIconFactory;
import org.apache.log4j.chainsaw.icons.LineIconFactory;
import org.apache.log4j.chainsaw.prefs.LoadSettingsEvent;
import org.apache.log4j.chainsaw.prefs.SaveSettingsEvent;
import org.apache.log4j.chainsaw.prefs.SettingsListener;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.net.SocketNodeEventListener;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.log4j.chainsaw.ChainsawReceiver;
import org.apache.log4j.chainsaw.LogUI;
import org.apache.log4j.chainsaw.ChainsawReceiverFactory;
import org.apache.log4j.chainsaw.ChainsawStatusBar;
import org.apache.log4j.chainsaw.logevents.Level;


/**
 * This panel is used to manage all the Receivers configured within Log4j
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class ReceiversPanel extends JPanel implements SettingsListener {
    final Action newReceiverButtonAction;
    final Action pauseReceiverButtonAction;
    final Action playReceiverButtonAction;
    final Action shutdownReceiverButtonAction;
    final Action saveReceiversButtonAction;
    final Action restartReceiverButtonAction;
    private final Action showReceiverHelpAction;
    private final Action startAllAction;
    private final JPopupMenu popupMenu = new ReceiverPopupMenu();
    private final JTree receiversTree = new JTree();
    private final NewReceiverPopupMenu newReceiverPopup =
        new NewReceiverPopupMenu();
    private final ReceiverToolbar buttonPanel;
    private final JSplitPane splitter = new JSplitPane();
    private final PluginPropertyEditorPanel pluginEditorPanel =
        new PluginPropertyEditorPanel();
    private final Logger logger = LogManager.getLogger(ReceiversPanel.class);

    private final LogUI m_parent;
    private final Map<Class,PropertyDescriptor[]> m_classToProperties = 
            new HashMap<>();
    private final ChainsawStatusBar m_statusBar;

    public ReceiversPanel(LogUI parentUi, ChainsawStatusBar sb) {
        super(new BorderLayout());
        m_statusBar = sb;
        m_parent = parentUi;
        final ReceiversTreeModel model = new ReceiversTreeModel();
        m_parent.addReceiverEventListener(model);

        receiversTree.setModel(model);

        receiversTree.setExpandsSelectedPaths(true);
        model.addTreeModelListener(
            new TreeModelListener() {
                public void treeNodesChanged(TreeModelEvent e) {
                    expandRoot();
                }

                public void treeNodesInserted(TreeModelEvent e) {
                    expandRoot();
                }

                public void treeNodesRemoved(TreeModelEvent e) {
                    expandRoot();
                }

                public void treeStructureChanged(TreeModelEvent e) {
                    expandRoot();
                }

                private void expandRoot() {
                    receiversTree.expandPath(
                        new TreePath(model.getPathToRoot(model.RootNode)));
                }
            });
        receiversTree.expandPath(
            new TreePath(model.getPathToRoot(model.RootNode)));

        receiversTree.addTreeWillExpandListener(
            new TreeWillExpandListener() {
                public void treeWillCollapse(TreeExpansionEvent event)
                    throws ExpandVetoException {
                    if (event.getPath().getLastPathComponent() == model.RootNode) {
                        throw new ExpandVetoException(event);
                    }
                }

                public void treeWillExpand(TreeExpansionEvent event) {
                }
            });

        receiversTree.addTreeSelectionListener(
            e -> {
                TreePath path = e.getNewLeadSelectionPath();

                if (path != null) {
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) path.getLastPathComponent();

                    if (
                        (node != null) && (node.getUserObject() != null)
                            && (node.getUserObject() instanceof ChainsawReceiver)) {
                        ChainsawReceiver p = (ChainsawReceiver) node.getUserObject();
                        logger.debug("plugin={}", p);
                        pluginEditorPanel.setReceiverAndProperties(p, 
                                m_classToProperties.get(p.getClass()));
                    } else {
                        pluginEditorPanel.setReceiverAndProperties(null, null);
                    }
                }
            });

        receiversTree.setToolTipText("Allows you to manage Log4j Receivers");
        newReceiverButtonAction =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    newReceiverPopup.show(
                        buttonPanel.newReceiverButton, 0,
                        buttonPanel.newReceiverButton.getHeight());
                }
            };
        newReceiverButtonAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.ICON_NEW_RECEIVER));
        newReceiverButtonAction.putValue(
            Action.SHORT_DESCRIPTION, "Creates and configures a new Receiver");
        newReceiverButtonAction.putValue(Action.NAME, "New Receiver");
        newReceiverButtonAction.putValue(
            Action.MNEMONIC_KEY, KeyEvent.VK_N);

        newReceiverButtonAction.setEnabled(true);

        playReceiverButtonAction =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    playCurrentlySelectedReceiver();
                }
            };

        playReceiverButtonAction.putValue(
            Action.SHORT_DESCRIPTION, "Resumes the selected Node");
        playReceiverButtonAction.putValue(Action.NAME, "Resume");
        playReceiverButtonAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.ICON_RESUME_RECEIVER));
        playReceiverButtonAction.setEnabled(false);
        playReceiverButtonAction.putValue(
            Action.MNEMONIC_KEY, KeyEvent.VK_R);

        pauseReceiverButtonAction =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    pauseCurrentlySelectedReceiver();
                }
            };

        pauseReceiverButtonAction.putValue(
            Action.SHORT_DESCRIPTION,
            "Pause the selected Receiver.  All events received will be discarded.");
        pauseReceiverButtonAction.putValue(Action.NAME, "Pause");

        pauseReceiverButtonAction.putValue(
            Action.MNEMONIC_KEY, KeyEvent.VK_P);

        pauseReceiverButtonAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.PAUSE));
        pauseReceiverButtonAction.setEnabled(false);

        shutdownReceiverButtonAction =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    shutdownCurrentlySelectedReceiver();
                }
            };

        shutdownReceiverButtonAction.putValue(
            Action.SHORT_DESCRIPTION,
            "Shuts down the selected Receiver, and removes it from the Plugin registry");
        shutdownReceiverButtonAction.putValue(Action.NAME, "Shutdown");

        shutdownReceiverButtonAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.ICON_STOP_RECEIVER));
        shutdownReceiverButtonAction.putValue(
            Action.MNEMONIC_KEY, KeyEvent.VK_S);

        shutdownReceiverButtonAction.setEnabled(false);

        saveReceiversButtonAction =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    saveReceivers();
                }
            };

        saveReceiversButtonAction.putValue(
            Action.SHORT_DESCRIPTION,
            "Save the current receiver configuration");
        saveReceiversButtonAction.putValue(Action.NAME, "Save receivers");

        saveReceiversButtonAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.FILE_SAVE_AS));
        saveReceiversButtonAction.putValue(
            Action.MNEMONIC_KEY, KeyEvent.VK_V);


        restartReceiverButtonAction =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    ChainsawReceiver selectedReceiver = getCurrentlySelectedReceiver();
                    if (selectedReceiver == null) {
                        return;
                    }
                    selectedReceiver.shutdown();
                    selectedReceiver.start();
                    //allow the visual receiver to get a container on restart
                    if (selectedReceiver instanceof VisualReceiver) {
                        ((VisualReceiver) selectedReceiver).setContainer(ReceiversPanel.this);
                    }
                }
            };

        restartReceiverButtonAction.putValue(
            Action.SHORT_DESCRIPTION,
            "Restarts the selected Receiver");
        restartReceiverButtonAction.putValue(Action.NAME, "Restart");

        restartReceiverButtonAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.ICON_RESTART));
        restartReceiverButtonAction.putValue(
            Action.MNEMONIC_KEY, KeyEvent.VK_R);

        restartReceiverButtonAction.setEnabled(false);

        showReceiverHelpAction =
            new AbstractAction("Help") {
                public void actionPerformed(ActionEvent e) {
                    ChainsawReceiver receiver = getCurrentlySelectedReceiver();

                    if (receiver != null) {
                        HelpManager.getInstance().showHelpForClass(receiver.getClass());
                    }
                }
            };

        showReceiverHelpAction.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.HELP));
        showReceiverHelpAction.putValue(
            Action.SHORT_DESCRIPTION, "Displays the JavaDoc page for this Plugin");

        startAllAction =
            new AbstractAction(
                "(Re)start All Receivers", new ImageIcon(ChainsawIcons.ICON_RESTART_ALL)) {
                public void actionPerformed(ActionEvent e) {
                    if (
                        JOptionPane.showConfirmDialog(
                            null,
                            "This will cause any active Receiver to stop, and disconnect.  Is this ok?",
                            "Confirm", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                        new Thread(
                            () -> {
                                List<ChainsawReceiver> allReceivers = m_parent.getAllReceivers();

                                for (ChainsawReceiver rx : allReceivers) {
                                    rx.shutdown();
                                    rx.start();
                                }

                                updateReceiverTreeInDispatchThread();
                                m_statusBar.setMessage(
                                    "All Receivers have been (re)started");
                            }).start();
                    }
                }
            };

        startAllAction.putValue(
            Action.SHORT_DESCRIPTION,
            "Ensures that any Receiver that isn't active, is started, and any started action is stopped, and then restarted");

        receiversTree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        receiversTree.setCellRenderer(new ReceiverTreeCellRenderer());
        receiversTree.setRowHeight(19);

        buttonPanel = new ReceiverToolbar();
        receiversTree.addTreeSelectionListener(buttonPanel);

        PopupListener popupListener = new PopupListener(popupMenu);
        receiversTree.addMouseListener(popupListener);
        this.addMouseListener(popupListener);

        JComponent component = receiversTree;
        JScrollPane pane = new JScrollPane(component);

        splitter.setOrientation(JSplitPane.VERTICAL_SPLIT);

        splitter.setTopComponent(pane);
        splitter.setBottomComponent(pluginEditorPanel);

        splitter.setResizeWeight(0.7);
        add(buttonPanel, BorderLayout.NORTH);
        add(splitter, BorderLayout.CENTER);

        /**
         * This Tree likes to be notified when Socket's are accepted so
         * we listen for them and update the Tree.
         */
        SocketNodeEventListener listener =
            new SocketNodeEventListener() {
                public void socketOpened(String remoteInfo) {
                    updateReceiverTreeInDispatchThread();
                }

                public void socketClosedEvent(Exception e) {
                    updateReceiverTreeInDispatchThread();
                }
            };
    }

    private void saveReceivers() {
        File saveConfigFile = SwingHelper.promptForFile(this, null, "Save receiver configuration XML file", false);
        if (saveConfigFile != null) {
            m_parent.saveReceiversToFile(saveConfigFile);
        }
    }

    protected ReceiversTreeModel getReceiverTreeModel() {
        return ((ReceiversTreeModel) receiversTree.getModel());
    }

    /**
     *
     */
    protected void updateCurrentlySelectedNodeInDispatchThread() {
        SwingUtilities.invokeLater(
            () -> {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) receiversTree
                        .getLastSelectedPathComponent();

                if (node == null) {
                    return;
                }

                getReceiverTreeModel().nodeChanged(node);
                updateActions();
            });
    }

    /**
     * Returns the currently selected Receiver, or null if there is no
     * selected Receiver (this could be because a) nothing at all is selected
     * or b) a non Receiver type node is selected
     *
     * @return Receiver or null
     */
    private ChainsawReceiver getCurrentlySelectedReceiver() {
        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) receiversTree.getLastSelectedPathComponent();

        if (node == null) {
            return null;
        }

        Object userObject = node.getUserObject();

        if (userObject instanceof ChainsawReceiver) {
            return (ChainsawReceiver) userObject;
        }

        return null;
    }

    private ChainsawReceiver[] getSelectedReceivers() {
        TreePath[] paths = receiversTree.getSelectionPaths();
        Collection receivers = new ArrayList();

        for (TreePath path : paths) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) path.getLastPathComponent();

            if ((node != null) && node.getUserObject() instanceof ChainsawReceiver) {
                receivers.add(node.getUserObject());
            }
        }

        return (ChainsawReceiver[]) receivers.toArray(new ChainsawReceiver[0]);
    }

    /**
     * Returns the currently seleted node's User Object, or null
     * if there is no selected node, or if the currently selected node has
     * not user Object
     *
     * @return Object representing currently seleted Node's User Object
     */
    private Object getCurrentlySelectedUserObject() {
        DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) receiversTree.getLastSelectedPathComponent();

        if (node == null) {
            return null;
        }

        return node.getUserObject();
    }

    /**
     * Takes the currently selected Receiver and pauess it, effectively
     * discarding any received event BEFORE it is even posted to the logger
     * repository.
     *
     * The user is NOT asked to confirm this operation
     */
    private void pauseCurrentlySelectedReceiver() {
        getCurrentlySelectedReceiver().setPaused(true);
        updateCurrentlySelectedNodeInDispatchThread();
    }

    /**
     * Ensures that the currently selected receiver active property is set to
     * true
     */
    private void playCurrentlySelectedReceiver() {
        getCurrentlySelectedReceiver().setPaused(false);
        updateCurrentlySelectedNodeInDispatchThread();
    }

    /**
     * Takes the currently selected Receiver and stops it, which effectively
     * removes it from the PluginRegistry.
     *
     * The user is asked to confirm this operation
     */
    private void shutdownCurrentlySelectedReceiver() {
        if (
            JOptionPane.showConfirmDialog(
                null,
                "Are you sure you wish to shutdown this receiver?\n\nThis will disconnect any network resources, and remove it from the PluginRegistry.",
                "Confirm stop of Receiver", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            new Thread(
                () -> {
                    ChainsawReceiver[] receivers = getSelectedReceivers();

                    if (receivers != null) {
                        for (ChainsawReceiver receiver : receivers) {
                            receiver.shutdown();
                        }
                    }
                }).start();
        }
    }

    /**
     * Sets the state of actions depending on certain conditions (i.e what is
     * currently selected etc.)
     */
    private void updateActions() {
        Object object = getCurrentlySelectedUserObject();

        if ((object != null) && object instanceof ChainsawReceiver) {
            ChainsawReceiver pauseable = (ChainsawReceiver) object;

            if (!pauseable.getPaused()) {
                pauseReceiverButtonAction.setEnabled(true);
                playReceiverButtonAction.setEnabled(false);
            } else {
                pauseReceiverButtonAction.setEnabled(false);
                playReceiverButtonAction.setEnabled(true);
            }
        } else {
            pauseReceiverButtonAction.setEnabled(false);
            playReceiverButtonAction.setEnabled(false);
        }

        if (object instanceof ChainsawReceiver) {
            newReceiverButtonAction.setEnabled(true);
            shutdownReceiverButtonAction.setEnabled(true);
            restartReceiverButtonAction.setEnabled(true);
        } else {
            shutdownReceiverButtonAction.setEnabled(false);
            restartReceiverButtonAction.setEnabled(false);
        }
    }

    /**
     * Ensures that the Receiver tree is updated with the latest information
     * and that this operation occurs in the Swing Event Dispatch thread.
     */
    public void updateReceiverTreeInDispatchThread() {
        logger.debug(
            "updateReceiverTreeInDispatchThread, should not be needed now");

        //    if (SwingUtilities.isEventDispatchThread()) {
        //      updateReceiverTree.run();
        //    } else {
        //      SwingUtilities.invokeLater(updateReceiverTree);
        //    }
    }

    /* (non-Javadoc)
     * @see java.awt.Component#setVisible(boolean)
     */
    public void setVisible(boolean aFlag) {
        boolean oldValue = isVisible();
        super.setVisible(aFlag);
        firePropertyChange("visible", oldValue, isVisible());
    }

    public void loadSettings(LoadSettingsEvent event) {
    }

    /**
     * Saves all the receivers which are active at shut down as a configuration
     * file which can be loaded when Chainsaw will be restarted.
     */

    public void saveSettings(SaveSettingsEvent event) {
        File file = new File(SettingsManager.getInstance().getSettingsDirectory(), "receiver-config.xml");
        m_parent.saveReceiversToFile(file);
    }

    /**
     * A popup menu that allows the user to choose which
     * style of Receiver to create, which spawns a relevant Dialog
     * to enter the information and create the Receiver
     *
     * @author Paul Smith &lt;psmith@apache.org&gt;
     */
    class NewReceiverPopupMenu extends JPopupMenu {
        NewReceiverPopupMenu() {
            ServiceLoader<ChainsawReceiverFactory> sl = ServiceLoader.load(ChainsawReceiverFactory.class);
            
            for( ChainsawReceiverFactory crFactory : sl ){
                add(
                    new AbstractAction("New " + crFactory.getReceiverName() + "...") {
                        public void actionPerformed(ActionEvent e) {
                            Container container = SwingUtilities.getAncestorOfClass(JFrame.class, ReceiversPanel.this);
                            final JDialog dialog = new JDialog((JFrame) container, "New " + crFactory.getReceiverName() + "...", true);

                            try {
                                final NewReceiverDialogPanel panel =
                                    NewReceiverDialogPanel.create(crFactory);
                                dialog.getContentPane().add(panel);
                                dialog.pack();
                                SwingHelper.centerOnScreen(dialog);

                                /**
                                 * Make the default button the ok button
                                 */
                                dialog.getRootPane().setDefaultButton(panel.getOkPanel().getOkButton());

                                /**
                                 * Use the standard Cancel metaphor
                                 */
                                SwingHelper.configureCancelForDialog(dialog, panel.getOkPanel().getCancelButton());

                                final PropertyDescriptor[] descriptors = crFactory.getPropertyDescriptors();
                                panel.getOkPanel().getOkButton().addActionListener(
                                    e2 -> {
                                        ChainsawReceiver receiver = panel.getReceiver();
                                        if (receiver.getName() != null && !receiver.getName().trim().equals("")) {
                                            dialog.dispose();
                                            // Notify the LogUI that a new reciever has been created so it can spawn a new tab
                                            m_parent.addReceiver(receiver);
                                            receiver.start();
                                            m_classToProperties.put(receiver.getClass(), descriptors);
                                            m_statusBar.setMessage("Receiver '" + receiver.getName() + "' started");
                                        } else {
                                            logger.error("Name required to create receiver");
                                        }
                                    });
                                dialog.setVisible(true);
                            } catch (Exception e1) {
                                logger.error(
                                    "Failed to create the new Receiver dialog", e1);
                            }
                        }
                    });
                }
        }
    }

    /**
     * A popup menu class for when the user uses the popup trigger action
     * on a node in the Receiver tree.
     *
     * @author Paul Smith &lt;psmith@apache.org&gt;
     */
    class ReceiverPopupMenu extends JPopupMenu {
        ReceiverPopupMenu() {
        }

        /* (non-Javadoc)
         * @see javax.swing.JPopupMenu#show(java.awt.Component, int, int)
         */
        public void show(Component invoker, int x, int y) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) receiversTree.getLastSelectedPathComponent();

            if (node == null) {
                return;
            }

            Object userObject = node.getUserObject();
            removeAll();

            if (userObject == getRootOfTree().getUserObject()) {
                buildForReceiversRoot();
            } else if (getCurrentlySelectedReceiver() != null) {
                buildForReceiverNode();
            } else {
                return;
            }

            this.invalidate();
            this.validate();

            super.show(invoker, x, y);
        }

        /**
         *
         */
        private DefaultMutableTreeNode getRootOfTree() {
            return (DefaultMutableTreeNode) receiversTree.getModel().getRoot();
        }

        /**
         * Builds the popup menu with relevant items for a selected
         * Receiver node in the Tree.
         */
        private void buildForReceiverNode() {

            add(playReceiverButtonAction);
            add(pauseReceiverButtonAction);
            add(restartReceiverButtonAction);
            add(shutdownReceiverButtonAction);
            add(saveReceiversButtonAction);

            addSeparator();

            final ChainsawReceiver r = getCurrentlySelectedReceiver();
            add(createLevelRadioButton(r, Level.TRACE));
            add(createLevelRadioButton(r, Level.DEBUG));
            add(createLevelRadioButton(r, Level.INFO));
            add(createLevelRadioButton(r, Level.WARN));
            add(createLevelRadioButton(r, Level.ERROR));
            addSeparator();
            add(createLevelRadioButton(r, Level.OFF));
            add(createLevelRadioButton(r, Level.ALL));
            addSeparator();
            add(showReceiverHelpAction);
        }

        private JRadioButtonMenuItem createLevelRadioButton(
            final ChainsawReceiver r, final Level l) {
            Map<String, Icon> levelIconMap = LevelIconFactory.getInstance().getLevelToIconMap();

            Action action =
                new AbstractAction(
                    l.toString(), levelIconMap.get(l.toString())) {
                    public void actionPerformed(ActionEvent e) {
                        if (r != null) {
                            r.setThreshold(l);
                            updateCurrentlySelectedNodeInDispatchThread();
                        }
                    }
                };

            JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);
            item.setSelected(r.getThreshold() == l);

            return item;
        }

        /**
         * Builds a relevant set of menus for when the Root node in the Receiver
         * tree has been selected
         */
        private void buildForReceiversRoot() {
            JMenuItem startAll = new JMenuItem(startAllAction);

            add(newReceiverButtonAction);

            addSeparator();
            add(startAll);
        }
    }

    /**
     * A simple Panel that has toolbar buttons for restarting,
     * playing, pausing, and stoping receivers
     *
     * @author Paul Smith &lt;psmith@apache.org&gt;
     */
    private class ReceiverToolbar extends JToolBar
        implements TreeSelectionListener {
        final SmallButton newReceiverButton;

        private ReceiverToolbar() {
            setFloatable(false);

            SmallButton restartReceiverButton = new SmallButton(restartReceiverButtonAction);
            restartReceiverButton.setText(null);

            SmallButton shutdownReceiverButton =
                new SmallButton(shutdownReceiverButtonAction);
            shutdownReceiverButton.setText(null);

            SmallButton saveReceiversButton =
                new SmallButton(saveReceiversButtonAction);
            saveReceiversButton.setText(null);

            SmallButton restartAllButton = new SmallButton(startAllAction);
            restartAllButton.setText(null);


            newReceiverButton = new SmallButton(newReceiverButtonAction);
            newReceiverButton.setText(null);
            newReceiverButton.addMouseListener(new PopupListener(newReceiverPopup));

            add(newReceiverButton);
            add(restartAllButton);

            addSeparator();

            add(restartReceiverButton);
            add(shutdownReceiverButton);
            add(saveReceiversButton);

            addSeparator();

            add(Box.createHorizontalGlue());

            SmallButton closeButton = new SmallButton.Builder()
                .shortDescription("Closes the Receiver panel")
                .action(() -> ReceiversPanel.this.setVisible(false))
                .build();
            add(closeButton);

            add(Box.createHorizontalStrut(5));
        }

        /**
         * Ensures the enabled property of the actions is set properly
         * according to the currently selected node in the tree
         */
        public void valueChanged(TreeSelectionEvent e) {
            updateActions();
        }
    }
}
