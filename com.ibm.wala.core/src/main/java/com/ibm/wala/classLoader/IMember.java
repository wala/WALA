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
package com.ibm.wala.classLoader;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchyDweller;
import com.ibm.wala.types.annotations.Annotation;
import java.util.Collection;

/**
 * Basic interface for an object that represents a single Java member (method or field) for analysis
 * purposes.
 */
public interface IMember extends IClassHierarchyDweller {

  /**
   * Return the object that represents the declaring class for this member.
   *
   * @return the object that represents the declaring class for this member.
   */
  IClass getDeclaringClass();

  /** @return the name of this member */
  Atom getName();

  /** Is this member static? */
  boolean isStatic();

  /** Get the annotations on this member, if any */
  Collection<Annotation> getAnnotations();
}
