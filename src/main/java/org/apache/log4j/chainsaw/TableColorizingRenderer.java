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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.icons.LevelIconFactory;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.Rule;


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
  private static final DateFormat DATE_FORMATTER = new SimpleDateFormat(Constants.SIMPLE_TIME_PATTERN);
  private static final Map iconMap = LevelIconFactory.getInstance().getLevelToIconMap();
  private RuleColorizer colorizer;
  private final JLabel levelComponent = new JLabel();
  private boolean levelUseIcons = false;
  private boolean wrapMsg = false;
  private DateFormat dateFormatInUse = DATE_FORMATTER;
  private int loggerPrecision = 0;
  private boolean toolTipsVisible;
  private String dateFormatTZ;
  private boolean useRelativeTimes = false;
  private long relativeTimestampBase;
  private static int borderWidth = 2;
  private static Color borderColor = (Color)UIManager.get("Table.selectionBackground");
  private static final Border LEFT_BORDER = BorderFactory.createMatteBorder(borderWidth, borderWidth, borderWidth, 0, borderColor);
  private static final Border MIDDLE_BORDER = BorderFactory.createMatteBorder(borderWidth, 0, borderWidth, 0, borderColor);
  private static final Border RIGHT_BORDER = BorderFactory.createMatteBorder(borderWidth, 0, borderWidth, borderWidth, borderColor);

  private static final Border LEFT_EMPTY_BORDER = BorderFactory.createEmptyBorder(borderWidth, borderWidth, borderWidth, 0);
  private static final Border MIDDLE_EMPTY_BORDER = BorderFactory.createEmptyBorder(borderWidth, 0, borderWidth, 0);
  private static final Border RIGHT_EMPTY_BORDER = BorderFactory.createEmptyBorder(borderWidth, 0, borderWidth, borderWidth);
  private JTextArea msgRenderer = new JTextArea();

    /**
   * Creates a new TableColorizingRenderer object.
   */
  public TableColorizingRenderer(RuleColorizer colorizer) {
    this.colorizer = colorizer;

    levelComponent.setOpaque(true);
    levelComponent.setVerticalAlignment(SwingConstants.TOP);

    levelComponent.setText("");
    levelComponent.setVerticalAlignment(SwingConstants.TOP);
    msgRenderer.setSize(1000, 2);
  }

  public void setToolTipsVisible(boolean toolTipsVisible) {
      this.toolTipsVisible = toolTipsVisible;
  }

  public Component getTableCellRendererComponent(
    final JTable table, Object value, boolean isSelected, boolean hasFocus,
    int row, int col) {
    value = formatField(value);

    JLabel labelRenderer = (JLabel)super.getTableCellRendererComponent(table, value,
        isSelected, hasFocus, row, col);
    labelRenderer.setVerticalAlignment(SwingConstants.TOP);
    JComponent component;
    TableColumn tableColumn = table.getColumnModel().getColumn(col);
    int colIndex = tableColumn.getModelIndex() + 1;

    if(colIndex == ChainsawColumns.INDEX_MESSAGE_COL_NAME) {
        component = msgRenderer;
        msgRenderer.setFont(labelRenderer.getFont());
    } else if (colIndex == ChainsawColumns.INDEX_LEVEL_COL_NAME) {
        component = levelComponent;
    } else {
        component = labelRenderer;
    }

    EventContainer container = (EventContainer) table.getModel();
    ExtendedLoggingEvent loggingEvent = container.getRow(row);
    //no event, use default renderer
    if (loggingEvent == null) {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
    }

    switch (colIndex) {
    case ChainsawColumns.INDEX_ID_COL_NAME:
      labelRenderer.setText(value.toString());
      break;

    case ChainsawColumns.INDEX_THROWABLE_COL_NAME:
      if (value instanceof String[] && ((String[])value).length > 0){
        labelRenderer.setText(((String[]) value)[0]);
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

    labelRenderer.setText(logger.substring(startPos + 1));
      break;
    case ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME:
    case ChainsawColumns.INDEX_CLASS_COL_NAME:
    case ChainsawColumns.INDEX_FILE_COL_NAME:
    case ChainsawColumns.INDEX_LINE_COL_NAME:
    case ChainsawColumns.INDEX_NDC_COL_NAME:
    case ChainsawColumns.INDEX_THREAD_COL_NAME:
    case ChainsawColumns.INDEX_TIMESTAMP_COL_NAME:
    case ChainsawColumns.INDEX_METHOD_COL_NAME:
      labelRenderer.setText(value.toString());
      break;

    case ChainsawColumns.INDEX_MESSAGE_COL_NAME:
        msgRenderer.setLineWrap(wrapMsg);
        msgRenderer.setWrapStyleWord(wrapMsg);

        if (wrapMsg) {
            int width = table.getColumnModel().getColumn(ChainsawColumns.INDEX_MESSAGE_COL_NAME).getWidth();
            msgRenderer.setSize(width, 2);
        }
        msgRenderer.setText(value.toString());
        if (wrapMsg) {
            int preferredHeight = (int) msgRenderer.getPreferredSize().getHeight();
            int tableRowHeight = table.getRowHeight();
            if(preferredHeight != tableRowHeight) {
                int rowHeight = Math.max(preferredHeight, tableRowHeight);
                table.setRowHeight(row, rowHeight);
            }
        }
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
          levelComponent.setToolTipText(labelRenderer.getToolTipText());
      }
      levelComponent.setForeground(labelRenderer.getForeground());
      levelComponent.setBackground(labelRenderer.getBackground());
      break;

    //remaining entries are properties
    default:
        Set propertySet = loggingEvent.getPropertyKeySet();
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
            labelRenderer.setText(loggingEvent.getProperty(headerName));
        }
        break;
    }

    Color background;
    Color foreground;
    Rule loggerRule = colorizer.getLoggerRule();
    //use logger colors in table instead of event colors if event passes logger rule
    if (loggerRule != null && loggerRule.evaluate(loggingEvent)) {
        background = ChainsawConstants.FIND_LOGGER_BACKGROUND;
        foreground = ChainsawConstants.FIND_LOGGER_FOREGROUND;
    } else {
        background = loggingEvent.getBackground();
        foreground = loggingEvent.getForeground();
    }

    /**
     * Colourize background based on row striping if the event still has a background color
     */
    if (background.equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)) {
      if ((row % 2) != 0) {
        background = ChainsawConstants.COLOR_ODD_ROW;
      } else {
        background = ChainsawConstants.COLOR_EVEN_ROW;
      }
    }

    component.setBackground(background);
    component.setForeground(foreground);

    if (isSelected) {
      if (col == 0) {
        component.setBorder(LEFT_BORDER);
      } else if (col == table.getColumnCount() - 1) {
        component.setBorder(RIGHT_BORDER);
      } else {
        component.setBorder(MIDDLE_BORDER);
      }
    } else {
      if (col == 0) {
        component.setBorder(LEFT_EMPTY_BORDER);
      } else if (col == table.getColumnCount() - 1) {
        component.setBorder(RIGHT_EMPTY_BORDER);
      } else {
        component.setBorder(MIDDLE_EMPTY_BORDER);
      }
    }

    return component;
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

    //handle date field
    if (useRelativeTimes)
    {
        return "" + (((Date)o).getTime() - relativeTimestampBase);
    }

    return dateFormatInUse.format((Date) o);
  }

    /**
    * Sets the property which determines whether to wrap the message
    * @param wrapMsg
    */
   public void setWrapMessage(boolean wrapMsg) {
     this.wrapMsg = wrapMsg;
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

  public void setUseRelativeTimes(long timeStamp) {
    useRelativeTimes = true;
    relativeTimestampBase = timeStamp;
  }

  public void setUseNormalTimes() {
    useRelativeTimes = false;
  }
}
