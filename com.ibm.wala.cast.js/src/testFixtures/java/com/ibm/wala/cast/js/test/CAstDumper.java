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

package com.ibm.wala.cast.js.test;

import static com.ibm.wala.cast.tree.CAstNode.ASSIGN;
import static com.ibm.wala.cast.tree.CAstNode.BLOCK_EXPR;
import static com.ibm.wala.cast.tree.CAstNode.BLOCK_STMT;
import static com.ibm.wala.cast.tree.CAstNode.EMPTY;

import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.NodeLabeller;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class for dumping a textual representation of a CAst syntax tree.
 *
 * <p>Similar to {@link CAstPrinter}, but additionally prints control flow information.
 *
 * <p>It also suppresses certain kinds of spurious nodes such as empty statements within a block or
 * block expressions with a single child, which are simply artifacts of the translation process.
 * This is not nice, but needed for our tests.
 *
 * @author mschaefer
 */
public class CAstDumper {
  private static final boolean NORMALISE = false;
  private final NodeLabeller labeller;

  public CAstDumper() {
    labeller = new NodeLabeller();
  }

  public CAstDumper(NodeLabeller labeller) {
    this.labeller = labeller;
  }

  private static String indent(int indent) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < indent; ++i) buf.append(' ');
    return buf.toString();
  }

  public String dump(CAstEntity entity) {
    StringBuilder buf = new StringBuilder();
    dump(entity, 0, buf);
    return buf.toString();
  }

  private void dump(CAstEntity entity, int indent, StringBuilder buf) {
    Collection<CAstEntity> scopedEntities = Collections.emptySet();
    if (entity.getKind() == CAstEntity.SCRIPT_ENTITY) {
      buf.append(indent(indent)).append(entity.getName()).append(":\n");
      scopedEntities = dumpScopedEntities(entity, indent + 2, buf);
      dump(entity.getAST(), indent, buf, entity.getControlFlow());
    } else if (entity.getKind() == CAstEntity.FUNCTION_ENTITY) {
      buf.append(indent(indent)).append("function ").append(entity.getName()).append('(');
      for (int i = 0; i < entity.getArgumentCount(); ++i) {
        if (i > 0) buf.append(", ");
        buf.append(entity.getArgumentNames()[i]);
      }
      buf.append(") {\n");
      scopedEntities = dumpScopedEntities(entity, indent + 2, buf);
      dump(entity.getAST(), indent + 2, buf, entity.getControlFlow());
      buf.append(indent(indent)).append("}\n\n");
    } else {
      throw new Error("Unknown entity kind " + entity.getKind());
    }
    for (CAstEntity scopedEntity : scopedEntities) dump(scopedEntity, indent, buf);
  }

  private Collection<CAstEntity> dumpScopedEntities(
      CAstEntity entity, int indent, StringBuilder buf) {
    ArrayList<CAstEntity> scopedEntities = new ArrayList<>();
    Map<CAstEntity, CAstNode> m = HashMapFactory.make();
    for (Entry<CAstNode, Collection<CAstEntity>> e : entity.getAllScopedEntities().entrySet())
      for (CAstEntity scopedEntity : e.getValue()) {
        scopedEntities.add(scopedEntity);
        m.put(scopedEntity, e.getKey());
      }
    scopedEntities.sort(Comparator.comparing(CAstEntity::getName));

    buf.append(indent(indent)).append("> ");
    boolean first = true;
    for (CAstEntity scopedEntity : scopedEntities) {
      if (first) first = false;
      else buf.append(", ");
      buf.append(scopedEntity.getName()).append('@').append(labeller.addNode(m.get(scopedEntity)));
    }
    buf.append('\n');
    return scopedEntities;
  }

  private boolean isTrivial(CAstNode node) {
    switch (node.getKind()) {
      case ASSIGN:
        return node.getChild(0).getKind() == CAstNode.VAR && isTrivial(node.getChild(1));
      case EMPTY:
        return true;
      case BLOCK_EXPR:
      case BLOCK_STMT:
        return getNonTrivialChildCount(node) == 0;
      default:
        return false;
    }
  }

  private int getNonTrivialChildCount(CAstNode node) {
    int cnt = 0;
    for (CAstNode child : node.getChildren()) if (!isTrivial(child)) ++cnt;
    return cnt;
  }

  @SuppressWarnings("unused")
  private void dump(CAstNode node, int indent, StringBuilder buf, CAstControlFlowMap cfg) {
    if (isTrivial(node)) return;
    // normalise away single-child block expressions
    if (NORMALISE && node.getKind() == CAstNode.BLOCK_EXPR && getNonTrivialChildCount(node) == 1) {
      for (CAstNode child : node.getChildren())
        if (!isTrivial(child)) dump(child, indent, buf, cfg);
    } else {
      buf.append(indent(indent)).append(labeller.addNode(node)).append(": ");
      if (node.getKind() == CAstNode.CONSTANT) {
        if (node.getValue() == null) buf.append("null");
        else if (node.getValue() instanceof Integer) buf.append(String.valueOf(node.getValue()));
        else buf.append("\"").append(node.getValue()).append("\"");
      } else if (node.getKind() == CAstNode.OPERATOR) {
        buf.append(node.getValue().toString());
      } else {
        buf.append(CAstPrinter.kindAsString(node.getKind()));
      }
      Collection<Object> labels = cfg.getTargetLabels(node);
      if (!labels.isEmpty()) {
        buf.append(" [");
        boolean first = true;
        for (Object label : labels) {
          CAstNode target = cfg.getTarget(node, label);
          if (first) {
            first = false;
          } else {
            buf.append(", ");
          }
          if (label instanceof CAstNode)
            buf.append("CAstNode@").append(labeller.addNode((CAstNode) label)).append(": ");
          else buf.append(label).append(": ");
          buf.append(labeller.addNode(target));
        }
        buf.append(']');
      }
      buf.append('\n');
      for (CAstNode child : node.getChildren()) {
        // omit empty statements in a block
        if (NORMALISE
            && node.getKind() == CAstNode.BLOCK_STMT
            && child != null
            && child.getKind() == CAstNode.EMPTY) continue;
        dump(child, indent + 2, buf, cfg);
      }
    }
  }
}
