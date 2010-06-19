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
package org.apache.log4j.chainsaw;

/**
 * This class is used to in saving and loading the tab settings
 * of Chainsaw....
 */

public class SavableTabSetting {
    private boolean welcome = false;
    private boolean chainsawLog = false;
    private boolean zeroConf = false;
    //not used currently, but leaving it here to prevent xstream exception for older clients
    private boolean dragdrop = false;

    public void setWelcome(boolean welcome) {
        this.welcome = welcome;
    }

    public void setChainsawLog(boolean chainsawLog) {
        this.chainsawLog = chainsawLog;
    }

    public void setZeroconf(boolean zeroConf)
    {
        this.zeroConf = zeroConf;
    }

    public boolean isWelcome() {
        return welcome;
    }

    public boolean isChainsawLog() {
        return chainsawLog;
    }

    public boolean isZeroconf() {
        return zeroConf;
    }
}
