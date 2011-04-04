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
package com.ibm.wala.cast.js.html.jericho;

import java.util.Map;

import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;


import com.ibm.wala.cast.js.html.ITag;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * ITag impel for Jericho generated tags
 * @author danielk 
 */
public class JerichoTag implements ITag {

	private final Element innerElement;
	private final String sourceFile;
	private Map<String, String> attributesMap;

	public JerichoTag(Element root, String sourceFile) {
		this.innerElement = root;
		Attributes attributes = innerElement.getStartTag().getAttributes();
		attributesMap = HashMapFactory.make();
		if (attributes != null) {
			attributesMap = attributes.populateMap(attributesMap, true);
		}
		this.sourceFile = sourceFile;
	}

	public Map<String, String> getAllAttributes() {
		return attributesMap;
	}

	public String getAttributeByName(String name) {
		return attributesMap.get(name.toLowerCase());
	}

	public Pair<Integer, String> getBodyText() {
		Segment content = innerElement.getContent();
		Integer lineNum = innerElement.getSource().getRow(content.getBegin());
		String body = content.toString();
		return Pair.make(lineNum, body);
	}

	public String getFilePath() {
		return sourceFile;
	}

	public String getName() {
		return innerElement.getName();
	}

	@Override
	public String toString() {
		return innerElement.toString();
	}

	public int getStartingLineNum() {
		return innerElement.getSource().getRow(innerElement.getStartTag().getBegin());
	}
}
