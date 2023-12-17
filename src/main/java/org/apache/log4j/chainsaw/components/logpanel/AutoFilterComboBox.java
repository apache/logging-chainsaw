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
package org.apache.log4j.chainsaw.components.logpanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;


public class AutoFilterComboBox extends JComboBox {
    private boolean bypassFiltering;
    private List allEntries = new ArrayList();
    private List displayedEntries = new ArrayList();

    AutoFilterComboBoxModel model = new AutoFilterComboBoxModel();
    //editor component
    private final JTextField textField = new JTextField();
    private String lastTextToMatch;

    public AutoFilterComboBox() {
        textField.setPreferredSize(getPreferredSize());
        setModel(model);
        setEditor(new AutoFilterEditor());
        ((JTextField) getEditor().getEditorComponent()).getDocument().addDocumentListener(new AutoFilterDocumentListener());
        setEditable(true);
        addPopupMenuListener(new PopupMenuListenerImpl());
    }

    public Vector getModelData() {
        //reverse the model order, because it will be un-reversed when we reload it from saved settings
        Vector vector = new Vector();
        for (Object allEntry : allEntries) {
            vector.insertElementAt(allEntry, 0);
        }
        return vector;
    }

    void refilter() {
        //only refilter if we're not bypassing filtering AND the text has changed since the last call to refilter
        String textToMatch = getEditor().getItem().toString();
        if (bypassFiltering || (lastTextToMatch != null && lastTextToMatch.equals(textToMatch))) {
            return;
        }
        lastTextToMatch = textToMatch;
        bypassFiltering = true;
        model.removeAllElements();
        List entriesCopy = new ArrayList(allEntries);
        for (Object anEntriesCopy : entriesCopy) {
            String thisEntry = anEntriesCopy.toString();
            if (thisEntry.toLowerCase(Locale.ENGLISH).contains(textToMatch.toLowerCase())) {
                model.addElement(thisEntry);
            }
        }
        bypassFiltering = false;
        //TODO: on no-match, don't filter at all (show the popup?)
        if (displayedEntries.size() > 0 && !textToMatch.isEmpty()) {
            showPopup();
        } else {
            hidePopup();
        }
    }
    class AutoFilterComboBoxModel extends AbstractListModel implements MutableComboBoxModel {
        private Object selectedItem;

        public void addElement(Object obj) {
            //assuming add is to displayed list...add to full list (only if not a dup)
            bypassFiltering = true;

            boolean entryExists = !allEntries.contains(obj);
            if (entryExists) {
                allEntries.add(obj);
            }
            displayedEntries.add(obj);
            if (!entryExists) {
                fireIntervalAdded(this, displayedEntries.size() - 1, displayedEntries.size());
            }
            bypassFiltering = false;
        }

        public void removeElement(Object obj) {
            int index = displayedEntries.indexOf(obj);
            if (index != -1) {
                removeElementAt(index);
            }
        }

        public void insertElementAt(Object obj, int index) {
            //assuming add is to displayed list...add to full list (only if not a dup)
            if (allEntries.contains(obj)) {
                return;
            }
            bypassFiltering = true;
            displayedEntries.add(index, obj);
            allEntries.add(index, obj);
            fireIntervalAdded(this, index, index);
            bypassFiltering = false;
            refilter();
        }

        public void removeElementAt(int index) {
            bypassFiltering = true;
            //assuming removal is from displayed list..remove from full list
            Object obj = displayedEntries.get(index);
            allEntries.remove(obj);
            displayedEntries.remove(obj);
            fireIntervalRemoved(this, index, index);
            bypassFiltering = false;
            refilter();
        }

        public void setSelectedItem(Object item) {
            if ((selectedItem != null && !selectedItem.equals(item)) || selectedItem == null && item != null) {
                selectedItem = item;
                fireContentsChanged(this, -1, -1);
            }
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public int getSize() {
            return displayedEntries.size();
        }

        public Object getElementAt(int index) {
            if (index >= 0 && index < displayedEntries.size()) {
                return displayedEntries.get(index);
            }
            return null;
        }

        public void removeAllElements() {
            bypassFiltering = true;
            int displayedEntrySize = displayedEntries.size();
            if (displayedEntrySize > 0) {
                displayedEntries.clear();
                //if firecontentschaned is used, the combobox resizes..use fireintervalremoved instead, which doesn't do that..
                fireIntervalRemoved(this, 0, displayedEntrySize - 1);
            }
            bypassFiltering = false;
        }

        public void showAllElements() {
            //first remove whatever is there and fire necessary events then add events
            removeAllElements();
            bypassFiltering = true;
            displayedEntries.addAll(allEntries);
            if (displayedEntries.size() > 0) {
                fireIntervalAdded(this, 0, displayedEntries.size() - 1);
            }
            bypassFiltering = false;
        }
    }

    class AutoFilterEditor implements ComboBoxEditor {
        public Component getEditorComponent() {
            return textField;
        }

        public void setItem(Object item) {
            if (bypassFiltering) {
                return;
            }
            bypassFiltering = true;
            if (item == null) {
                textField.setText("");
            } else {
                textField.setText(item.toString());
            }
            bypassFiltering = false;
        }

        public Object getItem() {
            return textField.getText();
        }

        public void selectAll() {
            textField.selectAll();
        }

        public void addActionListener(ActionListener listener) {
            textField.addActionListener(listener);
        }

        public void removeActionListener(ActionListener listener) {
            textField.removeActionListener(listener);
        }
    }

    class AutoFilterDocumentListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            refilter();
        }

        public void removeUpdate(DocumentEvent e) {
            refilter();
        }

        public void changedUpdate(DocumentEvent e) {
            refilter();
        }
    }


    private class PopupMenuListenerImpl implements PopupMenuListener {
        private boolean willBecomeVisible = false;

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            bypassFiltering = true;
            ((JComboBox) e.getSource()).setSelectedIndex(-1);
            bypassFiltering = false;
            if (!willBecomeVisible) {
                //we already have a match but we're showing the popup - unfilter
                if (displayedEntries.contains(textField.getText())) {
                    model.showAllElements();
                }

                //workaround for bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4743225
                //the height of the popup after updating entries in this listener was not updated..
                JComboBox list = (JComboBox) e.getSource();
                willBecomeVisible = true; // the flag is needed to prevent a loop
                try {
                    list.getUI().setPopupVisible(list, true);
                } finally {
                    willBecomeVisible = false;
                }
            }
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            //no-op
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
            //no-op
        }
    }
}
