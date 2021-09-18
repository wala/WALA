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
package com.ibm.wala.classLoader;

import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.core.util.bytecode.BytecodeStream;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.debug.UnimplementedError;
import java.util.Collection;
import java.util.Collections;

/**
 * An implementation of {@link IMethod}, usually for a synthesized method that is not read directly
 * from any source {@link Module}.
 */
public class SyntheticMethod implements IMethod {

  public static final SSAInstruction[] NO_STATEMENTS = new SSAInstruction[0];

  private final MethodReference method;

  protected final IMethod resolvedMethod;

  public final IClass declaringClass;

  private final boolean isStatic;

  private final boolean isFactory;

  public SyntheticMethod(
      MethodReference method, IClass declaringClass, boolean isStatic, boolean isFactory) {
    super();
    if (method == null) {
      throw new IllegalArgumentException("null method");
    }
    this.method = method;
    this.resolvedMethod = null;
    this.declaringClass = declaringClass;
    this.isStatic = isStatic;
    this.isFactory = isFactory;
  }

  public SyntheticMethod(
      IMethod method, IClass declaringClass, boolean isStatic, boolean isFactory) {
    super();
    if (method == null) {
      throw new IllegalArgumentException("null method");
    }
    this.resolvedMethod = method;
    this.method = resolvedMethod.getReference();
    this.declaringClass = declaringClass;
    this.isStatic = isStatic;
    this.isFactory = isFactory;
  }

  @Override
  public boolean isClinit() {
    return method.getSelector().equals(MethodReference.clinitSelector);
  }

  @Override
  public boolean isInit() {
    return method.getSelector().equals(MethodReference.initSelector);
  }

  /** @see com.ibm.wala.classLoader.IMethod#isStatic() */
  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isNative() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isProtected() {
    return false;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public boolean isBridge() {
    return false;
  }

  /** @see com.ibm.wala.classLoader.IMethod#isAbstract() */
  @Override
  public boolean isSynchronized() {
    return false;
  }

  @Override
  public boolean isAnnotation() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isModule() {
    return false;
  }

  @Override
  public boolean isWalaSynthetic() {
    return true;
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public MethodReference getReference() {
    return method;
  }

  /**
   * Create an {@link InducedCFG} from an instruction array.
   *
   * <p>NOTE: SIDE EFFECT!!! ... nulls out phi instructions in the instruction array!
   */
  public InducedCFG makeControlFlowGraph(SSAInstruction[] instructions) {
    return this.getDeclaringClass()
        .getClassLoader()
        .getLanguage()
        .makeInducedCFG(instructions, this, Everywhere.EVERYWHERE);
  }

  public BytecodeStream getBytecodeStream() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * TODO: why isn't this abstract?
   *
   * @see com.ibm.wala.classLoader.IMethod#getMaxLocals()
   *
   * @throws UnsupportedOperationException unconditionally
   */
  public int getMaxLocals() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * TODO: why isn't this abstract?
   *
   * @see com.ibm.wala.classLoader.IMethod#getMaxStackHeight()
   */
  public int getMaxStackHeight() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IClass getDeclaringClass() {
    return declaringClass;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("synthetic ");
    if (isFactoryMethod()) {
      s.append(" factory ");
    }
    s.append(method.toString());
    return s.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((declaringClass == null) ? 0 : declaringClass.hashCode());
    result = prime * result + ((method == null) ? 0 : method.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final SyntheticMethod other = (SyntheticMethod) obj;
    if (declaringClass == null) {
      if (other.declaringClass != null) return false;
    } else if (!declaringClass.equals(other.declaringClass)) return false;
    if (method == null) {
      if (other.method != null) return false;
    } else if (!method.equals(other.method)) return false;
    return true;
  }

  @Override
  public boolean hasExceptionHandler() {
    return false;
  }

  public boolean hasPoison() {
    return false;
  }

  public String getPoison() {
    return null;
  }

  public byte getPoisonLevel() {
    return -1;
  }

  /*
   * TODO: why isn't this abstract?
   *
   * @param options options governing SSA construction
   */
  @Deprecated
  public SSAInstruction[] getStatements(@SuppressWarnings("unused") SSAOptions options) {
    return NO_STATEMENTS;
  }

  /**
   * Most subclasses should override this.
   *
   * @param context TODO
   * @param options options governing IR conversion
   */
  public IR makeIR(Context context, SSAOptions options) throws UnimplementedError {
    throw new UnimplementedError("haven't implemented IR yet for class " + getClass());
  }

  @Override
  public TypeReference getParameterType(int i) {
    if (isStatic()) {
      return method.getParameterType(i);
    } else {
      if (i == 0) {
        return method.getDeclaringClass();
      } else {
        return method.getParameterType(i - 1);
      }
    }
  }

  @Override
  public int getNumberOfParameters() {
    int n = method.getNumberOfParameters();
    return isStatic() ? n : n + 1;
  }

  @Override
  public TypeReference[] getDeclaredExceptions() throws InvalidClassFileException {
    if (resolvedMethod == null) {
      return null;
    } else {
      return resolvedMethod.getDeclaredExceptions();
    }
  }

  @Override
  public Atom getName() {
    return method.getSelector().getName();
  }

  @Override
  public Descriptor getDescriptor() {
    return method.getSelector().getDescriptor();
  }
  /* BEGIN Custom change: : precise bytecode positions */

  @Override
  public SourcePosition getSourcePosition(int bcIndex) throws InvalidClassFileException {
    return null;
  }

  @Override
  public SourcePosition getParameterSourcePosition(int paramNum) throws InvalidClassFileException {
    return null;
  }
  /* END Custom change: precise bytecode positions */

  @Override
  public int getLineNumber(int bcIndex) {
    return -1;
  }

  public boolean isFactoryMethod() {
    return isFactory;
  }

  @Override
  public String getSignature() {
    return getReference().getSignature();
  }

  @Override
  public Selector getSelector() {
    return getReference().getSelector();
  }

  @Override
  public String getLocalVariableName(int bcIndex, int localNumber) {
    // no information is available
    return null;
  }

  @Override
  public boolean hasLocalVariableTable() {
    return false;
  }

  public SSAInstruction[] getStatements() {
    return getStatements(SSAOptions.defaultOptions());
  }

  @Override
  public TypeReference getReturnType() {
    return getReference().getReturnType();
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return getDeclaringClass().getClassHierarchy();
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return Collections.emptySet();
  }
}
