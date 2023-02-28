/*
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrike.shrikeBT.analysis;

/**
 * This interface provides information about the class hierarchy to some consumer, such as a
 * bytecode verifier.
 *
 * <p>All class names are given in JVM type format, e.g., Ljava/lang/Object;.
 */
public interface ClassHierarchyProvider {
  /**
   * @return the superclass of the given class, or null if the superclass is not known or cl is
   *     java.lang.Object
   */
  String getSuperClass(String cl);

  /** @return the superinterfaces of the given class, or null if they are not known */
  String[] getSuperInterfaces(String cl);

  /**
   * @return the complete set of direct subclasses or implementors of cl, or null if the complete
   *     set is not known
   */
  String[] getSubClasses(String cl);

  /** @return whether or not cl is an interface, or Constants.MAYBE if not known */
  int isInterface(String cl);
}
