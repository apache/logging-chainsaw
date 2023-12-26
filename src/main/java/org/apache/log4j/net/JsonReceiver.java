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
package org.apache.log4j.net;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverSkeleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The JsonReceiver class receives log events over a TCP socket(as JSON) and
 * turns those into log events.
 *
 * @author Robert Middleton
 */
public class JsonReceiver extends ChainsawReceiverSkeleton implements Runnable, PortBased {
    private static final Logger logger = LogManager.getLogger(JsonReceiver.class);

    private ServerSocket serverSocket;
    private Thread rxThread;
    public static final int DEFAULT_PORT = 4449;
    protected int port = DEFAULT_PORT;
    private boolean active = false;

    /**
     * The MulticastDNS zone advertised by an XMLSocketReceiver
     */
    public static final String ZONE = "_log4j_json_tcpaccept_receiver.local.";

    @Override
    public void shutdown() {
        // mark this as no longer running
        active = false;

        if (rxThread != null) {
            rxThread.interrupt();
            rxThread = null;
        }
        doShutdown();
    }

    /**
     * Does the actual shutting down by closing the server socket
     * and any connected sockets that have been created.
     */
    private synchronized void doShutdown() {
        active = false;

        logger.debug("{} doShutdown called", getName());

        // close the server socket
        closeServerSocket();
    }

    /**
     * Closes the server socket, if created.
     */
    private void closeServerSocket() {
        logger.debug("{} closing server socket", getName());

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
            // ignore for now
        }

        serverSocket = null;
    }

    @Override
    public void start() {
        logger.debug("Starting receiver");
        if (!isActive()) {
            rxThread = new Thread(this);
            rxThread.setDaemon(true);
            rxThread.start();

            active = true;
        }
    }

    @Override
    @SuppressFBWarnings
    public void run() {
        logger.debug("performing socket cleanup prior to entering loop for {}", name);
        /* Ensure we start fresh. */
        closeServerSocket();
        logger.debug("socket cleanup complete for {}", name);
        active = true;

        // start the server socket
        try {
            serverSocket = new ServerSocket(port, 1);
        } catch (Exception e) {
            logger.error("error starting JsonReceiver ({}), receiver did not start", this.getName());
            logger.error(e, e);
            active = false;
            doShutdown();

            return;
        }

        Socket socket = null;

        try {
            logger.debug("in run-about to enter while isactiveloop");

            while (!rxThread.isInterrupted()) {
                // if we have a socket, start watching it
                if (socket != null) {
                    logger.debug("socket not null - parsing data");
                    parseIncomingData(socket);
                }

                logger.debug("waiting to accept socket");

                // wait for a socket to open, then loop to start it
                socket = serverSocket.accept();
                logger.debug("accepted socket");
            }

            // socket not watched because we a no longer running
            // so close it now.
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            logger.warn("socket server disconnected, stopping");
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int portnum) {
        port = portnum;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private void parseIncomingData(Socket sock) {
        InputStream is;

        try {
            is = sock.getInputStream();
        } catch (Exception e) {
            logger.error("Exception opening InputStream to {}", sock);
            logger.error(e, e);
            return;
        }

        if (is != null) {
            Genson genson = new GensonBuilder().useDateAsTimestamp(true).create();

            try {
                // read data from the socket.
                // Once we have a full JSON message, parse it
                ChainsawLoggingEventBuilder build = new ChainsawLoggingEventBuilder();
                while (true) {
                    logger.debug("About to deserialize values");
                    Iterator<ECSLogEvent> iter = genson.deserializeValues(is, ECSLogEvent.class);
                    // Because the socket can be closed, if we don't have anything parsed
                    // assume that the socket is closed.
                    if (!iter.hasNext()) break;

                    while (iter.hasNext()) {
                        ECSLogEvent evt = iter.next();
                        append(evt.toChainsawLoggingEvent(build));
                    }
                }
            } catch (Exception e) {
                logger.error("Unexpected exception. Closing connection.", e);
            }
        }

        try {
            if (is != null) {
                is.close();
            }
        } catch (Exception e) {
            logger.error("Could not close connection.", e);
        }
    }
}
