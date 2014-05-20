/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.java.translator.polyglot;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.util.config.SetOfClasses;

public class PolyglotJavaSourceAnalysisEngine extends JavaSourceAnalysisEngine {

  public IRTranslatorExtension getTranslatorExtension() {
    return new JavaIRTranslatorExtension();
  }

  @Override
  protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions) {
    return new PolyglotClassLoaderFactory(exclusions, getTranslatorExtension());
  }

}
