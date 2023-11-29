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

import java.nio.charset.StandardCharsets;
import org.apache.log4j.helpers.Constants;
import org.apache.log4j.rule.ExpressionRule;
import org.apache.log4j.rule.Rule;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.log4j.chainsaw.receiver.ChainsawReceiverSkeleton;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEvent;
import org.apache.log4j.chainsaw.logevents.ChainsawLoggingEventBuilder;
import org.apache.log4j.chainsaw.logevents.Level;
import org.apache.log4j.chainsaw.logevents.LocationInfo;
import org.apache.logging.log4j.LogManager;

/**
 * LogFilePatternReceiver can parse and tail log files, converting entries into
 * LoggingEvents.  If the file doesn't exist when the receiver is initialized, the
 * receiver will look for the file once every 10 seconds.
 * <p>
 * This receiver relies on java.util.regex features to perform the parsing of text in the
 * log file, however the only regular expression field explicitly supported is
 * a glob-style wildcard used to ignore fields in the log file if needed.  All other
 * fields are parsed by using the supplied keywords.
 * <p>
 * <b>Features:</b><br>
 * - specify the URL of the log file to be processed<br>
 * - specify the timestamp format in the file (if one exists, using patterns from {@link java.text.SimpleDateFormat})<br>
 * - specify the pattern (logFormat) used in the log file using keywords, a wildcard character (*) and fixed text<br>
 * - 'tail' the file (allows the contents of the file to be continually read and new events processed)<br>
 * - supports the parsing of multi-line messages and exceptions
 * - 'hostname' property set to URL host (or 'file' if not available)
 * - 'application' property set to URL path (or value of fileURL if not available)
 * - 'group' property can be set to associate multiple log file receivers
 * <p>
 * <b>Keywords:</b><br>
 * TIMESTAMP<br>
 * LOGGER<br>
 * LEVEL<br>
 * THREAD<br>
 * CLASS<br>
 * FILE<br>
 * LINE<br>
 * METHOD<br>
 * RELATIVETIME<br>
 * MESSAGE<br>
 * NDC<br>
 * PROP(key)<br>
 * (NL)<br>
 * <p>
 * (NL) represents a new line embedded in the log format, supporting log formats whose fields span multiple lines
 * <p>
 * Use a * to ignore portions of the log format that should be ignored
 * <p>
 * Example:<br>
 * If your file's patternlayout is this:<br>
 * <b>%d %-5p [%t] %C{2} (%F:%L) - %m%n</b>
 * <p>
 * specify this as the log format:<br>
 * <b>TIMESTAMP LEVEL [THREAD] CLASS (FILE:LINE) - MESSAGE</b>
 * <p>
 * To define a PROPERTY field, use PROP(key)
 * <p>
 * Example:<br>
 * If you used the RELATIVETIME pattern layout character in the file,
 * you can use PROP(RELATIVETIME) in the logFormat definition to assign
 * the RELATIVETIME field as a property on the event.
 * <p>
 * If your file's patternlayout is this:<br>
 * <b>%r [%t] %-5p %c %x - %m%n</b>
 * <p>
 * specify this as the log format:<br>
 * <b>PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE</b>
 * <p>
 * Note the * - it can be used to ignore a single word or sequence of words in the log file
 * (in order for the wildcard to ignore a sequence of words, the text being ignored must be
 * followed by some delimiter, like '-' or '[') - ndc is being ignored in the following example.
 * <p>
 * Assign a filterExpression in order to only process events which match a filter.
 * If a filterExpression is not assigned, all events are processed.
 * <p>
 * <b>Limitations:</b><br>
 * - no support for the single-line version of throwable supported by patternlayout<br>
 * (this version of throwable will be included as the last line of the message)<br>
 * - the relativetime patternLayout character must be set as a property: PROP(RELATIVETIME)<br>
 * - messages should appear as the last field of the logFormat because the variability in message content<br>
 * - exceptions are converted if the exception stack trace (other than the first line of the exception)<br>
 * is stored in the log file with a tab followed by the word 'at' as the first characters in the line<br>
 * - tailing may fail if the file rolls over.
 * <p>
 * <b>Example receiver configuration settings</b> (add these as params, specifying a LogFilePatternReceiver 'plugin'):<br>
 * param: "timestampFormat" value="yyyy-MM-d HH:mm:ss,SSS"<br>
 * param: "logFormat" value="PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE"<br>
 * param: "fileURL" value="file:///c:/events.log"<br>
 * param: "tailing" value="true"
 * <p>
 * This configuration will be able to process these sample events:<br>
 * 710    [       Thread-0] DEBUG                   first.logger first - &lt;test&gt;   &lt;test2&gt;something here&lt;/test2&gt;   &lt;test3 blah=something/&gt;   &lt;test4&gt;       &lt;test5&gt;something else&lt;/test5&gt;   &lt;/test4&gt;&lt;/test&gt;<br>
 * 880    [       Thread-2] DEBUG                   first.logger third - &lt;test&gt;   &lt;test2&gt;something here&lt;/test2&gt;   &lt;test3 blah=something/&gt;   &lt;test4&gt;       &lt;test5&gt;something else&lt;/test5&gt;   &lt;/test4&gt;&lt;/test&gt;<br>
 * 880    [       Thread-0] INFO                    first.logger first - infomsg-0<br>
 * java.lang.Exception: someexception-first<br>
 * at Generator2.run(Generator2.java:102)<br>
 *
 * @author Scott Deboy
 */
