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
import com.ibm.wala.cast.java.translator.polyglot.*;
import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.tree.impl.*;
import com.ibm.wala.cast.util.CAstPrinter;

public class Java2IRTranslator {
    private final boolean DEBUG;

    protected final JavaSourceLoaderImpl fLoader;
    protected final TranslatorToCAst fSourceTranslator;
    CAstRewriterFactory<?> castRewriterFactory = null;

    public Java2IRTranslator(TranslatorToCAst sourceTranslator, 
			     JavaSourceLoaderImpl srcLoader)
    {
      this(sourceTranslator, srcLoader, false);
    }

    public Java2IRTranslator(TranslatorToCAst sourceTranslator, 
			     JavaSourceLoaderImpl srcLoader,
			     boolean debug) 
    {
      DEBUG = debug;
      fLoader= srcLoader;
      fSourceTranslator = sourceTranslator;
      
      IRTranslatorExtension ext = fLoader.getTranslatorExtension();
      if (ext != null) {
        castRewriterFactory = ext.getCAstRewriterFactory();
      }
    }

    public void translate(Object ast, String N) {
      CAstEntity ce= fSourceTranslator.translate(ast, N);

      if (DEBUG) {
	PrintWriter printWriter= new PrintWriter(System.out);
	CAstPrinter.printTo(ce, printWriter);
	printWriter.flush();
      }

      if (castRewriterFactory != null) {
        CAst cast = new CAstImpl();
        CAstRewriter<?> rw = castRewriterFactory.createCAstRewriter(cast);
        ce = rw.rewrite(ce);
        if (DEBUG) {
          PrintWriter printWriter= new PrintWriter(System.out);
          CAstPrinter.printTo(ce, printWriter);
          printWriter.flush();
        }
      }
   
      new JavaCAst2IRTranslator(ce, fLoader).translate();
    }
}
