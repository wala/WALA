/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

public interface VertexVisitor<T> {
  public abstract T visitVarVertex(VarVertex varVertex);

  public abstract T visitPropVertex(PropVertex propVertex);

  public abstract T visitUnknownVertex(UnknownVertex unknownVertex);

  public abstract T visitFuncVertex(FuncVertex funcVertex);

  public abstract T visitCreationSiteVertex(CreationSiteVertex csVertex);

  public abstract T visitParamVertex(ParamVertex paramVertex);

  public abstract T visitRetVertex(RetVertex retVertex);

  public abstract T visitCalleeVertex(CallVertex calleeVertex);

  public abstract T visitLexicalAccessVertex(LexicalVarVertex lexicalAccessVertex);

  public abstract T visitArgVertex(ArgVertex argVertex);

  public abstract T visitGlobalVertex(GlobalVertex globalVertex);

  public abstract T visitPrototypeVertex(PrototypeFieldVertex protoVertex);

  public abstract T visitReflectiveCallVertex(ReflectiveCallVertex reflectiveCallVertex);
}
