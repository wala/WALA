/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.classLoader;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.ParameterReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import java.util.Collection;

/** */
public interface IParameter extends IMember, ContextItem {

  /** @return the canonical MethodReference of the declaring method */
  MethodReference getMethodReference();

  /** @return the name of this parameter */
  @Override
  Atom getName();

  /** @return the canonical TypeReference of the declared type of the parameter */
  TypeReference getFieldTypeReference();

  /** @return canonical ParameterReference representing this parameter */
  ParameterReference getReference();

  /** Is this parameter final? */
  boolean isFinal();

  /** Get the annotations on this parameter, if any */
  @Override
  Collection<Annotation> getAnnotations();
}
