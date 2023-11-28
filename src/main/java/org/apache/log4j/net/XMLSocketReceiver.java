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

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverSkeleton;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.spi.Decoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * XMLSocketReceiver receives a remote logging event via XML on a configured
 * socket and "posts" it to a LoggerRepository as if the event were
 * generated locally. This class is designed to receive events from
 * the XMLSocketAppender class (or classes that send compatible events).
 * <p>
 * This receiver supports log files created using log4j's XMLLayout, as well as java.util.logging
 * XMLFormatter (via the org.apache.log4j.spi.Decoder interface).
 * <p>
 * By default, log4j's XMLLayout is supported (no need to specify a decoder in that case).
 * <p>
 * To configure this receiver to support java.util.logging's XMLFormatter, specify a 'decoder' param
 * of org.apache.log4j.xml.UtilLoggingXMLDecoder.
 * <p>
 * Once the event has been "posted", it will be handled by the
 * appenders currently configured in the LoggerRespository.
 *
 * @author Mark Womack
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class XMLSocketReceiver extends ChainsawReceiverSkeleton implements Runnable, PortBased {
    //default to log4j xml decoder
    protected String decoder = "org.apache.log4j.xml.XMLDecoder";
    private ServerSocket serverSocket;
    private List<Socket> socketList = new Vector<>();
    private Thread rThread;
    public static final int DEFAULT_PORT = 4448;
    protected int port = DEFAULT_PORT;
    private boolean active = false;

    private static final Logger logger = LogManager.getLogger();

    /**
     * The MulticastDNS zone advertised by an XMLSocketReceiver
     */
    public static final String ZONE = "_log4j_xml_tcpaccept_receiver.local.";

    /*
     * Log4j doesn't provide an XMLSocketAppender, but the MulticastDNS zone that should be advertised by one is:
     * _log4j_xml_tcpconnect_appender.local.
     */

    public XMLSocketReceiver() {
    }

    /**
     * Get the port to receive logging events on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port to receive logging events on.
     */
    public void setPort(int _port) {
        port = _port;
    }

    public String getDecoder() {
        return decoder;
    }

    /**
     * Specify the class name implementing org.apache.log4j.spi.Decoder that can process the file.
     */
    public void setDecoder(String _decoder) {
        decoder = _decoder;
    }

    /**
     * Starts the XMLSocketReceiver with the current options.
     */
    public void activateOptions() {
        if (!isActive()) {
            rThread = new Thread(this);
            rThread.setDaemon(true);
            rThread.start();

            active = true;
        }
    }

    /**
     * Called when the receiver should be stopped. Closes the
     * server socket and all of the open sockets.
     */
    @Override
    public synchronized void shutdown() {
        // mark this as no longer running
        active = false;

        if (rThread != null) {
            rThread.interrupt();
            rThread = null;
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

    /**
     * Loop, accepting new socket connections.
     */
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
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            logger.error(
                "error starting XMLSocketReceiver (" + this.getName()
                    + "), receiver did not start", e);
            active = false;
            doShutdown();

            return;
        }

        Socket socket = null;

        try {
            logger.debug("in run-about to enter while isactiveloop");

            active = true;

            while (!rThread.isInterrupted()) {
                // if we have a socket, start watching it
                if (socket != null ) {
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
            logger.warn(
                "socket server disconnected, stopping");
        }
    }

    @Override
    public void start() {
        logger.debug("Starting receiver");
        if (!isActive()) {
            rThread = new Thread(this);
            rThread.setDaemon(true);
            rThread.start();

            active = true;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private void parseIncomingData(Socket sock){
        InputStream is;
        Decoder d = null;
        
        try{
            d = (Decoder) Class.forName(decoder).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("Unable to load correct decoder", e);
            return;
        }

        try {
            is = sock.getInputStream();
        } catch (Exception e) {
            is = null;
            logger.error("Exception opening InputStream to " + sock, e);
            return;
        }

        while (is != null) {
            try{
                byte[] b = new byte[1024];
                int length = is.read(b);
                if (length == -1) {
                    logger.info(
                        "no bytes read from stream - closing connection.");
                    break;
                }
                List<ChainsawLoggingEvent> v = d.decodeEvents(new String(b, 0, length));

                for( ChainsawLoggingEvent evt : v ){
                    append(evt);
                }
            }catch(Exception ex){
                logger.error(ex);
                break;
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
