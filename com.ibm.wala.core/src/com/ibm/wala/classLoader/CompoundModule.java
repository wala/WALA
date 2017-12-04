/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.classLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;

public class CompoundModule implements ModuleEntry, Module, SourceModule {
  private final SourceModule[] constituents;
  private final URL name;
  
  public CompoundModule(URL name, SourceModule[] constituents) {
    this.name = name;
    this.constituents = constituents;
  }

  public SourceModule[] getConstituents() {
    SourceModule[] stuff = new SourceModule[ constituents.length ];
    System.arraycopy(constituents, 0, stuff, 0, constituents.length);
    return stuff;
  }
  
  @Override
  public Iterator<ModuleEntry> getEntries() {
    return new NonNullSingletonIterator<>(this);
  }
  
  @Override
  public boolean isModuleFile() {
    return false;
  }

  @Override
  public Module asModule() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getClassName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name.toString();
  }

  @Override
  public URL getURL() {
    return name;
  }
  
  @Override
  public boolean isClassFile() {
    return false;
  }

  @Override
  public boolean isSourceFile() {
    return true;
  }

  @Override
  public InputStream getInputStream() {
    return new InputStream() {
      private int index = 0;
      private InputStream currentStream;
      
      @Override
      public int read() throws IOException {
        if (currentStream == null) {
          if (index < constituents.length) {
            currentStream = constituents[index++].getInputStream();
          } else {
            return -1;
          }
        }
        int b = currentStream.read();
        if (b == -1) {
          currentStream = null;
          return read();
        }
        return b;
      }
    };
  }

  public class Reader extends java.io.Reader {
    private final List<Pair<Integer,URL>> locations = new ArrayList<>();
    private int line = 0;
    private int index = 0;
    private LineNumberReader currentReader;
    private URL currentName;
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      if (currentReader == null) {
        if (index < constituents.length) {
          currentName = constituents[index].getURL();
          currentReader = new LineNumberReader(new InputStreamReader(constituents[index++].getInputStream()));
        } else {
          return -1;
        }
      }
      
      int x;
      if ((x = currentReader.read(cbuf, off, len)) == -1) {
        line += currentReader.getLineNumber();
        locations.add(Pair.make(line, currentName));
        
        currentReader.close();
        currentReader = null;
        
        return read(cbuf, off, len);
      }

      return x;
    }

    @Override
    public void close() throws IOException {
      if (currentReader!= null) {
        currentReader.close();
        currentReader = null;
      }
    }  
    
    public Pair<Integer,URL> getOriginalPosition(int lineNumber) {
      int start = 0;
      for(int i = 0; i < locations.size(); i++) {
        if (locations.get(i).fst >= lineNumber) {
          return Pair.make(lineNumber - start, locations.get(i).snd);
        } else {
          start = locations.get(i).fst;
        }
      }
      throw new IllegalArgumentException("line number " + lineNumber + " too high");
    }
  }
    
  @Override
  public Reader getInputReader() {
    return new Reader();
  }

  @Override
  public Module getContainer() {
    // stitched together module has no single container
    return null;
  }

}
