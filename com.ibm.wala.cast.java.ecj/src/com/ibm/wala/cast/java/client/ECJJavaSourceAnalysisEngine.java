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
package com.ibm.wala.cast.java.client;

import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.config.SetOfClasses;

public class ECJJavaSourceAnalysisEngine extends JavaSourceAnalysisEngine<InstanceKey> {

  @Override
  protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions) {
    return new ECJClassLoaderFactory(exclusions);
  }

}
