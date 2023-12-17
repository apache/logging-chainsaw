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
package org.apache.log4j.chainsaw.osx;

import org.apache.log4j.chainsaw.logui.LogUI;

import java.awt.Desktop;

/**
 * This class leverages the 'Desktop' awt API in order to follow Mac-specific UI guidelines.
 * <p>
 *
 * @author psmith
 * @see "http://developer.apple.com/documentation/Java/index.html"
 */
public class OSXIntegration {
    public static final boolean IS_OSX = System.getProperty("os.name").startsWith("Mac OS X");
    private static final Desktop desktop = Desktop.getDesktop();

    public static void init(final LogUI logUI) {
        if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
            desktop.setAboutHandler(e ->
                logUI.showAboutBox()
            );
        }

        if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
            desktop.setPreferencesHandler(e ->
                logUI.showApplicationPreferences()
            );
        }

        if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
            desktop.setQuitHandler((e, r) -> {
                    if (
                        logUI.exit()) {
                        r.performQuit();
                    } else {
                        r.cancelQuit();
                    }
                }
            );
        }
    }
}
