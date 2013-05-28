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
 * Class representing a flow graph vertex. Vertices should never be instantiated
 * directly, but rather generated through a {@link VertexFactory}.
 * 
 * @author mschaefer
 *
 */
public abstract class Vertex {
	public abstract <T> T accept(VertexVisitor<T> visitor);
}
