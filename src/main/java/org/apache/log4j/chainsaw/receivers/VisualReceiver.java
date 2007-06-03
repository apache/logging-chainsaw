/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.chainsaw.receivers;

import java.awt.Container;

/**
 * If a receiver has a visual component, implement this interface and Chainsaw will call 
 * 'setContainer' passing in a container that the receiver can use.
 * <p>
 * For example, VFSLogFilePatternReceiver provides an optional 'promptForUserInfo', that
 * when set to true, will allow a login dialog to be displayed on top of the Chainsaw window.
 * 
 * @author Scott Deboy<sdeboy@apache.org>
 *
 */

public interface VisualReceiver {
	/**
	 * Provides access to a container.
	 * 
	 * @param container
	 */
	void setContainer(Container container);
}
