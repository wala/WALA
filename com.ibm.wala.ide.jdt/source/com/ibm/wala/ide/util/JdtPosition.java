/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ide.util;

import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IFile;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.util.debug.Assertions;

public final class JdtPosition implements Position {
    private final int firstOffset;

    private final int lastOffset;

    private final int firstLine, lastLine;

    private final String path;

    private final IFile eclipseFile;
    
    public JdtPosition(int start, int end, int startLine, int endLine, IFile eclipseFile, String path) {
      firstOffset = start;
      lastOffset = end;
      firstLine = startLine;
      lastLine = endLine;
      this.path = path;
      this.eclipseFile = eclipseFile;
    }

    @Override
    public int getFirstCol() {
      return -1;
    }

    @Override
    public int getFirstLine() {
      return firstLine;
    }

    @Override
    public Reader getReader() throws IOException {
      return null;
    }

    @Override
    public int getLastCol() {
      return -1;
    }

    @Override
    public int getLastLine() {
      return lastLine;
    }

    @Override
    public URL getURL() {
      try {
        return new URL("file:" + path);
      } catch (MalformedURLException e) {
        Assertions.UNREACHABLE(e.toString());
        return null;
      }
    }

    @Override
    public int compareTo(Object arg0) {
    	if (arg0 instanceof JdtPosition) {
    		if (firstOffset != ((JdtPosition)arg0).firstOffset) {
    			return firstOffset - ((JdtPosition)arg0).firstOffset;
    		} else if (lastOffset != ((JdtPosition)arg0).lastOffset) {
    			return lastOffset - ((JdtPosition)arg0).lastOffset;
    		}
    	}
    	
    	return 0;
    }

    @Override
    public int getFirstOffset() {
      return firstOffset;
    }

    @Override
    public int getLastOffset() {
      return lastOffset;
    }

    @Override
    public String toString() {
      return "[offset " + firstOffset + ":" + lastOffset + "]";
    }
    
    public IFile getEclipseFile() {
    	return eclipseFile;
    }

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JdtPosition) {
			JdtPosition jp = (JdtPosition) obj;
			return jp.getEclipseFile().equals(eclipseFile)
					&& jp.getFirstOffset() == firstOffset 
					&& jp.getLastOffset() == lastOffset;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return firstOffset + 12432*lastOffset;
	}
    
  }
