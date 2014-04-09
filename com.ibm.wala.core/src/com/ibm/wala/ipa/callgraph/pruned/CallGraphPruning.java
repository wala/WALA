package com.ibm.wala.ipa.callgraph.pruned;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;

public final class CallGraphPruning {
	
	public CallGraphPruning(CallGraph cg) {
		this.cg = cg;
	}
	
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
		
		if (DEBUG) {
			 System.out.println("Running optimization with depth: " + depth);
		}
		
		this.marked = new LinkedList<CGNode>();
		this.keep = new HashSet<CGNode>();
		this.visited = new LinkedList<CGNode>();
		this.depth = depth;
		
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
		
		if (checkLoader(root)) {
			keep.addAll(visited);
			addDepth(root);
		}
		visited.removeLast();		
	}
	
	private static boolean checkLoader(CGNode node) {
		return node.getMethod().getDeclaringClass().getClassLoader().getName().equals(AnalysisScope.APPLICATION);
	}
	
	private void addDepth(CGNode node) {
		
		LinkedList<CGNode> A = new LinkedList<CGNode>();
		LinkedList<CGNode> B = new LinkedList<CGNode>();
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
