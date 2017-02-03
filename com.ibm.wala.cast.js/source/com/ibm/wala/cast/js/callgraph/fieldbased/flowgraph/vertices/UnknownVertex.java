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

/**
 * The unknown vertex is used to model complicated data flow. For instance, thrown
 * exceptions flow into Unknown, and catch blocks read their values from it.
 * 
 * @author mschaefer
 *
 */
public class UnknownVertex extends Vertex {
	public static final UnknownVertex INSTANCE = new UnknownVertex();
	
	private UnknownVertex() {}

	@Override
	public <T> T accept(VertexVisitor<T> visitor) {
		return visitor.visitUnknownVertex(this);
	}
	
	@Override
	public String toString() {
		return "Unknown";
	}
}
