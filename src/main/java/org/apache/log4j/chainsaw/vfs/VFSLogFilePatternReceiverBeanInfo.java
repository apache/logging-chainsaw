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
package org.apache.log4j.chainsaw.vfs;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo class for the meta-data of the VFSLogFilePatternReceiver.
 */
public class VFSLogFilePatternReceiverBeanInfo extends SimpleBeanInfo {
    /* (non-Javadoc)
     * @see java.beans.BeanInfo#getPropertyDescriptors()
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return new PropertyDescriptor[] {
                new PropertyDescriptor("fileURL", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("timestampFormat", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("logFormat", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("name", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("tailing", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("autoReconnect", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("waitMillis", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("appendNonMatches", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("customLevelDefinitions", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("filterExpression", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("promptForUserInfo", VFSLogFilePatternReceiver.class),
                new PropertyDescriptor("group", VFSLogFilePatternReceiver.class),
            };
        } catch (Exception e) {
        }

        return null;
    }
}
