/******************************************************************************
 * Copyright (c) 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.TemporaryFile;
import com.ibm.wala.util.warnings.Warning;

/**
 * abstract class loader that performs CAst and IR generation for relevant
 * entities in a list of {@link Module}s. Subclasses provide the CAst / IR
 * translators appropriate for the language.
 * 
 */
public abstract class CAstAbstractModuleLoader extends CAstAbstractLoader {

  private static final boolean DEBUG = false;

  public CAstAbstractModuleLoader(IClassHierarchy cha, IClassLoader parent) {
    super(cha, parent);
  }

  public CAstAbstractModuleLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  /**
   * create the appropriate CAst translator for the language and source module
   */
  protected abstract TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry M) throws IOException;

  /**
   * should IR be generated for entity?
   */
  protected abstract boolean shouldTranslate(CAstEntity entity);

  /**
   * create the appropriate IR translator for the language
   */
  protected abstract TranslatorToIR initTranslator();

  protected File getLocalFile(SourceModule M) throws IOException {
    if (M instanceof SourceFileModule) {
      return ((SourceFileModule) M).getFile();
    } else {
      File f = File.createTempFile("module", ".txt");
      f.deleteOnExit();
      TemporaryFile.streamToFile(f, M.getInputStream());
      return f;
    }
  }

  /**
   * subclasses should override to perform actions after CAst and IR have been
   * generated. by default, do nothing
   */
  protected void finishTranslation() {

  }

  @Override
  public void init(final List<Module> modules) {

    final CAst ast = new CAstImpl();

    // convert everything to CAst
    final Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities = new LinkedHashSet<>();
    for (Module module : modules) {
      translateModuleToCAst(module, ast, topLevelEntities);
    }

    // generate IR as needed
    final TranslatorToIR xlatorToIR = initTranslator();

    for (Pair<CAstEntity, ModuleEntry> p : topLevelEntities) {
      if (shouldTranslate(p.fst)) {
        xlatorToIR.translate(p.fst, p.snd);
      }
    }

    if (DEBUG) {
      for (TypeName tn : types.keySet()) {
        try {
          System.err.println(("found type " + tn + " : " + types.get(tn) + " < " + types.get(tn).getSuperclass()));
        } catch (Exception e) {
          System.err.println(e);
        }
      }
    }

    finishTranslation();
  }

  /**
   * translate moduleEntry to CAst and store result in topLevelEntities
   * 
   * @param ast
   * @param topLevelEntities
   */
  private void translateModuleEntryToCAst(ModuleEntry moduleEntry, CAst ast, Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
    try {
      if (moduleEntry.isModuleFile()) {
        // nested module
        translateModuleToCAst(moduleEntry.asModule(), ast, topLevelEntities);
      } else {
        TranslatorToCAst xlatorToCAst = getTranslatorToCAst(ast, moduleEntry);

        CAstEntity fileEntity = null;
        try {
          fileEntity = xlatorToCAst.translateToCAst();
        
          if (DEBUG) {
            CAstPrinter.printTo(fileEntity, new PrintWriter(System.err));
          }
          topLevelEntities.add(Pair.make(fileEntity, moduleEntry));

        } catch (TranslatorToCAst.Error e) {
          addMessage(moduleEntry, e.warning);
        }
      }
    } catch (final IOException e) {
      addMessage(moduleEntry, new Warning(Warning.SEVERE) {
        @Override
        public String getMsg() {
          return "I/O issue: " + e.getMessage();
        }
      });
    } catch (final RuntimeException e) {
      final ByteArrayOutputStream s = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(s);
      e.printStackTrace(ps);
      addMessage(moduleEntry, new Warning(Warning.SEVERE) {
        @Override
        public String getMsg() {
          return "Parsing issue: " + new String(s.toByteArray());
        }
      });
    }
  }

  /**
   * translate all relevant entities in the module to CAst, storing the results
   * in topLevelEntities
   */
  private void translateModuleToCAst(Module module, CAst ast, Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
    for (ModuleEntry me : Iterator2Iterable.make(module.getEntries())) {
      translateModuleEntryToCAst(me, ast, topLevelEntities);
    }
  }

}
