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
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAInstruction;

/**
 *  This abstract instruction extends the abstract lexical invoke with
 * functionality to support invocations with a fixed number of
 * arguments---the only case in some languages and a common case even
 * in scripting languages.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public abstract class FixedParametersLexicalInvokeInstruction
    extends AbstractLexicalInvoke 
{

  /**
   * The value numbers of the arguments passed to the call.  For non-static methods,
   * params[0] == this.  If params == null, this should be a static method with
   * no parameters.
   */
  private final int[] params;

  public FixedParametersLexicalInvokeInstruction(int result, int[] params, int exception, CallSiteReference site) {
    super(result, exception, site);
    this.params = params;
  }

  /**
   * Constructor InvokeInstruction. This case for void return values
   * @param i
   * @param params
   */
  public FixedParametersLexicalInvokeInstruction(int[] params, int exception, CallSiteReference site) {
    this(-1, params, exception, site);
  }

  protected FixedParametersLexicalInvokeInstruction(int result, int[] params, int exception, CallSiteReference site, Access[] lexicalReads, Access[] lexicalWrites) {
    super(result, exception, site, lexicalReads, lexicalWrites);
    this.params = params;
  }

  protected abstract SSAInstruction copyInstruction(int result, int[] params, int exception, Access[] lexicalReads, Access[] lexicalWrites);

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    int newParams[] = params;
    Access[] reads = lexicalReads;

    if (uses != null) {
      int i = 0;

      newParams = new int[ params.length ];
      for(int j = 0; j < newParams.length; j++)
	newParams[j] = uses[i++];

      if (lexicalReads != null) {
	reads = new Access[ lexicalReads.length ];
	for(int j = 0; j < reads.length; j++)
	  reads[j] = new Access(lexicalReads[j].variableName, lexicalReads[j].variableDefiner, uses[i++]);
      }
    }

    int newLval = result;
    int newExp = exception;
    Access[] writes = lexicalWrites;
    
    if (defs != null) {
      int i = 0;
      if (result != -1) newLval = defs[i++];
      newExp = defs[i++];
      
      if (lexicalWrites != null) {
	writes = new Access[ lexicalWrites.length ];
	for(int j = 0; j < writes.length; j++)
	  writes[j] = new Access(lexicalWrites[j].variableName, lexicalWrites[j].variableDefiner, defs[i++]);
      }
    }

    return copyInstruction(newLval, newParams, newExp, reads, writes);
  }

  public int getNumberOfParameters() {
    if (params == null) {
      return 0;
    } else {
      return params.length;
    }
  }

  /**
   * @see com.ibm.wala.ssa.Instruction#getUse(int)
   */
  public int getUse(int j) {
    if (j < getNumberOfParameters())
      return params[j];
    else {
      return super.getUse(j);
    }
  }

}
