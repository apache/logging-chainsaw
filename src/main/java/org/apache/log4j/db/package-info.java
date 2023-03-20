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

/**
 * Provides means to append logging events into various databases. The persisted data can be later read back using {@link org.apache.log4j.db.DBReceiver}.
 * <p>
 * Most popular database systems, such as PostgreSQL, MySQL, Oracle, DB2 and MsSQL are supported.
 * </p>
 * <p>
 * Just as importantly, the way for obtaining JDBC connections is pluggable. Connections can be obtained through the traditional way of DriverManager, or
 * alternatively as a DataSource. A DataSource can be instantiated directly or it can obtained through JNDI.
 * </p>
 */
package org.apache.log4j.db;
