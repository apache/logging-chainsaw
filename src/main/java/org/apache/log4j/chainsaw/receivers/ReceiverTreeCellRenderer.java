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

import org.apache.log4j.chainsaw.Generator;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LevelIconFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;


/**
 * A TreeCellRenderer that can format the information of Receivers
 * and their children
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ReceiverTreeCellRenderer extends DefaultTreeCellRenderer {
    private Icon rootIcon = new ImageIcon(ChainsawIcons.ANIM_NET_CONNECT);
    private JPanel panel = new JPanel();
    private JLabel levelLabel = new JLabel();

    public ReceiverTreeCellRenderer() {
        super();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.X_AXIS);
        panel.setLayout(layout);
        panel.setOpaque(false);
        panel.add(levelLabel);
        //set preferredsize to something wide
        setPreferredSize(new Dimension(200, 19));
        panel.add(this);
    }

    public Component getTreeCellRendererComponent(
        JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
        int row, boolean focus) {
        super.getTreeCellRendererComponent(
            tree, value, sel, expanded, leaf, row, focus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object o = node.getUserObject();
        setText(o.toString());

        String tooltip = "";

        setIcon(null);
        if (
            o == ((ReceiversTreeModel) tree.getModel()).getRootNode().getUserObject()) {
            setText(o.toString());
        } else if (o instanceof String) {
            setText(o.toString());
            setIcon(null);
        } else if (o instanceof Generator) {
            Generator generator = (Generator) o;
            setText(generator.getName());
            setIcon(ChainsawIcons.ICON_HELP);
        }else if(o instanceof ChainsawReceiver){
            setText(((ChainsawReceiver) o).getName());
        } else {
            setText("(Unknown Type) :: " + o);
        }

        if (
            o == ((ReceiversTreeModel) tree.getModel()).getRootNode().getUserObject()) {
            setIcon(rootIcon);
        }

        levelLabel.setText(null);
        levelLabel.setIcon(null);

        if (o instanceof ChainsawReceiver) {
            ChainsawReceiver t = (ChainsawReceiver) o;

            if (t.getThreshold()!= null) {
                levelLabel.setIcon(
                    LevelIconFactory.getInstance().getLevelToIconMap().get(
                        t.getThreshold().toString()));

                if (levelLabel.getIcon() == null) {
                    levelLabel.setText(t.getThreshold().toString());
                }
            }
        }

        setToolTipText(tooltip);

        return panel;
    }
}
