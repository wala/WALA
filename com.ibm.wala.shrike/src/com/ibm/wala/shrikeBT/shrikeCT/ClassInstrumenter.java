/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT.shrikeCT;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.ibm.wala.shrikeBT.Compiler;
import com.ibm.wala.shrikeBT.ConstantPoolReader;
import com.ibm.wala.shrikeBT.Decoder.InvalidBytecodeException;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyProvider;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.CodeWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.LineNumberTableReader;
import com.ibm.wala.shrikeCT.LineNumberTableWriter;
import com.ibm.wala.shrikeCT.LocalVariableTableReader;
import com.ibm.wala.shrikeCT.LocalVariableTableWriter;
import com.ibm.wala.shrikeCT.StackMapConstants.StackMapFrame;
import com.ibm.wala.shrikeCT.StackMapTableReader;
import com.ibm.wala.shrikeCT.StackMapTableWriter;

/**
 * This class provides a convenient way to instrument every method in a class. It assumes you are using ShrikeCT to read and write
 * classes. It's stateful; initially every method is set to the original code read from the class, but you can then go in and modify
 * the methods.
 */
final public class ClassInstrumenter {
  final private boolean[] deletedMethods;

  final private MethodData[] methods;

  final private CodeReader[] oldCode;

  final private ClassReader cr;

  final private ConstantPoolReader cpr;

  private boolean createFakeLineNumbers = false;

  private int fakeLineOffset;

  private final String inputName;
  
  private final ClassHierarchyProvider cha;
  
  /**
   * Create a class instrumenter from raw bytes.
   */
  public ClassInstrumenter(String inputName, byte[] bytes, ClassHierarchyProvider cha) throws InvalidClassFileException {
    this(inputName, new ClassReader(bytes), cha);
  }

  /**
   * @return name of resource from which this class was read
   */
  public String getInputName() {
    return inputName;
  }
  
  /**
   * Calling this means that methods without line numbers get fake line numbers added: each bytecode instruction is treated as at
   * line 'offset' + the offset of the instruction.
   */
  public void enableFakeLineNumbers(int offset) {
    createFakeLineNumbers = true;
    fakeLineOffset = offset;
  }

  /**
   * Create a class instrumenter from a preinitialized class reader.
   * 
   * @throws IllegalArgumentException if cr is null
   */
  public ClassInstrumenter(String inputName, ClassReader cr, ClassHierarchyProvider cha) {
    if (cr == null) {
      throw new IllegalArgumentException("cr is null");
    }
    this.cr = cr;
    this.cha = cha;
    methods = new MethodData[cr.getMethodCount()];
    oldCode = new CodeReader[methods.length];
    cpr = CTDecoder.makeConstantPoolReader(cr);
    deletedMethods = new boolean[methods.length];
    this.inputName = inputName;
  }

  /**
   * @return the reader for the class
   */
  public ClassReader getReader() {
    return cr;
  }

  /**
   * Implement this interface to instrument every method of a class using visitMethods() below.
   */
  public static interface MethodExaminer {
    /**
     * Do something to the method.
     */
    public void examineCode(MethodData data);
  }

  private void prepareMethod(int i) throws InvalidClassFileException {
    if (deletedMethods[i]) {
      methods[i] = null;
    } else if (methods[i] == null) {
      ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
      cr.initMethodAttributeIterator(i, iter);
      for (; iter.isValid(); iter.advance()) {
        if (iter.getName().equals("Code")) {
          CodeReader code = new CodeReader(iter);
          CTDecoder d = new CTDecoder(code, cpr);
          try {
            d.decode();
          } catch (InvalidBytecodeException e) {
            throw new InvalidClassFileException(code.getRawOffset(), e.getMessage());
          }
          MethodData md = new MethodData(d, cr.getMethodAccessFlags(i), CTDecoder.convertClassToType(cr.getName()), cr
              .getMethodName(i), cr.getMethodType(i));
          methods[i] = md;
          oldCode[i] = code;
          return;
        }
      }
    }
  }

