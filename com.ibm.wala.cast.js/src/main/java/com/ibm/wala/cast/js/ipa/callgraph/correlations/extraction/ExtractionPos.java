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

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A special {@link ChildPos} representing the position of a node which is the body of a for-in
 * loop.
 *
 * <p>This also stores some additional data obtained while rewriting the loop body, such as whether
 * {@code return} statements were encountered.
 *
 * @author mschaefer
 */
public class ExtractionPos extends NodePos {
  private final CAstNode parent;
  private final ExtractionRegion region;
  private final NodePos parent_pos;
  private boolean contains_return;
  private boolean contains_this;
  private final Set<Pair<String, CAstNode>> goto_targets = HashSetFactory.make();
  private boolean contains_outer_goto;
  private final Set<ExtractionPos> nested_loops = HashSetFactory.make();
  private CAstEntity extracted_entity;
  private CAstNode callsite;

  public ExtractionPos(CAstNode parent, ExtractionRegion region, NodePos parent_pos) {
    this.parent = parent;
    this.region = region;
    this.parent_pos = parent_pos;
  }

  public CAstNode getParent() {
    return parent;
  }

  public int getStart() {
    return region.getStart();
  }

  public int getEnd() {
    return region.getEnd();
  }

  public ExtractionRegion getRegion() {
    return region;
  }

  public boolean contains(CAstNode node) {
    for (int i = getStart(); i < getEnd(); ++i)
      if (NodePos.inSubtree(node, parent.getChild(i))) return true;
    return false;
  }

  public List<String> getParameters() {
    return region.getParameters();
  }

  public void addGotoTarget(String label, CAstNode node) {
    // check whether this target lies beyond an enclosing for-in loop
    ExtractionPos outer = getEnclosingExtractionPos(parent_pos);
    if (outer != null && !outer.contains(node)) {
      // the goto needs to be handled by the outer loop
      outer.addGotoTarget(label, node);
      // but we need to remember to pass it on
      contains_outer_goto = true;
    } else {
      // this goto is our responsibility
      goto_targets.add(Pair.make(label, node));
    }
  }

  public boolean containsReturn() {
    return contains_return;
  }

  public void addReturn() {
    this.contains_return = true;
  }

  public Set<Pair<String, CAstNode>> getGotoTargets() {
    return Collections.unmodifiableSet(goto_targets);
  }

  public void addThis() {
    contains_this = true;
  }

  public boolean containsThis() {
    return contains_this;
  }

  public boolean containsGoto() {
    return !getGotoTargets().isEmpty();
  }

  public boolean containsOuterGoto() {
    return contains_outer_goto;
  }

  public boolean containsJump() {
    return containsGoto() || containsReturn() || containsOuterGoto();
  }

  public String getThisParmName() {
    return "thi$";
  }

  public void addNestedPos(ExtractionPos loop) {
    nested_loops.add(loop);
  }

  public Iterator<ExtractionPos> getNestedLoops() {
    return nested_loops.iterator();
  }

  public void setExtractedEntity(CAstEntity entity) {
    assert this.extracted_entity == null : "Cannot reset extracted entity.";
    extracted_entity = entity;
  }

  public CAstEntity getExtractedEntity() {
    assert extracted_entity != null : "Extracted entity not set.";
    return extracted_entity;
  }

  public void setCallSite(CAstNode callsite) {
    assert this.callsite == null : "Cannot reset call site.";
    this.callsite = callsite;
  }

  public CAstNode getCallSite() {
    assert callsite != null : "Call site not set.";
    return callsite;
  }

  @Override
  public <A> A accept(PosSwitch<A> ps) {
    return ps.caseForInLoopBodyPos(this);
  }

  // return the outermost enclosing extraction position around 'pos' within the same function;
  // "null" if there is none
  public static ExtractionPos getOutermostEnclosingExtractionPos(NodePos pos) {
    return pos.accept(
        new PosSwitch<ExtractionPos>() {
          @Override
          public ExtractionPos caseRootPos(RootPos pos) {
            return null;
          }

          @Override
          public ExtractionPos caseChildPos(ChildPos pos) {
            int kind = pos.getParent().getKind();
            if (kind == CAstNode.FUNCTION_STMT || kind == CAstNode.FUNCTION_EXPR) return null;
            return getOutermostEnclosingExtractionPos(pos.getParentPos());
          }

          @Override
          public ExtractionPos caseForInLoopBodyPos(ExtractionPos pos) {
            ExtractionPos outer = getEnclosingExtractionPos(pos.getParentPos());
            return outer == null ? pos : outer;
          }

          @Override
          public ExtractionPos caseLabelPos(LabelPos pos) {
            return getOutermostEnclosingExtractionPos(pos.getParentPos());
          }
        });
  }

  // return the innermost enclosing extraction position around 'pos' within the same function;
  // "null" if there is none
  public static ExtractionPos getEnclosingExtractionPos(NodePos pos) {
    return pos.accept(
        new PosSwitch<ExtractionPos>() {
          @Override
          public ExtractionPos caseRootPos(RootPos pos) {
            return null;
          }

          @Override
          public ExtractionPos caseChildPos(ChildPos pos) {
            int kind = pos.getParent().getKind();
            if (kind == CAstNode.FUNCTION_STMT || kind == CAstNode.FUNCTION_EXPR) return null;
            return getEnclosingExtractionPos(pos.getParentPos());
          }

          @Override
          public ExtractionPos caseForInLoopBodyPos(ExtractionPos pos) {
            return pos;
          }

          @Override
          public ExtractionPos caseLabelPos(LabelPos pos) {
            return getEnclosingExtractionPos(pos.getParentPos());
          }
        });
  }

  // is this the outermost for-in loop within its enclosing function?
  public boolean isOutermost() {
    return getEnclosingExtractionPos(parent_pos) == null;
  }

  public NodePos getParentPos() {
    return parent_pos;
  }
}
