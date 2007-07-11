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
package com.ibm.wala.cast.tree.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

public abstract class CAstRewriter<RewriteContext> {

  protected final CAst Ast;

  protected final boolean recursive;

  protected final RewriteContext rootContext;

  public CAstRewriter(CAst Ast, boolean recursive, RewriteContext rootContext) {
    this.Ast = Ast;
    this.recursive = recursive;
    this.rootContext = rootContext;
  }

  public interface Rewrite {

    CAstNode newRoot();

    CAstControlFlowMap newCfg();

    CAstSourcePositionMap newPos();

    CAstNodeTypeMap newTypes();

    Map<CAstNode, Collection<CAstEntity>> newChildren();

  }

  protected abstract CAstNode copyNodes(CAstNode root, RewriteContext context, Map<CAstNode, CAstNode> nodeMap);

  protected CAstNode flowOutTo(Map<CAstNode, CAstNode> nodeMap, CAstNode oldSource, Object label, CAstNode oldTarget,
      CAstControlFlowMap orig, CAstSourcePositionMap src) {
    return oldTarget;
  }

  private CAstControlFlowMap copyFlow(Map<CAstNode, CAstNode> nodeMap, CAstControlFlowMap orig, CAstSourcePositionMap src) {
    Set<CAstNode> mappedOutsideNodes = HashSetFactory.make(1);
    Collection<CAstNode> oldSources = orig.getMappedNodes();
    CAstControlFlowRecorder newMap = new CAstControlFlowRecorder(src);
    for (Iterator<CAstNode> NS = nodeMap.keySet().iterator(); NS.hasNext();) {
      CAstNode old = NS.next();
      CAstNode newNode = nodeMap.get(old);
      newMap.map(newNode, newNode);
      if (oldSources.contains(old)) {
        if (orig.getTarget(old, null) != null) {
          CAstNode oldTarget = orig.getTarget(old, null);
          if (nodeMap.containsKey(oldTarget)) {
            newMap.add(newNode, nodeMap.get(oldTarget), null);
          } else {
            CAstNode tgt = flowOutTo(nodeMap, old, null, oldTarget, orig, src);
            newMap.add(newNode, tgt, null);
            if (tgt != CAstControlFlowMap.EXCEPTION_TO_EXIT && !mappedOutsideNodes.contains(tgt)) {
              mappedOutsideNodes.add(tgt);
              newMap.map(tgt, tgt);
            }
          }
        }

        for (Iterator LS = orig.getTargetLabels(old).iterator(); LS.hasNext();) {
          Object label = LS.next();
          CAstNode oldTarget = orig.getTarget(old, label);
          if (nodeMap.containsKey(oldTarget)) {
            newMap.add(newNode, nodeMap.get(oldTarget), label);
          } else {
            CAstNode tgt = flowOutTo(nodeMap, old, null, oldTarget, orig, src);
            newMap.add(newNode, tgt, label);
            if (tgt != CAstControlFlowMap.EXCEPTION_TO_EXIT && !mappedOutsideNodes.contains(tgt)) {
              mappedOutsideNodes.add(tgt);
              newMap.map(tgt, tgt);
            }
          }
        }
      }
    }

    return newMap;
  }

  private CAstSourcePositionMap copySource(Map<CAstNode, CAstNode> nodeMap, CAstSourcePositionMap orig) {
    if (orig == null) {
      return null;
    } else {
      CAstSourcePositionRecorder newMap = new CAstSourcePositionRecorder();
      for (Iterator<CAstNode> NS = nodeMap.keySet().iterator(); NS.hasNext();) {
        CAstNode old = NS.next();
        CAstNode newNode = nodeMap.get(old);

        if (orig.getPosition(old) != null) {
          newMap.setPosition(newNode, orig.getPosition(old));
        }
      }

      return newMap;
    }
  }

  private CAstNodeTypeMap copyTypes(Map nodeMap, CAstNodeTypeMap orig) {
    if (orig != null) {
      CAstNodeTypeMapRecorder newMap = new CAstNodeTypeMapRecorder();
      for (Iterator NS = nodeMap.entrySet().iterator(); NS.hasNext();) {
        Map.Entry entry = (Map.Entry) NS.next();
        CAstNode oldNode = (CAstNode) entry.getKey();
        CAstNode newNode = (CAstNode) entry.getValue();

        if (orig.getNodeType(oldNode) != null) {
          newMap.add(newNode, orig.getNodeType(oldNode));
        }
      }

      return newMap;
    } else {
      return null;
    }
  }