  /**
   * Indicate that the method should be deleted from the class.
   * 
   * @param i the index of the method to delete
   */
  public void deleteMethod(int i) {
    deletedMethods[i] = true;
  }

  private final static ExceptionHandler[] noHandlers = new ExceptionHandler[0];

  // Xiangyu
  // create a empty method body and then user can apply patches later on
  public MethodData createEmptyMethodData(String name, String sig, int access) {
    // Instruction[] instructions=new Instruction[0];
    Instruction[] instructions = new Instruction[1];
    String type = Util.getReturnType(sig);
    if ("C".equals(type) || "B".equals(type) || "Z".equals(type) || "S".equals(type)) {
      type = "I";
    }
    instructions[0] = ReturnInstruction.make(type);
    ExceptionHandler[][] handlers = new ExceptionHandler[instructions.length][];
    Arrays.fill(handlers, noHandlers);
    int[] i2b = new int[instructions.length];
    for (int i = 0; i < i2b.length; i++) {
      i2b[i] = i;
    }
    MethodData md = null;
    try {
      md = new MethodData(access, Util.makeType(cr.getName()), name, sig, instructions, handlers, i2b);

    } catch (InvalidClassFileException ex) {
      ex.printStackTrace();
    }
    return md;

  }

  /**
   * Do something to every method in the class. This will visit all methods, including those already marked for deletion.
   * 
   * @param me the visitor to apply to each method
   */
  public void visitMethods(MethodExaminer me) throws InvalidClassFileException {
    for (int i = 0; i < methods.length; i++) {
      prepareMethod(i);
      if (methods[i] != null) {
        me.examineCode(methods[i]);
      }
    }
  }

  /**
   * Get the current state of method i. This can be edited using a MethodEditor.
   * 
   * @param i the index of the method to inspect
   */
  public MethodData visitMethod(int i) throws InvalidClassFileException {
    prepareMethod(i);
    return methods[i];
  }

  /**
   * Get the original code resource for the method.
   * 
   * @param i the index of the method to inspect
   */
  public CodeReader getMethodCode(int i) throws InvalidClassFileException {
    prepareMethod(i);
    return oldCode[i];
  }

  /**
   * Reset method i back to the code from the original class, and "undelete" it if it was marked for deletion.
   * 
   * @param i the index of the method to reset
   */
  public void resetMethod(int i) {
    deletedMethods[i] = false;
    methods[i] = null;
  }

  /**
   * Replace the code for method i with new code. This also "undeletes" the method if it was marked for deletion.
   * 
   * @param i the index of the method to replace
   * @throws IllegalArgumentException if md is null
   */
  public void replaceMethod(int i, MethodData md) {
    if (md == null) {
      throw new IllegalArgumentException("md is null");
    }
    deletedMethods[i] = false;
    methods[i] = md;
    oldCode[i] = null;
    md.setHasChanged();
  }

