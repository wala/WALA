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

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import java.io.IOException;

/** */
public interface ClassLoaderFactory {

  /**
   * Return a class loader corresponding to a given class loader identifier. Create one if
   * necessary.
   *
   * @param classLoaderReference identifier for the desired class loader
   * @return IClassLoader
   */
  IClassLoader getLoader(
      ClassLoaderReference classLoaderReference, IClassHierarchy cha, AnalysisScope scope)
      throws IOException;
}
