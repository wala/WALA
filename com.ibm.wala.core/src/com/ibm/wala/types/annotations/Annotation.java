/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.types.annotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.wala.shrikeCT.AnnotationsReader;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * Represents a member annotation, e.g., Java 5.0 class file annotations
 */
public class Annotation {

  private final TypeReference type;

  /**
   * named arguments to the annotation, represented as a mapping from name to
   * value. Note that for Java annotation arguments, the values are always
   * Strings, independent of their actual type in the bytecode.
   */
  private final Map<String, ElementValue> namedArguments;

  /**
   * unnamed arguments to the annotation (e.g., constructor arguments for C#
   * attributes), represented as an array of pairs (T,V), where T is the
   * argument type and V is the value. The array preserves the order in which
   * the arguments were passed.  If null, there are no unnamed arguments. 
   */
  private final Pair<TypeReference, Object>[] unnamedArguments;

  private Annotation(TypeReference type, Map<String, ElementValue> namedArguments, Pair<TypeReference, Object>[] unnamedArguments) {
    this.type = type;
    if (namedArguments == null) {
      throw new IllegalArgumentException("namedArguments is null");
    }
    this.namedArguments = namedArguments;
    this.unnamedArguments = unnamedArguments;
  }

  public static Annotation makeUnnamedAndNamed(TypeReference t, Map<String, ElementValue> namedArguments, Pair<TypeReference,Object>[] unnamedArguments) {
    return new Annotation(t, namedArguments, unnamedArguments);
  }
  public static Annotation makeWithUnnamed(TypeReference t, Pair<TypeReference, Object>[] unnamedArguments) {
    return new Annotation(t, Collections.<String,ElementValue>emptyMap(), unnamedArguments);
  }

  public static Annotation make(TypeReference t) {
    return new Annotation(t, Collections.<String,ElementValue>emptyMap(), null);
  }
  
  public static Annotation makeWithNamed(TypeReference t, Map<String,ElementValue> namedArguments) {
    return new Annotation(t, namedArguments, null);
  }
  
  public static Collection<Annotation> getAnnotationsFromReader(AnnotationsReader r, ClassLoaderReference clRef) throws InvalidClassFileException {
    if (r != null) {
      AnnotationAttribute[] allAnnotations = r.getAllAnnotations();      
      Collection<Annotation> result = convertToAnnotations(clRef, allAnnotations);
      return result;
    } else {
      return Collections.emptySet();
    }
    
  }

  /**
   * If r != null, return parameter annotations as an array with length equal to
   * number of annotatable parameters. Otherwise, return null.
   */
  @SuppressWarnings("unchecked")
  public static Collection<Annotation>[] getParameterAnnotationsFromReader(AnnotationsReader r, ClassLoaderReference clRef) throws InvalidClassFileException {
    if (r != null) {
      AnnotationAttribute[][] allAnnots = r.getAllParameterAnnotations();
      Collection<Annotation>[] result = new Collection[allAnnots.length];
      for (int i = 0; i < result.length; i++) {
        result[i] = convertToAnnotations(clRef, allAnnots[i]);
      }
      return result;
    } else {
      return null;
    }
  }

  protected static Collection<Annotation> convertToAnnotations(ClassLoaderReference clRef, AnnotationAttribute[] allAnnotations) {
    Collection<Annotation> result = HashSetFactory.make();
    for (AnnotationAttribute annot : allAnnotations) {
      String type = annot.type;
      type = type.replaceAll(";", "");
      TypeReference t = TypeReference.findOrCreate(clRef, type);
      result.add(makeWithNamed(t, annot.elementValues));
    }
    return result;
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("Annotation type " + type);
    if (unnamedArguments != null) {
      sb.append("[");
      for (Pair<TypeReference, Object> arg : unnamedArguments) {
        sb.append(" " + arg.fst.getName().getClassName() + ":" + arg.snd);
      }
      sb.append(" ]");
    }
    if (!namedArguments.isEmpty()) {
      sb.append(" " + new TreeMap<>(namedArguments));
    }
    return sb.toString();
  }



  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((namedArguments == null) ? 0 : namedArguments.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + Arrays.hashCode(unnamedArguments);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Annotation other = (Annotation) obj;
    if (namedArguments == null) {
      if (other.namedArguments != null)
        return false;
    } else if (!namedArguments.equals(other.namedArguments))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    if (!Arrays.equals(unnamedArguments, other.unnamedArguments))
      return false;
    return true;
  }

  /**
   * Get the unnamed arguments to the annotation (e.g., constructor arguments
   * for C# attributes), represented as an array of pairs (T,V), where T is the
   * argument type and V is the value. The array preserves the order in which
   * the arguments were passed. If null, there are no unnamed arguments.
   */
  public Pair<TypeReference, Object>[] getUnnamedArguments() {
    return unnamedArguments;
  }
 
  /**
   * Get the named arguments to the annotation, represented as a mapping from
   * name to value
   */
  public Map<String,ElementValue> getNamedArguments() {
    return namedArguments;
  }

  /**
   * Get the type of the annotation
   */
  public TypeReference getType() {
    return type;
  }

}
