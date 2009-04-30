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
package com.ibm.wala.cast.loader;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;

public abstract class SingleClassLoaderFactory implements ClassLoaderFactory {
  private IClassLoader THE_LOADER = null;

  public IClassLoader getLoader(ClassLoaderReference classLoaderReference,
				IClassHierarchy cha, 
				AnalysisScope scope)
  {
    if (THE_LOADER == null) {
      THE_LOADER = makeTheLoader(cha);
      try {
	THE_LOADER.init(scope.getModules(getTheReference()));
      } catch (java.io.IOException e) {
	Assertions.UNREACHABLE();
      }
    }

    assert classLoaderReference.equals(getTheReference());
    
    return THE_LOADER;
  }

  public IClassLoader getTheLoader() {
    return THE_LOADER;
  }

  public abstract ClassLoaderReference getTheReference();

  protected abstract IClassLoader makeTheLoader(IClassHierarchy cha);

}
