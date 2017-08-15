/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.loader;

import java.util.Collection;
import java.util.Map;

import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;

public abstract class AstMethod implements IMethod {

  public interface Retranslatable {
    void retranslate(AstTranslator xlator);
    CAstEntity getEntity();
  }
  
  public interface DebuggingInformation {

    Position getCodeBodyPosition();

    Position getInstructionPosition(int instructionOffset);

    String[][] getSourceNamesForValues();

  }

  /**
   * lexical access information for some entity scope. used during call graph
   * construction to handle lexical accesses.
   */
  public interface LexicalInformation {

    /**
     * names possibly accessed in a nested lexical scope, represented as pairs
     * (name,nameOfDefiningEntity)
     */
    public Pair<String, String>[] getExposedNames();

    /**
     * maps each exposed name (via its index in {@link #getExposedNames()}) to
     * its value number at method exit.
     */
    public int[] getExitExposedUses();

    /**
     * get a map from exposed name (via its index in {@link #getExposedNames()})
     * to its value number at the instruction at offset instructionOffset.
     */
    public int[] getExposedUses(int instructionOffset);

    /**
     * return all value numbers appearing as entries in either
     * {@link #getExposedUses(int)} or {@link #getExitExposedUses()}
     */
    public IntSet getAllExposedUses();

    /**
     * return the names of the enclosing methods declaring names that are
     * lexically accessed by the entity
     */
    public String[] getScopingParents();

    /**
     * returns true if name may be read in nested lexical scopes but cannot be
     * written
     */
    public boolean isReadOnly(String name);

    /**
     * get the name of this entity, as it appears in the definer portion of a
     * lexical name
     */
    public String getScopingName();

  }

  protected final IClass cls;
  private final Collection<CAstQualifier> qualifiers;
  private final AbstractCFG<?, ?> cfg;
  private final SymbolTable symtab;
  private final MethodReference ref;
  private final boolean hasCatchBlock;
  private final boolean hasMonitorOp;
  private final Map<IBasicBlock<SSAInstruction>, TypeReference[]> catchTypes;
  private final AstLexicalInformation lexicalInfo;
  private final DebuggingInformation debugInfo;
  private final Collection<Annotation> annotations;

  protected AstMethod(IClass cls, Collection<CAstQualifier> qualifiers, AbstractCFG<?, ?> cfg, SymbolTable symtab, MethodReference ref,
      boolean hasCatchBlock, Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo,
      DebuggingInformation debugInfo, Collection<Annotation> annotations) {
    this.cls = cls;
    this.cfg = cfg;
    this.ref = ref;
    this.symtab = symtab;
    this.qualifiers = qualifiers;
    this.catchTypes = caughtTypes;
    this.hasCatchBlock = hasCatchBlock;
    this.hasMonitorOp = hasMonitorOp;
    this.lexicalInfo = lexicalInfo;
    this.debugInfo = debugInfo;
    this.annotations = annotations;
  }

  protected AstMethod(IClass cls, Collection<CAstQualifier> qualifiers, MethodReference ref, Collection<Annotation> annotations) {
    this.cls = cls;
    this.qualifiers = qualifiers;
    this.ref = ref;
    this.annotations = annotations;

    this.cfg = null;
    this.symtab = null;
    this.catchTypes = null;
    this.hasCatchBlock = false;
    this.hasMonitorOp = false;
    this.lexicalInfo = null;
    this.debugInfo = null;

    assert isAbstract();
  }

  public AbstractCFG<?, ?> cfg() {
    return cfg;
  }

  public boolean hasCatchBlock() {
    return hasCatchBlock();
  }

  public SymbolTable symbolTable() {
    return symtab;
  }

  public Map<IBasicBlock<SSAInstruction>, TypeReference[]> catchTypes() {
    return catchTypes;
  }

  public LexicalInformation cloneLexicalInfo() {
    return new AstLexicalInformation(lexicalInfo);
  }

  public LexicalInformation lexicalInfo() {
    return lexicalInfo;
  }

  public DebuggingInformation debugInfo() {
    return debugInfo;
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return annotations;
  }

  /**
   * Parents of this method with respect to lexical scoping, that is, methods
   * containing state possibly referenced lexically in this method
   */
  public static abstract class LexicalParent {
    public abstract String getName();

    public abstract AstMethod getMethod();

    @Override
    public int hashCode() {
      return getName().hashCode() * getMethod().hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof LexicalParent) && getName().equals(((LexicalParent) o).getName())
          && getMethod().equals(((LexicalParent) o).getMethod());
    }
  }

  public abstract LexicalParent[] getParents();

  @Override
  public IClass getDeclaringClass() {
    return cls;
  }

  @Override
  public String getSignature() {
    return ref.getSignature();
  }

  @Override
  public Selector getSelector() {
    return ref.getSelector();
  }

  @Override
  public boolean isClinit() {
    return getSelector().equals(MethodReference.clinitSelector);
  }

  @Override
  public boolean isInit() {
    return getSelector().getName().equals(MethodReference.initAtom);
  }

  @Override
  public Atom getName() {
    return ref.getName();
  }

  @Override
  public Descriptor getDescriptor() {
    return ref.getDescriptor();
  }

  @Override
  public MethodReference getReference() {
    return ref;
  }

  @Override
  public TypeReference getReturnType() {
    return ref.getReturnType();
  }

  @Override
  public boolean isStatic() {
    return qualifiers.contains(CAstQualifier.STATIC);
  }

  @Override
  public boolean isSynchronized() {
    return qualifiers.contains(CAstQualifier.SYNCHRONIZED);
  }

  @Override
  public boolean isNative() {
    return qualifiers.contains(CAstQualifier.NATIVE);
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return qualifiers.contains(CAstQualifier.ABSTRACT);
  }

  @Override
  public boolean isPrivate() {
    return qualifiers.contains(CAstQualifier.PRIVATE);
  }

  @Override
  public boolean isProtected() {
    return qualifiers.contains(CAstQualifier.PROTECTED);
  }

  @Override
  public boolean isPublic() {
    return qualifiers.contains(CAstQualifier.PUBLIC);
  }

  @Override
  public boolean isFinal() {
    return qualifiers.contains(CAstQualifier.FINAL);
  }

  @Override
  public boolean isBridge() {
    return qualifiers.contains(CAstQualifier.VOLATILE);
  }

  public ControlFlowGraph<?, ?> getControlFlowGraph() {
    return cfg;
  }

  @Override
  public boolean hasExceptionHandler() {
    return hasCatchBlock;
  }

  public boolean hasMonitorOp() {
    return hasMonitorOp;
  }

  @Override
  public int getNumberOfParameters() {
    return symtab.getParameterValueNumbers().length;
  }
/** BEGIN Custom change: precise bytecode positions */
  
  /*
   * @see com.ibm.wala.classLoader.IMethod#getParameterSourcePosition(int)
   */
  @Override
  public SourcePosition getParameterSourcePosition(int paramNum) throws InvalidClassFileException {
    return null;
  }
/** END Custom change: precise bytecode positions */

  @Override
  public int getLineNumber(int instructionIndex) {
    Position pos = debugInfo.getInstructionPosition(instructionIndex);
    if (pos == null) {
      return -1;
    } else {
      return pos.getFirstLine();
    }
  }

  public Position getSourcePosition() {
    return debugInfo.getCodeBodyPosition();
  }

  @Override
  public Position getSourcePosition(int instructionIndex) {
    return debugInfo.getInstructionPosition(instructionIndex);
  }
}
