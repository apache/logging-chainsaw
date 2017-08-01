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

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.UIManager;

/**
 * Encapsulates the Chainsaw Application wide properties
 *
 * @author Paul Smith &lt;psmith@apache.org&gt;
 *
 */
public class ApplicationPreferenceModel {

    private boolean showNoReceiverWarning = true;
    private boolean statusBar = true;
    private boolean toolbar = true;
    private boolean receivers = false; //setting this to true will cause the receivers panel to be displayed by default
    private boolean confirmExit = true;
    private boolean showSplash = true;
    private String lookAndFeelClassName = "";
    private int toolTipDisplayMillis = 4000;
    private int cyclicBufferSize = 50000;
    private String lastUsedVersion = "";
    private int responsiveness = 3;
    private Color searchBackgroundColor = ChainsawConstants.FIND_LOGGER_BACKGROUND;
    private Color searchForegroundColor = ChainsawConstants.FIND_LOGGER_FOREGROUND;
    private Color alternatingColorForegroundColor = ChainsawConstants.COLOR_ODD_ROW_FOREGROUND;
    private Color alternatingColorBackgroundColor = ChainsawConstants.COLOR_ODD_ROW_BACKGROUND;

    private String identifierExpression = "PROP.hostname - PROP.application";

    private transient final PropertyChangeSupport propertySupport =
        new PropertyChangeSupport(this);
    
    private int tabPlacement = 3;
    
    /**
     * If not 'empty', this property will be used as the URL to load log4j configuration at startup
     */
    private Vector configurationURLs=new Vector();

    private String configurationURL = "";

    /**
     * Remember current config if provided via means other than configurationURL (command line arg, etc)
     */
    private transient String bypassConfigurationURL = null;
    /**
     *    this means for Receivers that require optional jars that can't be delivered
     *    by the Web start classloader, we need to be able to remove the SecurityManager in place
     */
    private boolean okToRemoveSecurityManager = false;
    private static final int CONFIGURATION_URL_ENTRY_COUNT = 10;
    private List defaultColumnNames = new ArrayList();
    private boolean defaultColumnsSet;
    private boolean bypassSearchColors = false;

