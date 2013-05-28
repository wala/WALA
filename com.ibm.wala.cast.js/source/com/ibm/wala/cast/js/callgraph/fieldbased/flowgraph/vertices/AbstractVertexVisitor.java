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
 * Visitor class for {@link Vertex}.
 *  
 * @author mschaefer
 */
public class AbstractVertexVisitor<T> implements VertexVisitor<T> {
	public T visitVertex(Vertex vertex) {
		return null;
	}
	
	public T visitVarVertex(VarVertex varVertex) {
		return visitVertex(varVertex);
	}

	public T visitPropVertex(PropVertex propVertex) {
		return visitVertex(propVertex);
	}

	public T visitUnknownVertex(UnknownVertex unknownVertex) {
		return visitVertex(unknownVertex);
	}

	public T visitFuncVertex(FuncVertex funcVertex) {
		return visitVertex(funcVertex);
	}

	public T visitParamVertex(ParamVertex paramVertex) {
		return visitVertex(paramVertex);
	}

	public T visitRetVertex(RetVertex retVertex) {
		return visitVertex(retVertex);
	}

	public T visitCalleeVertex(CallVertex calleeVertex) {
		return visitVertex(calleeVertex);
	}

	public T visitLexicalAccessVertex(LexicalVarVertex lexicalAccessVertex) {
		return visitVertex(lexicalAccessVertex);
	}

}
