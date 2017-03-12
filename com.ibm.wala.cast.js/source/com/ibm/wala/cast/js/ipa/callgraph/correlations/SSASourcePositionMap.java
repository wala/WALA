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

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Utility class used by {@link CorrelationSummary} to map SSA instructions to source positions. 
 * 
 * @author mschaefer
 *
 */
public class SSASourcePositionMap {
	private final AstMethod method;
	private final OrdinalSetMapping<SSAInstruction> instrIndices;
	
	public SSASourcePositionMap(AstMethod method, OrdinalSetMapping<SSAInstruction> instrIndices) {
		this.method = method;
		this.instrIndices = instrIndices;
	}
	
	public Position getPosition(SSAInstruction inst) {
		return method.getSourcePosition(instrIndices.getMappedIndex(inst));
	}
}
