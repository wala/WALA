package com.ibm.wala.cast.js.html;

import java.util.Map;

import com.ibm.wala.util.collections.Pair;

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
	 * @return a pair containing the start line of the tag's body and the actual body as a string
	 */
	public Pair<Integer, String> getBodyText();

	/**
	 * Returns the starting line number of the tag.
	 * @return null if no known
	 */
	public int getStartingLineNum();
}
