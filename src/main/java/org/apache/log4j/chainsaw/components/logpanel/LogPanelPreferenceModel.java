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

/*
 */
package org.apache.log4j.chainsaw.components.logpanel;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.chainsaw.layout.DefaultLayoutFactory;
import org.apache.log4j.helpers.Constants;

import javax.swing.table.TableColumn;
import java.util.*;

/**
 * Used to encapsulate all the preferences for a given LogPanel
 *
 * @author Paul Smith
 */
public class LogPanelPreferenceModel {
    public static final String ISO8601 = "ISO8601";
    public static final Collection<String> DATE_FORMATS = Collections.singletonList(ISO8601);
    public static final String PAUSED = "logpanel.paused";
    public static final String CLEAR_TABLE_EXPRESSION = "logpanel.clearTableExpression";
    public static final String ALWAYS_DISPLAY_EXPRESSION = "logpanel.alwaysDisplayExpression";
    public static final String HIDDEN_EXPRESSION = "logpanel.hiddenExpression";
    public static final String DATE_FORMAT_TIME_ZONE = "logpanel.dateFormatTimeZone";
    public static final String HIDDEN_LOGGERS = "logpanel.hiddenLoggers";
    public static final String LOG_TREE_PANEL_VISIBLE = "logpanel.logTreePanelVisible";
    public static final String TOOL_TIPS_VISIBLE = "logpanel.toolTipsVisible";
    public static final String THUMBNAIL_BAR_TOOL_TIPS_VISIBLE = "logpanel.thumbnailBarToolTipsVisible";
    public static final String SHOW_MILLIS_DELTA_AS_GAP = "logpanel.showMillisDeltaAsGap";
    public static final String SCROLL_TO_BOTTOM = "logpanel.scrollToBottom";
    public static final String DETAIL_PANE_VISIBLE = "logpanel.detailPaneVisible";
    public static final String LOGGER_PRECISION = "logpanel.loggerPrecision";
    public static final String SEARCH_RESULTS_VISIBLE = "logpanel.searchResultsVisible";
    public static final String WRAP_MSG = "logpanel.wrapMsg";
    public static final String VISIBLE_ORDERED_COLUMN_NAMES = "logpanel.visibleOrderedColumnNames";
    public static final String LEVEL_ICONS_DISPLAYED = "logpanel.levelIconsDisplayed";
    public static final String HIGHLIGHT_SEARCH_MATCH_TEXT = "logpanel.highlightSearchMatchText";
    private static final String LOWER_PANEL_DIVIDER_LOCATION = "logpanel.lowerPanelDividerLocation";
    private static final String LOG_TREE_DIVIDER_LOCATION = "logpanel.logTreeDividerLocation";
    private static final String CONVERSION_PATTERN = "logpanel.conversionPattern";
    public static final String DATE_FORMAT_PATTERN = "dateFormatPattern";
    AbstractConfiguration tabConfig;

    private final String defaultDateFormatPattern = Constants.SIMPLE_TIME_PATTERN;
    private Map<String, Integer> allColumnsAndWidths = new HashMap<>();
    private List<String> visibleOrderedColumnNames = new ArrayList<>();
    private final boolean pausedDefault = false;
    private final boolean logTreePanelVisibleDefault = true;
    private final boolean toolTipsVisibleDefault = false;
    private final boolean thumbnailBarToolTipsVisibleDefault = false;
    private final boolean showMillisDeltaAsGapDefault = false;
    private final boolean scrollToBottomDefault = true;
    private final int loggerPrecisionDefault = 0;
    private final boolean searchResultsVisibleDefault = true;
    private final boolean highlightSearchMatchTextDefault = true;
    private final boolean wrapMsgDefault = true;
    private final boolean levelIconsDisplayedDefault = false;

    private static final int lowerPanelDividerLocationDefault = 700;
    private static final int logTreeDividerLocationDefault = 230;

    private static final String conversionPatternDefault = DefaultLayoutFactory.getDefaultPatternLayout();

    private static final List<String> defaultOrderedColumnNames = List.of(ChainsawConstants.ID_COL_NAME,
        ChainsawConstants.TIMESTAMP_COL_NAME, ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE.toUpperCase(),
        ChainsawConstants.LEVEL_COL_NAME, ChainsawConstants.LOGGER_COL_NAME, ChainsawConstants.MESSAGE_COL_NAME);

