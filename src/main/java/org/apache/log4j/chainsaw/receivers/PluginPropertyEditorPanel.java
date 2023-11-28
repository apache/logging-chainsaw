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

import org.apache.log4j.chainsaw.logevents.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.Generator;
import org.apache.log4j.chainsaw.helper.TableCellEditorFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.List;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellRenderer;
import org.apache.log4j.chainsaw.ChainsawReceiver;


/**
 * A panel that allows the user to edit a particular Plugin, by using introspection
 * this class discovers the modifiable properties of the Plugin
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class PluginPropertyEditorPanel extends JPanel {

    private final JScrollPane scrollPane = new JScrollPane();
    private final JTable propertyTable = new JTable();

    private ChainsawReceiver m_receiver;
    private TableModel defaultModel = new DefaultTableModel(
        new String[]{"Property", "Value"}, 1);

    private static final Logger logger = LogManager.getLogger();

    /**
     *
     */
    public PluginPropertyEditorPanel() {
        super();
        initComponents();
    }

    /**
     *
     */
    private void initComponents() {
        propertyTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
        setLayout(new BorderLayout());
        scrollPane.setViewportView(propertyTable);

        add(scrollPane, BorderLayout.CENTER);

        propertyTable.setModel(
            defaultModel = new DefaultTableModel(
                new String[]{"Property", "Value"}, 1));

    }

    /**
     * @return Returns the plugin.
     */
    public final ChainsawReceiver getPlugin() {
        propertyTable.getColumnModel().getColumn(1)
            .getCellEditor().stopCellEditing();
        return m_receiver;
    }

    /**
     * @param plugin The plugin to set.
     */
    public final void setReceiverAndProperties(ChainsawReceiver plugin, PropertyDescriptor[] descriptors) {
        this.m_receiver = plugin;
        
        if(descriptors != null){
            PluginPropertyTableModel model =
                new PluginPropertyTableModel(descriptors);
            propertyTable.setModel(model);
            propertyTable.getColumnModel().getColumn(1)
                .setCellEditor(new PluginTableCellEditor());
            propertyTable.setEnabled(true);
        }else{
            propertyTable.setModel(defaultModel);
            propertyTable.setEnabled(false);
        }
    }

    /**
     * @author psmith
     */
    private class PluginTableCellEditor extends AbstractCellEditor
        implements TableCellEditor {

        private Map editorMap = new HashMap();
        private DefaultCellEditor defaultEditor = new DefaultCellEditor(
            new JTextField());
        private DefaultCellEditor currentEditor = defaultEditor;

        private PluginTableCellEditor() {

            editorMap.put(Boolean.class,
                TableCellEditorFactory.createBooleanTableCellEditor());
            editorMap.put(Level.class,
                TableCellEditorFactory.createLevelTableCellEditor());
            //support primitive boolean parameters with the appropriate editor
            editorMap.put(boolean.class, TableCellEditorFactory.createBooleanTableCellEditor());
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
         */
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {

            PluginPropertyTableModel model = (PluginPropertyTableModel) table.getModel();
            PropertyDescriptor descriptor = model.getDescriptors()[row];
            Class valueClass = descriptor.getPropertyType();

            if (editorMap.containsKey(valueClass)) {

                DefaultCellEditor editor =
                    (DefaultCellEditor) editorMap.get(valueClass);
                logger.debug("Located CellEditor for " + valueClass);
                currentEditor = editor;

                return currentEditor.getTableCellEditorComponent(table, value,
                    isSelected, row, column);
            }

            currentEditor = defaultEditor;
            logger.debug("Cell value class " + valueClass +
                " not know, using default editor");

            Component c = defaultEditor.getTableCellEditorComponent(table, value,
                isSelected, row, column);
            table.setRowHeight( row, c.getPreferredSize().height );
            return c;
        }

        /* (non-Javadoc)
         * @see javax.swing.CellEditor#getCellEditorValue()
         */
        public Object getCellEditorValue() {

            return currentEditor.getCellEditorValue();
        }

    }

    private class PluginPropertyTableModel extends AbstractTableModel {

        private final PropertyDescriptor[] descriptors;

        private PluginPropertyTableModel(PropertyDescriptor[] descriptors){
            super();

            List<PropertyDescriptor> list = new ArrayList<>(Arrays.asList(
                descriptors));

            list.sort((o1, o2) -> {

                PropertyDescriptor d1 = (PropertyDescriptor) o1;
                PropertyDescriptor d2 = (PropertyDescriptor) o2;

                return d1.getDisplayName().compareToIgnoreCase(
                    d2.getDisplayName());
            });
            this.descriptors = list.toArray(
                new PropertyDescriptor[0]);
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int col) {

            PropertyDescriptor d = descriptors[row];

            switch (col) {

                case 1:

                    try {

                        Object object = d.getReadMethod().invoke(m_receiver);

                        if (object != null) {

                            return object;
                        }
                    } catch (Exception e) {
                        logger.error(
                            "Error reading value for PropertyDescriptor " + d);
                    }

                    return "";

                case 0:
                    return d.getName();
            }

            return null;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnCount()
         */
        public int getColumnCount() {

            return 2;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getRowCount()
         */
        public int getRowCount() {

            return descriptors.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#isCellEditable(int, int)
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {

//        TODO Determine if the property is one of the ones a User could edit
            return columnIndex == 1 && descriptors[rowIndex].getWriteMethod() != null;

        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
        public String getColumnName(int column) {

            return (column == 0) ? "Property" : "Value";
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {


            if (columnIndex == 1) {
                //ensure name is set
                if (descriptors[rowIndex].getName().equalsIgnoreCase("name") && (aValue == null || aValue.toString().trim().isEmpty())) {
                    logger.error("Name required");
                    return;
                }
                aValue = translateValueIfNeeded(rowIndex, aValue);
                logger.debug(
                    "setValueAt, " + rowIndex + ", " + columnIndex +
                        ", value=" + aValue + ", valueClass" + aValue.getClass());

                try {
                    descriptors[rowIndex].getWriteMethod().invoke(m_receiver,
                        aValue);
                    fireTableCellUpdated(rowIndex, columnIndex);
                } catch (IllegalArgumentException e) {
                    // ignore
                } catch (Exception e) {
                    logger.error(
                        "Failed to modify the Plugin because of Exception", e);
                }

            } else {
                super.setValueAt(aValue, rowIndex, columnIndex);
            }

            // Since the value has been set, resize all of the rows(if required)
            propertyTable.setRowHeight(ChainsawConstants.DEFAULT_ROW_HEIGHT);
        }

        /**
         * @param row
         * @param value
         * @return
         */
        private Object translateValueIfNeeded(int row, Object value) {

            if ((descriptors[row].getPropertyType() == int.class) ||
                (descriptors[row].getPropertyType() == Integer.class)) {

                try {

                    return Integer.valueOf(value.toString());
                } catch (Exception e) {
                    logger.error("Failed to convert to Integer type");
                }
            }

            return value;
        }

        /**
         * @return Returns the descriptors.
         */
        public final PropertyDescriptor[] getDescriptors() {
            return descriptors;
        }
    }
}