  /**
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    /**
     * @param propertyName
     * @param listener
     */
    public void addPropertyChangeListener(String propertyName,
        PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    private void firePropertyChange(String propertyName, boolean oldValue,
        boolean newValue) {
        propertySupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    private void firePropertyChange(String propertyName, int oldValue,
        int newValue) {
        propertySupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    private void firePropertyChange(String propertyName, Object oldValue,
        Object newValue) {
        propertySupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * @param propertyName
     * @return listeners flag
     */
    public boolean hasListeners(String propertyName) {
        return propertySupport.hasListeners(propertyName);
    }

    /**
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    /**
     * @return Returns the showNoReceiverWarning.
     */
    public final boolean isShowNoReceiverWarning() {

        return showNoReceiverWarning;
    }
    
    public final String getIdentifierExpression() {
        return identifierExpression;
    }

    public final void setCyclicBufferSize(int newCyclicBufferSize) {
        int oldCyclicBufferSize = cyclicBufferSize;
        cyclicBufferSize = newCyclicBufferSize;
        firePropertyChange("cyclicBufferSize", oldCyclicBufferSize, newCyclicBufferSize);
    }
    
    public final int getCyclicBufferSize() {
        return cyclicBufferSize;
    }

    public final void setToolTipDisplayMillis(int newToolTipDisplayMillis) {
        int oldToolTipDisplayMillis = toolTipDisplayMillis;
        toolTipDisplayMillis = newToolTipDisplayMillis;
        firePropertyChange("toolTipDisplayMillis", oldToolTipDisplayMillis, newToolTipDisplayMillis);
    }
    
    public final int getToolTipDisplayMillis() {
        return toolTipDisplayMillis;
    }

    public final void setIdentifierExpression(String newIdentifierExpression) {
        String oldIdentifierExpression=identifierExpression;
        this.identifierExpression = newIdentifierExpression;
        firePropertyChange("identifierExpression", oldIdentifierExpression, newIdentifierExpression);
    }

    /**
     * @param newShowNoReceiverWarning The showNoReceiverWarning to set.
     */
    public final void setShowNoReceiverWarning(boolean newShowNoReceiverWarning) {
        boolean oldShowNoReceiverWarning=showNoReceiverWarning;
        this.showNoReceiverWarning = newShowNoReceiverWarning;
        firePropertyChange("showNoReceiverWarning", oldShowNoReceiverWarning, newShowNoReceiverWarning);
    }


    /**
     * Takes another model and copies all the values into this model
     * @param model
     */
    public void apply(ApplicationPreferenceModel model)
    {
      setIdentifierExpression(model.getIdentifierExpression());
      setShowNoReceiverWarning(model.isShowNoReceiverWarning() || (model.getConfigurationURL() == null || model.getConfigurationURL().trim().equals("")));
      setResponsiveness(model.getResponsiveness());
      setTabPlacement(model.getTabPlacement());
      setStatusBar(model.isStatusBar());
      setToolbar(model.isToolbar());
      setReceivers(model.isReceivers());
      if (model.getLookAndFeelClassName() != null && !model.getLookAndFeelClassName().trim().equals("")) {
          setLookAndFeelClassName(model.getLookAndFeelClassName());
      } else {
          //ensure current look and feel is selected
          setLookAndFeelClassName(UIManager.getLookAndFeel().getClass().getName());
      }
      setConfirmExit(model.isConfirmExit());
      setShowSplash(model.isShowSplash());
      setToolTipDisplayMillis(model.getToolTipDisplayMillis());
      setCyclicBufferSize(model.getCyclicBufferSize());
      Vector configurationURLs = model.getConfigurationURLs();
      if (configurationURLs != null) {
        setConfigurationURLs(configurationURLs);
      }
      //only set current config URL if bypass is null
      if (model.getBypassConfigurationURL() == null) {
        setConfigurationURL(model.getConfigurationURL());
      }
      setLastUsedVersion(model.getLastUsedVersion());
      setOkToRemoveSecurityManager(model.isOkToRemoveSecurityManager());
      Color searchForeground = model.getSearchForegroundColor();
      Color searchBackground = model.getSearchBackgroundColor();
      if (searchForeground != null && searchBackground != null) {
        setSearchBackgroundColor(searchBackground);
        setSearchForegroundColor(searchForeground);
      }

      Color alternatingForeground = model.getAlternatingColorForegroundColor();
      Color alternatingBackground = model.getAlternatingColorBackgroundColor();
      if (alternatingForeground != null && alternatingBackground != null) {
        setAlternatingBackgroundColor(alternatingBackground);
        setAlternatingForegroundColor(alternatingForeground);
      }
      if (model.isDefaultColumnsSet()) {
        setDefaultColumnNames(model.getDefaultColumnNames());
      }
      setBypassSearchColors(model.isBypassSearchColors());
    }

    //use a lighter version of search color as the delta color
    public Color getDeltaColor() {
        float factor = 1.3F;
        Color search = getSearchBackgroundColor();

        return new Color(boundColorValue((int)(search.getRed() * factor)),
                boundColorValue((int)(search.getGreen() * factor)),
                boundColorValue((int)(search.getBlue() * factor)));
    }

    private int boundColorValue(int colorValue) {
      return Math.min(Math.max(0, colorValue), 255);
    }


    /**
     * @return Returns the responsiveness.
     */
    public final int getResponsiveness()
    {
      return responsiveness;
    }
    /**
     * @param newValue The responsiveness to set.
     */
    public final void setResponsiveness(int newValue)
    {
      int oldvalue = responsiveness;
      
      if (newValue >= 1000) {
        responsiveness = (newValue - 750) / 1000;
      } else {
        responsiveness = newValue;
      }
      firePropertyChange("responsiveness", oldvalue, responsiveness);
    }

    /**
     * @param i
     */
    public void setTabPlacement(int i) {
      int oldValue = this.tabPlacement;
       this.tabPlacement = i;
       firePropertyChange("tabPlacement",oldValue,this.tabPlacement);
    }
    /**
     * @return Returns the tabPlacement.
     */
    public final int getTabPlacement() {
      return tabPlacement;
    }

    /**
     * @return Returns the statusBar.
     */
    public final boolean isStatusBar() {
      return statusBar;
    }

    public Vector getConfigurationURLs() {
        return configurationURLs;
    }

    public void setConfigurationURLs(Vector urls) {
        if (urls != null) {
            configurationURLs = urls;
        }
    }

    /**
     * @param statusBar The statusBar to set.
     */
    public final void setStatusBar(boolean statusBar) {
      boolean oldValue = this.statusBar;
      this.statusBar = statusBar;
      firePropertyChange("statusBar", oldValue, this.statusBar);
    }

    public void setAlternatingForegroundColor(Color alternatingColorForegroundColor) {
        this.alternatingColorForegroundColor = alternatingColorForegroundColor;
        firePropertyChange("alternatingColor", true, false);
    }

    public void setAlternatingBackgroundColor(Color alternatingColorBackgroundColor) {
        this.alternatingColorBackgroundColor = alternatingColorBackgroundColor;
        firePropertyChange("alternatingColor", true, false);
    }

    public void setSearchForegroundColor(Color searchForegroundColor) {
        this.searchForegroundColor = searchForegroundColor;
        firePropertyChange("searchColor", true, false);
    }

    public void setSearchBackgroundColor(Color searchBackgroundColor) {
        this.searchBackgroundColor = searchBackgroundColor;
        firePropertyChange("searchColor", true, false);
    }

    public Color getAlternatingColorBackgroundColor() {
        return alternatingColorBackgroundColor;
    }

    public Color getAlternatingColorForegroundColor() {
        return alternatingColorForegroundColor;
    }

    public Color getSearchBackgroundColor() {
        return searchBackgroundColor;
    }

    public Color getSearchForegroundColor() {
        return searchForegroundColor;
    }

    /**
     * @return Returns the receivers.
     */
    public final boolean isReceivers()
    {
      return receivers;
    }
    /**
     * @param receivers The receivers to set.
     */
    public final void setReceivers(boolean receivers)
    {
      boolean oldValue = this.receivers;
      this.receivers = receivers;
      firePropertyChange("receivers", oldValue, this.receivers);
    }
    /**
     * @return Returns the toolbar.
     */
    public final boolean isToolbar()
    {
      return toolbar;
    }
    /**
     * @param toolbar The toolbar to set.
     */
    public final void setToolbar(boolean toolbar)
    {
      boolean oldValue = this.toolbar;
      this.toolbar = toolbar;
      firePropertyChange("toolbar", oldValue, this.toolbar);
    }
    /**
     * @return Returns the lookAndFeelClassName.
     */
    public final String getLookAndFeelClassName() {
      return lookAndFeelClassName;
    }

    /**
     * @param lookAndFeelClassName The lookAndFeelClassName to set.
     */
    public final void setLookAndFeelClassName(String lookAndFeelClassName) {
      String oldValue = this.lookAndFeelClassName;
      this.lookAndFeelClassName = lookAndFeelClassName;
      firePropertyChange("lookAndFeelClassName", oldValue, this.lookAndFeelClassName);
    }

    /**
     * @return Returns the confirmExit.
     */
    public final boolean isConfirmExit() {
      return confirmExit;
    }

    /**
     * @param confirmExit The confirmExit to set.
     */
    public final void setConfirmExit(boolean confirmExit) {
      boolean oldValue = this.confirmExit;
      this.confirmExit = confirmExit;
      firePropertyChange("confirmExit", oldValue, this.confirmExit);
    }

    /**
     * @return Returns the showSplash.
     */
    public final boolean isShowSplash() {
      return showSplash;
    }
    /**
     * @param showSplash The showSplash to set.
     */
    public final void setShowSplash(boolean showSplash) {
      boolean oldValue = this.showSplash;
      this.showSplash = showSplash;
      firePropertyChange("showSplash", oldValue,this.showSplash);
    }
    /**
     * @return Returns the configurationURL.
     */
    public final String getConfigurationURL()
    {
        return this.configurationURL;
    }

    public final String getBypassConfigurationURL() {
        return bypassConfigurationURL;
    }
    /*
      Set to null to un-bypass
     */
    public void setBypassConfigurationURL(String bypassConfigurationURL) {
        //don't change configuration URL..configurationURL is persisted on app exit
        if (bypassConfigurationURL != null && bypassConfigurationURL.trim().equals("")) {
            this.bypassConfigurationURL = null;
        }
        this.bypassConfigurationURL = bypassConfigurationURL;
    }

    /**
     * @param configurationURL The configurationURL to set.
     */
    public final void setConfigurationURL(String configurationURL)
    {
        //don't add empty entries, but allow the current configuration URL to be set to an empty string
        Object oldValue = this.bypassConfigurationURL != null? this.bypassConfigurationURL:this.configurationURL;
        bypassConfigurationURL = null;
        if (configurationURL == null || configurationURL.trim().equals("")) {
            this.configurationURL = "";
            firePropertyChange("configurationURL", oldValue, this.configurationURL);
            return;
        }
        //add entry to MRU list
        if (!configurationURLs.contains(configurationURL)) {
          if (configurationURLs.size() == CONFIGURATION_URL_ENTRY_COUNT) {
              configurationURLs.remove(CONFIGURATION_URL_ENTRY_COUNT - 1);
          }
          configurationURLs.add(0, configurationURL);
        }
      this.configurationURL = configurationURL;
      firePropertyChange("configurationURL", oldValue, this.configurationURL);
    }
    /**
     * @return Returns the lastUsedVersion.
     */
    public final String getLastUsedVersion()
    {
        return this.lastUsedVersion;
    }
    /**
     * @param lastUsedVersion The lastUsedVersion to set.
     */
    public final void setLastUsedVersion(String lastUsedVersion)
    {
        String oldValue = this.lastUsedVersion;
        this.lastUsedVersion = lastUsedVersion;
        firePropertyChange("lastUsedVersion", oldValue, this.lastUsedVersion);
    }

	/**
	 * @return ok to remove security manager flag
	 */
	public final boolean isOkToRemoveSecurityManager() {
		return this.okToRemoveSecurityManager;
	}
	/**
	 * @param okToRemoveSecurityManager The okToRemoveSecurityManager to set.
	 */
	public final void setOkToRemoveSecurityManager(boolean okToRemoveSecurityManager) {
		boolean oldValue = this.okToRemoveSecurityManager;
        this.okToRemoveSecurityManager = okToRemoveSecurityManager;
        firePropertyChange("okToRemoveSecurityManager", oldValue, this.okToRemoveSecurityManager);
	}

  public void setDefaultColumnNames(List defaultColumnNames) {
    defaultColumnsSet = true;
    this.defaultColumnNames.clear();
    this.defaultColumnNames.addAll(defaultColumnNames);
  }

  public boolean isDefaultColumnsSet() {
    return defaultColumnsSet;
  }

  public List getDefaultColumnNames() {
    return defaultColumnNames;
  }

  public void setBypassSearchColors(boolean bypassSearchColors) {
    this.bypassSearchColors = bypassSearchColors;
  }

  public boolean isBypassSearchColors() {
    return bypassSearchColors;
  }
}
