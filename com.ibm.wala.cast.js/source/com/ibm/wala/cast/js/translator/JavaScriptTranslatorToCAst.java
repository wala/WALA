/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.translator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.debug.Assertions;

public interface JavaScriptTranslatorToCAst extends TranslatorToCAst {

  interface WalkContext<C extends WalkContext<C, T>, T> extends TranslatorToCAst.WalkContext<C, T> {

    @Override
    WalkContext<C, T> getParent();    

    default String script()  {
      return getParent().script();
    }

    /**
     * Add a name declaration to this context. For variables or constants, n
     * should be a {@link CAstNode#DECL_STMT}, and the initialization of the
     * variable (if any) may occur in a separate assignment. For functions, n
     * should be a {@link CAstNode#FUNCTION_STMT}, including the function body.
     */
    default void addNameDecl(CAstNode n) {
      getParent().addNameDecl(n);
    }

    default Collection<CAstNode> getNameDecls() {
      return getParent().getNameDecls();
    }

    default int setOperation(T node) {
      return getParent().setOperation(node);
    }

    default boolean foundMemberOperation(T node) {
      return getParent().foundMemberOperation(node);
    }

    default void copyOperation(T from, T to) {
      getParent().copyOperation(from, to);
    }
  }

  public static class RootContext<C extends WalkContext<C, T>, T> extends TranslatorToCAst.RootContext<C, T> implements WalkContext<C,T> {

    @Override
    public WalkContext<C, T> getParent() {
      assert false;
      return null;
    }

    @Override
    public String script() { return null; }

    @Override
    public T top() { 
      Assertions.UNREACHABLE();
      return null;
    }

    @Override
    public void addNameDecl(CAstNode v) {
      Assertions.UNREACHABLE();
    }

    @Override
    public Collection<CAstNode> getNameDecls() {
      Assertions.UNREACHABLE();
      return null;
    }

    @Override
    public CAstNode getCatchTarget() { 
      Assertions.UNREACHABLE();
      return null;
    }

    @Override
    public int setOperation(T node) {
      return -1;
    }

    @Override
    public boolean foundMemberOperation(T node) {
      return false;
    }

    @Override
    public void copyOperation(T from, T to) {
      Assertions.UNREACHABLE();
    }

  }

   public static class FunctionContext<C extends WalkContext<C, T>, T> extends TranslatorToCAst.FunctionContext<C, T> implements WalkContext<C,T> {

    private final Vector<CAstNode> initializers = new Vector<>();

    
    @Override
    public WalkContext<C, T> getParent() {
      return (WalkContext<C, T>) super.getParent();
    }

    protected FunctionContext(C parent, T s) {
      super(parent, s);
    }

    @Override
    public void addNameDecl(CAstNode v) { initializers.add(v); }

    @Override
    public Collection<CAstNode> getNameDecls() { return initializers; }

    @Override
    public String script() {
      return parent.script();
    }

    @Override
    public CAstNode getCatchTarget() {
      return CAstControlFlowMap.EXCEPTION_TO_EXIT;
    }

    @Override
    public int setOperation(T node) {
      return parent.setOperation(node);
    }

    @Override
    public boolean foundMemberOperation(T node) {
      return parent.foundMemberOperation(node);
    }

    @Override
    public void copyOperation(T from, T to) {
      parent.copyOperation(from, to);
    }
  }

  public static class ScriptContext<C extends WalkContext<C, T>, T> extends FunctionContext<C,T> {
    private final String script;

    ScriptContext(C parent, T s, String script) {
      super(parent, s);
      this.script = script;
    }

    @Override
    public String script() { return script; }
  }

  /**
   * Used to determine the value to be passed as the 'this' argument for a
   * function call. This is needed since in JavaScript, you can write a call
   * e(...) where e is some arbitrary expression, and in the case where e is a
   * property access like e'.f, we must discover that the value of expression e'
   * is passed as the 'this' parameter.
   * 
   * The general strategy is to store the value of the expression passed as the
   * 'this' parameter in baseVar, and then to use baseVar as the actual argument
   * sub-node for the CAst call node
   */
  public class MemberDestructuringContext<C extends WalkContext<C, T>, T> implements WalkContext<C,T> {
    private final WalkContext<C, T> parent;
    
    /**
     * node for which we actually care about what the base pointer is. this
     * helps to handle cases like x.y.f(), where we would like to store x.y in
     * baseVar, but not x when we recurse.
     */
    private final Set<T> baseFor = new HashSet<>();

    private int operationIndex;
    
    /**
     * have we discovered a value to be passed as the 'this' parameter?
     */
    private boolean foundBase = false;

    protected MemberDestructuringContext(C parent, T initialBaseFor, int operationIndex) {
      this.parent = parent;
      baseFor.add( initialBaseFor );
      this.operationIndex = operationIndex;
    }

    @Override
    public int setOperation(T node) { 
      if (baseFor.contains( node )) {
        foundBase = true;
        return operationIndex;
      } else {
        return -1;
      }
    }
      
    @Override
    public boolean foundMemberOperation(T node) {
      return foundBase;
    }

    @Override
    public void copyOperation(T from, T to) {
      if (baseFor.contains(from)) baseFor.add(to);
    }

    @Override
    public WalkContext<C, T> getParent() {
      return parent;
    }
  }

}
