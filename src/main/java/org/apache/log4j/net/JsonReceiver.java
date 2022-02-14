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
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import org.apache.log4j.chainsaw.ChainsawReceiverSkeleton;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The JsonReceiver class receives log events over a TCP socket(as JSON) and
 * turns those into log events.
 *
 * @author Robert Middleton
 */
public class JsonReceiver extends ChainsawReceiverSkeleton implements Runnable, PortBased {
    private ServerSocket m_serverSocket;
    private Thread m_rxThread;
    public static final int DEFAULT_PORT = 4449;
    protected int m_port = DEFAULT_PORT;
    private boolean m_advertiseViaMulticastDNS;
    private ZeroConfSupport m_zeroConf;
    private boolean active = false;
    
    private static final Logger logger = LogManager.getLogger();

    /**
     * The MulticastDNS zone advertised by an XMLSocketReceiver
     */
    public static final String ZONE = "_log4j_json_tcpaccept_receiver.local.";

    public JsonReceiver() {
    }

    @Override
    public void shutdown() {
        // mark this as no longer running
        active = false;

        if (m_rxThread != null) {
            m_rxThread.interrupt();
            m_rxThread = null;
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

        if (m_advertiseViaMulticastDNS) {
            m_zeroConf.unadvertise();
        }
    }

    /**
     * Closes the server socket, if created.
     */
    private void closeServerSocket() {
        logger.debug("{} closing server socket", getName());

        try {
            if (m_serverSocket != null) {
                m_serverSocket.close();
            }
        } catch (Exception e) {
            // ignore for now
        }

        m_serverSocket = null;
    }

    @Override
    public void start() {
        logger.debug("Starting receiver");
        if (!isActive()) {
            m_rxThread = new Thread(this);
            m_rxThread.setDaemon(true);
            m_rxThread.start();

            if (m_advertiseViaMulticastDNS) {
                m_zeroConf = new ZeroConfSupport(ZONE, m_port, getName());
                m_zeroConf.advertise();
            }

            active = true;
        }
    }

    @Override
    public void run() {
        /**
         * Ensure we start fresh.
         */
        logger.debug("performing socket cleanup prior to entering loop for {}", name);
        closeServerSocket();
        logger.debug("socket cleanup complete for {}", name);
        active = true;

        // start the server socket
        try {
            m_serverSocket = new ServerSocket(m_port, 1);
        } catch (Exception e) {
            logger.error(
                "error starting JsonReceiver (" + this.getName()
                    + "), receiver did not start", e);
            active = false;
            doShutdown();

            return;
        }

        Socket socket = null;

        try {
            logger.debug("in run-about to enter while isactiveloop");

            active = true;

            while (!m_rxThread.isInterrupted()) {
                // if we have a socket, start watching it
                if (socket != null ) {
                    logger.debug("socket not null - parsing data");
                    parseIncomingData(socket);
                }

                logger.debug("waiting to accept socket");

                // wait for a socket to open, then loop to start it
                socket = m_serverSocket.accept();
                logger.debug("accepted socket");
            }

            // socket not watched because we a no longer running
            // so close it now.
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            logger.warn(
                "socket server disconnected, stopping");
        }
    }

    @Override
    public int getPort() {
        return m_port;
    }
    
    public void setPort(int portnum){
        m_port = portnum;
    }

    public void setAdvertiseViaMulticastDNS(boolean advertiseViaMulticastDNS) {
        m_advertiseViaMulticastDNS = advertiseViaMulticastDNS;
    }

    public boolean isAdvertiseViaMulticastDNS() {
        return m_advertiseViaMulticastDNS;
    }

    @Override
    public boolean isActive() {
        return active;
    }
    
    private void parseIncomingData(Socket sock){
        InputStream is;
        
        try {
            is = sock.getInputStream();
        } catch (Exception e) {
            is = null;
            logger.error("Exception opening InputStream to " + sock, e);
            return;
        }

        if (is != null) {
            Genson genson = new GensonBuilder()
                    .useDateAsTimestamp(true)
                    .create();

            try {
                //read data from the socket.
                // Once we have a full JSON message, parse it
                ChainsawLoggingEventBuilder build = new ChainsawLoggingEventBuilder();
                while (true) {
                    logger.debug( "About to deserialize values" );
                    Iterator<ECSLogEvent> iter = genson.deserializeValues(is, ECSLogEvent.class);
                    // Because the socket can be closed, if we don't have anything parsed
                    // assume that the socket is closed.
                    if( !iter.hasNext() ) break;
                    while( iter.hasNext() ){
                        ECSLogEvent evt = iter.next();
                        append(evt.toChainsawLoggingEvent(build));
                    }
                }
            } catch (Exception e) {
                logger.error("Unexpected exception. Closing connection.", e);
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
    }
}
