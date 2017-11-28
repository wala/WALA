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
package com.ibm.wala.ipa.callgraph.impl;

import java.util.HashSet;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * Includes all application methods in an analysis scope as entrypoints.
 */
public class AllApplicationEntrypoints extends HashSet<Entrypoint> {

  private static final long serialVersionUID = 6541081454519490199L;
  private final static boolean DEBUG = false;

  /**
   * @param scope governing analyais scope
   * @param cha governing class hierarchy
   * @throws IllegalArgumentException if cha is null
   */
  public AllApplicationEntrypoints(AnalysisScope scope, final IClassHierarchy cha) {

    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    for (IClass klass : cha) {
      if (!klass.isInterface()) {
        if (isApplicationClass(scope, klass)) {
          for (IMethod method : klass.getDeclaredMethods()) {
            if (!method.isAbstract()) {
              add(new ArgumentTypeEntrypoint(method, cha));
            }
          }
        }
      }
    }
    if (DEBUG) {
      System.err.println((getClass() + "Number of EntryPoints:" + size()));
    }

  }

  /**
   * @return true iff klass is loaded by the application loader.
   */
  private static boolean isApplicationClass(AnalysisScope scope, IClass klass) {
    return scope.getApplicationLoader().equals(klass.getClassLoader().getReference());
  }
}
