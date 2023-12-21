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
package org.apache.log4j.chainsaw.components.tutorial;

import java.security.SecureRandom;

public class RandomWordGenerator {
    SecureRandom random = new SecureRandom();
    private final String[] SYLLABLES = {
        "can", "cen", "cin", "con", "cun",
        "na", "ne", "ni", "no", "nu",
        "ta", "te", "ti", "to", "tu",
        "la", "le", "li", "lo", "lu",
        "ma", "me", "mi", "mo", "mu",
        "ra", "re", "ri", "ro", "ru",
        "da", "de", "di", "do", "du",
        "fa", "fe", "fi", "fo", "fu",
        "sa", "se", "fi", "so", "su"
    };

    public String generateWord(int length) {
        StringBuilder word = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            word.append(SYLLABLES[random.nextInt(SYLLABLES.length)]);
        }
        return word.toString();
    }

    public String generateSentence(int words) {
        StringBuilder sentence = new StringBuilder(words);
        for (int i = 0; i < words; i++) {
            int randomSyllables = random.nextInt(6) + 2; // 2-7 syllabiles
            sentence.append(generateWord(randomSyllables)).append(" ");
        }
        return sentence.toString().trim();
    }

    public String generateSentence() {
        int words = random.nextInt(12) + 4;
        return generateSentence(words);
    }
}
