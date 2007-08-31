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

import java.util.*;

import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.util.*;
import com.ibm.wala.util.*;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.*;

public abstract class CAstRewriter<C extends CAstRewriter.RewriteContext<K>, K extends CAstRewriter.CopyKey<K>> {

  protected static final boolean DEBUG = false;

  public interface CopyKey<Self extends CopyKey> {

    int hashCode();

    boolean equals(Object o);

    Self parent();

  };

  public interface RewriteContext<K extends CopyKey> {

    K key();

  };

  public interface Rewrite {

    CAstNode newRoot();

    CAstControlFlowMap newCfg();

    CAstSourcePositionMap newPos();

    CAstNodeTypeMap newTypes();

    Map<CAstNode, Collection<CAstEntity>> newChildren();

  }

  protected final CAst Ast;

  protected final boolean recursive;

  protected final C rootContext;

  public CAstRewriter(CAst Ast, boolean recursive, C rootContext) {
    this.Ast = Ast;
    this.recursive = recursive;
    this.rootContext = rootContext;
  }

  protected abstract CAstNode copyNodes(CAstNode root, C context, Map nodeMap);

  protected CAstNode flowOutTo(Map<CAstNode, CAstNode> nodeMap, CAstNode oldSource, Object label, CAstNode oldTarget,
      CAstControlFlowMap orig, CAstSourcePositionMap src) {
    return oldTarget;
  }

  private CAstControlFlowMap copyFlow(Map<CAstNode, CAstNode> nodeMap, CAstControlFlowMap orig, CAstSourcePositionMap newSrc) {
    Set<CAstNode> mappedOutsideNodes = HashSetFactory.make(1);
    CAstControlFlowRecorder newMap = new CAstControlFlowRecorder(newSrc);
    Collection oldSources = orig.getMappedNodes();

    for (Iterator NS = nodeMap.entrySet().iterator(); NS.hasNext();) {
      Map.Entry entry = (Map.Entry) NS.next();
      Pair N = (Pair) entry.getKey();
      CAstNode oldSource = (CAstNode) N.fst;
      CopyKey key = (CopyKey) N.snd;

      CAstNode newSource = (CAstNode) entry.getValue();
      Assertions._assert(newSource != null);

      newMap.map(newSource, newSource);

      if (DEBUG) {
        Trace.println("\n\nlooking at " + key + ":" + CAstPrinter.print(oldSource));
      }

      if (oldSources.contains(oldSource)) {
        Iterator LS = orig.getTargetLabels(oldSource).iterator();
        if (orig.getTarget(oldSource, null) != null) {
          LS = IteratorPlusOne.make(LS, null);
        }

        while (LS.hasNext()) {
          Object label = LS.next();
          CAstNode oldTarget = orig.getTarget(oldSource, label);

          if (DEBUG) {
            Trace.println("old: " + label + " --> " + CAstPrinter.print(oldTarget));
          }

          Pair targetKey;
          CopyKey k = key;
          do {
            targetKey = Pair.make(oldTarget, k);
            if (k != null) {
              k = k.parent();
            } else {
              break;
            }
          } while (!nodeMap.containsKey(targetKey));

          CAstNode newTarget;
          if (nodeMap.containsKey(targetKey)) {
            newTarget = (CAstNode) nodeMap.get(targetKey);
            newMap.add(newSource, newTarget, label);

          } else {
            newTarget = flowOutTo(nodeMap, oldSource, label, oldTarget, orig, newSrc);
            newMap.add(newSource, newTarget, label);
            if (newTarget != CAstControlFlowMap.EXCEPTION_TO_EXIT && !mappedOutsideNodes.contains(newTarget)) {
              mappedOutsideNodes.add(newTarget);
              newMap.map(newTarget, newTarget);
            }
          }

          if (DEBUG) {
            Trace.println("mapping:old: " + CAstPrinter.print(oldSource) + "-- " + label + " --> " + CAstPrinter.print(oldTarget));
            Trace.println("mapping:new: " + CAstPrinter.print(newSource) + "-- " + label + " --> " + CAstPrinter.print(newTarget));
          }
        }
      }
    }

    return newMap;
  }

  private CAstSourcePositionMap copySource(Map nodeMap, CAstSourcePositionMap orig) {
    CAstSourcePositionRecorder newMap = new CAstSourcePositionRecorder();
    for (Iterator NS = nodeMap.entrySet().iterator(); NS.hasNext();) {
      Map.Entry entry = (Map.Entry) NS.next();
      Pair N = (Pair) entry.getKey();
      CAstNode oldNode = (CAstNode) N.fst;

      CAstNode newNode = (CAstNode) entry.getValue();

      if (orig.getPosition(oldNode) != null) {
        newMap.setPosition(newNode, orig.getPosition(oldNode));
      }
    }

    return newMap;
  }

  private CAstNodeTypeMap copyTypes(Map nodeMap, CAstNodeTypeMap orig) {
    if (orig != null) {
      CAstNodeTypeMapRecorder newMap = new CAstNodeTypeMapRecorder();
      for (Iterator NS = nodeMap.entrySet().iterator(); NS.hasNext();) {
        Map.Entry entry = (Map.Entry) NS.next();
        Pair N = (Pair) entry.getKey();
        CAstNode oldNode = (CAstNode) N.fst;

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

  private Map copyChildren(Map nodeMap, Map children) {
    final Map newChildren = new LinkedHashMap();

    for (Iterator NS = nodeMap.entrySet().iterator(); NS.hasNext();) {
      Map.Entry entry = (Map.Entry) NS.next();
      Pair N = (Pair) entry.getKey();
      CAstNode oldNode = (CAstNode) N.fst;

      CAstNode newNode = (CAstNode) entry.getValue();

      if (children.containsKey(oldNode)) {
        Set newEntities = new LinkedHashSet();
        newChildren.put(newNode, newEntities);
        for (Iterator oldEntities = ((Collection) children.get(oldNode)).iterator(); oldEntities.hasNext();) {
          newEntities.add(rewrite((CAstEntity) oldEntities.next()));
        }
      }
    }

    for (Iterator keys = children.entrySet().iterator(); keys.hasNext();) {
      Map.Entry entry = (Map.Entry) keys.next();
      Object key = entry.getKey();
      if (!(key instanceof CAstNode)) {
        Set newEntities = new LinkedHashSet();
        newChildren.put(key, newEntities);
        for (Iterator oldEntities = ((Collection) entry.getValue()).iterator(); oldEntities.hasNext();) {
          newEntities.add(rewrite((CAstEntity) oldEntities.next()));
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
