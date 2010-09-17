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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.icons.LevelIconFactory;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEvent;
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
  private boolean useRelativeTimesToFixedTime = false;
  private long relativeTimestampBase;

  private static int borderWidth = 2;

  private static Color borderColor = (Color)UIManager.get("Table.selectionBackground");

  private final JTextPane levelTextPane = new JTextPane();
  private JTextPane singleLineTextPane = new JTextPane();

  private final JPanel multiLinePanel = new JPanel(new BorderLayout());
  private final JPanel generalPanel = new JPanel(new BorderLayout());
  private final JPanel levelPanel = new JPanel(new BorderLayout());
  private ApplicationPreferenceModel applicationPreferenceModel;
  private JTextPane multiLineTextPane;
  private MutableAttributeSet boldAttributeSet;
  private TabSet tabs;
  private int maxHeight;
  private boolean useRelativeTimesToPrevious;
  private EventContainer eventContainer;
  private LogPanelPreferenceModel logPanelPreferenceModel;
  private SimpleAttributeSet insetAttributeSet;

    /**
   * Creates a new TableColorizingRenderer object.
   */
  public TableColorizingRenderer(RuleColorizer colorizer, ApplicationPreferenceModel applicationPreferenceModel,
                                 EventContainer eventContainer, LogPanelPreferenceModel logPanelPreferenceModel) {
    this.applicationPreferenceModel = applicationPreferenceModel;
    this.logPanelPreferenceModel = logPanelPreferenceModel;
    this.eventContainer = eventContainer;
    multiLinePanel.setLayout(new BoxLayout(multiLinePanel, BoxLayout.Y_AXIS));
    generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
    levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.Y_AXIS));
    maxHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

    //define the 'bold' attributeset
    boldAttributeSet = new SimpleAttributeSet();
    StyleConstants.setBold(boldAttributeSet, true);

    insetAttributeSet = new SimpleAttributeSet();
    StyleConstants.setLeftIndent(insetAttributeSet, 6);
    //throwable col may have a tab..if so, render the tab as col zero
    int pos = 0;
    int align = TabStop.ALIGN_LEFT;
    int leader = TabStop.LEAD_NONE;
    TabStop tabStop = new TabStop(pos, align, leader);
    tabs = new TabSet(new TabStop[]{tabStop});

    levelTextPane.setOpaque(true);
    levelTextPane.setText("");

    levelPanel.add(levelTextPane);

    this.colorizer = colorizer;
    multiLineTextPane = new JTextPane();
    multiLineTextPane.setEditorKit(new StyledEditorKit());

    singleLineTextPane.setEditorKit(new OneLineEditorKit());
    levelTextPane.setEditorKit(new OneLineEditorKit());

    multiLineTextPane.setEditable(false);
    multiLineTextPane.setFont(levelTextPane.getFont());

    multiLineTextPane.setParagraphAttributes(insetAttributeSet, false);
    singleLineTextPane.setParagraphAttributes(insetAttributeSet, false);
    levelTextPane.setParagraphAttributes(insetAttributeSet, false);
  }

  public void setToolTipsVisible(boolean toolTipsVisible) {
      this.toolTipsVisible = toolTipsVisible;
  }

  public Component getTableCellRendererComponent(
    final JTable table, Object value, boolean isSelected, boolean hasFocus,
    int row, int col) {
    EventContainer container = (EventContainer) table.getModel();
    ExtendedLoggingEvent loggingEvent = container.getRow(row);
    value = formatField(value, row, loggingEvent);
    TableColumn tableColumn = table.getColumnModel().getColumn(col);
    int width = tableColumn.getWidth();

    JLabel label = (JLabel)super.getTableCellRendererComponent(table, value,
        isSelected, hasFocus, row, col);
    //chainsawcolumns uses one-based indexing
    int colIndex = tableColumn.getModelIndex() + 1;

    //no event, use default renderer
    if (loggingEvent == null) {
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
    }
    long delta = 0;
    if (row > 0) {
        LoggingEvent previous = eventContainer.getRow(row - 1);
        delta = Math.min(ChainsawConstants.MILLIS_DELTA_RENDERING_HEIGHT_MAX, Math.max(0, (long) ((loggingEvent.getTimeStamp() - previous.getTimeStamp()) * ChainsawConstants.MILLIS_DELTA_RENDERING_FACTOR)));
    }

    Map matches = loggingEvent.getSearchMatches();

    JComponent component;
    switch (colIndex) {
    case ChainsawColumns.INDEX_THROWABLE_COL_NAME:
      if (value instanceof String[] && ((String[])value).length > 0){
          Style tabStyle = singleLineTextPane.getLogicalStyle();
          StyleConstants.setTabSet(tabStyle, tabs);
          //set the 1st tab at position 3
          singleLineTextPane.setLogicalStyle(tabStyle);
          //exception string is split into an array..just highlight the first line completely if anything in the exception matches if we have a match for the exception field
          Set exceptionMatches = (Set)matches.get(LoggingEventFieldResolver.EXCEPTION_FIELD);
          if (exceptionMatches != null && exceptionMatches.size() > 0) {
              singleLineTextPane.setText(((String[])value)[0]);
              boldAll((StyledDocument) singleLineTextPane.getDocument());
          } else {
              singleLineTextPane.setText(((String[])value)[0]);
          }
      } else {
        singleLineTextPane.setText("");
      }
      layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
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
        singleLineTextPane.setText(logger.substring(startPos + 1));
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.LOGGER_FIELD), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
      break;
    case ChainsawColumns.INDEX_ID_COL_NAME:
        singleLineTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.PROP_FIELD + "LOG4JID"), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_CLASS_COL_NAME:
        singleLineTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.CLASS_FIELD), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_FILE_COL_NAME:
        singleLineTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.FILE_FIELD), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_LINE_COL_NAME:
        singleLineTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.LINE_FIELD), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_NDC_COL_NAME:
        singleLineTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.NDC_FIELD), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_THREAD_COL_NAME:
        singleLineTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.THREAD_FIELD), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_TIMESTAMP_COL_NAME:
        //timestamp matches contain the millis..not the display text..just highlight if we have a match for the timestamp field
        Set timestampMatches = (Set)matches.get(LoggingEventFieldResolver.TIMESTAMP_FIELD);
        if (timestampMatches != null && timestampMatches.size() > 0) {
            singleLineTextPane.setText(value.toString());
            boldAll((StyledDocument) singleLineTextPane.getDocument());
        } else {
            singleLineTextPane.setText(value.toString());
        }
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_METHOD_COL_NAME:
        singleLineTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.METHOD_FIELD), (StyledDocument) singleLineTextPane.getDocument());
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    case ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME:
    case ChainsawColumns.INDEX_MESSAGE_COL_NAME:
        String thisString = value.toString().trim();
        multiLineTextPane.setText(thisString);

        if (colIndex == ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME) {
            //property keys are set as all uppercase
            setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.PROP_FIELD + ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE.toUpperCase()), (StyledDocument) multiLineTextPane.getDocument());
        } else {
            setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.MSG_FIELD), (StyledDocument) multiLineTextPane.getDocument());
        }
        multiLinePanel.removeAll();
        if (delta > 0 && logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
            JPanel newPanel = new JPanel();
            newPanel.setOpaque(true);
            newPanel.setBackground(applicationPreferenceModel.getDeltaColor());
            newPanel.setPreferredSize(new Dimension(width, (int) delta));
            multiLinePanel.add(newPanel, BorderLayout.NORTH);
        }
        multiLinePanel.add(multiLineTextPane, BorderLayout.SOUTH);

        if (delta == 0 || !logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
          if (col == 0) {
            multiLineTextPane.setBorder(getLeftBorder(isSelected, delta));
          } else if (col == table.getColumnCount() - 1) {
            multiLineTextPane.setBorder(getRightBorder(isSelected, delta));
          } else {
            multiLineTextPane.setBorder(getMiddleBorder(isSelected, delta));
          }
        } else {
            if (col == 0) {
              multiLineTextPane.setBorder(getLeftBorder(isSelected, 0));
            } else if (col == table.getColumnCount() - 1) {
              multiLineTextPane.setBorder(getRightBorder(isSelected, 0));
            } else {
              multiLineTextPane.setBorder(getMiddleBorder(isSelected, 0));
            }
        }
        int currentMarkerHeight = loggingEvent.getMarkerHeight();
        int currentMsgHeight = loggingEvent.getMsgHeight();
        int newRowHeight = ChainsawConstants.DEFAULT_ROW_HEIGHT;
        boolean setHeight = false;

        if (wrap) {
            /*
            calculating the height -would- be the correct thing to do, but setting the size to screen size works as well and
            doesn't incur massive overhead, like calculateHeight does
            Map paramMap = new HashMap();
            paramMap.put(TextAttribute.FONT, multiLineTextPane.getFont());

            int calculatedHeight = calculateHeight(thisString, width, paramMap);
             */
            //instead, set size to max height
            multiLineTextPane.setSize(new Dimension(width, maxHeight));
            int multiLinePanelPrefHeight = multiLinePanel.getPreferredSize().height;
            newRowHeight = Math.max(ChainsawConstants.DEFAULT_ROW_HEIGHT, multiLinePanelPrefHeight);

        }
        if (!wrap && logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
            multiLineTextPane.setSize(new Dimension(Integer.MAX_VALUE, ChainsawConstants.DEFAULT_ROW_HEIGHT));
            newRowHeight = (int) (ChainsawConstants.DEFAULT_ROW_HEIGHT + delta);
        }

        if (colIndex == ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME) {
            loggingEvent.setMarkerHeight(newRowHeight);
            if (newRowHeight != currentMarkerHeight && newRowHeight >= loggingEvent.getMsgHeight()) {
                setHeight = true;
            }
        }

        if (colIndex == ChainsawColumns.INDEX_MESSAGE_COL_NAME) {
            loggingEvent.setMsgHeight(newRowHeight);
            if (newRowHeight != currentMsgHeight && newRowHeight >= loggingEvent.getMarkerHeight()) {
                setHeight = true;
            }
        }
        if (setHeight) {
            table.setRowHeight(row, newRowHeight);
        }

        component = multiLinePanel;
        break;
    case ChainsawColumns.INDEX_LEVEL_COL_NAME:
      if (levelUseIcons) {
        levelTextPane.setText("");
        levelTextPane.insertIcon((Icon) iconMap.get(value.toString()));
        if (!toolTipsVisible) {
          levelTextPane.setToolTipText(value.toString());
        }
      } else {
        levelTextPane.setText(value.toString());
        setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.LEVEL_FIELD), (StyledDocument) levelTextPane.getDocument());
        if (!toolTipsVisible) {
            levelTextPane.setToolTipText(null);
        }
      }
      if (toolTipsVisible) {
          levelTextPane.setToolTipText(label.getToolTipText());
      }
      levelTextPane.setForeground(label.getForeground());
      levelTextPane.setBackground(label.getBackground());
      layoutRenderingPanel(levelPanel, levelTextPane, delta, isSelected, width, col, table);
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
            singleLineTextPane.setText(loggingEvent.getProperty(thisProp));
            setHighlightAttributesInternal(propKeyMatches, (StyledDocument) singleLineTextPane.getDocument());
        } else {
            singleLineTextPane.setText("");
        }
        layoutRenderingPanel(generalPanel, singleLineTextPane, delta, isSelected, width, col, table);
        component = generalPanel;
        break;
    }

    Color background;
    Color foreground;
    Rule loggerRule = colorizer.getLoggerRule();
    //use logger colors in table instead of event colors if event passes logger rule
    if (loggerRule != null && loggerRule.evaluate(loggingEvent, null)) {
        background = applicationPreferenceModel.getSearchBackgroundColor();
        foreground = applicationPreferenceModel.getSearchForegroundColor();
    } else {
        background = loggingEvent.isSearchMatch()?applicationPreferenceModel.getSearchBackgroundColor():loggingEvent.getBackground();
        foreground = loggingEvent.isSearchMatch()?applicationPreferenceModel.getSearchForegroundColor():loggingEvent.getForeground();
    }

    /**
     * Colourize background based on row striping if the event still has default foreground and background color
     */
    if (background.equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND) && foreground.equals(ChainsawConstants.COLOR_DEFAULT_FOREGROUND)) {
      if ((row % 2) != 0) {
        background = applicationPreferenceModel.getAlternatingColorBackgroundColor();
        foreground = applicationPreferenceModel.getAlternatingColorForegroundColor();
      }
    }

    component.setBackground(background);
    component.setForeground(foreground);

    //update the background & foreground of the jtextpane using styles
    if (multiLineTextPane != null)
    {
        updateColors(multiLineTextPane, background, foreground);
    }
    updateColors(levelTextPane, background, foreground);
    updateColors(singleLineTextPane, background, foreground);

    return component;
  }

    private void layoutRenderingPanel(JComponent container, JComponent bottomComponent, long delta, boolean isSelected,
                                      int width, int col, JTable table) {
        container.removeAll();
        if (delta == 0 || !logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
          if (col == 0) {
            bottomComponent.setBorder(getLeftBorder(isSelected, delta));
          } else if (col == table.getColumnCount() - 1) {
            bottomComponent.setBorder(getRightBorder(isSelected, delta));
          } else {
            bottomComponent.setBorder(getMiddleBorder(isSelected, delta));
          }
        } else {
            JPanel newPanel = new JPanel();
            newPanel.setOpaque(true);
            newPanel.setBackground(applicationPreferenceModel.getDeltaColor());
            newPanel.setPreferredSize(new Dimension(width, (int) delta));
            container.add(newPanel, BorderLayout.NORTH);
            if (col == 0) {
              bottomComponent.setBorder(getLeftBorder(isSelected, 0));
            } else if (col == table.getColumnCount() - 1) {
              bottomComponent.setBorder(getRightBorder(isSelected, 0));
            } else {
              bottomComponent.setBorder(getMiddleBorder(isSelected, 0));
            }
        }

        container.add(bottomComponent, BorderLayout.SOUTH);
    }

    private Border getLeftBorder(boolean isSelected, long delta) {
        Border LEFT_BORDER = BorderFactory.createMatteBorder(borderWidth, borderWidth, borderWidth, 0, borderColor);
        Border LEFT_EMPTY_BORDER = BorderFactory.createEmptyBorder(borderWidth, borderWidth, borderWidth, 0);

        Border innerBorder =isSelected?LEFT_BORDER : LEFT_EMPTY_BORDER;
        if (delta == 0 || !wrap || !logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
            return innerBorder;
        } else {
            Border outerBorder = BorderFactory.createMatteBorder((int) Math.max(borderWidth, delta), 0, 0, 0, applicationPreferenceModel.getDeltaColor());
            return BorderFactory.createCompoundBorder(outerBorder, innerBorder);
        }
    }

    private Border getRightBorder(boolean isSelected, long delta) {
        Border RIGHT_BORDER = BorderFactory.createMatteBorder(borderWidth, 0, borderWidth, borderWidth, borderColor);
        Border RIGHT_EMPTY_BORDER = BorderFactory.createEmptyBorder(borderWidth, 0, borderWidth, borderWidth);
        Border innerBorder =isSelected?RIGHT_BORDER : RIGHT_EMPTY_BORDER;
        if (delta == 0 || !wrap || !logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
            return innerBorder;
        } else {
            Border outerBorder = BorderFactory.createMatteBorder((int) Math.max(borderWidth, delta), 0, 0, 0, applicationPreferenceModel.getDeltaColor());
            return BorderFactory.createCompoundBorder(outerBorder, innerBorder);
        }
    }

    private Border getMiddleBorder(boolean isSelected, long delta) {
        Border MIDDLE_BORDER = BorderFactory.createMatteBorder(borderWidth, 0, borderWidth, 0, borderColor);
        Border MIDDLE_EMPTY_BORDER = BorderFactory.createEmptyBorder(borderWidth, 0, borderWidth, 0);
        Border innerBorder =isSelected ?MIDDLE_BORDER : MIDDLE_EMPTY_BORDER;
        if (delta == 0 || !wrap || !logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
            return innerBorder;
        } else {
            Border outerBorder = BorderFactory.createMatteBorder((int)Math.max(borderWidth, delta), 0, 0, 0, applicationPreferenceModel.getDeltaColor());
            return BorderFactory.createCompoundBorder(outerBorder, innerBorder);
        }
    }

    private void updateColors(JTextPane textPane, Color background, Color foreground)
    {
        StyledDocument styledDocument = textPane.getStyledDocument();
        MutableAttributeSet attributes = textPane.getInputAttributes();
        StyleConstants.setForeground(attributes, foreground);
        styledDocument.setCharacterAttributes(0, styledDocument.getLength() + 1, attributes, false);
        textPane.setBackground(background);
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
   * @param field object
   *
   * @param renderingRow
   * @return formatted object
   */
  private Object formatField(Object field, int renderingRow, ExtendedLoggingEvent loggingEvent) {
    if (!(field instanceof Date)) {
      return (field == null ? "" : field);
    }

    //handle date field
    if (useRelativeTimesToFixedTime) {
        return "" + (((Date)field).getTime() - relativeTimestampBase);
    }
    if (useRelativeTimesToPrevious) {
        return loggingEvent.getProperty(ChainsawConstants.MILLIS_DELTA);
    }

    return dateFormatInUse.format((Date) field);
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
    useRelativeTimesToFixedTime = true;
    useRelativeTimesToPrevious = false;
    relativeTimestampBase = timeStamp;
  }

  public void setUseRelativeTimesToPreviousRow() {
     useRelativeTimesToFixedTime = false;
     useRelativeTimesToPrevious = true;
  }

  public void setUseNormalTimes() {
    useRelativeTimesToFixedTime = false;
    useRelativeTimesToPrevious = false;
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

    private void setHighlightAttributesInternal(Object matchSet, StyledDocument styledDocument) {
        if (!highlightSearchMatchText) {
            return;
        }
        setHighlightAttributes(matchSet, styledDocument);
    }

    public void setHighlightAttributes(Object matchSet, StyledDocument styledDocument) {
        if (matchSet instanceof Set) {
            Set thisSet = (Set)matchSet;
            for (Iterator iter = thisSet.iterator();iter.hasNext();) {
                String thisEntry = iter.next().toString();
                bold(thisEntry, styledDocument);
            }
        }
    }

    private void boldAll(StyledDocument styledDocument) {
        if (!highlightSearchMatchText) {
            return;
        }
        styledDocument.setCharacterAttributes(0, styledDocument.getLength(), boldAttributeSet, false);
    }
    
    private void bold(String textToBold, StyledDocument styledDocument) {
        try {
            String lowerInput = styledDocument.getText(0, styledDocument.getLength()).toLowerCase();
            String lowerTextToBold = textToBold.toLowerCase();
            int textToBoldLength = textToBold.length();
            int firstIndex = 0;
            int currentIndex;
            while ((currentIndex = lowerInput.indexOf(lowerTextToBold, firstIndex)) > -1) {
                styledDocument.setCharacterAttributes(currentIndex, textToBoldLength, boldAttributeSet, false);
                firstIndex = currentIndex + textToBoldLength;
            }
        }
        catch (BadLocationException e) {
            //ignore
        }
    }

    public void setHighlightSearchMatchText(boolean highlightSearchMatchText)
    {
        this.highlightSearchMatchText = highlightSearchMatchText;
    }

    private class OneLineEditorKit extends StyledEditorKit {
        private ViewFactory viewFactoryImpl = new ViewFactoryImpl();

        public ViewFactory getViewFactory() {
            return viewFactoryImpl;
        }
    }

    private class ViewFactoryImpl implements ViewFactory {
        public View create(Element elem)
        {
            String elementName = elem.getName();
            if (elementName != null)
            {
                if (elementName.equals(AbstractDocument.ParagraphElementName)) {
                    return new OneLineParagraphView(elem);
                } else  if (elementName.equals(AbstractDocument.ContentElementName)) {
                    return new LabelView(elem);
                } else if (elementName.equals(AbstractDocument.SectionElementName)) {
                    return new BoxView(elem, View.Y_AXIS);
                } else if (elementName.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (elementName.equals(StyleConstants.IconElementName)) {
                    return new IconView(elem);
                }
            }
            return new LabelView(elem);
        }
    }

    private class OneLineParagraphView extends ParagraphView {
        public OneLineParagraphView(Element elem) {
            super(elem);
        }

        //this is the main fix - set the flow span to be max val
        public int getFlowSpan(int index) {
            return Integer.MAX_VALUE;
        }
    }
}
