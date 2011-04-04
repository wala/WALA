package com.ibm.wala.cast.js.html;

/******************************************************************************
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
import java.util.Map;

/**
 * @author danielk
 * Data structure representing an HTML tag, with its attributes and content. Used by the HTML parser when calling the callback.
 */
public interface ITag {

	/**
	 * @return tag's name (e.g., "HEAD" / "HTML" / "FORM")
	 */
	public String getName();

	/**
	 * Retrieves a specific attribute 
	 * @param name
	 * @return null if there is no such attribute
	 */
	public String getAttributeByName(String name);

	public Map<String, String> getAllAttributes();

	/**
	 * Returns the starting line number of the tag.
	 * @return null if no known
	 */
	public int getStartingLineNum();
}
