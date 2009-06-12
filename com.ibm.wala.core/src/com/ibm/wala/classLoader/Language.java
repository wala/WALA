package com.ibm.wala.classLoader;

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * Main interface for language-specific information. This interface helps build analyses which can operate over multiple languages.
 * 
 * TODO: document the rest of this interface.
 */
public interface Language {

  /**
   * The canonical {@link Language} implementation for Java
   */
  public static JavaLanguage JAVA = new JavaLanguage();

  /**
   * What is the name of the language?
   */
  Atom getName();

  /**
   * If this language is "derived" from some other langauge, which one?
   */
  Language getBaseLanguage();

  /**
   * Yuck? Languages are mutable?
   */
  void registerDerivedLanguage(Language l);

  Set<Language> getDerivedLanguages();

  /**
   * What is the root type in a type hierarchy for this language? e.g. java.lang.Object in Java.
   */
  TypeReference getRootType();

  /**
   * What is the root type of exceptions in this language? e.g. java.lang.Throwable in Java
   */
  TypeReference getThrowableType();

  TypeReference getConstantType(Object o);

  boolean isNullType(TypeReference type);

  boolean isIntType(TypeReference type);

  boolean isLongType(TypeReference type);

  boolean isVoidType(TypeReference type);
  
  boolean isFloatType(TypeReference type);

  boolean isDoubleType(TypeReference type);

  boolean isStringType(TypeReference type);

  boolean isMetadataType(TypeReference type);

  boolean isCharType(TypeReference type);
  
  boolean isBooleanType(TypeReference type);
  
  Object getMetadataToken(Object value);

  TypeReference[] getArrayInterfaces();

  TypeName lookupPrimitiveType(String name);

  SSAInstructionFactory instructionFactory();

  Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha) throws InvalidClassFileException;

  TypeReference getStringType();

  TypeReference getMetadataType();
  
  TypeReference getPointerType(TypeReference pointee);
  
  PrimitiveType getPrimitive(TypeReference reference);

  boolean isObjectType(TypeReference reference);
}
