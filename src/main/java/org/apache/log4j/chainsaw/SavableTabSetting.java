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
