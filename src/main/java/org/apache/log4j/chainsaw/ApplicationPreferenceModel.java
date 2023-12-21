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
package org.apache.log4j.chainsaw;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.commons.configuration2.event.EventListener;

/**
 * Encapsulates the Chainsaw Application wide properties
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 */
public class ApplicationPreferenceModel {

    public static final String SHOW_NO_RECEIVER_WARNING = "showNoReceiverWarning";
    public static final String IDENTIFIER_EXPRESSION = "identifierExpression";
    public static final String CYCLIC_BUFFER_SIZE = "cyclicBufferSize";
    public static final String TOOL_TIP_DISPLAY_MILLIS = "toolTipDisplayMillis";
    public static final String RESPONSIVENESS = "responsiveness";
    public static final String TAB_PLACEMENT = "tabPlacement";
    public static final String STATUS_BAR_VISIBLE = "statusBarVisible";
    public static final String ALTERNATING_FOREGROUND_COLOR = "alternatingForegroundColor";
    public static final String ALTERNATING_BACKGROUND_COLOR = "alternatingBackgroundColor";
    public static final String SEARCH_FOREGROUND_COLOR = "searchForegroundColor";
    public static final String SEARCH_BACKGROUND_COLOR = "searchBackgroundColor";
    public static final String RECEIVERS_VISIBLE = "receiversVisible";
    public static final String TOOLBAR_VISIBLE = "toolbarVisible";
    public static final String LOOK_AND_FEEL_CLASS_NAME = "lookAndFeelClassName";
    public static final String CONFIRM_EXIT = "confirmExit";
    public static final String SHOW_SPLASH = "showSplash";
    public static final String CONFIGURATION_URL = "configurationURL";
    public static final String BYPASS_CONFIGURATION_URL = "bypassConfigurationURL";
    public static final String DEFAULT_COLUMN_NAMES = "defaultColumnNames";
    public static final String BYPASS_SEARCH_COLORS = "bypassSearchColors";
    private final AbstractConfiguration globalConfiguration;
    private final int toolTipDisplayMillisDefault = 4000;
    private final int cyclicBufferSizeDefault = 50000;
    private final int responsivenessDefault = 3;
    private final Color searchBackgroundColorDefault = ChainsawConstants.FIND_LOGGER_BACKGROUND;
    private final Color searchForegroundColorDefault = ChainsawConstants.FIND_LOGGER_FOREGROUND;
    private final Color alternatingForegroundColorDefault = ChainsawConstants.COLOR_ODD_ROW_FOREGROUND;
    private final Color alternatingBackgroundColorDefault = ChainsawConstants.COLOR_ODD_ROW_BACKGROUND;

    private final String identifierExpressionDefault = "PROP.hostname - PROP.application";

    private final int tabPlacementDefault = 3;

    private final boolean bypassSearchColorsDefault = false;
    private final boolean statusBarVisibleDefault = true;
    private final boolean receiversDefault = false;
    private final boolean showNoReceiverWarningDefault = true;
    private final boolean isToolbarDefault = true;
    private final boolean confirmExitDefault = true;
    private final boolean showSplashDefault = true;
    private List<String> defaultColumns = List.of("LOGGER", "MARKER", "TIMESTAMP", "LEVEL", "MESSAGE");

    public ApplicationPreferenceModel(AbstractConfiguration globalConfiguration) {
        this.globalConfiguration = globalConfiguration;
    }

    public void addEventListener(EventListener<ConfigurationEvent> listener) {
        globalConfiguration.addEventListener(ConfigurationEvent.SET_PROPERTY, listener);
    }

    /**
     * @return Returns the showNoReceiverWarning.
     */
    public final boolean isShowNoReceiverWarning() {
        return globalConfiguration.getBoolean(SHOW_NO_RECEIVER_WARNING, showNoReceiverWarningDefault);
    }

    public final String getIdentifierExpression() {
        return globalConfiguration.getString(IDENTIFIER_EXPRESSION, identifierExpressionDefault);
    }

    public final void setCyclicBufferSize(int newCyclicBufferSize) {
        globalConfiguration.setProperty(CYCLIC_BUFFER_SIZE, newCyclicBufferSize);
    }

    public final int getCyclicBufferSize() {
        return globalConfiguration.getInt(CYCLIC_BUFFER_SIZE, cyclicBufferSizeDefault);
    }

    public final void setToolTipDisplayMillis(int newToolTipDisplayMillis) {
        globalConfiguration.setProperty(TOOL_TIP_DISPLAY_MILLIS, newToolTipDisplayMillis);
    }

    public final int getToolTipDisplayMillis() {
        return globalConfiguration.getInt(TOOL_TIP_DISPLAY_MILLIS, toolTipDisplayMillisDefault);
    }

