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

package org.apache.log4j.helpers;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;

/**
 * An extension of the Level class that provides support for java.util.logging
 * Levels.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public final class UtilLoggingLevel {

    /**
     * Numerical value for SEVERE.
     */
    private static final int SEVERE_INT = 22000;
    /**
     * Numerical value for WARNING.
     */
    private static final int WARNING_INT = 21000;

    /**
     * Numerical value for INFO.
     */
    private static final int INFO_INT = 20000;

    /**
     * Numerical value for CONFIG.
     */
    private static final int CONFIG_INT = 14000;

    /**
     * Numerical value for FINE.
     */
    private static final int FINE_INT = 13000;

    /**
     * Numerical value for FINER.
     */
    private static final int FINER_INT = 12000;

    /**
     * Numerical value for FINEST.
     */
    private static final int FINEST_INT = 11000;

    /**
     * SEVERE.
     */
    private static final Level SEVERE =
            Level.forName("SEVERE", SEVERE_INT);
    /**
     * WARNING.
     */
    private static final Level WARNING =
            Level.forName("WARNING", WARNING_INT);
    /**
     * INFO.
     */
    private static final Level INFO =
            Level.forName("INFO", INFO_INT);
    /**
     * CONFIG.
     */
    private static final Level CONFIG =
            Level.forName("CONFIG", CONFIG_INT);
    /**
     * FINE.
     */
    private static final Level FINE =
            Level.forName("FINE", FINE_INT);
    /**
     * FINER.
     */
    private static final Level FINER =
            Level.forName("FINER", FINER_INT);
    /**
     * FINEST.
     */
    private static final Level FINEST =
            Level.forName("FINEST", FINEST_INT);

    /**
     * Get level with specified symbolic name.
     * @param s symbolic name.
     * @return matching level or Level.DEBUG if no match.
     */
    public static Level toLevel(final String s) {
        return toLevel(s, Level.DEBUG);
    }


    /**
     * Get level with specified symbolic name.
     * @param sArg symbolic name.
     * @param defaultLevel level to return if no match.
     * @return matching level or defaultLevel if no match.
     */
    public static Level toLevel(final String sArg,
                                final Level defaultLevel) {
        if (sArg == null) {
            return defaultLevel;
        }

        String s = sArg.toUpperCase();

        if (s.equals("SEVERE")) {
            return SEVERE;
        }

        //if(s.equals("FINE")) return Level.FINE;
        if (s.equals("WARNING")) {
            return WARNING;
        }

        if (s.equals("INFO")) {
            return INFO;
        }

        if (s.equals("CONFI")) {
            return CONFIG;
        }

        if (s.equals("FINE")) {
            return FINE;
        }

        if (s.equals("FINER")) {
            return FINER;
        }

        if (s.equals("FINEST")) {
            return FINEST;
        }
        return defaultLevel;
    }

}
