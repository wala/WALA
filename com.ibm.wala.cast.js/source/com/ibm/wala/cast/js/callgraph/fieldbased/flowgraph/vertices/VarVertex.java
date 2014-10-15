/******************************************************************************
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;


/**
 * A variable vertex represents an SSA variable inside a given function.
 * 
 * @author mschaefer
 *
 */
public final class VarVertex extends Vertex implements PointerKey {
	private final FuncVertex func;
	private final int valueNumber;
	
	VarVertex(FuncVertex func, int valueNumber) {
		this.func = func;
		this.valueNumber = valueNumber;
	}
	
	public FuncVertex getFunction() {
	  return func;
	}
	
	public int getValueNumber() {
	  return valueNumber;
	}

	@Override
	public <T> T accept(VertexVisitor<T> visitor) {
		return visitor.visitVarVertex(this);
	}

	@Override
	public String toString() {
	  return "Var(" + func + ", " + valueNumber + ")";
	}
}
