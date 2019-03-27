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
package com.ibm.wala.cast.js.ipa.summaries;

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class JavaScriptSummary extends MethodSummary {

  private final int declaredParameters;

  public JavaScriptSummary(MethodReference ref, int declaredParameters) {
    super(ref);
    this.declaredParameters = declaredParameters;
    addStatement(
        JavaScriptLoader.JS
            .instructionFactory()
            .NewInstruction(
                getNumberOfStatements(),
                declaredParameters + 1,
                NewSiteReference.make(getNumberOfStatements(), JavaScriptTypes.Array)));
  }

  @Override
  public int getNumberOfParameters() {
    return declaredParameters;
  }

  @Override
  public TypeReference getParameterType(int i) {
    return JavaScriptTypes.Root;
  }
}
