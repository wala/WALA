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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;

import com.ibm.wala.cast.js.html.ITag;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * ITag impel for Jericho generated tags
 * @author danielk 
 */
public class JerichoTag implements ITag {

	private final Element innerElement;
	private final String sourceFile;
	private Map<String, Pair<String, Position>> allAttributes = null;
	
	public JerichoTag(Element root, String sourceFile) {
		this.innerElement = root;
		this.sourceFile = sourceFile;
	}

	 private Position getPosition(final Segment e) {
	    return new AbstractSourcePosition() {

	      @Override
	      public int getFirstLine() {
	        return e.getSource().getRowColumnVector(e.getBegin()).getRow();
	      }

	      @Override
	      public int getLastLine() {
	        return e.getSource().getRowColumnVector(e.getEnd()).getRow();
	      }

	      @Override
	      public int getFirstCol() {
	        return e.getSource().getRowColumnVector(e.getBegin()).getColumn();
	      }

	      @Override
	      public int getLastCol() {
	        return e.getSource().getRowColumnVector(e.getEnd()).getColumn();
	      }

	      @Override
	      public int getFirstOffset() {
	         return e.getBegin();
	      }

	      @Override
	      public int getLastOffset() {
	        return e.getEnd();
	      }

	      @Override
	      public URL getURL() {
	        try {
	          return new URL("file://" + sourceFile);
	        } catch (MalformedURLException e) {
	          return null;
	        }
	      }

	      @Override
	      public InputStream getInputStream() throws IOException {
	        return new FileInputStream(sourceFile);
	      }
	    };
	  }

	private Map<String, Pair<String, Position>> makeAllAttributes() {
	  Map<String, Pair<String, Position>> result = HashMapFactory.make();
	  if (innerElement.getStartTag().getAttributes() != null) {
	    for (Attribute a : innerElement.getStartTag().getAttributes()) {
	      result.put(
	        a.getName().toLowerCase(), 
	        Pair.make(a.getValue(), getPosition(a.getValueSegment())));
	    }
	  }
		return result;
	}

	 public Map<String, Pair<String, Position>> getAllAttributes() {
	   if (allAttributes == null) {
	     allAttributes = makeAllAttributes();
	   }
	   return allAttributes;
	 }
	 
	public Pair<String, Position> getAttributeByName(String name) {
    if (allAttributes == null) {
      allAttributes = makeAllAttributes();
    }
		return allAttributes.get(name.toLowerCase());
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
	
	 public Position getElementPosition() {
	   return getPosition(innerElement);
	 }
 
	 public Position getContentPosition() {
     return getPosition(innerElement.getContent());
   }
}
