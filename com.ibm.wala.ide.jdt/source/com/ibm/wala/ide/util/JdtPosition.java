package com.ibm.wala.ide.util;

import java.io.IOException;
import java.io.InputStream;
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

    public int getFirstCol() {
      return -1;
    }

    public int getFirstLine() {
      return firstLine;
    }

    public InputStream getInputStream() throws IOException {
      return null;
    }

    public int getLastCol() {
      return -1;
    }

    public int getLastLine() {
      return lastLine;
    }

    public URL getURL() {
      try {
        return new URL("file:" + path);
      } catch (MalformedURLException e) {
        Assertions.UNREACHABLE(e.toString());
        return null;
      }
    }

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

    public int getFirstOffset() {
      return firstOffset;
    }

    public int getLastOffset() {
      return lastOffset;
    }

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