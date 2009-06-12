/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MemberReference;

/**
 * This interface holds information describing reflection behavior specified by the user.
 */
public interface ReflectionSpecification {

  /**
   * @return the TypeAbstraction which represents the set of types allocated by the new instance at the specified bytecode index; or
   *         null if not specified.
   */
  TypeAbstraction getTypeForNewInstance(MemberReference method, int bcIndex, IClassHierarchy cha);
}