    public final void setIdentifierExpression(String newIdentifierExpression) {
        globalConfiguration.setProperty(IDENTIFIER_EXPRESSION, newIdentifierExpression);
    }

    /**
     * @param newShowNoReceiverWarning The showNoReceiverWarning to set.
     */
    public final void setShowNoReceiverWarning(boolean newShowNoReceiverWarning) {
        globalConfiguration.setProperty(SHOW_NO_RECEIVER_WARNING, newShowNoReceiverWarning);
    }

    /**
     * Takes another model and copies all the values into this model
     *
     * @param model
     */
    public void apply(ApplicationPreferenceModel model) {
        setIdentifierExpression(model.getIdentifierExpression());
        setShowNoReceiverWarning(model.isShowNoReceiverWarning()
                || (model.getConfigurationURL() == null
                        || model.getConfigurationURL().trim().isEmpty()));
        setResponsiveness(model.getResponsiveness());
        setTabPlacement(model.getTabPlacement());
        setStatusBarVisible(model.isStatusBarVisible());
        setToolbarVisible(model.isToolbarVisible());
        setReceiversVisible(model.isReceiversVisible());
        if (model.getLookAndFeelClassName() != null
                && !model.getLookAndFeelClassName().trim().isEmpty()) {
            setLookAndFeelClassName(model.getLookAndFeelClassName());
        } else {
            // ensure current look and feel is selected
            setLookAndFeelClassName(UIManager.getLookAndFeel().getClass().getName());
        }
        setConfirmExit(model.isConfirmExit());
        setShowSplash(model.isShowSplash());
        setToolTipDisplayMillis(model.getToolTipDisplayMillis());
        setCyclicBufferSize(model.getCyclicBufferSize());
        // only set current config URL if bypass is null
        if (model.getBypassConfigurationURL() == null) {
            setConfigurationURL(model.getConfigurationURL());
        }
        Color searchForeground = model.getSearchForegroundColor();
        Color searchBackground = model.getSearchBackgroundColor();
        if (searchForeground != null && searchBackground != null) {
            setSearchBackgroundColor(searchBackground);
            setSearchForegroundColor(searchForeground);
        }

        Color alternatingForeground = model.getAlternatingForegroundColor();
        Color alternatingBackground = model.getAlternatingBackgroundColor();
        if (alternatingForeground != null && alternatingBackground != null) {
            setAlternatingBackgroundColor(alternatingBackground);
            setAlternatingForegroundColor(alternatingForeground);
        }
        if (!model.getDefaultColumnNames().isEmpty()) {
            setDefaultColumnNames(model.getDefaultColumnNames());
        }
        setBypassSearchColors(model.isBypassSearchColors());
    }

    // use a lighter version of search color as the delta color
    public Color getDeltaColor() {
        float factor = 1.3F;
        Color search = getSearchBackgroundColor();

        return new Color(
                boundColorValue((int) (search.getRed() * factor)),
                boundColorValue((int) (search.getGreen() * factor)),
                boundColorValue((int) (search.getBlue() * factor)));
    }

    private int boundColorValue(int colorValue) {
        return Math.min(Math.max(0, colorValue), 255);
    }

    /**
     * @return Returns the responsiveness.
     */
    public final int getResponsiveness() {
        return globalConfiguration.getInt(RESPONSIVENESS, responsivenessDefault);
    }

    /**
     * @param newValue The responsiveness to set.
     */
    public final void setResponsiveness(int newValue) {
        globalConfiguration.setProperty(RESPONSIVENESS, newValue);
    }

    /**
     * @param i
     */
    public void setTabPlacement(int i) {
        globalConfiguration.setProperty(TAB_PLACEMENT, i);
    }

    /**
     * @return Returns the tabPlacement.
     */
    public final int getTabPlacement() {
        return globalConfiguration.getInt(TAB_PLACEMENT, tabPlacementDefault);
    }

    /**
     * @return Returns the statusBar.
     */
    public final boolean isStatusBarVisible() {
        return globalConfiguration.getBoolean(STATUS_BAR_VISIBLE, statusBarVisibleDefault);
    }

    /**
     * @param statusBarVisible The statusBarVisible to set.
     */
    public final void setStatusBarVisible(boolean statusBarVisible) {
        globalConfiguration.setProperty(STATUS_BAR_VISIBLE, statusBarVisible);
    }

    public void setAlternatingForegroundColor(Color alternatingColorForegroundColor) {
        globalConfiguration.setProperty(ALTERNATING_FOREGROUND_COLOR, alternatingColorForegroundColor);
    }

    public void setAlternatingBackgroundColor(Color alternatingColorBackgroundColor) {
        globalConfiguration.setProperty(ALTERNATING_BACKGROUND_COLOR, alternatingColorBackgroundColor);
    }