  /**
   * Check whether any methods in the class have actually been changed.
   */
  public boolean isChanged() {
    for (int i = 0; i < methods.length; i++) {
      if (deletedMethods[i] || (methods[i] != null && methods[i].getHasChanged())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a class which is a copy of the original class but with the new method code. We return the ClassWriter used, so more
   * methods and fields (and other changes) can still be added.
   * 
   * We fix up any debug information to be consistent with the changes to the code.
   */
  public ClassWriter emitClass() throws InvalidClassFileException {
    return emitClass(new ClassWriter());
  }

  public ClassWriter emitClass(ClassWriter w) throws InvalidClassFileException {
    emitClassInto(w);
    return w;
  }

  /**
   * Copy the contents of the old class, plus any method modifications, into a new ClassWriter. The ClassWriter must be empty!
   * 
   * @param w the classwriter to copy into.
   */
  private void emitClassInto(ClassWriter w) throws InvalidClassFileException {
    w.setMajorVersion(cr.getMajorVersion());
    w.setMinorVersion(cr.getMinorVersion());
    w.setRawCP(cr.getCP(), false);
    w.setAccessFlags(cr.getAccessFlags());
    w.setNameIndex(cr.getNameIndex());
    w.setSuperNameIndex(cr.getSuperNameIndex());
    w.setInterfaceNameIndices(cr.getInterfaceNameIndices());

    int fieldCount = cr.getFieldCount();
    for (int i = 0; i < fieldCount; i++) {
      w.addRawField(new ClassWriter.RawElement(cr.getBytes(), cr.getFieldRawOffset(i), cr.getFieldRawSize(i)));
    }

    for (int i = 0; i < methods.length; i++) {
      MethodData md = methods[i];
      if (!deletedMethods[i]) {
        if (md == null || !md.getHasChanged()) {
          w.addRawMethod(new ClassWriter.RawElement(cr.getBytes(), cr.getMethodRawOffset(i), cr.getMethodRawSize(i)));
        } else {
          CTCompiler comp = CTCompiler.make(w, md);
          comp.setPresetConstants(cpr);

          try {
            comp.compile();
          } catch (Error ex) {
            ex.printStackTrace();
            throw new Error("Error compiling method " + md + ": " + ex.getMessage());
          } catch (Exception ex) {
            ex.printStackTrace();
            throw new Error("Error compiling method " + md + ": " + ex.getMessage());
          }

          CodeReader oc = oldCode[i];
          int flags = cr.getMethodAccessFlags(i);
          // we're not installing a native method here
          flags &= ~ClassConstants.ACC_NATIVE;
          w.addMethod(flags, cr.getMethodNameIndex(i), cr.getMethodTypeIndex(i), makeMethodAttributes(i, w, oc, comp.getOutput(), md));
          Compiler.Output[] aux = comp.getAuxiliaryMethods();
          if (aux != null) {
            for (Compiler.Output a : aux) {
              w.addMethod(a.getAccessFlags(), a.getMethodName(), a.getMethodSignature(), makeMethodAttributes(i, w, oc, a, md));
            }
          }
        }
      }
    }

    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
    cr.initClassAttributeIterator(iter);
    for (; iter.isValid(); iter.advance()) {
      w.addClassAttribute(new ClassWriter.RawElement(cr.getBytes(), iter.getRawOffset(), iter.getRawSize()));
    }
  }

  private static CodeWriter makeNewCode(ClassWriter w, Compiler.Output output) {
    CodeWriter code = new CodeWriter(w);
    code.setMaxStack(output.getMaxStack());
    code.setMaxLocals(output.getMaxLocals());
    code.setCode(output.getCode());
    code.setRawHandlers(output.getRawHandlers());
    return code;
  }

  private LineNumberTableWriter makeNewLines(ClassWriter w, CodeReader oldCode, Compiler.Output output)
      throws InvalidClassFileException {
    int[] newLineMap = null;
    int[] oldLineMap = LineNumberTableReader.makeBytecodeToSourceMap(oldCode);
    if (oldLineMap != null) {
      // Map the old line number map onto the new bytecodes
      int[] newToOldMap = output.getNewBytecodesToOldBytecodes();
      newLineMap = new int[newToOldMap.length];
      for (int i = 0; i < newToOldMap.length; i++) {
        int old = newToOldMap[i];
        if (old >= 0) {
          newLineMap[i] = oldLineMap[old];
        }
      }
    } else if (createFakeLineNumbers) {
      newLineMap = new int[output.getCode().length];
      for (int i = 0; i < newLineMap.length; i++) {
        newLineMap[i] = i + fakeLineOffset;
      }
    } else {
      return null;
    }

    // Now compress it into the JVM form
    int[] rawTable = LineNumberTableWriter.makeRawTable(newLineMap);
    if (rawTable == null || rawTable.length == 0) {
      return null;
    } else {
      LineNumberTableWriter lines = new LineNumberTableWriter(w);
      lines.setRawTable(rawTable);
      return lines;
    }
  }

  private static LocalVariableTableWriter makeNewLocals(ClassWriter w, CodeReader oldCode, Compiler.Output output)
      throws InvalidClassFileException {
    int[][] oldMap = LocalVariableTableReader.makeVarMap(oldCode);
    if (oldMap != null) {
      // Map the old map onto the new bytecodes
      int[] newToOldMap = output.getNewBytecodesToOldBytecodes();
      int[][] newMap = new int[newToOldMap.length][];
      int[] lastLocals = null;
      for (int i = 0; i < newToOldMap.length; i++) {
        int old = newToOldMap[i];
        if (old >= 0) {
          newMap[i] = oldMap[old];
          lastLocals = newMap[i];
        } else {
          newMap[i] = lastLocals;
        }
      }

      int[] rawTable = LocalVariableTableWriter.makeRawTable(newMap, output);
      if (rawTable == null || rawTable.length == 0) {
        return null;
      } else {
        LocalVariableTableWriter locals = new LocalVariableTableWriter(w);
        locals.setRawTable(rawTable);
        return locals;
      }
    } else {
      return null;
    }
  }

  private ClassWriter.Element[] makeMethodAttributes(int m, ClassWriter w, CodeReader oldCode, Compiler.Output output, MethodData md)
      throws InvalidClassFileException {
    CodeWriter code = makeNewCode(w, output);

    int codeAttrCount = 0;
    LineNumberTableWriter lines = null;
    LocalVariableTableWriter locals = null;
    StackMapTableWriter stacks = null;
    if (oldCode != null) {
      lines = makeNewLines(w, oldCode, output);
      if (lines != null) {
        codeAttrCount++;
      }
      locals = makeNewLocals(w, oldCode, output);
      if (locals != null) {
        codeAttrCount++;
      }
      if (oldCode.getClassReader().getMajorVersion() > 50) {
        try { 
          List<StackMapFrame> sm = StackMapTableReader.readStackMap(oldCode);

          String[][] varTypes = null;
          int[] newToOld = output.getNewBytecodesToOldBytecodes();
          int[][] vars = LocalVariableTableReader.makeVarMap(oldCode);
          if (vars != null) {
            varTypes = new String[newToOld.length][];
            for(int i = 0; i < newToOld.length; i++) {
              int idx = newToOld[i];
              if (idx != -1 && vars[idx] != null) {
                varTypes[i] = new String[vars[idx].length / 2];
                for(int j = 1; j < vars[idx].length; j += 2) {
                  int type = vars[idx][j];
                  varTypes[i][j/2] = type==0? null: oldCode.getClassReader().getCP().getCPUtf8(type);
                }
              }
            }
          }
          
          stacks = new StackMapTableWriter(w, md, output, cha, varTypes , sm);
          codeAttrCount++;
        } catch (IOException | FailureException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    ClassWriter.Element[] codeAttributes = new ClassWriter.Element[codeAttrCount];
    int codeAttrIndex = 0;
    if (lines != null) {
      codeAttributes[codeAttrIndex++] = lines;
    }
    if (locals != null) {
      codeAttributes[codeAttrIndex++] = locals;
    }
    if (stacks != null) {
      codeAttributes[codeAttrIndex++] = stacks;      
    }
    code.setAttributes(codeAttributes);

    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
    cr.initMethodAttributeIterator(m, iter);
    int methodAttrCount = iter.getRemainingAttributesCount();
    if (oldCode == null) {
      methodAttrCount++;
    }
    ClassWriter.Element[] methodAttributes = new ClassWriter.Element[methodAttrCount];
    for (int i = 0; iter.isValid(); iter.advance()) {
      if (iter.getName().equals("Code")) {
        methodAttributes[i] = code;
        code = null;
        if (oldCode == null) {
          throw new Error("No old code provided, but Code attribute found");
        }
      } else {
        methodAttributes[i] = new ClassWriter.RawElement(cr.getBytes(), iter.getRawOffset(), iter.getRawSize());
      }
      i++;
    }
    if (oldCode == null) {
      if (code == null) {
        throw new Error("Old code not provided but existing code was found and replaced");
      }
      methodAttributes[methodAttrCount - 1] = code;
    }

    return methodAttributes;
  }
}
