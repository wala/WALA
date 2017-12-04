/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.ipa.callgraph.pruned;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;


public final class CallGraphPruning {
  
  public CallGraphPruning(CallGraph cg) {
		this.cg = cg;
	}
	
	private PruningPolicy pruningPolicy;
	private Set<CGNode> keep;
	private LinkedList<CGNode> visited;
	private List<CGNode> marked;
	private int depth;
	private CallGraph cg;
	
	private boolean DEBUG = false;
	
	/**
	 * Searches all nodes in the callgraph that correspond to a method of the application (and not the system library).
	 * It includes methods from the system library that transitively may call back into the application.
	 * @return Set of relevant callgraph nodes.
	 */
	public Set<CGNode> findApplicationNodes() {
		return findApplicationNodes(0);
	}
	
	/**
	 * Searches all nodes in the callgraph that correspond to a method of the application (and not the system library).
	 * It includes methods from the system library that transitively may call back into the application. Library
	 * methods that do not transitively call back into application methods are cut at the level provided by parameter
	 * depth.
	 * @param depth The level at which non-returning library methods are cut off. 
	 * @return Set of relevant callgraph nodes.
	 */
	public Set<CGNode> findApplicationNodes(final int depth) {
	  return findNodes(depth, ApplicationLoaderPolicy.INSTANCE);
	}
	
	/**
	 * Searches all nodes in the callgraph according to the given pruning policy. It includes all methods which
	 * transitively may call methods which comply to the given pruning policy. All other methods are cut at the level
	 * provided by parameter 'depth'
	 * @param depth the level at which methods which do not comply to the given pruning policy are cut off.
	 * @param policy pruning policy which decides which branches are kept in the call graph
	 * @return set of relevant callgraph nodes
	 */
	public Set<CGNode> findNodes(final int depth, PruningPolicy policy) {
	  if (DEBUG) {
      System.out.println("Running optimization with depth: " + depth);
   }
   
   this.marked = new LinkedList<>();
   this.keep = new HashSet<>();
   this.visited = new LinkedList<>();
   this.depth = depth;
   this.pruningPolicy = policy;
   
   dfs(cg.getFakeRootNode());
   
   return keep;
	}
	
	private void dfs(CGNode root) {
		
		visited.addLast(root);		
		
		Iterator<CGNode> it = cg.getSuccNodes(root);
		while (it.hasNext()) {
			CGNode next = it.next();
			if (!marked.contains(next)) {
				marked.add(next);
				dfs(next);
			} else {
				if (keep.contains(next)) {
					keep.addAll(visited);
				}
			}
		}
		
		if (pruningPolicy.check(root)) {
			keep.addAll(visited);
			addDepth(root);
		}
		visited.removeLast();		
	}
	
	private void addDepth(CGNode node) {
		
		LinkedList<CGNode> A = new LinkedList<>();
		LinkedList<CGNode> B = new LinkedList<>();
		int i = depth;
		A.add(node);
		while (i > 0) {
			
			for (CGNode n : A) {
				Iterator<CGNode> it = cg.getSuccNodes(n);
				while (it.hasNext()) {
					B.add(it.next());
				}
			}
			
			if (DEBUG) {
				 System.out.println("Tiefe: " + B);
			}
			
			keep.addAll(B);
			A.clear();
			A.addAll(B);
			B.clear();			
			i--;
		}
	}
	
	
}
