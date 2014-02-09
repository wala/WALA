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

import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.collections.Pair;

/**
 * A function vertex represents a function object (or, more precisely, all function objects
 * arising from a single function expression or declaration).
 * 
 * @author mschaefer
 */
public class FuncVertex extends Vertex implements InstanceKey {
	// the IClass representing this function in the class hierarchy
	private final IClass klass;

	FuncVertex(IClass method) {
		this.klass = method;
	}
	
	@Override
  public IClass getConcreteType() {
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

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    assert false;
    return null;
  }
}
