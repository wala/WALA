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

import com.ibm.wala.shrikeBT.ConstantPoolReader;
import com.ibm.wala.shrikeBT.Decoder;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.ConstantPoolParser;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * This class decodes Java bytecodes into ShrikeBT code using a ShrikeCT class
 * reader.
 */
final public class CTDecoder extends Decoder {

  /**
   * Decode the code resource 'r'.
   */
  public CTDecoder(CodeReader r) {
    this(r, makeConstantPoolReader(r.getClassReader()));
  }

  /**
   * Decode the code resource 'r' using the predeclared constant pool reader
   * 'cpr' (obtained by makeConstantPoolReader below).
   */
  public CTDecoder(CodeReader r, ConstantPoolReader cpr) {
    super(r.getBytecode(), r.getRawHandlers(), cpr);
  }

  /**
   * Convert the internal JVM class name to a JVM type name (e.g.,
   * java/lang/Object to Ljava/lang/Object;).
   */
  public static String convertClassToType(String s) {
    if (s.length() > 0 && s.charAt(0) != '[') {
      return "L" + s + ";";
    } else {
      return s;
    }
  }

  /**
   * Build a ConstantPoolReader implementation to read the constant pool from
   * 'cr'.
   */
  public static ConstantPoolReader makeConstantPoolReader(ClassReader cr) {
    return new CPReader(cr.getCP());
  }

  final static class CPReader extends ConstantPoolReader {
    private ConstantPoolParser cp;

    CPReader(ConstantPoolParser cp) {
      this.cp = cp;
    }

    public int getConstantPoolItemType(int index) {
      return cp.getItemType(index);
    }

    private Error convertToError(InvalidClassFileException e) {
      e.printStackTrace();
      return new Error("Invalid class file: " + e.getMessage());
    }

    public int getConstantPoolInteger(int index) {
      try {
        return cp.getCPInt(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public float getConstantPoolFloat(int index) {
      try {
        return cp.getCPFloat(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public long getConstantPoolLong(int index) {
      try {
        return cp.getCPLong(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public double getConstantPoolDouble(int index) {
      try {
        return cp.getCPDouble(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public String getConstantPoolString(int index) {
      try {
        return cp.getCPString(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public String getConstantPoolClassType(int index) {
      try {
        return convertClassToType(cp.getCPClass(index));
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public String getConstantPoolMemberClassType(int index) {
      try {
        return convertClassToType(cp.getCPRefClass(index));
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public String getConstantPoolMemberName(int index) {
      try {
        return cp.getCPRefName(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    public String getConstantPoolMemberType(int index) {
      try {
        return cp.getCPRefType(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }
  }
}