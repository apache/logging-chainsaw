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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.apache.log4j.chainsaw.filter.FilterModel;
import org.apache.log4j.rule.RuleFactory;
import org.apache.log4j.spi.LoggingEventFieldResolver;

/**
 * A popup menu which assists in building expression rules.  Completes event keywords, operators and
 * context if available.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class ExpressionRuleContext extends KeyAdapter {
    RuleFactory factory = RuleFactory.getInstance();
    LoggingEventFieldResolver resolver = LoggingEventFieldResolver.getInstance();
    JPopupMenu contextMenu = new JPopupMenu();
    JList<String> list = new JList<>();
    FilterModel filterModel;
    JScrollPane scrollPane = new JScrollPane(list);
    final JTextComponent textComponent;
    private final DefaultListModel<String> fieldModel = new DefaultListModel<>();
    private final DefaultListModel<String> operatorModel = new DefaultListModel<>();

    public ExpressionRuleContext(final FilterModel filterModel, final JTextComponent textComponent) {
        this.filterModel = filterModel;
        this.textComponent = textComponent;
        fieldModel.addElement("LOGGER");
        fieldModel.addElement("LEVEL");
        fieldModel.addElement("CLASS");
        fieldModel.addElement("FILE");
        fieldModel.addElement("LINE");
        fieldModel.addElement("METHOD");
        fieldModel.addElement("MSG");
        fieldModel.addElement("NDC");
        fieldModel.addElement("EXCEPTION");
        fieldModel.addElement("TIMESTAMP");
        fieldModel.addElement("THREAD");
        fieldModel.addElement("PROP.");

        operatorModel.addElement("&&");
        operatorModel.addElement("||");
        operatorModel.addElement("!");
        operatorModel.addElement("!=");
        operatorModel.addElement("==");
        operatorModel.addElement("~=");
        operatorModel.addElement("LIKE");
        operatorModel.addElement("EXISTS");
        operatorModel.addElement("<");
        operatorModel.addElement(">");
        operatorModel.addElement("<=");
        operatorModel.addElement(">=");

        // make long to avoid scrollbar
        list.setVisibleRowCount(13);

        PopupListener popupListener = new PopupListener();
        textComponent.addMouseListener(popupListener);

        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String value = list.getSelectedValue();
                    String contextKey = getContextKey();
                    if (contextKey != null && (!(contextKey.endsWith(".")))) {
                        value = "'" + value + "'";
                    }

                    updateField(value);
                    contextMenu.setVisible(false);
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String value = list.getSelectedValue();
                    String contextKey = getContextKey();
                    if (contextKey != null && (!(contextKey.endsWith(".")))) {
                        value = "'" + value + "'";
                    }

                    updateField(value);
                    contextMenu.setVisible(false);
                }
            }
        });

        contextMenu.insert(scrollPane, 0);
    }

    private void updateField(String value) {
        if (textComponent.getSelectedText() == null && !value.endsWith(".")) {
            value = value + " ";
        }

        textComponent.replaceSelection(value);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
            displayContext();
        }
    }

    public void displayContext() {
        String lastField = getContextKey();

        if (lastField == null) {
            if (isOperatorContextValid()) {
                list.setModel(operatorModel);
                list.setSelectedIndex(0);

                Point p = textComponent.getCaret().getMagicCaretPosition();
                contextMenu.doLayout();
                contextMenu.show(textComponent, p.x, (p.y + (textComponent.getHeight() - 5)));
                list.requestFocus();
            } else if (isFieldContextValid()) {
                list.setModel(fieldModel);
                list.setSelectedIndex(0);

                Point p = textComponent.getCaret().getMagicCaretPosition();
                if (p == null) {
                    p = new Point(
                            textComponent.getLocation().x,
                            (textComponent.getLocation().y - textComponent.getHeight() + 5));
                }

                contextMenu.doLayout();
                contextMenu.show(textComponent, p.x, (p.y + (textComponent.getHeight() - 5)));
                list.requestFocus();
            }
        } else {
            ListModel model = filterModel.getContainer().getModel(lastField);
            if (model == null) {
                return;
            }
            list.setModel(model);
            list.setSelectedIndex(0);

            Point p = textComponent.getCaret().getMagicCaretPosition();
            contextMenu.doLayout();
            contextMenu.show(textComponent, p.x, (p.y + (textComponent.getHeight() - 5)));
            list.requestFocus();
        }
    }

    private boolean isFieldContextValid() {
        String text = textComponent.getText();
        int currentPosition = textComponent.getSelectionStart();
        return ((currentPosition == 0) || (text.charAt(currentPosition - 1) == ' '));
    }

    private String getContextKey() {
        String field = getField();

        if (field == null) {
            field = getSubField();
        }

        return field;
    }

    private boolean isOperatorContextValid() {
        String text = textComponent.getText();

        int currentPosition = textComponent.getSelectionStart();
        if ((currentPosition < 1) || (text.charAt(currentPosition - 1) != ' ')) {
            return false;
        }

        int lastFieldPosition = text.lastIndexOf(" ", currentPosition - 1);
        if (lastFieldPosition == -1) {
            return false;
        }

        int lastFieldStartPosition = Math.max(0, text.lastIndexOf(" ", lastFieldPosition - 1));
        String field = text.substring(lastFieldStartPosition, lastFieldPosition)
                .toUpperCase()
                .trim();

        return resolver.isField(field);
    }

    /*
     * Returns the currently active field which can be used to display a context menu
     * the field returned is the left hand portion of an expression (for example, logger == )
     * logger is the field that is returned
     */
    private String getField() {
        String text = textComponent.getText();

        int currentPosition = textComponent.getSelectionStart();
        if ((currentPosition < 1) || (text.charAt(currentPosition - 1) != ' ')) {
            return null;
        }

        int symbolPosition = text.lastIndexOf(" ", currentPosition - 1);
        if (symbolPosition < 0) {
            return null;
        }

        int lastFieldPosition = text.lastIndexOf(" ", symbolPosition - 1);
        if (lastFieldPosition < 0) {
            return null;
        }

        int lastFieldStartPosition = Math.max(0, text.lastIndexOf(" ", lastFieldPosition - 1));
        String lastSymbol =
                text.substring(lastFieldPosition + 1, symbolPosition).trim();

        String lastField =
                text.substring(lastFieldStartPosition, lastFieldPosition).trim();

        if (factory.isRule(lastSymbol) && filterModel.getContainer().modelExists(lastField)) {
            return lastField;
        }

        return null;
    }

    // subfields allow the key portion of a field to provide context menu support
    // and are available after the fieldname and a . (for example, PROP.)
    private String getSubField() {
        int currentPosition = textComponent.getSelectionStart();
        String text = textComponent.getText();

        if (text.substring(0, currentPosition).toUpperCase().endsWith("PROP.")) {
            return "PROP.";
        }
        return null;
    }

    class PopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            checkPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            checkPopup(e);
        }

        private void checkPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                displayContext();
            }
        }
    }
}