  private Map<CAstNode, Collection<CAstEntity>> copyChildren(Map nodeMap, Map<CAstNode, Collection<CAstEntity>> children) {
    final Map<CAstNode, Collection<CAstEntity>> newChildren = new LinkedHashMap<CAstNode, Collection<CAstEntity>>();

    for (Iterator NS = nodeMap.entrySet().iterator(); NS.hasNext();) {
      Map.Entry entry = (Map.Entry) NS.next();
      CAstNode oldNode = (CAstNode) entry.getKey();
      CAstNode newNode = (CAstNode) entry.getValue();

      if (children.containsKey(oldNode)) {
        Set<CAstEntity> newEntities = new LinkedHashSet<CAstEntity>();
        newChildren.put(newNode, newEntities);
        for (Iterator oldEntities = children.get(oldNode).iterator(); oldEntities.hasNext();) {
          CAstEntity oldE = (CAstEntity) oldEntities.next();
          newEntities.add(recursive ? rewrite(oldE) : oldE);
        }
      }
    }

    for (Iterator<Map.Entry<CAstNode, Collection<CAstEntity>>> keys = children.entrySet().iterator(); keys.hasNext();) {
      Map.Entry<CAstNode, Collection<CAstEntity>> entry = keys.next();
      CAstNode key = entry.getKey();
      if (!(key instanceof CAstNode)) {
        Set<CAstEntity> newEntities = new LinkedHashSet<CAstEntity>();
        newChildren.put(key, newEntities);
        for (Iterator oldEntities = entry.getValue().iterator(); oldEntities.hasNext();) {
          CAstEntity oldE = (CAstEntity) oldEntities.next();
          newEntities.add(recursive ? rewrite(oldE) : oldE);
        }
      }
    }

    return newChildren;
  }

  public Rewrite rewrite(CAstNode root, final CAstControlFlowMap cfg, final CAstSourcePositionMap pos, final CAstNodeTypeMap types,
      final Map<CAstNode, Collection<CAstEntity>> children) {
    final Map<CAstNode, CAstNode> nodes = HashMapFactory.make();
    final CAstNode newRoot = copyNodes(root, rootContext, nodes);
    return new Rewrite() {
      private CAstControlFlowMap theCfg = null;

      private CAstSourcePositionMap theSource = null;

      private CAstNodeTypeMap theTypes = null;

      private Map<CAstNode, Collection<CAstEntity>> theChildren = null;

      public CAstNode newRoot() {
        return newRoot;
      }

      public CAstControlFlowMap newCfg() {
        if (theCfg == null)
          theCfg = copyFlow(nodes, cfg, newPos());
        return theCfg;
      }

      public CAstSourcePositionMap newPos() {
        if (theSource == null)
          theSource = copySource(nodes, pos);
        return theSource;
      }

      public CAstNodeTypeMap newTypes() {
        if (theTypes == null)
          theTypes = copyTypes(nodes, types);
        return theTypes;
      }

      public Map<CAstNode, Collection<CAstEntity>> newChildren() {
        if (theChildren == null)
          theChildren = copyChildren(nodes, children);
        return theChildren;
      }
    };
  }

  public CAstEntity rewrite(final CAstEntity root) {
    if (root.getAST() != null) {
      final Rewrite rewrite = rewrite(root.getAST(), root.getControlFlow(), root.getSourceMap(), root.getNodeTypeMap(), root
          .getAllScopedEntities());

      return new DelegatingEntity(root) {
        public String toString() {
          return root.toString() + " (clone)";
        }

        public Iterator getScopedEntities(CAstNode construct) {
          Map newChildren = getAllScopedEntities();
          if (newChildren.containsKey(construct)) {
            return ((Collection) newChildren.get(construct)).iterator();
          } else {
            return EmptyIterator.instance();
          }
        }

        public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
          return rewrite.newChildren();
        }

        public CAstNode getAST() {
          return rewrite.newRoot();
        }

        public CAstNodeTypeMap getNodeTypeMap() {
          return rewrite.newTypes();
        }

        public CAstSourcePositionMap getSourceMap() {
          return rewrite.newPos();
        }

        public CAstControlFlowMap getControlFlow() {
          return rewrite.newCfg();
        }
      };

    } else if (recursive) {

      Map<CAstNode, Collection<CAstEntity>> children = root.getAllScopedEntities();
      final Map<CAstNode, Collection<CAstEntity>> newChildren = new LinkedHashMap<CAstNode, Collection<CAstEntity>>();
      for (Iterator<Map.Entry<CAstNode, Collection<CAstEntity>>> keys = children.entrySet().iterator(); keys.hasNext();) {
        Map.Entry<CAstNode, Collection<CAstEntity>> entry = keys.next();
        CAstNode key = entry.getKey();
        Set<CAstEntity> newValues = new LinkedHashSet<CAstEntity>();
        newChildren.put(key, newValues);
        for (Iterator es = entry.getValue().iterator(); es.hasNext();) {
          newValues.add(rewrite((CAstEntity) es.next()));
        }
      }

      return new DelegatingEntity(root) {
        public String toString() {
          return root.toString() + " (clone)";
        }

        public Iterator getScopedEntities(CAstNode construct) {
          if (newChildren.containsKey(construct)) {
            return newChildren.get(construct).iterator();
          } else {
            return EmptyIterator.instance();
          }
        }

        public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
          return newChildren;
        }
      };

    } else {
      return root;
    }
  }

}
