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

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.Decoder;
import com.ibm.wala.shrikeBT.IndirectionData;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeCT.AnnotationsReader;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.ExceptionsReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.LineNumberTableReader;
import com.ibm.wala.shrikeCT.LocalVariableTableReader;
import com.ibm.wala.shrikeCT.RuntimeInvisibleAnnotationsReader;
import com.ibm.wala.shrikeCT.RuntimeVisibleAnnotationsReader;
import com.ibm.wala.shrikeCT.SignatureReader;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.generics.MethodTypeSignature;
import com.ibm.wala.util.debug.Assertions;

/**
 * A wrapper around a Shrike object that represents a method
 */
public final class ShrikeCTMethod extends ShrikeBTMethod implements IBytecodeMethod {

  /**
   * The index of this method in the declaring class's method list according to Shrike CT.
   */
  final private int shrikeMethodIndex;

  /**
   * JVM-level modifiers for this method a value of -1 means "uninitialized"
   */
  private int modifiers = -1;

  private final IClassHierarchy cha;

  public ShrikeCTMethod(IClass klass, int index) {

    super(klass);
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    this.shrikeMethodIndex = index;
    this.cha = klass.getClassHierarchy();
  }

  @Override
  public byte[] getBytecodes() {
    CodeReader code = getCodeReader();
    if (code == null) {
      return null;
    } else {
      return code.getBytecode();
    }
  }

  @Override
  protected String getMethodName() throws InvalidClassFileException {
    ClassReader reader = getClassReader();
    return reader.getMethodName(shrikeMethodIndex);
  }

  @Override
  protected String getMethodSignature() throws InvalidClassFileException {
    ClassReader reader = getClassReader();
    return reader.getMethodType(shrikeMethodIndex);
  }

  @Override
  protected int getModifiers() {
    if (modifiers == -1) {
      modifiers = getClassReader().getMethodAccessFlags(shrikeMethodIndex);
    }
    return modifiers;
  }

  @Override
  protected Decoder makeDecoder() {
    CodeReader reader = getCodeReader();
    if (reader == null) {
      return null;
    }
    final Decoder d = new CTDecoder(reader);
    try {
      d.decode();
    } catch (Decoder.InvalidBytecodeException ex) {
      Assertions.UNREACHABLE();
    }
    return d;
  }

  @Override
  public int getMaxLocals() {
    CodeReader reader = getCodeReader();
    return reader.getMaxLocals();
  }

  @Override
  public int getMaxStackHeight() {
    CodeReader reader = getCodeReader();
    // note that Shrike returns the maximum index in the zero-indexed stack
    // array.
    // Instead, we want the max number of entries on the stack.
    // So we add 1.
    // Additionally, ShrikeBT may add additional stack entries with
    // Constant instructions. We add an additional 1 to account for this,
    // which seems to handle all ShrikeBT code generation patterns.
    // TODO: ShrikeBT should have a getMaxStack method on Decoder, I think.
    return reader.getMaxStack() + 2;
  }

  @Override
  public boolean hasExceptionHandler() {
    CodeReader reader = getCodeReader();
    if (reader == null)
      return false;
    int[] handlers = reader.getRawHandlers();
    return handlers != null && handlers.length > 0;
  }

  @Override
  protected String[] getDeclaredExceptionTypeNames() throws InvalidClassFileException {
    ExceptionsReader reader = getExceptionReader();
    if (reader == null) {
      return null;
    } else {
      return reader.getClasses();
    }
  }

  @Override
  protected void processDebugInfo(BytecodeInfo bcInfo) throws InvalidClassFileException {
    CodeReader cr = getCodeReader();
    bcInfo.lineNumberMap = LineNumberTableReader.makeBytecodeToSourceMap(cr);
    bcInfo.localVariableMap = LocalVariableTableReader.makeVarMap(cr);
  }

  @Override
  public String getLocalVariableName(int bcIndex, int localNumber){
    int[][] map = null;
    try {
      map = getBCInfo().localVariableMap;
    } catch (InvalidClassFileException e1) {
      return null;
    }

    if (localNumber > getMaxLocals()) {
      throw new IllegalArgumentException("illegal local number: " + localNumber + ", method " + getName() + " uses at most "
          + getMaxLocals());
    }

    if (map == null) {
      return null;
    } else {
      int[] localPairs = map[bcIndex];
      int localIndex = localNumber * 2;
      if (localPairs == null || localIndex >= localPairs.length) {
        // no information about the specified local at this program point
        return null;
      }
      int nameIndex = localPairs[localIndex];
      if (nameIndex == 0) {
        return null;
      } else {
        try {
          return getClassReader().getCP().getCPUtf8(nameIndex);
        } catch (InvalidClassFileException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE();
          return null;
        }
      }
    }
  }

