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
package org.apache.log4j.chainsaw;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableColumn;


/**
 * A ListCellRenderer that display a check box if the value
 * has been "checked".
 * 
 * Borrowed heavily from the excellent book "Swing, 2nd Edition" by
 * Matthew Robinson  & Pavel Vorobiev.
 * 
 * @author Paul Smith
 *
 */
public abstract class CheckListCellRenderer extends JCheckBox
  implements ListCellRenderer {
  private final Border noFocusBorder =
    BorderFactory.createEmptyBorder(1, 1, 1, 1);

  /**
   *
   */
  public CheckListCellRenderer() {
    super();
    setOpaque(true);
    setBorder(noFocusBorder);
  }

  /* (non-Javadoc)
   * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
   */
  public Component getListCellRendererComponent(
    JList list, Object value, int index, boolean isSelected,
    boolean cellHasFocus) {
	  setText(((TableColumn)value).getHeaderValue().toString());
    setBackground(
      isSelected ? list.getSelectionBackground() : list.getBackground());
    setForeground(
      isSelected ? list.getSelectionForeground() : list.getForeground());
    setFont(list.getFont());
    setBorder(
      cellHasFocus ? UIManager.getBorder("List.focusCellHighlightBorder")
                   : noFocusBorder);

    setSelected(isSelected(value));
    return this;
  }

/**
 * @param value
 * @return selected flag
 */
protected abstract boolean isSelected(Object value);
}