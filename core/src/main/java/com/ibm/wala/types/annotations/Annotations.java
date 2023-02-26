/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.types.annotations;

import com.ibm.wala.classLoader.FieldImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.debug.Assertions;
import java.util.Collection;

public class Annotations {
  public static final TypeName INTERNAL =
      TypeName.findOrCreateClassName("com/ibm/wala/annotations", "Internal");

  public static final TypeName NONNULL =
      TypeName.findOrCreateClassName("com/ibm/wala/annotations", "NonNull");

  /** Does a particular method have a particular annotation? */
  public static boolean hasAnnotation(IMethod m, TypeName type) {
    if (m instanceof ShrikeCTMethod) {
      Collection<Annotation> annotations = null;
      try {
        annotations = ((ShrikeCTMethod) m).getRuntimeInvisibleAnnotations();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      for (Annotation a : annotations) {
        if (a.getType().getName().equals(type)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Does a particular class have a particular annotation? */
  public static boolean hasAnnotation(IClass c, TypeName type) {
    if (c instanceof ShrikeClass) {
      Collection<Annotation> annotations = null;
      try {
        annotations = ((ShrikeClass) c).getRuntimeInvisibleAnnotations();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      for (Annotation a : annotations) {
        if (a.getType().getName().equals(type)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean hasAnnotation(IField field, TypeName type) {
    if (field instanceof FieldImpl) {
      FieldImpl f = (FieldImpl) field;
      Collection<Annotation> annotations = f.getAnnotations();
      if (annotations != null) {
        for (Annotation a : annotations) {
          if (a.getType().getName().equals(type)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
