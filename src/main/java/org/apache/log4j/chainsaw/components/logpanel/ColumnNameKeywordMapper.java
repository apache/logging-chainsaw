package org.apache.log4j.chainsaw.components.logpanel;

import org.apache.log4j.chainsaw.ChainsawConstants;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.spi.LoggingEventFieldResolver;

import java.util.HashMap;
import java.util.Map;

public class ColumnNameKeywordMapper {

    final Map<String, String> columnNameKeywordMap = new HashMap<>();

    {
        columnNameKeywordMap.put(ChainsawConstants.CLASS_COL_NAME, LoggingEventFieldResolver.CLASS_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.FILE_COL_NAME, LoggingEventFieldResolver.FILE_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.LEVEL_COL_NAME, LoggingEventFieldResolver.LEVEL_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.LINE_COL_NAME, LoggingEventFieldResolver.LINE_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.LOGGER_COL_NAME, LoggingEventFieldResolver.LOGGER_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.NDC_COL_NAME, LoggingEventFieldResolver.NDC_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.MESSAGE_COL_NAME, LoggingEventFieldResolver.MSG_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.THREAD_COL_NAME, LoggingEventFieldResolver.THREAD_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.THROWABLE_COL_NAME, LoggingEventFieldResolver.EXCEPTION_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.TIMESTAMP_COL_NAME, LoggingEventFieldResolver.TIMESTAMP_FIELD);
        columnNameKeywordMap.put(ChainsawConstants.ID_COL_NAME.toUpperCase(), LoggingEventFieldResolver.PROP_FIELD + Constants.LOG4J_ID_KEY);
        columnNameKeywordMap.put(ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE.toUpperCase(), LoggingEventFieldResolver.PROP_FIELD + ChainsawConstants.LOG4J_MARKER_COL_NAME_LOWERCASE);
        columnNameKeywordMap.put(ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE.toUpperCase(), LoggingEventFieldResolver.PROP_FIELD + ChainsawConstants.MILLIS_DELTA_COL_NAME_LOWERCASE);
    }

    public boolean contains(String key) {
        return columnNameKeywordMap.containsKey(key);
    }

    public String get(String key) {
        return columnNameKeywordMap.get(key);
    }

    public void put(String key, String value) {
        columnNameKeywordMap.put(key, value);
    }
}