public class LogFilePatternReceiver extends ChainsawReceiverSkeleton {
    private final List<String> keywords = new ArrayList<>();

    private static final String PROP_START = "PROP(";
    private static final String PROP_END = ")";

    private static final String LOGGER = "LOGGER";
    private static final String MESSAGE = "MESSAGE";
    private static final String TIMESTAMP = "TIMESTAMP";
    private static final String NDC = "NDC";
    private static final String LEVEL = "LEVEL";
    private static final String THREAD = "THREAD";
    private static final String CLASS = "CLASS";
    private static final String FILE = "FILE";
    private static final String LINE = "LINE";
    private static final String METHOD = "METHOD";
    private static final String NEWLINE = "(NL)";

    private static final String DEFAULT_HOST = "file";

    //all lines other than first line of exception begin with tab followed by 'at' followed by text
    private static final String EXCEPTION_PATTERN = "^\\s+at.*";
    private static final String REGEXP_DEFAULT_WILDCARD = ".*?";
    private static final String REGEXP_GREEDY_WILDCARD = ".*";
    private static final String PATTERN_WILDCARD = "*";
    //pull in optional leading and trailing spaces
    private static final String NOSPACE_GROUP = "(\\s*?\\S*?\\s*?)";
    private static final String DEFAULT_GROUP = "(" + REGEXP_DEFAULT_WILDCARD + ")";
    private static final String GREEDY_GROUP = "(" + REGEXP_GREEDY_WILDCARD + ")";
    private static final String MULTIPLE_SPACES_REGEXP = "[ ]+";
    private static final String NEWLINE_REGEXP = "\n";
    private final String newLine = System.getProperty("line.separator");

    private final String[] emptyException = new String[]{""};

    private SimpleDateFormat dateFormat;
    private String timestampFormat;
    private String logFormat;
    private String customLevelDefinitions;
    private String fileURL;
    private String host;
    private String path;
    private boolean tailing;
    private String filterExpression;
    private long waitMillis = 2000; //default 2 seconds
    private String group;

    private static final String VALID_DATEFORMAT_CHARS = "GyYMwWDdFEuaHkKhmsSzZX";
    private static final String VALID_DATEFORMAT_CHAR_PATTERN = "[" + VALID_DATEFORMAT_CHARS + "]";

    private Rule expressionRule;

    private Map<String, String> currentMap;
    private List<String> additionalLines;
    private List<String> matchingKeywords;

    private String regexp;
    private Reader reader;
    private Pattern regexpPattern;
    private Pattern exceptionPattern;
    private String timestampPatternText;

    private boolean useCurrentThread;
    public static final int MISSING_FILE_RETRY_MILLIS = 10000;
    private boolean appendNonMatches;
    private final Map<String, Level> customLevelDefinitionMap = new HashMap<>();

    //default to one line - this number is incremented for each (NL) found in the logFormat
    private int lineCount = 1;

    protected boolean active = false;

