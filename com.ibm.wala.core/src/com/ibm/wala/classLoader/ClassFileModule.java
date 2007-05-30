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

package com.ibm.wala.classLoader;

import java.io.File;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.ShrikeClassReaderHandle;
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * A module which is a wrapper around a .class file
 * 
 * @author sfink
 */
public class ClassFileModule extends FileModule implements Module, ModuleEntry {

  private final String className;

  public ClassFileModule(File f) {
    super(f);
    // this is delicate: TODO, clean it up a bit.
    ShrikeClassReaderHandle reader = new ShrikeClassReaderHandle(this);
    ImmutableByteArray name = null;
    try {
      name = ImmutableByteArray.make(reader.get().getName());
    } catch (InvalidClassFileException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    className = name.toString();
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ClassFileModule:" + getFile();
  }


  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#isClassFile()
   */
  public boolean isClassFile() {
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#getClassName()
   */
  public String getClassName() {
    return className;
  }


  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#isSourceFile()
   */
  public boolean isSourceFile() {
    return false;
  }


}
