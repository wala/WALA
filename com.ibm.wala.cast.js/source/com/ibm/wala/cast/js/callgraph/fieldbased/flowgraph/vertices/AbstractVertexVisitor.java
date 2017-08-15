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
	public T visitVertex() {
		return null;
	}
	
	@Override
  public T visitVarVertex(VarVertex varVertex) {
		return visitVertex();
	}

	@Override
  public T visitPropVertex(PropVertex propVertex) {
		return visitVertex();
	}

	@Override
  public T visitUnknownVertex(UnknownVertex unknownVertex) {
		return visitVertex();
	}

	@Override
  public T visitFuncVertex(FuncVertex funcVertex) {
		return visitVertex();
	}

	 @Override
	  public T visitCreationSiteVertex(CreationSiteVertex csVertex) {
	    return visitVertex();
	  }

	@Override
  public T visitParamVertex(ParamVertex paramVertex) {
		return visitVertex();
	}

	@Override
  public T visitRetVertex(RetVertex retVertex) {
		return visitVertex();
	}

	@Override
	public T visitArgVertex(ArgVertex argVertex) {
	  return visitVertex();
	}

	@Override
  public T visitCalleeVertex(CallVertex calleeVertex) {
		return visitVertex();
	}

	@Override
  public T visitLexicalAccessVertex(LexicalVarVertex lexicalAccessVertex) {
		return visitVertex();
	}

	 @Override
	  public T visitGlobalVertex(GlobalVertex globalVertex) {
	    return visitVertex();
	  }

	 @Override
	 public T visitPrototypeVertex(PrototypeFieldVertex protoVertex) {
	   return visitVertex();
	 }
}
