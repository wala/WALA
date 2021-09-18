/*
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.html;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

/**
 * Represents a region of source code, with source locations. Regions can be added to other {@link
 * SourceRegion}s, with nested source location information maintained.
 */
public class SourceRegion {

  private final StringBuilder source = new StringBuilder();

  /** source location information */
  private FileMapping fileMapping;

  private int currentLine = 1;

  public SourceRegion() {}

  public void print(final String text, Position originalPos, URL url, boolean bogusURL) {
    int startOffset = source.length();
    source.append(text);
    int endOffset = source.length();

    int numberOfLineDrops = getNumberOfLineDrops(text);

    if (originalPos != null) {
      RangeFileMapping map;
      if (bogusURL) {
        map =
            new RangeFileMapping(
                startOffset,
                endOffset,
                currentLine,
                currentLine + numberOfLineDrops,
                originalPos,
                url) {
              @Override
              public Reader getInputStream() throws IOException {
                return new StringReader(text);
              }
            };
      } else {
        map =
            new RangeFileMapping(
                startOffset,
                endOffset,
                currentLine,
                currentLine + numberOfLineDrops,
                originalPos,
                url);
      }
      if (fileMapping == null) {
        fileMapping = map;
      } else {
        fileMapping = new CompositeFileMapping(map, fileMapping);
      }
    }

    currentLine += numberOfLineDrops;
  }

  public void println(String text, Position originalPos, URL url, boolean bogusURL) {
    print(text + '\n', originalPos, url, bogusURL);
  }

  public void print(String text) {
    print(text, null, null, true);
  }

  public void println(String text) {
    print(text + '\n');
  }

  public FileMapping writeToFile(PrintWriter ps) {
    ps.print(source);
    ps.flush();
    return fileMapping;
  }

  public void write(SourceRegion otherRegion) {
    int rangeStart = source.length();
    String text = otherRegion.source.toString();
    source.append(text);
    int rangeEnd = source.length();

    int numberOfLineDrops = getNumberOfLineDrops(text);

    if (otherRegion.fileMapping != null) {
      FileMapping map =
          new NestedRangeMapping(
              rangeStart,
              rangeEnd,
              currentLine,
              currentLine + numberOfLineDrops,
              otherRegion.fileMapping);
      if (fileMapping == null) {
        fileMapping = map;
      } else {
        fileMapping = new CompositeFileMapping(map, fileMapping);
      }
    }

    currentLine += numberOfLineDrops;
  }

  public void dump(PrintWriter ps) {
    ps.println(source);
  }

  private static int getNumberOfLineDrops(String text) {
    int ret = 0;
    int i = text.indexOf('\n');
    while (i != -1) {
      ret++;
      if (i < text.length() - 1) {
        i = text.indexOf('\n', i + 1);
      } else {
        break; // CR was the the last character.
      }
    }
    return ret;
  }
}
