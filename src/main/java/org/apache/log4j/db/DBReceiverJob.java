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

package org.apache.log4j.db;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.log4j.scheduler.Job;
import org.apache.log4j.spi.ComponentBase;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Actual retrieval of data is made by the instance of DBReceiverJob associated
 * with DBReceiver.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
class DBReceiverJob extends ComponentBase implements Job {

  String sqlException = "SELECT trace_line FROM logging_event_exception where event_id=? ORDER by i ASC";
  String sqlProperties = "SELECT mapped_key, mapped_value FROM logging_event_property WHERE event_id=?";
  String sqlSelect = 
    "SELECT " +
    "sequence_number, timestamp, rendered_message, logger_name, " +
    "level_string, ndc, thread_name, reference_flag, " +
    "caller_filename, caller_class, caller_method, caller_line, " +
    "event_id " +
    "FROM logging_event " +
    "WHERE event_id > ?  ORDER BY event_id ASC";


  long lastId = Short.MIN_VALUE;

  DBReceiver parentDBReceiver;

  DBReceiverJob(DBReceiver parent) {
    parentDBReceiver = parent;
  }

  public void execute() {
    getLogger().debug("DBReceiverJob.execute() called");

    Connection connection = null;

    try {
      connection = parentDBReceiver.connectionSource.getConnection();
      PreparedStatement statement = connection.prepareStatement(sqlSelect);
      statement.setLong(1, lastId);
      ResultSet rs = statement.executeQuery();
      //rs.beforeFirst();

      while (rs.next()) {
	    Logger logger = null;
	    long timeStamp = 0L;
	    String level = null;
	    String threadName = null;
	    String message = null;
	    String ndc = null;
	    String className = null;
	    String methodName = null;
	    String fileName = null;
	    String lineNumber = null;
	    Map<String, String> properties = new HashMap<>();
	

        //event.setSequenceNumber(rs.getLong(1));
        timeStamp = rs.getLong(2);
        message = rs.getString(3);
		logger = LogManager.getLogger(rs.getString(4));
        level = rs.getString(5);
		Level levelImpl = Level.toLevel(level.trim());

        ndc = rs.getString(6);
        threadName = rs.getString(7);

        short mask = rs.getShort(8);

        fileName = rs.getString(9);
        className = rs.getString(10);
        methodName = rs.getString(11);
        lineNumber = rs.getString(12).trim();

		StackTraceElement locationInfo;
        if (fileName.equals("?")) {
          locationInfo = null;
        } else {
          locationInfo = new StackTraceElement(fileName, className,
              methodName, Integer.parseInt(lineNumber));
        }

        long id = rs.getLong(13);
        //LogLog.info("Received event with id=" + id);
        lastId = id;

		ThrowableProxy throwableInfo = null;
        if ((mask & DBHelper.EXCEPTION_EXISTS) != 0) {
          throwableInfo = getException(connection, id);
        }

        // Scott asked for this info to be
        properties.put("log4jid", Long.toString(id));

	    LogEvent event = Log4jLogEvent.newBuilder()
	                .setLoggerFqcn(logger.getName())
	                .setLoggerName(logger.getName())
	                .setTimeMillis(timeStamp)
	                .setLevel(levelImpl)
	                .setMessage(new SimpleMessage(message))
	                .setThreadName(threadName)
	                .setThrownProxy(throwableInfo)
                    .setContextStack(new MutableThreadContextStack(Arrays.asList(ndc.split(" "))))
	                .setContextMap(properties)
	                .setSource(locationInfo)
	                .build();

        if ((mask & DBHelper.PROPERTIES_EXIST) != 0) {
          getProperties(connection, id, event);
        }

        if (!parentDBReceiver.isPaused()) {
          parentDBReceiver.doPost(event);
        }
      } // while
      statement.close();
      statement = null;
    } catch (SQLException sqle) {
      getLogger().error("Problem receiving events", sqle);
    } finally {
      closeConnection(connection);
    }
  }

  void closeConnection(Connection connection) {
    if (connection != null) {
      try {
        //LogLog.warn("closing the connection. ", new Exception("x"));
        connection.close();
      } catch (SQLException sqle) {
        // nothing we can do here
      }
    }
  }

  /**
   * Retrieve the event properties from the logging_event_property table.
   * 
   * @param connection
   * @param id
   * @param event
   * @throws SQLException
   */
  void getProperties(Connection connection, long id, LogEvent event)
      throws SQLException {

    PreparedStatement statement = connection.prepareStatement(sqlProperties);
    try {
      statement.setLong(1, id);
      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        String key = rs.getString(1);
        String value = rs.getString(2);
        event.getContextMap().put(key, value);
      }
    } finally {
      statement.close();
    }
  }

  /**
   * Retrieve the exception string representation from the
   * logging_event_exception table.
   * 
   * @param connection
   * @param id
   * @throws SQLException
   */
  ThrowableProxy getException(Connection connection, long id)
      throws SQLException {

    PreparedStatement statement = null;

    try {
      statement = connection.prepareStatement(sqlException);
      statement.setLong(1, id);
      ResultSet rs = statement.executeQuery();

      Vector v = new Vector();

      while (rs.next()) {
        //int i = rs.getShort(1);
        v.add(rs.getString(1));
      }

      int len = v.size();
      String[] strRep = new String[len];
      for (int i = 0; i < len; i++) {
        strRep[i] = (String) v.get(i);
      }
      // we've filled strRep, we now attach it to the event
      return new ThrowableProxy(strRep);
    } finally {
      if (statement != null) {
        statement.close();
      }
    }
  }
}