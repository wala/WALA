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
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.ConstantPoolParser;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * This class decodes Java bytecodes into ShrikeBT code using a ShrikeCT class reader.
 */
final public class CTDecoder extends Decoder {

  /**
   * Decode the code resource 'r'.
   * 
   * @throws NullPointerException if r is null
   */
  public CTDecoder(CodeReader r) throws NullPointerException {
    this(r, makeConstantPoolReader(r.getClassReader()));
  }

  /**
   * Decode the code resource 'r' using the predeclared constant pool reader 'cpr' (obtained by makeConstantPoolReader below).
   * 
   * @throws NullPointerException if r is null
   */
  public CTDecoder(CodeReader r, ConstantPoolReader cpr) throws NullPointerException {
    super(r.getBytecode(), r.getRawHandlers(), cpr);
  }

  /**
   * Convert the internal JVM class name to a JVM type name (e.g., java/lang/Object to Ljava/lang/Object;).
   * 
   * @throws IllegalArgumentException if s is null
   */
  public static String convertClassToType(String s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    if (s.length() > 0 && s.charAt(0) != '[') {
      return "L" + s + ";";
    } else {
      return s;
    }
  }

  /**
   * Build a ConstantPoolReader implementation to read the constant pool from 'cr'.
   */
  public static ConstantPoolReader makeConstantPoolReader(ClassReader cr) throws IllegalArgumentException {
    if (cr == null) {
      throw new IllegalArgumentException("illegal null cr");
    }
    return new CPReader(cr.getCP());
  }

  final static class CPReader extends ConstantPoolReader {
    final private ConstantPoolParser cp;

    CPReader(ConstantPoolParser cp) {
      this.cp = cp;
    }

    @Override
    public int getConstantPoolItemType(int index) {
      return cp.getItemType(index);
    }

    private Error convertToError(InvalidClassFileException e) {
      e.printStackTrace();
      return new Error("Invalid class file: " + e.getMessage());
    }

    @Override
    public int getConstantPoolInteger(int index) {
      try {
        return cp.getCPInt(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public float getConstantPoolFloat(int index) {
      try {
        return cp.getCPFloat(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public long getConstantPoolLong(int index) {
      try {
        return cp.getCPLong(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public double getConstantPoolDouble(int index) {
      try {
        return cp.getCPDouble(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolMethodType(int index) {
      try {
        return cp.getCPMethodType(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolString(int index) {
      try {
        return cp.getCPString(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolClassType(int index) {
      try {
        return convertClassToType(cp.getCPClass(index));
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolMemberClassType(int index) {
      try {
        return convertClassToType(cp.getCPRefClass(index));
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolMemberName(int index) {
      try {
        return cp.getCPRefName(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolMemberType(int index) {
      try {
        return cp.getCPRefType(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolHandleClassType(int index) {
      try {
        return cp.getCPHandleClass(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolHandleName(int index) {
      try {
        return cp.getCPHandleName(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolHandleType(int index) {
      try {
        return cp.getCPHandleType(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public byte getConstantPoolHandleKind(int index) {
      try {
        return cp.getCPHandleKind(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public BootstrapMethod getConstantPoolDynamicBootstrap(int index) {
      try {
        return cp.getCPDynBootstrap(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolDynamicName(int index) {
      try {
        return cp.getCPDynName(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }

    @Override
    public String getConstantPoolDynamicType(int index) {
      try {
        return cp.getCPDynType(index);
      } catch (InvalidClassFileException e) {
        throw convertToError(e);
      }
    }
  }
}
