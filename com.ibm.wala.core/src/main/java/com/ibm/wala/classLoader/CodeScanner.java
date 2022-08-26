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

import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Simple utilities to scan {@link IMethod}s to gather information without building an IR. */
public class CodeScanner {

  /** @throws IllegalArgumentException if m is null */
  public static Collection<CallSiteReference> getCallSites(IMethod m)
      throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getCallSites(sm.getStatements());
    } else {
      IBytecodeMethod<?> bm = (IBytecodeMethod<?>) m;
      return bm.getCallSites();
    }
  }

  /** @throws IllegalArgumentException if m is null */
  public static Collection<FieldReference> getFieldsRead(IMethod m)
      throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getFieldsRead(sm.getStatements());
    } else {
      IBytecodeMethod<?> bm = (IBytecodeMethod<?>) m;
      ArrayList<FieldReference> result = new ArrayList<>();
      for (FieldReference fr : Iterator2Iterable.make(bm.getFieldsRead())) {
        result.add(fr);
      }
      return result;
    }
  }

  /** @throws IllegalArgumentException if m is null */
  public static Collection<FieldReference> getFieldsWritten(IMethod m)
      throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getFieldsWritten(sm.getStatements());
    } else {
      IBytecodeMethod<?> bm = (IBytecodeMethod<?>) m;
      ArrayList<FieldReference> result = new ArrayList<>();
      for (FieldReference fr : Iterator2Iterable.make(bm.getFieldsWritten())) {
        result.add(fr);
      }
      return result;
    }
  }

  /** get the element types of the arrays that m may update */
  public static Collection<TypeReference> getArraysWritten(IMethod m)
      throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getArraysWritten(sm.getStatements());
    } else {
      IBytecodeMethod<?> bm = (IBytecodeMethod<?>) m;
      ArrayList<TypeReference> result = new ArrayList<>();
      for (TypeReference tr : Iterator2Iterable.make(bm.getArraysWritten())) {
        result.add(tr);
      }
      return result;
    }
  }

  /** @throws IllegalArgumentException if m is null */
  public static Collection<NewSiteReference> getNewSites(IMethod m)
      throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getNewSites(sm.getStatements());
    } else {
      IBytecodeMethod<?> bm = (IBytecodeMethod<?>) m;
      return bm.getNewSites();
    }
  }

  public static boolean hasObjectArrayLoad(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
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
    if (m.isWalaSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return hasObjectArrayStore(sm.getStatements());
    } else {
      return hasShrikeBTObjectArrayStore((ShrikeCTMethod) m);
    }
  }

  public static Set<TypeReference> getCaughtExceptions(IMethod m) throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
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
   * @throws IllegalArgumentException if m is null
   */
  public static Iterator<TypeReference> iterateCastTypes(IMethod m)
      throws InvalidClassFileException {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (m.isWalaSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return iterateCastTypes(sm.getStatements());
    } else {
      return iterateShrikeBTCastTypes((ShrikeCTMethod) m);
    }
  }

  private static Iterator<TypeReference> iterateShrikeBTCastTypes(ShrikeCTMethod wrapper)
      throws InvalidClassFileException {
    return wrapper.getCastTypes();
  }

  private static Set<TypeReference> getShrikeBTCaughtExceptions(ShrikeCTMethod method)
      throws InvalidClassFileException {
    return method.getCaughtExceptionTypes();
  }

  private static boolean hasShrikeBTObjectArrayStore(ShrikeCTMethod M)
      throws InvalidClassFileException {
    for (TypeReference t : Iterator2Iterable.make(M.getArraysWritten())) {
      if (t.isReferenceType()) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasShrikeBTObjectArrayLoad(ShrikeCTMethod M)
      throws InvalidClassFileException {
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
  public static Set<TypeReference> getCaughtExceptions(SSAInstruction[] statements)
      throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final HashSet<TypeReference> result = HashSetFactory.make(10);
    Visitor v =
        new Visitor() {
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

  /** @throws IllegalArgumentException if statements == null */
  public static Iterator<TypeReference> iterateCastTypes(SSAInstruction[] statements)
      throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final HashSet<TypeReference> result = HashSetFactory.make(10);
    for (SSAInstruction statement : statements) {
      if (statement != null) {
        if (statement instanceof SSACheckCastInstruction) {
          SSACheckCastInstruction c = (SSACheckCastInstruction) statement;
          result.addAll(Arrays.asList(c.getDeclaredResultTypes()));
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
    final List<CallSiteReference> result = new ArrayList<>();
    Visitor v =
        new Visitor() {
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
    final List<NewSiteReference> result = new ArrayList<>();
    Visitor v =
        new Visitor() {
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
  public static List<FieldReference> getFieldsRead(SSAInstruction[] statements)
      throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final List<FieldReference> result = new ArrayList<>();
    Visitor v =
        new Visitor() {
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
  public static List<FieldReference> getFieldsWritten(SSAInstruction[] statements)
      throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final List<FieldReference> result = new ArrayList<>();
    Visitor v =
        new Visitor() {
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
  public static List<TypeReference> getArraysWritten(SSAInstruction[] statements)
      throws IllegalArgumentException {
    if (statements == null) {
      throw new IllegalArgumentException("statements == null");
    }
    final List<TypeReference> result = new ArrayList<>();
    Visitor v =
        new Visitor() {

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

  public static boolean hasObjectArrayLoad(SSAInstruction[] statements)
      throws IllegalArgumentException {

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

  public static boolean hasObjectArrayStore(SSAInstruction[] statements)
      throws IllegalArgumentException {

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
