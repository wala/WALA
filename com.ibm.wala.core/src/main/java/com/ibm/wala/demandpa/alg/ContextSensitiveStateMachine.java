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
package com.ibm.wala.demandpa.alg;

import com.ibm.wala.demandpa.alg.statemachine.StateMachine;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.alg.statemachine.StatesMergedException;
import com.ibm.wala.demandpa.flowgraph.AssignBarLabel;
import com.ibm.wala.demandpa.flowgraph.AssignGlobalBarLabel;
import com.ibm.wala.demandpa.flowgraph.AssignGlobalLabel;
import com.ibm.wala.demandpa.flowgraph.AssignLabel;
import com.ibm.wala.demandpa.flowgraph.GetFieldBarLabel;
import com.ibm.wala.demandpa.flowgraph.GetFieldLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.demandpa.flowgraph.MatchBarLabel;
import com.ibm.wala.demandpa.flowgraph.MatchLabel;
import com.ibm.wala.demandpa.flowgraph.NewBarLabel;
import com.ibm.wala.demandpa.flowgraph.NewLabel;
import com.ibm.wala.demandpa.flowgraph.ParamBarLabel;
import com.ibm.wala.demandpa.flowgraph.ParamLabel;
import com.ibm.wala.demandpa.flowgraph.PutFieldBarLabel;
import com.ibm.wala.demandpa.flowgraph.PutFieldLabel;
import com.ibm.wala.demandpa.flowgraph.ReturnBarLabel;
import com.ibm.wala.demandpa.flowgraph.ReturnLabel;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Collection;
import java.util.HashSet;

/**
 * A state machine for tracking calling context during a points-to query. Filters unrealizable
 * paths.
 */
public class ContextSensitiveStateMachine implements StateMachine<IFlowLabel> {

  private static final boolean DEBUG = false;

  private static final boolean DEBUG_RECURSION = false;

  /** The empty call stack. Note that the empty stack essentially represents all possible states. */
  private final CallStack emptyStack = CallStack.emptyCallStack();

  @Override
  public CallStack getStartState() {
    return emptyStack;
  }

  private final RecursionHandler recursionHandler;

  private class CSLabelVisitor implements IFlowLabelVisitor {

    final CallStack prevStack;

    State nextState = null;

    CSLabelVisitor(CallStack prevStack) {
      this.prevStack = prevStack;
    }

