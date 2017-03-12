/******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NoKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * Extension of {@link CAstRewriter} which allows adding or deleting control flow edges, and keeps track of
 * the current entity.
 * 
 * TODO: This class is an unholy mess. It should be restructured considerably.
 * 
 * @author mschaefer
 *
 */
public abstract class CAstRewriterExt extends CAstRewriter<NodePos, NoKey> {

	/**
	 * A control flow edge to be added to the CFG.
	 * 
	 * @author mschaefer
	 */
	protected static class Edge {
		private CAstNode from;
		private Object label;
		private CAstNode to;

		public Edge(CAstNode from, Object label, CAstNode to) {
			assert from != null;
			assert to != null;
			this.from = from;
			this.label = label;
			this.to = to;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime + from.hashCode();
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			result = prime * result + to.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Edge))
				return false;
			Edge that = (Edge)obj;
			return this.from.equals(that.from) &&
			(this.label == null ? that.label == null : this.label.equals(that.label)) &&
			this.to.equals(that.to);
		}
	}

	private final Map<CAstControlFlowMap, Set<CAstNode>> extra_nodes = HashMapFactory.make();
	private final Map<CAstControlFlowMap, Set<Edge>> extra_flow = HashMapFactory.make();
	private final Map<CAstControlFlowMap, Set<CAstNode>> flow_to_delete = HashMapFactory.make();
	
	// information about an entity to add to the AST
	private static class Entity {
		private final CAstNode anchor;
		private final CAstEntity me;
		public Entity(CAstNode anchor, CAstEntity me) {
			assert me != null;
			this.anchor = anchor;
			this.me = me;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = prime + ((anchor == null) ? 0 : anchor.hashCode());
			return prime * result + me.hashCode();
		}
	}
	private final HashSet<Entity> entities_to_add = HashSetFactory.make();
	private final Stack<CAstEntity> entities = new Stack<>();
	
	public CAstNode addNode(CAstNode node, CAstControlFlowMap flow) {
	  Set<CAstNode> nodes = extra_nodes.get(flow);
	  if(nodes == null)
	    extra_nodes.put(flow, nodes = HashSetFactory.make());
	  nodes.add(node);
	  return node;
	}

	public CAstNode addFlow(CAstNode node, Object label, CAstNode target, CAstControlFlowMap flow) {
		Set<Edge> edges = extra_flow.get(flow);
		if(edges == null)
			extra_flow.put(flow, edges = HashSetFactory.make());
		edges.add(new Edge(node, label, target));
		return node;
	}

	public void deleteFlow(CAstNode node, CAstEntity entity) {
		CAstControlFlowMap flow = entity.getControlFlow();
		Set<CAstNode> tmp = flow_to_delete.get(flow);
		if(tmp == null)
			flow_to_delete.put(flow, tmp = HashSetFactory.make());
		tmp.add(node);
	}
	
	protected boolean isFlowDeleted(CAstNode node, CAstEntity entity) {
		CAstControlFlowMap flow = entity.getControlFlow();
		return flow_to_delete.containsKey(flow) && flow_to_delete.get(flow).contains(node);
	}

	public CAstEntity getCurrentEntity() {
		return entities.peek();
	}
	
	public Iterable<CAstEntity> getEnclosingEntities() {
		return entities;
	}
	
	public void addEntity(CAstNode anchor, CAstEntity entity) {
		entities_to_add.add(new Entity(anchor, entity));
	}

	@Override
	protected Map<CAstNode,Collection<CAstEntity>> copyChildren(CAstNode root, Map<Pair<CAstNode,NoKey>,CAstNode> nodeMap, Map<CAstNode,Collection<CAstEntity>> children) {
		Map<CAstNode, Collection<CAstEntity>> map = super.copyChildren(root, nodeMap, children);
		// extend with local mapping information
		for(Iterator<Entity> es = entities_to_add.iterator(); es.hasNext(); ) {
			Entity e = es.next();
			boolean relevant = NodePos.inSubtree(e.anchor, nodeMap.get(Pair.make(root, null))) || NodePos.inSubtree(e.anchor, root);
			if(relevant) {
				Collection<CAstEntity> c = map.get(e.anchor);
				if(c == null)
					map.put(e.anchor, c = HashSetFactory.make());
				c.add(e.me);
				es.remove();
			}
		}
		return map;
	}
	
	@Override
	protected CAstNode flowOutTo(Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap,
			CAstNode oldSource, Object label, CAstNode oldTarget,
			CAstControlFlowMap orig, CAstSourcePositionMap src) {
		if(oldTarget == CAstControlFlowMap.EXCEPTION_TO_EXIT)
			return oldTarget;
		Assertions.UNREACHABLE();
		return super.flowOutTo(nodeMap, oldSource, label, oldTarget, orig, src);
	}
	
	@Override
	protected CAstControlFlowMap copyFlow(Map<Pair<CAstNode,NoKey>,CAstNode> nodeMap, CAstControlFlowMap orig, CAstSourcePositionMap newSrc) {
		Map<Pair<CAstNode,NoKey>,CAstNode> nodeMapCopy = HashMapFactory.make(nodeMap);
		// delete flow if necessary
		// TODO: this is bad; what if one of the deleted nodes occurs as a cflow target?
		if(flow_to_delete.containsKey(orig)) {
			for(CAstNode node : flow_to_delete.get(orig)) {
				nodeMapCopy.remove(Pair.make(node, null));
			}
			//flow_to_delete.remove(orig);
		}
		CAstControlFlowRecorder flow = (CAstControlFlowRecorder)super.copyFlow(nodeMapCopy, orig, newSrc);
		// extend with local flow information
		if(extra_nodes.containsKey(orig)) {
		  for(CAstNode nd : extra_nodes.get(orig))
		    flow.map(nd, nd);
		}
		if(extra_flow.containsKey(orig)) {
			for(Edge e : extra_flow.get(orig)) {
				CAstNode from = e.from;
				Object label = e.label;
				CAstNode to = e.to;
				if(nodeMap.containsKey(Pair.make(from, null)))
					from = nodeMap.get(Pair.make(from, null));
				if(nodeMap.containsKey(Pair.make(to, null)))
					to = nodeMap.get(Pair.make(to, null));
				if(!flow.isMapped(from))
					flow.map(from, from);
				if(!flow.isMapped(to))
					flow.map(to, to);
				flow.add(from, to, label);
			}
			/*
			 * Here, we would like to say extra_flow.remove(orig) to get rid of the extra control flow
			 * information, but that would not be correct: a single old cfg may be carved up into several
			 * new ones, each of which needs to be extended with the appropriate extra flow from the old cfg.
			 * 
			 * Unfortunately, we now end up extending _every_ new cfg with _all_ the extra flow from the old
			 * cfg, which doesn't sound right either.
			 */
		}
		return flow;
	}
	
	Map<CAstEntity, CAstEntity> rewrite_cache = HashMapFactory.make();
	@Override
	public CAstEntity rewrite(CAstEntity root) {
		// avoid rewriting the same entity more than once
		// TODO: figure out why this happens in the first place
		if(rewrite_cache.containsKey(root)) {
			return rewrite_cache.get(root);
		} else {
			entities.push(root);
			enterEntity(root);
			CAstEntity entity = super.rewrite(root);
			rewrite_cache.put(root, entity);
			leaveEntity(root);
			entities.pop();
			return entity;
		}
	}
	
	protected void enterEntity(CAstEntity entity) {}
	protected void leaveEntity(CAstEntity entity) {}

	public CAstRewriterExt(CAst Ast, boolean recursive,	NodePos rootContext) {
		super(Ast, recursive, rootContext);
	}

}
