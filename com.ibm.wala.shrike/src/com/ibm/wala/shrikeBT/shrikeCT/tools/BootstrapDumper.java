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

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.invoke.CallSite;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import com.ibm.wala.shrikeBT.Decoder.InvalidBytecodeException;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.InvokeDynamicInstruction;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

public class BootstrapDumper {
  final private PrintWriter w;

  /**
   * Get ready to print a class to the given output stream.
   */
  public BootstrapDumper(PrintWriter w) {
    this.w = w;
  }

  public static void main(String[] args) throws Exception {
    OfflineInstrumenter oi = new OfflineInstrumenter();
    String[] classpathEntries = oi.parseStandardArgs(args);
    
    PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));

    BootstrapDumper p = new BootstrapDumper(w);

    URL[] urls = new URL[ classpathEntries.length-1 ];
    for(int i = 1; i < classpathEntries.length; i++) {
      System.err.println(classpathEntries[i]);
      File f = new File(classpathEntries[i]);
      assert f.exists();
      urls[i-1] = f.toURI().toURL(); 
    }
    try (final URLClassLoader image = URLClassLoader.newInstance(urls, BootstrapDumper.class.getClassLoader().getParent())) {
      System.err.println(image);

      ClassInstrumenter ci;
      oi.beginTraversal();
      while ((ci = oi.nextClass()) != null) {
        try {
          p.doClass(image, ci.getReader());
        } finally {
          w.flush();
        }
      }
    }

    oi.close();
  }

  private void dumpAttributes(Class<?> cl, ClassReader.AttrIterator attrs) throws InvalidClassFileException,
      InvalidBytecodeException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    for (; attrs.isValid(); attrs.advance()) {
      String name = attrs.getName();
      if (name.equals("Code")) {
        CodeReader code = new CodeReader(attrs);

        CTDecoder decoder = new CTDecoder(code);
        decoder.decode();
        IInstruction[] insts = decoder.getInstructions();
        for(IInstruction inst : insts) {
          if (inst instanceof InvokeDynamicInstruction) {
            CallSite target = ((InvokeDynamicInstruction)inst).bootstrap(cl);
            w.println(target.dynamicInvoker());
            w.println(target.getTarget());
            /*
             * only in Java 8.  Uncomment when we mandate Java 8.
            try {
              w.println(MethodHandles.reflectAs(Method.class, target.dynamicInvoker()));
            } catch (Throwable e) {
              System.out.println(e);
            }
            */
          }
        }
      }
    }
  }



  /**
   * Print a class.
   * @throws InvocationTargetException 
   * @throws IllegalAccessException 
   * @throws SecurityException 
   * @throws NoSuchMethodException 
   * @throws ClassNotFoundException 
   * 
   * @throws IllegalArgumentException if cr is null
   * @throws NoSuchFieldException 
   */
  public void doClass(ClassLoader image, final ClassReader cr) throws InvalidClassFileException, InvalidBytecodeException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    if (cr == null) {
      throw new IllegalArgumentException("cr is null");
    }

    ClassReader.AttrIterator attrs = new ClassReader.AttrIterator();
    cr.initClassAttributeIterator(attrs);
    int methodCount = cr.getMethodCount();
    
    for (int i = 0; i < methodCount; i++) {
      cr.initMethodAttributeIterator(i, attrs);
      dumpAttributes(Class.forName(cr.getName().replace('/', '.'), false, image), attrs);
    }
  }
}
