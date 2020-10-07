/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An implementation of a CAstControlFlowMap that is designed to be used by producers of CAPA asts.
 * In addition to implementing the control flow map, it additionally allows clients to record
 * control flow mappings in terms of some arbitrary type object that are then mapped to CAstNodes by
 * the client. These objects can be anything, but one common use is that some type of parse tree is
 * walked to build a capa ast, with control flow being recorded in terms of parse tree nodes and
 * then ast nodes being mapped to parse tree nodes.
 *
 * <p>Note that, at present, support for mapping control flow on ast nodes directly is clunky. It is
 * necessary to establish that an ast nodes maps to itself, i.e. call xx.map(node, node).
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CAstControlFlowRecorder implements CAstControlFlowMap {
  private final CAstSourcePositionMap src;

  private final Map<CAstNode, Object> CAstToNode = new LinkedHashMap<>();

  private final Map<Object, CAstNode> nodeToCAst = new LinkedHashMap<>();

  private final Map<Key, Object> table = new LinkedHashMap<>();

  private final Map<Object, Set<Object>> labelMap = new LinkedHashMap<>();

  private final Map<Object, Set<Object>> sourceMap = new LinkedHashMap<>();

  /**
   * for optimizing {@link #getMappedNodes()}; methods that change the set of mapped nodes should
   * null out this field
   */
  private Collection<CAstNode> cachedMappedNodes = null;

  private static class Key {
    private final Object label;

    private final Object from;

    Key(Object label, Object from) {
      assert from != null;
      this.from = from;
      this.label = label;
    }

    @Override
    public int hashCode() {
      if (label != null) return from.hashCode() * label.hashCode();
      else return from.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof Key) && from == ((Key) o).from && Objects.equals(label, ((Key) o).label);
    }

    @Override
    public String toString() {
      return "<key " + label + " : " + from + '>';
    }
  }

  public CAstControlFlowRecorder(CAstSourcePositionMap src) {
    this.src = src;
    map(EXCEPTION_TO_EXIT, EXCEPTION_TO_EXIT);
  }

  @Override
  public CAstNode getTarget(CAstNode from, Object label) {
    assert CAstToNode.get(from) != null;
    Key key = new Key(label, CAstToNode.get(from));
    if (table.containsKey(key)) {
      Object target = table.get(key);
      assert nodeToCAst.containsKey(target);
      return nodeToCAst.get(target);
    } else return null;
  }

  @Override
  public Collection<Object> getTargetLabels(CAstNode from) {
    Object node = CAstToNode.get(from);
    Set<Object> found = labelMap.get(node);
    return found == null ? Collections.emptySet() : found;
  }

  @Override
  public Set<Object> getSourceNodes(CAstNode to) {
    Set<Object> found = sourceMap.get(CAstToNode.get(to));
    return found == null ? Collections.emptySet() : found;
  }

  @Override
  public Collection<CAstNode> getMappedNodes() {
    Collection<CAstNode> nodes = cachedMappedNodes;
    if (nodes == null) {
      nodes = new LinkedHashSet<>();
      for (Map.Entry<Key, Object> entry : table.entrySet()) {
        nodes.add(nodeToCAst.get(entry.getKey().from));
        nodes.add(nodeToCAst.get(entry.getValue()));
      }
      cachedMappedNodes = nodes;
    }
    return nodes;
  }

  /**
   * Add a control-flow edge from the `from' node to the `to' node with the (possibly null) label
   * `label'. These nodes must be mapped by the client to CAstNodes using the `map' call; this
   * mapping can happen before or after this add call.
   */
  public void add(Object from, Object to, Object label) {
    assert from != null;
    assert to != null;

    assert !((from instanceof CAstNode)
        && ((CAstNode) from).getKind() == CAstNode.GOTO
        && to == EXCEPTION_TO_EXIT);

    if (CAstToNode.containsKey(to)) {
      to = CAstToNode.get(to);
    }

    if (CAstToNode.containsKey(from)) {
      from = CAstToNode.get(from);
    }

    table.put(new Key(label, from), to);

    if (!labelMap.containsKey(from)) {
      labelMap.put(from, HashSetFactory.make(2));
    }
    Set<Object> ls = labelMap.get(from);
    ls.add(label);

    if (!sourceMap.containsKey(to)) {
      sourceMap.put(to, HashSetFactory.make(2));
    }
    Set<Object> ss = sourceMap.get(to);
    ss.add(from);
  }

  /**
   * Establish a mapping between some object `node' and the ast node `ast'. Objects used as
   * endpoints in a control flow edge must be mapped to ast nodes using this call.
   */
  public void map(Object node, CAstNode ast) {
    assert node != null;
    assert ast != null;
    assert !nodeToCAst.containsKey(node) || nodeToCAst.get(node) == ast
        : node + " already mapped:\n" + this;
    assert !CAstToNode.containsKey(ast) || CAstToNode.get(ast) == node
        : ast + " already mapped:\n" + this;
    nodeToCAst.put(node, ast);
    cachedMappedNodes = null;
    CAstToNode.put(ast, node);
  }

  public void addAll(CAstControlFlowMap other) {
    for (CAstNode n : other.getMappedNodes()) {
      if (!CAstToNode.containsKey(n)) {
        map(n, n);
      }
      for (Object l : other.getTargetLabels(n)) {
        CAstNode to = other.getTarget(n, l);
        add(n, to, l);
      }
    }
  }

  public boolean isMapped(Object node) {
    return nodeToCAst.containsKey(node);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("control flow map\n");
    for (Map.Entry<Key, Object> entry : table.entrySet()) {
      final Key key = entry.getKey();
      sb.append(key.from);
      if (src != null
          && nodeToCAst.get(key.from) != null
          && src.getPosition(nodeToCAst.get(key.from)) != null) {
        sb.append(" (").append(src.getPosition(nodeToCAst.get(key.from))).append(") ");
      }
      sb.append(" -- ");
      sb.append(key.label);
      sb.append(" --> ");
      sb.append(entry.getValue());
      sb.append('\n');
    }
    sb.append('\n');
    return sb.toString();
  }
}
