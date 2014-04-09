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

import com.ibm.wala.classLoader.IClass;

/**
 * A function vertex represents a function object (or, more precisely, all function objects
 * arising from a single function expression or declaration).
 * 
 * @author mschaefer
 */
public class FuncVertex extends Vertex {
	// the IClass representing this function in the class hierarchy
	private final IClass klass;

	FuncVertex(IClass method) {
		this.klass = method;
	}
	
	public IClass getIClass() {
		return klass;
	}
	
	public String getFullName() {
		return klass.getName().toString();
	}

	@Override
	public <T> T accept(VertexVisitor<T> visitor) {
		return visitor.visitFuncVertex(this);
	}

	@Override
	public String toString() {
		String methodName = klass.getName().toString();
    return "Func(" + methodName.substring(methodName.lastIndexOf('/')+1) + ")";
	}
}
