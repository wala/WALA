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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;

public class PrunedCallGraph implements CallGraph {
	
	private CallGraph cg;
	private Set<CGNode> keep;
	private Map<CGNode,Set<CGNode>> remove = Collections.emptyMap();
	
	public PrunedCallGraph(CallGraph cg, Set<CGNode> keep) {
		this.cg = cg;
		this.keep = keep;
	}

  public PrunedCallGraph(CallGraph cg, Set<CGNode> keep, Map<CGNode,Set<CGNode>> remove) {
    this(cg, keep);
    this.remove = remove;
  }
  
	@Override
	public void removeNodeAndEdges(CGNode n) throws UnsupportedOperationException {
		cg.removeNodeAndEdges(n);
		keep.remove(n);
		remove.remove(n);
	}

	@Override
	public Iterator<CGNode> iterator() {
		Iterator<CGNode> tmp = cg.iterator();
		Collection<CGNode> col = new LinkedList<>();
		while (tmp.hasNext()) {
			CGNode n = tmp.next();
			if (keep.contains(n)) {
				col.add(n);
			}
		}
		
		return col.iterator();
	}

	@Override
	public int getNumberOfNodes() {
		return keep.size();
	}

	@Override
	public void addNode(CGNode n) {
		cg.addNode(n);
		keep.add(n);
	}

	@Override
	public void removeNode(CGNode n) throws UnsupportedOperationException {
		cg.removeNode(n);
		keep.remove(n);
    remove.remove(n);
	}

	@Override
	public boolean containsNode(CGNode n) {
		return cg.containsNode(n) && keep.contains(n);
	}

	private boolean removedEdge(CGNode src, CGNode target) {
	  return remove.containsKey(src) && remove.get(src).contains(target);
	}
	
	@Override
	public Iterator<CGNode> getPredNodes(CGNode n) {
		Iterator<CGNode> tmp = cg.getPredNodes(n);
		Collection<CGNode> col = new LinkedList<>();
		while (tmp.hasNext()) {
			CGNode no = tmp.next();
			if (keep.contains(no) && !removedEdge(no, n)) {
				col.add(no);
			}
		}
		
		return col.iterator();
	}

	@Override
	public int getPredNodeCount(CGNode n) {
		Iterator<CGNode> tmp = cg.getPredNodes(n);
		int cnt = 0;
		while (tmp.hasNext()) {
			CGNode no = tmp.next();
			if (keep.contains(no) && !removedEdge(no, n)) {
				cnt++;
			}
		}
		return cnt;
	}


	@Override
	public Iterator<CGNode> getSuccNodes(CGNode n) {
		Iterator<CGNode> tmp = cg.getSuccNodes(n);
		Collection<CGNode> col = new LinkedList<>();
		while (tmp.hasNext()) {
			CGNode no = tmp.next();
			if (keep.contains(no) && !removedEdge(n, no)) {
				col.add(no);
			}
		}
		
		return col.iterator();
	}


	@Override
	public int getSuccNodeCount(CGNode n) {
		Iterator<CGNode> tmp = cg.getSuccNodes(n);
		int cnt = 0;
		while (tmp.hasNext()) {
			CGNode no = tmp.next();
			if (keep.contains(no) && !removedEdge(n, no)) {
				cnt++;
			}
		}
		return cnt;
	}


	@Override
	public void addEdge(CGNode src, CGNode dst) {
		if (keep.contains(src) && keep.contains(dst)){
			cg.addEdge(src, dst);
		}
	}


	@Override
	public void removeEdge(CGNode src, CGNode dst) throws UnsupportedOperationException {
		cg.removeEdge(src, dst);
	}


	@Override
	public void removeAllIncidentEdges(CGNode node) throws UnsupportedOperationException {
		cg.removeAllIncidentEdges(node);
	}


	@Override
	public void removeIncomingEdges(CGNode node) throws UnsupportedOperationException {
		cg.removeIncomingEdges(node);
	}


	@Override
	public void removeOutgoingEdges(CGNode node) throws UnsupportedOperationException {
		cg.removeOutgoingEdges(node);
	}


