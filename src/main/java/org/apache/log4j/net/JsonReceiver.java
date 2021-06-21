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

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;
import static org.apache.log4j.net.XMLSocketReceiver.ZONE;
import org.apache.log4j.plugins.Pauseable;
import org.apache.log4j.plugins.Plugin;
import org.apache.log4j.plugins.Receiver;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;

/**
 * The JsonReceiver class receives log events over a TCP socket(as JSON) and
 * turns those into log events.
 *
 * @author Robert Middleton
 */
public class JsonReceiver extends Receiver implements Runnable, PortBased, Pauseable {
    private boolean m_paused;
    //default to log4j xml decoder
    protected String m_decoder = "org.apache.log4j.xml.XMLDecoder";
    private ServerSocket m_serverSocket;
    private List<Socket> m_socketList = new Vector<>();
    private Thread m_rxThread;
    public static final int DEFAULT_PORT = 4449;
    protected int m_port = DEFAULT_PORT;
    private boolean m_advertiseViaMulticastDNS;
    private ZeroConfSupport m_zeroConf;

    /**
     * The MulticastDNS zone advertised by an XMLSocketReceiver
     */
    public static final String ZONE = "_log4j_json_tcpaccept_receiver.local.";

    public JsonReceiver() {
    }

    public JsonReceiver(int _port) {
        m_port = _port;
    }

    public JsonReceiver(int _port, LoggerRepository _repository) {
        m_port = _port;
        repository = _repository;
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

        getLogger().debug("{} doShutdown called", getName());

        // close the server socket
        closeServerSocket();

        // close all of the accepted sockets
        closeAllAcceptedSockets();

        if (m_advertiseViaMulticastDNS) {
            m_zeroConf.unadvertise();
        }
    }

    /**
     * Closes the server socket, if created.
     */
    private void closeServerSocket() {
        getLogger().debug("{} closing server socket", getName());

        try {
            if (m_serverSocket != null) {
                m_serverSocket.close();
            }
        } catch (Exception e) {
            // ignore for now
        }

        m_serverSocket = null;
    }

    /**
     * Closes all the connected sockets in the List.
     */
    private synchronized void closeAllAcceptedSockets() {
        for (Socket sock : m_socketList) {
            try {
                sock.close();
            } catch (Exception e) {
                // ignore for now
            }
        }

        // clear member variables
        m_socketList.clear();
    }

    @Override
    public void activateOptions() {
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
        getLogger().debug("performing socket cleanup prior to entering loop for {}", name);
        closeServerSocket();
        closeAllAcceptedSockets();
        getLogger().debug("socket cleanup complete for {}", name);
        active = true;

        // start the server socket
        try {
            m_serverSocket = new ServerSocket(m_port);
        } catch (Exception e) {
            getLogger().error(
                "error starting JsonReceiver (" + this.getName()
                    + "), receiver did not start", e);
            active = false;
            doShutdown();

            return;
        }

        Socket socket = null;

        try {
            getLogger().debug("in run-about to enter while isactiveloop");

            active = true;

            while (!m_rxThread.isInterrupted()) {
                // if we have a socket, start watching it
                if (socket != null) {
                    getLogger().debug("socket not null - creating and starting socketnode");
                    m_socketList.add(socket);

                    JsonSocketNode node = new JsonSocketNode(socket, this);
                    node.setLoggerRepository(this.repository);
                    new Thread(node).start();
                }

                getLogger().debug("waiting to accept socket");

                // wait for a socket to open, then loop to start it
                socket = m_serverSocket.accept();
                getLogger().debug("accepted socket");
            }

            // socket not watched because we a no longer running
            // so close it now.
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            getLogger().warn(
                "socket server disconnected, stopping");
        }
    }

    @Override
    public int getPort() {
        return m_port;
    }

    @Override
    public void setPaused(boolean paused) {
        m_paused = paused;
    }

    @Override
    public boolean isPaused() {
        return m_paused;
    }

    public boolean isEquivalent(Plugin testPlugin) {
        if ((testPlugin != null) && testPlugin instanceof JsonReceiver) {
            JsonReceiver sReceiver = (JsonReceiver) testPlugin;

            return (m_port == sReceiver.getPort() && super.isEquivalent(testPlugin));
        }

        return false;
    }

    public void setAdvertiseViaMulticastDNS(boolean advertiseViaMulticastDNS) {
        m_advertiseViaMulticastDNS = advertiseViaMulticastDNS;
    }

    public boolean isAdvertiseViaMulticastDNS() {
        return m_advertiseViaMulticastDNS;
    }

    @Override
    public void doPost(LoggingEvent event) {
        if (!isPaused()) {
            super.doPost(event);
        }
    }
}
