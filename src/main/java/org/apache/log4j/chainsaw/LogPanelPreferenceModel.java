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
package org.apache.log4j.chainsaw;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.table.TableColumn;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.chainsaw.prefs.SettingsManager;
import org.apache.log4j.helpers.Constants;


/**
 *  Used to encapsulate all the preferences for a given LogPanel
 * @author Paul Smith
 */
public class LogPanelPreferenceModel implements Serializable{
  public static final String ISO8601 = "ISO8601";
  public static final Collection DATE_FORMATS;
  private static final Logger logger = LogManager.getLogger(LogPanelPreferenceModel.class);

 private static final long serialVersionUID = 7526472295622776147L;
  static {
    Collection list = new ArrayList();

    Properties properties = SettingsManager.getInstance().getDefaultSettings();

    for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();

      if (entry.getKey().toString().startsWith("DateFormat")) {
        list.add(entry.getValue());
      }
    }

    DATE_FORMATS = Collections.unmodifiableCollection(list);
  }

  private transient final PropertyChangeSupport propertySupport =
    new PropertyChangeSupport(this);
  private String dateFormatPattern = Constants.SIMPLE_TIME_PATTERN;
  private boolean levelIcons;
  private List allColumns = new ArrayList();
  private List visibleColumns = new ArrayList();
  private List visibleColumnOrder = new ArrayList();
  private boolean detailPaneVisible;
  private boolean toolTips;
  //default thumbnail bar tooltips to true
  private boolean thumbnailBarToolTips = true;
  private boolean scrollToBottom;
  private boolean logTreePanelVisible;
  private String loggerPrecision = "";

  private Collection hiddenLoggers = new HashSet();
  private String timeZone;
  private boolean wrapMsg;
  private boolean highlightSearchMatchText;
  private String hiddenExpression;
  private String clearTableExpression;

    /**
   * Returns an <b>unmodifiable</b> list of the columns.
   * 
   * The reason it is unmodifiable is to enforce the requirement that
   * the List is actually unique columns.  IT _could_ be a set,
   * but we need to maintain the order of insertion.
   * 
   * @return
   */
  public List getColumns() {
      return Collections.unmodifiableList(allColumns);
  }
  
  /**
   * Returns an <b>unmodifiable</b> list of the visible columns.
   * 
   * The reason it is unmodifiable is to enforce the requirement that
   * the List is actually unique columns.  IT _could_ be a set,
   * but we need to maintain the order of insertion.
   * 
   * @return
   */
  public List getVisibleColumns() {
      return Collections.unmodifiableList(visibleColumns);
  }
  
  public void clearColumns(){
      Object oldValue = this.allColumns;
      allColumns = new ArrayList();
      propertySupport.firePropertyChange("columns", oldValue, allColumns);
  }
  
  private TableColumn findColumnByHeader(List list, String header) {
	  for (Iterator iter = list.iterator();iter.hasNext();) {
		  TableColumn c = (TableColumn)iter.next();
		  if (c.getHeaderValue().equals(header)) {
			  return c;
		  }
	  }
	  return null;
  }
  
  public void setVisibleColumnOrder(List visibleColumnOrder) {
	  this.visibleColumnOrder = visibleColumnOrder;
  }
  
  public List getVisibleColumnOrder() {
	  return visibleColumnOrder;
  }
  
  public boolean addColumn(TableColumn column){
	  if (findColumnByHeader(allColumns, column.getHeaderValue().toString()) != null) {
		  return false;
	  }

      Object oldValue = allColumns;
      allColumns = new ArrayList(allColumns);
      allColumns.add(column);
      
      propertySupport.firePropertyChange("columns", oldValue, allColumns);
      return true;
  }
  
  private void setColumns(List columns) {
      Object oldValue = allColumns;
      allColumns = new ArrayList(columns);
      propertySupport.firePropertyChange("columns", oldValue, columns);
  }

