/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import java.util.List;

public class TwoLevelExtractionRegion extends ExtractionRegion {
  private final int start_inner, end_inner;

  public TwoLevelExtractionRegion(
      int start,
      int end,
      int start_inner,
      int end_inner,
      List<String> parameters,
      List<String> locals) {
    super(start, end, parameters, locals);
    this.start_inner = start_inner;
    this.end_inner = end_inner;
  }

  public int getStartInner() {
    return this.start_inner;
  }

  public int getEndInner() {
    return this.end_inner;
  }
}