    private static final Map<String, Integer> defaultOrderedColumnNamesAndWidths =
        Map.of(ChainsawConstants.ID_COL_NAME, 75, ChainsawConstants.TIMESTAMP_COL_NAME, 100,
            ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE.toUpperCase(), 120, ChainsawConstants.LEVEL_COL_NAME, 75,
            ChainsawConstants.LOGGER_COL_NAME, 125, ChainsawConstants.MESSAGE_COL_NAME, 650);

    public LogPanelPreferenceModel(AbstractConfiguration tabConfig) {
        this.tabConfig = tabConfig;
    }

    public void addEventListener(EventListener<ConfigurationEvent> listener) {
        tabConfig.addEventListener(ConfigurationEvent.SET_PROPERTY, listener);
    }

    public void setCyclic(boolean cyclic) {
        tabConfig.setProperty("cyclic", cyclic);
    }

    public boolean isCyclic() {
        return tabConfig.getBoolean("cyclic", false);
    }

    /**
     * Returns an <b>unmodifiable</b> list of the visible columns.
     * <p>
     * The reason it is unmodifiable is to enforce the requirement that
     * the List is actually unique columns.  IT _could_ be a set,
     * but we need to maintain the order of insertion.
     *
     */
    public List<String> getVisibleOrderedColumnNames() {
        return defaultOrderedColumnNames;
    }

    public void setVisibleOrderedColumnNames(List<String> visibleOrderedColumNames) {
        this.visibleOrderedColumnNames = visibleOrderedColumNames;
    }

    public boolean addColumn(TableColumn column) {
        if (allColumnsAndWidths.containsKey(column.getHeaderValue().toString())) {
            return false;
        }

        allColumnsAndWidths.put(column.getHeaderValue().toString(), column.getWidth());
        return true;
    }

    /**
     * Returns the Date Pattern string for the alternate date formatter.
     *
     * @return date pattern
     */
    public final String getDateFormatPattern() {
        return tabConfig.getString(DATE_FORMAT_PATTERN, defaultDateFormatPattern);
    }

    public final void setDefaultDatePatternFormat() {
        tabConfig.setProperty(DATE_FORMAT_PATTERN, defaultDateFormatPattern);
    }

    public final void setDateFormatPattern(String dateFormatPattern) {
        tabConfig.setProperty(DATE_FORMAT_PATTERN, dateFormatPattern);
    }

    /**
     * Applies all the properties of another model to this model
     *
     * @param model the model to copy
     *              all the properties from
     */
    public void apply(LogPanelPreferenceModel model) {
        setCyclic(model.isCyclic());
        setLoggerPrecision(model.getLoggerPrecision());
        setDateFormatPattern(model.getDateFormatPattern());
        setLevelIconsDisplayed(model.isLevelIconsDisplayed());
        setWrapMessage(model.isWrapMessage());
        setHighlightSearchMatchText(model.isHighlightSearchMatchText());
        setDateFormatTimeZone(model.getDateFormatTimeZone());
        setToolTipsVisible(model.isToolTipsVisible());
        setThumbnailBarToolTipsVisible((model.isThumbnailBarToolTipsVisible()));
        setScrollToBottom(model.isScrollToBottom());
        setDetailPaneVisible(model.isDetailPaneVisible());
        setLogTreePanelVisible(model.isLogTreePanelVisible());
        setVisibleOrderedColumnNames(model.getVisibleOrderedColumnNames());
        setSearchResultsVisible(model.isSearchResultsVisible());
        setVisibleOrderedColumnNames(model.getVisibleOrderedColumnNames());
        setHiddenLoggers(model.getHiddenLoggers());
        setHiddenExpression(model.getHiddenExpression());
        setAlwaysDisplayExpression(model.getAlwaysDisplayExpression());
        setShowMillisDeltaAsGap(model.isShowMillisDeltaAsGap());
        setClearTableExpression(model.getClearTableExpression());
    }

    /**
     * Returns true if this the fast ISO8601DateFormat object
     * should be used instead of SimpleDateFormat
     *
     * @return use ISO8601 format flag
     */
    public boolean isUseISO8601Format() {
        return getDateFormatPattern().equals(ISO8601);
    }

