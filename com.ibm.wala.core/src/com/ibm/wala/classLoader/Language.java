package com.ibm.wala.classLoader;

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public interface Language {

  public static JavaLanguage JAVA = new JavaLanguage();

  Atom getName();

  Language getBaseLanguage();

  void registerDerivedLanguage(Language l);

  Set<Language> getDerivedLanguages();

  TypeReference getRootType();

  TypeReference getThrowableType();

  TypeReference getConstantType(Object o);

  boolean isNullType(TypeReference type);

  boolean isIntType(TypeReference type);

  boolean isLongType(TypeReference type);
  
  boolean isFloatType(TypeReference type);
  
  boolean isDoubleType(TypeReference type);
  
  boolean isStringType(TypeReference type);

  boolean isMetadataType(TypeReference type);

  Object getMetadataToken(Object value);
  
  TypeReference[] getArrayInterfaces();
  
  TypeName lookupPrimitiveType(String name);
  
  SSAInstructionFactory instructionFactory();
  
  Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha) throws InvalidClassFileException;
}
