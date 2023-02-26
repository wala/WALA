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
  T visitVarVertex(VarVertex varVertex);

  T visitPropVertex(PropVertex propVertex);

  T visitUnknownVertex(UnknownVertex unknownVertex);

  T visitFuncVertex(FuncVertex funcVertex);

  T visitCreationSiteVertex(CreationSiteVertex csVertex);

  T visitParamVertex(ParamVertex paramVertex);

  T visitRetVertex(RetVertex retVertex);

  T visitCalleeVertex(CallVertex calleeVertex);

  T visitLexicalAccessVertex(LexicalVarVertex lexicalAccessVertex);

  T visitArgVertex(ArgVertex argVertex);

  T visitGlobalVertex(GlobalVertex globalVertex);

  T visitPrototypeVertex(PrototypeFieldVertex protoVertex);

  T visitReflectiveCallVertex(ReflectiveCallVertex reflectiveCallVertex);
}
