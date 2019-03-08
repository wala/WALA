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

import static com.ibm.wala.cast.tree.CAstNode.ASSIGN;
import static com.ibm.wala.cast.tree.CAstNode.BLOCK_STMT;
import static com.ibm.wala.cast.tree.CAstNode.DECL_STMT;
import static com.ibm.wala.cast.tree.CAstNode.EACH_ELEMENT_GET;
import static com.ibm.wala.cast.tree.CAstNode.EMPTY;
import static com.ibm.wala.cast.tree.CAstNode.LABEL_STMT;
import static com.ibm.wala.cast.tree.CAstNode.LOCAL_SCOPE;
import static com.ibm.wala.cast.tree.CAstNode.VAR;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.pattern.Alt;
import com.ibm.wala.cast.tree.pattern.AnyNode;
import com.ibm.wala.cast.tree.pattern.NodeOfKind;
import com.ibm.wala.cast.tree.pattern.SomeConstant;
import com.ibm.wala.cast.tree.pattern.SubtreeOfKind;
import java.util.Collections;
import java.util.List;

/**
 * A policy telling a {@link ClosureExtractor} to extract the body of every for-in loop.
 *
 * <p>NB: This policy matches on Rhino-specific encodings of for-in loops and hence is inherently
 * non-portable.
 *
 * @author mschaefer
 */
public class ForInBodyExtractionPolicy extends ExtractionPolicy {
  public static final ForInBodyExtractionPolicy INSTANCE = new ForInBodyExtractionPolicy();

  public static final ExtractionPolicyFactory FACTORY =
      new ExtractionPolicyFactory() {
        @Override
        public ExtractionPolicy createPolicy(CAstEntity entity) {
          return INSTANCE;
        }
      };

  private ForInBodyExtractionPolicy() {}

  @Override
  public List<ExtractionRegion> extract(CAstNode node) {
    SomeConstant loopVar = new SomeConstant();

    /*
     * matches Rhino 1.7.3 encoding of for-in loop bodies:
     *
     *   BLOCK_STMT
     *     decl/assign of loop variable
     *     optional SCOPE
     *       <loopBody>
     */
    if (new NodeOfKind(
            BLOCK_STMT,
            new Alt(
                new NodeOfKind(DECL_STMT, loopVar, new SubtreeOfKind(EACH_ELEMENT_GET)),
                new NodeOfKind(
                    ASSIGN, new NodeOfKind(VAR, loopVar), new SubtreeOfKind(EACH_ELEMENT_GET))),
            new AnyNode(),
            new NodeOfKind(BLOCK_STMT, new SubtreeOfKind(LABEL_STMT), new SubtreeOfKind(EMPTY)))
        .matches(node)) {
      List<String> parms = Collections.singletonList((String) loopVar.getLastMatch());
      if (node.getChild(1).getKind() == LOCAL_SCOPE) {
        return Collections.<ExtractionRegion>singletonList(
            new TwoLevelExtractionRegion(1, 2, 0, -1, parms, Collections.<String>emptyList()));
      } else {
        return Collections.singletonList(
            new ExtractionRegion(1, 2, parms, Collections.<String>emptyList()));
      }
    }

    /* matches Rhino < 1.7.3 encoding of for-in loop bodies:
     *
     *   BLOCK_STMT
     *     ASSIGN
     *       VAR <loopVar>
     *       EACH_ELEMENT_GET
     *         VAR <forin_tmp>
     *     <loopBody>
     */
    if (new NodeOfKind(
            BLOCK_STMT,
            new NodeOfKind(
                ASSIGN, new NodeOfKind(VAR, loopVar), new SubtreeOfKind(EACH_ELEMENT_GET)),
            new AnyNode())
        .matches(node)) {
      List<String> parms = Collections.singletonList((String) loopVar.getLastMatch());
      return Collections.singletonList(
          new ExtractionRegion(1, 2, parms, Collections.<String>emptyList()));
    }
    return null;
  }
}
