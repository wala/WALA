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

import java.io.IOException;
import java.util.Collection;

import com.ibm.wala.classLoader.ShrikeClass.GetReader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.Decoder;
import com.ibm.wala.shrikeBT.IndirectionData;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeCT.AnnotationsReader;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationType;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.ExceptionsReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.LineNumberTableReader;
import com.ibm.wala.shrikeCT.LocalVariableTableReader;
import com.ibm.wala.shrikeCT.SignatureReader;
import com.ibm.wala.shrikeCT.SourceFileReader;
import com.ibm.wala.shrikeCT.SourcePositionTableReader;
import com.ibm.wala.shrikeCT.SourcePositionTableReader.Position;
import com.ibm.wala.shrikeCT.TypeAnnotationsReader;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.types.annotations.TypeAnnotation;
import com.ibm.wala.types.generics.MethodTypeSignature;
import com.ibm.wala.util.collections.HashSetFactory;
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
/** BEGIN Custom change: precise positions */
  
  private static final class SPos implements SourcePosition {
    String fileName;
    final int firstLine;
    final int lastLine;
    final int firstCol;
    final int lastCol;
    
    private SPos(String fileName, int firstLine, int lastLine, int firstCol, int lastCol) {
      this.firstLine = firstLine;
      this.lastLine = lastLine;
      this.firstCol = firstCol;
      this.lastCol = lastCol;
      this.fileName = fileName;
    }


    @Override
    public int getFirstCol() {
      return firstCol;
    }

    @Override
    public int getFirstLine() {
      return firstLine;
    }

    @Override
    public int getFirstOffset() {
      return 0;
    }

    @Override
    public int getLastCol() {
      return lastCol;
    }

    @Override
    public int getLastLine() {
      return lastLine;
    }

    @Override
    public int getLastOffset() {
      return 0;
    }

    @Override
    public int compareTo(Object o) {
      if (o instanceof SourcePosition) {
        SourcePosition p = (SourcePosition) o;
        if (firstLine != p.getFirstLine()) {
          return firstLine - p.getFirstLine();
        } else if (firstCol != p.getFirstCol()) {
          return firstCol - p.getFirstCol();
        } else if (lastLine != p.getLastLine()) {
          return lastLine - p.getLastLine();
        } else if (lastCol != p.getLastCol()) {
          return lastCol - p.getLastCol();
        } else {
          return 0;
        }
      } else {
        return -1;
      }
    }
    
    @Override
    public String toString() {
      return fileName + "(" + firstLine + "," + firstCol + "-" + lastLine + "," + lastCol + ")";
    }
  }
