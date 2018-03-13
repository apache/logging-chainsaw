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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.VectorAppender;
import org.apache.log4j.LoggerRepositoryExImpl;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.spi.LoggerRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;


/**
 * This test case writes a few events into a databases and reads them
 * back comparing the event written and read back.
 * 
 * <p>It relies heavily on the proper configuration of its environment
 * in joran config files as well system properties.
 * </p>
 * 
 * <p>See also the Ant build file in the tests/ directory.</p> 
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class FullCycleDBTest
       extends TestCase {
  
  Vector<LoggingEvent> witnessEvents;
  Hierarchy lrWrite;
  LoggerRepository lrRead;
  String appendConfigFile = null;
  String readConfigFile = null;
  
  
  /*
   * @see TestCase#setUp()
   */
  protected void setUp()
         throws Exception {
    super.setUp();
    appendConfigFile = "append-with-drivermanager1.xml";
    readConfigFile = "read-with-drivermanager1.xml";

    witnessEvents = new Vector<LoggingEvent>();
    lrWrite = new Hierarchy(new RootLogger(Level.DEBUG));
    lrRead = new LoggerRepositoryExImpl(new Hierarchy(new RootLogger(Level.DEBUG)));


    //
    //   attempt to define tables in in-memory database
    //      will throw exception if already defined.
    //
        Class.forName("org.hsqldb.jdbcDriver");
      try (Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:testdb")) {
          Statement s = connection.createStatement();
          s.executeUpdate("CREATE TABLE logging_event " +
                  "( sequence_number   BIGINT NOT NULL, " +
                  " timestamp         BIGINT NOT NULL, " +
                  " rendered_message  LONGVARCHAR NOT NULL, " +
                  " logger_name       VARCHAR NOT NULL, " +
                  " level_string      VARCHAR NOT NULL, " +
                  " ndc               LONGVARCHAR, " +
                  " thread_name       VARCHAR, " +
                  " reference_flag    SMALLINT, " +
                  " caller_filename   VARCHAR, " +
                  " caller_class      VARCHAR, " +
                  " caller_method     VARCHAR, " +
                  " caller_line       CHAR(4), " +
                  " event_id          INT NOT NULL IDENTITY)");
          s.executeUpdate("CREATE TABLE logging_event_property " +
                  "( event_id	      INT NOT NULL, " +
                  " mapped_key        VARCHAR(254) NOT NULL, " +
                  " mapped_value      LONGVARCHAR, " +
                  " PRIMARY KEY(event_id, mapped_key), " +
                  " FOREIGN KEY (event_id) REFERENCES logging_event(event_id))");
          s.executeUpdate("CREATE TABLE logging_event_exception" +
                  "  ( event_id         INT NOT NULL, " +
                  "    i                SMALLINT NOT NULL," +
                  "    trace_line       VARCHAR NOT NULL," +
                  "    PRIMARY KEY(event_id, i)," +
                  "    FOREIGN KEY (event_id) REFERENCES logging_event(event_id))");
      } catch (SQLException ex) {
          String s = ex.toString();
      }

  }


  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown()
         throws Exception {
    super.tearDown();
    lrRead.shutdown();
    witnessEvents = null;
  }

  public FullCycleDBTest(String arg0) {
    super(arg0);
  }

  
  /**
   * This test starts by writing a single event to a DB using DBAppender
   * and then reads it back using DBReceiver.
   * 
   * DB related information is specified within the configuration files.
   * @throws Exception on error
   */
  public void testSingleOutput()
         throws Exception {
    DOMConfigurator jc1 = new DOMConfigurator();
    InputStream is = FullCycleDBTest.class.getResourceAsStream(appendConfigFile);
    jc1.doConfigure(is, lrWrite);
    is.close();
  
    long startTime = System.currentTimeMillis();
    System.out.println("***startTime is  "+startTime);

    // Write out just one log message
    Logger out = lrWrite.getLogger("testSingleOutput.out");
    out.debug("some message"+startTime);

    VectorAppender witnessAppender = (VectorAppender) lrWrite.getRootLogger().getAppender("VECTOR");
    witnessEvents = witnessAppender.getVector();
    assertEquals(1, witnessEvents.size());    

    // We have to close all appenders before starting to read
    lrWrite.shutdown();

    // now read it back
    readBack(readConfigFile, startTime);

  }

  /**
   * This test starts by writing a single event to a DB using DBAppender
   * and then reads it back using DBReceiver.
   * 
   * The written event includes MDC and repository properties as well as
   * exception info.
   * 
   * DB related information is specified within the configuration files.
   * @throws IOException on error
   */
  public void testAllFields() throws IOException {
    DOMConfigurator jc1 = new DOMConfigurator();
    InputStream is = FullCycleDBTest.class.getResourceAsStream(appendConfigFile);
    jc1.doConfigure(is, lrWrite);
    is.close();
  
    long startTime = System.currentTimeMillis();
    
    // Write out just one log message
    MDC.put("key1", "value1-"+startTime);
    MDC.put("key2", "value2-"+startTime);
    Map mdcMap = MDC.getContext();
//    LogLog.info("**********"+mdcMap.size());
    
    // Write out just one log message
    Logger out = lrWrite.getLogger("out"+startTime);

    out.debug("some message"+startTime);
    MDC.put("key3", "value2-"+startTime);
    out.error("some error message"+startTime, new Exception("testing"));
    
    // we clear the MDC to avoid interference with the events read back from
    // the db
    MDC.remove("key1");
    MDC.remove("key2");
    MDC.remove("key3");

    VectorAppender witnessAppender = (VectorAppender) lrWrite.getRootLogger().getAppender("VECTOR");
    witnessEvents = witnessAppender.getVector();
    assertEquals(2, witnessEvents.size());    

    // We have to close all appenders just before starting to read
    lrWrite.shutdown();
    
    readBack(readConfigFile, startTime);
  }


  void readBack(String configfile, long startTime) throws IOException {
    DOMConfigurator jc2 = new DOMConfigurator();
    InputStream is = FullCycleDBTest.class.getResourceAsStream(configfile);
    jc2.doConfigure(is, lrRead);
    is.close();
    
    // wait a little to allow events to be read
    try { Thread.sleep(3100); } catch(Exception e) {}
    VectorAppender va = (VectorAppender) lrRead.getRootLogger().getAppender("VECTOR");
    Vector<LoggingEvent> returnedEvents = getRelevantEventsFromVA(va, startTime);
    
    compareEvents(witnessEvents, returnedEvents);
    
  }
  
  void compareEvents(Vector<LoggingEvent> l, Vector<LoggingEvent> r) {
    assertNotNull("left vector of events should not be null");
    assertEquals(l.size(), r.size());
    
    for(int i = 0; i < r.size(); i++) {
      LoggingEvent le = l.get(i);
      LoggingEvent re = r.get(i);
      assertEquals(le.getMessage(),        re.getMessage());
      assertEquals(le.getLoggerName(),     re.getLoggerName());
      assertEquals(le.getLevel(),          re.getLevel());
      assertEquals(le.getThreadName(), re.getThreadName());
      if(re.getTimeStamp() < le.getTimeStamp()) {
        fail("Returned event cannot preceed witness timestamp");
      }

      Map sourceMap = re.getProperties();
      Map remap;
      if (sourceMap == null) {
          remap = new HashMap();
      } else {
          remap = new HashMap(sourceMap);
          if (remap.containsKey(Constants.LOG4J_ID_KEY)) {
              remap.remove(Constants.LOG4J_ID_KEY);
          }
      }
      if(le.getProperties() == null || le.getProperties().size() == 0) {
        if(remap.size() != 0) {
          System.out.println("properties are "+remap);
          fail("Returned event should have been empty");
        }
      } else {
        assertEquals(le.getProperties(), remap);
      }
      comprareStringArrays( le.getThrowableStrRep(),  re.getThrowableStrRep());
      compareLocationInfo(le, re);
    } 
  }
  
  void comprareStringArrays(String[] la, String[] ra) {
    if((la == null) && (ra == null)) {
      return;
    }
    assertEquals(la.length, ra.length);
    for(int i = 0; i < la.length; i++) {
      assertEquals(la[i], ra[i]);
    }
  }
  
  void compareLocationInfo(LoggingEvent l, LoggingEvent r) {
    if(l.locationInformationExists()) {
      assertEquals(l.getLocationInformation().fullInfo, r.getLocationInformation().fullInfo);
    } else {
      assertEquals(LocationInfo.NA_LOCATION_INFO, r.getLocationInformation());
    }
  }
  
  Vector<LoggingEvent> getRelevantEventsFromVA(VectorAppender va, long startTime) {
    assertNotNull(va);
    Vector<LoggingEvent> v = va.getVector();
    Vector<LoggingEvent> r = new Vector<LoggingEvent>();
    // remove all elements older than startTime
      for (Object aV : v) {
          LoggingEvent event = (LoggingEvent) aV;
          if (startTime > event.getTimeStamp()) {
              System.out.println("***Removing event with timestamp " + event.getTimeStamp());
          } else {
              System.out.println("***Keeping event with timestamo" + event.getTimeStamp());
              r.add(event);
          }
      }
    return r;
  }

  void dump(Vector v) {
      for (Object aV : v) {
          LoggingEvent le = (LoggingEvent) aV;
          System.out.println("---" + le.getLevel() + " " + le.getLoggerName() + " " + le.getMessage());
      }
  }
  
  public static Test XXsuite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new FullCycleDBTest("testSingleOutput"));
    suite.addTest(new FullCycleDBTest("testAllFields"));
    return suite;
  }
}
