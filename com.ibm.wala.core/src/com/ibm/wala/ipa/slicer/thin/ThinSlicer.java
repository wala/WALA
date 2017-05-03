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
package com.ibm.wala.ipa.slicer.thin;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;

/**
 * A cheap, context-insensitive thin slicer based on reachability over a custom SDG.
 * 
 * This is a prototype implementation; not tuned.
 * 
 * Currently supports backward slices only.
 * 
 * TODO: Introduce a slicer interface common between this and the CS slicer. TODO: This hasn't been tested much. Need regression
 * tests.
 * 
 */
public class ThinSlicer extends CISlicer {

  public ThinSlicer(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
    this(cg, pa, ModRef.make());
  }

  public ThinSlicer(CallGraph cg, PointerAnalysis<InstanceKey> pa, ModRef<InstanceKey> modRef) {
    super(cg, pa, modRef, DataDependenceOptions.NO_HEAP, ControlDependenceOptions.NONE);
  }
}
