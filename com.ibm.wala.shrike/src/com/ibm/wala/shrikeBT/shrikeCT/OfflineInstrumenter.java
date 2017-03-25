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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.tools.OfflineInstrumenterBase;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * This class provides a convenient way to iterate through a collection of Java classes and instrument their code. This is just a
 * specialization of OfflineInstrumenterBase to use the shrikeCT functionality.
 */
final public class OfflineInstrumenter extends OfflineInstrumenterBase {

  @Override
  protected Object makeClassFromStream(String inputName, BufferedInputStream s) throws IOException {
    byte[] bytes = new byte[s.available()];
    Util.readFully(s, bytes);
    try {
      return new ClassInstrumenter(inputName, bytes, cha);
    } catch (InvalidClassFileException e) {
      throw new IOException("Class is invalid: " + e.getMessage());
    }
  }

  @Override
  protected String getClassName(Object cl) {
    try {
      return ((ClassInstrumenter) cl).getReader().getName().replace('/', '.');
    } catch (InvalidClassFileException e) {
      return null;
    }
  }

  @Override
  protected void writeClassTo(Object cl, Object mods, OutputStream s) throws IOException {
    ClassInstrumenter ci = (ClassInstrumenter) cl;
    ClassWriter cw = (ClassWriter) mods;
    if (cw == null) {
      s.write(ci.getReader().getBytes());
    } else {
      s.write(cw.makeBytes());
    }
  }

  /**
   * Get the next class to be instrumented.
   */
  public ClassInstrumenter nextClass() throws IOException {
    return (ClassInstrumenter) internalNextClass();
  }

  /**
   * Update the original class with some method changes. 'code' should be the result of out.emitClass(). You can add new fields and
   * methods to 'code' (or make other changes) before calling this method.
   */
  public void outputModifiedClass(ClassInstrumenter out, ClassWriter code) throws IllegalStateException, IOException {
    internalOutputModifiedClass(out, out.getInputName(), code);
  }

  /**
   * Update the original class with some method changes. This method calls out.emitClass() for you.
   */
  public void outputModifiedClass(ClassInstrumenter out) throws IllegalArgumentException, IOException {
    if (out == null) {
      throw new IllegalArgumentException();
    }
    try {
      internalOutputModifiedClass(out, out.getInputName(), out.emitClass());
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      throw new IOException("Invalid class file");
    }
  }
}
