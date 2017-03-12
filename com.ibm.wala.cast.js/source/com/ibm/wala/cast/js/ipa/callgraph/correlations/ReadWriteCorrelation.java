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

import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AbstractReflectiveGet;
import com.ibm.wala.cast.ir.ssa.AbstractReflectivePut;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

/**
 * The most basic form of correlation: an intra-procedurally correlated pair of a dynamic property read
 * and a dynamic property write.
 * 
 * @author mschaefer
 *
 */
public class ReadWriteCorrelation extends Correlation {
	private final AbstractReflectiveGet get;
	private final AbstractReflectivePut put;
	
	public ReadWriteCorrelation(AbstractReflectiveGet get, AbstractReflectivePut put,
	                            String indexName, Set<String> flownThroughLocals) {
	  super(indexName, flownThroughLocals);
		this.get = get;
		this.put = put;
	}

	@Override
	public Position getStartPosition(SSASourcePositionMap positions) {
		return positions.getPosition(get);
	}
	
	@Override
	public Position getEndPosition(SSASourcePositionMap positions) {
	  return positions.getPosition(put);
	}
	
	@Override
	public String pp(SSASourcePositionMap positions) {
		return get + "@" + positions.getPosition(get) + " [" + getIndexName() + "]-> " + put + "@" + positions.getPosition(put);
	}
	
	@Override
	public <T> T accept(CorrelationVisitor<T> visitor) {
	  return visitor.visitReadWriteCorrelation(this);
	}
}
