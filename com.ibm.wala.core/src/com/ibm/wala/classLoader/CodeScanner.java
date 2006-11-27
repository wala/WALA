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
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.warnings.WarningSet;

/**
 *
 * This class can scan an IMethod and generate a list of call sites in the
 * method.
 * 
 * @author sfink
 */
public class CodeScanner {

  /**
   * @return iterator of call site references.
   * @throws InvalidClassFileException
   */
  public static Iterator<CallSiteReference> iterateCallSites(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getCallSites(sm.getStatements(SSAOptions.defaultOptions(), warnings)).iterator();
    } else {
      return getCallSitesFromShrikeBT((ShrikeCTMethodWrapper) m);
    }
  }

  /**
   * @return iterator of FieldReference
   * @throws InvalidClassFileException
   */
  public static Iterator<FieldReference> iterateFieldsRead(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getFieldsRead(sm.getStatements(SSAOptions.defaultOptions(), warnings)).iterator();
    } else {
      return getFieldsReadFromShrikeBT((ShrikeCTMethodWrapper) m).iterator();
    }
  }

  /**
   * @return iterator of FieldReference
   * @throws InvalidClassFileException
   */
  public static Iterator<FieldReference> iterateFieldsWritten(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getFieldsWritten(sm.getStatements(SSAOptions.defaultOptions(), warnings)).iterator();
    } else {
      return getFieldsWrittenFromShrikeBT((ShrikeCTMethodWrapper) m).iterator();
    }
  }

  /**
   * @return iterator of TypeReference.
   * @throws InvalidClassFileException
   */
  public static Iterator<NewSiteReference> iterateNewSites(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getNewSites(sm.getStatements(SSAOptions.defaultOptions(), warnings)).iterator();
    } else {
      return getNewSitesFromShrikeBT((ShrikeCTMethodWrapper) m);
    }
  }

  public static boolean hasObjectArrayLoad(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return hasObjectArrayLoad(sm.getStatements(SSAOptions.defaultOptions(), warnings));
    } else {
      return hasShrikeBTObjectArrayLoad((ShrikeCTMethodWrapper) m);
    }
  }

  public static boolean hasObjectArrayStore(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return hasObjectArrayStore(sm.getStatements(SSAOptions.defaultOptions(), warnings));
    } else {
      return hasShrikeBTObjectArrayStore((ShrikeCTMethodWrapper) m);
    }
  }

  public static Set getCaughtExceptions(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return getCaughtExceptions(sm.getStatements(SSAOptions.defaultOptions(), warnings));
    } else {
      return getShrikeBTCaughtExceptions((ShrikeCTMethodWrapper) m);
    }
  }

  /**
   * Return the types this method may cast to
   * 
   * @return iterator of TypeReference
   * @throws InvalidClassFileException
   */
  public static Iterator iterateCastTypes(IMethod m, WarningSet warnings) throws InvalidClassFileException {
    if (m.isSynthetic()) {
      SyntheticMethod sm = (SyntheticMethod) m;
      return iterateCastTypes(sm.getStatements(SSAOptions.defaultOptions(), warnings));
    } else {
      return iterateShrikeBTCastTypes((ShrikeCTMethodWrapper) m);
    }
  }

  private static Iterator iterateShrikeBTCastTypes(ShrikeCTMethodWrapper wrapper) throws InvalidClassFileException {
    return wrapper.getCastTypes();
  }

  private static Set getShrikeBTCaughtExceptions(ShrikeCTMethodWrapper method) throws InvalidClassFileException {
    return method.getCaughtExceptionTypes();
  }

  private static boolean hasShrikeBTObjectArrayStore(ShrikeCTMethodWrapper M) throws InvalidClassFileException {
    for (Iterator it = M.getArraysWritten(); it.hasNext();) {
      TypeReference t = (TypeReference) it.next();
      if (t.isReferenceType()) {
        return true;
      }
    }
    return false;
  }

  private static Iterator<CallSiteReference> getCallSitesFromShrikeBT(ShrikeCTMethodWrapper M) throws InvalidClassFileException {
    return M.getCallSites();
  }

  /**
   * @param M
   * @return Iterator of TypeReference
   * @throws InvalidClassFileException
   */
  private static Iterator<NewSiteReference> getNewSitesFromShrikeBT(ShrikeCTMethodWrapper M) throws InvalidClassFileException {
    return M.getNewSites();
  }

  private static List<FieldReference> getFieldsReadFromShrikeBT(ShrikeCTMethodWrapper M) throws InvalidClassFileException {
    // TODO move the logic here from ShrikeCTMethodWrapper
    LinkedList<FieldReference> result = new LinkedList<FieldReference>();
    for (Iterator<FieldReference> it = M.getFieldsRead(); it.hasNext();) {
      result.add(it.next());
    }
    return result;
  }

  private static List<FieldReference> getFieldsWrittenFromShrikeBT(ShrikeCTMethodWrapper M) throws InvalidClassFileException {
    // TODO move the logic here from ShrikeCTMethodWrapper
    LinkedList<FieldReference> result = new LinkedList<FieldReference>();
    for (Iterator<FieldReference> it = M.getFieldsWritten(); it.hasNext();) {
      result.add(it.next());
    }
    return result;
  }

  private static boolean hasShrikeBTObjectArrayLoad(ShrikeCTMethodWrapper M) throws InvalidClassFileException {
    for (Iterator it = M.getArraysRead(); it.hasNext();) {
      TypeReference t = (TypeReference) it.next();
      if (t.isReferenceType()) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return Set <TypeReference>
   */
  public static Set<TypeReference> getCaughtExceptions(SSAInstruction[] statements) {
    final HashSet<TypeReference> result = HashSetFactory.make(10);
    Visitor v = new Visitor() {
      public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
        Collection<TypeReference> t = instruction.getExceptionTypes();
        result.addAll(t);
      }
    };
    for (int i = 0; i < statements.length; i++) {
      SSAInstruction s = statements[i];
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   */
  public static Iterator<TypeReference> iterateCastTypes(SSAInstruction[] statements) {
    final HashSet<TypeReference> result = HashSetFactory.make(10);
    for (int i = 0; i < statements.length; i++) {
      if (statements[i] != null) {
        if (statements[i] instanceof SSACheckCastInstruction) {
          SSACheckCastInstruction c = (SSACheckCastInstruction) statements[i];
          result.add(c.getDeclaredResultType());
        }
      }
    }
    return result.iterator();
  }

  /**
   * @param statements
   *          list of ssa statements
   * @return List of InvokeInstruction
   */
  private static List<CallSiteReference> getCallSites(SSAInstruction[] statements) {
    final List<CallSiteReference> result = new LinkedList<CallSiteReference>();
    Visitor v = new Visitor() {
      public void visitInvoke(SSAInvokeInstruction instruction) {
        result.add(instruction.getCallSite());
      }
    };
    for (int i = 0; i < statements.length; i++) {
      SSAInstruction s = statements[i];
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @param statements
   *          list of ssa statements
   * @return List of InvokeInstruction
   */
  private static List<NewSiteReference> getNewSites(SSAInstruction[] statements) {
    final List<NewSiteReference> result = new LinkedList<NewSiteReference>();
    Visitor v = new Visitor() {
      public void visitNew(SSANewInstruction instruction) {
        result.add(instruction.getNewSite());
      }
    };
    for (int i = 0; i < statements.length; i++) {
      SSAInstruction s = statements[i];
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @param statements
   *          list of ssa statements
   * @return List of FieldReference
   */
  public static List<FieldReference> getFieldsRead(SSAInstruction[] statements) {
    final List<FieldReference> result = new LinkedList<FieldReference>();
    Visitor v = new Visitor() {
      public void visitGet(SSAGetInstruction instruction) {
        result.add(instruction.getDeclaredField());
      }
    };
    for (int i = 0; i < statements.length; i++) {
      SSAInstruction s = statements[i];
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  /**
   * @param statements
   *          list of ssa statements
   * @return List of FieldReference
   */
  public static List<FieldReference> getFieldsWritten(SSAInstruction[] statements) {
    final List<FieldReference> result = new LinkedList<FieldReference>();
    Visitor v = new Visitor() {
      public void visitPut(SSAPutInstruction instruction) {
        result.add(instruction.getDeclaredField());
      }
    };
    for (int i = 0; i < statements.length; i++) {
      SSAInstruction s = statements[i];
      if (s != null) {
        s.visit(v);
      }
    }
    return result;
  }

  public static boolean hasObjectArrayLoad(SSAInstruction[] statements) {

    class ScanVisitor extends Visitor {
      boolean foundOne = false;

      public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
        if (!instruction.typeIsPrimitive()) {
          foundOne = true;
        }
      }
    }
    ScanVisitor v = new ScanVisitor();
    for (int i = 0; i < statements.length; i++) {
      SSAInstruction s = statements[i];
      if (s != null) {
        s.visit(v);
      }
      if (v.foundOne) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasObjectArrayStore(SSAInstruction[] statements) {

    class ScanVisitor extends Visitor {
      boolean foundOne = false;

      public void visitArrayStore(SSAArrayStoreInstruction instruction) {
        if (!instruction.typeIsPrimitive()) {
          foundOne = true;
        }
      }
    }
    ScanVisitor v = new ScanVisitor();
    for (int i = 0; i < statements.length; i++) {
      SSAInstruction s = statements[i];
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