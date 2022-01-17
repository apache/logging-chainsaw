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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import org.apache.log4j.chainsaw.ChainsawReceiver;
import org.apache.log4j.chainsaw.ChainsawReceiverFactory;

/**
 *
 * @author robert
 */
public class JsonReceiverFactory implements ChainsawReceiverFactory {

    @Override
    public ChainsawReceiver create() {
        return new JsonReceiver();
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() throws IntrospectionException {
        return new PropertyDescriptor[]{
                new PropertyDescriptor("name", JsonReceiver.class)
//                new PropertyDescriptor("address", MulticastReceiver.class),
//                new PropertyDescriptor("port", MulticastReceiver.class),
//                new PropertyDescriptor("threshold", MulticastReceiver.class),
//                new PropertyDescriptor("decoder", MulticastReceiver.class),
//                new PropertyDescriptor("advertiseViaMulticastDNS", MulticastReceiver.class),
            };
    }

    @Override
    public String getReceiverName() {
        return "Json Receiver";
    }
    
}
