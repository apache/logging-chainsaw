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

import java.awt.Font;

import javax.swing.UIManager;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;

/**
 * Apply system font and size (normal size + 1) rule if the JEditorPane document contains html.
 */
public class JTextComponentFormatter
{
    public static void applySystemFontAndSize(JTextComponent textComponent) {
        Document document = textComponent.getDocument();
        if (document instanceof HTMLDocument) {
          Font font = UIManager.getFont("Label.font");
          String bodyRule = "body { font-family: " + font.getFamily() + "; font-size: " + (font.getSize() + 1) + "pt; }";
          ((HTMLDocument)document).getStyleSheet().addRule(bodyRule);
        }
    }
}
