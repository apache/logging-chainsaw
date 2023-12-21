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
package org.apache.log4j.chainsaw.helper;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import org.apache.log4j.chainsaw.logevents.Level;

/**
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class TableCellEditorFactory {

    /**
     * Creates a TableCellEditor to be used for editing boolean values
     *
     * @return TableCellEditor
     */
    public static final TableCellEditor createBooleanTableCellEditor() {

        JComboBox<Boolean> comboBox = new JComboBox<>(new Boolean[] {Boolean.TRUE, Boolean.FALSE});
        return new DefaultCellEditor(comboBox);
    }

    /**
     *
     */
    private TableCellEditorFactory() {}

    /**
     * @return table cell editor
     */
    public static Object createLevelTableCellEditor() {
        JComboBox<Level> comboBox = new JComboBox<>(new Level[] {
            Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL, Level.OFF, Level.ALL
        });
        return new DefaultCellEditor(comboBox);
    }
}