    private final ChainsawLoggingEventBuilder build = new ChainsawLoggingEventBuilder();

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    public LogFilePatternReceiver() {
        keywords.add(TIMESTAMP);
        keywords.add(LOGGER);
        keywords.add(LEVEL);
        keywords.add(THREAD);
        keywords.add(CLASS);
        keywords.add(FILE);
        keywords.add(LINE);
        keywords.add(METHOD);
        keywords.add(MESSAGE);
        keywords.add(NDC);
        try {
            exceptionPattern = Pattern.compile(EXCEPTION_PATTERN);
        } catch (PatternSyntaxException pse) {
            //shouldn't happen
        }
    }

    /**
     * Accessor
     *
     * @return file URL
     */
    public String getFileURL() {
        return fileURL;
    }

    /**
     * Mutator
     *
     * @param fileURL
     */
    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    /**
     * If the log file contains non-log4j level strings, they can be mapped to log4j levels using the format (android example):
     * V=TRACE,D=DEBUG,I=INFO,W=WARN,E=ERROR,F=FATAL,S=OFF
     *
     * @param customLevelDefinitions the level definition string
     */
    public void setCustomLevelDefinitions(String customLevelDefinitions) {
        this.customLevelDefinitions = customLevelDefinitions;
    }

    public String getCustomLevelDefinitions() {
        return customLevelDefinitions;
    }

    /**
     * Accessor
     *
     * @return append non matches
     */
    public boolean isAppendNonMatches() {
        return appendNonMatches;
    }

    /**
     * Mutator
     *
     * @param appendNonMatches
     */
    public void setAppendNonMatches(boolean appendNonMatches) {
        this.appendNonMatches = appendNonMatches;
    }

    /**
     * Accessor
     *
     * @return filter expression
     */
    public String getFilterExpression() {
        return filterExpression;
    }

    /**
     * Mutator
     *
     * @param filterExpression
     */
    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    /**
     * Accessor
     *
     * @return tailing
     */
    public boolean isTailing() {
        return tailing;
    }

    /**
     * Mutator
     *
     * @param tailing
     */
    public void setTailing(boolean tailing) {
        this.tailing = tailing;
    }

    /**
     * When true, this property uses the current Thread to perform the import,
     * otherwise when false (the default), a new Thread is created and started to manage
     * the import.
     *
     * @return
     */
    public final boolean isUseCurrentThread() {
        return useCurrentThread;
    }

    /**
     * Sets whether the current Thread or a new Thread is created to perform the import,
     * the default being false (new Thread created).
     *
     * @param useCurrentThread
     */
    public final void setUseCurrentThread(boolean useCurrentThread) {
        this.useCurrentThread = useCurrentThread;
    }

    /**
     * Accessor
     *
     * @return log format
     */
    public String getLogFormat() {
        return logFormat;
    }

    /**
     * Mutator
     *
     * @param logFormat the format
     */
    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    /**
     * Mutator
     */
    public void setGroup(String group) {
        this.group = group;
    }


    /**
     * Accessor
     *
     * @return group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Mutator.  Specify a pattern from {@link java.text.SimpleDateFormat}
     *
     * @param timestampFormat
     */
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    /**
     * Accessor
     *
     * @return timestamp format
     */
    public String getTimestampFormat() {
        return timestampFormat;
    }

    /**
     * Accessor
     *
     * @return millis between retrieves of content
     */
    public long getWaitMillis() {
        return waitMillis;
    }

    /**
     * Mutator
     *
     * @param waitMillis
     */
    public void setWaitMillis(long waitMillis) {
        this.waitMillis = waitMillis;
    }