    @Override
    public void visitAssign(AssignLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitAssignBar(AssignBarLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitAssignGlobal(AssignGlobalLabel label, Object dst) {
      nextState = emptyStack;
    }

    @Override
    public void visitAssignGlobalBar(AssignGlobalBarLabel label, Object dst) {
      nextState = emptyStack;
    }

    @Override
    public void visitGetField(GetFieldLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitGetFieldBar(GetFieldBarLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitMatch(MatchLabel label, Object dst) {
      nextState = emptyStack;
    }

    @Override
    public void visitMatchBar(MatchBarLabel label, Object dst) {
      nextState = emptyStack;
    }

    @Override
    public void visitNew(NewLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitNewBar(NewBarLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitParam(ParamLabel label, Object dst) {
      handleMethodExit(label.getCallSite());
    }

    private void handleMethodExit(CallerSiteContext callSite) {
      if (recursionHandler.isRecursive(callSite)) {
        nextState = prevStack;
      } else if (prevStack.isEmpty()) {
        nextState = prevStack;
      } else if (prevStack.peek().equals(callSite)) {
        nextState = prevStack.pop();
      } else {
        nextState = ERROR;
      }
    }

    @Override
    public void visitParamBar(ParamBarLabel label, Object dst) {
      // method entry
      handleMethodEntry(label.getCallSite());
    }

    private void handleMethodEntry(CallerSiteContext callSite) {
      if (recursionHandler.isRecursive(callSite)) {
        // just ignore it; we don't track recursive calls
        nextState = prevStack;
      } else if (prevStack.contains(callSite)) {
        if (DEBUG_RECURSION) {
          System.err.println("FOUND RECURSION");
          System.err.println("stack " + prevStack + " contains " + callSite);
        }
        CallerSiteContext topCallSite = null;
        CallStack tmpStack = prevStack;
        // mark the appropriate call sites as recursive
        // and pop them
        Collection<CallerSiteContext> newRecursiveSites = HashSetFactory.make();
        do {
          topCallSite = tmpStack.peek();
          newRecursiveSites.add(topCallSite);
          tmpStack = tmpStack.pop();
        } while (!topCallSite.equals(callSite) && !tmpStack.isEmpty());
        recursionHandler.makeRecursive(newRecursiveSites);
        // here we throw the states merged exception to indicate
        // that recursion was detected
        // ideally, we would update all relevant data structures and continue,
        // but it greatly complicates the analysis implementation
        throw new StatesMergedException();
      } else {
        nextState = prevStack.push(callSite);
      }
    }

    @Override
    public void visitPutField(PutFieldLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitPutFieldBar(PutFieldBarLabel label, Object dst) {
      nextState = prevStack;
    }

    @Override
    public void visitReturn(ReturnLabel label, Object dst) {
      handleMethodEntry(label.getCallSite());
    }

    @Override
    public void visitReturnBar(ReturnBarLabel label, Object dst) {
      handleMethodExit(label.getCallSite());
    }
  }

  @Override
  public State transition(State prevState, IFlowLabel label)
      throws IllegalArgumentException, IllegalArgumentException {
    if (prevState == null) {
      throw new IllegalArgumentException("prevState == null");
    }
    if (!(prevState instanceof CallStack)) {
      throw new IllegalArgumentException(
          "not ( prevState instanceof com.ibm.wala.demandpa.alg.CallStack ) ");
    }
    CallStack prevStack = (CallStack) prevState;
    if (!prevStack.isEmpty() && recursionHandler.isRecursive(prevStack.peek())) {
      // I don't think this is possible anymore
      assert false;
      // just pop off the call site
      return transition(prevStack.pop(), label);
    }
    CSLabelVisitor v = new CSLabelVisitor(prevStack);
    label.visit(v, null);
    if (DEBUG) {
      if (prevStack != v.nextState && v.nextState != ERROR) {
        System.err.println("prev stack " + prevStack);
        System.err.println("label " + label);
        System.err.println("recursive call sites " + recursionHandler);
        System.err.println("next stack " + v.nextState);
      }
    }
    return v.nextState;
  }

  private ContextSensitiveStateMachine(RecursionHandler recursionHandler) {
    this.recursionHandler = recursionHandler;
  }

  public static class Factory implements StateMachineFactory<IFlowLabel> {

    private final RecursionHandler prototype;

    public Factory(RecursionHandler prototype) {
      this.prototype = prototype;
    }

    public Factory() {
      this(new BasicRecursionHandler());
    }

    @Override
    public StateMachine<IFlowLabel> make() {
      return new ContextSensitiveStateMachine(prototype.makeNew());
    }
  }

  public static interface RecursionHandler {

    public boolean isRecursive(CallerSiteContext callSite);

    public void makeRecursive(Collection<CallerSiteContext> callSites);

    /** in lieu of creating factories */
    public RecursionHandler makeNew();
  }

  /**
   * handles method recursion by only collapsing cycles of recursive calls observed during analysis
   */
  public static class BasicRecursionHandler implements RecursionHandler {

    private final HashSet<CallerSiteContext> recursiveCallSites = HashSetFactory.make();

    @Override
    public boolean isRecursive(CallerSiteContext callSite) {
      return recursiveCallSites.contains(callSite);
    }

    @Override
    public void makeRecursive(Collection<CallerSiteContext> callSites) {
      recursiveCallSites.addAll(callSites);
    }

    @Override
    public RecursionHandler makeNew() {
      return new BasicRecursionHandler();
    }
  }
}
