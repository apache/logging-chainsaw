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

import org.apache.log4j.helpers.Constants;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Comparator;


/**
 * @author Claude Duguay
 * @author Paul Smith &lt;psmith@apache.org&gt;
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class ColumnComparator implements Comparator {
    protected int index;
    protected boolean ascending;
    protected String columnName;

    public ColumnComparator(String columnName, int index, boolean ascending) {
        this.columnName = columnName;
        this.index = index;
        this.ascending = ascending;
    }

    public int compare(Object o1, Object o2) {
        int sort = 1;

        if (o1 instanceof LoggingEventWrapper && o2 instanceof LoggingEventWrapper) {

//		TODO not everything catered for here yet...

            LoggingEvent e1 = ((LoggingEventWrapper) o1).getLoggingEvent();
            LoggingEvent e2 = ((LoggingEventWrapper) o2).getLoggingEvent();

            switch (index + 1) {
                case ChainsawColumns.INDEX_LEVEL_COL_NAME:
                    sort = e1.getLevel().isGreaterOrEqual(e2.getLevel()) ? 1 : (-1);

                    break;

                case ChainsawColumns.INDEX_LOGGER_COL_NAME:
                    sort = e1.getLoggerName().compareToIgnoreCase(e2.getLoggerName());

                    break;

                case ChainsawColumns.INDEX_MESSAGE_COL_NAME:
                    sort =
                        e1.getMessage().toString().compareToIgnoreCase(
                            e2.getMessage().toString());

                    break;

                case ChainsawColumns.INDEX_NDC_COL_NAME:
                    if (e1.getNDC() != null && e2.getNDC() != null) {
                        sort =
                            e1.getNDC().compareToIgnoreCase(
                                e2.getNDC());
                    } else if (e1.getNDC() == null && e2.getNDC() == null) {
                        sort = 0;
                    } else if (e1.getNDC() == null) {
                        sort = -1;
                    } else if (e2.getNDC() == null) {
                        sort = 1;
                    }

                    break;

                case ChainsawColumns.INDEX_METHOD_COL_NAME:

                    if (
                        (e1.locationInformationExists())
                            & (e2.locationInformationExists())) {
                        sort =
                            e1.getLocationInformation().getMethodName().compareToIgnoreCase(
                                e2.getLocationInformation().getMethodName());
                    }

                    break;

                case ChainsawColumns.INDEX_CLASS_COL_NAME:

                    if (
                        (e1.locationInformationExists())
                            & (e2.locationInformationExists())) {
                        sort =
                            e1.getLocationInformation().getClassName().compareToIgnoreCase(
                                e2.getLocationInformation().getClassName());
                    }

                    break;

                case ChainsawColumns.INDEX_FILE_COL_NAME:

                    if (
                        (e1.locationInformationExists())
                            & (e2.locationInformationExists())) {
                        sort =
                            e1.getLocationInformation().getFileName().compareToIgnoreCase(
                                e2.getLocationInformation().getFileName());
                    }

                    break;

                case ChainsawColumns.INDEX_TIMESTAMP_COL_NAME:
                    sort = (Long.compare(e1.getTimeStamp(), e2.getTimeStamp()));
                    break;

                case ChainsawColumns.INDEX_THREAD_COL_NAME:
                    sort = e1.getThreadName().compareToIgnoreCase(e2.getThreadName());
                    break;

                case ChainsawColumns.INDEX_ID_COL_NAME:
                    int id1 = Integer.parseInt(e1.getProperty(Constants.LOG4J_ID_KEY));
                    int id2 = Integer.parseInt(e2.getProperty(Constants.LOG4J_ID_KEY));
                    sort = Integer.compare(id2, id1);
                    break;

                case ChainsawColumns.INDEX_THROWABLE_COL_NAME:
                    if (e1.getThrowableStrRep() != null && e2.getThrowableStrRep() != null) {
                        String[] s1 = e1.getThrowableStrRep();
                        String[] s2 = e2.getThrowableStrRep();
                        boolean foundDiff = false;
                        for (int i = 0; i < s1.length; i++) {
                            if (foundDiff || i > s2.length) {
                                break;
                            }
                            sort = s1[i].compareToIgnoreCase(s2[i]);
                            foundDiff = sort != 0;
                        }
                    }
                    break;

                case ChainsawColumns.INDEX_LINE_COL_NAME:
                    if (
                        (e1.locationInformationExists())
                            & (e2.locationInformationExists())) {
                        sort =
                            e1.getLocationInformation().getLineNumber().compareToIgnoreCase(
                                e2.getLocationInformation().getLineNumber());
                    }
                    break;

                //other columns may be Property values - see if there is an Property value matching column name
                default:
                    if (e1.getProperty(columnName) != null && e2.getProperty(columnName) != null) {
                        sort = e1.getProperty(columnName).compareToIgnoreCase(e2.getProperty(columnName));
                    }
            }
        }

        sort = Integer.compare(sort, 0);

        if (!ascending && (sort != 0)) {
            sort = (sort < 0) ? 1 : (-1);
        }

        return sort;
    }
}
