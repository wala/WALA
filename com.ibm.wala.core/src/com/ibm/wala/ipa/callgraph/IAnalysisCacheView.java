/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;

public interface IAnalysisCacheView {

  void invalidate(IMethod method, Context C);

  IRFactory<IMethod> getIRFactory();

  /**
   * Find or create an IR for the method using the {@link Everywhere} context and default {@link SSAOptions}
   */
  IR getIR(IMethod method);

  /**
   * Find or create a DefUse for the IR using the {@link Everywhere} context 
   */
  DefUse getDefUse(IR ir);

  IR getIR(IMethod method, Context context);

}