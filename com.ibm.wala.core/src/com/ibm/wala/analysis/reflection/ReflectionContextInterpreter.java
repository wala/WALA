/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ReflectionSpecification;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * {@link SSAContextInterpreter} to handle all reflection procession.
 * 
 * @author sjfink
 *
 */
public class ReflectionContextInterpreter extends DelegatingSSAContextInterpreter {

  public static ReflectionContextInterpreter createReflectionContextInterpreter(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
      ReflectionSpecification userSpec) {
    return new ReflectionContextInterpreter(cha, options, cache, userSpec);
  }

  private ReflectionContextInterpreter(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache, ReflectionSpecification userSpec) {
    super(new DelegatingSSAContextInterpreter(new ForNameContextInterpreter(), new NewInstanceContextInterpreter(cha)),
        new FactoryBypassInterpreter(options, cache, userSpec));
  }

}
