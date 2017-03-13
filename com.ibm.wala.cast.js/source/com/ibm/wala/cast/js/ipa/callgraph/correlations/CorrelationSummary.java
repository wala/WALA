/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * A utility class holding information about correlations identified by a {@link CorrelationFinder}.
 * 
 * @author mschaefer
 *
 */
public final class CorrelationSummary {
  private final SSASourcePositionMap positions;
  private final Set<Correlation> correlations = HashSetFactory.make();

  public CorrelationSummary(IMethod method, OrdinalSetMapping<SSAInstruction> instrIndices) {
    positions = new SSASourcePositionMap((AstMethod)method, instrIndices);
  }

  public void addCorrelation(Correlation correlation) {
    correlations.add(correlation);
  }

  public List<Pair<Position, String>> pp() {
    List<Pair<Position, String>> res = new ArrayList<>();
    for(Correlation correlation : correlations) {
      res.add(Pair.make(correlation.getStartPosition(positions), correlation.pp(positions)));
    }
    return res;
  }

  public Set<Correlation> getCorrelations() {
    return correlations;
  }
  
  public boolean isEmpty() {
    return correlations.isEmpty();
  }

  public SSASourcePositionMap getPositions() {
    return positions;
  }
}