    /**
     * Walk the additionalLines list, looking for the EXCEPTION_PATTERN.
     * <p>
     * Return the index of the first matched line
     * (the match may be the 1st line of an exception)
     * <p>
     * Assumptions: <br>
     * - the additionalLines list may contain both message and exception lines<br>
     * - message lines are added to the additionalLines list and then
     * exception lines (all message lines occur in the list prior to all
     * exception lines)
     *
     * @return -1 if no exception line exists, line number otherwise
     */
    private int getExceptionLine() {
        for (int i = 0; i < additionalLines.size(); i++) {
            Matcher exceptionMatcher = exceptionPattern.matcher(additionalLines.get(i));
            if (exceptionMatcher.matches()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Combine all message lines occuring in the additionalLines list, adding
     * a newline character between each line
     * <p>
     * the event will already have a message - combine this message
     * with the message lines in the additionalLines list
     * (all entries prior to the exceptionLine index)
     *
     * @param firstMessageLine primary message line
     * @param exceptionLine    index of first exception line
     * @return message
     */
    private String buildMessage(String firstMessageLine, int exceptionLine) {
        if (additionalLines.isEmpty()) {
            return firstMessageLine;
        }
        StringBuilder message = new StringBuilder();
        if (firstMessageLine != null) {
            message.append(firstMessageLine);
        }

        int linesToProcess = (exceptionLine == -1 ? additionalLines.size() : exceptionLine);

        for (int i = 0; i < linesToProcess; i++) {
            message.append(newLine);
            message.append(additionalLines.get(i));
        }
        return message.toString();
    }

    /**
     * Combine all exception lines occuring in the additionalLines list into a
     * String array
     * <p>
     * (all entries equal to or greater than the exceptionLine index)
     *
     * @param exceptionLine index of first exception line
     * @return exception
     */
    private String[] buildException(int exceptionLine) {
        if (exceptionLine == -1) {
            return emptyException;
        }
        String[] exception = new String[additionalLines.size() - exceptionLine - 1];
        for (int i = 0; i < exception.length; i++) {
            exception[i] = additionalLines.get(i + exceptionLine);
        }
        return exception;
    }

    /**
     * Construct a logging event from currentMap and additionalLines
     * (additionalLines contains multiple message lines and any exception lines)
     * <p>
     * CurrentMap and additionalLines are cleared in the process
     *
     * @return event
     */
    private ChainsawLoggingEvent buildEvent() {
        if (currentMap.isEmpty()) {
            if (!additionalLines.isEmpty()) {
                for (Object additionalLine : additionalLines) {
                    logger.debug("found non-matching line: " + additionalLine);
                }
            }
            additionalLines.clear();
            return null;
        }
        //the current map contains fields - build an event
        int exceptionLine = getExceptionLine();
        String[] exception = buildException(exceptionLine);

        //messages are listed before exceptions in additionallines
        if (!additionalLines.isEmpty() && exception.length > 0) {
            currentMap.put(MESSAGE, buildMessage(currentMap.get(MESSAGE),
                exceptionLine));
        }
        ChainsawLoggingEvent event = convertToEvent(currentMap, exception);
        currentMap.clear();
        additionalLines.clear();
        return event;
    }

    /**
     * Read, parse and optionally tail the log file, converting entries into logging events.
     * <p>
     * A runtimeException is thrown if the logFormat pattern is malformed.
     *
     * @param bufferedReader
     * @throws IOException
     */
    protected void process(BufferedReader bufferedReader) throws IOException {
        Matcher eventMatcher;
        Matcher exceptionMatcher;
        String readLine;
        //if newlines are provided in the logFormat - (NL) - combine the lines prior to matching
        while ((readLine = bufferedReader.readLine()) != null) {
            StringBuilder line = new StringBuilder(readLine);
            //there is already one line (read above, start i at 1
            for (int i = 1; i < lineCount; i++) {
                String thisLine = bufferedReader.readLine();
                if (thisLine != null) {
                    line.append(newLine).append(thisLine);
                }
            }
            String input = line.toString();
            eventMatcher = regexpPattern.matcher(input);
            //skip empty line entries
            if (input.trim().isEmpty()) {
                continue;
            }
            exceptionMatcher = exceptionPattern.matcher(input);
            if (eventMatcher.matches()) {
                //build an event from the previous match (held in current map)
                ChainsawLoggingEvent event = buildEvent();
                if (event != null) {
                    if (passesExpression(event)) {
                        append(event);
                    }
                }
                currentMap.putAll(processEvent(eventMatcher.toMatchResult()));
            } else if (exceptionMatcher.matches()) {
                //an exception line
                additionalLines.add(input);
            } else {
                //neither...either post an event with the line or append as additional lines
                //if this was a logging event with multiple lines, each line will show up as its own event instead of being
                //appended as multiple lines on the same event..
                //choice is to have each non-matching line show up as its own line, or append them all to a previous event
                if (appendNonMatches) {
                    //hold on to the previous time, so we can do our best to preserve time-based ordering if the event is a non-match
                    String lastTime = (String) currentMap.get(TIMESTAMP);
                    //build an event from the previous match (held in current map)
                    if (currentMap.size() > 0) {
                        ChainsawLoggingEvent event = buildEvent();
                        if (event != null) {
                            if (passesExpression(event)) {
                                append(event);
                            }
                        }
                    }
                    if (lastTime != null) {
                        currentMap.put(TIMESTAMP, lastTime);
                    }
                    currentMap.put(MESSAGE, input);
                } else {
                    additionalLines.add(input);
                }
            }
        }

        //process last event if one exists
        ChainsawLoggingEvent event = buildEvent();
        if (event != null) {
            if (passesExpression(event)) {
                append(event);
            }
        }
    }

    protected void createPattern() {
        regexpPattern = Pattern.compile(regexp);
    }

    /**
     * Helper method that supports the evaluation of the expression
     *
     * @param event
     * @return true if expression isn't set, or the result of the evaluation otherwise
     */
    private boolean passesExpression(ChainsawLoggingEvent event) {
        if (event != null) {
            if (expressionRule != null) {
//                return (expressionRule.evaluate(event, null));
            }
        }
        return true;
    }

    /**
     * Convert the match into a map.
     * <p>
     * Relies on the fact that the matchingKeywords list is in the same
     * order as the groups in the regular expression
     *
     * @param result
     * @return map
     */
    private Map<String, String> processEvent(MatchResult result) {
        Map<String, String> map = new HashMap<>();
        //group zero is the entire match - process all other groups
        for (int i = 1; i < result.groupCount() + 1; i++) {
            String key = matchingKeywords.get(i - 1);
            String value = result.group(i);
            map.put(key, value);
        }
        return map;
    }

    /**
     * Helper method that will convert timestamp format to a pattern
     *
     * @return string
     */
    private String convertTimestamp() {
        //some locales (for example, French) generate timestamp text with characters not included in \w -
        // now using \S (all non-whitespace characters) instead of /w
        String result = "";
        if (timestampFormat != null) {
            result = timestampFormat.replaceAll(Pattern.quote("+"), "[+]");
            result = result.replaceAll(VALID_DATEFORMAT_CHAR_PATTERN, "\\\\S+");
            //make sure dots in timestamp are escaped
            result = result.replaceAll(Pattern.quote("."), "\\\\.");
        }
        return result;
    }

    protected void setHost(String host) {
        this.host = host;
    }

    protected void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    /**
     * Build the regular expression needed to parse log entries
     */
    protected void initialize() {
        if (host == null && path == null) {
            try {
                URL url = new URL(fileURL);
                host = url.getHost();
                path = url.getPath();
            } catch (MalformedURLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        if (host == null || host.trim().isEmpty()) {
            host = DEFAULT_HOST;
        }
        if (path == null || path.trim().isEmpty()) {
            path = fileURL;
        }

        currentMap = new HashMap<>();
        additionalLines = new ArrayList<>();
        matchingKeywords = new ArrayList<>();

        if (timestampFormat != null) {
            dateFormat = new SimpleDateFormat(quoteTimeStampChars(timestampFormat));
            timestampPatternText = convertTimestamp();
        }
        //if custom level definitions exist, parse them
        updateCustomLevelDefinitionMap();
        try {
            if (filterExpression != null) {
                expressionRule = ExpressionRule.getRule(filterExpression);
            }
        } catch (Exception e) {
            logger.warn("Invalid filter expression: " + filterExpression, e);
        }

        List<String> buildingKeywords = new ArrayList<>();

        String newPattern = logFormat;

        //process newlines - (NL) - in the logFormat - before processing properties
        int index = 0;
        while (index > -1) {
            index = newPattern.indexOf(NEWLINE);
            if (index > -1) {
                //keep track of number of expected newlines in the format, so the lines can be concatenated prior to matching
                lineCount++;
                newPattern = singleReplace(newPattern, NEWLINE, NEWLINE_REGEXP);
            }
        }

        String current = newPattern;
        //build a list of property names and temporarily replace the property with an empty string,
        //we'll rebuild the pattern later
        List<String> propertyNames = new ArrayList<>();
        index = 0;
        while (index > -1) {
            if (current.contains(PROP_START) && current.contains(PROP_END)) {
                index = current.indexOf(PROP_START);
                String longPropertyName = current.substring(current.indexOf(PROP_START), current.indexOf(PROP_END) + 1);
                String shortProp = getShortPropertyName(longPropertyName);
                buildingKeywords.add(shortProp);
                propertyNames.add(longPropertyName);
                current = current.substring(longPropertyName.length() + 1 + index);
                newPattern = singleReplace(newPattern, longPropertyName, Integer.toString(buildingKeywords.size() - 1));
            } else {
                //no properties
                index = -1;
            }
        }

        /*
         * we're using a treemap, so the index will be used as the key to ensure
         * keywords are ordered correctly
         *
         * examine pattern, adding keywords to an index-based map patterns can
         * contain only one of these per entry...properties are the only 'keyword'
         * that can occur multiple times in an entry
         */
        for (String keyword1 : keywords) {
            int index2 = newPattern.indexOf(keyword1);
            if (index2 > -1) {
                buildingKeywords.add(keyword1);
                newPattern = singleReplace(newPattern, keyword1, Integer.toString(buildingKeywords.size() - 1));
            }
        }

        StringBuilder buildingInt = new StringBuilder();

        for (int i = 0; i < newPattern.length(); i++) {
            String thisValue = newPattern.substring(i, i + 1);
            if (isInteger(thisValue)) {
                buildingInt.append(thisValue);
            } else {
                String stringInt = buildingInt.toString();
                if (isInteger(stringInt)) {
                    matchingKeywords.add(buildingKeywords.get(Integer.parseInt(stringInt)));
                }
                //reset
                buildingInt.setLength(0);
            }
        }

        //if the very last value is an int, make sure to add it
        String stringInt = buildingInt.toString();
        if (isInteger(stringInt)) {
            matchingKeywords.add(buildingKeywords.get(Integer.parseInt(stringInt)));
        }

        newPattern = replaceMetaChars(newPattern);

        //compress one or more spaces in the pattern into the [ ]+ regexp
        //(supports padding of level in log files)
        newPattern = newPattern.replaceAll(MULTIPLE_SPACES_REGEXP, MULTIPLE_SPACES_REGEXP);
        newPattern = newPattern.replaceAll(Pattern.quote(PATTERN_WILDCARD), REGEXP_DEFAULT_WILDCARD);
        //use buildingKeywords here to ensure correct order
        for (int i = 0; i < buildingKeywords.size(); i++) {
            String keyword = buildingKeywords.get(i);
            //make the final keyword greedy (we're assuming it's the message)
            if (i == (buildingKeywords.size() - 1)) {
                newPattern = singleReplace(newPattern, String.valueOf(i), GREEDY_GROUP);
            } else if (TIMESTAMP.equals(keyword)) {
                newPattern = singleReplace(newPattern, String.valueOf(i), "(" + timestampPatternText + ")");
            } else if (LOGGER.equals(keyword) || LEVEL.equals(keyword)) {
                newPattern = singleReplace(newPattern, String.valueOf(i), NOSPACE_GROUP);
            } else {
                newPattern = singleReplace(newPattern, String.valueOf(i), DEFAULT_GROUP);
            }
        }

        regexp = newPattern;
        logger.debug("regexp is " + regexp);
    }

    private void updateCustomLevelDefinitionMap() {
        if (customLevelDefinitions != null) {
            StringTokenizer entryTokenizer = new StringTokenizer(customLevelDefinitions, ",");

            customLevelDefinitionMap.clear();
            while (entryTokenizer.hasMoreTokens()) {
                StringTokenizer innerTokenizer = new StringTokenizer(entryTokenizer.nextToken(), "=");
                customLevelDefinitionMap.put(innerTokenizer.nextToken(), Level.valueOf(innerTokenizer.nextToken()));
            }
        }
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private String quoteTimeStampChars(String input) {
        //put single quotes around text that isn't a supported dateformat char
        StringBuilder result = new StringBuilder();
        //ok to default to false because we also check for index zero below
        boolean lastCharIsDateFormat = false;
        for (int i = 0; i < input.length(); i++) {
            String thisVal = input.substring(i, i + 1);
            boolean thisCharIsDateFormat = VALID_DATEFORMAT_CHARS.contains(thisVal);
            //we have encountered a non-dateformat char
            if (!thisCharIsDateFormat && (i == 0 || lastCharIsDateFormat)) {
                result.append("'");
            }
            //we have encountered a dateformat char after previously encountering a non-dateformat char
            if (thisCharIsDateFormat && i > 0 && !lastCharIsDateFormat) {
                result.append("'");
            }
            lastCharIsDateFormat = thisCharIsDateFormat;
            result.append(thisVal);
        }
        //append an end single-quote if we ended with non-dateformat char
        if (!lastCharIsDateFormat) {
            result.append("'");
        }
        return result.toString();
    }

    private String singleReplace(String inputString, String oldString, String newString) {
        int propLength = oldString.length();
        int startPos = inputString.indexOf(oldString);
        if (startPos == -1) {
            logger.info("string: " + oldString + " not found in input: " + inputString + " - returning input");
            return inputString;
        }
        if (startPos == 0) {
            inputString = inputString.substring(propLength);
            inputString = newString + inputString;
        } else {
            inputString = inputString.substring(0, startPos) + newString + inputString.substring(startPos + propLength);
        }
        return inputString;
    }

    private String getShortPropertyName(String longPropertyName) {
        String currentProp = longPropertyName.substring(longPropertyName.indexOf(PROP_START));
        String prop = currentProp.substring(0, currentProp.indexOf(PROP_END) + 1);
        String shortProp = prop.substring(PROP_START.length(), prop.length() - 1);
        return shortProp;
    }

    /**
     * Some perl5 characters may occur in the log file format.
     * Escape these characters to prevent parsing errors.
     *
     * @param input
     * @return string
     */
    private String replaceMetaChars(String input) {
        //escape backslash first since that character is used to escape the remaining meta chars
        input = input.replaceAll("\\\\", "\\\\\\");

        //don't escape star - it's used as the wildcard
        input = input.replaceAll(Pattern.quote("]"), "\\\\]");
        input = input.replaceAll(Pattern.quote("["), "\\\\[");
        input = input.replaceAll(Pattern.quote("^"), "\\\\^");
        input = input.replaceAll(Pattern.quote("$"), "\\\\$");
        input = input.replaceAll(Pattern.quote("."), "\\\\.");
        input = input.replaceAll(Pattern.quote("|"), "\\\\|");
        input = input.replaceAll(Pattern.quote("?"), "\\\\?");
        input = input.replaceAll(Pattern.quote("+"), "\\\\+");
        input = input.replaceAll(Pattern.quote("("), "\\\\(");
        input = input.replaceAll(Pattern.quote(")"), "\\\\)");
        input = input.replaceAll(Pattern.quote("-"), "\\\\-");
        input = input.replaceAll(Pattern.quote("{"), "\\\\{");
        input = input.replaceAll(Pattern.quote("}"), "\\\\}");
        input = input.replaceAll(Pattern.quote("#"), "\\\\#");
        return input;
    }

    /**
     * Convert a keyword-to-values map to a LoggingEvent
     *
     * @param fieldMap
     * @param exception
     * @return logging event
     */
    private ChainsawLoggingEvent convertToEvent(Map<String, String> fieldMap, String[] exception) {
        if (fieldMap == null) {
            return null;
        }

        //a logger must exist at a minimum for the event to be processed
        if (!fieldMap.containsKey(LOGGER)) {
            fieldMap.put(LOGGER, "Unknown");
        }
        if (exception == null) {
            exception = emptyException;
        }

        String loggerValue;
        long timeStamp = 0L;
        String level;
        String threadName;
        String message;
        String ndc;
        String className;
        String methodName;
        String eventFileName;
        String lineNumber;
        Map<String, String> properties = new HashMap<>();

        loggerValue = fieldMap.remove(LOGGER);

        if ((dateFormat != null) && fieldMap.containsKey(TIMESTAMP)) {
            try {
                timeStamp = dateFormat.parse(fieldMap.remove(TIMESTAMP))
                    .getTime();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //use current time if timestamp not parseable/dateformat not specified
        if (timeStamp == 0L) {
            timeStamp = System.currentTimeMillis();
        }

        message = fieldMap.remove(MESSAGE);
        if (message == null) {
            message = "";
        }

        level = fieldMap.remove(LEVEL);
        Level levelImpl;
        if (level == null) {
            levelImpl = Level.DEBUG;
        } else {
            //first try to resolve against custom level definition map, then fall back to regular levels
            levelImpl = customLevelDefinitionMap.get(level);
            if (levelImpl == null) {
                levelImpl = Level.valueOf(level.trim());
            }
        }
        level = levelImpl.name();

        threadName = fieldMap.remove(THREAD);

        ndc = fieldMap.remove(NDC);

        className = fieldMap.remove(CLASS);

        methodName = fieldMap.remove(METHOD);

        eventFileName = fieldMap.remove(FILE);

        lineNumber = fieldMap.remove(LINE);

        properties.put(Constants.HOSTNAME_KEY, host);
        properties.put(Constants.APPLICATION_KEY, path);
        properties.put(Constants.RECEIVER_NAME_KEY, getName());
        if (group != null) {
            properties.put(Constants.GROUP_KEY, group);
        }

        //all remaining entries in fieldmap are properties
        properties.putAll(fieldMap);

        LocationInfo info = null;

        if ((eventFileName != null) || (className != null) || (methodName != null)
            || (lineNumber != null)) {
            info = new LocationInfo(eventFileName, className, methodName,
                    Integer.parseInt(lineNumber));
        }

        build.clear();
        build.setLogger(loggerValue)
                .setTimestamp(Instant.ofEpochMilli(timeStamp))
                .setLevelFromString(level)
                .setMessage(message)
                .setThreadName(threadName)
                .setLocationInfo(info)
                .setNDC(ndc)
                .setMDC(properties);

        return build.create();
    }

//  public static void main(String[] args) {
//    org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
//    org.apache.log4j.ConsoleAppender appender = new org.apache.log4j.ConsoleAppender(new org.apache.log4j.SimpleLayout());
//    appender.setName("console");
//    rootLogger.addAppender(appender);
//    LogFilePatternReceiver test = new LogFilePatternReceiver();
//    org.apache.log4j.spi.LoggerRepository repo = new org.apache.log4j.LoggerRepositoryExImpl(org.apache.log4j.LogManager.getLoggerRepository());
//    test.setLoggerRepository(repo);
//    test.setLogFormat("PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE");
//    test.setTailing(false);
//    test.setAppendNonMatches(true);
//    test.setTimestampFormat("yyyy-MM-d HH:mm:ss,SSS");
//    test.setFileURL("file:///C:/log/test.log");
//    test.activateOptions();
//  }

    /**
     * Close the reader.
     */
    @Override
    public void shutdown() {
        logger.info(getPath() + " shutdown");
        active = false;
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Read and process the log file.
     */
    @Override
    public void start() {
        logger.info("activateOptions");
        active = true;
        Runnable runnable = new Runnable() {
            public void run() {
                initialize();
                while (reader == null) {
                    logger.info("attempting to load file: " + getFileURL());
                    try {
                        reader = new InputStreamReader(new URL(getFileURL()).openStream(), StandardCharsets.UTF_8);
                    } catch (FileNotFoundException fnfe) {
                        logger.info("file not available - will try again");
                        synchronized (this) {
                            try {
                                wait(MISSING_FILE_RETRY_MILLIS);
                            } catch (InterruptedException ie) {
                            }
                        }
                    } catch (IOException ioe) {
                        logger.warn("unable to load file", ioe);
                        return;
                    }
                }
                try {
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    createPattern();
                    do {
                        process(bufferedReader);
                        try {
                            synchronized (this) {
                                wait(waitMillis);
                            }
                        } catch (InterruptedException ie) {
                        }
                        if (tailing) {
                            logger.debug("tailing file");
                        }
                    } while (tailing);

                } catch (IOException ioe) {
                    //io exception - probably shut down
                    logger.info("stream closed");
                }
                logger.debug("processing " + path + " complete");
                shutdown();
            }
        };
        if (useCurrentThread) {
            runnable.run();
        } else {
            new Thread(runnable, "LogFilePatternReceiver-" + getName()).start();
        }
    }
}
