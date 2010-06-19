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
package org.apache.log4j.chainsaw.favourites;

/**
 * A Fauvourite is just a named container of on object that can be used
 * as a basis (prototype) for the creation of exact copies.
 * 
 * Clients should use the FavouritesRegistry to create instances of this class
 * so that explicit checks can be performed about the suitability of the
 * prototype.
 * 
 * @author Paul Smith <psmith@apache.org>
 *
 */
public final class Favourite {

    private String name;
    private Object prototype;

    /**
     * @param name
     * @param object
     */
    Favourite(String name, Object prtotype) {
        this.name = name;
        this.prototype = prtotype;
    }


    /**
     * @return Returns the name.
     */
    public final String getName() {

        return name;
    }

    /**
     * Returns the object that would be used as a basis to create new
     * instances of that same object.
     * @return Returns the prototype.
     */
    public final Object getPrototype() {
      return prototype;
    }
}