    public void setSearchForegroundColor(Color searchForegroundColor) {
        globalConfiguration.setProperty(SEARCH_FOREGROUND_COLOR, searchForegroundColor);
    }

    public void setSearchBackgroundColor(Color searchBackgroundColor) {
        globalConfiguration.setProperty(SEARCH_BACKGROUND_COLOR, searchBackgroundColor);
    }

    public Color getAlternatingBackgroundColor() {
        return globalConfiguration.get(Color.class, ALTERNATING_BACKGROUND_COLOR, alternatingBackgroundColorDefault);
    }

    public Color getAlternatingForegroundColor() {
        return globalConfiguration.get(Color.class, ALTERNATING_FOREGROUND_COLOR, alternatingForegroundColorDefault);
    }

    public Color getSearchBackgroundColor() {
        return globalConfiguration.get(Color.class, SEARCH_BACKGROUND_COLOR, searchBackgroundColorDefault);
    }

    public Color getSearchForegroundColor() {
        return globalConfiguration.get(Color.class, SEARCH_FOREGROUND_COLOR, searchForegroundColorDefault);
    }

    /**
     * @return Returns the receivers.
     */
    public final boolean isReceiversVisible() {
        return globalConfiguration.getBoolean(RECEIVERS_VISIBLE, receiversDefault);
    }

    /**
     * @param receiversVisible The receiversVisible to set.
     */
    public final void setReceiversVisible(boolean receiversVisible) {
        globalConfiguration.setProperty(RECEIVERS_VISIBLE, receiversVisible);
    }

    /**
     * @return Returns the toolbar.
     */
    public final boolean isToolbarVisible() {
        return globalConfiguration.getBoolean(TOOLBAR_VISIBLE, isToolbarDefault);
    }

    /**
     * @param toolbarVisible The toolbarVisible to set.
     */
    public final void setToolbarVisible(boolean toolbarVisible) {
        globalConfiguration.setProperty(TOOLBAR_VISIBLE, toolbarVisible);
    }

    /**
     * @return Returns the lookAndFeelClassName.
     */
    public final String getLookAndFeelClassName() {
        return globalConfiguration.getString(LOOK_AND_FEEL_CLASS_NAME, "");
    }

    /**
     * @param lookAndFeelClassName The lookAndFeelClassName to set.
     */
    public final void setLookAndFeelClassName(String lookAndFeelClassName) {
        globalConfiguration.setProperty(LOOK_AND_FEEL_CLASS_NAME, lookAndFeelClassName);
    }

    /**
     * @return Returns the confirmExit.
     */
    public final boolean isConfirmExit() {
        return globalConfiguration.getBoolean(CONFIRM_EXIT, confirmExitDefault);
    }

    /**
     * @param confirmExit The confirmExit to set.
     */
    public final void setConfirmExit(boolean confirmExit) {
        globalConfiguration.setProperty(CONFIRM_EXIT, confirmExit);
    }

    /**
     * @return Returns the showSplash.
     */
    public final boolean isShowSplash() {
        return globalConfiguration.getBoolean(SHOW_SPLASH, showSplashDefault);
    }

    /**
     * @param showSplash The showSplash to set.
     */
    public final void setShowSplash(boolean showSplash) {
        globalConfiguration.setProperty(SHOW_SPLASH, showSplash);
    }

    /**
     * @return Returns the configurationURL.
     */
    public final String getConfigurationURL() {
        return globalConfiguration.getString(CONFIGURATION_URL, "");
    }

    public final String getBypassConfigurationURL() {
        return globalConfiguration.getString(BYPASS_CONFIGURATION_URL, "");
    }

    /*
     Set to null to un-bypass
    */
    public void setBypassConfigurationURL(String bypassConfigurationURL) {
        globalConfiguration.setProperty(BYPASS_CONFIGURATION_URL, bypassConfigurationURL);
    }

    /**
     * @param configurationURL The configurationURL to set.
     */
    public final void setConfigurationURL(String configurationURL) {
        globalConfiguration.setProperty(CONFIGURATION_URL, configurationURL);
    }

    public void setDefaultColumnNames(List<String> defaultColumnNames) {
        globalConfiguration.setProperty(DEFAULT_COLUMN_NAMES, defaultColumnNames);
    }

    public List<String> getDefaultColumnNames() {
        return globalConfiguration.getList(String.class, DEFAULT_COLUMN_NAMES, defaultColumns);
    }

    public void setBypassSearchColors(boolean bypassSearchColors) {
        globalConfiguration.setProperty(BYPASS_SEARCH_COLORS, bypassSearchColors);
    }

    public boolean isBypassSearchColors() {
        return globalConfiguration.getBoolean(BYPASS_SEARCH_COLORS, bypassSearchColorsDefault);
    }
}