/**
   * Returns the Date Pattern string for the alternate date formatter.
   * @return date pattern
   */
  public final String getDateFormatPattern() {
    return dateFormatPattern;
  }

  public final void setDefaultDatePatternFormat() {
	    String oldVal = this.dateFormatPattern;
	    this.dateFormatPattern = Constants.SIMPLE_TIME_PATTERN;
	    propertySupport.firePropertyChange(
	    	      "dateFormatPattern", oldVal, this.dateFormatPattern);
  }
  /**
   * @param dateFormatPattern
   */
  public final void setDateFormatPattern(String dateFormatPattern) {
    String oldVal = this.dateFormatPattern;
    this.dateFormatPattern = dateFormatPattern;
    propertySupport.firePropertyChange(
      "dateFormatPattern", oldVal, this.dateFormatPattern);
  }

  /**
   * @param listener
   */
  public synchronized void addPropertyChangeListener(
    PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  /**
   * @param propertyName
   * @param listener
   */
  public synchronized void addPropertyChangeListener(
    String propertyName, PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(propertyName, listener);
  }

  /**
   * @param listener
   */
  public synchronized void removePropertyChangeListener(
    PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  /**
   * @param propertyName
   * @param listener
   */
  public synchronized void removePropertyChangeListener(
    String propertyName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propertyName, listener);
  }

  /**
   * Applies all the properties of another model to this model
   *
   * @param model the model to copy
   * all the properties from
   */
  public void apply(LogPanelPreferenceModel model) {
    setLoggerPrecision(model.getLoggerPrecision());
    setDateFormatPattern(model.getDateFormatPattern());
    setLevelIcons(model.isLevelIcons());
    setWrapMessage(model.isWrapMessage());
    setHighlightSearchMatchText(model.isHighlightSearchMatchText());
    setTimeZone(model.getTimeZone());
    setToolTips(model.isToolTips());
    setThumbnailBarToolTips((model.isThumbnailBarToolTips()));
    setScrollToBottom(model.isScrollToBottom());
    setDetailPaneVisible(model.isDetailPaneVisible());
    setLogTreePanelVisible(model.isLogTreePanelVisible());
    setVisibleColumnOrder(model.getVisibleColumnOrder());

    // we have to copy the list, because getColumns() is unmodifiable
    setColumns(model.getColumns());
    
    setVisibleColumns(model.getVisibleColumns());
    setHiddenLoggers(model.getHiddenLoggers());
    setHiddenExpression(model.getHiddenExpression());
    setClearTableExpression(model.getClearTableExpression());
  }

  /**
   * Returns true if this the fast ISO8601DateFormat object
   * should be used instead of SimpleDateFormat
   * @return use ISO8601 format flag
   */
  public boolean isUseISO8601Format() {
    return getDateFormatPattern().equals(ISO8601);
  }

  /**
   * @return level icons flag
   */
  public boolean isLevelIcons() {
    return levelIcons;
  }

  public boolean isWrapMessage() {
    return wrapMsg;
  }

  public boolean isHighlightSearchMatchText() {
    return highlightSearchMatchText;
  }

  /**
   * @param levelIcons
   */
  public void setLevelIcons(boolean levelIcons) {
    this.levelIcons = levelIcons;
    propertySupport.firePropertyChange("levelIcons", !levelIcons, levelIcons);
  }

  /**
   * @param wrapMsg
   */
  public void setWrapMessage(boolean wrapMsg) {
    this.wrapMsg = wrapMsg;
    propertySupport.firePropertyChange("wrapMessage", !wrapMsg, wrapMsg);
  }

    /**
     * @param highlightSearchMatchText
     */
    public void setHighlightSearchMatchText(boolean highlightSearchMatchText) {
      this.highlightSearchMatchText = highlightSearchMatchText;
      propertySupport.firePropertyChange("highlightSearchMatchText", !highlightSearchMatchText, highlightSearchMatchText);
    }

  /**
   * @param loggerPrecision - an integer representing the number of packages to display, 
   * or an empty string representing 'display all packages' 
   */
  public void setLoggerPrecision(String loggerPrecision) {
    String oldVal = this.loggerPrecision;
    this.loggerPrecision = loggerPrecision;
    propertySupport.firePropertyChange("loggerPrecision", oldVal, this.loggerPrecision);      
  }
  
  /**
   * Returns the Logger precision.
   * @return logger precision
   */
  public final String getLoggerPrecision() {
    return loggerPrecision;
  }

  /**
   * Returns true if the named column should be made visible otherwise
   * false.
   * @param column
   * @return column visible flag
   */
  public boolean isColumnVisible(TableColumn column) {
	  return (findColumnByHeader(visibleColumns, column.getHeaderValue().toString()) != null);
  }

  private void setVisibleColumns(List visibleColumns) {
      Object oldValue = new ArrayList();
      this.visibleColumns = new ArrayList(visibleColumns);
      
      propertySupport.firePropertyChange("visibleColumns", oldValue, this.visibleColumns);
  }

  public void setColumnVisible(String columnName, boolean isVisible) {
    boolean wasVisible = findColumnByHeader(visibleColumns, columnName) != null;

      //because we're a list and not a set, ensure we keep at most
    //one entry for a tablecolumn
    Object col = findColumnByHeader(allColumns, columnName);
    if (isVisible && !wasVisible) {
		visibleColumns.add(col);
		visibleColumnOrder.add(col);
	    propertySupport.firePropertyChange("visibleColumns", new Boolean(isVisible), new Boolean(wasVisible));
	}
    if (!isVisible && wasVisible) {
		visibleColumns.remove(col);
		visibleColumnOrder.remove(col);
	    propertySupport.firePropertyChange("visibleColumns", new Boolean(isVisible), new Boolean(wasVisible));
	}
  }
  
  /**
   * Toggles the state between visible, non-visible for a particular Column name
   * @param column
   */
  public void toggleColumn(TableColumn column) {
    setColumnVisible(column.getHeaderValue().toString(), !isColumnVisible(column));
  }

  /**
   * @return detail pane visible flag
   */
  public final boolean isDetailPaneVisible() {
    return detailPaneVisible;
  }

  /**
   * @param detailPaneVisible
   */
  public final void setDetailPaneVisible(boolean detailPaneVisible) {
    boolean oldValue = this.detailPaneVisible;
    this.detailPaneVisible = detailPaneVisible;
    propertySupport.firePropertyChange(
      "detailPaneVisible", oldValue, this.detailPaneVisible);
  }

  /**
   * @return scroll to bottom flag
   */
  public final boolean isScrollToBottom() {
    return scrollToBottom;
  }

  /**
   * @param scrollToBottom
   */
  public final void setScrollToBottom(boolean scrollToBottom) {
    boolean oldValue = this.scrollToBottom;
    this.scrollToBottom = scrollToBottom;
    propertySupport.firePropertyChange(
      "scrollToBottom", oldValue, this.scrollToBottom);
  }

  public final void setThumbnailBarToolTips(boolean thumbnailBarToolTips) {
      boolean oldValue = this.thumbnailBarToolTips;
      this.thumbnailBarToolTips = thumbnailBarToolTips;
      propertySupport.firePropertyChange("thumbnailBarToolTips", oldValue, this.thumbnailBarToolTips);
  }

  public final boolean isThumbnailBarToolTips() {
      return thumbnailBarToolTips;
  }

  /**
   * @return tool tips enabled flag
   */
  public final boolean isToolTips() {
    return toolTips;
  }

  /**
   * @param toolTips
   */
  public final void setToolTips(boolean toolTips) {
    boolean oldValue = this.toolTips;
    this.toolTips = toolTips;
    propertySupport.firePropertyChange("toolTips", oldValue, this.toolTips);
  }

  /**
   * @return log tree panel visible flag
   */
  public final boolean isLogTreePanelVisible() {
    return logTreePanelVisible;
  }

  /**
   * @param logTreePanelVisible
   */
  public final void setLogTreePanelVisible(boolean logTreePanelVisible) {
    boolean oldValue = this.logTreePanelVisible;
    this.logTreePanelVisible = logTreePanelVisible;
    propertySupport.firePropertyChange(
      "logTreePanelVisible", oldValue, this.logTreePanelVisible);
  }

  /**
   * @return custom date format flag
   */
  public boolean isCustomDateFormat()
  {
    return !DATE_FORMATS.contains(getDateFormatPattern()) && !isUseISO8601Format();
  }

  public void setHiddenLoggers(Collection hiddenSet) {
      Object oldValue = this.hiddenLoggers;
      this.hiddenLoggers = hiddenSet;
      propertySupport.firePropertyChange("hiddenLoggers", oldValue, this.hiddenLoggers);
  }

  public Collection getHiddenLoggers() {
    return hiddenLoggers;
  }

  public String getTimeZone() {
    return timeZone;
  }
  
  public void setTimeZone(String timeZone) {
      Object oldValue = this.timeZone;
      this.timeZone = timeZone;
      propertySupport.firePropertyChange("dateFormatTimeZone", oldValue, this.timeZone);
  }

  public void setHiddenExpression(String hiddenExpression) {
    Object oldValue = this.hiddenExpression;
    this.hiddenExpression = hiddenExpression;
    propertySupport.firePropertyChange("hiddenExpression", oldValue, this.hiddenExpression);
  }

  public String getHiddenExpression() {
    return hiddenExpression;
  }

  public void setClearTableExpression(String clearTableExpression) {
    Object oldValue = this.clearTableExpression;
    this.clearTableExpression = clearTableExpression;
    //no propertychange if both null
    if (oldValue == null && this.clearTableExpression == null) {
        return;
    }
    propertySupport.firePropertyChange("clearTableExpression", oldValue, this.clearTableExpression);
  }

  public String getClearTableExpression() {
    return clearTableExpression;
  }
}