    /**
     * @return level icons flag
     */
    public boolean isLevelIconsDisplayed() {
        return tabConfig.getBoolean(LEVEL_ICONS_DISPLAYED, levelIconsDisplayedDefault);
    }

    public boolean isWrapMessage() {
        return tabConfig.getBoolean(WRAP_MSG, wrapMsgDefault);
    }

    public boolean isHighlightSearchMatchText() {
        return tabConfig.getBoolean(HIGHLIGHT_SEARCH_MATCH_TEXT, highlightSearchMatchTextDefault);
    }

    public void setLevelIconsDisplayed(boolean isLevelIconsDisplayed) {
        tabConfig.setProperty(LEVEL_ICONS_DISPLAYED, isLevelIconsDisplayed);
    }

    public void setSearchResultsVisible(boolean isSearchResultsVisible) {
        tabConfig.setProperty(SEARCH_RESULTS_VISIBLE, isSearchResultsVisible);
    }

    public boolean isSearchResultsVisible() {
        return tabConfig.getBoolean(SEARCH_RESULTS_VISIBLE, searchResultsVisibleDefault);
    }

    public void setWrapMessage(boolean isWrapMsg) {
        tabConfig.setProperty(WRAP_MSG, isWrapMsg);
    }

    public void setHighlightSearchMatchText(boolean highlightSearchMatchText) {
        tabConfig.setProperty(HIGHLIGHT_SEARCH_MATCH_TEXT, highlightSearchMatchText);
    }

    /**
     * @param loggerPrecision - an integer representing the number of packages to display,
     *                        or zero, representing 'display all packages'
     */
    public void setLoggerPrecision(int loggerPrecision) {
        tabConfig.setProperty(LOGGER_PRECISION, loggerPrecision);
    }

    /**
     * Returns the Logger precision.
     *
     * @return logger precision
     */
    public final int getLoggerPrecision() {
        return tabConfig.getInt(LOGGER_PRECISION, loggerPrecisionDefault);
    }

    /**
     * Returns true if the named column should be made visible otherwise
     * false.
     *
     * @param column
     * @return column visible flag
     */
    public boolean isColumnVisible(TableColumn column) {
        return visibleOrderedColumnNames.contains(column.getHeaderValue().toString());
    }

    public void setColumnVisible(String columnName, boolean isVisible) {
        if (isVisible) {
            if (!visibleOrderedColumnNames.contains(columnName)) {
                visibleOrderedColumnNames.add(columnName);
            }
        } else {
            visibleOrderedColumnNames.remove(columnName);
        }
        tabConfig.setProperty(VISIBLE_ORDERED_COLUMN_NAMES, visibleOrderedColumnNames);
    }

    /**
     * Toggles the state between visible, non-visible for a particular Column name
     *
     * @param column
     */
    public void toggleColumn(TableColumn column) {
        setColumnVisible(column.getHeaderValue().toString(), !isColumnVisible(column));
    }

    /**
     * @return detail pane visible flag
     */
    public final boolean isDetailPaneVisible() {
        return tabConfig.getBoolean(DETAIL_PANE_VISIBLE, true);
    }

    public final void setDetailPaneVisible(boolean detailPaneVisible) {
        tabConfig.setProperty(DETAIL_PANE_VISIBLE, detailPaneVisible);
    }

    /**
     * @return scroll to bottom flag
     */
    public final boolean isScrollToBottom() {
        return tabConfig.getBoolean(SCROLL_TO_BOTTOM, scrollToBottomDefault);
    }

    public final boolean isShowMillisDeltaAsGap() {
        return tabConfig.getBoolean(SHOW_MILLIS_DELTA_AS_GAP, showMillisDeltaAsGapDefault);
    }

    public final void setScrollToBottom(boolean scrollToBottom) {
        tabConfig.setProperty(SCROLL_TO_BOTTOM, scrollToBottom);
    }

    public final void setShowMillisDeltaAsGap(boolean showMillisDeltaAsGap) {
        tabConfig.setProperty(SHOW_MILLIS_DELTA_AS_GAP, showMillisDeltaAsGap);
    }

    public final void setThumbnailBarToolTipsVisible(boolean thumbnailBarToolTipsVisible) {
        tabConfig.setProperty(THUMBNAIL_BAR_TOOL_TIPS_VISIBLE, thumbnailBarToolTipsVisible);
    }

