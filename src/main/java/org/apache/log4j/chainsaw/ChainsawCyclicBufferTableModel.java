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
package org.apache.log4j.chainsaw;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.table.AbstractTableModel;
import org.apache.log4j.chainsaw.color.RuleColorizer;
import org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameListener;
import org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameModel;
import org.apache.log4j.chainsaw.components.loggernamepanel.LoggerNameModelSupport;
import org.apache.log4j.chainsaw.helper.SwingHelper;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.Rule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A CyclicBuffer implementation of the EventContainer.
 * <p>
 * NOTE:  This implementation prevents duplicate rows from being added to the model.
 * <p>
 * Ignoring duplicates was added to support receivers which may attempt to deliver the same
 * event more than once but can be safely ignored (for example, the database receiver
 * when set to retrieve in a loop).
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 * @author Stephen Pain
 */
public class ChainsawCyclicBufferTableModel extends AbstractTableModel
        implements EventContainer, PropertyChangeListener {

    // cyclic field used internally in this class, but not exposed via the eventcontainer
    private boolean cyclic = true;
    private final int cyclicBufferSize;
    // original list of LoggingEventWrapper instances
    private List<LoggingEventWrapper> unfilteredList;
    // filtered list of LoggingEventWrapper instances
    private List<LoggingEventWrapper> filteredList;
    private boolean currentSortAscending;
    private int currentSortColumn;
    private final EventListenerList eventListenerList = new EventListenerList();
    private final List<String> columnNames = new ArrayList<>(ChainsawColumns.getColumnsNames());
    private boolean sortEnabled = false;
    private boolean reachedCapacity = false;
    private final Logger logger = LogManager.getLogger();

    //  protected final Object syncLock = new Object();
    private final LoggerNameModel loggerNameModelDelegate = new LoggerNameModelSupport();
    private final Object mutex = new Object();

    // because we may be using a cyclic buffer, if an ID is not provided in the property,
    // use and increment this row counter as the ID for each received row
    private int uniqueRow;
    private final Set uniquePropertyKeys = new HashSet();
    private Rule ruleMediator;
    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
    private RuleColorizer colorizer;
    private final String tableModelName;

    public ChainsawCyclicBufferTableModel(int cyclicBufferSize, RuleColorizer colorizer, String tableModelName) {
        propertySupport.addPropertyChangeListener("cyclic", new ModelChanger());
        this.cyclicBufferSize = cyclicBufferSize;
        this.colorizer = colorizer;
        this.tableModelName = tableModelName;

        unfilteredList = new CyclicBufferList(cyclicBufferSize);
        filteredList = new CyclicBufferList(cyclicBufferSize);
    }

    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof Rule) {
            if (evt.getSource() == ruleMediator && evt.getPropertyName().equals("findRule")) {
                if (((RuleMediator) evt.getSource()).isFindRuleRequired()) {
                    // only refilter if find rule is required
                    reFilter();
                }
            } else {
                reFilter();
            }
        }
    }

    @Override
    public List<LoggingEventWrapper> getMatchingEvents(Rule rule) {
        List<LoggingEventWrapper> list = new ArrayList<>();
        List<LoggingEventWrapper> unfilteredCopy;
        synchronized (mutex) {
            unfilteredCopy = new ArrayList<>(unfilteredList);
        }

        for (LoggingEventWrapper loggingEventWrapper : unfilteredCopy) {
            if (rule.evaluate(loggingEventWrapper.getLoggingEvent(), null)) {
                list.add(loggingEventWrapper);
            }
        }

        return list;
    }

    @Override
    public void reFilter() {
        final int previousSize;
        final int newSize;
        synchronized (mutex) {
            // post refilter with newValue of TRUE (filtering is about to begin)
            propertySupport.firePropertyChange("refilter", Boolean.FALSE, Boolean.TRUE);
            previousSize = filteredList.size();
            filteredList.clear();
            if (ruleMediator == null) {
                LoggingEventWrapper lastEvent = null;
                for (LoggingEventWrapper loggingEventWrapper : unfilteredList) {
                    loggingEventWrapper.setDisplayed(true);
                    updateEventMillisDelta(loggingEventWrapper, lastEvent);
                    filteredList.add(loggingEventWrapper);
                    lastEvent = loggingEventWrapper;
                }
            } else {
                Iterator<LoggingEventWrapper> iter = unfilteredList.iterator();
                LoggingEventWrapper lastEvent = null;
                while (iter.hasNext()) {
                    LoggingEventWrapper loggingEventWrapper = iter.next();

                    if (ruleMediator.evaluate(loggingEventWrapper.getLoggingEvent(), null)) {
                        loggingEventWrapper.setDisplayed(true);
                        filteredList.add(loggingEventWrapper);
                        updateEventMillisDelta(loggingEventWrapper, lastEvent);
                        lastEvent = loggingEventWrapper;
                    } else {
                        loggingEventWrapper.setDisplayed(false);
                    }
                }
            }
            newSize = filteredList.size();
        }
        SwingHelper.invokeOnEDT(() -> {
            if (newSize > 0) {
                if (previousSize == newSize) {
                    // same - update all
                    fireTableRowsUpdated(0, newSize - 1);
                } else if (previousSize > newSize) {
                    // less now..update and delete difference
                    fireTableRowsUpdated(0, newSize - 1);
                    // swing bug exposed by variable height rows when calling fireTableRowsDeleted..use
                    // tabledatacchanged
                    fireTableDataChanged();
                } else if (previousSize < newSize) {
                    // more now..update and insert difference
                    if (previousSize > 0) {
                        fireTableRowsUpdated(0, previousSize - 1);
                    }
                    fireTableRowsInserted(Math.max(0, previousSize), newSize - 1);
                }
            } else {
                // no rows to show
                fireTableDataChanged();
            }
            notifyCountListeners();
            // post refilter with newValue of FALSE (filtering is complete)
            SwingHelper.invokeOnEDT(() -> propertySupport.firePropertyChange("refilter", Boolean.TRUE, Boolean.FALSE));
        });
    }

    @Override
    public int locate(Rule rule, int startLocation, boolean searchForward) {
        List<LoggingEventWrapper> filteredListCopy;
        synchronized (mutex) {
            filteredListCopy = new ArrayList<>(filteredList);
        }
        if (searchForward) {
            for (int i = startLocation; i < filteredListCopy.size(); i++) {
                if (rule.evaluate((filteredListCopy.get(i)).getLoggingEvent(), null)) {
                    return i;
                }
            }
            // if there was no match, start at row zero and go to startLocation
            for (int i = 0; i < startLocation; i++) {
                if (rule.evaluate((filteredListCopy.get(i)).getLoggingEvent(), null)) {
                    return i;
                }
            }
        } else {
            for (int i = startLocation; i > -1; i--) {
                if (rule.evaluate((filteredListCopy.get(i)).getLoggingEvent(), null)) {
                    return i;
                }
            }
            // if there was no match, start at row list.size() - 1 and go to startLocation
            for (int i = filteredListCopy.size() - 1; i > startLocation; i--) {
                if (rule.evaluate((filteredListCopy.get(i)).getLoggingEvent(), null)) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public void removeLoggerNameListener(LoggerNameListener l) {
        loggerNameModelDelegate.removeLoggerNameListener(l);
    }

    @Override
    public boolean addLoggerName(String loggerName) {
        return loggerNameModelDelegate.addLoggerName(loggerName);
    }

    @Override
    public String toString() {
        return "ChainsawCyclicBufferTableModel{name='" + tableModelName + '\'' + '}';
    }

    @Override
    public void reset() {
        loggerNameModelDelegate.reset();
    }

    @Override
    public void addLoggerNameListener(LoggerNameListener l) {
        loggerNameModelDelegate.addLoggerNameListener(l);
    }

    @Override
    public Collection<String> getLoggerNames() {
        return loggerNameModelDelegate.getLoggerNames();
    }

    @Override
    public void addEventCountListener(EventCountListener listener) {
        eventListenerList.add(EventCountListener.class, listener);
    }

    @Override
    public boolean isSortable(int col) {
        return true;
    }

    @Override
    public void notifyCountListeners() {
        EventCountListener[] listeners = eventListenerList.getListeners(EventCountListener.class);

        int filteredListSize;
        int unfilteredListSize;
        synchronized (mutex) {
            filteredListSize = filteredList.size();
            unfilteredListSize = unfilteredList.size();
        }
        for (EventCountListener listener : listeners) {
            listener.eventCountChanged(filteredListSize, unfilteredListSize);
        }
    }

    /**
     * Changes the underlying display rule in use.  If there was
     * a previous Rule defined, this Model removes itself as a listener
     * from the old rule, and adds itself to the new rule (if the new Rule is not Null).
     * <p>
     * In any case, the model ensures the Filtered list is made up to date in a separate thread.
     */
    @Override
    public void setRuleMediator(RuleMediator ruleMediator) {
        if (this.ruleMediator != null) {
            this.ruleMediator.removePropertyChangeListener(this);
        }

        this.ruleMediator = ruleMediator;

        if (this.ruleMediator != null) {
            this.ruleMediator.addPropertyChangeListener(this);
        }
        reFilter();
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#sort()
     */
    @Override
    public void sort() {
        boolean sort;
        final int filteredListSize;
        synchronized (mutex) {
            filteredListSize = filteredList.size();
            sort = (sortEnabled && filteredListSize > 0);
            if (sort) {
                // reset display (used to ensure row height is updated)
                LoggingEventWrapper lastEvent = null;
                for (LoggingEventWrapper e : filteredList) {
                    e.setDisplayed(true);
                    updateEventMillisDelta(e, lastEvent);
                    lastEvent = e;
                }
                filteredList.sort(new ColumnComparator(
                        getColumnName(currentSortColumn), currentSortColumn, currentSortAscending));
            }
        }
        if (sort) {
            SwingHelper.invokeOnEDT(() -> fireTableRowsUpdated(0, Math.max(filteredListSize - 1, 0)));
        }
    }

    @Override
    public boolean isSortEnabled() {
        return sortEnabled;
    }

    @Override
    public void sortColumn(int col, boolean ascending) {
        logger.debug("request to sort col={}", col);
        currentSortAscending = ascending;
        currentSortColumn = col;
        sortEnabled = true;
        sort();
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#clear()
     */
    @Override
    public void clearModel() {
        reachedCapacity = false;

        synchronized (mutex) {
            unfilteredList.clear();
            filteredList.clear();
            uniqueRow = 0;
        }

        SwingHelper.invokeOnEDT(this::fireTableDataChanged);

        notifyCountListeners();
        loggerNameModelDelegate.reset();
    }

    @Override
    public List<LoggingEventWrapper> getAllEvents() {
        synchronized (mutex) {
            return new ArrayList<>(unfilteredList);
        }
    }

    @Override
    public List<LoggingEventWrapper> getFilteredEvents() {

        synchronized (mutex) {
            return new ArrayList(filteredList);
        }
    }

    @Override
    public int getRowIndex(LoggingEventWrapper loggingEventWrapper) {
        synchronized (mutex) {
            return filteredList.indexOf(loggingEventWrapper);
        }
    }

    @Override
    public void removePropertyFromEvents(String propName) {
        // first remove the event from any displayed events, so we can fire row updated event
        List<LoggingEventWrapper> filteredListCopy;
        List<LoggingEventWrapper> unfilteredListCopy;
        synchronized (mutex) {
            filteredListCopy = new ArrayList<>(filteredList);
            unfilteredListCopy = new ArrayList<>(unfilteredList);
        }
        for (int i = 0; i < filteredListCopy.size(); i++) {
            LoggingEventWrapper loggingEventWrapper = filteredListCopy.get(i);
            Object result = loggingEventWrapper.removeProperty(propName);
            if (result != null) {
                fireRowUpdated(i, false);
            }
        }
        // now remove the event from all events
        for (LoggingEventWrapper loggingEventWrapper : unfilteredListCopy) {
            loggingEventWrapper.removeProperty(propName);
        }
    }

    @Override
    public int updateEventsWithFindRule(Rule findRule) {
        int count = 0;
        List<LoggingEventWrapper> unfilteredListCopy;
        synchronized (mutex) {
            unfilteredListCopy = new ArrayList<>(unfilteredList);
        }
        for (LoggingEventWrapper loggingEventWrapper : unfilteredListCopy) {
            loggingEventWrapper.evaluateSearchRule(findRule);
            // return the count of visible search matches
            if (loggingEventWrapper.isSearchMatch() && loggingEventWrapper.isDisplayed()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int findColoredRow(int startLocation, boolean searchForward) {
        List<LoggingEventWrapper> filteredListCopy;
        synchronized (mutex) {
            filteredListCopy = new ArrayList<>(filteredList);
        }
        if (searchForward) {
            for (int i = startLocation; i < filteredListCopy.size(); i++) {
                LoggingEventWrapper event = filteredListCopy.get(i);
                if (!event.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)
                        || !event.getColorRuleForeground().equals(ChainsawConstants.COLOR_DEFAULT_FOREGROUND)) {
                    return i;
                }
            }
            // searching forward, no colorized event was found - now start at row zero and go to startLocation
            for (int i = 0; i < startLocation; i++) {
                LoggingEventWrapper event = filteredListCopy.get(i);
                if (!event.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)
                        || !event.getColorRuleForeground().equals(ChainsawConstants.COLOR_DEFAULT_FOREGROUND)) {
                    return i;
                }
            }
        } else {
            for (int i = startLocation; i > -1; i--) {
                LoggingEventWrapper event = filteredListCopy.get(i);
                if (!event.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)
                        || !event.getColorRuleForeground().equals(ChainsawConstants.COLOR_DEFAULT_FOREGROUND)) {
                    return i;
                }
            }
            // searching backward, no colorized event was found - now start at list.size() - 1 and go to startLocation
            for (int i = filteredListCopy.size() - 1; i > startLocation; i--) {
                LoggingEventWrapper event = filteredListCopy.get(i);
                if (!event.getColorRuleBackground().equals(ChainsawConstants.COLOR_DEFAULT_BACKGROUND)
                        || !event.getColorRuleForeground().equals(ChainsawConstants.COLOR_DEFAULT_FOREGROUND)) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public int getSearchMatchCount() {
        int searchMatchCount = 0;
        synchronized (mutex) {
            for (LoggingEventWrapper wrapper : filteredList) {
                if (wrapper.isSearchMatch() && wrapper.isDisplayed()) {
                    searchMatchCount++;
                }
            }
        }
        return searchMatchCount;
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public LoggingEventWrapper getRow(int row) {
        synchronized (mutex) {
            if (row < filteredList.size() && row > -1) {
                return filteredList.get(row);
            }
        }

        return null;
    }

    @Override
    public int getRowCount() {
        synchronized (mutex) {
            return filteredList.size();
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ChainsawLoggingEvent event = null;

        synchronized (mutex) {
            if (rowIndex < filteredList.size() && rowIndex > -1) {
                event = (filteredList.get(rowIndex)).getLoggingEvent();
            }
        }

        if (event == null) {
            return null;
        }

        switch (columnIndex + 1) {
            case ChainsawColumns.INDEX_ID_COL_NAME:
                Object id = event.getProperty(Constants.LOG4J_ID_KEY);
                return Objects.requireNonNullElse(id, rowIndex);
            case ChainsawColumns.INDEX_LEVEL_COL_NAME:
                return event.m_level;

            case ChainsawColumns.INDEX_LOG4J_MARKER_COL_NAME:
                return event.getProperty(ChainsawConstants.LOG4J_MARKER_COL_NAME);

                /*
                            case ChainsawColumns.INDEX_MILLIS_DELTA_COL_NAME:
                                return event.getProperty(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE);

                */
            case ChainsawColumns.INDEX_LOGGER_COL_NAME:
                return event.m_logger;

            case ChainsawColumns.INDEX_TIMESTAMP_COL_NAME:
                return event.m_timestamp;

            case ChainsawColumns.INDEX_MESSAGE_COL_NAME:
                return event.m_message;

                /*
                            case ChainsawColumns.INDEX_NDC_COL_NAME:
                                return event.m_ndc;

                            case ChainsawColumns.INDEX_THREAD_COL_NAME:
                                return event.m_threadName;

                            case ChainsawColumns.INDEX_THROWABLE_COL_NAME:
                                return "IMPLEMENT ME";

                            case ChainsawColumns.INDEX_CLASS_COL_NAME:
                                return info == null ? "" : info.className;

                            case ChainsawColumns.INDEX_FILE_COL_NAME:
                                return info == null ? "" : info.fileName;

                            case ChainsawColumns.INDEX_LINE_COL_NAME:
                                return info == null ? -1 : info.lineNumber;

                            case ChainsawColumns.INDEX_METHOD_COL_NAME:
                                return info == null ? "" : info.methodName;
                */

            default:

                //                if (columnIndex < columnNames.size()) {
                //                    //case may not match..try case sensitive and fall back to case-insensitive
                //                    String result = event.getProperty(columnNames.get(columnIndex).toString());
                //                    if (result == null) {
                //                        String lowerColName =
                // columnNames.get(columnIndex).toString().toLowerCase(Locale.ENGLISH);
                //                        Set<String> entrySet = event.entrySet();
                //                        for (String entry : entrySet) {
                //                            if (entry.equalsIgnoreCase(lowerColName)) {
                //                                result = thisEntry.getValue().toString();
                //                            }
                //                        }
                //                    }
                //                    if (result != null) {
                //                        return result;
                //                    }
                //                }
        }
        return "";
    }

    @Override
    public boolean isAddRow(LoggingEventWrapper loggingEventWrapper) {
        Object id = loggingEventWrapper.getLoggingEvent().getProperty(Constants.LOG4J_ID_KEY);

        // only set the property if it doesn't already exist
        if (id == null) {
            id = ++uniqueRow;
            loggingEventWrapper.setProperty(Constants.LOG4J_ID_KEY, id.toString());
        }

        loggingEventWrapper.updateColorRuleColors(
                colorizer.getBackgroundColor(loggingEventWrapper.getLoggingEvent()),
                colorizer.getForegroundColor(loggingEventWrapper.getLoggingEvent()));
        Rule findRule = colorizer.getFindRule();
        if (findRule != null) {
            loggingEventWrapper.evaluateSearchRule(colorizer.getFindRule());
        }

        boolean rowAdded = false;

        /*
         * If we're in cyclic mode and over budget on the size, the addition of a new event will
         * cause the oldest event to fall off the cliff. We need to remove that events ID from the
         * Set so we are not keeping track of IDs for all events ever received (we'd run out of
         * memory...)
         */
        synchronized (mutex) {
            if (cyclic) {
                CyclicBufferList bufferList = (CyclicBufferList) unfilteredList;
                if (bufferList.size() == bufferList.getMaxSize()) {
                    reachedCapacity = true;
                }
            }
            int unfilteredSize = unfilteredList.size();
            LoggingEventWrapper lastLoggingEventWrapper = null;
            if (unfilteredSize > 0) {
                lastLoggingEventWrapper = unfilteredList.get(unfilteredSize - 1);
            }
            unfilteredList.add(loggingEventWrapper);
            if ((ruleMediator == null) || (ruleMediator.evaluate(loggingEventWrapper.getLoggingEvent(), null))) {
                loggingEventWrapper.setDisplayed(true);
                updateEventMillisDelta(loggingEventWrapper, lastLoggingEventWrapper);
                filteredList.add(loggingEventWrapper);
                rowAdded = true;
            } else {
                loggingEventWrapper.setDisplayed(false);
            }
        }

        checkForNewColumn(loggingEventWrapper);

        return rowAdded;
    }

    private void updateEventMillisDelta(
            LoggingEventWrapper loggingEventWrapper, LoggingEventWrapper lastLoggingEventWrapper) {
        if (lastLoggingEventWrapper != null) {
            loggingEventWrapper.setPreviousDisplayedEventTimestamp(
                    lastLoggingEventWrapper.getLoggingEvent().m_timestamp);
        } else {
            // delta to same event = 0
            loggingEventWrapper.setPreviousDisplayedEventTimestamp(loggingEventWrapper.getLoggingEvent().m_timestamp);
        }
    }

    private void checkForNewColumn(LoggingEventWrapper loggingEventWrapper) {
        /**
         * Is this a new Property key we haven't seen before?  Remember that now MDC has been merged
         * into the Properties collection
         */
        boolean newColumn = uniquePropertyKeys.addAll(loggingEventWrapper.getPropertyKeySet());

        if (newColumn) {
            /**
             * If so, we should add them as columns and notify listeners.
             */
            for (Object o : loggingEventWrapper.getPropertyKeySet()) {
                String key = o.toString().toUpperCase();

                // add all keys except the 'log4jid' key (columnNames is all-caps)
                if (!columnNames.contains(key) && !(Constants.LOG4J_ID_KEY.equalsIgnoreCase(key))) {
                    columnNames.add(key);
                    logger.debug("Adding col '{}', columnNames={}", key, columnNames);
                    fireNewKeyColumnAdded(new NewKeyEvent(
                            this,
                            columnNames.indexOf(key),
                            key,
                            loggingEventWrapper.getLoggingEvent().getProperty(key)));
                }
            }
        }
    }

    @Override
    public void fireTableEvent(final int begin, final int end, final int count) {
        SwingHelper.invokeOnEDT(() -> {
            if (cyclic) {
                if (!reachedCapacity) {
                    // if we didn't loop and it's the 1st time, insert
                    if ((begin + count) < cyclicBufferSize) {
                        fireTableRowsInserted(begin, end);
                    } else {
                        // we did loop - insert and then update rows
                        // rows are zero-indexed, subtract 1 from cyclicbuffersize for the event notification
                        fireTableRowsInserted(begin, cyclicBufferSize - 1);
                        fireTableRowsUpdated(0, cyclicBufferSize - 1);
                        reachedCapacity = true;
                    }
                } else {
                    fireTableRowsUpdated(0, cyclicBufferSize - 1);
                }
            } else {
                fireTableRowsInserted(begin, end);
            }
        });
    }

    @Override
    public void fireRowUpdated(int row, boolean checkForNewColumns) {
        LoggingEventWrapper loggingEventWrapper = getRow(row);
        if (loggingEventWrapper != null) {
            loggingEventWrapper.updateColorRuleColors(
                    colorizer.getBackgroundColor(loggingEventWrapper.getLoggingEvent()),
                    colorizer.getForegroundColor(loggingEventWrapper.getLoggingEvent()));
            Rule findRule = colorizer.getFindRule();
            if (findRule != null) {
                loggingEventWrapper.evaluateSearchRule(colorizer.getFindRule());
            }

            fireTableRowsUpdated(row, row);
            if (checkForNewColumns) {
                // row may have had a column added..if so, make sure a column is added
                checkForNewColumn(loggingEventWrapper);
            }
        }
    }

    private void fireNewKeyColumnAdded(NewKeyEvent e) {
        NewKeyListener[] listeners = eventListenerList.getListeners(NewKeyListener.class);

        for (NewKeyListener listener : listeners) {
            listener.newKeyAdded(e);
        }
    }

    @Override
    public int getMaxSize() {
        return cyclicBufferSize;
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#addNewKeyListener(org.apache.log4j.chainsaw.NewKeyListener)
     */
    @Override
    public void addNewKeyListener(NewKeyListener l) {
        eventListenerList.add(NewKeyListener.class, l);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#removeNewKeyListener(org.apache.log4j.chainsaw.NewKeyListener)
     */
    @Override
    public void removeNewKeyListener(NewKeyListener l) {
        eventListenerList.remove(NewKeyListener.class, l);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#isCellEditable(int, int)
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (getColumnName(columnIndex).equalsIgnoreCase(ChainsawConstants.LOG4J_MARKER_COL_NAME)) {
            return true;
        }

        return columnIndex < columnNames.size() && super.isCellEditable(rowIndex, columnIndex);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#setCyclic(boolean)
     */
    @Override
    public void setCyclic(final boolean cyclic) {
        if (this.cyclic == cyclic) {
            return;
        }

        final boolean old = this.cyclic;
        this.cyclic = cyclic;
        propertySupport.firePropertyChange("cyclic", old, cyclic);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertySupport.addPropertyChangeListener(l);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)
     */
    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        propertySupport.addPropertyChangeListener(propertyName, l);
    }

    /* (non-Javadoc)
     * @see org.apache.log4j.chainsaw.EventContainer#size()
     */
    @Override
    public int size() {
        synchronized (mutex) {
            return unfilteredList.size();
        }
    }

    private class ModelChanger implements PropertyChangeListener {
        /* (non-Javadoc)
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void propertyChange(PropertyChangeEvent arg0) {
            Thread thread = new Thread(() -> {
                ProgressMonitor monitor = null;

                int index = 0;

                try {
                    synchronized (mutex) {
                        monitor = new ProgressMonitor(
                                null,
                                "Switching models...",
                                "Transferring between data structures, please wait...",
                                0,
                                unfilteredList.size() + 1);
                        monitor.setMillisToDecideToPopup(250);
                        monitor.setMillisToPopup(100);
                        logger.debug("Changing Model, isCyclic is now {}", cyclic);

                        List newUnfilteredList;
                        List newFilteredList;

                        if (cyclic) {
                            newUnfilteredList = new CyclicBufferList(cyclicBufferSize);
                            newFilteredList = new CyclicBufferList(cyclicBufferSize);
                        } else {
                            newUnfilteredList = new ArrayList(cyclicBufferSize);
                            newFilteredList = new ArrayList(cyclicBufferSize);
                        }

                        for (Object anUnfilteredList : unfilteredList) {
                            LoggingEventWrapper loggingEventWrapper = (LoggingEventWrapper) anUnfilteredList;
                            newUnfilteredList.add(loggingEventWrapper);
                            monitor.setProgress(index++);
                        }

                        unfilteredList = newUnfilteredList;
                        filteredList = newFilteredList;
                    }

                    monitor.setNote("Refiltering...");
                    reFilter();

                    monitor.setProgress(index++);
                } finally {
                    if (monitor != null) {
                        monitor.close();
                    }
                }

                logger.debug("Model Change completed");
            });
            thread.setPriority(Thread.MIN_PRIORITY + 1);
            thread.start();
        }
    }
}
