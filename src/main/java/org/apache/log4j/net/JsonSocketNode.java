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

package org.apache.log4j.net;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.stream.ObjectReader;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.ComponentBase;
import org.apache.log4j.spi.Decoder;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;


/**
 * Read {@link LoggingEvent} objects sent from a remote client using JSON over
 * Sockets (TCP). These logging events are logged according to local
 * policy, as if they were generated locally.
 */
public class JsonSocketNode extends ComponentBase implements Runnable {
    Socket m_socket;
    Receiver m_receiver;
    SocketNodeEventListener m_listener;
    private List<Byte> m_jsonBuffer;

    /**
     * Constructor for socket and logger repository.
     */
    public JsonSocketNode(
        Socket socket, LoggerRepository hierarchy) {
        this.repository = hierarchy;

        this.m_socket = socket;
    }

    /**
     * Constructor for socket and reciever.
     */
    public JsonSocketNode(Socket socket, Receiver receiver) {
        this.m_socket = socket;
        this.m_receiver = receiver;
    }

    /**
     * Set the event listener on this node.
     */
    public void setListener(SocketNodeEventListener _listener) {
        m_listener = _listener;
    }

    public void run() {
        Logger remoteLogger;
        Exception listenerException = null;
        InputStream is;

        if ((this.m_receiver == null) ) {
            listenerException =
                new Exception(
                    "No receiver provided.  Cannot process JSON socket events");
            getLogger().error(
                "Exception constructing JSON Socket Receiver", listenerException);
        }

        m_jsonBuffer = new ArrayList<>( 8192 );

        try {
            is = m_socket.getInputStream();
        } catch (Exception e) {
            is = null;
            listenerException = e;
            getLogger().error("Exception opening InputStream to " + m_socket, e);
        }

        if (is != null) {
            String hostName = m_socket.getInetAddress().getHostName();
            String remoteInfo = hostName + ":" + m_socket.getPort();
            Genson genson = new GensonBuilder()
                    .useDateAsTimestamp(true)
                    .create();

            try {
                //read data from the socket.
                // Once we have a full JSON message, parse it
                while (true) {
                    getLogger().debug( "About to deserialize values" );
                    Iterator<ECSLogEvent> iter = genson.deserializeValues(is, ECSLogEvent.class);
                    // Because the socket can be closed, if we don't have anything parsed
                    // assume that the socket is closed.
                    if( !iter.hasNext() ) break;
                    while( iter.hasNext() ){
                        ECSLogEvent evt = iter.next();
                        LoggingEvent e = evt.toLoggingEvent();
                        e.setProperty(Constants.HOSTNAME_KEY, hostName);

                        // store the known remote info in an event property
                        e.setProperty("log4j.remoteSourceInfo", remoteInfo);

                        // if configured with a receiver, tell it to post the event
                        if (m_receiver != null) {
                            m_receiver.doPost(e);

                            // else post it via the hierarchy
                        } else {
                            // get a logger from the hierarchy. The name of the logger
                            // is taken to be the name contained in the event.
                            remoteLogger = repository.getLogger(e.getLoggerName());

                            //event.logger = remoteLogger;
                            // apply the logger-level filter
                            if (
                                e.getLevel().isGreaterOrEqual(
                                    remoteLogger.getEffectiveLevel())) {
                                // finally log the event as if was generated locally
                                remoteLogger.callAppenders(e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().error("Unexpected exception. Closing connection.", e);
                listenerException = e;
            }
        }

        // close the socket
        try {
            if (is != null) {
                is.close();
            }
        } catch (Exception e) {
            //logger.info("Could not close connection.", e);
        }

        // send event to listener, if configured
        if (m_listener != null) {
            m_listener.socketClosedEvent(listenerException);
        }
    }
}
