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
/*
 */
package org.apache.log4j.chainsaw;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.filter.FilterModel;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LineIconFactory;
import org.apache.log4j.rule.AbstractRule;
import org.apache.log4j.rule.ColorRule;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;


/**
 * A panel that encapsulates the Logger Name tree, with associated actions
 * and implements the Rule interface so that it can filter in/out events
 * that do not match the users request for refining the view based on Loggers.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
final class LoggerNameTreePanel extends JPanel implements LoggerNameListener {
    //~ Static fields/initializers ==============================================

    private static final int WARN_DEPTH = 4;

    //~ Instance fields =========================================================

    private LoggerNameTreeCellRenderer cellRenderer =
        new LoggerNameTreeCellRenderer();
    private final Action clearIgnoreListAction;
    private final Action closeAction;
    private final JButton closeButton = new SmallButton();
    private final Action collapseAction;
    private final JButton collapseButton = new SmallButton();
    private final Action editLoggerAction;
    private final JButton editLoggerButton = new SmallButton();
    private final Action expandAction;
    private final Action findAction;
    private final Action clearFindNextAction;
    private final Action defineColorRuleForLoggerAction;
    private final Action setRefineFocusAction;
    private final Action updateRefineFocusAction;
    private final Action updateFindAction;
    private final JButton expandButton = new SmallButton();
    private final Action focusOnAction;
    private final Action clearRefineFocusAction;
    private final SmallToggleButton focusOnLoggerButton =
        new SmallToggleButton();
    private final Set hiddenSet = new HashSet();
    private final Action hideAction;
    private final Action hideSubLoggersAction;
    private final AbstractConfiguration m_panelConfig;

    private final JList ignoreList = new JList();
    private final JEditorPane ignoreExpressionEntryField = new JEditorPane();
    private final JEditorPane alwaysDisplayExpressionEntryField = new JEditorPane();
    private final JScrollPane ignoreListScroll = new JScrollPane(ignoreList);
    private final JDialog ignoreDialog = new JDialog();
    private final JDialog ignoreExpressionDialog = new JDialog();
    private final JDialog alwaysDisplayExpressionDialog = new JDialog();
    private final JLabel ignoreSummary = new JLabel("0 hidden loggers");
    private final JLabel ignoreExpressionSummary = new JLabel("Ignore expression");
    private final JLabel alwaysDisplayExpressionSummary = new JLabel("Always displayed expression");
    private final SmallToggleButton ignoreLoggerButton = new SmallToggleButton();
    private final EventListenerList listenerList = new EventListenerList();
    private final JTree logTree;
    private final Logger logger = LogManager.getLogger(LoggerNameTreePanel.class);

    //  private final EventListenerList focusOnActionListeners =
    //    new EventListenerList();
    private final LogPanelLoggerTreeModel logTreeModel;
    private final PopupListener popupListener;
    private final LoggerTreePopupMenu popupMenu;
    private final VisibilityRuleDelegate visibilityRuleDelegate;
    private Rule colorRuleDelegate;
    private final JScrollPane scrollTree;
    private final JToolBar toolbar = new JToolBar();
    private final LogPanel logPanel;
    private final RuleColorizer colorizer;
    private Rule ignoreExpressionRule;
    private Rule alwaysDisplayExpressionRule;
    private boolean expandRootLatch = false;
    private String currentlySelectedLoggerName;

    //~ Constructors ============================================================

    /**
     * Creates a new LoggerNameTreePanel object.
     *
     * @param logTreeModel
     */
    LoggerNameTreePanel(LogPanelLoggerTreeModel logTreeModel, 
            AbstractConfiguration panelConfiguration,
            LogPanel logPanel,
            RuleColorizer colorizer,
            FilterModel filterModel) {
        super();
        this.logTreeModel = logTreeModel;
        m_panelConfig = panelConfiguration;
        this.logPanel = logPanel;
        this.colorizer = colorizer;

        setLayout(new BorderLayout());
        ignoreExpressionEntryField.setPreferredSize(new Dimension(300, 150));
        alwaysDisplayExpressionEntryField.setPreferredSize(new Dimension(300, 150));
        alwaysDisplayExpressionSummary.setMinimumSize(new Dimension(10, alwaysDisplayExpressionSummary.getHeight()));
        ignoreExpressionSummary.setMinimumSize(new Dimension(10, ignoreExpressionSummary.getHeight()));
        ignoreSummary.setMinimumSize(new Dimension(10, ignoreSummary.getHeight()));

        JTextComponentFormatter.applySystemFontAndSize(ignoreExpressionEntryField);
        JTextComponentFormatter.applySystemFontAndSize(alwaysDisplayExpressionEntryField);

        visibilityRuleDelegate = new VisibilityRuleDelegate();
        colorRuleDelegate =
            new AbstractRule() {
                public boolean evaluate(ChainsawLoggingEvent e, Map matches) {
                    boolean hiddenLogger = e.m_logger != null && isHiddenLogger(e.m_logger);
                    boolean hiddenExpression = (ignoreExpressionRule != null && ignoreExpressionRule.evaluate(e, null));
                    boolean alwaysDisplayExpression = (alwaysDisplayExpressionRule != null && alwaysDisplayExpressionRule.evaluate(e, null));
                    boolean hidden = (!alwaysDisplayExpression) && (hiddenLogger || hiddenExpression);
                    String currentlySelectedLoggerName = getCurrentlySelectedLoggerName();

                    return !isFocusOnSelected() && !hidden && currentlySelectedLoggerName != null && !"".equals(currentlySelectedLoggerName) && (e.m_logger.startsWith(currentlySelectedLoggerName + ".") || e.m_logger.endsWith(currentlySelectedLoggerName));
                }
            };

        logTree =
            new JTree(logTreeModel) {
                public String getToolTipText(MouseEvent ev) {
                    if (ev == null) {
                        return null;
                    }

                    TreePath path = logTree.getPathForLocation(ev.getX(), ev.getY());

                    String loggerName = getLoggerName(path);

                    if (hiddenSet.contains(loggerName)) {
                        loggerName += " (you are ignoring this logger)";
                    }

                    return loggerName;
                }
            };

        ToolTipManager.sharedInstance().registerComponent(logTree);
        logTree.setCellRenderer(cellRenderer);

        //	============================================
        logTreeModel.addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) {
            }

            public void treeNodesInserted(TreeModelEvent e) {
                if (!expandRootLatch) {
                    ensureRootExpanded();
                    expandRootLatch = true;
                }
            }

            public void treeNodesRemoved(TreeModelEvent e) {
            }

            public void treeStructureChanged(TreeModelEvent e) {
            }
        });

        logTree.setEditable(false);

        //	TODO decide if Multi-selection is useful, and how it would work
        TreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
        selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        logTree.setSelectionModel(selectionModel);

        logTree.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        scrollTree = new JScrollPane(logTree);
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));

        expandAction = createExpandAction();
        findAction = createFindNextAction();
        clearFindNextAction = createClearFindNextAction();
        defineColorRuleForLoggerAction = createDefineColorRuleForLoggerAction();
        clearRefineFocusAction = createClearRefineFocusAction();
        setRefineFocusAction = createSetRefineFocusAction();
        updateRefineFocusAction = createUpdateRefineFocusAction();
        updateFindAction = createUpdateFindAction();
        editLoggerAction = createEditLoggerAction();
        closeAction = createCloseAction();
        collapseAction = createCollapseAction();
        focusOnAction = createFocusOnAction();
        hideAction = createIgnoreAction();
        hideSubLoggersAction = createIgnoreAllAction();
        clearIgnoreListAction = createClearIgnoreListAction();

        popupMenu = new LoggerTreePopupMenu();
        popupListener = new PopupListener(popupMenu);

        setupListeners();
        configureToolbarPanel();

        add(toolbar, BorderLayout.NORTH);
        add(scrollTree, BorderLayout.CENTER);

        ignoreDialog.setTitle("Hidden/Ignored Loggers");
        ignoreDialog.setModal(true);

        ignoreExpressionDialog.setTitle("Hidden/Ignored Expression");
        ignoreExpressionDialog.setModal(true);

        alwaysDisplayExpressionDialog.setTitle("Always displayed Expression");
        alwaysDisplayExpressionDialog.setModal(true);

        JPanel ignorePanel = new JPanel();
        ignorePanel.setLayout(new BoxLayout(ignorePanel, BoxLayout.Y_AXIS));
        JPanel ignoreSummaryPanel = new JPanel();
        ignoreSummaryPanel.setLayout(new BoxLayout(ignoreSummaryPanel, BoxLayout.X_AXIS));
        ignoreSummaryPanel.add(ignoreSummary);

        Action showIgnoreDialogAction = new AbstractAction("...") {
            public void actionPerformed(ActionEvent e) {
                LogPanel.centerAndSetVisible(ignoreDialog);
            }
        };

        Action showIgnoreExpressionDialogAction = new AbstractAction("...") {
            public void actionPerformed(ActionEvent e) {
                LogPanel.centerAndSetVisible(ignoreExpressionDialog);
            }
        };


        Action showAlwaysDisplayExpressionDialogAction = new AbstractAction("...") {
            public void actionPerformed(ActionEvent e) {
                LogPanel.centerAndSetVisible(alwaysDisplayExpressionDialog);
            }
        };

        showIgnoreDialogAction.putValue(Action.SHORT_DESCRIPTION, "Click to view and manage your hidden/ignored loggers");
        JButton btnShowIgnoreDialog = new SmallButton(showIgnoreDialogAction);

        ignoreSummaryPanel.add(btnShowIgnoreDialog);
        ignorePanel.add(ignoreSummaryPanel);

        JPanel ignoreExpressionPanel = new JPanel();
        ignoreExpressionPanel.setLayout(new BoxLayout(ignoreExpressionPanel, BoxLayout.X_AXIS));
        ignoreExpressionPanel.add(ignoreExpressionSummary);
        showIgnoreExpressionDialogAction.putValue(Action.SHORT_DESCRIPTION, "Click to view and manage your hidden/ignored expression");
        JButton btnShowIgnoreExpressionDialog = new SmallButton(showIgnoreExpressionDialogAction);
        ignoreExpressionPanel.add(btnShowIgnoreExpressionDialog);

        ignorePanel.add(ignoreExpressionPanel);

        JPanel alwaysDisplayExpressionPanel = new JPanel();
        alwaysDisplayExpressionPanel.setLayout(new BoxLayout(alwaysDisplayExpressionPanel, BoxLayout.X_AXIS));
        alwaysDisplayExpressionPanel.add(alwaysDisplayExpressionSummary);
        showAlwaysDisplayExpressionDialogAction.putValue(Action.SHORT_DESCRIPTION, "Click to view and manage your always-displayed expression");
        JButton btnShowAlwaysDisplayExpressionDialog = new SmallButton(showAlwaysDisplayExpressionDialogAction);
        alwaysDisplayExpressionPanel.add(btnShowAlwaysDisplayExpressionDialog);

        ignorePanel.add(alwaysDisplayExpressionPanel);

        add(ignorePanel, BorderLayout.SOUTH);

        ignoreList.setModel(new DefaultListModel());
        ignoreList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (
                    (e.getClickCount() > 1)
                        && ((e.getModifiers() & InputEvent.BUTTON1_MASK) > 0)) {
                    int index = ignoreList.locationToIndex(e.getPoint());

                    if (index >= 0) {
                        String string =
                            ignoreList.getModel().getElementAt(index).toString();
                        toggleHiddenLogger(string);
                        fireChangeEvent();

                        /**
                         * TODO this needs to get the node that has this logger and fire a visual update
                         */
                        LoggerNameTreePanel.this.logTreeModel.nodeStructureChanged(
                            (TreeNode) LoggerNameTreePanel.this.logTreeModel.getRoot());
                    }
                }
            }
        });

        JPanel ignoreListPanel = new JPanel(new BorderLayout());
        ignoreListScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Double click an entry to unhide it"));
        ignoreListPanel.add(ignoreListScroll, BorderLayout.CENTER);

        JPanel ignoreExpressionDialogPanel = new JPanel(new BorderLayout());
        ignoreExpressionEntryField.addKeyListener(new ExpressionRuleContext(filterModel, ignoreExpressionEntryField));

        ignoreExpressionDialogPanel.add(new JScrollPane(ignoreExpressionEntryField), BorderLayout.CENTER);
        JButton ignoreExpressionCloseButton = new JButton(new AbstractAction(" Close ") {
            public void actionPerformed(ActionEvent e) {
                String ignoreText = ignoreExpressionEntryField.getText();

                if (updateIgnoreExpression(ignoreText)) {
                    ignoreExpressionDialog.setVisible(false);
                }
            }
        });


        JPanel alwaysDisplayExpressionDialogPanel = new JPanel(new BorderLayout());
        alwaysDisplayExpressionEntryField.addKeyListener(new ExpressionRuleContext(filterModel, alwaysDisplayExpressionEntryField));

        alwaysDisplayExpressionDialogPanel.add(new JScrollPane(alwaysDisplayExpressionEntryField), BorderLayout.CENTER);
        JButton alwaysDisplayExpressionCloseButton = new JButton(new AbstractAction(" Close ") {
            public void actionPerformed(ActionEvent e) {
                String alwaysDisplayText = alwaysDisplayExpressionEntryField.getText();

                if (updateAlwaysDisplayExpression(alwaysDisplayText)) {
                    alwaysDisplayExpressionDialog.setVisible(false);
                }
            }
        });

        JPanel closeAlwaysDisplayExpressionPanel = new JPanel();
        closeAlwaysDisplayExpressionPanel.add(alwaysDisplayExpressionCloseButton);
        alwaysDisplayExpressionDialogPanel.add(closeAlwaysDisplayExpressionPanel, BorderLayout.SOUTH);

        JPanel closeIgnoreExpressionPanel = new JPanel();
        closeIgnoreExpressionPanel.add(ignoreExpressionCloseButton);
        ignoreExpressionDialogPanel.add(closeIgnoreExpressionPanel, BorderLayout.SOUTH);

        Box ignoreListButtonPanel = Box.createHorizontalBox();

        JButton unhideAll = new JButton(new AbstractAction(" Unhide All ") {

            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(() -> clearIgnoreListAction.actionPerformed(e));

            }
        });
        ignoreListButtonPanel.add(unhideAll);

        ignoreListButtonPanel.add(Box.createHorizontalGlue());
        JButton ignoreCloseButton = new JButton(new AbstractAction(" Close ") {

            public void actionPerformed(ActionEvent e) {
                ignoreDialog.setVisible(false);

            }
        });
        ignoreListButtonPanel.add(ignoreCloseButton);


        ignoreListPanel.add(ignoreListButtonPanel, BorderLayout.SOUTH);


        ignoreDialog.getContentPane().add(ignoreListPanel);
        ignoreDialog.pack();

        ignoreExpressionDialog.getContentPane().add(ignoreExpressionDialogPanel);
        ignoreExpressionDialog.pack();

        alwaysDisplayExpressionDialog.getContentPane().add(alwaysDisplayExpressionDialogPanel);
        alwaysDisplayExpressionDialog.pack();
    }

    private boolean updateIgnoreExpression(String ignoreText) {
        try {
            if (ignoreText != null && !ignoreText.trim().equals("")) {
                ignoreExpressionRule = ExpressionRule.getRule(ignoreText);
            } else {
                ignoreExpressionRule = null;
            }
            visibilityRuleDelegate.firePropertyChange("hiddenSet", null, null);

            updateDisplay();
            ignoreExpressionEntryField.setBackground(UIManager.getColor("TextField.background"));
            return true;
        } catch (IllegalArgumentException iae) {
            ignoreExpressionEntryField.setToolTipText(iae.getMessage());
            ignoreExpressionEntryField.setBackground(ChainsawConstants.INVALID_EXPRESSION_BACKGROUND);
            return false;
        }
    }

    private boolean updateAlwaysDisplayExpression(String alwaysDisplayText) {
        try {
            if (alwaysDisplayText != null && !alwaysDisplayText.trim().equals("")) {
                alwaysDisplayExpressionRule = ExpressionRule.getRule(alwaysDisplayText);
            } else {
                alwaysDisplayExpressionRule = null;
            }
            visibilityRuleDelegate.firePropertyChange("alwaysDisplayedSet", null, null);

            updateDisplay();
            alwaysDisplayExpressionEntryField.setBackground(UIManager.getColor("TextField.background"));
            return true;
        } catch (IllegalArgumentException iae) {
            alwaysDisplayExpressionEntryField.setToolTipText(iae.getMessage());
            alwaysDisplayExpressionEntryField.setBackground(ChainsawConstants.INVALID_EXPRESSION_BACKGROUND);
            return false;
        }
    }

    //~ Methods =================================================================

    /**
     * Adds a change Listener to this LoggerNameTreePanel to be notfied
     * when the State of the Focus or Hidden details have changed.
     *
     * @param l
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public Rule getLoggerColorRule() {
        return colorRuleDelegate;
    }

    public Rule getLoggerVisibilityRule() {
        return visibilityRuleDelegate;
    }

    /**
     * DOCUMENT ME!
     *
     * @param l DOCUMENT ME!
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Ensures the Focus is set to a specific logger name
     *
     * @param
     */
    public void setFocusOn(String newLogger) {
        DefaultMutableTreeNode node = logTreeModel.lookupLogger(newLogger);

        if (node != null) {
            TreeNode[] nodes = node.getPath();
            TreePath treePath = new TreePath(nodes);
            logTree.setSelectionPath(treePath);

            if (!focusOnLoggerButton.isSelected()) {
                focusOnLoggerButton.doClick();
            }
        } else {
            logger.error("failed to lookup logger " + newLogger);
        }
    }

    private boolean isHiddenLogger(String loggerName) {
        for (Object aHiddenSet : hiddenSet) {
            String hiddenLoggerEntry = aHiddenSet.toString();
            if (loggerName.startsWith(hiddenLoggerEntry + ".") || loggerName.endsWith(hiddenLoggerEntry)) {
                return true;
            }
        }
        return false;
    }


    /**
     * DOCUMENT ME!
     *
     * @param logger
     */
    protected void toggleHiddenLogger(String logger) {
        if (!hiddenSet.contains(logger)) {
            hiddenSet.add(logger);
        } else {
            hiddenSet.remove(logger);
        }

        visibilityRuleDelegate.firePropertyChange("hiddenSet", null, null);
    }

    /**
     * Returns the full name of the Logger that is represented by
     * the currently selected Logger node in the tree.
     * <p>
     * This is the dotted name, of the current node including all it's parents.
     * <p>
     * If multiple Nodes are selected, the first path is used
     *
     * @return Logger Name or null if nothing selected
     */
    String getCurrentlySelectedLoggerName() {
        TreePath[] paths = logTree.getSelectionPaths();

        if ((paths == null) || (paths.length == 0)) {
            return null;
        }

        TreePath firstPath = paths[0];

        return getLoggerName(firstPath);
    }

    /**
     * Returns the full
     *
     * @param path DOCUMENT ME!
     * @return
     */
    String getLoggerName(TreePath path) {
        if (path != null) {
            Object[] objects = path.getPath();
            StringBuilder buf = new StringBuilder();

            for (int i = 1; i < objects.length; i++) {
                buf.append(objects[i].toString());

                if (i < (objects.length - 1)) {
                    buf.append(".");
                }
            }

            return buf.toString();
        }

        return null;
    }

    /**
     * adds a Collection of Strings to the ignore List and notifise all listeners of
     * both the "hiddenSet" property and those expecting the Rule to change
     * via the ChangeListener interface
     *
     * @param fqnLoggersToIgnore
     */
    void ignore(Collection fqnLoggersToIgnore) {
        hiddenSet.addAll(fqnLoggersToIgnore);
        visibilityRuleDelegate.firePropertyChange("hiddenSet", null, null);
        fireChangeEvent();
    }

    /**
     * Returns true if the FocusOn element has been selected
     *
     * @return true if the FocusOn action/lement has been selected
     */
    boolean isFocusOnSelected() {
        return focusOnAction.getValue("checked") != null;
    }

    void setFocusOnSelected(boolean selected) {
        if (selected) {
            focusOnAction.putValue("checked", Boolean.TRUE);
        } else {
            focusOnAction.putValue("checked", null);
        }
    }

    /**
     * Given the currently selected nodes
     * collapses all the children of those nodes.
     */
    private void collapseCurrentlySelectedNode() {
        TreePath[] paths = logTree.getSelectionPaths();

        if (paths == null) {
            return;
        }

        logger.debug("Collapsing all children of selected node");

        for (TreePath path : paths) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) path.getLastPathComponent();
            Enumeration enumeration = node.depthFirstEnumeration();

            while (enumeration.hasMoreElements()) {
                DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) enumeration.nextElement();

                if ((child.getParent() != null) && (child != node)) {
                    TreeNode[] nodes =
                        ((DefaultMutableTreeNode) child.getParent()).getPath();

                    TreePath treePath = new TreePath(nodes);
                    logTree.collapsePath(treePath);
                }
            }
        }

        ensureRootExpanded();
    }

    /**
     * configures all the components that are used in the mini-toolbar of this
     * component
     */
    private void configureToolbarPanel() {
        toolbar.setFloatable(false);

        expandButton.setAction(expandAction);
        expandButton.setText(null);
        collapseButton.setAction(collapseAction);
        collapseButton.setText(null);
        focusOnLoggerButton.setAction(focusOnAction);
        focusOnLoggerButton.setText(null);
        ignoreLoggerButton.setAction(hideAction);
        ignoreLoggerButton.setText(null);

        expandButton.setFont(expandButton.getFont().deriveFont(Font.BOLD));
        collapseButton.setFont(collapseButton.getFont().deriveFont(Font.BOLD));

        editLoggerButton.setAction(editLoggerAction);
        editLoggerButton.setText(null);
        closeButton.setAction(closeAction);
        closeButton.setText(null);

        toolbar.add(expandButton);
        toolbar.add(collapseButton);
        toolbar.addSeparator();
        toolbar.add(focusOnLoggerButton);
        toolbar.add(ignoreLoggerButton);

        //    toolbar.add(editLoggerButton);
        toolbar.addSeparator();

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(closeButton);
        toolbar.add(Box.createHorizontalStrut(5));
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    private Action createClearIgnoreListAction() {
        Action action = new AbstractAction("Clear Ignore list", null) {
            public void actionPerformed(ActionEvent e) {
                ignoreLoggerButton.setSelected(false);
                logTreeModel.reload();
                hiddenSet.clear();
                fireChangeEvent();
            }
        };

        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Removes all entries from the Ignore list so you can see their events in the view");

        return action;
    }

    /**
     * An action that closes (hides) this panel
     *
     * @return
     */
    private Action createCloseAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                m_panelConfig.setProperty("logpanel.logTreePanelVisible", false);
            }
        };

        action.putValue(Action.NAME, "Close");
        action.putValue(Action.SHORT_DESCRIPTION, "Closes the Logger panel");
        action.putValue(Action.SMALL_ICON, LineIconFactory.createCloseIcon());

        return action;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    private Action createCollapseAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                collapseCurrentlySelectedNode();
            }
        };

        action.putValue(Action.SMALL_ICON, LineIconFactory.createCollapseIcon());
        action.putValue(Action.NAME, "Collapse Branch");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Collapses all the children of the currently selected node");
        action.setEnabled(false);

        return action;
    }

    private Action createEditLoggerAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
            }
        };

        //    TODO enable this when it's ready.
        action.putValue("enabled", Boolean.FALSE);

        action.putValue(Action.NAME, "Edit filters/colors");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Allows you to specify filters and coloring for this Logger");
        action.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.ICON_EDIT_RECEIVER));
        action.setEnabled(false);

        return action;
    }

    /**
     * Creates an action that is used to expand the selected node
     * and all children
     *
     * @return an Action
     */
    private Action createExpandAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                expandCurrentlySelectedNode();
            }
        };

        action.putValue(Action.SMALL_ICON, LineIconFactory.createExpandIcon());
        action.putValue(Action.NAME, "Expand branch");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Expands all the child nodes of the currently selected node, recursively");
        action.setEnabled(false);

        return action;
    }

    /**
     * Creates an action that is used to find the next match of the selected node (similar to default selection behavior
     * except the search field is populated and the next match is selected.
     *
     * @return an Action
     */
    private Action createFindNextAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                findNextUsingCurrentlySelectedNode();
            }
        };

        action.putValue(Action.NAME, "Find next");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Find using the selected node");
        action.setEnabled(false);

        return action;
    }

    private Action createSetRefineFocusAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setRefineFocusUsingCurrentlySelectedNode();
            }
        };

        action.putValue(Action.NAME, "Set 'refine focus' to selected logger");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Refine focus on the selected node");
        action.setEnabled(false);

        return action;
    }

    private Action createUpdateRefineFocusAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                updateRefineFocusUsingCurrentlySelectedNode();
            }
        };

        action.putValue(Action.NAME, "Update 'refine focus' to include selected logger");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Add selected node to 'refine focus' field");
        action.setEnabled(false);

        return action;
    }

    private Action createUpdateFindAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                updateFindUsingCurrentlySelectedNode();
            }
        };

        action.putValue(Action.NAME, "Update 'find' to include selected logger");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Add selected node to 'find' field");
        action.setEnabled(false);

        return action;
    }

    private void updateFindUsingCurrentlySelectedNode() {
        String selectedLogger = getCurrentlySelectedLoggerName();
        TreePath[] paths = logTree.getSelectionPaths();

        if (paths == null) {
            return;
        }
        String currentFindText = logPanel.getFindText();
        logPanel.setFindText(currentFindText + " || logger ~= " + selectedLogger);
    }

    private void updateRefineFocusUsingCurrentlySelectedNode() {
        String selectedLogger = getCurrentlySelectedLoggerName();
        TreePath[] paths = logTree.getSelectionPaths();

        if (paths == null) {
            return;
        }
        String currentFilterText = logPanel.getRefineFocusText();
        logPanel.setRefineFocusText(currentFilterText + " || logger ~= " + selectedLogger);
    }

    private void setRefineFocusUsingCurrentlySelectedNode() {
        String selectedLogger = getCurrentlySelectedLoggerName();
        TreePath[] paths = logTree.getSelectionPaths();

        if (paths == null) {
            return;
        }
        logPanel.setRefineFocusText("logger ~= " + selectedLogger);
    }

    private Action createDefineColorRuleForLoggerAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String selectedLogger = getCurrentlySelectedLoggerName();
                TreePath[] paths = logTree.getSelectionPaths();

                if (paths == null) {
                    return;
                }
                Color c = JColorChooser.showDialog(getRootPane(), "Choose a color", Color.red);
                if (c != null) {
                    String expression = "logger like '^" + selectedLogger + ".*'";
                    colorizer.addRule(new ColorRule(expression,
                        ExpressionRule.getRule(expression), c, ChainsawConstants.COLOR_DEFAULT_FOREGROUND));
                }
            }
        };

        action.putValue(Action.NAME, "Define color rule for selected logger");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Define color rule for logger");
        action.setEnabled(false);
        return action;
    }

    /**
     * Creates an action that is used to find the next match of the selected node (similar to default selection behavior
     * except the search field is populated and the next match is selected.
     *
     * @return an Action
     */
    private Action createClearFindNextAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                clearFindNext();
            }
        };

        action.putValue(Action.NAME, "Clear find field");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Clear the find field");
        action.setEnabled(false);

        return action;
    }

    private Action createClearRefineFocusAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                clearRefineFocus();
            }
        };

        action.putValue(Action.NAME, "Clear 'refine focus' field");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Clear the refine focus field");
        action.setEnabled(false);

        return action;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    private Action createFocusOnAction() {
        final Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                toggleFocusOnState();
            }
        };

        action.putValue(Action.NAME, "Focus");
        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Allows you to Focus on the selected logger by setting a filter that discards all but this Logger");
        action.putValue(
            Action.SMALL_ICON, new ImageIcon(ChainsawIcons.WINDOW_ICON));

        action.setEnabled(false);

        return action;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    private Action createIgnoreAllAction() {
        Action action =
            new AbstractAction(
                "Ignore loggers below selection") {
                public void actionPerformed(ActionEvent e) {
                    //add all top level loggers as hidden loggers
                    TreePath[] paths = logTree.getSelectionPaths();

                    StringBuilder parentPathString = new StringBuilder();
                    DefaultMutableTreeNode root;
                    if ((paths == null) || (paths.length == 0)) {
                        root = (DefaultMutableTreeNode) logTreeModel.getRoot();
                    } else {
                        root = (DefaultMutableTreeNode) logTree.getSelectionPath().getLastPathComponent();
                        TreeNode[] path = root.getPath();
                        //don't add 'root logger' to path string
                        for (int i = 1; i < path.length; i++) {
                            if (i > 1) {
                                parentPathString.append(".");
                            }
                            parentPathString.append(path[i].toString());
                        }
                        if (!(parentPathString.toString().equals(""))) {
                            parentPathString.append(".");
                        }
                    }
                    Enumeration topLevelLoggersEnumeration = root.children();
                    Set topLevelLoggersSet = new HashSet();
                    while (topLevelLoggersEnumeration.hasMoreElements()) {
                        String thisLogger = topLevelLoggersEnumeration.nextElement().toString();
                        topLevelLoggersSet.add(parentPathString + thisLogger);
                    }
                    if (topLevelLoggersSet.size() > 0) {
                        ignore(topLevelLoggersSet);
                    }

                    logTreeModel.nodeChanged(root);
                    ignoreLoggerButton.setSelected(false);
                    focusOnAction.setEnabled(false);
                    popupMenu.hideCheck.setSelected(false);
                    fireChangeEvent();
                }
            };

        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Adds all loggers to your Ignore list (unhide loggers you want to see in the view)");

        return action;
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    private Action createIgnoreAction() {
        Action action =
            new AbstractAction(
                "Ignore this Logger", new ImageIcon(ChainsawIcons.ICON_COLLAPSE)) {
                public void actionPerformed(ActionEvent e) {
                    String logger = getCurrentlySelectedLoggerName();

                    if (logger != null) {
                        toggleHiddenLogger(logger);
                        logTreeModel.nodeChanged(
                            (TreeNode) logTree.getSelectionPath().getLastPathComponent());
                        ignoreLoggerButton.setSelected(hiddenSet.contains(logger));
                        focusOnAction.setEnabled(!hiddenSet.contains(logger));
                        popupMenu.hideCheck.setSelected(hiddenSet.contains(logger));
                    }

                    fireChangeEvent();
                }
            };

        action.putValue(
            Action.SHORT_DESCRIPTION,
            "Adds the selected Logger to your Ignore list, filtering those events from view");

        return action;
    }

    private void ensureRootExpanded() {
        logger.debug("Ensuring Root node is expanded.");

        final DefaultMutableTreeNode root =
            (DefaultMutableTreeNode) logTreeModel.getRoot();
        SwingUtilities.invokeLater(() -> logTree.expandPath(new TreePath(root)));
    }

    private void findNextUsingCurrentlySelectedNode() {
        String selectedLogger = getCurrentlySelectedLoggerName();
        TreePath[] paths = logTree.getSelectionPaths();

        if (paths == null) {
            return;
        }
        logPanel.setFindText("logger like '^" + selectedLogger + ".*'");
    }

    private void clearFindNext() {
        logPanel.setFindText("");
    }

    private void clearRefineFocus() {
        logPanel.setRefineFocusText("");
    }

    /**
     * Expands the currently selected node (if any)
     * including all the children.
     */
    private void expandCurrentlySelectedNode() {
        TreePath[] paths = logTree.getSelectionPaths();

        if (paths == null) {
            return;
        }

        logger.debug("Expanding all children of selected node");

        for (TreePath path : paths) {
            /**
             * TODO this is commented out, right now it expands all nodes including the root, so if there is a large tree..... look out.
             */

            //      /**
            //       * Handle an expansion of the Root node by only doing the first level.
            //       * Safe...
            //       */
            //      if (path.getPathCount() == 1) {
            //        logTree.expandPath(path);
            //
            //        return;
            //      }

            DefaultMutableTreeNode treeNode =
                (DefaultMutableTreeNode) path.getLastPathComponent();

            Enumeration depthEnum = treeNode.depthFirstEnumeration();

            if (!depthEnum.hasMoreElements()) {
                break;
            }

            List<Integer> depths = new ArrayList<>();

            while (depthEnum.hasMoreElements()) {
                depths.add(
                    ((DefaultMutableTreeNode) depthEnum.nextElement()).getDepth());
            }

            Collections.sort(depths);
            Collections.reverse(depths);

            int maxDepth = depths.get(0);

            if (maxDepth > WARN_DEPTH) {
                logger.warn("Should warn user, depth=" + maxDepth);
            }

            depthEnum = treeNode.depthFirstEnumeration();

            while (depthEnum.hasMoreElements()) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) depthEnum.nextElement();

                if (node.isLeaf() && node.getParent() != null) {
                    TreeNode[] nodes =
                        ((DefaultMutableTreeNode) node.getParent()).getPath();
                    TreePath treePath = new TreePath(nodes);

                    logger.debug("Expanding path:" + treePath);

                    logTree.expandPath(treePath);
                }
            }
        }
    }

    private void fireChangeEvent() {
        ChangeListener[] listeners =
            listenerList.getListeners(ChangeListener.class);
        ChangeEvent e = null;

        for (ChangeListener listener : listeners) {
            if (e == null) {
                e = new ChangeEvent(this);
            }

            listener.stateChanged(e);
        }
    }

    private void reconfigureMenuText() {
        String logger = getCurrentlySelectedLoggerName();

        if ((logger == null) || (logger.length() == 0)) {
            focusOnAction.putValue(Action.NAME, "Focus On...");
            hideAction.putValue(Action.NAME, "Ignore...");
            findAction.putValue(Action.NAME, "Find...");
            setRefineFocusAction.putValue(Action.NAME, "Set refine focus field");
            updateRefineFocusAction.putValue(Action.NAME, "Add to refine focus field");
            updateFindAction.putValue(Action.NAME, "Add to find field");
            defineColorRuleForLoggerAction.putValue(Action.NAME, "Define color rule");
        } else {
            focusOnAction.putValue(Action.NAME, "Focus On '" + logger + "'");
            hideAction.putValue(Action.NAME, "Ignore '" + logger + "'");
            findAction.putValue(Action.NAME, "Find '" + logger + "'");
            setRefineFocusAction.putValue(Action.NAME, "Set refine focus field to '" + logger + "'");
            updateRefineFocusAction.putValue(Action.NAME, "Add '" + logger + "' to 'refine focus' field");
            updateFindAction.putValue(Action.NAME, "Add '" + logger + "' to 'find' field");
            defineColorRuleForLoggerAction.putValue(Action.NAME, "Define color rule for '" + logger + "'");
        }

        // need to ensure the button doens't update itself with the text, looks stupid otherwise
        hideSubLoggersAction.putValue(Action.NAME, "Ignore loggers below selection");
        focusOnLoggerButton.setText(null);
        ignoreLoggerButton.setText(null);
    }

    /**
     * Configures varoius listeners etc for the components within
     * this Class.
     */
    private void setupListeners() {
        logTree.addMouseMotionListener(new MouseKeyIconListener());

        /**
         * Enable the actions depending on state of the tree selection
         */
        logTree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            TreeNode node = null;

            if (path != null) {
                node = (TreeNode) path.getLastPathComponent();
            }
            boolean focusOnSelected = isFocusOnSelected();
            //          editLoggerAction.setEnabled(path != null);
            currentlySelectedLoggerName = getCurrentlySelectedLoggerName();
            focusOnAction.setEnabled(
                (path != null) && (node != null) && (node.getParent() != null)
                    && !hiddenSet.contains(currentlySelectedLoggerName));
            hideAction.setEnabled(
                (path != null) && (node != null) && (node.getParent() != null));
            if (!focusOnAction.isEnabled()) {
                setFocusOnSelected(false);
            } else {
            }

            expandAction.setEnabled(path != null);
            findAction.setEnabled(path != null);
            clearFindNextAction.setEnabled(true);
            defineColorRuleForLoggerAction.setEnabled(path != null);
            setRefineFocusAction.setEnabled(path != null);
            updateRefineFocusAction.setEnabled(path != null);
            updateFindAction.setEnabled(path != null);
            clearRefineFocusAction.setEnabled(true);

            if (currentlySelectedLoggerName != null) {
                boolean isHidden = hiddenSet.contains(currentlySelectedLoggerName);
                popupMenu.hideCheck.setSelected(isHidden);
                ignoreLoggerButton.setSelected(isHidden);
            }

            collapseAction.setEnabled(path != null);

            reconfigureMenuText();
            if (isFocusOnSelected()) {
                fireChangeEvent();
            }
            //fire change event if we toggled focus off
            if (focusOnSelected && !isFocusOnSelected()) {
                fireChangeEvent();
            }
            //trigger a table repaint
            logPanel.repaint();
        });

        logTree.addMouseListener(popupListener);

        /**
         * This listener ensures the Tool bar toggle button and popup menu check box
         * stay in sync, plus notifies all the ChangeListeners that
         * an effective filter criteria has been modified
         */
        focusOnAction.addPropertyChangeListener(evt -> {
            popupMenu.focusOnCheck.setSelected(isFocusOnSelected());
            focusOnLoggerButton.setSelected(isFocusOnSelected());

            if (logTree.getSelectionPath() != null) {
                logTreeModel.nodeChanged(
                    (TreeNode) logTree.getSelectionPath().getLastPathComponent());
            }
        });

        hideAction.addPropertyChangeListener(evt -> {
            if (logTree.getSelectionPath() != null) {
                logTreeModel.nodeChanged(
                    (TreeNode) logTree.getSelectionPath().getLastPathComponent());
            }
        });

        //    /**
        //     * Now add a MouseListener that fires the expansion
        //     * action if CTRL + DBL CLICK is done.
        //     */
        //    logTree.addMouseListener(
        //      new MouseAdapter() {
        //        public void mouseClicked(MouseEvent e) {
        //          if (
        //            (e.getClickCount() > 1)
        //              && ((e.getModifiers() & InputEvent.CTRL_MASK) > 0)
        //              && ((e.getModifiers() & InputEvent.BUTTON1_MASK) > 0)) {
        //            expandCurrentlySelectedNode();
        //            e.consume();
        //          } else if (e.getClickCount() > 1) {
        //            super.mouseClicked(e);
        //            logger.debug("Ignoring dbl click event " + e);
        //          }
        //        }
        //      });

        logTree.addMouseListener(new MouseFocusOnListener());

        /**
         * We listen for when the FocusOn action changes, and then  translate
         * that to a RuleChange
         */
        addChangeListener(evt -> {
            visibilityRuleDelegate.firePropertyChange("rule", null, null);
            updateDisplay();
        });

        visibilityRuleDelegate.addPropertyChangeListener(event -> {
            if (event.getPropertyName().equals("hiddenSet")) {
                updateDisplay();
            }
        });
    }

    private void updateDisplay() {
        updateHiddenSetModels();
        updateIgnoreSummary();
        updateIgnoreExpressionSummary();
        updateAlwaysDisplayExpressionSummary();
    }

    private void updateHiddenSetModels() {
        DefaultListModel model = (DefaultListModel) ignoreList.getModel();
        model.clear();
        List sortedIgnoreList = new ArrayList(hiddenSet);
        Collections.sort(sortedIgnoreList);

        for (Object aSortedIgnoreList : sortedIgnoreList) {
            String string = (String) aSortedIgnoreList;
            model.addElement(string);
        }

//      ignoreList.setModel(model);

    }

    private void updateIgnoreSummary() {
        ignoreSummary.setText(ignoreList.getModel().getSize() + " hidden loggers");
    }

    private void updateIgnoreExpressionSummary() {
        ignoreExpressionSummary.setText(ignoreExpressionRule != null ? "Ignore (set)" : "Ignore (unset)");
    }

    private void updateAlwaysDisplayExpressionSummary() {
        alwaysDisplayExpressionSummary.setText(alwaysDisplayExpressionRule != null ? "Always displayed (set)" : "Always displayed (unset)");
    }

    private void toggleFocusOnState() {
        setFocusOnSelected(!isFocusOnSelected());
        fireChangeEvent();
    }

    public Collection getHiddenSet() {
        return Collections.unmodifiableSet(hiddenSet);
    }

    public String getHiddenExpression() {
        String text = ignoreExpressionEntryField.getText();
        if (text == null || text.trim().equals("")) {
            return null;
        }
        return text.trim();
    }

    public void setHiddenExpression(String hiddenExpression) {
        ignoreExpressionEntryField.setText(hiddenExpression);
        updateIgnoreExpression(hiddenExpression);
    }

    public String getAlwaysDisplayExpression() {
        String text = alwaysDisplayExpressionEntryField.getText();
        if (text == null || text.trim().equals("")) {
            return null;
        }
        return text.trim();
    }

    public void setAlwaysDisplayExpression(String alwaysDisplayExpression) {
        alwaysDisplayExpressionEntryField.setText(alwaysDisplayExpression);
        updateAlwaysDisplayExpression(alwaysDisplayExpression);
    }

    public void loggerNameAdded(String loggerName) {
        //no-op
    }

    public void reset() {
        expandRootLatch = false;
        //keep track if focuson was active when we were reset
        final String logger = currentlySelectedLoggerName;
        final boolean focusOnSelected = isFocusOnSelected();
        if (logger == null || !focusOnSelected) {
            return;
        }

        //loggernameAdded runs on EDT
        logTreeModel.loggerNameAdded(logger);
        EventQueue.invokeLater(() -> setFocusOn(logger));
    }

    //~ Inner Classes ===========================================================

    /**
     * DOCUMENT ME!
     *
     * @author $author$
     * @author Paul Smith &lt;psmith@apache.org&gt;
     * @version $Revision$, $Date$
     */
    private class LoggerNameTreeCellRenderer extends DefaultTreeCellRenderer {
        //~ Constructors ==========================================================

        //    private JPanel panel = new JPanel();
        private LoggerNameTreeCellRenderer() {
            super();

            //      panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            //      panel.add(this);
            setLeafIcon(null);
            setOpaque(false);
        }

        //~ Methods ===============================================================

        /* (non-Javadoc)
         * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
         */

        /**
         * DOCUMENT ME!
         *
         * @param tree     DOCUMENT ME!
         * @param value    DOCUMENT ME!
         * @param sel      DOCUMENT ME!
         * @param expanded DOCUMENT ME!
         * @param leaf     DOCUMENT ME!
         * @param row      DOCUMENT ME!
         * @param focus    DOCUMENT ME!
         * @return DOCUMENT ME!
         */
        public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean focus) {
            JLabel component =
                (JLabel) super.getTreeCellRendererComponent(
                    tree, value, sel, expanded, leaf, row, focus);

            Font originalFont = new Font(component.getFont().getName(), component.getFont().getStyle(), component.getFont().getSize());

            int style = Font.PLAIN;

            if (sel && focusOnLoggerButton.isSelected()) {
                style = style | Font.BOLD;
            }

            String logger =
                getLoggerName(
                    new TreePath(((DefaultMutableTreeNode) value).getPath()));

            if (hiddenSet.contains(logger)) {
                //        component.setEnabled(false);
                //        component.setIcon(leaf?null:getDefaultOpenIcon());
                style = style | Font.ITALIC;

                //        logger.debug("TreeRenderer: '" + logger + "' is in hiddenSet, italicizing");
            } else {
                //          logger.debug("TreeRenderer: '" + logger + "' is NOT in hiddenSet, leaving plain");
                //        component.setEnabled(true);
            }

            if (originalFont != null) {
                Font font2 = originalFont.deriveFont(style);

                if (font2 != null) {
                    component.setFont(font2);
                }
            }

            return component;
        }
    }

    private class LoggerTreePopupMenu extends JPopupMenu {
        //~ Instance fields =======================================================

        JCheckBoxMenuItem focusOnCheck = new JCheckBoxMenuItem();
        JCheckBoxMenuItem hideCheck = new JCheckBoxMenuItem();

        //~ Constructors ==========================================================

        private LoggerTreePopupMenu() {
            initMenu();
        }

        //~ Methods ===============================================================

        /* (non-Javadoc)
         * @see javax.swing.JPopupMenu#show(java.awt.Component, int, int)
         */

        /**
         * DOCUMENT ME!
         *
         * @param invoker DOCUMENT ME!
         * @param x       DOCUMENT ME!
         * @param y       DOCUMENT ME!
         */
        public void show(Component invoker, int x, int y) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) logTree.getLastSelectedPathComponent();

            if (node == null) {
                return;
            }

            super.show(invoker, x, y);
        }

        /**
         * DOCUMENT ME!
         */
        private void initMenu() {
            focusOnCheck.setAction(focusOnAction);
            hideCheck.setAction(hideAction);
            add(expandAction);
            add(collapseAction);
            addSeparator();
            add(focusOnCheck);
            add(hideCheck);
            addSeparator();
            add(setRefineFocusAction);
            add(updateRefineFocusAction);
            add(clearRefineFocusAction);
            addSeparator();
            add(findAction);
            add(updateFindAction);
            add(clearFindNextAction);

            addSeparator();
            add(defineColorRuleForLoggerAction);
            addSeparator();

            add(hideSubLoggersAction);
            add(clearIgnoreListAction);
        }
    }

    private final class MouseFocusOnListener extends MouseAdapter {
        //~ Methods ===============================================================

        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
         */

        /**
         * DOCUMENT ME!
         *
         * @param e DOCUMENT ME!
         */
        public void mouseClicked(MouseEvent e) {
            if (
                (e.getClickCount() > 1)
                    && ((e.getModifiers() & InputEvent.CTRL_MASK) > 0)
                    && ((e.getModifiers() & InputEvent.SHIFT_MASK) > 0)) {
                ignoreLoggerAtPoint(e.getPoint());
                e.consume();
                fireChangeEvent();
            } else if (
                (e.getClickCount() > 1)
                    && ((e.getModifiers() & InputEvent.CTRL_MASK) > 0)) {
                focusAnLoggerAtPoint(e.getPoint());
                e.consume();
                fireChangeEvent();
            }
        }

        /**
         * DOCUMENT ME!
         *
         * @param point
         */
        private void focusAnLoggerAtPoint(Point point) {
            String logger = getLoggerAtPoint(point);

            if (logger != null) {
                toggleFocusOnState();
            }
        }

        /**
         * DOCUMENT ME!
         *
         * @param point
         * @return
         */
        private String getLoggerAtPoint(Point point) {
            TreePath path = logTree.getPathForLocation(point.x, point.y);

            if (path != null) {
                return getLoggerName(path);
            }

            return null;
        }

        /**
         * DOCUMENT ME!
         *
         * @param point
         */
        private void ignoreLoggerAtPoint(Point point) {
            String logger = getLoggerAtPoint(point);

            if (logger != null) {
                toggleHiddenLogger(logger);
                fireChangeEvent();
            }
        }
    }

    private final class MouseKeyIconListener extends MouseMotionAdapter
        implements MouseMotionListener {
        //~ Instance fields =======================================================

        Cursor focusOnCursor =
            Toolkit.getDefaultToolkit().createCustomCursor(
                ChainsawIcons.FOCUS_ON_ICON.getImage(), new Point(10, 10), "");
        Cursor ignoreCursor =
            Toolkit.getDefaultToolkit().createCustomCursor(
                ChainsawIcons.IGNORE_ICON.getImage(), new Point(10, 10), "");

        //~ Methods ===============================================================

        /* (non-Javadoc)
         * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
         */

        /**
         * DOCUMENT ME!
         *
         * @param e DOCUMENT ME!
         */
        public void mouseMoved(MouseEvent e) {
            //      logger.debug(e.toString());
            if (
                ((e.getModifiers() & InputEvent.CTRL_MASK) > 0)
                    && ((e.getModifiers() & InputEvent.SHIFT_MASK) > 0)) {
                logTree.setCursor(ignoreCursor);
            } else if ((e.getModifiers() & InputEvent.CTRL_MASK) > 0) {
                logTree.setCursor(focusOnCursor);
            } else {
                logTree.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    class VisibilityRuleDelegate extends AbstractRule {
        public boolean evaluate(ChainsawLoggingEvent e, Map matches) {
            String currentlySelectedLoggerName = getCurrentlySelectedLoggerName();
            boolean hiddenLogger = e.m_logger != null && isHiddenLogger(e.m_logger);
            boolean hiddenExpression = (ignoreExpressionRule != null && ignoreExpressionRule.evaluate(e, null));
            boolean alwaysDisplayExpression = (alwaysDisplayExpressionRule != null && alwaysDisplayExpressionRule.evaluate(e, null));
            boolean hidden = (!alwaysDisplayExpression) && (hiddenLogger || hiddenExpression);
            if (currentlySelectedLoggerName == null) {
                //if there is no selected logger, pass if not hidden
                return !hidden;
            }
            boolean result = (e.m_logger != null) && !hidden;

            if (result && isFocusOnSelected()) {
                result = (e.m_logger != null && (e.m_logger.startsWith(currentlySelectedLoggerName + ".") || e.m_logger.endsWith(currentlySelectedLoggerName)));
            }

            return result;
        }

        public void firePropertyChange(String propertyName, Object oldVal, Object newVal) {
            super.firePropertyChange(propertyName, oldVal, newVal);
        }
    }

}
