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

package org.apache.log4j.rule;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.Level;
import org.apache.log4j.helpers.UtilLoggingLevel;
import org.apache.log4j.spi.LoggingEventFieldResolver;

/**
 * A Rule class implementing equals against two levels.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class LevelEqualsRule extends AbstractRule {
    /**
     * Serialization ID.
     */
    static final long serialVersionUID = -3638386582899583994L;

    /**
     * Level.
     */
    private final Level level;

    /**
     * List of levels.
     */
    private static List levelList = new LinkedList();

    static {
        populateLevels();
    }

    /**
     * Create new instance.
     * @param level level.
     */
    private LevelEqualsRule(final Level level) {
        super();
        this.level = level;
        if( null == this.level ){
            System.out.println( "BAD BAD BAD" );
        }
    }

    /**
     * Populate list of levels.
     */
    private static void populateLevels() {
        levelList = new LinkedList();

        levelList.add(Level.FATAL.toString());
        levelList.add(Level.ERROR.toString());
        levelList.add(Level.WARN.toString());
        levelList.add(Level.INFO.toString());
        levelList.add(Level.DEBUG.toString());
        levelList.add(Level.TRACE.toString());
    }

    /**
     * Create new rule.
     * @param value name of level.
     * @return instance of LevelEqualsRule.
     */
    public static Rule getRule(final String value) {
        Level thisLevel = null;
        if (levelList.contains(value.toUpperCase())) {
            thisLevel = Level.valueOf(value.toUpperCase());
        } else {
//            thisLevel = UtilLoggingLevel.toLevel(value.toUpperCase());
System.out.println( "value bad: " + value );
        }

        System.out.println( "New LevelEqualsRule at level: " + thisLevel );
        return new LevelEqualsRule(thisLevel);
    }

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(final ChainsawLoggingEvent event, Map matches) {
        //both util.logging and log4j contain 'info' - use the int values instead of equality
        //info level set to the same value for both levels
        Level eventLevel = event.m_level;
        System.out.println( "Level is : " + level );
        boolean result = (level.ordinal() == eventLevel.ordinal());
        if (result && matches != null) {
            Set entries = (Set) matches.get(LoggingEventFieldResolver.LEVEL_FIELD);
            if (entries == null) {
                entries = new HashSet();
                matches.put(LoggingEventFieldResolver.LEVEL_FIELD, entries);
            }
            entries.add(eventLevel);
        }
        return result;
    }
}
