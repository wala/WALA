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

import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Decoder;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IArrayLoadInstruction;
import com.ibm.wala.shrikeBT.IArrayStoreInstruction;
import com.ibm.wala.shrikeBT.IGetInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.IPutInstruction;
import com.ibm.wala.shrikeBT.ITypeTestInstruction;
import com.ibm.wala.shrikeBT.MonitorInstruction;
import com.ibm.wala.shrikeBT.NewInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.bytecode.BytecodeStream;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.shrike.ShrikeUtil;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;

/**
 * A wrapper around a Shrike object that represents a method
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
/** BEGIN Custom change: precise positions */
    
    /**
     * Cached map representing position information for bytecode instruction
     * at given index
     */
    protected SourcePosition[] positionMap;
    
    /**
     * Sourcecode positions for method parameters
     */
    protected SourcePosition[] paramPositionMap;
    
/** END Custom change: precise positions */
    
    /**
     * Cached map representing line number information in ShrikeCT format TODO: do more careful caching than just soft references
     */
    protected int[] lineNumberMap;

    /**
     * an array mapping bytecode offsets to arrays representing the local variable maps for each offset; a local variable map is
     * represented as an array of localVars*2 elements, containing a pair (nameIndex, typeIndex) for each local variable; a pair
     * (0,0) indicates there is no information for that local variable at that offset
     */
    protected int[][] localVariableMap;

    /**
     * Exception types this method might throw. Computed on demand.
     */
    private TypeReference[] exceptionTypes;
  }

  /**
   * Cache the information about the method statements.
   */
  private SoftReference<BytecodeInfo> bcInfo;

  public ShrikeBTMethod(IClass klass) {
    this.declaringClass = klass;
  }

  protected synchronized BytecodeInfo getBCInfo() throws InvalidClassFileException {
    BytecodeInfo result = null;
    if (bcInfo != null) {
      result = bcInfo.get();
    }
    if (result == null) {
      result = computeBCInfo();
      bcInfo = new SoftReference<>(result);
    }
    return result;
  }

  /**
   * Return the program counter (bytecode index) for a particular Shrike instruction index.
   * 
   * @throws InvalidClassFileException
   */
  public int getBytecodeIndex(int instructionIndex) throws InvalidClassFileException {
    return getBCInfo().pcMap[instructionIndex];
  }

  /**
   * Return the Shrike instruction index for a particular valid program counter (bytecode index), or -1 if 
   * the Shrike instriction index could not be determined. 
   * 
   * This ShrikeBTMethod must not be native.
   * 
   * @throws InvalidClassFileException {@link UnsupportedOperationException}
   */
  public int getInstructionIndex(int bcIndex) throws InvalidClassFileException {
    if (isNative()) {
      throw new UnsupportedOperationException("getInstructionIndex(int bcIndex) is only supported for non-native bytecode");
    }

    final BytecodeInfo info = getBCInfo();
    if (info.decoder.containsSubroutines()) return -1;

    final int[] pcMap = info.pcMap;
    assert isSorted(pcMap);

    int iindex = Arrays.binarySearch(pcMap, bcIndex);
    if (iindex < 0) return -1;

    // Unfortunately, pcMap is not always *strictly* sorted: given bcIndex, there may be multiple adjacent indices
    // i,j such that pcMap[i] == bcIndex == pcMap[j]. We pick the least such index.
    while (iindex > 0 && pcMap[iindex - 1] == bcIndex) iindex--;
    return iindex;
  }

  private static boolean isSorted(int[] a) {
    for (int i = 0; i < a.length - 1; i++) {
      if (a[i+1] < a[i]) return false;
    }
    return true;
  }

  /**
   * Return the number of Shrike instructions for this method.
   * 
   * @throws InvalidClassFileException
   */
  public int getNumShrikeInstructions() throws InvalidClassFileException {
    return getBCInfo().pcMap.length;
  }

  /**
   * @throws InvalidClassFileException
   */
  public Collection<CallSiteReference> getCallSites() throws InvalidClassFileException {
    Collection<CallSiteReference> empty = Collections.emptySet();
    if (isNative()) {
      return empty;
    }
    return (getBCInfo().callSites == null) ? empty : Collections.unmodifiableCollection(Arrays.asList(getBCInfo().callSites));
  }

  /**
   * @throws InvalidClassFileException
   */
  Collection<NewSiteReference> getNewSites() throws InvalidClassFileException {
    Collection<NewSiteReference> empty = Collections.emptySet();
    if (isNative()) {
      return empty;
    }

    return (getBCInfo().newSites == null) ? empty : Collections.unmodifiableCollection(Arrays.asList(getBCInfo().newSites));
  }

  /**
   * @return {@link Set}&lt;{@link TypeReference}&gt;, the exceptions that statements in this method may throw,
   * @throws InvalidClassFileException
   */
  public Collection<TypeReference> getImplicitExceptionTypes() throws InvalidClassFileException {
    if (isNative()) {
      return Collections.emptySet();
    }
    return (getBCInfo().implicitExceptions == null) ? Arrays.asList(new TypeReference[0]) : Arrays
        .asList(getBCInfo().implicitExceptions);
  }

  /**
   * Do a cheap pass over the bytecodes to collect some mapping information. Some methods require this as a pre-req to accessing
   * ShrikeCT information.
   * 
   * @throws InvalidClassFileException
   */
  private BytecodeInfo computeBCInfo() throws InvalidClassFileException {
    BytecodeInfo result = new BytecodeInfo();
    result.exceptionTypes = computeDeclaredExceptions();

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
  public Iterator<TypeReference> getArraysRead() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    return (getBCInfo().arraysRead == null) ? EmptyIterator.instance() : Arrays.asList(getBCInfo().arraysRead).iterator();
  }

  /**
   * @return Iterator of TypeReference
   * @throws InvalidClassFileException
   */
  public Iterator<TypeReference> getArraysWritten() throws InvalidClassFileException {
    if (isNative()) {
      return EmptyIterator.instance();
    }
    if (getBCInfo().fieldsRead == null) {
      return EmptyIterator.instance();
    } else {
      List<TypeReference> list = Arrays.asList(getBCInfo().arraysWritten);
      return list.iterator();
    }
  }

  /**
   * @return Iterator of TypeReference
   * @throws InvalidClassFileException
   */
  public Iterator<TypeReference> getCastTypes() throws InvalidClassFileException {
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
      Descriptor D = Descriptor.findOrCreate(declaringClass.getClassLoader().getLanguage(), desc);
      return MethodReference.findOrCreate(declaringClass.getReference(), name, D);
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  @Override
  public MethodReference getReference() {
    if (methodReference == null) {
      methodReference = computeMethodReference();
    }
    return methodReference;
  }

  @Override
  public boolean isClinit() {
    return getReference().getSelector().equals(MethodReference.clinitSelector);
  }

  @Override
  public boolean isInit() {
    return getReference().getName().equals(MethodReference.initAtom);
  }

  protected abstract int getModifiers();

  @Override
  public boolean isNative() {
    return ((getModifiers() & Constants.ACC_NATIVE) != 0);
  }

  @Override
  public boolean isAbstract() {
    return ((getModifiers() & Constants.ACC_ABSTRACT) != 0);
  }

  @Override
  public boolean isPrivate() {
    return ((getModifiers() & Constants.ACC_PRIVATE) != 0);
  }

  @Override
  public boolean isProtected() {
    return ((getModifiers() & Constants.ACC_PROTECTED) != 0);
  }

  @Override
  public boolean isPublic() {
    return ((getModifiers() & Constants.ACC_PUBLIC) != 0);
  }

  @Override
  public boolean isFinal() {
    return ((getModifiers() & Constants.ACC_FINAL) != 0);
  }

  @Override
  public boolean isBridge() {
    return ((getModifiers() & Constants.ACC_VOLATILE) != 0);
  }

  @Override
  public boolean isSynchronized() {
    return ((getModifiers() & Constants.ACC_SYNCHRONIZED) != 0);
  }

  @Override
  public boolean isStatic() {
    return ((getModifiers() & Constants.ACC_STATIC) != 0);
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
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
    if (!isAbstract() && info.decoder == null) {
      throw new InvalidClassFileException(-1, "non-abstract method " + getReference() + " has no bytecodes");
    }
    if (info.decoder == null) {
      return;
    }
    info.pcMap = info.decoder.getInstructionsToBytecodes();

    processDebugInfo(info);

    SimpleVisitor simpleVisitor = new SimpleVisitor(info);

    BytecodeLanguage lang = (BytecodeLanguage) getDeclaringClass().getClassLoader().getLanguage();
    IInstruction[] instructions = info.decoder.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      simpleVisitor.setInstructionIndex(i);
      instructions[i].visit(simpleVisitor);
      if (instructions[i].isPEI()) {
        Collection<TypeReference> t = lang.getImplicitExceptionTypes(instructions[i]);
        if (t != null) {
          simpleVisitor.implicitExceptions.addAll(t);
        }
      }
    }

    // copy the Set results into arrays; will use less
    // storage
    copyVisitorSetsToArrays(simpleVisitor, info);
  }

  private static void copyVisitorSetsToArrays(SimpleVisitor simpleVisitor, BytecodeInfo info) {
    info.newSites = new NewSiteReference[simpleVisitor.newSites.size()];
    int i = 0;
    for (NewSiteReference newSiteReference : simpleVisitor.newSites) {
      info.newSites[i++] = newSiteReference;
    }

    info.fieldsRead = new FieldReference[simpleVisitor.fieldsRead.size()];
    i = 0;
    for (FieldReference fieldReference : simpleVisitor.fieldsRead) {
      info.fieldsRead[i++] = fieldReference;
    }

    info.fieldsRead = new FieldReference[simpleVisitor.fieldsRead.size()];
    i = 0;
    for (FieldReference fieldReference : simpleVisitor.fieldsRead) {
      info.fieldsRead[i++] = fieldReference;
    }

    info.fieldsWritten = new FieldReference[simpleVisitor.fieldsWritten.size()];
    i = 0;
    for (FieldReference fieldReference : simpleVisitor.fieldsWritten) {
      info.fieldsWritten[i++] = fieldReference;
    }

    info.callSites = new CallSiteReference[simpleVisitor.callSites.size()];
    i = 0;
    for (CallSiteReference callSiteReference : simpleVisitor.callSites) {
      info.callSites[i++] = callSiteReference;
    }

    info.arraysRead = new TypeReference[simpleVisitor.arraysRead.size()];
    i = 0;
    for (TypeReference typeReference : simpleVisitor.arraysRead) {
      info.arraysRead[i++] = typeReference;
    }

    info.arraysWritten = new TypeReference[simpleVisitor.arraysWritten.size()];
    i = 0;
    for (TypeReference typeReference : simpleVisitor.arraysWritten) {
      info.arraysWritten[i++] = typeReference;
    }

    info.implicitExceptions = new TypeReference[simpleVisitor.implicitExceptions.size()];
    i = 0;
    for (TypeReference typeReference : simpleVisitor.implicitExceptions) {
      info.implicitExceptions[i++] = typeReference;
    }

    info.castTypes = new TypeReference[simpleVisitor.castTypes.size()];
    i = 0;
    for (TypeReference typeReference : simpleVisitor.castTypes) {
      info.castTypes[i++] = typeReference;
    }

    info.hasMonitorOp = simpleVisitor.hasMonitorOp;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getReference().toString();
  }

  /**
   * @see java.lang.Object#equals(Object)
   */
  @Override
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
  @Override
  public int hashCode() {
    return 9661 * getReference().hashCode();
  }

  /*
   * @see com.ibm.wala.classLoader.IMethod#getMaxLocals()
   */
  public abstract int getMaxLocals();

  // TODO: ShrikeBT should have a getMaxStack method on Decoder, I think.
  public abstract int getMaxStackHeight();

  @Override
  public Atom getName() {
    return getReference().getName();
  }

  @Override
  public Descriptor getDescriptor() {
    return getReference().getDescriptor();
  }

  /**
   * 
   * A visitor used to process bytecodes
   * 
   */
  private class SimpleVisitor extends IInstruction.Visitor {

    private final BytecodeInfo info;

    public SimpleVisitor(BytecodeInfo info) {
      this.info = info;
    }

    // TODO: make a better Set implementation for these.
    final Set<CallSiteReference> callSites = HashSetFactory.make(5);

    final Set<FieldReference> fieldsWritten = HashSetFactory.make(5);

    final Set<FieldReference> fieldsRead = HashSetFactory.make(5);

    final Set<NewSiteReference> newSites = HashSetFactory.make(5);

    final Set<TypeReference> arraysRead = HashSetFactory.make(5);

    final Set<TypeReference> arraysWritten = HashSetFactory.make(5);

    final Set<TypeReference> implicitExceptions = HashSetFactory.make(5);

    final Set<TypeReference> castTypes = HashSetFactory.make(5);

    boolean hasMonitorOp;

    private int instructionIndex;

    public void setInstructionIndex(int i) {
      instructionIndex = i;
    }

    public int getProgramCounter() {
      return info.pcMap[instructionIndex];
    }

    @Override
    public void visitMonitor(MonitorInstruction instruction) {
      hasMonitorOp = true;
    }

    @Override
    public void visitNew(NewInstruction instruction) {
      ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
      TypeReference t = ShrikeUtil.makeTypeReference(loader, instruction.getType());
      newSites.add(NewSiteReference.make(getProgramCounter(), t));
    }

    @Override
    public void visitGet(IGetInstruction instruction) {
      ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
      FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(), instruction
          .getFieldType());
      fieldsRead.add(f);
    }

    @Override
    public void visitPut(IPutInstruction instruction) {
      ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
      FieldReference f = FieldReference.findOrCreate(loader, instruction.getClassType(), instruction.getFieldName(), instruction
          .getFieldType());
      fieldsWritten.add(f);
    }

    @Override
    public void visitInvoke(IInvokeInstruction instruction) {
      IClassLoader loader = getDeclaringClass().getClassLoader();
      MethodReference m = MethodReference.findOrCreate(loader.getLanguage(), loader.getReference(), instruction.getClassType(),
          instruction.getMethodName(), instruction.getMethodSignature());
      int programCounter = 0;
      programCounter = getProgramCounter();
      CallSiteReference site = null;
      site = CallSiteReference.make(programCounter, m, instruction.getInvocationCode());
      callSites.add(site);
    }

    /*
     * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitArrayLoad(com.ibm.wala.shrikeBT.ArrayLoadInstruction)
     */
    @Override
    public void visitArrayLoad(IArrayLoadInstruction instruction) {
      arraysRead.add(ShrikeUtil.makeTypeReference(getDeclaringClass().getClassLoader().getReference(), instruction.getType()));
    }

    /*
     * @see com.ibm.wala.shrikeBT.Instruction.Visitor#visitArrayStore(com.ibm.wala.shrikeBT.ArrayStoreInstruction)
     */
    @Override
    public void visitArrayStore(IArrayStoreInstruction instruction) {
      arraysWritten.add(ShrikeUtil.makeTypeReference(getDeclaringClass().getClassLoader().getReference(), instruction.getType()));
    }

    @Override
    public void visitCheckCast(ITypeTestInstruction instruction) {
      for(String t : instruction.getTypes()) {
        castTypes.add(ShrikeUtil.makeTypeReference(getDeclaringClass().getClassLoader().getReference(), t));
      }
    }
  }

  /**
   */
  public IInstruction[] getInstructions() throws InvalidClassFileException {
    if (getBCInfo().decoder == null) {
      return null;
    } else {
      return getBCInfo().decoder.getInstructions();
    }
  }

  public ExceptionHandler[][] getHandlers() throws InvalidClassFileException {
    if (getBCInfo().decoder == null) {
      return null;
    } else {
      return getBCInfo().decoder.getHandlers();
    }
  }

  /**
   * By convention, for a non-static method, getParameterType(0) is the this pointer
   */
  @Override
  public TypeReference getParameterType(int i) {
    if (!isStatic()) {
      if (i == 0) {
        return declaringClass.getReference();
      } else {
        return getReference().getParameterType(i - 1);
      }
    } else {
      return getReference().getParameterType(i);
    }
  }

  /**
   * Method getNumberOfParameters. This result includes the "this" pointer if applicable
   * 
   * @return int
   */
  @Override
  public int getNumberOfParameters() {
    if (isStatic() || isClinit()) {
      return getReference().getNumberOfParameters();
    } else {
      return getReference().getNumberOfParameters() + 1;
    }
  }

  /*
   * @see com.ibm.wala.classLoader.IMethod#hasExceptionHandler()
   */
  @Override
  public abstract boolean hasExceptionHandler();

  /**
   * Clients should not modify the returned array. TODO: clone to avoid the problem?
   * 
   * @throws InvalidClassFileException
   * 
   * @see com.ibm.wala.classLoader.IMethod#getDeclaredExceptions()
   */
  @Override
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
      if (strings == null)
        return null;

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
/** BEGIN Custom change: precise bytecode positions */
  
  /*
   * @see com.ibm.wala.classLoader.IMethod#getSourcePosition(int)
   */
  @Override
  public SourcePosition getSourcePosition(int bcIndex) throws InvalidClassFileException {
    return (getBCInfo().positionMap == null) ? null : getBCInfo().positionMap[bcIndex];
  }

  /*
   * @see com.ibm.wala.classLoader.IMethod#getParameterSourcePosition(int)
   */
  @Override
  public SourcePosition getParameterSourcePosition(int paramNum) throws InvalidClassFileException {
    return (getBCInfo().paramPositionMap == null) ? null : getBCInfo().paramPositionMap[paramNum];
  }
