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
package org.apache.log4j.chainsaw.components.loggernamepanel;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.ExpressionRuleContext;
import org.apache.log4j.chainsaw.JTextComponentFormatter;
import org.apache.log4j.chainsaw.PopupListener;
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.components.elements.SmallButton;
import org.apache.log4j.chainsaw.components.elements.SmallToggleButton;
import org.apache.log4j.chainsaw.components.logpanel.LogPanel;
import org.apache.log4j.chainsaw.components.logpanel.LogPanelLoggerTreeModel;
import org.apache.log4j.chainsaw.components.logpanel.LogPanelPreferenceModel;
import org.apache.log4j.chainsaw.filter.FilterModel;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LineIconFactory;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.rule.AbstractRule;
import org.apache.log4j.rule.ColorRule;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A panel that encapsulates the Logger Name tree, with associated actions
 * and implements the Rule interface so that it can filter in/out events
 * that do not match the users request for refining the view based on Loggers.
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public final class LoggerNameTreePanel extends JPanel implements LoggerNameListener {
    public static final String ACTION_NAME_CLOSE = " Close ";
    public static final String ACTION_NAME_UNHIDE_ALL = " Unhide All ";
    public static final String ACTION_NAME_CLEAR_IGNORE_LIST = "Clear Ignore list";
    public static final String PROPERTY_HIDDEN_SET = "hiddenSet";
    public static final String KEY_CHECKED = "checked";
    private final Logger logger = LogManager.getLogger(LoggerNameTreePanel.class);

    private static final int WARN_DEPTH = 4;
    private final LogPanelPreferenceModel logPanelPreferenceModel;

    private final LoggerNameTreeCellRenderer cellRenderer = new LoggerNameTreeCellRenderer();
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
    private final SmallToggleButton focusOnLoggerButton = new SmallToggleButton();
    private final Set<String> hiddenSet = new HashSet<>();
    private final Action hideAction;
    private final Action hideSubLoggersAction;
    private final JList ignoreList = new JList();
    private final JEditorPane ignoreExpressionEntryField = new JEditorPane();
    private final JEditorPane alwaysDisplayExpressionEntryField = new JEditorPane();
    private final JDialog ignoreDialog = new JDialog();
    private final JDialog ignoreExpressionDialog = new JDialog();
    private final JDialog alwaysDisplayExpressionDialog = new JDialog();
    private final JLabel ignoreSummary = new JLabel("0 hidden loggers");
    private final JLabel ignoreExpressionSummary = new JLabel("Ignore expression");
    private final JLabel alwaysDisplayExpressionSummary = new JLabel("Always displayed expression");
    private final SmallToggleButton ignoreLoggerButton = new SmallToggleButton();
    private final EventListenerList listenerList = new EventListenerList();
    private final JTree logTree;

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

    /**
     * Creates a new LoggerNameTreePanel object.
     *
     * @param logTreeModel
     */
    public LoggerNameTreePanel(
            LogPanelLoggerTreeModel logTreeModel,
            LogPanelPreferenceModel logPanelPreferenceModel,
            LogPanel logPanel,
            RuleColorizer colorizer,
            FilterModel filterModel) {
        super();
        this.logTreeModel = logTreeModel;
        this.logPanel = logPanel;
        this.logPanelPreferenceModel = logPanelPreferenceModel;
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
        colorRuleDelegate = new AbstractRule() {
            @Override
            public boolean evaluate(ChainsawLoggingEvent e, Map matches) {
                boolean hiddenLogger = e.m_logger != null && isHiddenLogger(e.m_logger);
                boolean hiddenExpression = (ignoreExpressionRule != null && ignoreExpressionRule.evaluate(e, null));
                boolean alwaysDisplayExpression =
                        (alwaysDisplayExpressionRule != null && alwaysDisplayExpressionRule.evaluate(e, null));
                boolean hidden = (!alwaysDisplayExpression) && (hiddenLogger || hiddenExpression);

                String selectedLoggerName = getCurrentlySelectedLoggerName();
                return !isFocusOnSelected()
                        && !hidden
                        && selectedLoggerName != null
                        && !"".equals(selectedLoggerName)
                        && (e.m_logger.startsWith(selectedLoggerName + ".") || e.m_logger.endsWith(selectedLoggerName));
            }
        };

        logTree = new JTree(logTreeModel) {
            @Override
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
            @Override
            public void treeNodesChanged(TreeModelEvent e) {}

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                if (!expandRootLatch) {
                    ensureRootExpanded();
                    expandRootLatch = true;
                }
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {}

            @Override
            public void treeStructureChanged(TreeModelEvent e) {}
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

        JButton btnShowIgnoreDialog = new SmallButton.Builder()
                .name("...")
                .shortDescription("Click to view and manage your hidden/ignored loggers")
                .action(() -> LogPanel.centerAndSetVisible(ignoreDialog))
                .build();

        ignoreSummaryPanel.add(btnShowIgnoreDialog);
        ignorePanel.add(ignoreSummaryPanel);

        JPanel ignoreExpressionPanel = new JPanel();
        ignoreExpressionPanel.setLayout(new BoxLayout(ignoreExpressionPanel, BoxLayout.X_AXIS));
        ignoreExpressionPanel.add(ignoreExpressionSummary);

        JButton btnShowIgnoreExpressionDialog = new SmallButton.Builder()
                .name("...")
                .shortDescription("Click to view and manage your hidden/ignored expression")
                .action(() -> LogPanel.centerAndSetVisible(ignoreExpressionDialog))
                .build();
        ignoreExpressionPanel.add(btnShowIgnoreExpressionDialog);

        ignorePanel.add(ignoreExpressionPanel);

        JPanel alwaysDisplayExpressionPanel = new JPanel();
        alwaysDisplayExpressionPanel.setLayout(new BoxLayout(alwaysDisplayExpressionPanel, BoxLayout.X_AXIS));
        alwaysDisplayExpressionPanel.add(alwaysDisplayExpressionSummary);

        JButton btnShowAlwaysDisplayExpressionDialog = new SmallButton.Builder()
                .name("...")
                .shortDescription("Click to view and manage your always-displayed expression")
                .action(() -> LogPanel.centerAndSetVisible(alwaysDisplayExpressionDialog))
                .build();

        alwaysDisplayExpressionPanel.add(btnShowAlwaysDisplayExpressionDialog);

        ignorePanel.add(alwaysDisplayExpressionPanel);

        add(ignorePanel, BorderLayout.SOUTH);

        ignoreList.setModel(new DefaultListModel());
        ignoreList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1 && ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) > 0)) {
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
        JScrollPane ignoreListScroll = new JScrollPane(ignoreList);
        ignoreListScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Double click an entry to unhide it"));
        ignoreListPanel.add(ignoreListScroll, BorderLayout.CENTER);

        JPanel ignoreExpressionDialogPanel = new JPanel(new BorderLayout());
        ignoreExpressionEntryField.addKeyListener(new ExpressionRuleContext(filterModel, ignoreExpressionEntryField));

        ignoreExpressionDialogPanel.add(new JScrollPane(ignoreExpressionEntryField), BorderLayout.CENTER);
        JButton ignoreExpressionCloseButton = new JButton(new AbstractAction(ACTION_NAME_CLOSE) {
            public void actionPerformed(ActionEvent e) {
                String ignoreText = ignoreExpressionEntryField.getText();

                if (updateIgnoreExpression(ignoreText)) {
                    ignoreExpressionDialog.setVisible(false);
                }
            }
        });

        JPanel alwaysDisplayExpressionDialogPanel = new JPanel(new BorderLayout());
        alwaysDisplayExpressionEntryField.addKeyListener(
                new ExpressionRuleContext(filterModel, alwaysDisplayExpressionEntryField));

        alwaysDisplayExpressionDialogPanel.add(new JScrollPane(alwaysDisplayExpressionEntryField), BorderLayout.CENTER);
        JButton alwaysDisplayExpressionCloseButton = new JButton(new AbstractAction(ACTION_NAME_CLOSE) {
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

        JButton unhideAll = new JButton(new AbstractAction(ACTION_NAME_UNHIDE_ALL) {

            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(() -> clearIgnoreListAction.actionPerformed(e));
            }
        });
        ignoreListButtonPanel.add(unhideAll);

        ignoreListButtonPanel.add(Box.createHorizontalGlue());
        JButton ignoreCloseButton = new JButton(new AbstractAction(ACTION_NAME_CLOSE) {

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
            if (ignoreText != null && !ignoreText.trim().isEmpty()) {
                ignoreExpressionRule = ExpressionRule.getRule(ignoreText);
            } else {
                ignoreExpressionRule = null;
            }
            visibilityRuleDelegate.firePropertyChange(PROPERTY_HIDDEN_SET, null, null);

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
            if (alwaysDisplayText != null && !alwaysDisplayText.trim().isEmpty()) {
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

    /**
     * Adds a change Listener to this LoggerNameTreePanel to be notfied
     * when the State of the Focus or Hidden details have changed.
     *
     * @param l the change listener to add
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

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Ensures the Focus is set to a specific logger name
     *
     * @param newLogger the name of the logger to focus on
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
            logger.error("failed to lookup logger {}", newLogger);
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

    private void toggleHiddenLogger(String logger) {
        if (!hiddenSet.contains(logger)) {
            hiddenSet.add(logger);
        } else {
            hiddenSet.remove(logger);
        }

        visibilityRuleDelegate.firePropertyChange(PROPERTY_HIDDEN_SET, null, null);
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
     * @param fqnLoggersToIgnore the loggers to ignore
     */
    public void ignore(Collection<String> fqnLoggersToIgnore) {
        hiddenSet.addAll(fqnLoggersToIgnore);
        visibilityRuleDelegate.firePropertyChange(PROPERTY_HIDDEN_SET, null, null);
        fireChangeEvent();
    }

    /**
     * Returns true if the FocusOn element has been selected
     *
     * @return true if the FocusOn action/lement has been selected
     */
    boolean isFocusOnSelected() {
        return focusOnAction.getValue(KEY_CHECKED) != null;
    }

    void setFocusOnSelected(boolean selected) {
        if (selected) {
            focusOnAction.putValue(KEY_CHECKED, Boolean.TRUE);
        } else {
            focusOnAction.putValue(KEY_CHECKED, null);
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
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Enumeration<TreeNode> enumeration = node.depthFirstEnumeration();

            while (enumeration.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) enumeration.nextElement();

                if ((child.getParent() != null) && (child != node)) {
                    TreeNode[] nodes = ((DefaultMutableTreeNode) child.getParent()).getPath();

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

    private Action createClearIgnoreListAction() {
        Action action = new AbstractAction(ACTION_NAME_CLEAR_IGNORE_LIST, null) {
            @Override
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
     * @return the close action
     */
    private Action createCloseAction() {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logPanelPreferenceModel.setLogTreePanelVisible(false);
            }
        };

        action.putValue(Action.NAME, "Close");
        action.putValue(Action.SHORT_DESCRIPTION, "Closes the Logger panel");
        action.putValue(Action.SMALL_ICON, LineIconFactory.createCloseIcon());

        return action;
    }

    private Action createCollapseAction() {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                collapseCurrentlySelectedNode();
            }
        };

        action.putValue(Action.SMALL_ICON, LineIconFactory.createCollapseIcon());
        action.putValue(Action.NAME, "Collapse Branch");
        action.putValue(Action.SHORT_DESCRIPTION, "Collapses all the children of the currently selected node");
        action.setEnabled(false);

        return action;
    }

    private Action createEditLoggerAction() {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
            }
        };

        //    TODO enable this when it's ready.
        action.putValue("enabled", Boolean.FALSE);

        action.putValue(Action.NAME, "Edit filters/colors");
        action.putValue(Action.SHORT_DESCRIPTION, "Allows you to specify filters and coloring for this Logger");
        action.putValue(Action.SMALL_ICON, new ImageIcon(ChainsawIcons.ICON_EDIT_RECEIVER));
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
            @Override
            public void actionPerformed(ActionEvent e) {
                expandCurrentlySelectedNode();
            }
        };

        action.putValue(Action.SMALL_ICON, LineIconFactory.createExpandIcon());
        action.putValue(Action.NAME, "Expand branch");
        action.putValue(
                Action.SHORT_DESCRIPTION, "Expands all the child nodes of the currently selected node, recursively");
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
            @Override
            public void actionPerformed(ActionEvent e) {
                findNextUsingCurrentlySelectedNode();
            }
        };

        action.putValue(Action.NAME, "Find next");
        action.putValue(Action.SHORT_DESCRIPTION, "Find using the selected node");
        action.setEnabled(false);

        return action;
    }

    private Action createSetRefineFocusAction() {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRefineFocusUsingCurrentlySelectedNode();
            }
        };

        action.putValue(Action.NAME, "Set 'refine focus' to selected logger");
        action.putValue(Action.SHORT_DESCRIPTION, "Refine focus on the selected node");
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
        action.putValue(Action.SHORT_DESCRIPTION, "Add selected node to 'refine focus' field");
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
        action.putValue(Action.SHORT_DESCRIPTION, "Add selected node to 'find' field");
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
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedLogger = getCurrentlySelectedLoggerName();
                TreePath[] paths = logTree.getSelectionPaths();

                if (paths == null) {
                    return;
                }
                Color c = JColorChooser.showDialog(getRootPane(), "Choose a color", Color.red);
                if (c != null) {
                    String expression = "logger like '^" + selectedLogger + ".*'";
                    colorizer.addRule(new ColorRule(
                            expression,
                            ExpressionRule.getRule(expression),
                            c,
                            ChainsawConstants.COLOR_DEFAULT_FOREGROUND));
                }
            }
        };

        action.putValue(Action.NAME, "Define color rule for selected logger");
        action.putValue(Action.SHORT_DESCRIPTION, "Define color rule for logger");
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
            @Override
            public void actionPerformed(ActionEvent e) {
                clearFindNext();
            }
        };

        action.putValue(Action.NAME, "Clear find field");
        action.putValue(Action.SHORT_DESCRIPTION, "Clear the find field");
        action.setEnabled(false);

        return action;
    }

    private Action createClearRefineFocusAction() {
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearRefineFocus();
            }
        };

        action.putValue(Action.NAME, "Clear 'refine focus' field");
        action.putValue(Action.SHORT_DESCRIPTION, "Clear the refine focus field");
        action.setEnabled(false);

        return action;
    }

    private Action createFocusOnAction() {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFocusOnState();
            }
        };

        action.putValue(Action.NAME, "Focus");
        action.putValue(
                Action.SHORT_DESCRIPTION,
                "Allows you to Focus on the selected logger by setting a filter that discards all but this Logger");
        action.putValue(Action.SMALL_ICON, new ImageIcon(ChainsawIcons.WINDOW_ICON));

        action.setEnabled(false);

        return action;
    }

    private Action createIgnoreAllAction() {
        Action action = new AbstractAction("Ignore loggers below selection") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // add all top level loggers as hidden loggers
                TreePath[] paths = logTree.getSelectionPaths();

                StringBuilder parentPathString = new StringBuilder();
                DefaultMutableTreeNode root;

                if ((paths == null) || (paths.length == 0)) {
                    root = (DefaultMutableTreeNode) logTreeModel.getRoot();
                } else {
                    root = (DefaultMutableTreeNode) logTree.getSelectionPath().getLastPathComponent();
                    TreeNode[] path = root.getPath();
                    // don't add 'root logger' to path string
                    for (int i = 1; i < path.length; i++) {
                        if (i > 1) {
                            parentPathString.append(".");
                        }
                        parentPathString.append(path[i].toString());
                    }
                    if (!(parentPathString.toString().isEmpty())) {
                        parentPathString.append(".");
                    }
                }
                Enumeration<TreeNode> topLevelLoggersEnumeration = root.children();
                Set<String> topLevelLoggersSet = new HashSet<>();
                while (topLevelLoggersEnumeration.hasMoreElements()) {
                    String thisLogger = topLevelLoggersEnumeration.nextElement().toString();
                    topLevelLoggersSet.add(parentPathString + thisLogger);
                }

                if (!topLevelLoggersSet.isEmpty()) {
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

    private Action createIgnoreAction() {
        Action action = new AbstractAction("Ignore this Logger", new ImageIcon(ChainsawIcons.ICON_COLLAPSE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedLoggerName = getCurrentlySelectedLoggerName();

                if (selectedLoggerName != null) {
                    toggleHiddenLogger(selectedLoggerName);
                    logTreeModel.nodeChanged(
                            (TreeNode) logTree.getSelectionPath().getLastPathComponent());
                    ignoreLoggerButton.setSelected(hiddenSet.contains(selectedLoggerName));
                    focusOnAction.setEnabled(!hiddenSet.contains(selectedLoggerName));
                    popupMenu.hideCheck.setSelected(hiddenSet.contains(selectedLoggerName));
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

        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) logTreeModel.getRoot();
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

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            Enumeration<TreeNode> depthEnum = treeNode.depthFirstEnumeration();

            if (!depthEnum.hasMoreElements()) {
                break;
            }

            List<Integer> depths = new ArrayList<>();

            while (depthEnum.hasMoreElements()) {
                depths.add(((DefaultMutableTreeNode) depthEnum.nextElement()).getDepth());
            }

            Collections.sort(depths);
            Collections.reverse(depths);

            int maxDepth = depths.get(0);

            if (maxDepth > WARN_DEPTH) {
                logger.warn("Should warn user, depth={}", maxDepth);
            }

            depthEnum = treeNode.depthFirstEnumeration();

            while (depthEnum.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) depthEnum.nextElement();

                if (node.isLeaf() && node.getParent() != null) {
                    TreeNode[] nodes = ((DefaultMutableTreeNode) node.getParent()).getPath();
                    TreePath treePath = new TreePath(nodes);

                    logger.debug("Expanding path: {}", treePath);

                    logTree.expandPath(treePath);
                }
            }
        }
    }

    private void fireChangeEvent() {
        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        ChangeEvent e = null;

        for (ChangeListener listener : listeners) {
            if (e == null) {
                e = new ChangeEvent(this);
            }

            listener.stateChanged(e);
        }
    }

    private void reconfigureMenuText() {
        String selectedLoggerName = getCurrentlySelectedLoggerName();

        if ((selectedLoggerName == null) || (selectedLoggerName.length() == 0)) {
            focusOnAction.putValue(Action.NAME, "Focus On...");
            hideAction.putValue(Action.NAME, "Ignore...");
            findAction.putValue(Action.NAME, "Find...");
            setRefineFocusAction.putValue(Action.NAME, "Set refine focus field");
            updateRefineFocusAction.putValue(Action.NAME, "Add to refine focus field");
            updateFindAction.putValue(Action.NAME, "Add to find field");
            defineColorRuleForLoggerAction.putValue(Action.NAME, "Define color rule");
        } else {
            focusOnAction.putValue(Action.NAME, "Focus On '" + selectedLoggerName + "'");
            hideAction.putValue(Action.NAME, "Ignore '" + selectedLoggerName + "'");
            findAction.putValue(Action.NAME, "Find '" + selectedLoggerName + "'");
            setRefineFocusAction.putValue(Action.NAME, "Set refine focus field to '" + selectedLoggerName + "'");
            updateRefineFocusAction.putValue(Action.NAME, "Add '" + selectedLoggerName + "' to 'refine focus' field");
            updateFindAction.putValue(Action.NAME, "Add '" + selectedLoggerName + "' to 'find' field");
            defineColorRuleForLoggerAction.putValue(Action.NAME, "Define color rule for '" + selectedLoggerName + "'");
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
            focusOnAction.setEnabled((path != null)
                    && (node != null)
                    && (node.getParent() != null)
                    && !hiddenSet.contains(currentlySelectedLoggerName));
            hideAction.setEnabled((path != null) && (node != null) && (node.getParent() != null));
            if (!focusOnAction.isEnabled()) {
                setFocusOnSelected(false);
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
            // fire change event if we toggled focus off
            if (focusOnSelected && !isFocusOnSelected()) {
                fireChangeEvent();
            }
            // trigger a table repaint
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
                logTreeModel.nodeChanged((TreeNode) logTree.getSelectionPath().getLastPathComponent());
            }
        });

        hideAction.addPropertyChangeListener(evt -> {
            if (logTree.getSelectionPath() != null) {
                logTreeModel.nodeChanged((TreeNode) logTree.getSelectionPath().getLastPathComponent());
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
            if (event.getPropertyName().equals(PROPERTY_HIDDEN_SET)) {
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
        List<String> sortedIgnoreList = new ArrayList<>(hiddenSet);
        Collections.sort(sortedIgnoreList);

        for (String aSortedIgnoreList : sortedIgnoreList) {
            model.addElement(aSortedIgnoreList);
        }
    }

    private void updateIgnoreSummary() {
        ignoreSummary.setText(ignoreList.getModel().getSize() + " hidden loggers");
    }

    private void updateIgnoreExpressionSummary() {
        ignoreExpressionSummary.setText(ignoreExpressionRule != null ? "Ignore (set)" : "Ignore (unset)");
    }

    private void updateAlwaysDisplayExpressionSummary() {
        alwaysDisplayExpressionSummary.setText(
                alwaysDisplayExpressionRule != null ? "Always displayed (set)" : "Always displayed (unset)");
    }

    private void toggleFocusOnState() {
        setFocusOnSelected(!isFocusOnSelected());
        fireChangeEvent();
    }

    public Collection<String> getHiddenSet() {
        return Collections.unmodifiableSet(hiddenSet);
    }

    public String getHiddenExpression() {
        String text = ignoreExpressionEntryField.getText();
        if (text == null || text.trim().isEmpty()) {
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
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }

    public void setAlwaysDisplayExpression(String alwaysDisplayExpression) {
        alwaysDisplayExpressionEntryField.setText(alwaysDisplayExpression);
        updateAlwaysDisplayExpression(alwaysDisplayExpression);
    }

    public void loggerNameAdded(String loggerName) {
        // no-op
    }

    public void reset() {
        expandRootLatch = false;
        // keep track if focuson was active when we were reset
        final boolean focusOnSelected = isFocusOnSelected();
        if (currentlySelectedLoggerName == null || !focusOnSelected) {
            return;
        }

        // loggernameAdded runs on EDT
        logTreeModel.loggerNameAdded(currentlySelectedLoggerName);
        EventQueue.invokeLater(() -> setFocusOn(currentlySelectedLoggerName));
    }

    private class LoggerNameTreeCellRenderer extends DefaultTreeCellRenderer {
        //    private JPanel panel = new JPanel();
        private LoggerNameTreeCellRenderer() {
            super();

            //      panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            //      panel.add(this);
            setLeafIcon(null);
            setOpaque(false);
        }

        /* (non-Javadoc)
         * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
         */
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean focus) {
            JLabel component =
                    (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focus);

            Font originalFont = new Font(
                    component.getFont().getName(),
                    component.getFont().getStyle(),
                    component.getFont().getSize());

            int style = Font.PLAIN;

            if (sel && focusOnLoggerButton.isSelected()) {
                style = style | Font.BOLD;
            }

            String loggerName = getLoggerName(new TreePath(((DefaultMutableTreeNode) value).getPath()));

            if (hiddenSet.contains(loggerName)) {
                //        component.setEnabled(false);
                //        component.setIcon(leaf?null:getDefaultOpenIcon());
                style = style | Font.ITALIC;

                //        logger.debug("TreeRenderer: '" + logger + "' is in hiddenSet, italicizing");
            } else {
                //          logger.debug("TreeRenderer: '" + logger + "' is NOT in hiddenSet, leaving plain");
                //        component.setEnabled(true);
            }

            Font font2 = originalFont.deriveFont(style);
            if (font2 != null) {
                component.setFont(font2);
            }

            return component;
        }
    }

    private class LoggerTreePopupMenu extends JPopupMenu {
        JCheckBoxMenuItem focusOnCheck = new JCheckBoxMenuItem();
        JCheckBoxMenuItem hideCheck = new JCheckBoxMenuItem();

        private LoggerTreePopupMenu() {
            initMenu();
        }

        /* (non-Javadoc)
         * @see javax.swing.JPopupMenu#show(java.awt.Component, int, int)
         */
        @Override
        public void show(Component invoker, int x, int y) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) logTree.getLastSelectedPathComponent();

            if (node == null) {
                return;
            }

            super.show(invoker, x, y);
        }

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
        /* (non-Javadoc)
         * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1
                    && ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0)
                    && ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0)) {
                ignoreLoggerAtPoint(e.getPoint());
                e.consume();
                fireChangeEvent();
            } else if (e.getClickCount() > 1 && ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0)) {
                focusAnLoggerAtPoint(e.getPoint());
                e.consume();
                fireChangeEvent();
            }
        }

        private void focusAnLoggerAtPoint(Point point) {
            if (getLoggerAtPoint(point) != null) {
                toggleFocusOnState();
            }
        }

        private String getLoggerAtPoint(Point point) {
            TreePath path = logTree.getPathForLocation(point.x, point.y);

            if (path != null) {
                return getLoggerName(path);
            }

            return null;
        }

        private void ignoreLoggerAtPoint(Point point) {
            String loggerAtPoint = getLoggerAtPoint(point);

            if (loggerAtPoint != null) {
                toggleHiddenLogger(loggerAtPoint);
                fireChangeEvent();
            }
        }
    }

    private final class MouseKeyIconListener extends MouseMotionAdapter implements MouseMotionListener {
        Cursor focusOnCursor = Toolkit.getDefaultToolkit()
                .createCustomCursor(ChainsawIcons.FOCUS_ON_ICON.getImage(), new Point(10, 10), "");
        Cursor ignoreCursor = Toolkit.getDefaultToolkit()
                .createCustomCursor(ChainsawIcons.IGNORE_ICON.getImage(), new Point(10, 10), "");

        /* (non-Javadoc)
         * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            if (((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0)
                    && ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) > 0)) {
                logTree.setCursor(ignoreCursor);
            } else if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0) {
                logTree.setCursor(focusOnCursor);
            } else {
                logTree.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    class VisibilityRuleDelegate extends AbstractRule {
        @Override
        public boolean evaluate(ChainsawLoggingEvent event, Map matches) {
            String selectedLoggerName = getCurrentlySelectedLoggerName();
            boolean hiddenLogger = event.m_logger != null && isHiddenLogger(event.m_logger);
            boolean hiddenExpression = (ignoreExpressionRule != null && ignoreExpressionRule.evaluate(event, null));
            boolean alwaysDisplayExpression =
                    (alwaysDisplayExpressionRule != null && alwaysDisplayExpressionRule.evaluate(event, null));
            boolean hidden = (!alwaysDisplayExpression) && (hiddenLogger || hiddenExpression);
            if (selectedLoggerName == null) {
                // if there is no selected logger, pass if not hidden
                return !hidden;
            }

            boolean result = (event.m_logger != null) && !hidden;

            if (result && isFocusOnSelected()) {
                result = (event.m_logger != null
                        && (event.m_logger.startsWith(selectedLoggerName + ".")
                                || event.m_logger.endsWith(selectedLoggerName)));
            }

            return result;
        }

        @Override
        public void firePropertyChange(String propertyName, Object oldVal, Object newVal) {
            super.firePropertyChange(propertyName, oldVal, newVal);
        }
    }
}
