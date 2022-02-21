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

import java.time.Instant;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.log4j.chainsaw.logevents.Level;
import org.apache.log4j.chainsaw.logevents.LocationInfo;


/**
 * Class designed to stress, and/or test the Chainsaw GUI by sending it
 * lots of Logging Events.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class Generator extends ChainsawReceiverSkeleton implements Runnable {
    private static final String logger1 =
        "com.mycompany.mycomponentA";
    private static final String logger2 =
        "com.mycompany.mycomponentB";
    private static final String logger3 =
        "com.someothercompany.corecomponent";
    private final String baseString_;
    private Thread thread;
    private boolean shutdown;
    private ChainsawLoggingEventBuilder m_builder;

    public Generator(String name) {
        setName(name);
        baseString_ = name;
        m_builder = new ChainsawLoggingEventBuilder();
    }

    private ChainsawLoggingEvent createEvent(
        Level level, String logger, String msg, Throwable t) {
        m_builder.clear();
        m_builder.setLogger( logger )
                .setTimestamp(Instant.now())
                .setLevel(level)
                .setMessage(msg)
                .setLocationInfo(new LocationInfo("file", logger, "method", 123))
                .setNDC("NDC Value");
        return m_builder.create();
    }

    public void run() {
//        NDC.push(baseString_);
//        MDC.put("some string", "some value" + baseString_);

        int i = 0;

        while (!shutdown) {
            append(createEvent(Level.TRACE, logger1, "tracemsg" + i++, null));
            append(
                createEvent(
                    Level.DEBUG, logger1,
                    "debugmsg " + i++
                        + " g dg sdfa sadf sdf safd fsda asfd sdfa sdaf asfd asdf fasd fasd adfs fasd adfs fads afds afds afsd afsd afsd afsd afsd fasd asfd asfd afsd fasd afsd",
                    null));
            append(createEvent(Level.INFO, logger1, "infomsg " + i++, null));
            append(createEvent(Level.WARN, logger1, "warnmsg " + i++, null));
            append(createEvent(Level.ERROR, logger1, "errormsg " + i++, null));
            append(createEvent(Level.FATAL, logger1, "fatalmsg " + i++, new Exception("someexception-" + baseString_)));
            append(createEvent(Level.TRACE, logger2, "tracemsg" + i++, null));
            append(
                createEvent(
                    Level.DEBUG, logger2,
                    "debugmsg " + i++
                        + " g dg sdfa sadf sdf safd fsda asfd sdfa sdaf asfd asdf fasd fasd adfs fasd adfs fads afds afds afsd afsd afsd afsd afsd fasd asfd asfd afsd fasd afsd",
                    null));
            append(createEvent(Level.INFO, logger2, "infomsg " + i++, null));
            append(createEvent(Level.WARN, logger2, "warnmsg " + i++, null));
            append(createEvent(Level.ERROR, logger2, "errormsg " + i++, null));
            append(createEvent(Level.FATAL, logger2, "fatalmsg " + i++, new Exception("someexception-" + baseString_)));
            append(createEvent(Level.TRACE, logger3, "tracemsg" + i++, null));
            append(
                createEvent(
                    Level.DEBUG, logger3,
                    "debugmsg " + i++
                        + " g dg sdfa sadf sdf safd fsda asfd sdfa sdaf asfd asdf fasd fasd adfs fasd adfs fads afds afds afsd afsd afsd afsd afsd fasd asfd asfd afsd fasd afsd",
                    null));
            append(createEvent(Level.INFO, logger3, "infomsg " + i++, null));
            append(createEvent(Level.WARN, logger3, "warnmsg " + i++, null));
            append(createEvent(Level.ERROR, logger3, "errormsg " + i++, null));
            append(createEvent(Level.FATAL, logger3, "fatalmsg " + i++, new Exception("someexception-" + baseString_)));

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

    @Override
    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
}
