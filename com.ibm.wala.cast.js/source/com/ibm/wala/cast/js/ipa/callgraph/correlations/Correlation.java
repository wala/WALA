/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

/**
 * A correlation exists between a dynamic property read r and a dynamic property write w such that
 * the value read in r may flow into w, and r and w are guaranteed to access a property of the same name.
 * 
 * We additionally track the set of local variables the value read in r may flow through before reaching
 * w. These will be candidates for localisation when extracting the correlation into a closure.
 * 
 * @author mschaefer
 *
 */
public abstract class Correlation {
  private final String indexName;
  private final Set<String> flownThroughLocals;
  
  protected Correlation(String indexName, Set<String> flownThroughLocals) {
    this.indexName = indexName;
    this.flownThroughLocals = new HashSet<>(flownThroughLocals);
  }
  
  public String getIndexName() {
    return indexName;
  }
  
  public Set<String> getFlownThroughLocals() {
    return flownThroughLocals;
  }
  
	public abstract Position getStartPosition(SSASourcePositionMap positions);
	public abstract Position getEndPosition(SSASourcePositionMap positions);
	public abstract String pp(SSASourcePositionMap positions);
	public abstract <T> T accept(CorrelationVisitor<T> visitor);
}
