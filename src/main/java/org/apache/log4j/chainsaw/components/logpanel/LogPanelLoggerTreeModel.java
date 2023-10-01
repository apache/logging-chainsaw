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
package org.apache.log4j.chainsaw.components.logpanel;

import org.apache.log4j.chainsaw.LoggerNameListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.*;


/**
 * A TreeModel that represents the Loggers for a given LogPanel
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class LogPanelLoggerTreeModel extends DefaultTreeModel
    implements LoggerNameListener {
    private Map<String, LogPanelTreeNode> fullPackageMap = new HashMap<>();
    private final Logger logger = LogManager.getLogger(LogPanelLoggerTreeModel.class);

    LogPanelLoggerTreeModel() {
        super(new LogPanelTreeNode("Root Logger"));
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.LoggerNameListener#loggerNameAdded(java.lang.String)
     */
    public void loggerNameAdded(final String loggerName) {
        //invoke later, not on current EDT
        SwingUtilities.invokeLater(
            () -> addLoggerNameInDispatchThread(loggerName));
    }

    public void reset() {
        DefaultMutableTreeNode current = (DefaultMutableTreeNode) getRoot();
        current.removeAllChildren();
        fullPackageMap.clear();
        nodeStructureChanged(current);
    }

    private void addLoggerNameInDispatchThread(final String loggerName) {
        String[] packages = tokenize(loggerName);

        /**
         * The packages array is effectively the tree
         * path that must exist within the tree, so
         * we walk the tree ensuring each level is present
         */
        DefaultMutableTreeNode current = (DefaultMutableTreeNode) getRoot();


/**
 * This label is used to break out when descending the
 * current tree hierachy, and it has matched a package name
 * with an already existing TreeNode.
 */
        outerFor:
        for (int i = 0; i < packages.length; i++) {
            String packageName = packages[i];
            Enumeration enumeration = current.children();

            while (enumeration.hasMoreElements()) {
                DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) enumeration.nextElement();
                String childName = child.getUserObject().toString();

                if (childName.equals(packageName)) {
                    /**
                     * This the current known branch to descend
                     */
                    current = child;

                    /**
                     * we've found it, so break back to the outer
                     * for loop to continue processing further
                     * down the tree
                     */
                    continue outerFor;
                }
            }

            /*
             * So we haven't found this index in the current children,
             * better create the child
             */
            final LogPanelTreeNode newChild = new LogPanelTreeNode(packageName);

            StringBuilder fullPackageBuf = new StringBuilder();

            for (int j = 0; j <= i; j++) {
                fullPackageBuf.append(packages[j]);

                if (j < i) {
                    fullPackageBuf.append(".");
                }
            }

            logger.debug("Adding to Map " + fullPackageBuf.toString());
            fullPackageMap.put(fullPackageBuf.toString(), newChild);

            final DefaultMutableTreeNode changedNode = current;

            changedNode.add(newChild);

            final int[] changedIndices = new int[changedNode.getChildCount()];

            for (int j = 0; j < changedIndices.length; j++) {
                changedIndices[j] = j;
            }

            nodesWereInserted(
                changedNode, new int[]{changedNode.getIndex(newChild)});
            nodesChanged(changedNode, changedIndices);
            current = newChild;
        }
    }

    public LogPanelTreeNode lookupLogger(String newLogger) {
        if (fullPackageMap.containsKey(newLogger)) {
            return fullPackageMap.get(newLogger);
        } else {
            logger.debug("No logger found matching '" + newLogger + "'");
            logger.debug("Map Dump: " + fullPackageMap);
        }

        return null;
    }

    /**
     * Takes the loggerName and tokenizes it into it's
     * package name lements returning the elements
     * via the Stirng[]
     *
     * @param loggerName
     * @return array of strings representing the package hierarchy
     */
    private String[] tokenize(String loggerName) {
        StringTokenizer tok = new StringTokenizer(loggerName, ".");

        String[] tokens = new String[tok.countTokens()];

        int index = 0;

        while (tok.hasMoreTokens()) {
            tokens[index++] = tok.nextToken();
        }

        return tokens;
    }

    private static class LogPanelTreeNode extends DefaultMutableTreeNode {
        protected static Comparator nodeComparator =
            (o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString());

        private LogPanelTreeNode(String logName) {
            super(logName);
        }

        public void insert(MutableTreeNode newChild, int childIndex) {
            //      logger.debug("[" + this.getUserObject() + "] inserting child " + newChild + " @ index " + childIndex);
            //      logger.debug("Children now: " + this.children);
            super.insert(newChild, childIndex);

            //	  logger.debug("Children after insert: " + this.children);
            this.children.sort(nodeComparator);

            //	  logger.debug("Children after sort: " + this.children);
        }
    }
}
