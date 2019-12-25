/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.html;

import com.ibm.wala.cast.js.html.RangeFileMapping.Range;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

public class NestedRangeMapping implements FileMapping {
  private final Range range;
  private final FileMapping innerMapping;

  public NestedRangeMapping(
      int rangeStart,
      int rangeEnd,
      int rangeStartingLine,
      int rangeEndingLine,
      FileMapping innerMapping) {
    assert innerMapping != null;
    this.range = new Range(rangeStart, rangeEnd, rangeStartingLine, rangeEndingLine);
    this.innerMapping = innerMapping;
  }

  @Override
  public IncludedPosition getIncludedPosition(final Position pos) {
    if (range.includes(pos)) {
      return innerMapping.getIncludedPosition(
          new AbstractSourcePosition() {
            @Override
            public int getFirstLine() {
              return pos.getFirstLine() - range.getStartingLine() + 1;
            }

            @Override
            public int getLastLine() {
              return pos.getLastLine() == -1
                  ? -1
                  : (pos.getLastLine() - range.getStartingLine() + 1);
            }

            @Override
            public int getFirstCol() {
              return pos.getFirstCol();
            }

            @Override
            public int getLastCol() {
              return pos.getLastCol();
            }

            @Override
            public int getFirstOffset() {
              return pos.getFirstOffset() == -1 ? -1 : (pos.getFirstOffset() - range.getStart());
            }

            @Override
            public int getLastOffset() {
              return pos.getLastOffset() == -1 ? -1 : (pos.getLastOffset() - range.getStart());
            }

            @Override
            public URL getURL() {
              return pos.getURL();
            }

            @Override
            public Reader getReader() throws IOException {
              return pos.getReader();
            }
          });
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return range + "(" + innerMapping + ')';
  }
}
