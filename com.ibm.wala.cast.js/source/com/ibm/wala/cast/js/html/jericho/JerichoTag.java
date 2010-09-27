package com.ibm.wala.cast.js.html.jericho;

import java.util.Map;

import au.id.jericho.lib.html.Attributes;
import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.Segment;

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

	@SuppressWarnings("unchecked")
	public JerichoTag(Element root, String sourceFile) {
		this.innerElement = root;
		Attributes attributes = innerElement.getStartTag().getAttributes();
		attributesMap = HashMapFactory.make();
		if (attributes != null) {
			attributesMap = attributes.populateMap(attributesMap, false);
		}
		this.sourceFile = sourceFile;
	}

	public Map<String, String> getAllAttributes() {
		return attributesMap;
	}

	public String getAttributeByName(String name) {
		return attributesMap.get(name);
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
