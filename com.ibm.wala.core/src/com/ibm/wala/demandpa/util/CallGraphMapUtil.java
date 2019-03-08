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
package com.ibm.wala.demandpa.util;

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractLocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.MultiNewArrayInNode;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ExceptionReturnValueKey;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import java.util.Set;

/**
 * utility methods for mapping various program entities from one call graph to the corresponding
 * entity in another one
 *
 * @author Manu Sridharan
 */
public class CallGraphMapUtil {

  /**
   * map a call graph node from one call graph to the corresponding node in another. Note that the
   * target call graph must be context-insensitive for the method, i.e., the only context for the
   * method should be Everywhere.EVERYWHERE.
   *
   * @return the corresponding node, or {@code null} if the method is not in the target call graph
   * @throws IllegalArgumentException if fromCG == null
   */
  public static CGNode mapCGNode(CGNode orig, CallGraph fromCG, CallGraph toCG)
      throws IllegalArgumentException {
    if (fromCG == null) {
      throw new IllegalArgumentException("fromCG == null");
    }
    if (orig == fromCG.getFakeRootNode()) {
      return toCG.getFakeRootNode();
    } else {
      MethodReference methodRef = orig.getMethod().getReference();
      if (methodRef
          .toString()
          .equals("< Primordial, Ljava/lang/Object, clone()Ljava/lang/Object; >")) {
        // NOTE: clone() is cloned one level, even by RTA, so we need to handle it
        CGNode ret = toCG.getNode(orig.getMethod(), orig.getContext());
        if (ret == null) {
          System.err.println(("WEIRD can't map node " + orig));
        }
        return ret;
      } else {
        Set<CGNode> nodes = toCG.getNodes(methodRef);
        int size = nodes.size();
        assert size <= 1;
        return (size == 0) ? null : nodes.iterator().next();
      }
    }
  }

  public static InstanceKey mapInstKey(
      InstanceKey ik, CallGraph fromCG, CallGraph toCG, HeapModel heapModel)
      throws UnimplementedError, NullPointerException {
    InstanceKey ret = null;
    if (ik instanceof InstanceKeyWithNode) {
      CGNode oldCGNode = ((InstanceKeyWithNode) ik).getNode();
      CGNode newCGNode = mapCGNode(oldCGNode, fromCG, toCG);
      if (newCGNode == null) {
        return null;
      }
      if (ik instanceof AllocationSiteInNode) {
        if (ik instanceof NormalAllocationInNode) {
          ret =
              heapModel.getInstanceKeyForAllocation(
                  newCGNode, ((AllocationSiteInNode) ik).getSite());
        } else if (ik instanceof MultiNewArrayInNode) {
          MultiNewArrayInNode mnik = (MultiNewArrayInNode) ik;

          ret = heapModel.getInstanceKeyForMultiNewArray(newCGNode, mnik.getSite(), mnik.getDim());
        } else {
          Assertions.UNREACHABLE();
        }
      } else {
        Assertions.UNREACHABLE();
      }
    } else if (ik instanceof ConcreteTypeKey) {
      return ik;
    } else {
      Assertions.UNREACHABLE();
    }
    assert ret != null;
    assert ret.getClass() == ik.getClass();
    return ret;
  }

  public static PointerKey mapPointerKey(
      PointerKey pk, CallGraph fromCG, CallGraph toCG, HeapModel heapModel)
      throws UnimplementedError {
    PointerKey ret = null;
    if (pk instanceof AbstractLocalPointerKey) {
      CGNode oldCGNode = ((AbstractLocalPointerKey) pk).getNode();
      CGNode newCGNode = mapCGNode(oldCGNode, fromCG, toCG);
      if (newCGNode == null) {
        return null;
      }
      if (pk instanceof LocalPointerKey) {
        ret = heapModel.getPointerKeyForLocal(newCGNode, ((LocalPointerKey) pk).getValueNumber());
      } else if (pk instanceof ReturnValueKey) {
        // NOTE: must check for ExceptionReturnValueKey first,
        // since its a subclass if ReturnValueKey
        if (pk instanceof ExceptionReturnValueKey) {
          ret = heapModel.getPointerKeyForExceptionalReturnValue(newCGNode);
        } else {
          ret = heapModel.getPointerKeyForReturnValue(newCGNode);
        }
      } else {
        Assertions.UNREACHABLE();
      }
    } else {
      Assertions.UNREACHABLE();
    }
    assert ret != null;
    return ret;
  }
}
