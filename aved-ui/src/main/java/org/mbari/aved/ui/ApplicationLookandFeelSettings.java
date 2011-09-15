/*
 * @(#)ApplicationLookandFeelSettings.java
 * 
 * Copyright 2011 MBARI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */



package org.mbari.aved.ui;

//~--- non-JDK imports --------------------------------------------------------

import com.jgoodies.clearlook.ClearLookManager;
import com.jgoodies.clearlook.ClearLookMode;
import com.jgoodies.plaf.BorderStyle;
import com.jgoodies.plaf.FontSizeHints;
import com.jgoodies.plaf.HeaderStyle;
import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.plastic.PlasticTheme;
import com.jgoodies.plaf.plastic.PlasticXPLookAndFeel;
import com.jgoodies.plaf.plastic.theme.*;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Color;

import javax.swing.LookAndFeel;

/**
 * Describes most of the optional settings of the JGoodies Looks.
 * Based on the DemoFrame example to configure the UI.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.1.1.1 $
 *
 * @see     com.jgoodies.plaf.BorderStyle
 * @see     com.jgoodies.plaf.FontSizeHints
 * @see     com.jgoodies.plaf.HeaderStyle
 * @see     com.jgoodies.plaf.Options
 */
public final class ApplicationLookandFeelSettings {
    private ClearLookMode clearLookMode;
    private String        clearLookPolicyName;
    private FontSizeHints fontSizeHints;
    private Color         highlightColor;
    private Boolean       menuBar3DHint;
    private HeaderStyle   menuBarHeaderStyle;
    private BorderStyle   menuBarPlasticBorderStyle;
    private BorderStyle   menuBarWindowsBorderStyle;
    private boolean       plasticHighContrastFocusEnabled;
    private String        plasticTabStyle;
    private Boolean       popupDropShadowEnabled;
    private Color         selectedColor;
    private LookAndFeel   selectedLookAndFeel;
    private PlasticTheme  selectedTheme;
    private boolean       tabIconsEnabled;
    private Boolean       toolBar3DHint;
    private HeaderStyle   toolBarHeaderStyle;
    private BorderStyle   toolBarPlasticBorderStyle;
    private BorderStyle   toolBarWindowsBorderStyle;
    private boolean       useNarrowButtons;
    private Boolean       useSystemFonts;

    // Instance Creation ******************************************************
    private ApplicationLookandFeelSettings() {

        // Override default constructor; prevents instantiability.
    }

    public static ApplicationLookandFeelSettings createDefault() {
        ApplicationLookandFeelSettings settings = new ApplicationLookandFeelSettings();

        settings.setSelectedLookAndFeel(new PlasticXPLookAndFeel());
        settings.setSelectedTheme(new DesertBluer());
        settings.setUseSystemFonts(Boolean.TRUE);
        settings.setFontSizeHints(FontSizeHints.MIXED);
        settings.setUseNarrowButtons(false);
        settings.setTabIconsEnabled(true);
        settings.setPlasticTabStyle(PlasticLookAndFeel.TAB_STYLE_DEFAULT_VALUE);
        settings.setPlasticHighContrastFocusEnabled(false);
        settings.setMenuBarHeaderStyle(null);
        settings.setMenuBarPlasticBorderStyle(null);
        settings.setMenuBarWindowsBorderStyle(null);
        settings.setMenuBar3DHint(null);
        settings.setToolBarHeaderStyle(null);
        settings.setToolBarPlasticBorderStyle(null);
        settings.setToolBarWindowsBorderStyle(null);
        settings.setToolBar3DHint(null);
        settings.setClearLookMode(ClearLookMode.OFF);
        settings.setClearLookPolicyName(ClearLookManager.getPolicy().getClass().getName());
        settings.setHighlightColor(new Color(108, 207, 255));
        settings.setSelectedColor(new Color(0, 128, 192));

        return settings;
    }

