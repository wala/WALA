/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
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
import java.util.Set;

import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * Main interface for language-specific information. This interface helps build
 * analyses which can operate over multiple languages.
 *
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
   * What is the root type in a type hierarchy for this language? e.g.
   * java.lang.Object in Java.
   */
  TypeReference getRootType();

  /**
   * What is the root type of exceptions in this language? e.g.
   * java.lang.Throwable in Java
   */
  TypeReference getThrowableType();

  /**
   * Given a Java constant o, return the appropriate language type to associate
   * with the constant. Possible types for o can be language dependent, but
   * typically include Boolean, String, Integer, Float, etc.
   */
  TypeReference getConstantType(Object o);

  /**
   * Is t the type of the language's null value? Should return true if
   * <code>t == null</code> (?).
   */
  boolean isNullType(TypeReference t);

  boolean isIntType(TypeReference t);

  boolean isLongType(TypeReference t);

  boolean isVoidType(TypeReference t);

  boolean isFloatType(TypeReference t);

  boolean isDoubleType(TypeReference t);

  boolean isStringType(TypeReference t);

  /**
   * Is t a "metadata" type for the language, i.e., a type describing some other
   * type (e.g., java.lang.Class for Java)?
   */
  boolean isMetadataType(TypeReference t);

  boolean isCharType(TypeReference t);

  boolean isBooleanType(TypeReference t);

  /**
   * Get the representation of the meta-data corresponding to value. For
   * example, in Java, if value represents some type, the returned object should
   * be the corresponding {@link TypeReference}. The returned object should be
   * appropriate for use as the token in an {@link SSALoadMetadataInstruction}
   * for the language
   * 
   */
  Object getMetadataToken(Object value);

  /**
   * get the interfaces implemented by all arrays in the language
   */
  TypeReference[] getArrayInterfaces();

  /**
   * Given a source-level primitive type name, get the corresponding "low-level"
   * type name, e.g., the corresponding character to use in a Java method
   * descriptor
   */
  TypeName lookupPrimitiveType(String name);

  SSAInstructionFactory instructionFactory();

  /**
   * determine the set of possible exception types a call to target may throw
   */
  Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha) throws InvalidClassFileException;

  TypeReference getStringType();

  TypeReference getPointerType(TypeReference pointee);

  /**
   * get the abstraction of a primitive type to be used for type inference
   * 
   * @see TypeInference
   */
  PrimitiveType getPrimitive(TypeReference reference);
  
  /**
   * do MethodReference objects have declared parameter types?
   */
  boolean methodsHaveDeclaredParameterTypes();
   
}
