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

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.shrikeBT.ArrayLoadInstruction;
import com.ibm.wala.shrikeBT.ArrayStoreInstruction;
import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrikeBT.CheckCastInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Decoder;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.GetInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeBT.PutInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.ShrikeUtil;
import com.ibm.wala.util.bytecode.BytecodeStream;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * A wrapper around a Shrike object that represents a method
 * 
 * @author sfink
 */
public abstract class ShrikeBTMethod implements IMethod, BytecodeConstants {

  /**
   * Some verbose progress output?
   */
  private final static boolean verbose = false;

  private static int methodsParsed = 0;

  /**
   * A wrapper around the declaring class.
   */
  protected final IClass declaringClass;

  /**
   * Canonical reference for this method
   */
  private MethodReference methodReference;

  // break these out to save some space; they're computed lazily.
  protected static class BytecodeInfo {
    Decoder decoder;

    CallSiteReference[] callSites;

    FieldReference[] fieldsWritten;

    FieldReference[] fieldsRead;

    NewSiteReference[] newSites;

    TypeReference[] arraysRead;

    TypeReference[] arraysWritten;

    TypeReference[] implicitExceptions;

    TypeReference[] castTypes;

    boolean hasMonitorOp;

    /**
     * Mapping from instruction index to program counter.
     */
    private int[] pcMap;

    /**
     * Cached map representing line number information in ShrikeCT format TODO:
     * do more careful caching than just soft references
     */
    protected int[] lineNumberMap;

    /**
     * an array mapping bytecode offsets to arrays representing the local
     * variable maps for each offset; a local variable map is represented as an
     * array of localVars*2 elements, containing a pair (nameIndex, typeIndex)
     * for each local variable; a pair (0,0) indicates there is no information
     * for that local variable at that offset
     */
    protected int[][] localVariableMap;

    /**
     * Exception types this method might throw. Computed on demand.
     */
    private TypeReference[] exceptionTypes;
    
    /**
     * the "Signature" attribute; holds information on generics
     */
    protected String genericsSignature;
  }

  /**
   * Cache the information about the method statements.
   */
  private SoftReference<BytecodeInfo> bcInfo;

  public ShrikeBTMethod(IClass klass) {
    this.declaringClass = klass;
  }
  
  protected BytecodeInfo getBCInfo() throws InvalidClassFileException {
    BytecodeInfo result = null;
    if (bcInfo != null) {
      result = bcInfo.get();
    }
    if (result == null) {
      result = computeBCInfo();
      bcInfo = new SoftReference<BytecodeInfo>(result);
    }
    return result;
  }

  /**
   * Return the program counter (bytecode index) for a particular Shrike
   * instruction index.
   * 
   * @throws InvalidClassFileException
   */
  public int getBytecodeIndex(int instructionIndex) throws InvalidClassFileException {
    return getBCInfo().pcMap[instructionIndex];
  }

