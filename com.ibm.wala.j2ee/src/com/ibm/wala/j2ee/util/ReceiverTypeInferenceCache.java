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
package com.ibm.wala.j2ee.util;

import java.util.Map;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.ref.CacheReference;

/**
 * A soft cache of results of type inference
 * 
 * @author sfink
 */
public class ReceiverTypeInferenceCache {

  private final AnalysisCache analysisCache;

  public ReceiverTypeInferenceCache(AnalysisCache cache) {
    this.analysisCache = cache;
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
//        options.setUsePiNodes(true);
        IR ir = analysisCache.getSSACache().findOrCreateIR(n.getMethod(), n.getContext(),  options);
        TypeInference T = TypeInference.make(ir, false);

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
