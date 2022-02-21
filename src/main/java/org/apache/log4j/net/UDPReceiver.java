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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;
import org.apache.log4j.chainsaw.ChainsawReceiverSkeleton;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.spi.Decoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Receive LoggingEvents encoded with an XMLLayout, convert the XML data to a
 * LoggingEvent and post the LoggingEvent.
 *
 * @author Scott Deboy &lt;sdeboy@apache.org&gt;
 */
public class UDPReceiver extends ChainsawReceiverSkeleton implements PortBased {
    private static final int PACKET_LENGTH = 16384;
    private UDPReceiverThread receiverThread;
    private String encoding;

    //default to log4j xml decoder
    private String decoder = "org.apache.log4j.xml.XMLDecoder";
    private Decoder decoderImpl;
    private boolean closed = false;
    private int port;
    private DatagramSocket socket;
    private boolean active = true;

    private static final Logger logger = LogManager.getLogger();

    /**
     * The MulticastDNS zone advertised by a UDPReceiver
     */
    public static final String ZONE = "_log4j_xml_udp_receiver.local.";


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * The <b>Encoding</b> option specifies how the bytes are encoded.  If this
     * option is not specified, the system encoding will be used.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns value of the <b>Encoding</b> option.
     */
    public String getEncoding() {
        return encoding;
    }

    public String getDecoder() {
        return decoder;
    }

    public void setDecoder(String decoder) {
        this.decoder = decoder;
    }

    public synchronized void shutdown() {
        if (closed == true) {
            return;
        }
        closed = true;
        active = false;
        // Closing the datagram socket will unblock the UDPReceiverThread if it is
        // was waiting to receive data from the socket.
        if (socket != null) {
            socket.close();
        }

        try {
            if (receiverThread != null) {
                receiverThread.join();
            }
        } catch (InterruptedException ie) {
        }
    }

    @Override
    public void start() {
        try {
            Class c = Class.forName(decoder);
            Object o = c.newInstance();

            if (o instanceof Decoder) {
                this.decoderImpl = (Decoder) o;
            }
        } catch (ClassNotFoundException cnfe) {
            logger.warn("Unable to find decoder", cnfe);
        } catch (IllegalAccessException | InstantiationException iae) {
            logger.warn("Could not construct decoder", iae);
        }

        try {
            socket = new DatagramSocket(port);
            receiverThread = new UDPReceiverThread();
            receiverThread.start();
            active = true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    class UDPReceiverThread extends Thread {
        public UDPReceiverThread() {
            setDaemon(true);
        }

        public void run() {
            byte[] b = new byte[PACKET_LENGTH];
            DatagramPacket p = new DatagramPacket(b, b.length);

            while (!UDPReceiver.this.closed) {
                try {
                    socket.receive(p);

                    //this string constructor which accepts a charset throws an exception if it is
                    //null
                    String data;
                    if (encoding == null) {
                        data = new String(p.getData(), 0, p.getLength());
                    } else {
                        data = new String(p.getData(), 0, p.getLength(), encoding);
                    }

                    List<ChainsawLoggingEvent> v = decoderImpl.decodeEvents(data);
                    for( ChainsawLoggingEvent evt : v ){
                        append(evt);
                    }
                } catch (SocketException se) {
                    //disconnected
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            //LogLog.debug(UDPReceiver.this.getName() + "'s thread is ending.");
        }
    }
}
