/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released by the University of
 * California under the terms listed below.
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 *
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.demandpa.alg.refinepolicy;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.demandpa.alg.statemachine.StateMachine;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Manually annotated policy for refining field accesses. */
public class ManualFieldPolicy implements FieldRefinePolicy {

  protected final Pattern refinePattern; // =

  // Pattern.compile("Lca/mcgill/sable/util|Ljava/util|Lpolyglot/util/TypedList");

  private static final int NUM_DECISIONS_TO_TRACK = 10;

  private final boolean[] decisions = new boolean[NUM_DECISIONS_TO_TRACK];

  private int curDecision;

  private final IClass[] encounteredClasses = new IClass[NUM_DECISIONS_TO_TRACK];

  @Override
  public boolean shouldRefine(
      IField field,
      PointerKey basePtr,
      PointerKey val,
      IFlowLabel label,
      StateMachine.State state) {
    if (field == null) {
      throw new IllegalArgumentException("null field");
    }
    if (field == ArrayContents.v()) {
      return true;
    } else {
      final IClass declaringClass = field.getDeclaringClass();
      final Matcher m = refinePattern.matcher(declaringClass.toString());
      final boolean foundPattern = m.find();
      recordDecision(declaringClass, foundPattern);
      return foundPattern;
    }
  }

  private void recordDecision(final IClass declaringClass, final boolean foundPattern) {
    if (curDecision < NUM_DECISIONS_TO_TRACK) {
      IClass topMostClass = removeInner(declaringClass);
      if (notSuperOfAnyEncountered(topMostClass)) {
        encounteredClasses[curDecision] = topMostClass;
        decisions[curDecision] = foundPattern;
        curDecision++;
      }
    }
  }

  private boolean notSuperOfAnyEncountered(IClass klass) {
    for (int i = 0; i < curDecision; i++) {
      if (cha.isAssignableFrom(klass, encounteredClasses[i])) {
        return false;
      }
    }
    return true;
  }

  private final IClassHierarchy cha;

  /**
   * @return the top-level {@link IClass} where klass is declared, or klass itself if klass is
   *     top-level
   */
  private IClass removeInner(IClass klass) {
    ClassLoaderReference cl = klass.getClassLoader().getReference();
    String klassStr = klass.getName().toString();
    int dollarIndex = klassStr.indexOf('$');
    if (dollarIndex == -1) {
      return klass;
    } else {
      String topMostName = klassStr.substring(0, dollarIndex);
      IClass topMostClass = cha.lookupClass(TypeReference.findOrCreate(cl, topMostName));
      assert topMostClass != null;
      return topMostClass;
    }
  }

  /**
   * @param refinePattern a pattern for detecting which match edges to refine. If the <em>declaring
   *     class</em> of the field related to the match edge matches the pattern, the match edge will
   *     be refined. For example, the pattern {@code Pattern.compile("Ljava/util")} will cause all
   *     fields of classes in the {@code java.util} package to be refined.
   */
  public ManualFieldPolicy(IClassHierarchy cha, Pattern refinePattern) {
    this.cha = cha;
    this.refinePattern = refinePattern;
  }

  @Override
  public boolean nextPass() {
    return false;
  }

  /** @return a String representation of the decisions made by this */
  public String getHistory() {
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < curDecision; i++) {
      if (decisions[i]) {
        ret.append("refined ");
      } else {
        ret.append("skipped ");
      }
      ret.append(encounteredClasses[i]);
      if (i + 1 < curDecision) {
        ret.append(", ");
      }
    }
    return ret.toString();
  }
}