/** END Custom change: precise bytecode positions */

  /*
   * @see com.ibm.wala.classLoader.IMethod#getLineNumber(int)
   */
  @Override
  public int getLineNumber(int bcIndex) {
    try {
      return (getBCInfo().lineNumberMap == null) ? -1 : getBCInfo().lineNumberMap[bcIndex];
    } catch (InvalidClassFileException e) {
      return -1;
    }
  }

  /**
   * @return {@link Set}&lt;{@link TypeReference}&gt;
   * @throws InvalidClassFileException
   */
  public Set<TypeReference> getCaughtExceptionTypes() throws InvalidClassFileException {

    ExceptionHandler[][] handlers = getHandlers();
    if (handlers == null) {
      return Collections.emptySet();
    }
    HashSet<TypeReference> result = HashSetFactory.make(10);
    ClassLoaderReference loader = getReference().getDeclaringClass().getClassLoader();
    for (ExceptionHandler[] handler : handlers) {
      for (ExceptionHandler element : handler) {
        TypeReference t = ShrikeUtil.makeTypeReference(loader, element.getCatchClass());
        if (t == null) {
          t = TypeReference.JavaLangThrowable;
        }
        result.add(t);
      }
    }
    return result;
  }

  /*
   * @see com.ibm.wala.classLoader.IMethod#getSignature()
   */
  @Override
  public String getSignature() {
    return getReference().getSignature();
  }

  /*
   * @see com.ibm.wala.classLoader.IMethod#getSelector()
   */
  @Override
  public Selector getSelector() {
    return getReference().getSelector();
  }

  /*
   * @see com.ibm.wala.classLoader.IMethod#getLocalVariableName(int, int)
   */
  @Override
  public abstract String getLocalVariableName(int bcIndex, int localNumber);

  /*
   * TODO: cache for efficiency?
   * 
   * @see com.ibm.wala.classLoader.IMethod#hasLocalVariableTable()
   */
  @Override
  public abstract boolean hasLocalVariableTable();

  /**
   * Clear all optional cached data associated with this class.
   */
  public void clearCaches() {
    bcInfo = null;
  }
}
