/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util;

import java.util.Collection;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.RuntimeInvisibleAnnotationsReader.UnimplementedException;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.debug.Assertions;

public class InternalUtils {
  public static final TypeName INTERNAL = TypeName.findOrCreateClassName("com/ibm/wala/annotations", "Internal");

  /**
   * Does a particular method have the WALA "internal" annotation?
   */
  public static boolean hasInternalAnnotation(IMethod m) {
    if (m instanceof ShrikeCTMethod) {
      Collection<Annotation> annotations = null;
      try {
        annotations = ((ShrikeCTMethod) m).getRuntimeInvisibleAnnotations();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      } catch (UnimplementedException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      for (Annotation a : annotations) {
        if (a.getType().getName().equals(INTERNAL)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Does a particular class have the WALA "internal" annotation?
   */
  public static boolean hasInternalAnnotation(IClass c) {
    if (c instanceof ShrikeClass) {
      Collection<Annotation> annotations = null;
      try {
        annotations = ((ShrikeClass) c).getRuntimeInvisibleAnnotations();
      } catch (InvalidClassFileException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      } catch (UnimplementedException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      for (Annotation a : annotations) {
        if (a.getType().getName().equals(INTERNAL)) {
          return true;
        }
      }
    }
    return false;
  }
}
