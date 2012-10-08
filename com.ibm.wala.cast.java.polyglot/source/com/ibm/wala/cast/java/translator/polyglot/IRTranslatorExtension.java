/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Oct 21, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;

public interface IRTranslatorExtension {
  void setSourceLoader(PolyglotSourceLoaderImpl jsli);

  /**
   * @return the identity mapper, for mapping AST nodes to WALA TypeReferences, MethodReferences and FieldReferences. Helps clients
   *         to correlate analysis results to AST nodes.
   */
  PolyglotIdentityMapper getIdentityMapper();

  /**
   */
  CAstRewriterFactory<?,?> getCAstRewriterFactory();

  boolean getReplicateForDoLoops();
}
