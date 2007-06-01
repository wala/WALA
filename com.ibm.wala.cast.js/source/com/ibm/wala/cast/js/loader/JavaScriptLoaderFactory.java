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
package com.ibm.wala.cast.js.loader;

import com.ibm.wala.cast.js.translator.*;
import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.cast.loader.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.types.*;

public class JavaScriptLoaderFactory extends SingleClassLoaderFactory {
  private final JavaScriptTranslatorFactory translatorFactory;

  public JavaScriptLoaderFactory(JavaScriptTranslatorFactory factory) {
    this.translatorFactory = factory;
  }

  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new JavaScriptLoader( cha, translatorFactory );
  }

  public ClassLoaderReference getTheReference() {
    return JavaScriptTypes.jsLoader;
  }
}
