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
package com.ibm.wala.cast.js.html;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public class CompositeFileMapping implements FileMapping {
  private final List<FileMapping> mappings = new ArrayList<>(2);
    
  public CompositeFileMapping(FileMapping a, FileMapping b) {
    addMapping(a);
    addMapping(b);    
  }

  private void addMapping(FileMapping fm) {
    if (fm instanceof CompositeFileMapping) {
      mappings.addAll(((CompositeFileMapping)fm).mappings);
    } else {
      mappings.add(fm);
    }
  }
  @Override
  public IncludedPosition getIncludedPosition(Position line) {
    for (FileMapping fm : mappings) {
      IncludedPosition result = fm.getIncludedPosition(line);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return mappings.toString();
  }
}
