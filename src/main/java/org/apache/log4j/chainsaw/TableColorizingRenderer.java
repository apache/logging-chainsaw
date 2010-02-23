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

import java.awt.Color;
import java.awt.Component;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.log4j.chainsaw.color.Colorizer;
import org.apache.log4j.chainsaw.icons.ChainsawIcons;
import org.apache.log4j.chainsaw.icons.LevelIconFactory;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.spi.LoggingEvent;


/**
 * A specific TableCellRenderer that colourizes a particular cell based on
 * some ColourFilters that have been stored according to the value for the row
 *
 * @author Claude Duguay
 * @author Scott Deboy <sdeboy@apache.org>
 * @author Paul Smith <psmith@apache.org>
 *
 */
public class TableColorizingRenderer extends DefaultTableCellRenderer {
  private static final DateFormat DATE_FORMATTER =
    new SimpleDateFormat(Constants.SIMPLE_TIME_PATTERN);
  private static final Map iconMap =
    LevelIconFactory.getInstance().getLevelToIconMap();
  private Colorizer colorizer;
  private final JLabel idComponent = new JLabel();
  private final JLabel levelComponent = new JLabel();
  private boolean levelUseIcons = false;
  private DateFormat dateFormatInUse = DATE_FORMATTER;
  private int loggerPrecision = 0;
  private boolean toolTipsVisible;
  private String dateFormatTZ;
  private final Icon markerIcon = new ImageIcon(ChainsawIcons.MARKER);

  /**
   * Creates a new TableColorizingRenderer object.
   */
  public TableColorizingRenderer(Colorizer colorizer) {
    this.colorizer = colorizer;
    idComponent.setBorder(BorderFactory.createRaisedBevelBorder());
    idComponent.setBackground(Color.gray);
    idComponent.setHorizontalAlignment(SwingConstants.CENTER);
    idComponent.setOpaque(true);

    levelComponent.setOpaque(true);
    levelComponent.setHorizontalAlignment(SwingConstants.CENTER);

    levelComponent.setText("");
  }
  
  public void setToolTipsVisible(boolean toolTipsVisible) {
      this.toolTipsVisible = toolTipsVisible;
  }

  public Component getTableCellRendererComponent(
    final JTable table, Object value, boolean isSelected, boolean hasFocus,
    int row, int col) {
    value = formatField(value);

    JLabel c = (JLabel)super.getTableCellRendererComponent(table, value, 
        isSelected, hasFocus, row, col);

    TableColumn tableColumn = table.getColumnModel().getColumn(col);
    int colIndex = tableColumn.getModelIndex() + 1;

    EventContainer container = (EventContainer) table.getModel();
    LoggingEvent event = container.getRow(row);
    //no event, use default renderer
    if (event == null) {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
    }

    switch (colIndex) {
    case ChainsawColumns.INDEX_ID_COL_NAME:
      idComponent.setText(value.toString());
      idComponent.setForeground(c.getForeground());
      idComponent.setBackground(c.getBackground());
      c = idComponent;
      break;

    case ChainsawColumns.INDEX_THROWABLE_COL_NAME:
      if (value instanceof String[]) {
        c.setText(((String[]) value)[0]);
      }
      break;

    case ChainsawColumns.INDEX_LOGGER_COL_NAME:
      if (loggerPrecision == 0) {
        break;
      }
      String logger = value.toString();
      int startPos = -1;

      for (int i = 0; i < loggerPrecision; i++) {
        startPos = logger.indexOf(".", startPos + 1);
        if (startPos < 0) {
          break;
        }
      }

    c.setText(logger.substring(startPos + 1));
      break;
    case ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME:
    case ChainsawColumns.INDEX_CLASS_COL_NAME:
    case ChainsawColumns.INDEX_FILE_COL_NAME:
    case ChainsawColumns.INDEX_LINE_COL_NAME:
    case ChainsawColumns.INDEX_MESSAGE_COL_NAME:
    case ChainsawColumns.INDEX_NDC_COL_NAME:
    case ChainsawColumns.INDEX_THREAD_COL_NAME:
    case ChainsawColumns.INDEX_TIMESTAMP_COL_NAME:
    case ChainsawColumns.INDEX_METHOD_COL_NAME:
      c.setText(value.toString());
      break;

    case ChainsawColumns.INDEX_LEVEL_COL_NAME:
      if (levelUseIcons) {
        levelComponent.setIcon((Icon) iconMap.get(value.toString()));

        if (levelComponent.getIcon() != null) {
          levelComponent.setText("");
        }
        if (!toolTipsVisible) {
          levelComponent.setToolTipText(value.toString());
        }
      } else {
        levelComponent.setIcon(null);
        levelComponent.setText(value.toString());
        if (!toolTipsVisible) {
            levelComponent.setToolTipText(null);
        }
      }
      if (toolTipsVisible) {
          levelComponent.setToolTipText(c.getToolTipText());
      }
      levelComponent.setForeground(c.getForeground());
      levelComponent.setBackground(c.getBackground());

      c = levelComponent;
      break;

    //remaining entries are properties
    default:
        Set propertySet = event.getPropertyKeySet();
        String headerName = tableColumn.getHeaderValue().toString().toLowerCase();
        String thisProp = null;
        //find the property in the property set...case-sensitive
        for (Iterator iter = propertySet.iterator();iter.hasNext();) {
            String entry = iter.next().toString();
            if (entry.toLowerCase().equals(headerName)) {
                thisProp = entry;
                break;
            }
        }
        if (thisProp != null) {
            c.setText(event.getProperty(headerName));
        }
        break;
    }
    //set the 'marker' icon next to the zeroth column if marker is set
    if (col == 0) {
      if (event.getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME) != null) {
        c.setIcon(markerIcon);
      } else {
        //only null out the column if it's not the level field
        if (colIndex != ChainsawColumns.INDEX_LEVEL_COL_NAME) {
            c.setIcon(null);
        }
      }
    }

