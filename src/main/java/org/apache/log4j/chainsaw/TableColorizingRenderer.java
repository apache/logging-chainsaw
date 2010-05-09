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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.icons.LevelIconFactory;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.helpers.Transform;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEventFieldResolver;


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
  private boolean levelUseIcons = false;
  private boolean wrap = false;
  private boolean highlightSearchMatchText;
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

  private final JLabel levelLabel = new JLabel();
  private JLabel generalLabel = new JLabel();

  private final JPanel multiLinePanel = new JPanel();
  private final JPanel generalPanel = new JPanel();
  private final JPanel levelPanel = new JPanel();

    /**
   * Creates a new TableColorizingRenderer object.
   */
  public TableColorizingRenderer(RuleColorizer colorizer) {
    multiLinePanel.setLayout(new BoxLayout(multiLinePanel, BoxLayout.Y_AXIS));
    generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
    levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.Y_AXIS));

    multiLinePanel.setAlignmentX(TOP_ALIGNMENT);
    generalPanel.setAlignmentX(TOP_ALIGNMENT);
    levelPanel.setAlignmentX(TOP_ALIGNMENT);

    generalLabel.setVerticalAlignment(SwingConstants.TOP);
    levelLabel.setVerticalAlignment(SwingConstants.TOP);
    levelLabel.setOpaque(true);
    levelLabel.setText("");

    generalPanel.add(generalLabel);
    levelPanel.add(levelLabel);

    this.colorizer = colorizer;
  }

  public void setToolTipsVisible(boolean toolTipsVisible) {
      this.toolTipsVisible = toolTipsVisible;
  }

  public Component getTableCellRendererComponent(
    final JTable table, Object value, boolean isSelected, boolean hasFocus,
    int row, int col) {
    value = formatField(value);
    TableColumn tableColumn = table.getColumnModel().getColumn(col);

    //null unless needed
    JTextPane multiLineTextPane = null;
    JLabel label = (JLabel)super.getTableCellRendererComponent(table, value,
        isSelected, hasFocus, row, col);
    //chainsawcolumns uses one-based indexing
    int colIndex = tableColumn.getModelIndex() + 1;

    EventContainer container = (EventContainer) table.getModel();
    ExtendedLoggingEvent loggingEvent = container.getRow(row);
    //no event, use default renderer
    if (loggingEvent == null) {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
    }
    Map matches = loggingEvent.getSearchMatches();

    JComponent component;
    switch (colIndex) {
    case ChainsawColumns.INDEX_THROWABLE_COL_NAME:
      if (value instanceof String[] && ((String[])value).length > 0){
          //exception string is split into an array..just highlight the first line completely if anything in the exception matches if we have a match for the exception field
          Set exceptionMatches = (Set)matches.get(LoggingEventFieldResolver.EXCEPTION_FIELD);
          if (exceptionMatches != null && exceptionMatches.size() > 0) {
              generalLabel.setText(bold(((String[])value)[0]));
          } else {
              generalLabel.setText(((String[])value)[0]);
          }
      } else {
        generalLabel.setText("");
      }
      component = generalPanel;
      break;
    case ChainsawColumns.INDEX_LOGGER_COL_NAME:
      String logger = value.toString();
      int startPos = -1;

      for (int i = 0; i < loggerPrecision; i++) {
        startPos = logger.indexOf(".", startPos + 1);
        if (startPos < 0) {
          break;
        }
      }
      generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.LOGGER_FIELD), logger.substring(startPos + 1)));
      component = generalPanel;
      break;
    case ChainsawColumns.INDEX_ID_COL_NAME:
        generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.PROP_FIELD + "LOG4JID"), value.toString()));
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_CLASS_COL_NAME:
        generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.CLASS_FIELD), value.toString()));
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_FILE_COL_NAME:
        generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.FILE_FIELD), value.toString()));
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_LINE_COL_NAME:
        generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.LINE_FIELD), value.toString()));
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_NDC_COL_NAME:
        generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.NDC_FIELD), value.toString()));
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_THREAD_COL_NAME:
        generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.THREAD_FIELD), value.toString()));
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_TIMESTAMP_COL_NAME:
        //timestamp matches contain the millis..not the display text..just highlight if we have a match for the timestamp field
        Set timestampMatches = (Set)matches.get(LoggingEventFieldResolver.TIMESTAMP_FIELD);
        if (timestampMatches != null && timestampMatches.size() > 0) {
            generalLabel.setText(bold(value.toString()));
        } else {
            generalLabel.setText(value.toString());
        }
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_METHOD_COL_NAME:
        generalLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.METHOD_FIELD), value.toString()));
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME:
    case ChainsawColumns.INDEX_MESSAGE_COL_NAME:
        int width = tableColumn.getWidth();

        String thisString;
        if (colIndex == ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME) {
            //property keys are set as all uppercase
            thisString = buildHighlightString(matches.get(LoggingEventFieldResolver.PROP_FIELD + ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE.toUpperCase()), value.toString().trim());
        } else {
            thisString = buildHighlightString(matches.get(LoggingEventFieldResolver.MSG_FIELD), value.toString().trim());
        }
        int tableRowHeight = table.getRowHeight(row);
        multiLineTextPane = new JTextPane();
        multiLineTextPane.setEditorKit(new HTMLEditorKit());
        multiLineTextPane.setMargin(null);
        multiLineTextPane.setEditable(false);
        multiLineTextPane.setFont(label.getFont());
        setText(thisString);
        multiLinePanel.removeAll();
        multiLinePanel.add(multiLineTextPane);
        HTMLDocument document = new HTMLDocument();
        multiLineTextPane.setDocument(document);
        Font font = label.getFont();
        String bodyRule = "body { font-family: " + font.getFamily() + "; font-size: " + (font.getSize()) + "pt; }";
        ((HTMLDocument)multiLineTextPane.getDocument()).getStyleSheet().addRule(bodyRule);
        multiLineTextPane.setText(thisString);
        if (wrap) {
            Map paramMap = new HashMap();
            paramMap.put(TextAttribute.FONT, multiLineTextPane.getFont());

            int calculatedHeight = calculateHeight(thisString, width, paramMap);
            //set preferred size to default height
            multiLineTextPane.setSize(new Dimension(width, calculatedHeight));

            int multiLinePanelPrefHeight = multiLinePanel.getPreferredSize().height;
            if(tableRowHeight < multiLinePanelPrefHeight) {
                table.setRowHeight(row, Math.max(ChainsawConstants.DEFAULT_ROW_HEIGHT, multiLinePanelPrefHeight));
            }
        }
        component = multiLinePanel;
        break;
    case ChainsawColumns.INDEX_LEVEL_COL_NAME:
      if (levelUseIcons) {
        levelLabel.setIcon((Icon) iconMap.get(value.toString()));

        if (levelLabel.getIcon() != null) {
          levelLabel.setText("");
        }
        if (!toolTipsVisible) {
          levelLabel.setToolTipText(value.toString());
        }
      } else {
        levelLabel.setIcon(null);
        levelLabel.setText(buildHighlightString(matches.get(LoggingEventFieldResolver.LEVEL_FIELD), value.toString()));
        if (!toolTipsVisible) {
            levelLabel.setToolTipText(null);
        }
      }
      if (toolTipsVisible) {
          levelLabel.setToolTipText(label.getToolTipText());
      }
      levelLabel.setForeground(label.getForeground());
      levelLabel.setBackground(label.getBackground());
      component = levelPanel;
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
            String propKey = LoggingEventFieldResolver.PROP_FIELD + thisProp.toUpperCase();
            Set propKeyMatches = (Set)matches.get(propKey);
            generalLabel.setText(buildHighlightString(propKeyMatches, loggingEvent.getProperty(thisProp)));
        } else {
            generalLabel.setText("");
        }
        component = generalPanel;
        break;
    }

    Color background;
    Color foreground;
    Rule loggerRule = colorizer.getLoggerRule();
    //use logger colors in table instead of event colors if event passes logger rule
    if (loggerRule != null && loggerRule.evaluate(loggingEvent, null)) {
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

    //set the colors of the components inside 'component'
    if (multiLineTextPane != null)
    {
        multiLineTextPane.setBackground(background);
        multiLineTextPane.setForeground(foreground);
    }
    levelLabel.setBackground(background);
    levelLabel.setForeground(foreground);
    generalLabel.setBackground(background);
    generalLabel.setForeground(foreground);

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
     this.wrap = wrapMsg;
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

   private int calculateHeight(String string, int width, Map paramMap) {
     if (string.trim().length() == 0) {
         return ChainsawConstants.DEFAULT_ROW_HEIGHT;
     }
     AttributedCharacterIterator paragraph = new AttributedString(string, paramMap).getIterator();
     LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, new FontRenderContext(null, true, true));
     float height = 0;
     lineMeasurer.setPosition(paragraph.getBeginIndex());
     TextLayout layout;
     while (lineMeasurer.getPosition() < paragraph.getEndIndex()) {
       layout = lineMeasurer.nextLayout(width);
         float layoutHeight = layout.getAscent() + layout.getDescent() + layout.getLeading();
         height += layoutHeight;
     }
     return Math.max(ChainsawConstants.DEFAULT_ROW_HEIGHT, (int) height);
    }

    private String buildHighlightString(Object matchSet, String input) {
        if (!highlightSearchMatchText) {
            return Transform.escapeTags(input);
        }
        if (matchSet instanceof Set) {
            Set thisSet = (Set)matchSet;
            //start with result as input and replace each time
            String result = input;
            for (Iterator iter = thisSet.iterator();iter.hasNext();) {
                String thisEntry = iter.next().toString();
                result = bold(result, thisEntry);
            }
            return "<html>" + escapeAllButBoldTags(result) + "</html>";
        }
        return Transform.escapeTags(input);
    }

    private String escapeAllButBoldTags(String input) {
            if (!highlightSearchMatchText) {
                return Transform.escapeTags(input);
            }
            String lowerInput = input.toLowerCase();
            String lowerBoldStart = "<b>";
            String lowerBoldEnd = "</b>";
            int boldStartLength = lowerBoldStart.length();
            int boldEndLength = lowerBoldEnd.length();
            int firstIndex = 0;
            int currentIndex = 0;
            StringBuffer newString = new StringBuffer("");
            while ((currentIndex = lowerInput.indexOf(lowerBoldStart, firstIndex)) > -1) {
                newString.append(Transform.escapeTags(input.substring(firstIndex, currentIndex)));
                newString.append(lowerBoldStart);
                firstIndex = currentIndex + boldStartLength;
                currentIndex = lowerInput.indexOf(lowerBoldEnd, firstIndex);
                if (currentIndex > -1) {
                    newString.append(Transform.escapeTags(input.substring(firstIndex, currentIndex)));
                    newString.append(lowerBoldEnd);
                    firstIndex = currentIndex + boldEndLength;
                }
            }
            newString.append(Transform.escapeTags(input.substring(firstIndex, input.length())));
            return newString.toString();
        }

    private String bold(String input) {
        if (!highlightSearchMatchText) {
            return Transform.escapeTags(input);
        }
        return "<html><b>" + Transform.escapeTags(input) + "</b></html>";
    }
    
    private String bold(String input, String textToBold) {
        String lowerInput = input.toLowerCase();
        String lowerTextToBold = textToBold.toLowerCase();
        int textToBoldLength = textToBold.length();
        int firstIndex = 0;
        int currentIndex = 0;
        StringBuffer newString = new StringBuffer("");
        while ((currentIndex = lowerInput.indexOf(lowerTextToBold, firstIndex)) > -1) {
            newString.append(input.substring(firstIndex, currentIndex));
            newString.append("<b>");
            newString.append(input.substring(currentIndex, currentIndex + textToBoldLength));
            newString.append("</b>");
            firstIndex = currentIndex + textToBoldLength;
        }
        newString.append(input.substring(firstIndex, input.length()));
        return newString.toString();
    }

    public void setHighlightSearchMatchText(boolean highlightSearchMatchText)
    {
        this.highlightSearchMatchText = highlightSearchMatchText;
    }
}