    public final boolean isThumbnailBarToolTipsVisible() {
        return tabConfig.getBoolean(THUMBNAIL_BAR_TOOL_TIPS_VISIBLE, thumbnailBarToolTipsVisibleDefault);
    }

    /**
     * @return tool tips enabled flag
     */
    public final boolean isToolTipsVisible() {
        return tabConfig.getBoolean(TOOL_TIPS_VISIBLE, toolTipsVisibleDefault);
    }

    public final void setToolTipsVisible(boolean toolTipsVisible) {
        tabConfig.setProperty(TOOL_TIPS_VISIBLE, toolTipsVisible);
    }

    /**
     * @return log tree panel visible flag
     */
    public final boolean isLogTreePanelVisible() {
        return tabConfig.getBoolean(LOG_TREE_PANEL_VISIBLE, logTreePanelVisibleDefault);
    }

    public final void setLogTreePanelVisible(boolean logTreePanelVisible) {
        tabConfig.setProperty(LOG_TREE_PANEL_VISIBLE, logTreePanelVisible);
    }

    /**
     * @return custom date format flag
     */
    public boolean isCustomDateFormat() {
        return !DATE_FORMATS.contains(getDateFormatPattern()) && !isUseISO8601Format();
    }

    public void setHiddenLoggers(Collection<String> hiddenLoggers) {
        tabConfig.setProperty(HIDDEN_LOGGERS, hiddenLoggers);
    }

    public Collection<String> getHiddenLoggers() {
        return tabConfig.getList(String.class, HIDDEN_LOGGERS, Collections.emptyList());
    }

    public String getDateFormatTimeZone() {
        return tabConfig.getString(DATE_FORMAT_TIME_ZONE, "");
    }

    public void setDateFormatTimeZone(String dateFormatTimeZone) {
        tabConfig.setProperty(DATE_FORMAT_TIME_ZONE, dateFormatTimeZone);
    }

    public void setHiddenExpression(String hiddenExpression) {
        tabConfig.setProperty(HIDDEN_EXPRESSION, hiddenExpression);
    }

    public String getHiddenExpression() {
        return tabConfig.getString(HIDDEN_EXPRESSION, "");
    }

    public void setAlwaysDisplayExpression(String alwaysDisplayExpression) {
        tabConfig.setProperty(ALWAYS_DISPLAY_EXPRESSION, alwaysDisplayExpression);
    }

    public String getAlwaysDisplayExpression() {
        return tabConfig.getString(ALWAYS_DISPLAY_EXPRESSION, "");
    }

    public void setClearTableExpression(String clearTableExpression) {
        tabConfig.setProperty(CLEAR_TABLE_EXPRESSION, clearTableExpression);
    }

    public String getClearTableExpression() {
        return tabConfig.getString(CLEAR_TABLE_EXPRESSION, "");
    }

    public void setPaused(boolean paused) {
        tabConfig.setProperty(PAUSED, paused);
    }

    public boolean isPaused() {
        return tabConfig.getBoolean(PAUSED, pausedDefault);
    }

    public int getLowerPanelDividerLocation() {
        return tabConfig.getInt(LOWER_PANEL_DIVIDER_LOCATION, lowerPanelDividerLocationDefault);
    }

    public void setLowerPanelDividerLocation(int lowerPanelDividerLocation) {
        tabConfig.setProperty(LOWER_PANEL_DIVIDER_LOCATION, lowerPanelDividerLocation);
    }

    public int getLogTreeDividerLocation() {
        return tabConfig.getInt(LOG_TREE_DIVIDER_LOCATION, logTreeDividerLocationDefault);
    }

    public void setLogTreeDividerLocation(int logTreeDividerLocation) {
        tabConfig.setProperty(LOG_TREE_DIVIDER_LOCATION, logTreeDividerLocation);
    }

    public String getConversionPattern() {
        return tabConfig.getString(CONVERSION_PATTERN, conversionPatternDefault);
    }

    public void setConversionPattern(String conversionPattern) {
        tabConfig.setProperty(CONVERSION_PATTERN, conversionPatternDefault);
    }

    public Map<String, Integer> getAllColumnNamesAndWidths() {
        return defaultOrderedColumnNamesAndWidths;
    }
}
