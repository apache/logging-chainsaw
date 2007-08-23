package org.apache.log4j.chainsaw;

/**
 * This class is used to in saving and loading the tab settings
 * of Chainsaw....
 */

public class SavableTabSetting {
    private boolean welcome = false;
    private boolean dragdrop = false;
    private boolean chainsawLog = false;

    public void setDragdrop(boolean dragdrop) {
        this.dragdrop = dragdrop;
    }

    public void setWelcome(boolean welcome) {
        this.welcome = welcome;
    }

    public void setChainsawLog(boolean chainsawLog) {
        this.chainsawLog = chainsawLog;
    }

    public boolean isWelcome() {
        return welcome;
    }

    public boolean isDragdrop() {
        return dragdrop;
    }

    public boolean isChainsawLog() {
        return chainsawLog;
    }
}
