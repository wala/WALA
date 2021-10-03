/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.util;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SourceBuffer {
  private static final class DetailedPosition implements Position {
    private final int endOffset;
    private final int endLine;
    private final int endColumn;
    private final int startColumn;
    private final Position p;
    private final int startLine;
    private final int startOffset;

    private DetailedPosition(
        int endOffset,
        int endLine,
        int endColumn,
        int startColumn,
        Position p,
        int startLine,
        int startOffset) {
      this.endOffset = endOffset;
      this.endLine = endLine;
      this.endColumn = endColumn;
      this.startColumn = startColumn;
      this.p = p;
      this.startLine = startLine;
      this.startOffset = startOffset;
    }

    @Override
    public int getFirstLine() {
      return startLine;
    }

    @Override
    public int getLastLine() {
      return endLine;
    }

    @Override
    public int getFirstCol() {
      return startColumn;
    }

    @Override
    public int getLastCol() {
      return endColumn;
    }

    @Override
    public int getFirstOffset() {
      return startOffset;
    }

    @Override
    public int getLastOffset() {
      return endOffset;
    }

    @Override
    public int compareTo(SourcePosition o) {
      return p.compareTo(o);
    }

    @Override
    public URL getURL() {
      return p.getURL();
    }

    @Override
    public Reader getReader() throws IOException {
      return p.getReader();
    }
  }

  private String[] lines;
  private final Position p;
  public final Position detail;

  public SourceBuffer(Position p) throws IOException {
    this.p = p;

    try (Reader pr = p.getReader()) {
      try (BufferedReader reader = new BufferedReader(pr)) {

        String currentLine = null;
        List<String> lines = new ArrayList<>();
        int offset = 0, line = 0;
        do {
          currentLine = reader.readLine();
          if (currentLine == null) {
            this.lines = new String[0];
            detail = null;
            return;
          }
          offset += (currentLine.length() + 1);
          line++;
        } while (p.getLastOffset() >= 0 ? p.getFirstOffset() > offset : p.getFirstLine() > line);

        // partial first line
        int endOffset = -1;
        int endLine = -1;
        int endColumn = -1;
        int startOffset = -1;
        int startLine = line;
        int startColumn = -1;
        if (p.getLastOffset() >= 0) {
          if (p.getFirstOffset() == offset) {
            startOffset = p.getFirstOffset();
            startColumn = 0;
            lines.add("\n");
          } else {
            startOffset = p.getFirstOffset() - (offset - currentLine.length() - 1);
            startColumn = startOffset;
            if (offset > p.getLastOffset()) {
              endOffset = p.getLastOffset() - (offset - currentLine.length() - 1);
              endLine = line;
              endColumn = endOffset;
              lines.add(currentLine.substring(startOffset, endOffset));
            } else {
              lines.add(currentLine.substring(startOffset));
            }
          }
        } else {
          lines.add(currentLine.substring(Math.max(p.getFirstCol(), 0)));
          startColumn = p.getFirstCol();
        }

        while (p.getLastOffset() >= 0 ? p.getLastOffset() >= offset : p.getLastLine() >= line) {
          currentLine = reader.readLine();

          if (currentLine == null) {
            offset = p.getLastOffset();
            break;
          } else {
            offset += currentLine.length() + 1;
          }
          line++;
          if (p.getLastOffset() >= 0) {
            if (offset > p.getLastOffset()) {
              endColumn = currentLine.length() - (offset - p.getLastOffset()) + 1;
              lines.add(currentLine.substring(0, endColumn));
              endLine = line;
              endOffset = p.getLastOffset();
              break;
            } else {
              lines.add(currentLine);
            }
          } else {
            if (p.getLastLine() == line) {
              lines.add(currentLine.substring(0, p.getLastCol()));
              endColumn = p.getLastCol();
              endLine = line;
              endOffset = offset - (currentLine.length() - p.getLastCol());
              break;
            } else {
              lines.add(currentLine);
            }
          }
        }

        this.lines = lines.toArray(new String[0]);

        this.detail =
            new DetailedPosition(
                endOffset, endLine, endColumn, startColumn, p, startLine, startOffset);

        reader.close();
        pr.close();
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      if (i == lines.length - 1) {
        result.append(lines[i]);
      } else {
        result.append(lines[i]).append('\n');
      }
    }

    return result.toString();
  }

  public void substitute(Position range, String newText) {
    int startLine = range.getFirstLine() - p.getFirstLine();
    int endLine = range.getLastLine() - p.getFirstLine();

    if (startLine != endLine) {
      String newLines[] = new String[lines.length - (endLine - startLine)];
      int i = 0;
      while (i < startLine) {
        newLines[i] = lines[i];
        i++;
      }
      newLines[i++] =
          lines[startLine].substring(0, range.getFirstCol())
              + lines[endLine].substring(range.getLastCol());
      while (i < newLines.length) {
        newLines[i] = lines[i + (endLine - startLine)];
        i++;
      }

      lines = newLines;
      endLine = startLine;

      final Position hack = range;
      range =
          new AbstractSourcePosition() {
            @Override
            public int getFirstLine() {
              return hack.getFirstLine();
            }

            @Override
            public int getLastLine() {
              return hack.getFirstLine();
            }

            @Override
            public int getFirstCol() {
              return hack.getFirstCol();
            }

            @Override
            public int getLastCol() {
              return hack.getFirstCol();
            }

            @Override
            public int getFirstOffset() {
              return hack.getFirstOffset();
            }

            @Override
            public int getLastOffset() {
              return hack.getFirstOffset();
            }

            @Override
            public URL getURL() {
              return hack.getURL();
            }

            @Override
            public Reader getReader() throws IOException {
              return hack.getReader();
            }
          };
    }

    String[] newTextLines = newText.split("\n");

    if (newTextLines.length == 1) {
      lines[startLine] =
          lines[startLine].substring(0, range.getFirstCol())
              + newTextLines[0]
              + lines[startLine].substring(range.getLastCol() + 1);
    } else {
      String[] newLines = new String[lines.length + newTextLines.length - 1];
      int i = 0;
      while (i < startLine) {
        newLines[i] = lines[i];
        i++;
      }

      newLines[i++] = lines[startLine].substring(0, range.getFirstCol()) + newTextLines[0];

      for (int j = 1; j < newTextLines.length - 1; j++) {
        lines[i++] = newTextLines[j];
      }

      newLines[i++] =
          newTextLines[newTextLines.length - 1] + lines[endLine].substring(range.getLastCol() + 1);

      while (i < newLines.length) {
        newLines[i] = lines[i - newTextLines.length + 1];
        i++;
      }

      lines = newLines;
    }
  }
}
