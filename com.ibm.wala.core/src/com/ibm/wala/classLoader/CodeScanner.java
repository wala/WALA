/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.classLoader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Simple utilities to scan {@link IMethod}s to gather information without building an IR.
 */
public class CodeScanner {

  /**
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException if m is null
   */
  public static Collection<CallSiteReference> getCallSites(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getCallSites(sm.getStatements());
    } else {
      return getCallSitesFromShrikeBT((IBytecodeMethod) m);
    }
  }

  /**
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException if m is null
   */
  public static Collection<FieldReference> getFieldsRead(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getFieldsRead(sm.getStatements());
    } else {
      return getFieldsReadFromShrikeBT((ShrikeCTMethod) m);
    }
  }

  /**
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException if m is null
   */
  public static Collection<FieldReference> getFieldsWritten(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getFieldsWritten(sm.getStatements());
    } else {
      return getFieldsWrittenFromShrikeBT((ShrikeCTMethod) m);
    }
  }

  /**
   * get the element types of the arrays that m may update
   * 
   * @throws InvalidClassFileException
   */
  public static Collection<TypeReference> getArraysWritten(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getArraysWritten(sm.getStatements());
    } else {
      return getArraysWrittenFromShrikeBT((ShrikeCTMethod) m);
    }
  }

  /**
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException if m is null
   */
  public static Collection<NewSiteReference> getNewSites(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getNewSites(sm.getStatements());
    } else {
      return getNewSitesFromShrikeBT((ShrikeCTMethod) m);
    }
  }

  public static boolean hasObjectArrayLoad(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return hasObjectArrayLoad(sm.getStatements());
    } else {
      return hasShrikeBTObjectArrayLoad((ShrikeCTMethod) m);
    }
  }

  public static boolean hasObjectArrayStore(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return hasObjectArrayStore(sm.getStatements());
    } else {
      return hasShrikeBTObjectArrayStore((ShrikeCTMethod) m);
    }
  }

  public static Set getCaughtExceptions(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getCaughtExceptions(sm.getStatements());
    } else {
      return getShrikeBTCaughtExceptions((ShrikeCTMethod) m);
    }
  }

  /**
   * Return the types this method may cast to
   * 
   * @return iterator of TypeReference
   * @throws InvalidClassFileException
   * @throws IllegalArgumentException if m is null
   */
  public static Iterator<TypeReference> iterateCastTypes(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return iterateCastTypes(sm.getStatements());
    } else {
      return iterateShrikeBTCastTypes((ShrikeCTMethod) m);
    }
  }

  private static Iterator<TypeReference> iterateShrikeBTCastTypes(ShrikeCTMethod wrapper) throws InvalidClassFileException {
    return wrapper.getCastTypes();
  }

  private static Set getShrikeBTCaughtExceptions(ShrikeCTMethod method) throws InvalidClassFileException {
    return method.getCaughtExceptionTypes();
  }

  private static boolean hasShrikeBTObjectArrayStore(ShrikeCTMethod M) throws InvalidClassFileException {
    for (TypeReference t : Iterator2Iterable.make(M.getArraysWritten())) {
      if (t.isReferenceType()) {
        return true;
      }
    }
    return false;
  }

  private static Collection<CallSiteReference> getCallSitesFromShrikeBT(IBytecodeMethod<?> M) throws InvalidClassFileException {
    return M.getCallSites();
  }

  /**
   * @param M
   * @return Iterator of TypeReference
   * @throws InvalidClassFileException
   */
  private static Collection<NewSiteReference> getNewSitesFromShrikeBT(ShrikeCTMethod M) throws InvalidClassFileException {
    return M.getNewSites();
  }

  private static List<FieldReference> getFieldsReadFromShrikeBT(ShrikeCTMethod M) throws InvalidClassFileException {
    // TODO move the logic here from ShrikeCTMethodWrapper
    LinkedList<FieldReference> result = new LinkedList<>();
    for (FieldReference fr : Iterator2Iterable.make(M.getFieldsRead())) {
      result.add(fr);
    }
    return result;
  }

  private static List<FieldReference> getFieldsWrittenFromShrikeBT(ShrikeCTMethod M) throws InvalidClassFileException {
    // TODO move the logic here from ShrikeCTMethodWrapper
    LinkedList<FieldReference> result = new LinkedList<>();
    for (FieldReference fr : Iterator2Iterable.make(M.getFieldsWritten())) {
      result.add(fr);
    }
    return result;
  }

  private static List<TypeReference> getArraysWrittenFromShrikeBT(ShrikeCTMethod M) throws InvalidClassFileException {
    // TODO move the logic here from ShrikeCTMethodWrapper
    List<TypeReference> result = new LinkedList<>();
    for (TypeReference tr : Iterator2Iterable.make(M.getArraysWritten())) {
      result.add(tr);
    }
    return result;
  }

  private static boolean hasShrikeBTObjectArrayLoad(ShrikeCTMethod M) throws InvalidClassFileException {
    for (TypeReference tr : Iterator2Iterable.make(M.getArraysRead())) {
      if (tr.isReferenceType()) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return {@link Set}&lt;{@link TypeReference}&gt;
   * @throws IllegalArgumentException if statements == null
   */
  public static Set<TypeReference> getCaughtExceptions(SSAInstruction[] statements) throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final HashSet<TypeReference> result = HashSetFactory.make(10);
    Visitor v = new Visitor() {
      @Override
      public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
        Collection<TypeReference> t = instruction.getExceptionTypes();
        result.addAll(t);
      }
    };
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @throws IllegalArgumentException if statements == null
   */
  public static Iterator<TypeReference> iterateCastTypes(SSAInstruction[] statements) throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final HashSet<TypeReference> result = HashSetFactory.make(10);
    for (SSAInstruction statement : statements) {
      if (statement != null) {
        if (statement instanceof SSACheckCastInstruction) {
          SSACheckCastInstruction c = (SSACheckCastInstruction) statement;
          for(TypeReference t : c.getDeclaredResultTypes()) {
            result.add(t);
          }
        }
      }
    }
    return result.iterator();
  }

  /**
   * @param statements list of ssa statements
   * @return List of InvokeInstruction
   */
  private static List<CallSiteReference> getCallSites(SSAInstruction[] statements) {
    final List<CallSiteReference> result = new LinkedList<>();
    Visitor v = new Visitor() {
      @Override
      public void visitInvoke(SSAInvokeInstruction instruction) {
        result.add(instruction.getCallSite());
      }
    };
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @param statements list of ssa statements
   * @return List of InvokeInstruction
   */
  private static List<NewSiteReference> getNewSites(SSAInstruction[] statements) {
    final List<NewSiteReference> result = new LinkedList<>();
    Visitor v = new Visitor() {
      @Override
      public void visitNew(SSANewInstruction instruction) {
        result.add(instruction.getNewSite());
      }
    };
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @param statements list of ssa statements
   * @return List of FieldReference
   * @throws IllegalArgumentException if statements == null
   */
  public static List<FieldReference> getFieldsRead(SSAInstruction[] statements) throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final List<FieldReference> result = new LinkedList<>();
    Visitor v = new Visitor() {
      @Override
      public void visitGet(SSAGetInstruction instruction) {
        result.add(instruction.getDeclaredField());
      }
    };
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @param statements list of ssa statements
   * @return List of FieldReference
   * @throws IllegalArgumentException if statements == null
   */
  public static List<FieldReference> getFieldsWritten(SSAInstruction[] statements) throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final List<FieldReference> result = new LinkedList<>();
    Visitor v = new Visitor() {
      @Override
      public void visitPut(SSAPutInstruction instruction) {
        result.add(instruction.getDeclaredField());
      }
    };
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @param statements list of ssa statements
   * @return List of TypeReference
   * @throws IllegalArgumentException if statements == null
   */
  public static List<TypeReference> getArraysWritten(SSAInstruction[] statements) throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final List<TypeReference> result = new LinkedList<>();
    Visitor v = new Visitor() {

      @Override
      public void visitArrayStore(SSAArrayStoreInstruction instruction) {
        result.add(instruction.getElementType());
      }
      
    };
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  public static boolean hasObjectArrayLoad(SSAInstruction[] statements) throws IllegalArgumentException {

    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    class ScanVisitor extends Visitor {
      boolean foundOne = false;

      @Override
      public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
        if (!instruction.typeIsPrimitive()) {
          foundOne = true;
        }
      }
    }
    ScanVisitor v = new ScanVisitor();
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
      if (v.foundOne) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasObjectArrayStore(SSAInstruction[] statements) throws IllegalArgumentException {

    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    class ScanVisitor extends Visitor {
      boolean foundOne = false;

      @Override
      public void visitArrayStore(SSAArrayStoreInstruction instruction) {
        if (!instruction.typeIsPrimitive()) {
          foundOne = true;
        }
      }
    }
    ScanVisitor v = new ScanVisitor();
    for (SSAInstruction s : statements) {
      if (s != null) {
        s.visit(v);
      }
      if (v.foundOne) {
        return true;
      }
    }
    return false;
  }
}
