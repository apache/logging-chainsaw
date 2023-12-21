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
package org.apache.log4j.chainsaw.receivers;

import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import org.apache.log4j.chainsaw.ReceiverEventListener;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A TreeModel that encapsulates the details of all the Receivers and their
 * related information in the Log4j framework
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ReceiversTreeModel extends DefaultTreeModel implements ReceiverEventListener {
    private static final String ROOTNODE_LABEL = "Receivers";
    final DefaultMutableTreeNode NoReceiversNode = new DefaultMutableTreeNode("No Receivers defined");
    final DefaultMutableTreeNode RootNode;
    private final Logger logger = LogManager.getLogger(ReceiversTreeModel.class);

    ReceiversTreeModel() {
        super(new DefaultMutableTreeNode(ROOTNODE_LABEL));
        RootNode = (DefaultMutableTreeNode) getRoot();
        refresh();
    }

    /**
     * Creates a new ReceiversTreeModel by querying the Log4j Plugin Repository
     * and building up the required information.
     *
     * @return ReceiversTreeModel
     */
    public final synchronized ReceiversTreeModel refresh() {
        //        RootNode.removeAllChildren();
        //
        //        LoggerRepository repo = LogManager.getLoggerRepository();
        //        Collection receivers;
        //        if (repo instanceof LoggerRepositoryEx) {
        //            receivers = ((LoggerRepositoryEx) repo).getPluginRegistry().getPlugins(Receiver.class);
        //        } else {
        //            receivers = new Vector();
        //        }
        //
        //        updateRootDisplay();
        //
        //        if (receivers.size() == 0) {
        //            getRootNode().add(NoReceiversNode);
        //        } else {
        //            for (Object receiver : receivers) {
        //                final Receiver item = (Receiver) receiver;
        //                final DefaultMutableTreeNode receiverNode = new DefaultMutableTreeNode(item);
        //
        //                item.addPropertyChangeListener(creatPluginPropertyChangeListener(item, receiverNode));
        //                getRootNode().add(receiverNode);
        //            }
        //        }
        //
        //        reload();

        return this;
    }

    private PropertyChangeListener creatPluginPropertyChangeListener(
            final ChainsawReceiver item, final DefaultMutableTreeNode receiverNode) {
        return evt -> {
            logger.debug(evt.toString());
            ReceiversTreeModel.this.fireTreeNodesChanged(item, receiverNode.getPath(), null, null);
        };
    }

    /**
     * Ensure the Root node of this tree is updated with the latest information
     * and that listeners are notified.
     */
    void updateRootDisplay() {
        getRootNode().setUserObject(ROOTNODE_LABEL);
        nodeChanged(getRootNode());
    }

    DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) getRoot();
    }

    TreeNode resolvePluginNode(ChainsawReceiver p) {
        /**
         * Lets walk the tree, top-down until we find the node with the plugin
         * attached.
         *
         * Since the tree hierachy is quite flat, this is should not
         * be a performance issue at all, but if it is,
         * then "I have no recollection of that Senator".
         */
        TreeNode treeNode = null;
        Enumeration e = getRootNode().breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.getUserObject().equals(p)) {
                treeNode = node;
                break;
            }
        }
        return treeNode;
    }

    @Override
    public void receiverAdded(ChainsawReceiver rx) {
        if (NoReceiversNode.getParent() == getRootNode()) {
            int index = getRootNode().getIndex(NoReceiversNode);
            getRootNode().remove(NoReceiversNode);
            nodesWereRemoved(getRootNode(), new int[] {index}, new Object[] {NoReceiversNode});
        }

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(rx);
        getRootNode().add(newNode);
        rx.addPropertyChangeListener(creatPluginPropertyChangeListener(rx, newNode));
        nodesWereInserted(getRootNode(), new int[] {getRootNode().getIndex(newNode)});
    }

    @Override
    public void receiverRemoved(ChainsawReceiver rx) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) resolvePluginNode(rx);
        if (node != null) {
            int index = getRootNode().getIndex(node);
            getRootNode().remove(node);
            nodesWereRemoved(getRootNode(), new int[] {index}, new Object[] {node});
        }

        if (getRootNode().getChildCount() == 0) {
            getRootNode().add(NoReceiversNode);

            int index = getRootNode().getIndex(NoReceiversNode);
            nodesWereInserted(getRootNode(), new int[] {index});
        }
    }
}
