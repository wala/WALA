/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Aug 22, 2005
 */
package com.ibm.wala.cast.java.translator;

import java.io.PrintWriter;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.classLoader.ModuleEntry;

public class Java2IRTranslator {
  private final boolean DEBUG;

  protected final JavaSourceLoaderImpl fLoader;

  CAstRewriterFactory<?, ?> castRewriterFactory = null;

  public Java2IRTranslator(JavaSourceLoaderImpl srcLoader) {
    this(srcLoader, false);
  }

  public Java2IRTranslator(JavaSourceLoaderImpl srcLoader, boolean debug) {
    this(srcLoader, null, debug);
  }

  public Java2IRTranslator(JavaSourceLoaderImpl srcLoader,
      CAstRewriterFactory<?, ?> castRewriterFactory) {
    this(srcLoader, castRewriterFactory, false);
  }

  public Java2IRTranslator(JavaSourceLoaderImpl srcLoader,
      CAstRewriterFactory<?, ?> castRewriterFactory, boolean debug) {
    DEBUG = debug;
    fLoader = srcLoader;
    this.castRewriterFactory = castRewriterFactory;
  }

  public void translate(ModuleEntry module, CAstEntity ce) {
    if (DEBUG) {
      PrintWriter printWriter = new PrintWriter(System.out);
      CAstPrinter.printTo(ce, printWriter);
      printWriter.flush();
    }

    if (castRewriterFactory != null) {
      CAst cast = new CAstImpl();
      CAstRewriter<?, ?> rw = castRewriterFactory.createCAstRewriter(cast);
      ce = rw.rewrite(ce);
      if (DEBUG) {
        PrintWriter printWriter = new PrintWriter(System.out);
        CAstPrinter.printTo(ce, printWriter);
        printWriter.flush();
      }
    }

    new JavaCAst2IRTranslator(module, ce, fLoader).translate();
  }
}