    if (isSelected) {
      return c;
    }

    Color background = null;
    Color foreground = null;

    if (colorizer != null) {

      if (event == null) {
        //ignore...probably changed displayed cols
        return c;
      }
      background = colorizer.getBackgroundColor(event);
      foreground = colorizer.getForegroundColor(event);
    }

    /**
     * Colourize based on row striping
     */
    if (background == null) {
      if ((row % 2) != 0) {
        background = ChainsawConstants.COLOR_ODD_ROW;
      } else {
        background = ChainsawConstants.COLOR_EVEN_ROW;
      }
    }

    if (foreground == null) {
      foreground = Color.black;
    }
    
    c.setBackground(background);
    c.setForeground(foreground);
    
    return c;
  }

  /**
   * Changes the Date Formatting object to be used for rendering dates.
   * @param formatter
   */
  void setDateFormatter(DateFormat formatter) {
    this.dateFormatInUse = formatter;
    if (dateFormatInUse != null && dateFormatTZ != null && !("".equals(dateFormatTZ))) {
      dateFormatInUse.setTimeZone(TimeZone.getTimeZone(dateFormatTZ));
    } else {
      dateFormatInUse.setTimeZone(TimeZone.getDefault());
    }
  }

  /**
   * Changes the Logger precision.
   * @param loggerPrecisionText
   */
  void setLoggerPrecision(String loggerPrecisionText) {
    try {
      loggerPrecision = Integer.parseInt(loggerPrecisionText);
    } catch (NumberFormatException nfe) {
        loggerPrecision = 0;
    }
  }

  /**
   *Format date field
   *
   * @param o object
   *
   * @return formatted object
   */
  private Object formatField(Object o) {
    if (!(o instanceof Date)) {
      return (o == null ? "" : o);
    }
    
    return dateFormatInUse.format((Date) o);
  }

  /**
   * @param colorizer
   */
  public void setColorizer(Colorizer colorizer) {
    this.colorizer = colorizer;
  }

   /**
   * Sets the property which determines whether to use Icons or text
   * for the Level column
   * @param levelUseIcons
   */
  public void setLevelUseIcons(boolean levelUseIcons) {
    this.levelUseIcons = levelUseIcons;
  }

  public void setTimeZone(String dateFormatTZ) {
    this.dateFormatTZ = dateFormatTZ;

    if (dateFormatInUse != null && dateFormatTZ != null && !("".equals(dateFormatTZ))) {
      dateFormatInUse.setTimeZone(TimeZone.getTimeZone(dateFormatTZ));
    } else {
      dateFormatInUse.setTimeZone(TimeZone.getDefault());
    }
  }
}