/** END Custom change: precise positions */
  
  @Override
  protected void processDebugInfo(BytecodeInfo bcInfo) throws InvalidClassFileException {
    CodeReader cr = getCodeReader();
    bcInfo.lineNumberMap = LineNumberTableReader.makeBytecodeToSourceMap(cr);
    bcInfo.localVariableMap = LocalVariableTableReader.makeVarMap(cr);
/** BEGIN Custom change: precise bytecode positions */
    
    Position param = null;
    try {
        param = SourcePositionTableReader.findParameterPosition(shrikeMethodIndex, cr);
    } catch (IOException e) {
        e.printStackTrace();
    }
    
    bcInfo.paramPositionMap = new SPos[getNumberOfParameters()];
    if (param != null) {
      String fileName = ((ShrikeClass)getDeclaringClass()).getSourceFileReader().getSourceFile();
      SPos paramPos = new SPos(fileName, param.firstLine, param.lastLine, param.firstCol, param.lastCol);
      for (int i = 0; i < getNumberOfParameters(); i++) {
        bcInfo.paramPositionMap[i] = paramPos;
      }
    }

    Position pos[] = null;
    try {
      pos = SourcePositionTableReader.makeBytecodeToPositionMap(cr);
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    if (pos == null && bcInfo.lineNumberMap != null) {
      pos = SourcePositionTableReader.makeLineNumberToPositionMap(bcInfo.lineNumberMap);
    }
    
    if (pos != null) {
      String sourceFile = null;
      SourceFileReader reader = ((ShrikeClass)getDeclaringClass()).getSourceFileReader();
      if (reader != null) {
        sourceFile = reader.getSourceFile();
      }
      bcInfo.positionMap = new SPos[pos.length];
      for (int i = 0; i < pos.length; i++) {
        Position p = pos[i];
        bcInfo.positionMap[i] = new SPos(sourceFile, p.firstLine, p.lastLine, p.firstCol, p.lastCol);
      }
    }
/** END Custom change: : precise bytecode positions */
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
      throw new IllegalArgumentException("illegal local number: " + localNumber + ", method " + getDeclaringClass().getName() + 
              "." + getName() + " uses at most " + getMaxLocals());
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

  private <T> T getReader(String attrName, GetReader<T> reader) {
    ClassReader.AttrIterator iter = new AttrIterator();
    getClassReader().initMethodAttributeIterator(shrikeMethodIndex, iter);

    return ShrikeClass.getReader(iter, attrName, reader);
  }

  private CodeReader getCodeReader() {
    return getReader("Code", CodeReader::new);
  }
 
  private ExceptionsReader getExceptionReader() {
    return getReader("Exceptions", ExceptionsReader::new);
  }
 
  private SignatureReader getSignatureReader() {
    return getReader("Signature", SignatureReader::new);
  }

  private AnnotationsReader getAnnotationsReader(AnnotationType type) {
    ClassReader.AttrIterator iter = new AttrIterator();
    getClassReader().initMethodAttributeIterator(shrikeMethodIndex, iter);

    return AnnotationsReader.getReaderForAnnotation(type, iter);
  }
  
  private TypeAnnotationsReader getTypeAnnotationsReaderAtMethodInfo(TypeAnnotationsReader.AnnotationType type) {
    ClassReader.AttrIterator iter = new AttrIterator();
    getClassReader().initMethodAttributeIterator(shrikeMethodIndex, iter);

    return TypeAnnotationsReader.getReaderForAnnotationAtMethodInfo(type, iter, getExceptionReader(), getSignatureReader());
  }
  
  private TypeAnnotationsReader getTypeAnnotationsReaderAtCode(TypeAnnotationsReader.AnnotationType type) {
    final CodeReader codeReader = getCodeReader();
    if (codeReader == null) return null;
    
    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
    codeReader.initAttributeIterator(iter);
    return TypeAnnotationsReader.getReaderForAnnotationAtCode(type, iter, getCodeReader());
  }

  private String computeGenericsSignature() throws InvalidClassFileException {
    SignatureReader reader = getSignatureReader();
    if (reader == null) {
      return null;
    } else {
      return reader.getSignature();
    }
  }

  @Override
  public TypeReference getReturnType() {
    return getReference().getReturnType();
  }

  @Override
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

  @Override
  public Collection<Annotation> getAnnotations(boolean runtimeInvisible) throws InvalidClassFileException {
    AnnotationsReader r = getAnnotationsReader(runtimeInvisible ? AnnotationType.RuntimeInvisibleAnnotations
        : AnnotationType.RuntimeVisibleAnnotations);
    return Annotation.getAnnotationsFromReader(r, getDeclaringClass().getClassLoader().getReference());
  }
  
  public Collection<TypeAnnotation> getTypeAnnotationsAtMethodInfo(boolean runtimeInvisible) throws InvalidClassFileException {
    TypeAnnotationsReader r = getTypeAnnotationsReaderAtMethodInfo(
        runtimeInvisible ? TypeAnnotationsReader.AnnotationType.RuntimeInvisibleTypeAnnotations
                         : TypeAnnotationsReader.AnnotationType.RuntimeVisibleTypeAnnotations
    );
    final ClassLoaderReference clRef = getDeclaringClass().getClassLoader().getReference();
    return TypeAnnotation.getTypeAnnotationsFromReader(
        r,
        TypeAnnotation.targetConverterAtMethodInfo(clRef),
        clRef
    );
  }
  
  public Collection<TypeAnnotation> getTypeAnnotationsAtCode(boolean runtimeInvisible) throws InvalidClassFileException {
    TypeAnnotationsReader r = getTypeAnnotationsReaderAtCode(
        runtimeInvisible ? TypeAnnotationsReader.AnnotationType.RuntimeInvisibleTypeAnnotations
                         : TypeAnnotationsReader.AnnotationType.RuntimeVisibleTypeAnnotations
    );
    final ClassLoaderReference clRef = getDeclaringClass().getClassLoader().getReference();
    return TypeAnnotation.getTypeAnnotationsFromReader(
        r,
        TypeAnnotation.targetConverterAtCode(clRef, this),
        clRef
    );
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    Collection<Annotation> result = HashSetFactory.make();
    try {
      result.addAll(getAnnotations(true));
      result.addAll(getAnnotations(false));
    } catch (InvalidClassFileException e) {
      
    }
    return result;
  }

  /**
   * get annotations on parameters as an array of Collections, where each array
   * element gives the annotations on the corresponding parameter. Note that the
   * 'this' parameter for an instance method cannot have annotations.
   */
  @Override
  public Collection<Annotation>[] getParameterAnnotations() {
    int numAnnotatedParams = isStatic() ? getNumberOfParameters() : getNumberOfParameters() - 1;
    @SuppressWarnings("unchecked")
    Collection<Annotation>[] result = new Collection[numAnnotatedParams];
    for (int i = 0; i < result.length; i++) {
      result[i] = HashSetFactory.make();
    }
    try {
      ClassLoaderReference reference = getDeclaringClass().getClassLoader().getReference();
      AnnotationsReader r = getAnnotationsReader(AnnotationType.RuntimeInvisibleParameterAnnotations);
      Collection<Annotation>[] paramAnnots = Annotation.getParameterAnnotationsFromReader(r, reference);
      if (paramAnnots != null) {
        assert paramAnnots.length == result.length : paramAnnots.length + " != " + result.length;
        for (int i = 0; i < result.length; i++) {
          result[i].addAll(paramAnnots[i]);
        }
      }
      r = getAnnotationsReader(AnnotationType.RuntimeVisibleParameterAnnotations);
      paramAnnots = Annotation.getParameterAnnotationsFromReader(r, reference);
      if (paramAnnots != null) {
        assert paramAnnots.length == result.length;
        for (int i = 0; i < result.length; i++) {
          result[i].addAll(paramAnnots[i]);
        }
      }
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
    }
    return result;
  }
  
  private static final IndirectionData NO_INDIRECTIONS = new IndirectionData() {

    private final int[] NOTHING = new int[0];
    
    @Override
    public int[] indirectlyReadLocals(int instructionIndex) {
      return NOTHING;
    }

    @Override
    public int[] indirectlyWrittenLocals(int instructionIndex) {
      return NOTHING;
    }
    
  };
  
  @Override
  public IndirectionData getIndirectionData() {
    return NO_INDIRECTIONS;
  }
}
