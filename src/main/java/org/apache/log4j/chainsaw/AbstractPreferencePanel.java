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
package org.apache.log4j.chainsaw;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.tree.*;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;

/**
 * Some basic plumbing for Preference related dialogs.
 *
 * Sub classes have the following responsibilities:
 *
 * <ul>
 * <li>Must call this this classes initComponents() method from
 * within the sub-classes constructor, this method laysout the
 * panel and calls the abstract createTreeModel method to initialise the
 * tree of Panels.
 * <li>Must override createTreeModel() method to return a TreeModel whose
 * TreeNodes getUserObject() method will return a JComponent that is displayed
 * as the selected editable Preference panel.  This JComponent's .toString() method is
 * used as the title of the preference panel
 * </ul>
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public abstract class AbstractPreferencePanel extends JPanel {

    private final JLabel titleLabel = new JLabel("Selected Pref Panel");
    private final JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    private final JPanel selectedPrefPanel = new JPanel(new BorderLayout(0, 3));
    private final JButton okButton = new JButton(" OK ");
    private final JButton cancelButton = new JButton(" Cancel ");
    private ActionListener okCancelListener;
    private Component currentlyDisplayedPanel = null;
    private final JTree prefTree = new JTree();

    /**
     * Setup and layout for the components
     */
    protected void initComponents() {
        //		setBorder(BorderFactory.createLineBorder(Color.red));
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        prefTree.setModel(createTreeModel());

        prefTree.setRootVisible(false);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(ChainsawIcons.ICON_PREFERENCES);
        prefTree.setCellRenderer(renderer);

        final JScrollPane treeScroll = new JScrollPane(prefTree);

        treeScroll.setPreferredSize(new Dimension(200, 240));

        titleLabel.setFont(titleLabel.getFont().deriveFont(16.0f));
        titleLabel.setBorder(BorderFactory.createEtchedBorder());
        titleLabel.setBackground(Color.white);
        titleLabel.setOpaque(true);

        selectedPrefPanel.add(titleLabel, BorderLayout.NORTH);

        mainPanel.add(treeScroll, BorderLayout.WEST);
        mainPanel.add(selectedPrefPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        Box buttonBox = Box.createHorizontalBox();
        List<JButton> buttons = SwingHelper.orderOKCancelButtons(okButton, cancelButton);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(buttons.get(0));
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(buttons.get(1));

        add(buttonBox, BorderLayout.SOUTH);

        DefaultTreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();
        treeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        prefTree.setSelectionModel(treeSelectionModel);
        prefTree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            setDisplayedPrefPanel((JComponent) node.getUserObject());
        });

        // ensure the first pref panel is selected and displayed
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) prefTree.getModel().getRoot();
        DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) root.getFirstChild();
        prefTree.setSelectionPath(new TreePath(firstNode.getPath()));
    }

    /**
     * @return tree model
     */
    protected abstract TreeModel createTreeModel();

    /**
     * Ensures a specific panel is displayed in the spot where
     * preferences can be selected.
     *
     * @param panel
     */
    protected void setDisplayedPrefPanel(JComponent panel) {
        if (currentlyDisplayedPanel != null) {
            selectedPrefPanel.remove(currentlyDisplayedPanel);
        }

        selectedPrefPanel.add(panel, BorderLayout.CENTER);
        currentlyDisplayedPanel = panel;
        String title = panel.toString();
        titleLabel.setText(title);
        selectedPrefPanel.revalidate();
        selectedPrefPanel.repaint();
    }

    public void notifyOfLookAndFeelChange() {
        SwingUtilities.updateComponentTreeUI(this);

        Enumeration enumeration = ((DefaultMutableTreeNode) prefTree.getModel().getRoot()).breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (node.getUserObject() instanceof Component) {
                Component c = (Component) node.getUserObject();
                SwingUtilities.updateComponentTreeUI(c);
            }
        }
    }

    public void setOkCancelActionListener(ActionListener l) {
        this.okCancelListener = l;
    }

    public void hidePanel() {
        if (okCancelListener != null) {
            okCancelListener.actionPerformed(null);
        }
    }

    /**
     * @return Returns the cancelButton.
     */
    protected final JButton getCancelButton() {
        return cancelButton;
    }

    /**
     * @return Returns the okButton.
     */
    protected final JButton getOkButton() {
        return okButton;
    }
}