  /*
   * TODO: cache for efficiency?
   * 
   * @see com.ibm.wala.classLoader.IMethod#hasLocalVariableTable()
   */
  @Override
  public boolean hasLocalVariableTable() {
    try {
      ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
      getCodeReader().initAttributeIterator(iter);
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals("LocalVariableTable")) {
          return true;
        }
      }
      return false;
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return false;
    }
  }

  private ClassReader getClassReader() {
    return ((ShrikeClass) getDeclaringClass()).getReader();
  }

  private CodeReader getCodeReader() {
    ClassReader.AttrIterator iter = new AttrIterator();
    getClassReader().initMethodAttributeIterator(shrikeMethodIndex, iter);

    // search for the code attribute
    CodeReader code = null;
    try {
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals("Code")) {
          code = new CodeReader(iter);
          break;
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return code;
  }

  private ExceptionsReader getExceptionReader() {
    ClassReader.AttrIterator iter = new AttrIterator();
    getClassReader().initMethodAttributeIterator(shrikeMethodIndex, iter);

    // search for the desired attribute
    ExceptionsReader result = null;
    try {
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals("Exceptions")) {
          result = new ExceptionsReader(iter);
          break;
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return result;
  }

  private SignatureReader getSignatureReader() {
    ClassReader.AttrIterator iter = new AttrIterator();
    getClassReader().initMethodAttributeIterator(shrikeMethodIndex, iter);

    // search for the desired attribute
    SignatureReader result = null;
    try {
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals("Signature")) {
          result = new SignatureReader(iter);
          break;
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return result;
  }

  private AnnotationsReader getAnnotationsReader(boolean runtimeInvisible) {
    ClassReader.AttrIterator iter = new AttrIterator();
    getClassReader().initMethodAttributeIterator(shrikeMethodIndex, iter);

    // search for the desired attribute
    AnnotationsReader result = null;
    try {
      for (; iter.isValid(); iter.advance()) {
        if (runtimeInvisible) {
          if (iter.getName().equals(RuntimeInvisibleAnnotationsReader.attrName)) {
            result = new RuntimeInvisibleAnnotationsReader(iter);
            break;
          }
        } else {
          if (iter.getName().equals(RuntimeVisibleAnnotationsReader.attrName)) {
            result = new RuntimeVisibleAnnotationsReader(iter);
            break;
          }
        }
      }
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE();
    }
    return result;
  }

  private String computeGenericsSignature() throws InvalidClassFileException {
    SignatureReader reader = getSignatureReader();
    if (reader == null) {
      return null;
    } else {
      return reader.getSignature();
    }
  }

  public TypeReference getReturnType() {
    return getReference().getReturnType();
  }

  public IClassHierarchy getClassHierarchy() {
    return cha;
  }

  /**
   * TODO: cache?
   * 
   * @return raw "Signature" attribute from the bytecode
   * @throws InvalidClassFileException
   */
  private String getGenericsSignature() throws InvalidClassFileException {
    return computeGenericsSignature();
  }

  /**
   * UNDER CONSTRUCTION
   * 
   * @throws InvalidClassFileException
   */
  public MethodTypeSignature getMethodTypeSignature() throws InvalidClassFileException {
    String sig = getGenericsSignature();
    return sig == null ? null : MethodTypeSignature.make(sig);
  }

  /**
   * read the runtime-invisible annotations from the class file
   */
  public Collection<Annotation> getRuntimeInvisibleAnnotations() throws InvalidClassFileException {
    return getAnnotations(true);
  }

  /**
   * read the runtime-visible annotations from the class file
   */
  public Collection<Annotation> getRuntimeVisibleAnnotations() throws InvalidClassFileException {
    return getAnnotations(false);
  }

  public Collection<Annotation> getAnnotations(boolean runtimeInvisible) throws InvalidClassFileException {
    AnnotationsReader r = getAnnotationsReader(runtimeInvisible);
    return Annotation.getAnnotationsFromReader(r, getDeclaringClass().getClassLoader().getReference());
  }


  private static final IndirectionData NO_INDIRECTIONS = new IndirectionData() {

    private final int[] NOTHING = new int[0];
    
    public int[] indirectlyReadLocals(int instructionIndex) {
      return NOTHING;
    }

    public int[] indirectlyWrittenLocals(int instructionIndex) {
      return NOTHING;
    }
    
  };
  
  public IndirectionData getIndirectionData() {
    return NO_INDIRECTIONS;
  }
}
