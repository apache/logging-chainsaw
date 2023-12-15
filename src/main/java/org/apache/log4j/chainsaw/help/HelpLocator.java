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
package org.apache.log4j.chainsaw.help;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A helper class that assists the HelpManager by serving as a collection of
 * Class loaders based on URL roots.
 *
 * @author Paul Smith
 * &lt;psmith@apache.org&gt;
 */
class HelpLocator {
    private static final Logger logger = LogManager.getLogger(HelpLocator.class);
    private final List<ClassLoader> classLoaders = new ArrayList<>();

    /**
     * Adds a ClassLoader to be used as a help resource locator
     */
    void installClassloaderLocator(ClassLoader cl) {
        classLoaders.add(cl);
    }

    /**
     * Adds a new locator to the current set of locators by using the passed in
     * URL as a base to locate help resources. The URL SHOULD end in a '/'
     * character.
     */
    void installLocator(URL url) {
        try {
            classLoaders.add(new HelpResourceLoader(url));
        } catch (Exception e) {
            logger.error("Failed to setup the resource loaders for the Help Subsystem");
        }
    }

    /**
     * Locates a help resource by using the internal resource locator collection.
     *
     * @return URL of the located resource, or null if it cannot be located.
     */
    URL findResource(String name) {
        for (ClassLoader classLoader : classLoaders) {
            URL url = classLoader.getResource(name);

            if (url != null) {
                return url;
            }
        }

        return null;
    }

    private static class HelpResourceLoader extends ClassLoader {
        private final URL root;

        private HelpResourceLoader(URL root) {
            this.root = root;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.ClassLoader#findResource(java.lang.String)
         */
        @Override
        protected URL findResource(String name) {
            URL url = super.findResource(name);

            if (url != null) {
                return url;
            }

            try {
                URL resourceURL = new URL(root, name);
                URL[] urlArray = new URL[]{root, resourceURL};
                logger.debug("Looking for Help resource at: {}", resourceURL.toExternalForm());
                logger.debug("urlArray={}", Arrays.asList(urlArray));
                return new URLClassLoader(urlArray).findResource(name);
            } catch (MalformedURLException e) {
                logger.error(e, e);
            }

            return null;
        }
    }
}