	@Override
	public boolean hasEdge(CGNode src, CGNode dst) {
		return cg.hasEdge(src, dst) && keep.contains(src) &&  keep.contains(dst) && !removedEdge(src, dst);
	}


	@Override
	public int getNumber(CGNode N) {
		if (keep.contains(N)) {
			return cg.getNumber(N);
		} else {
			return -1;
		}
		
	}


	@Override
	public CGNode getNode(int number) {
		if(keep.contains(cg.getNode(number))) {
			return cg.getNode(number);
		} else {
			return null;
		}
	}


	@Override
	public int getMaxNumber() {
		return cg.getMaxNumber();
	}


	@Override
	public Iterator<CGNode> iterateNodes(IntSet s) {
		Iterator<CGNode> tmp = cg.iterateNodes(s);
		Collection<CGNode> col = new LinkedList<>();
		while (tmp.hasNext()) {
			CGNode n = tmp.next();
			if (keep.contains(n)) {
				col.add(n);
			}
		}
		
		return col.iterator();
	}


	@Override
	public IntSet getSuccNodeNumbers(CGNode node) {
		if (!keep.contains(node)){
			return null;
		}
		IntSet tmp = cg.getSuccNodeNumbers(node);
		BitVectorIntSet kp = new BitVectorIntSet();
		for (CGNode n : keep) {
		  if (!removedEdge(node, n)) {
		    kp.add(getNumber(n));
		  }
		}
		return tmp.intersection(kp);
	}


	@Override
	public IntSet getPredNodeNumbers(CGNode node) {
		if (!keep.contains(node)){
			return null;
		}
		if (!keep.contains(node)){
			return null;
		}
		IntSet tmp = cg.getPredNodeNumbers(node);
		BitVectorIntSet kp = new BitVectorIntSet();
		for (CGNode n : keep) {
      if (!removedEdge(n, node)) {
        kp.add(getNumber(n));
      }
		}
		return tmp.intersection(kp);
	}


	@Override
	public CGNode getFakeRootNode() {
		if (keep.contains(cg.getFakeRootNode())) {
			return cg.getFakeRootNode();
		} else {
			return null;
		}
	}

	@Override
	 public CGNode getFakeWorldClinitNode() {
	    if (keep.contains(cg.getFakeWorldClinitNode())) {
	      return cg.getFakeRootNode();
	    } else {
	      return null;
	    }
	  }


	@Override
	public Collection<CGNode> getEntrypointNodes() {
		Collection<CGNode> tmp = cg.getEntrypointNodes();
		Set<CGNode> ret = new HashSet<>();
		for (CGNode n : tmp) {
			if (keep.contains(n)) {
				ret.add(n);
			}
		}
		return ret;
	}


	@Override
	public CGNode getNode(IMethod method, Context C) {
		if(keep.contains(cg.getNode(method, C))) {
			return cg.getNode(method, C);
		} else {
			return null;
		}
	}


	@Override
	public Set<CGNode> getNodes(MethodReference m) {
		Set<CGNode> tmp = cg.getNodes(m);
		Set<CGNode> ret = new HashSet<>();
		for (CGNode n : tmp) {
			if (keep.contains(n)) {
				ret.add(n);
			}
		}
		return ret;
	}


	@Override
	public IClassHierarchy getClassHierarchy() {
		return cg.getClassHierarchy();
	}


	@Override
	public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site) {
		if (!keep.contains(node)){
			return null;
		}
		Set<CGNode> tmp = cg.getPossibleTargets(node, site);
		Set<CGNode> ret = new HashSet<>();
		for (CGNode n : tmp) {
			if (keep.contains(n) && !removedEdge(node, n)) {
				ret.add(n);
			}
		}
		return ret;
	}


	@Override
	public int getNumberOfTargets(CGNode node, CallSiteReference site) {
		if (!keep.contains(node)){
			return -1;
		}
		return getPossibleTargets(node, site).size();
	}


	@Override
	public Iterator<CallSiteReference> getPossibleSites(CGNode src,	CGNode target) {
		if (!(keep.contains(src) && keep.contains(target)) || removedEdge(src, target)){
			return null;
		}
		return cg.getPossibleSites(src, target);
	}

}