    // Accessors **************************************************************
    private void setHighlightColor(Color color) {
        highlightColor = color;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    private void setSelectedColor(Color color) {
        selectedColor = color;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public ClearLookMode getClearLookMode() {
        return clearLookMode;
    }

    public void setClearLookMode(ClearLookMode clearLookMode) {
        this.clearLookMode = clearLookMode;
    }

    public String getClearLookPolicyName() {
        return clearLookPolicyName;
    }

    public void setClearLookPolicyName(String clearLookPolicyName) {
        this.clearLookPolicyName = clearLookPolicyName;
    }

    public FontSizeHints getFontSizeHints() {
        return fontSizeHints;
    }

    public void setFontSizeHints(FontSizeHints fontSizeHints) {
        this.fontSizeHints = fontSizeHints;
    }

    public Boolean getMenuBar3DHint() {
        return menuBar3DHint;
    }

    public void setMenuBar3DHint(Boolean menuBar3DHint) {
        this.menuBar3DHint = menuBar3DHint;
    }

    public HeaderStyle getMenuBarHeaderStyle() {
        return menuBarHeaderStyle;
    }

    public void setMenuBarHeaderStyle(HeaderStyle menuBarHeaderStyle) {
        this.menuBarHeaderStyle = menuBarHeaderStyle;
    }

    public BorderStyle getMenuBarPlasticBorderStyle() {
        return menuBarPlasticBorderStyle;
    }

    public void setMenuBarPlasticBorderStyle(BorderStyle menuBarPlasticBorderStyle) {
        this.menuBarPlasticBorderStyle = menuBarPlasticBorderStyle;
    }

    public BorderStyle getMenuBarWindowsBorderStyle() {
        return menuBarWindowsBorderStyle;
    }

    public void setMenuBarWindowsBorderStyle(BorderStyle menuBarWindowsBorderStyle) {
        this.menuBarWindowsBorderStyle = menuBarWindowsBorderStyle;
    }

    public Boolean isPopupDropShadowEnabled() {
        return popupDropShadowEnabled;
    }

    public void setPopupDropShadowEnabled(Boolean popupDropShadowEnabled) {
        this.popupDropShadowEnabled = popupDropShadowEnabled;
    }

    public boolean isPlasticHighContrastFocusEnabled() {
        return plasticHighContrastFocusEnabled;
    }

    public void setPlasticHighContrastFocusEnabled(boolean plasticHighContrastFocusEnabled) {
        this.plasticHighContrastFocusEnabled = plasticHighContrastFocusEnabled;
    }

    public String getPlasticTabStyle() {
        return plasticTabStyle;
    }

    public void setPlasticTabStyle(String plasticTabStyle) {
        this.plasticTabStyle = plasticTabStyle;
    }

    public LookAndFeel getSelectedLookAndFeel() {
        return selectedLookAndFeel;
    }

    public void setSelectedLookAndFeel(LookAndFeel selectedLookAndFeel) {
        this.selectedLookAndFeel = selectedLookAndFeel;
    }

    public PlasticTheme getSelectedTheme() {
        return selectedTheme;
    }

    public void setSelectedTheme(PlasticTheme selectedTheme) {
        this.selectedTheme = selectedTheme;
    }

    public boolean isTabIconsEnabled() {
        return tabIconsEnabled;
    }

    public void setTabIconsEnabled(boolean tabIconsEnabled) {
        this.tabIconsEnabled = tabIconsEnabled;
    }

    public Boolean getToolBar3DHint() {
        return toolBar3DHint;
    }

    public void setToolBar3DHint(Boolean toolBar3DHint) {
        this.toolBar3DHint = toolBar3DHint;
    }

    public HeaderStyle getToolBarHeaderStyle() {
        return toolBarHeaderStyle;
    }

    public void setToolBarHeaderStyle(HeaderStyle toolBarHeaderStyle) {
        this.toolBarHeaderStyle = toolBarHeaderStyle;
    }

    public BorderStyle getToolBarPlasticBorderStyle() {
        return toolBarPlasticBorderStyle;
    }

    public void setToolBarPlasticBorderStyle(BorderStyle toolBarPlasticBorderStyle) {
        this.toolBarPlasticBorderStyle = toolBarPlasticBorderStyle;
    }

    public BorderStyle getToolBarWindowsBorderStyle() {
        return toolBarWindowsBorderStyle;
    }

    public void setToolBarWindowsBorderStyle(BorderStyle toolBarWindowsBorderStyle) {
        this.toolBarWindowsBorderStyle = toolBarWindowsBorderStyle;
    }

    public boolean isUseNarrowButtons() {
        return useNarrowButtons;
    }

    public void setUseNarrowButtons(boolean useNarrowButtons) {
        this.useNarrowButtons = useNarrowButtons;
    }

    public Boolean isUseSystemFonts() {
        return useSystemFonts;
    }

    public void setUseSystemFonts(Boolean useSystemFonts) {
        this.useSystemFonts = useSystemFonts;
    }
}
