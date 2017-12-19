/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.shrike;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.ref.CacheReference;

/**
 * A soft handle to a Shrike class reader
 * 
 * TODO: implement more effective caching than just soft references TODO: push
 * weakness up the chain the InputStream, etc ... TODO: reduce reliance on
 * reader throughout the analysis packages
 */
public class ShrikeClassReaderHandle {

  private final static boolean DEBUG = false;
  /**
   * The module entry that defines the class file
   */
  private final ModuleEntry entry;

  private Object reader;

  /**
   * The number of times we hydrate the reader
   */
  private int hydrateCount = 0;

  public ShrikeClassReaderHandle(ModuleEntry entry) {
    if (entry == null) {
      throw new IllegalArgumentException("null entry");
    }
    this.entry = entry;
  }

  /**
   * @return an instance of the class reader ... create one if necessary
   * @throws InvalidClassFileException iff Shrike fails to read the class file
   *        correctly.
   */
  public ClassReader get() throws InvalidClassFileException {
    ClassReader result = (ClassReader) CacheReference.get(reader);
    if (result == null) {
      hydrateCount++;
      if (DEBUG) {
        if (hydrateCount > 1) {
          System.err.println(("Hydrate " + entry + " " + hydrateCount));
          try {
            throw new Exception();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      ByteArrayOutputStream S = new ByteArrayOutputStream();
      try {
        InputStream s = entry.getInputStream();
        readBytes(s, S);
        s.close();
      } catch (IOException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      result = new ClassReader(S.toByteArray());
      reader = CacheReference.make(result);
    }
    return result;
  }

  /**
   * Read is into bytes
   * @throws IOException
   */
  private static void readBytes(InputStream is, ByteArrayOutputStream bytes) throws IOException {
    int n = 0;
    byte[] buffer = new byte[1024];
    while (n > -1) {
      n = is.read(buffer, 0, 1024);
      if (n > -1) {
        bytes.write(buffer, 0, n);
      }
    }
  }

  public String getFileName() {
    return entry.getName();
  }

  /**
   * Force the reference to be cleared/collected
   */
  public void clear() {
    reader = null;
  }

  public ModuleEntry getModuleEntry() {
    return entry;
  }}
