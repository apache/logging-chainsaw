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

import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.components.logpanel.LogPanelPreferenceModel;
import org.apache.log4j.chainsaw.icons.LevelIconFactory;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.Rule;
import org.apache.log4j.spi.LoggingEventFieldResolver;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.log4j.chainsaw.prefs.SettingsManager;


/**
 * A specific TableCellRenderer that colourizes a particular cell based on
 * some ColourFilters that have been stored according to the value for the row
 *
 * @author Claude Duguay
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class TableColorizingRenderer extends DefaultTableCellRenderer {
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat(Constants.SIMPLE_TIME_PATTERN);
    private final Map<String, Icon> iconMap;
    private RuleColorizer colorizer;
    private boolean levelUseIcons = false;
    private boolean wrap = false;
    private boolean highlightSearchMatchText;
    private DateFormat dateFormatInUse = DATE_FORMATTER;
    private int loggerPrecision = 0;
    private boolean toolTipsVisible;
    private String dateFormatTZ;
    private boolean useRelativeTimesToFixedTime = false;
    private ZonedDateTime relativeTimestampBase;

    private static int borderWidth = ChainsawConstants.TABLE_BORDER_WIDTH;

    private final Color borderColor;

    private final JTextPane levelTextPane = new JTextPane();
    private JTextPane singleLineTextPane = new JTextPane();

    private final JPanel multiLinePanel = new JPanel(new BorderLayout());
    private final JPanel generalPanel = new JPanel(new BorderLayout());
    private final JPanel levelPanel = new JPanel(new BorderLayout());
    private SettingsManager settingsManager;
    private ApplicationPreferenceModel applicationPreferenceModel;
    private JTextPane multiLineTextPane;
    private MutableAttributeSet boldAttributeSet;
    private TabSet tabs;
    private int maxHeight;
    private boolean useRelativeTimesToPrevious;
    private EventContainer eventContainer;
    private LogPanelPreferenceModel logPanelPreferenceModel;
    private SimpleAttributeSet insetAttributeSet;
    private boolean colorizeSearch;

    /**
     * Creates a new TableColorizingRenderer object.
     */
    public TableColorizingRenderer(SettingsManager settingsManager, RuleColorizer colorizer,
                                   EventContainer eventContainer, LogPanelPreferenceModel logPanelPreferenceModel,
                                   ApplicationPreferenceModel applicationPreferenceModel, boolean colorizeSearch) {
        this.settingsManager = settingsManager;
        this.applicationPreferenceModel = applicationPreferenceModel;
        this.logPanelPreferenceModel = logPanelPreferenceModel;
        this.eventContainer = eventContainer;
        this.colorizeSearch = colorizeSearch;
        multiLinePanel.setLayout(new BoxLayout(multiLinePanel, BoxLayout.Y_AXIS));
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.Y_AXIS));
        maxHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

        iconMap = new HashMap<>();
        try {
            iconMap.putAll(LevelIconFactory.getInstance().getLevelToIconMap());
        } catch (IllegalStateException ise) {
            //ignore
        }

        if (UIManager.get("Table.selectionBackground") != null) {
            borderColor = (Color) UIManager.get("Table.selectionBackground");
        } else {
            borderColor = Color.BLUE;
        }
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
        LoggingEventWrapper loggingEventWrapper = container.getRow(row);
        value = formatField(value, loggingEventWrapper);
        TableColumn tableColumn = table.getColumnModel().getColumn(col);
        int width = tableColumn.getWidth();
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
            isSelected, hasFocus, row, col);
        //chainsawcolumns uses one-based indexing
        int colIndex = tableColumn.getModelIndex() + 1;

        //no event, use default renderer
        if (loggingEventWrapper == null) {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        }
        long delta = 0;
        if (row > 0) {
            LoggingEventWrapper previous = eventContainer.getRow(row - 1);
            long millisBetween = ChronoUnit.MILLIS.between( loggingEventWrapper.getLoggingEvent().m_timestamp, previous.getLoggingEvent().m_timestamp );
            delta = Math.min(ChainsawConstants.MILLIS_DELTA_RENDERING_HEIGHT_MAX, 
                    Math.max(0, (long) ((millisBetween) * ChainsawConstants.MILLIS_DELTA_RENDERING_FACTOR)));
        }

        Map matches = loggingEventWrapper.getSearchMatches();

        JComponent component;
        switch (colIndex) {
            case ChainsawColumns.INDEX_THROWABLE_COL_NAME:
                if (value instanceof String[] && ((String[]) value).length > 0) {
                    Style tabStyle = singleLineTextPane.getLogicalStyle();
                    StyleConstants.setTabSet(tabStyle, tabs);
                    //set the 1st tab at position 3
                    singleLineTextPane.setLogicalStyle(tabStyle);
                    //exception string is split into an array..just highlight the first line completely if anything in the exception matches if we have a match for the exception field
                    Set exceptionMatches = (Set) matches.get(LoggingEventFieldResolver.EXCEPTION_FIELD);
                    if (exceptionMatches != null && exceptionMatches.size() > 0) {
                        singleLineTextPane.setText(((String[]) value)[0]);
                        boldAll((StyledDocument) singleLineTextPane.getDocument());
                    } else {
                        singleLineTextPane.setText(((String[]) value)[0]);
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
                Set timestampMatches = (Set) matches.get(LoggingEventFieldResolver.TIMESTAMP_FIELD);
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
                JTextPane textPane = wrap ? multiLineTextPane : singleLineTextPane;
                JComponent textPaneContainer = wrap ? multiLinePanel : generalPanel;
                textPane.setText(thisString);

                if (colIndex == ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME) {
                    //property keys are set as all uppercase
                    setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.PROP_FIELD + ChainsawConstants.LOG4J_MARKER_COL_NAME), (StyledDocument) textPane.getDocument());
                } else {
                    setHighlightAttributesInternal(matches.get(LoggingEventFieldResolver.MSG_FIELD), (StyledDocument) textPane.getDocument());
                }
                textPaneContainer.removeAll();
                if (delta > 0 && logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
                    JPanel newPanel = new JPanel();
                    newPanel.setOpaque(true);
                    newPanel.setBackground(applicationPreferenceModel.getDeltaColor());
                    newPanel.setPreferredSize(new Dimension(width, (int) delta));
                    textPaneContainer.add(newPanel, BorderLayout.NORTH);
                }
                textPaneContainer.add(textPane, BorderLayout.SOUTH);

                if (delta == 0 || !logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
                    if (col == 0) {
                        textPane.setBorder(getLeftBorder(isSelected, delta));
                    } else if (col == table.getColumnCount() - 1) {
                        textPane.setBorder(getRightBorder(isSelected, delta));
                    } else {
                        textPane.setBorder(getMiddleBorder(isSelected, delta));
                    }
                } else {
                    if (col == 0) {
                        textPane.setBorder(getLeftBorder(isSelected, 0));
                    } else if (col == table.getColumnCount() - 1) {
                        textPane.setBorder(getRightBorder(isSelected, 0));
                    } else {
                        textPane.setBorder(getMiddleBorder(isSelected, 0));
                    }
                }
                int currentMarkerHeight = loggingEventWrapper.getMarkerHeight();
                int currentMsgHeight = loggingEventWrapper.getMsgHeight();
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
                    textPane.setSize(new Dimension(width, maxHeight));
                    int multiLinePanelPrefHeight = textPaneContainer.getPreferredSize().height;
                    newRowHeight = Math.max(ChainsawConstants.DEFAULT_ROW_HEIGHT, multiLinePanelPrefHeight);

                }
                if (!wrap && logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
                    textPane.setSize(new Dimension(Integer.MAX_VALUE, ChainsawConstants.DEFAULT_ROW_HEIGHT));
                    newRowHeight = (int) (ChainsawConstants.DEFAULT_ROW_HEIGHT + delta);
                }

                if (colIndex == ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME) {
                    loggingEventWrapper.setMarkerHeight(newRowHeight);
                    if (newRowHeight != currentMarkerHeight && newRowHeight >= loggingEventWrapper.getMsgHeight()) {
                        setHeight = true;
                    }
                }

                if (colIndex == ChainsawColumns.INDEX_MESSAGE_COL_NAME) {
                    loggingEventWrapper.setMsgHeight(newRowHeight);
                    if (newRowHeight != currentMsgHeight && newRowHeight >= loggingEventWrapper.getMarkerHeight()) {
                        setHeight = true;
                    }
                }
                if (setHeight) {
                    table.setRowHeight(row, newRowHeight);
                }

                component = textPaneContainer;
                break;
            case ChainsawColumns.INDEX_LEVEL_COL_NAME:
                if (levelUseIcons) {
                    levelTextPane.setText("");
                    levelTextPane.insertIcon(iconMap.get(value.toString()));
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
                Set propertySet = loggingEventWrapper.getPropertyKeySet();
                String headerName = tableColumn.getHeaderValue().toString().toLowerCase();
                String thisProp = null;
                //find the property in the property set...case-sensitive
                for (Object aPropertySet : propertySet) {
                    String entry = aPropertySet.toString();
                    if (entry.equalsIgnoreCase(headerName)) {
                        thisProp = entry;
                        break;
                    }
                }
                if (thisProp != null) {
                    String propKey = LoggingEventFieldResolver.PROP_FIELD + thisProp.toUpperCase();
                    Set propKeyMatches = (Set) matches.get(propKey);
                    singleLineTextPane.setText(loggingEventWrapper.getLoggingEvent().getProperty(thisProp));
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
        AbstractConfiguration configuration = settingsManager.getGlobalConfiguration();
        //use logger colors in table instead of event colors if event passes logger rule
        if (loggerRule != null && loggerRule.evaluate(loggingEventWrapper.getLoggingEvent(), null)) {
            background = configuration.get(Color.class, "searchBackgroundColor", ChainsawConstants.FIND_LOGGER_BACKGROUND);
            foreground = configuration.get(Color.class, "searchForegroundColor", ChainsawConstants.FIND_LOGGER_FOREGROUND);
        } else {
            if (colorizeSearch && !configuration.getBoolean("bypassSearchColors", false)) {
                background = loggingEventWrapper.isSearchMatch() ? configuration.get(Color.class, "searchBackgroundColor", ChainsawConstants.FIND_LOGGER_BACKGROUND) : loggingEventWrapper.getBackground();
                foreground = loggingEventWrapper.isSearchMatch() ? configuration.get(Color.class, "searchForegroundColor", ChainsawConstants.FIND_LOGGER_FOREGROUND) : loggingEventWrapper.getForeground();
            } else {
                background = loggingEventWrapper.getBackground();
                foreground = loggingEventWrapper.getForeground();
            }
        }

        /**
         * Colourize background based on row striping if the event still has default foreground and background color
         */
        if (background.equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND) && foreground.equals(ChainsawConstants.COLOR_DEFAULT_FOREGROUND) && (row % 2) != 0) {
            background = configuration.get(Color.class, "alternatingColorBackground", ChainsawConstants.COLOR_ODD_ROW_BACKGROUND);
            foreground = configuration.get(Color.class, "alternatingColorForeground", ChainsawConstants.COLOR_ODD_ROW_FOREGROUND);
        }

        component.setBackground(background);
        component.setForeground(foreground);

        //update the background & foreground of the jtextpane using styles
        if (multiLineTextPane != null) {
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

        Border innerBorder = isSelected ? LEFT_BORDER : LEFT_EMPTY_BORDER;
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
        Border innerBorder = isSelected ? RIGHT_BORDER : RIGHT_EMPTY_BORDER;
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
        Border innerBorder = isSelected ? MIDDLE_BORDER : MIDDLE_EMPTY_BORDER;
        if (delta == 0 || !wrap || !logPanelPreferenceModel.isShowMillisDeltaAsGap()) {
            return innerBorder;
        } else {
            Border outerBorder = BorderFactory.createMatteBorder((int) Math.max(borderWidth, delta), 0, 0, 0, applicationPreferenceModel.getDeltaColor());
            return BorderFactory.createCompoundBorder(outerBorder, innerBorder);
        }
    }

    private void updateColors(JTextPane textPane, Color background, Color foreground) {
        StyledDocument styledDocument = textPane.getStyledDocument();
        MutableAttributeSet attributes = textPane.getInputAttributes();
        StyleConstants.setForeground(attributes, foreground);
        styledDocument.setCharacterAttributes(0, styledDocument.getLength() + 1, attributes, false);
        textPane.setBackground(background);
    }

    /**
     * Changes the Date Formatting object to be used for rendering dates.
     *
     * @param formatter
     */
    public void setDateFormatter(DateFormat formatter) {
        this.dateFormatInUse = formatter;
        if (dateFormatInUse != null && dateFormatTZ != null && !("".equals(dateFormatTZ))) {
            dateFormatInUse.setTimeZone(TimeZone.getTimeZone(dateFormatTZ));
        } else {
            dateFormatInUse.setTimeZone(TimeZone.getDefault());
        }
    }

    /**
     * Changes the Logger precision.
     *
     * @param loggerPrecision
     */
    public void setLoggerPrecision(int loggerPrecision) {
        this.loggerPrecision = loggerPrecision;
    }

    /**
     * Format date field
     *
     * @param field object
     * @return formatted object
     */
    private Object formatField(Object field, LoggingEventWrapper loggingEventWrapper) {
        if (!(field instanceof ZonedDateTime)) {
            return (field == null ? "" : field);
        }

        //handle date field
        if (useRelativeTimesToFixedTime) {
            ZonedDateTime dt = (ZonedDateTime)field;
            return "" + ChronoUnit.MILLIS.between(dt, relativeTimestampBase);
        }
        if (useRelativeTimesToPrevious) {
            return loggingEventWrapper.getLoggingEvent().getProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE);
        }

        return ((ZonedDateTime)field).format(DateTimeFormatter.ISO_DATE);
    }

    /**
     * Sets the property which determines whether to wrap the message
     *
     * @param wrapMsg
     */
    public void setWrapMessage(boolean wrapMsg) {
        this.wrap = wrapMsg;
    }

    /**
     * Sets the property which determines whether to use Icons or text
     * for the Level column
     *
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

    public void setUseRelativeTimes(ZonedDateTime timeStamp) {
        useRelativeTimesToFixedTime = true;
        useRelativeTimesToPrevious = false;
        if( timeStamp != null ){
            relativeTimestampBase = timeStamp;
        }else{
            relativeTimestampBase = ZonedDateTime.now();
        }
    }

    public void setUseRelativeTimesToPreviousRow() {
        useRelativeTimesToFixedTime = false;
        useRelativeTimesToPrevious = true;
    }

    public void setUseNormalTimes() {
        useRelativeTimesToFixedTime = false;
        useRelativeTimesToPrevious = false;
    }

  /*
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
    */

    private void setHighlightAttributesInternal(Object matchSet, StyledDocument styledDocument) {
        if (!highlightSearchMatchText) {
            return;
        }
        setHighlightAttributes(matchSet, styledDocument);
    }

    public void setHighlightAttributes(Object matchSet, StyledDocument styledDocument) {
        if (matchSet instanceof Set) {
            Set thisSet = (Set) matchSet;
            for (Object aThisSet : thisSet) {
                String thisEntry = aThisSet.toString();
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
        } catch (BadLocationException e) {
            //ignore
        }
    }

    public void setHighlightSearchMatchText(boolean highlightSearchMatchText) {
        this.highlightSearchMatchText = highlightSearchMatchText;
    }

    private class OneLineEditorKit extends StyledEditorKit {
        private ViewFactory viewFactoryImpl = new ViewFactoryImpl();

        public ViewFactory getViewFactory() {
            return viewFactoryImpl;
        }
    }

    private class ViewFactoryImpl implements ViewFactory {
        public View create(Element elem) {
            String elementName = elem.getName();
            if (elementName != null) {
                switch (elementName) {
                    case AbstractDocument.ParagraphElementName:
                        return new OneLineParagraphView(elem);
                    case AbstractDocument.ContentElementName:
                        return new LabelView(elem);
                    case AbstractDocument.SectionElementName:
                        return new BoxView(elem, View.Y_AXIS);
                    case StyleConstants.ComponentElementName:
                        return new ComponentView(elem);
                    case StyleConstants.IconElementName:
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