  /**
   * @return an Iterator of CallSiteReferences from this method.
   * @throws InvalidClassFileException
   */
  Iterator<CallSiteReference> getCallSites() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    Iterator<CallSiteReference> empty = EmptyIterator.instance();
    return (getBCInfo().callSites == null) ? empty : Arrays.asList(getBCInfo().callSites).iterator();
  }

  /**
   * @return an Iterator of NewlSiteReferences from this method.
   * @throws InvalidClassFileException
   */
  Iterator<NewSiteReference> getNewSites() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    Iterator<NewSiteReference> empty = EmptyIterator.instance();
    return (getBCInfo().newSites == null) ? empty : Arrays.asList(getBCInfo().newSites).iterator();
  }

  /**
   * @return Set <TypeReference>, the exceptions that statements in this method
   *         may throw,
   * @throws InvalidClassFileException
   */
  public Collection getImplicitExceptionTypes() throws InvalidClassFileException {
    if (isNative()) {
      return Collections.EMPTY_SET;
    }
    return (getBCInfo().implicitExceptions == null) ? Arrays.asList(new TypeReference[0]) : Arrays.asList(getBCInfo().implicitExceptions);
  }

  /**
   * Do a cheap pass over the bytecodes to collect some
   * mapping information. Some methods require this as a pre-req to accessing
   * ShrikeCT information.
   * 
   * @throws InvalidClassFileException
   */
  private BytecodeInfo computeBCInfo() throws InvalidClassFileException {
    BytecodeInfo result = new BytecodeInfo();
    result.exceptionTypes = computeDeclaredExceptions();
    result.genericsSignature = computeGenericsSignature();

    if (isNative()) {
      return result;
    }
    if (verbose) {
      methodsParsed += 1;
      if (methodsParsed % 100 == 0) {
        System.out.println(methodsParsed + " methods processed...");
      }
    }

    processBytecodesWithShrikeBT(result);
    return result;
  }

  /**
   * @return true iff this method has a monitorenter or monitorexit
   * @throws InvalidClassFileException
   */
  public boolean hasMonitorOp() throws InvalidClassFileException {
    if (isNative()) {
      return false;
    }
    return getBCInfo().hasMonitorOp;
  }

  /**
   * @return Set of FieldReference
   * @throws InvalidClassFileException
   */
  public Iterator<FieldReference> getFieldsWritten() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    if (getBCInfo().fieldsWritten == null) {
      return EmptyIterator.instance();
    } else {
      List<FieldReference> l = Arrays.asList(getBCInfo().fieldsWritten);
      return l.iterator();
    }
  }

  /**
   * @return Iterator of FieldReference
   * @throws InvalidClassFileException
   */
  public Iterator<FieldReference> getFieldsRead() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    if (getBCInfo().fieldsRead == null) {
      return EmptyIterator.instance();
    } else {
      List<FieldReference> l = Arrays.asList(getBCInfo().fieldsRead);
      return l.iterator();
    }
  }

  /**
   * @return Iterator of TypeReference
   * @throws InvalidClassFileException
   */
  public Iterator getArraysRead() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    return (getBCInfo().arraysRead == null) ? EmptyIterator.instance() : Arrays.asList(getBCInfo().arraysRead).iterator();
  }

  /**
   * @return Iterator of TypeReference
   * @throws InvalidClassFileException
   */
  public Iterator getArraysWritten() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    return (getBCInfo().arraysWritten == null) ? EmptyIterator.instance() : Arrays.asList(getBCInfo().arraysWritten).iterator();
  }

  /**
   * @return Iterator of TypeReference
   * @throws InvalidClassFileException
   */
  public Iterator getCastTypes() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    return (getBCInfo().castTypes == null) ? EmptyIterator.instance() : Arrays.asList(getBCInfo().castTypes).iterator();
  }

  protected abstract byte[] getBytecodes();

  /**
   * Method getBytecodeStream.
   * 
   * @return the bytecode stream for this method, or null if no bytecodes.
   */
  public BytecodeStream getBytecodeStream() {
    byte[] bytecodes = getBytecodes();
    if (bytecodes == null) {
      return null;
    } else {
      return new BytecodeStream(this, bytecodes);
    }
  }

  protected abstract String getMethodName() throws InvalidClassFileException;

  protected abstract String getMethodSignature() throws InvalidClassFileException;

  private MethodReference computeMethodReference() {
    try {
      Atom name = Atom.findOrCreateUnicodeAtom(getMethodName());
      ImmutableByteArray desc = ImmutableByteArray.make(getMethodSignature());
      Descriptor D = Descriptor.findOrCreate(desc);
      return 
	MethodReference.findOrCreate(declaringClass.getReference(), name, D);
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public MethodReference getReference() {
    if (methodReference == null) {
      methodReference = computeMethodReference();
    }
    return methodReference;
  }

  public boolean isClinit() {
    return getReference().getSelector().equals(MethodReference.clinitSelector);
  }

  public boolean isInit() {
    return getReference().getName().equals(MethodReference.initAtom);
  }

  protected abstract int getModifiers();

  public boolean isNative() {
    return ((getModifiers() & Constants.ACC_NATIVE) != 0);
  }

  public boolean isAbstract() {
    return ((getModifiers() & Constants.ACC_ABSTRACT) != 0);
  }

  public boolean isPrivate() {
    return ((getModifiers() & Constants.ACC_PRIVATE) != 0);
  }

  public boolean isProtected() {
    return ((getModifiers() & Constants.ACC_PROTECTED) != 0);
  }

  public boolean isPublic() {
    return ((getModifiers() & Constants.ACC_PUBLIC) != 0);
  }

  public boolean isFinal() {
    return ((getModifiers() & Constants.ACC_FINAL) != 0);
  }
  
  public boolean isVolatile() {
    return ((getModifiers() & Constants.ACC_VOLATILE) != 0);
  }

  public boolean isSynchronized() {
    return ((getModifiers() & Constants.ACC_SYNCHRONIZED) != 0);
  }

  public boolean isStatic() {
    return ((getModifiers() & Constants.ACC_STATIC) != 0);
  }

  public boolean isSynthetic() {
    return false;
  }

  public IClass getDeclaringClass() {
    return declaringClass;
  }

  /**
   * Find the decoder object for this method, or create one if necessary.
   * 
   * @return null if the method has no code.
   */
  protected abstract Decoder makeDecoder();

  /**
   * Walk through the bytecodes and collect trivial information.
   * 
   * @throws InvalidClassFileException
   */
  protected abstract void processDebugInfo(BytecodeInfo bcInfo) throws InvalidClassFileException;


  private void processBytecodesWithShrikeBT(BytecodeInfo info) throws InvalidClassFileException {
    info.decoder = makeDecoder();
    if (Assertions.verifyAssertions) {
      if (!isAbstract() && info.decoder == null) {
        Assertions.UNREACHABLE("bad method " + getReference());
      }
    }
    if (info.decoder == null) {
      return;
    }
    info.pcMap = info.decoder.getInstructionsToBytecodes();

    processDebugInfo(info);

    SimpleVisitor simpleVisitor = new SimpleVisitor(info);

    Instruction[] instructions = info.decoder.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      simpleVisitor.setInstructionIndex(i);
      instructions[i].visit(simpleVisitor);
      if (Exceptions.isPEI(instructions[i])) {
        Collection<TypeReference> t = Exceptions.getIndependentExceptionTypes(instructions[i]);
        simpleVisitor.implicitExceptions.addAll(t);
      }
    }

    // copy the Set results into arrays; will use less
    // storage
    copyVisitorSetsToArrays(simpleVisitor, info);
  }

  private void copyVisitorSetsToArrays(SimpleVisitor simpleVisitor, BytecodeInfo info) {
    info.newSites = new NewSiteReference[simpleVisitor.newSites.size()];
    int i = 0;
    for (Iterator<NewSiteReference> it = simpleVisitor.newSites.iterator(); it.hasNext();) {
      info.newSites[i++] = it.next();
    }

    info.fieldsRead = new FieldReference[simpleVisitor.fieldsRead.size()];
    i = 0;
    for (Iterator<FieldReference> it = simpleVisitor.fieldsRead.iterator(); it.hasNext();) {
      info.fieldsRead[i++] = it.next();
    }

    info.fieldsRead = new FieldReference[simpleVisitor.fieldsRead.size()];
    i = 0;
    for (Iterator<FieldReference> it = simpleVisitor.fieldsRead.iterator(); it.hasNext();) {
      info.fieldsRead[i++] = it.next();
    }

    info.fieldsWritten = new FieldReference[simpleVisitor.fieldsWritten.size()];
    i = 0;
    for (Iterator<FieldReference> it = simpleVisitor.fieldsWritten.iterator(); it.hasNext();) {
      info.fieldsWritten[i++] = it.next();
    }

    info.callSites = new CallSiteReference[simpleVisitor.callSites.size()];
    i = 0;
    for (Iterator<CallSiteReference> it = simpleVisitor.callSites.iterator(); it.hasNext();) {
      info.callSites[i++] = it.next();
    }

    info.arraysRead = new TypeReference[simpleVisitor.arraysRead.size()];
    i = 0;
    for (Iterator<TypeReference> it = simpleVisitor.arraysRead.iterator(); it.hasNext();) {
      info.arraysRead[i++] = it.next();
    }

    info.arraysWritten = new TypeReference[simpleVisitor.arraysWritten.size()];
    i = 0;
    for (Iterator<TypeReference> it = simpleVisitor.arraysWritten.iterator(); it.hasNext();) {
      info.arraysWritten[i++] = it.next();
    }

    info.implicitExceptions = new TypeReference[simpleVisitor.implicitExceptions.size()];
    i = 0;
    for (Iterator it = simpleVisitor.implicitExceptions.iterator(); it.hasNext();) {
      info.implicitExceptions[i++] = (TypeReference) it.next();
    }

    info.castTypes = new TypeReference[simpleVisitor.castTypes.size()];
    i = 0;
    for (Iterator<TypeReference> it = simpleVisitor.castTypes.iterator(); it.hasNext();) {
      info.castTypes[i++] = it.next();
    }

    info.hasMonitorOp = simpleVisitor.hasMonitorOp;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return getReference().toString();
  }

  /**
   * @see java.lang.Object#equals(Object)
   */
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final.
    // if (this.getClass().equals(obj.getClass())) {
    if (obj instanceof ShrikeBTMethod) {
      ShrikeBTMethod that = (ShrikeBTMethod) obj;
      return (getDeclaringClass().equals(that.getDeclaringClass()) && getReference().equals(that.getReference()));
    } else {
      return false;
    }
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return 9661 * getReference().hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getMaxLocals()
   */
  public abstract int getMaxLocals();

  // TODO: ShrikeBT should have a getMaxStack method on Decoder, I think.
  public abstract int getMaxStackHeight();

  public Atom getName() {
    return getReference().getName();
  }

  public Descriptor getDescriptor() {
    return getReference().getDescriptor();
  }

  /**
   * 
   * A visitor used to process bytecodes
   *
   */
  private class SimpleVisitor extends Instruction.Visitor {
    
    private final BytecodeInfo info;
    
    public SimpleVisitor(BytecodeInfo info) {
      this.info = info; 
    }

    // TODO: make a better Set implementation for these.
    Set<CallSiteReference> callSites = HashSetFactory.make(5);

    Set<FieldReference> fieldsWritten = HashSetFactory.make(5);

    Set<FieldReference> fieldsRead = HashSetFactory.make(5);

    Set<NewSiteReference> newSites = HashSetFactory.make(5);

    Set<TypeReference> arraysRead = HashSetFactory.make(5);

    Set<TypeReference> arraysWritten = HashSetFactory.make(5);

    Set<TypeReference> implicitExceptions = HashSetFactory.make(5);

    Set<TypeReference> castTypes = HashSetFactory.make(5);

    boolean hasMonitorOp;

    private int instructionIndex;

    public void setInstructionIndex(int i) {
      instructionIndex = i;
    }

    public int getProgramCounter() throws InvalidClassFileException {
      return info.pcMap[instructionIndex];
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitMonitor(com.ibm.wala.shrikeBT.MonitorInstruction)
     */
    public void visitMonitor(MonitorInstruction instruction) {
      hasMonitorOp = true;
    }

    public void visitNew(NewInstruction instruction) {
      ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
      TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
      try {
        newSites.add(NewSiteReference.make(getProgramCounter(), t));
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }

    public void visitGet(GetInstruction instruction) {
      ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
      FieldReference f = ShrikeUtil.makeFieldReference(loader, instruction.getClassType(), instruction.getFieldName(), instruction
          .getFieldType());
      fieldsRead.add(f);
    }

    public void visitPut(PutInstruction instruction) {
      ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
      FieldReference f = ShrikeUtil.makeFieldReference(loader, instruction.getClassType(), instruction.getFieldName(), instruction
          .getFieldType());
      fieldsWritten.add(f);
    }

    public void visitInvoke(InvokeInstruction instruction) {
      ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
      MethodReference m = ShrikeUtil.makeMethodReference(loader, instruction.getClassType(), instruction.getMethodName(),
          instruction.getMethodSignature());
      int programCounter = 0;
      try {
        programCounter = getProgramCounter();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      CallSiteReference site = null;
      int nParams = m.getNumberOfParameters();
      switch (instruction.getInvocationMode()) {
      case Constants.OP_invokestatic:
        site = CallSiteReference.make(programCounter, m, IInvokeInstruction.Dispatch.STATIC);
        break;
      case Constants.OP_invokeinterface:
        site = CallSiteReference.make(programCounter, m, IInvokeInstruction.Dispatch.INTERFACE);
        nParams++;
        break;
      case Constants.OP_invokespecial:
        site = CallSiteReference.make(programCounter, m, IInvokeInstruction.Dispatch.SPECIAL);
        nParams++;
        break;
      case Constants.OP_invokevirtual:
        site = CallSiteReference.make(programCounter, m, IInvokeInstruction.Dispatch.VIRTUAL);
        nParams++;
        break;
      default:
        Assertions.UNREACHABLE();
      }
      callSites.add(site);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitArrayLoad(com.ibm.wala.shrikeBT.ArrayLoadInstruction)
     */
    public void visitArrayLoad(ArrayLoadInstruction instruction) {
      arraysRead.add(ShrikeUtil.makeTypeReference(getDeclaringClass().getClassLoader().getReference(), instruction.getType()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitArrayStore(com.ibm.wala.shrikeBT.ArrayStoreInstruction)
     */
    public void visitArrayStore(ArrayStoreInstruction instruction) {
      arraysWritten.add(ShrikeUtil.makeTypeReference(getDeclaringClass().getClassLoader().getReference(), instruction.getType()));
    }

    public void visitCheckCast(CheckCastInstruction instruction) {
      castTypes.add(ShrikeUtil.makeTypeReference(getDeclaringClass().getClassLoader().getReference(), instruction.getType()));
    }

  }

  /**
   * Method getInstructions.
   * 
   * @return Instruction[]
   * @throws InvalidClassFileException
   */
  public Instruction[] getInstructions() throws InvalidClassFileException {
    if (getBCInfo().decoder == null) {
      return null;
    } else {
      return getBCInfo().decoder.getInstructions();
    }
  }

  /**
   * Method getHandlers.
   * 
   * @return ExceptionHandler[][]
   * @throws InvalidClassFileException
   */
  public ExceptionHandler[][] getHandlers() throws InvalidClassFileException {
    if (getBCInfo().decoder == null) {
      return null;
    } else {
      return getBCInfo().decoder.getHandlers();
    }
  }

  /**
   * Method getParameterType. By convention, for a non-static method,
   * getParameterType(0) is the this pointer
   * 
   * @param i
   * @return TypeReference
   */
  public TypeReference getParameterType(int i) {
    if (!isStatic()) {
      if (i == 0)
        return declaringClass.getReference();
      else
        return getReference().getParameterType(i - 1);
    } else {
      return getReference().getParameterType(i);
    }
  }

  /**
   * Method getNumberOfParameters. This result includes the "this" pointer if
   * applicable
   * 
   * @return int
   */
  public int getNumberOfParameters() {
    if (isStatic() || isClinit()) {
      return getReference().getNumberOfParameters();
    } else {
      return getReference().getNumberOfParameters() + 1;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#hasExceptionHandler()
   */
  public abstract boolean hasExceptionHandler();

  /**
   * Clients should not modify the returned array. TODO: clone to avoid the
   * problem?
   * @throws InvalidClassFileException 
   * 
   * @see com.ibm.wala.classLoader.IMethod#getDeclaredExceptions()
   */
  public TypeReference[] getDeclaredExceptions() throws InvalidClassFileException {
    return (getBCInfo().exceptionTypes == null) ? new TypeReference[0] : getBCInfo().exceptionTypes;
  }

  protected abstract String[] getDeclaredExceptionTypeNames() throws InvalidClassFileException;

  /**
   * 
   * @see com.ibm.wala.classLoader.IMethod#getDeclaredExceptions()
   */
  private TypeReference[] computeDeclaredExceptions() {
    try {
      String[] strings = getDeclaredExceptionTypeNames();
      if (strings == null) return null;

      ClassLoaderReference loader = getDeclaringClass().getClassLoader().getReference();

      TypeReference[] result = new TypeReference[strings.length];
      for (int i = 0; i < result.length; i++) {
        result[i] = TypeReference.findOrCreate(loader, TypeName.findOrCreate(ImmutableByteArray.make("L" + strings[i])));
      }
      return result;
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
      return null;
    }
  }
  
  protected abstract String computeGenericsSignature() throws InvalidClassFileException; 

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getLineNumber(int)
   */
  public int getLineNumber(int bcIndex) throws InvalidClassFileException {
    return (getBCInfo().lineNumberMap == null) ? -1 : getBCInfo().lineNumberMap[bcIndex];
  }

  /**
   * @return Set <TypeReference>
   * @throws InvalidClassFileException
   */
  public Set<TypeReference> getCaughtExceptionTypes() throws InvalidClassFileException {

    ExceptionHandler[][] handlers = getHandlers();
    if (handlers == null) {
      return Collections.emptySet();
    }
    HashSet<TypeReference> result = HashSetFactory.make(10);
    ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
    for (int i = 0; i < handlers.length; i++) {
      for (int j = 0; j < handlers[i].length; j++) {
        TypeReference t = ShrikeUtil.makeTypeReference(loader, handlers[i][j].getCatchClass());
        if (t == null) {
          t = TypeReference.JavaLangThrowable;
        }
        result.add(t);
      }
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getSignature()
   */
  public String getSignature() {
    return getReference().getSignature();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getSelector()
   */
  public Selector getSelector() {
    return getReference().getSelector();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getLocalVariableName(int, int)
   */

  public abstract String getLocalVariableName(int bcIndex, int localNumber) throws InvalidClassFileException;

  /*
   * TODO: cache for efficiency? (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#hasLocalVariableTable()
   */
  public abstract boolean hasLocalVariableTable();

  /**
   * Clear all optional cached data associated with this class.
   */
  public void clearCaches() {
    bcInfo = null;
  }
}
