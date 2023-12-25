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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiver;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverFactory;

public class UDPReceiverFactory implements ChainsawReceiverFactory {

    @Override
    public ChainsawReceiver create() {
        return new UDPReceiver();
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() throws IntrospectionException {
        return new PropertyDescriptor[] {
            new PropertyDescriptor("name", UDPReceiver.class),
            new PropertyDescriptor("port", UDPReceiver.class),
            new PropertyDescriptor("encoding", UDPReceiver.class),
            new PropertyDescriptor("decoder", UDPReceiver.class),
        };
    }

    @Override
    public String getReceiverName() {
        return "UDPReceiver";
    }

    @Override
    public String getReceiverDocumentation() {
        return "<html>The UDP recevier has the following parameters:<br/>" + "<ul>"
                + "<li>port - the port to listen on for incoming data on all interfaces</li>"
                + "<li>encoding - the encoding of the data(e.g. UTF-8)</li>"
                + "<li>decoder - the specific decoder to use to decode the data.  Only XML decoding is built-in</li>"
                + "</ul>"
                + "</html>";
    }
}
