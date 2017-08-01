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
package org.apache.log4j.chainsaw.receivers;

import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.chainsaw.receivers.ReceiversHelper;
import org.apache.log4j.chainsaw.vfs.VFSLogFilePatternReceiver;
import org.apache.log4j.net.JMSReceiver;
import org.apache.log4j.net.MulticastReceiver;
import org.apache.log4j.net.SocketHubReceiver;
import org.apache.log4j.net.SocketReceiver;
import org.apache.log4j.net.UDPReceiver;
import org.apache.log4j.net.XMLSocketReceiver;
import org.apache.log4j.db.DBReceiver;
import org.apache.log4j.varia.LogFilePatternReceiver;


/**
 * Test class to ensure that all the know receiver classes can be retrieved and are recognised as
 * valid Classess
 * 
 * @author psmith
 *
 */
public class ReceiversHelperTest extends TestCase {

    public ReceiversHelperTest(String test) {
        super(test);
    }

    public void testKnownReceivers() {

        List list = ReceiversHelper.getInstance().getKnownReceiverClasses();

        Class[] expectedList =
            new Class[] {
                MulticastReceiver.class, 
                SocketHubReceiver.class, 
                SocketReceiver.class,
                UDPReceiver.class, 
                XMLSocketReceiver.class,
                LogFilePatternReceiver.class,
                JMSReceiver.class,
                DBReceiver.class,
                VFSLogFilePatternReceiver.class,
            };

        for (int i = 0; i < expectedList.length; i++) {

            Class c = expectedList[i];
            assertTrue("Should have found class " + c.getName(),
                list.contains(c));
        }
    }
}
