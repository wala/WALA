/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import static java.util.Objects.requireNonNullElseGet;

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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Extension of {@link CAstRewriter} which allows adding or deleting control flow edges, and keeps
 * track of the current entity.
 *
 * <p>TODO: This class is an unholy mess. It should be restructured considerably.
 *
 * @author mschaefer
 */
public abstract class CAstRewriterExt extends CAstRewriter<NodePos, NoKey> {

  /**
   * A control flow edge to be added to the CFG.
   *
   * @author mschaefer
   */
  protected static class Edge {
    private final CAstNode from;
    private final Object label;
    private final CAstNode to;

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

      int result = prime + from.hashCode();
      result = prime * result + ((label == null) ? 0 : label.hashCode());
      result = prime * result + to.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Edge)) return false;
      Edge that = (Edge) obj;
      return this.from.equals(that.from)
          && Objects.equals(this.label, that.label)
          && this.to.equals(that.to);
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
  private final ArrayDeque<CAstEntity> entities = new ArrayDeque<>();

  public CAstNode addNode(CAstNode node, CAstControlFlowMap flow) {
    extra_nodes.computeIfAbsent(flow, key -> HashSetFactory.make()).add(node);
    return node;
  }

  public CAstNode addFlow(CAstNode node, Object label, CAstNode target, CAstControlFlowMap flow) {
    extra_flow
        .computeIfAbsent(flow, key -> HashSetFactory.make())
        .add(new Edge(node, label, target));
    return node;
  }

  public void deleteFlow(CAstNode node, CAstEntity entity) {
    CAstControlFlowMap flow = entity.getControlFlow();
    flow_to_delete.computeIfAbsent(flow, key -> HashSetFactory.make()).add(node);
  }

  protected boolean isFlowDeleted(CAstNode node, CAstEntity entity) {
    CAstControlFlowMap flow = entity.getControlFlow();
    return flow_to_delete.getOrDefault(flow, Collections.emptySet()).contains(node);
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
  protected Map<CAstNode, Collection<CAstEntity>> copyChildren(
      CAstNode root,
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap,
      Map<CAstNode, Collection<CAstEntity>> children) {
    Map<CAstNode, Collection<CAstEntity>> map = super.copyChildren(root, nodeMap, children);
    // extend with local mapping information
    for (Iterator<Entity> es = entities_to_add.iterator(); es.hasNext(); ) {
      Entity e = es.next();
      boolean relevant =
          NodePos.inSubtree(e.anchor, nodeMap.get(Pair.make(root, null)))
              || NodePos.inSubtree(e.anchor, root);
      if (relevant) {
        map.computeIfAbsent(e.anchor, key -> HashSetFactory.make()).add(e.me);
        es.remove();
      }
    }
    return map;
  }

  @Override
  protected CAstNode flowOutTo(
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap,
      CAstNode oldSource,
      Object label,
      CAstNode oldTarget,
      CAstControlFlowMap orig,
      CAstSourcePositionMap src) {
    if (oldTarget == CAstControlFlowMap.EXCEPTION_TO_EXIT) return oldTarget;
    Assertions.UNREACHABLE();
    return super.flowOutTo(nodeMap, oldSource, label, oldTarget, orig, src);
  }

  @Override
  protected CAstControlFlowMap copyFlow(
      Map<Pair<CAstNode, NoKey>, @NonNull CAstNode> nodeMap,
      CAstControlFlowMap orig,
      CAstSourcePositionMap newSrc) {
    Map<Pair<CAstNode, NoKey>, CAstNode> nodeMapCopy = HashMapFactory.make(nodeMap);
    // delete flow if necessary
    // TODO: this is bad; what if one of the deleted nodes occurs as a cflow target?
    for (CAstNode node : flow_to_delete.getOrDefault(orig, Collections.emptySet())) {
      nodeMapCopy.remove(Pair.make(node, null));
    }
    // flow_to_delete.remove(orig);
    CAstControlFlowRecorder flow =
        (CAstControlFlowRecorder) super.copyFlow(nodeMapCopy, orig, newSrc);
    // extend with local flow information
    for (CAstNode nd :
        requireNonNullElseGet(extra_nodes.get(orig), Collections::<CAstNode>emptySet)) {
      flow.map(nd, nd);
    }
    for (Edge e : requireNonNullElseGet(extra_flow.get(orig), Collections::<Edge>emptySet)) {
      CAstNode from = e.from;
      Object label = e.label;
      CAstNode to = e.to;
      from = nodeMap.getOrDefault(Pair.make(from, null), from);
      to = nodeMap.getOrDefault(Pair.make(to, null), to);
      from = nodeMap.getOrDefault(Pair.make(from, null), from);
      to = nodeMap.getOrDefault(Pair.make(to, null), to);
      if (!flow.isMapped(from)) flow.map(from, from);
      if (!flow.isMapped(to)) flow.map(to, to);
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
    return flow;
  }

  Map<CAstEntity, CAstEntity> rewrite_cache = HashMapFactory.make();

  @Override
  public CAstEntity rewrite(CAstEntity root) {
    // avoid rewriting the same entity more than once
    // TODO: figure out why this happens in the first place
    final var found = rewrite_cache.get(root);
    if (found != null) {
      return found;
    } else {
      entities.push(root);
      enterEntity(root);
      CAstEntity entity = super.rewrite(root);
      rewrite_cache.put(root, entity);
      leaveEntity();
      entities.pop();
      return entity;
    }
  }

  protected void enterEntity(@SuppressWarnings("unused") CAstEntity entity) {}

  protected void leaveEntity() {}

  public CAstRewriterExt(CAst Ast, boolean recursive, NodePos rootContext) {
    super(Ast, recursive, rootContext);
  }
}
