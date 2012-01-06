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

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

/**
 * A correlation exists between a dynamic property read r and a dynamic property write w such that
 * the value read in r may flow into w, and r and w are guaranteed to access a property of the same name.
 * 
 * @author mschaefer
 *
 */
public abstract class Correlation { 
  private final String indexName;
  
  protected Correlation(String indexName) {
    this.indexName = indexName;
  }
  
  public String getIndexName() {
    return indexName;
  }
  
	public abstract Position getStartPosition(SSASourcePositionMap positions);
	public abstract Position getEndPosition(SSASourcePositionMap positions);
	public abstract String pp(SSASourcePositionMap positions);
	public abstract <T> T accept(CorrelationVisitor<T> visitor);
}