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
package org.apache.log4j.varia;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import org.apache.log4j.chainsaw.ChainsawReceiver;
import org.apache.log4j.chainsaw.ChainsawReceiverFactory;

/**
 *
 */
public class LogFilePatternReceiverFactory implements ChainsawReceiverFactory {

    @Override
    public ChainsawReceiver create() {
        return new LogFilePatternReceiver();
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() throws IntrospectionException {
        return new PropertyDescriptor[]{
            new PropertyDescriptor("name", LogFilePatternReceiver.class),
            new PropertyDescriptor("fileURL", LogFilePatternReceiver.class),
            new PropertyDescriptor("appendNonMatches", LogFilePatternReceiver.class),
            new PropertyDescriptor("filterExpression", LogFilePatternReceiver.class),
            new PropertyDescriptor("tailing", LogFilePatternReceiver.class),
            new PropertyDescriptor("logFormat", LogFilePatternReceiver.class),
            new PropertyDescriptor("group", LogFilePatternReceiver.class),
            new PropertyDescriptor("timestampFormat", LogFilePatternReceiver.class),
            new PropertyDescriptor("waitMillis", LogFilePatternReceiver.class),
        };
    }

    @Override
    public String getReceiverName() {
        return "LogFilePatternReceiver";
    }
}
