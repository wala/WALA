/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Jonathan Bardin     <astrosus@gmail.com>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.classLoader;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.util.io.TemporaryFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarFile;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

/**
 * A module which is a wrapper around .dex and .apk file.
 *
 * @author barjo
 */
public class DexFileModule implements Module {
  private final File f;
  private final DexFile dexfile;
  private final Collection<ModuleEntry> entries;
  public static final int AUTO_INFER_API_LEVEL = -1;

  public static DexFileModule make(File f) throws IllegalArgumentException, IOException {
    return make(f, AUTO_INFER_API_LEVEL);
  }

  public static DexFileModule make(File f, int apiLevel)
      throws IllegalArgumentException, IOException {
    if (f.getName().endsWith("jar")) {
      try (final JarFile jar = new JarFile(f)) {
        return new DexFileModule(jar);
      }
    } else {
      return new DexFileModule(f, apiLevel);
    }
  }

  private static File tf(JarFile f) throws IOException {
    String name = f.getName();
    if (name.indexOf('/') >= 0) {
      name = name.substring(name.lastIndexOf('/') + 1);
    }
    File tf = Files.createTempFile("name", "_classes.dex").toFile();
    tf.deleteOnExit();
    System.err.println("using " + tf);
    return tf;
  }

  private DexFileModule(JarFile f) throws IllegalArgumentException, IOException {
    this(TemporaryFile.streamToFile(tf(f), f.getInputStream(f.getEntry("classes.dex"))));
  }

  private DexFileModule(File f) throws IllegalArgumentException {
    this(f, AUTO_INFER_API_LEVEL);
  }

  /** @param f the .dex or .apk file */
  private DexFileModule(File f, int apiLevel) throws IllegalArgumentException {
    try {
      this.f = f;
      dexfile =
          DexFileFactory.loadDexFile(
              f, apiLevel == AUTO_INFER_API_LEVEL ? null : Opcodes.forApi(apiLevel));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }

    // create ModuleEntries from ClassDefItem
    entries = new HashSet<>();

    for (ClassDef cdefitems : dexfile.getClasses()) {
      entries.add(new DexModuleEntry(cdefitems, this));
    }
  }

  /**
   * @param f the .oat or .apk file
   * @param entry the name of the .dex file inside the container file
   * @param apiLevel the api level wanted
   */
  public DexFileModule(File f, String entry, int apiLevel) throws IllegalArgumentException {
    try {
      this.f = f;
      dexfile =
          DexFileFactory.loadDexEntry(
                  f,
                  entry,
                  true,
                  apiLevel == AUTO_INFER_API_LEVEL ? null : Opcodes.forApi(apiLevel))
              .getDexFile();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }

    // create ModuleEntries from ClassDefItem
    entries = new HashSet<>();
    for (ClassDef cdefitems : dexfile.getClasses()) {
      entries.add(new DexModuleEntry(cdefitems, this));
    }
  }

  public DexFileModule(File f, String entry) throws IllegalArgumentException {
    this(f, entry, AUTO_INFER_API_LEVEL);
  }

  /** @return The DexFile associated to this module. */
  public DexFile getDexFile() {
    return dexfile;
  }

  /** @return The DexFile associated to this module. */
  public File getFile() {
    return f;
  }

  /*
   * (non-Javadoc)
   *
   * @see com.ibm.wala.classLoader.Module#getEntries()
   */
  @Override
  public Iterator<ModuleEntry> getEntries() {
    return entries.iterator();
  }
}
