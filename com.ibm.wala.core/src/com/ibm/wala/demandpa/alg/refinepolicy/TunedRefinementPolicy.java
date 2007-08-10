/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright ©2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient’s reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents’ employees.
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

import java.util.Collection;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * A refinement policy that iteratively adds more types to refine, based on
 * which type was encountered first in each analysis pass.
 * 
 * @author Manu Sridharan
 * 
 */
public class TunedRefinementPolicy implements RefinementPolicy {

  private final ClassHierarchy cha;

  private class TunedFieldRefinementPolicy implements FieldRefinePolicy {

    private final Collection<IClass> typesToRefine = HashSetFactory.make();

    private IClass firstSkippedClass = null;

    public boolean nextPass() {
      if (firstSkippedClass != null) {
        typesToRefine.add(firstSkippedClass);
        firstSkippedClass = null;
        return true;
      } else {
        return false;
      }
    }

    public boolean shouldRefine(IField field) {
      if (field == ArrayContents.v()) {
        return true;
      }
      IClass classToCheck = removeInner(field.getDeclaringClass());
      if (superOfAnyEncountered(classToCheck)) {
        return true;
      } else {
        if (firstSkippedClass == null) {
          firstSkippedClass = classToCheck;
        }
        return false;
      }
    }

    private boolean superOfAnyEncountered(IClass klass) {
      for (IClass toRefine : typesToRefine) {
        if (cha.isAssignableFrom(klass, toRefine)) {
          return true;
        }
      }
      return false;
    }

    /**
     * 
     * @param klass
     * @return the top-level {@link IClass} where klass is declared, or klass
     *         itself if klass is top-level or if top-level class not loaded
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
        return (topMostClass != null) ? topMostClass : klass;
      }
    }

  }

  private final CallGraphRefinePolicy cgRefinePolicy = new AlwaysRefineCGPolicy();

  private final FieldRefinePolicy fieldRefinePolicy = new TunedFieldRefinementPolicy();

  private static final int NUM_PASSES = 3;

  private static final int[] BUDGET_PER_PASS = { 1000, 12000, 12000 };

  public TunedRefinementPolicy(ClassHierarchy cha) {
    this.cha = cha;
  }

  public int getBudgetForPass(int passNum) {
    return BUDGET_PER_PASS[passNum];
  }

  public CallGraphRefinePolicy getCallGraphRefinePolicy() {
    return cgRefinePolicy;
  }

  public FieldRefinePolicy getFieldRefinePolicy() {
    return fieldRefinePolicy;
  }

  public int getNumPasses() {
    return NUM_PASSES;
  }

  public boolean nextPass() {
    return fieldRefinePolicy.nextPass();
  }

  public static class Factory implements RefinementPolicyFactory {

    private final ClassHierarchy cha;

    public Factory(ClassHierarchy cha) {
      this.cha = cha;
    }

    public RefinementPolicy make() {
      return new TunedRefinementPolicy(cha);
    }

  }
}
