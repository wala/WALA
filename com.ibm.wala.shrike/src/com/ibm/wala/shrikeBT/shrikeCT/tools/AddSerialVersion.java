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
package com.ibm.wala.shrikeBT.shrikeCT.tools;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeCT.ClassConstants;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.ConstantValueWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

public class AddSerialVersion {

  // this class should not be instantiated
  private AddSerialVersion() {
  }

  /**
   * This method computes the serialVersionUID for class r (if there isn't one already) and adds the field to the classwriter w.
   * 
   * When run as a program, just takes a list of class files as command line arguments and computes their serialVersionUIDs.
   * 
   * @throws IllegalArgumentException if r is null
   */
  public static void addSerialVersionUID(ClassReader r, ClassWriter w) throws InvalidClassFileException {
    if (r == null) {
      throw new IllegalArgumentException("r is null");
    }
    int numFields = r.getFieldCount();
    for (int i = 0; i < numFields; i++) {
      if (r.getFieldName(i).equals("serialVersionUID")) {
        return; // already has a serialVersionUID
      }
    }

    long UID = computeSerialVersionUID(r);
    w.addField(ClassConstants.ACC_PUBLIC | ClassConstants.ACC_STATIC | ClassConstants.ACC_FINAL, "serialVersionUID", "J",
        new ClassWriter.Element[] { new ConstantValueWriter(w, UID) });
  }

  /**
   * This class implements a stream that just discards everything written to it.
   */
  public static final class SinkOutputStream extends OutputStream {
    @Override
    public void write(int b) {
    }

    @Override
    public void write(byte[] b) {
    }

    @Override
    public void write(byte[] b, int off, int len) {
    }
  }

  /**
   * This method computes the serialVersionUID for class r. See the specification at
   * http://java.sun.com/j2se/1.4.2/docs/guide/serialization/spec/class.html
   * 
   * @throws IllegalArgumentException if r is null
   */
  public static long computeSerialVersionUID(final ClassReader r) throws InvalidClassFileException {
    if (r == null) {
      throw new IllegalArgumentException("r is null");
    }
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException e) {
      throw new Error("SHA algorithm not supported: " + e.getMessage());
    }
    try (
      SinkOutputStream sink = new SinkOutputStream();
      DataOutputStream out = new DataOutputStream(new DigestOutputStream(sink, digest));
    ) {
      try {
        // step 1
        out.writeUTF(r.getName());
        // step 2
        out.writeInt(r.getAccessFlags());
  
        // step 3
        String[] interfaces = r.getInterfaceNames();
        Arrays.sort(interfaces);
        for (String interface1 : interfaces) {
          out.writeUTF(interface1);
        }
  
        // step 4
        Integer[] fields = new Integer[r.getFieldCount()];
        final String[] fieldNames = new String[fields.length];
        int fieldCount = 0;
        for (int f = 0; f < fields.length; f++) {
          int flags = r.getFieldAccessFlags(f);
          if ((flags & ClassConstants.ACC_PRIVATE) == 0 || (flags & (ClassConstants.ACC_STATIC | ClassConstants.ACC_TRANSIENT)) == 0) {
            fields[fieldCount] = new Integer(f);
            fieldNames[f] = r.getFieldName(f);
            fieldCount++;
          }
        }
        Arrays.sort(fields, 0, fieldCount, (o1, o2) -> {
          String name1 = fieldNames[o1.intValue()];
          String name2 = fieldNames[o2.intValue()];
          return name1.compareTo(name2);
        });
        for (int i = 0; i < fieldCount; i++) {
          int f = fields[i].intValue();
          out.writeUTF(fieldNames[f]);
          out.writeInt(r.getFieldAccessFlags(f));
          out.writeUTF(r.getFieldType(f));
        }
  
        // steps 5, 6 and 7
        Integer[] methods = new Integer[r.getMethodCount()];
        final int[] methodKinds = new int[methods.length];
        final String[] methodSigs = new String[methods.length];
        int methodCount = 0;
        for (int m = 0; m < methodSigs.length; m++) {
          String name = r.getMethodName(m);
          int flags = r.getMethodAccessFlags(m);
          if (name.equals("<clinit>") || (flags & ClassConstants.ACC_PRIVATE) == 0) {
            methods[methodCount] = new Integer(m);
            methodSigs[m] = name + r.getMethodType(m);
            if (name.equals("<clinit>")) {
              methodKinds[m] = 0;
            } else if (name.equals("<init>")) {
              methodKinds[m] = 1;
            } else {
              methodKinds[m] = 2;
            }
            methodCount++;
          }
        }
        Arrays.sort(methods, 0, methodCount, (o1, o2) -> {
          int m1 = o1.intValue();
          int m2 = o2.intValue();
          if (methodKinds[m1] != methodKinds[m2]) {
            return methodKinds[m1] - methodKinds[m2];
          }
          String name1 = methodSigs[m1];
          String name2 = methodSigs[m2];
          return name1.compareTo(name2);
        });
        for (int i = 0; i < methodCount; i++) {
          int m = methods[i].intValue();
          out.writeUTF(r.getMethodName(m));
          out.writeInt(r.getMethodAccessFlags(m));
          out.writeUTF(r.getMethodType(m));
        }
      } catch (IOException e1) {
        throw new Error("Unexpected IOException: " + e1.getMessage());
      }
    } catch (IOException e2) {
    }

    byte[] hash = digest.digest();
    return (hash[0] & 0xFF) | (hash[1] & 0xFF) << 8 | (hash[2] & 0xFF) << 16 | hash[3] << 24 | (hash[4] & 0xFF) << 32
        | (hash[5] & 0xFF) << 40 | (hash[6] & 0xFF) << 48 | (hash[7] & 0xFF) << 56;
  }

  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        throw new IllegalArgumentException("args[" + i + "] is null");
      }
      try (final FileInputStream in = new FileInputStream(args[i])) {
        byte[] data = Util.readFully(in);
        ClassReader r = new ClassReader(data);
        System.out.println(Util.makeClass(r.getName()) + ": serialVersionUID = " + computeSerialVersionUID(r));
      } catch (FileNotFoundException e) {
        System.err.println("File not found: " + args[i]);
      } catch (IOException e) {
        System.err.println("Error reading file: " + args[i]);
      } catch (InvalidClassFileException e) {
        System.err.println("Invalid class file: " + args[i]);
      }
    }
  }
}
