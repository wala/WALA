/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.tree.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

public class CAstSourcePositionRecorder implements CAstSourcePositionMap {
 
  private final HashMap<CAstNode, Position> positions = HashMapFactory.make();

  @Override
  public Position getPosition(CAstNode n) {
    return positions.get(n);
  }

  @Override
  public Iterator<CAstNode> getMappedNodes() {
    return positions.keySet().iterator();
  }

  public void setPosition(CAstNode n, Position p) {
    positions.put(n, p);
  }

  public void setPosition(CAstNode n, 
			  final int fl, 
			  final int fc, 
			  final int ll,
			  final int lc,
			  final String url,
			  final String file)
      throws MalformedURLException
  {
    setPosition(n, fl, fc, ll, lc, new URL(url), new URL(file));
  }

  public void setPosition(CAstNode n, 
			  final int fl, 
			  final int fc, 
			  final int ll,
			  final int lc,
			  final URL url,
			  final URL file)
  {
    setPosition(n,
      new AbstractSourcePosition() {
	@Override
  public int getFirstLine() { return fl; }
	@Override
  public int getLastLine() { return ll; }
	@Override
  public int getFirstCol() { return fc; }
	@Override
  public int getLastCol() { return lc; }
	@Override
  public int getFirstOffset() { return -1; }
	@Override
  public int getLastOffset() { return -1; }
	@Override
  public URL getURL() { return url; }
	@Override
  public Reader getReader() throws IOException { 
	  return new InputStreamReader(file.openConnection().getInputStream());
	}
	@Override
  public String toString() {
	  return "["+fl+":"+fc+"]->["+ll+":"+lc+"]";
	}
      });
  }
    
  public void setPosition(CAstNode n, int lineNumber, String url, String file)
    throws MalformedURLException
  {
    setPosition(n, lineNumber, new URL(url), new URL(file));
  }

  public void setPosition(CAstNode n, int lineNumber, URL url, URL file) {
    setPosition(n, new LineNumberPosition(url, file, lineNumber));
  }

  public void addAll(CAstSourcePositionMap other) {
    for(CAstNode node : Iterator2Iterable.make(other.getMappedNodes())) {
      setPosition(node, other.getPosition(node));
    }
  }
}
