/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.typeInference;

import java.util.Map;

import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.CacheReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * A soft cache of results of type inference
 * 
 * @author sfink
 */
public class ReceiverTypeInferenceCache {

  private final WarningSet warnings;

  private final IClassHierarchy cha;

  private final AnalysisOptions options;

  public ReceiverTypeInferenceCache(IClassHierarchy cha, AnalysisOptions options, WarningSet warnings) {
    this.warnings = warnings;
    this.options = options;
    this.cha = cha;

  }

  /**
   * A cache of TypeInference results; a mapping from CGNode ->
   * ReceiverTypeInference
   */
  private final Map<CGNode, Object> typeInferenceMap = HashMapFactory.make();

  /**
   * @param n
   *          node
   * @return null if unable to perform type inference
   */
  public ReceiverTypeInference findOrCreate(CGNode n) {
    Object ref = typeInferenceMap.get(n);
    ReceiverTypeInference result = (ReceiverTypeInference) CacheReference.get(ref);
    try {
      if (result == null) {
        SSAOptions options = SSAOptions.defaultOptions();
        options.setUsePiNodes(true);
        IR ir = this.options.getSSACache().findOrCreateIR(n.getMethod(), n.getContext(), cha, options, warnings);
        TypeInference T = new TypeInference(ir);
        T.solve();

        result = new ReceiverTypeInference(T);
        ref = CacheReference.make(result);
        typeInferenceMap.put(n, ref);
      }
      return result;
    } catch (ClassCastException e) {
      // this might happen if it's not a ShrikeCTMethodWrapper
      return null;
    }
  }

}
