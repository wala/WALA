/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import java.util.Iterator;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchyDweller;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * interface for an SDG
 * 
 * @author sjfink
 *
 */
public interface ISDG extends NumberedGraph<Statement>, IClassHierarchyDweller {

  ControlDependenceOptions getCOptions();

  PDG getPDG(CGNode node);

  Iterator<? extends Statement> iterateLazyNodes();

}
