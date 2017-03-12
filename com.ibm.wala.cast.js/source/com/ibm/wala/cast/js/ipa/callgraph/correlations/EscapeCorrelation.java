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
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * An escape correlation conservatively captures inter-procedural correlated pairs: for a dynamic property
 * read <i>r</i> of the form <code>e[p]</code>, if both the result of <i>r</i> and the value of <code>p</code>
 * flow into a function call <i>c</i>, we consider <i>r</i> and <i>c</i> to be a correlated pair to account
 * for the fact that the function called by <i>c</i> may perform a write of property <code>p</code>. 
 * 
 * @author mschaefer
 *
 */
public class EscapeCorrelation extends Correlation {
	private final AbstractReflectiveGet get;
	private final SSAAbstractInvokeInstruction invoke;
	
	public EscapeCorrelation(AbstractReflectiveGet get, SSAAbstractInvokeInstruction invoke,
	                         String indexName, Set<String> flownThroughLocals) {
	  super(indexName, flownThroughLocals);
		this.get = get;
		this.invoke = invoke;
	}
	
	@Override
	public Position getStartPosition(SSASourcePositionMap positions) {
		return positions.getPosition(get);
	}
	
	@Override
	public Position getEndPosition(SSASourcePositionMap positions) {
	  return positions.getPosition(invoke);
	}
	
	public int getNumberOfArguments() {
	  return invoke.getNumberOfParameters() - 2; // deduct one for the function object, one for the receiver
	}
	
	@Override
	public String pp(SSASourcePositionMap positions) {
		return get + "@" + positions.getPosition(get) + " [" + getIndexName() + "] ->? " + invoke + "@" + positions.getPosition(invoke);
	}
	
	@Override
	public <T> T accept(CorrelationVisitor<T> visitor) {
	  return visitor.visitEscapeCorrelation(this);
	}
}
