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
package com.ibm.wala.stringAnalysis.translator;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.stringAnalysis.grammar.*;

public interface ICalleeResolver {
    public Set<GR> resolve(PropagationCallGraphBuilder builder, CallGraph cg, CGNode node, GR gr, GRule rule, CalleeMap calleeMap);
}
