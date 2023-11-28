package org.apache.log4j.chainsaw.components.splash;

import org.apache.log4j.chainsaw.helper.SwingHelper;

import java.awt.*;

public class SplashViewer {
    private ChainsawSplash splash;

    public void showSplash(Frame owner) {
        splash = new ChainsawSplash(owner);
        SwingHelper.centerOnScreen(splash);
        splash.setVisible(true);
    }

    public void removeSplash() {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
        }
    }
}
