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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.NDC;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;


/**
 * Class designed to stress, and/or test the Chainsaw GUI by sending it
 * lots of Logging Events.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 *
 */
public class Generator extends Receiver implements Runnable {
  private static final Logger logger1 =
    Logger.getLogger("com.mycompany.mycomponentA");
  private static final Logger logger2 =
    Logger.getLogger("com.mycompany.mycomponentB");
  private static final Logger logger3 =
    Logger.getLogger("com.someothercompany.corecomponent");
  private final String baseString_;
  private Thread thread;
  private boolean shutdown;

  public Generator(String name) {
    setName(name);
    baseString_ = name;
  }

  private LoggingEvent createEvent(
    Level level, Logger logger, String msg, Throwable t) {
      ThrowableInformation ti = new ThrowableInformation(t);
      Map<String, String> properties = new HashMap<>();
      properties.put(Constants.APPLICATION_KEY, getName());
      properties.put(Constants.HOSTNAME_KEY, "localhost");
      LocationInfo li = new LocationInfo("file", logger.getClass().getName(), "method", "123");
    LoggingEvent e = new LoggingEvent(
        logger.getClass().getName(), logger, System.currentTimeMillis(), level, msg, "Thread=1", ti, "NDC value", li, properties);
    return e;
  }

  public void run() {
    NDC.push(baseString_);
    MDC.put("some string", "some value" + baseString_);

    int i = 0;

    while (!shutdown) {
     doPost(createEvent(Level.TRACE, logger1, "tracemsg" + i++, null));
     doPost(
        createEvent(
          Level.DEBUG, logger1,
          "debugmsg " + i++
          + " g dg sdfa sadf sdf safd fsda asfd sdfa sdaf asfd asdf fasd fasd adfs fasd adfs fads afds afds afsd afsd afsd afsd afsd fasd asfd asfd afsd fasd afsd",
          null));
     doPost(createEvent(Level.INFO, logger1, "infomsg " + i++, null));
     doPost(createEvent(Level.WARN, logger1, "warnmsg " + i++, null));
     doPost(createEvent(Level.ERROR, logger1, "errormsg " + i++, null));
     doPost(createEvent(Level.FATAL, logger1, "fatalmsg " + i++, new Exception("someexception-" + baseString_)));
     doPost(createEvent(Level.TRACE, logger2, "tracemsg" + i++, null));
     doPost(
        createEvent(
          Level.DEBUG, logger2,
          "debugmsg " + i++
          + " g dg sdfa sadf sdf safd fsda asfd sdfa sdaf asfd asdf fasd fasd adfs fasd adfs fads afds afds afsd afsd afsd afsd afsd fasd asfd asfd afsd fasd afsd",
          null));
     doPost(createEvent(Level.INFO, logger2, "infomsg " + i++, null));
     doPost(createEvent(Level.WARN, logger2, "warnmsg " + i++, null));
     doPost(createEvent(Level.ERROR, logger2, "errormsg " + i++, null));
     doPost(createEvent(Level.FATAL, logger2, "fatalmsg " + i++, new Exception("someexception-" + baseString_)));
     doPost(createEvent(Level.TRACE, logger3, "tracemsg" + i++, null));
     doPost(
        createEvent(
          Level.DEBUG, logger3,
          "debugmsg " + i++
          + " g dg sdfa sadf sdf safd fsda asfd sdfa sdaf asfd asdf fasd fasd adfs fasd adfs fads afds afds afsd afsd afsd afsd afsd fasd asfd asfd afsd fasd afsd",
          null));
     doPost(createEvent(Level.INFO, logger3, "infomsg " + i++, null));
     doPost(createEvent(Level.WARN, logger3, "warnmsg " + i++, null));
     doPost(createEvent(Level.ERROR, logger3, "errormsg " + i++, null));
     doPost(createEvent(Level.FATAL, logger3, "fatalmsg " + i++, new Exception("someexception-" + baseString_)));

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ie) {
      }
    }
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.plugins.Plugin#shutdown()
   */
  public void shutdown() {
    shutdown = true;
  }

  /* (non-Javadoc)
   * @see org.apache.log4j.spi.OptionHandler#activateOptions()
   */
  public void activateOptions() {
    thread = new Thread(this);
    thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }
}
