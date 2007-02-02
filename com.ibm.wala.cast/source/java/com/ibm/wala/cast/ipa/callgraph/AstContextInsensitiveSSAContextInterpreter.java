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
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.loader.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.cfa.*;
import com.ibm.wala.ipa.cha.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.warnings.WarningSet;

import java.util.*;

public class AstContextInsensitiveSSAContextInterpreter
  extends ContextInsensitiveSSAInterpreter 
{

  public AstContextInsensitiveSSAContextInterpreter(AnalysisOptions options, ClassHierarchy cha) {
    super(options, cha);
  }

  public boolean understands(IMethod method, Context context) {
    return method instanceof AstMethod;
  }

  public Iterator iterateCallSites(CGNode N, WarningSet warnings) {
    IR ir = getIR(N, warnings);
    if (ir == null) {
      return EmptyIterator.instance();
    } else {
      return ir.iterateCallSites();
    }
  }

}
