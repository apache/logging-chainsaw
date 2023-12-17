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
package org.apache.log4j.chainsaw.logui;

import org.apache.log4j.chainsaw.components.tabbedpane.ChainsawTabbedPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class LogUiKeyStrokeCreator {

    static void createKeyStrokeGotoLine(ChainsawTabbedPane tabbedPane, LogUI logUI) {
        KeyStroke ksGotoLine =
            KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            ksGotoLine, "GotoLine");

        Action gotoLine =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    String inputLine = JOptionPane.showInputDialog(logUI, "Enter the line number to go:", "Goto Line", JOptionPane.PLAIN_MESSAGE);
                    try {
                        int lineNumber = Integer.parseInt(inputLine);
                        int row = logUI.getCurrentLogPanel().setSelectedEvent(lineNumber);
                        if (row == -1) {
                            JOptionPane.showMessageDialog(logUI, "You have entered an invalid line number", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(logUI, "You have entered an invalid line number", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

        tabbedPane.getActionMap().put("GotoLine", gotoLine);
    }

    static void createKeyStrokeLeft(ChainsawTabbedPane tabbedPane) {
        KeyStroke ksLeft =
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            ksLeft, "MoveLeft");

        Action moveLeft =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int temp = tabbedPane.getSelectedIndex();
                    --temp;

                    if (temp > -1) {
                        tabbedPane.setSelectedTab(temp);
                    }
                }
            };
        tabbedPane.getActionMap().put("MoveLeft", moveLeft);
    }

    static void createKeyStrokeRight(ChainsawTabbedPane tabbedPane) {
        KeyStroke ksRight =
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            ksRight, "MoveRight");
        Action moveRight =
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    int temp = tabbedPane.getSelectedIndex();
                    ++temp;

                    if (temp != tabbedPane.getTabCount()) {
                        tabbedPane.setSelectedTab(temp);
                    }
                }
            };
        tabbedPane.getActionMap().put("MoveRight", moveRight);
    }
}
