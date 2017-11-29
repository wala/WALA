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
package com.ibm.wala.cast.tree.rewrite;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.impl.DelegatingEntity;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * Abstract superclass for types performing a rewrite operation on a CAst. The
 * CAst is not mutated; instead, a new CAst is created which delegates to the
 * original CAst where no transformation was performed.
 * 
 * @param <C>
 *          type of the RewriteContext used when traversing the original CAst
 *          during the rewrite operation
 * @param <K>
 *          a key used to ease cloning of partial ASTs. When rewriting an AST,
 *          sub-classes maintain a mapping from (original node, key) pairs
 *          (where key is of type K) to new nodes; see
 *          {@link #copyNodes}
 */
public abstract class CAstRewriter<C extends CAstRewriter.RewriteContext<K>, K extends CAstRewriter.CopyKey<K>> {

  protected static final boolean DEBUG = false;

  /**
   * interface to be implemented by keys used for cloning sub-trees during the
   * rewrite
   */
  public interface CopyKey<Self extends CopyKey<Self>> {

    @Override
    int hashCode();

    @Override
    boolean equals(Object o);

    /**
     * keys have parent pointers, useful for when nesting cloning must occur
     * (e.g., unrolling of nested loops)
     */
    Self parent();

  }

  /**
   * interface to be implemented by contexts used while traversing the AST
   */
  public interface RewriteContext<K extends CopyKey<K>> {

    /**
     * get the cloning key for this context
     */
    K key();

  }

  /**
   * represents a rewritten CAst
   */
  public interface Rewrite {

    CAstNode newRoot();

    CAstControlFlowMap newCfg();

    CAstSourcePositionMap newPos();

    CAstNodeTypeMap newTypes();

    Map<CAstNode, Collection<CAstEntity>> newChildren();

  }

  protected final CAst Ast;

  /**
   * for CAstEntity nodes r s.t. r.getAst() == null, should the scoped entities
   * of r be rewritten?
   */
  protected final boolean recursive;

  protected final C rootContext;

  public CAstRewriter(CAst Ast, boolean recursive, C rootContext) {
    this.Ast = Ast;
    this.recursive = recursive;
    this.rootContext = rootContext;
  }

  /**
   * rewrite the CAst rooted at root under some context, returning the node at
   * the root of the rewritten tree. mutate nodeMap in the process, indicating
   * how (original node, copy key) pairs are mapped to nodes in the rewritten
   * tree.
   */
  protected abstract CAstNode copyNodes(CAstNode root, final CAstControlFlowMap cfg, C context, Map<Pair<CAstNode, K>, CAstNode> nodeMap);

  /**
   * in {@link #copyFlow(Map, CAstControlFlowMap, CAstSourcePositionMap)}, if
   * the source of some original CFG edge is replicated, but we find no replica
   * for the target, what node should be the target of the CFG edge in the
   * rewritten AST? By default, just uses the original target.
   * 
   */
  @SuppressWarnings("unused")
  protected CAstNode flowOutTo(Map<Pair<CAstNode, K>, CAstNode> nodeMap, CAstNode oldSource, Object label, CAstNode oldTarget,
      CAstControlFlowMap orig, CAstSourcePositionMap src) {
    return oldTarget;
  }

  /**
   * create a control-flow map for the rewritten tree, given the mapping from
   * (original node, copy key) pairs ot new nodes and the original control-flow
   * map.
   */
  protected CAstControlFlowMap copyFlow(Map<Pair<CAstNode, K>, CAstNode> nodeMap, CAstControlFlowMap orig,
      CAstSourcePositionMap newSrc) {

    // the new control-flow map
    final CAstControlFlowRecorder newMap = new CAstControlFlowRecorder(newSrc);
    // tracks which CAstNodes not present in nodeMap's key set (under any copy
    // key) are added as targets of CFG edges
    // via a call to flowOutTo() (see below); used to ensure these nodes are
    // only mapped to themselves once in newMap
    final Set<CAstNode> mappedOutsideNodes = HashSetFactory.make(1);
    // all edge targets in new control-flow map; must all be mapped to
    // themselves
    Set<CAstNode> allNewTargetNodes = HashSetFactory.make(1);
    Collection<CAstNode> oldSources = orig.getMappedNodes();

    for (Entry<Pair<CAstNode, K>, CAstNode> entry : nodeMap.entrySet()) {
      Pair<CAstNode, K> N = entry.getKey();
      CAstNode oldSource = N.fst;
      K key = N.snd;

      CAstNode newSource = entry.getValue();
      assert newSource != null;

      newMap.map(newSource, newSource);

      if (DEBUG) {
        System.err.println(("\n\nlooking at " + key + ":" + CAstPrinter.print(oldSource)));
      }

      if (oldSources.contains(oldSource)) {
        Iterator<Object> LS = orig.getTargetLabels(oldSource).iterator();
        //if (orig.getTarget(oldSource, null) != null) {
        //  LS = IteratorPlusOne.make(LS, null);
        //}

        while (LS.hasNext()) {
          Object origLabel = LS.next();
          CAstNode oldTarget = orig.getTarget(oldSource, origLabel);
          assert oldTarget != null;

          if (DEBUG) {
            System.err.println(("old: " + origLabel + " --> " + CAstPrinter.print(oldTarget)));
          }

          // try to find a k in key's parent chain such that (oldTarget, k) is
          // in nodeMap's key set
          Pair<CAstNode,CopyKey<K>> targetKey;
          CopyKey<K> k = key;
          do {
            targetKey = Pair.make(oldTarget, k);
            if (k != null) {
              k = k.parent();
            } else {
              break;
            }
          } while (!nodeMap.containsKey(targetKey));

          Object newLabel;
          if (nodeMap.containsKey(Pair.make(origLabel, targetKey.snd))) { // label
                                                                          // is
                                                                          // mapped
                                                                          // too
            newLabel = nodeMap.get(Pair.make(origLabel, targetKey.snd));
          } else {
            newLabel = origLabel;
          }

          CAstNode newTarget;
          if (nodeMap.containsKey(targetKey)) {
            newTarget = nodeMap.get(targetKey);
            newMap.add(newSource, newTarget, newLabel);
            allNewTargetNodes.add(newTarget);

          } else {
            // could not discover target of CFG edge in nodeMap under any key related to the current source key.
            // the edge might have been deleted, or it may end at a node above the root where we were
            // rewriting
            // ask flowOutTo() to just choose a target
            newTarget = flowOutTo(nodeMap, oldSource, origLabel, oldTarget, orig, newSrc);
            allNewTargetNodes.add(newTarget);
            newMap.add(newSource, newTarget, newLabel);
            if (newTarget != CAstControlFlowMap.EXCEPTION_TO_EXIT && !mappedOutsideNodes.contains(newTarget)) {
              mappedOutsideNodes.add(newTarget);
              newMap.map(newTarget, newTarget);
            }
          }

          if (DEBUG) {
            System.err.println(("mapping:old: " + CAstPrinter.print(oldSource) + "-- " + origLabel + " --> " + CAstPrinter
                .print(oldTarget)));
            System.err.println(("mapping:new: " + CAstPrinter.print(newSource) + "-- " + newLabel + " --> " + CAstPrinter
                .print(newTarget)));
          }
        }
      }
    }

    allNewTargetNodes.removeAll(newMap.getMappedNodes());
    for (CAstNode newTarget : allNewTargetNodes) {
      if (newTarget != CAstControlFlowMap.EXCEPTION_TO_EXIT) {
        newMap.map(newTarget, newTarget); 
      }
    }
    
    
    assert !oldNodesInNewMap(nodeMap, newMap);

    return newMap;
  }

  // check whether newMap contains any CFG edges involving nodes in the domain of nodeMap
  private boolean oldNodesInNewMap(Map<Pair<CAstNode, K>, CAstNode> nodeMap, final CAstControlFlowRecorder newMap) {
    HashSet<CAstNode> oldNodes = HashSetFactory.make();
    for(Entry<Pair<CAstNode, K>, CAstNode> e : nodeMap.entrySet())
      oldNodes.add(e.getKey().fst);
    for(CAstNode mappedNode : newMap.getMappedNodes()) {
      if(oldNodes.contains(mappedNode))
        return true;
      for(Object lbl : newMap.getTargetLabels(mappedNode))
        if(oldNodes.contains(newMap.getTarget(mappedNode, lbl)))
          return true;
    }
    return false;
  }

  protected CAstSourcePositionMap copySource(Map<Pair<CAstNode, K>, CAstNode> nodeMap, CAstSourcePositionMap orig) {
    CAstSourcePositionRecorder newMap = new CAstSourcePositionRecorder();
    for (Entry<Pair<CAstNode, K>, CAstNode> entry : nodeMap.entrySet()) {
      Pair<CAstNode, K> N = entry.getKey();
      CAstNode oldNode = N.fst;

      CAstNode newNode = entry.getValue();

      if (orig.getPosition(oldNode) != null) {
        newMap.setPosition(newNode, orig.getPosition(oldNode));
      }
    }

    return newMap;
  }

  protected CAstNodeTypeMap copyTypes(Map<Pair<CAstNode, K>, CAstNode> nodeMap, CAstNodeTypeMap orig) {
    if (orig != null) {
      CAstNodeTypeMapRecorder newMap = new CAstNodeTypeMapRecorder();
      for (Entry<Pair<CAstNode, K>, CAstNode> entry : nodeMap.entrySet()) {
        Pair<CAstNode, K> N = entry.getKey();
        CAstNode oldNode = N.fst;

        CAstNode newNode = entry.getValue();

        if (orig.getNodeType(oldNode) != null) {
          newMap.add(newNode, orig.getNodeType(oldNode));
        }
      }

      return newMap;
    } else {
      return null;
    }
  }

  protected Map<CAstNode, Collection<CAstEntity>> copyChildren(@SuppressWarnings("unused") CAstNode root, Map<Pair<CAstNode, K>, CAstNode> nodeMap,
      Map<CAstNode, Collection<CAstEntity>> children) {
    final Map<CAstNode, Collection<CAstEntity>> newChildren = new LinkedHashMap<>();

    for (Entry<Pair<CAstNode, K>, CAstNode> entry : nodeMap.entrySet()) {
      Pair<CAstNode, K> N = entry.getKey();
      CAstNode oldNode = N.fst;

      CAstNode newNode = entry.getValue();

      if (children.containsKey(oldNode)) {
        Set<CAstEntity> newEntities = new LinkedHashSet<>();
        newChildren.put(newNode, newEntities);
        for (CAstEntity cAstEntity : children.get(oldNode)) {
          newEntities.add(rewrite(cAstEntity));
        }
      }
    }

    for (Entry<CAstNode, Collection<CAstEntity>> entry : children.entrySet()) {
      CAstNode key = entry.getKey();
      if (key == null) {
        Set<CAstEntity> newEntities = new LinkedHashSet<>();
        newChildren.put(key, newEntities);
        for (CAstEntity oldEntity : entry.getValue()) {
          newEntities.add(rewrite(oldEntity));
        }
      }
    }

    return newChildren;
  }

  /**
   * rewrite the CAst sub-tree rooted at root
   */
  public Rewrite rewrite(final CAstNode root, final CAstControlFlowMap cfg, final CAstSourcePositionMap pos, final CAstNodeTypeMap types,
      final Map<CAstNode, Collection<CAstEntity>> children) {
    final Map<Pair<CAstNode, K>, CAstNode> nodes = HashMapFactory.make();
    final CAstNode newRoot = copyNodes(root, cfg, rootContext, nodes);
    return new Rewrite() {
      private CAstControlFlowMap theCfg = null;

      private CAstSourcePositionMap theSource = null;

      private CAstNodeTypeMap theTypes = null;

      private Map<CAstNode, Collection<CAstEntity>> theChildren = null;

      @Override
      public CAstNode newRoot() {
        return newRoot;
      }

      @Override
      public CAstControlFlowMap newCfg() {
        if (theCfg == null)
          theCfg = copyFlow(nodes, cfg, newPos());
        return theCfg;
      }

      @Override
      public CAstSourcePositionMap newPos() {
        if (theSource == null && pos != null)
          theSource = copySource(nodes, pos);
        return theSource;
      }

      @Override
      public CAstNodeTypeMap newTypes() {
        if (theTypes == null)
          theTypes = copyTypes(nodes, types);
        return theTypes;
      }

      @Override
      public Map<CAstNode, Collection<CAstEntity>> newChildren() {
        if (theChildren == null)
          theChildren = copyChildren(root, nodes, children);
        return theChildren;
      }
    };
  }

  /**
   * perform the rewrite on a {@link CAstEntity}, returning the new
   * {@link CAstEntity} as the result
   */
  public CAstEntity rewrite(final CAstEntity root) {

    if (root.getAST() != null) {
      final Rewrite rewrite = rewrite(root.getAST(), root.getControlFlow(), root.getSourceMap(), root.getNodeTypeMap(),
          root.getAllScopedEntities());

      return new DelegatingEntity(root) {
        @Override
        public String toString() {
          return root.toString() + " (clone)";
        }

        @Override
        public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
          Map<CAstNode, Collection<CAstEntity>> newChildren = getAllScopedEntities();
          if (newChildren.containsKey(construct)) {
            return newChildren.get(construct).iterator();
          } else {
            return EmptyIterator.instance();
          }
        }

        @Override
        public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
          return rewrite.newChildren();
        }

        @Override
        public CAstNode getAST() {
          return rewrite.newRoot();
        }

        @Override
        public CAstNodeTypeMap getNodeTypeMap() {
          return rewrite.newTypes();
        }

        @Override
        public CAstSourcePositionMap getSourceMap() {
          return rewrite.newPos();
        }

        @Override
        public CAstControlFlowMap getControlFlow() {
          return rewrite.newCfg();
        }
      };

    } else if (recursive) {

      Map<CAstNode, Collection<CAstEntity>> children = root.getAllScopedEntities();
      final Map<CAstNode, Collection<CAstEntity>> newChildren = new LinkedHashMap<>();
      for (Entry<CAstNode, Collection<CAstEntity>> entry : children.entrySet()) {
        CAstNode key = entry.getKey();
        Set<CAstEntity> newValues = new LinkedHashSet<>();
        newChildren.put(key, newValues);
        for (CAstEntity entity : entry.getValue()) {
          newValues.add(rewrite(entity));
        }
      }

      return new DelegatingEntity(root) {
        @Override
        public String toString() {
          return root.toString() + " (clone)";
        }

        @Override
        public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
          if (newChildren.containsKey(construct)) {
            return newChildren.get(construct).iterator();
          } else {
            return EmptyIterator.instance();
          }
        }

        @Override
        public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
          return newChildren;
        }
      };

    } else {
      return root;
    }
  }

}
