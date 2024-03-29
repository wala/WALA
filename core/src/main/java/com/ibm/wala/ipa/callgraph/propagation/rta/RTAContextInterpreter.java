/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.propagation.rta;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHAContextInterpreter;
import com.ibm.wala.types.FieldReference;
import java.util.Iterator;

/** This object will analyze a method in a context and return information needed for RTA. */
public interface RTAContextInterpreter extends CHAContextInterpreter {

  /**
   * @return an Iterator of the types that may be allocated by a given method in a given context.
   */
  @Override
  Iterator<NewSiteReference> iterateNewSites(CGNode node);

  /**
   * @return iterator of FieldReference
   */
  Iterator<FieldReference> iterateFieldsRead(CGNode node);

  /**
   * @return iterator of FieldReference
   */
  Iterator<FieldReference> iterateFieldsWritten(CGNode node);

  /**
   * record that the "factory" method of a node should be interpreted to allocate a particular
   * class.
   *
   * <p>TODO: this is a little ugly, is there a better place to move this?
   *
   * @return true iff a NEW type was recorded, false if the type was previously recorded.
   */
  boolean recordFactoryType(CGNode node, IClass klass);
}
