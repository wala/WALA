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
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cast.util.TemporaryFile;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Pair;
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
  protected abstract TranslatorToCAst getTranslatorToCAst(CAst ast, SourceModule M) throws IOException;

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
      TemporaryFile.streamToFile(f.getAbsolutePath(), M.getInputStream());
      return f;
    }
  }

  /**
   * subclasses should override to perform actions after CAst and IR have been
   * generated. by default, do nothing
   */
  protected void finishTranslation() {

  }

  public void init(final List<Module> modules) {

    final CAst ast = new CAstImpl();

    // convert everything to CAst
    final Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities = new LinkedHashSet<Pair<CAstEntity, ModuleEntry>>();
    for (Iterator<Module> mes = modules.iterator(); mes.hasNext();) {
      translateModuleToCAst(mes.next(), ast, topLevelEntities);
    }

    // generate IR as needed
    final TranslatorToIR xlatorToIR = initTranslator();

    for (Iterator<Pair<CAstEntity, ModuleEntry>> tles = topLevelEntities.iterator(); tles.hasNext();) {
      Pair<CAstEntity, ModuleEntry> p = tles.next();
      if (shouldTranslate(p.fst)) {
        xlatorToIR.translate(p.fst, p.snd);
      }
    }

    if (DEBUG) {
      for (Iterator ts = types.keySet().iterator(); ts.hasNext();) {
        TypeName tn = (TypeName) ts.next();
        try {
          System.err.println(("found type " + tn + " : " + types.get(tn) + " < " + ((IClass) types.get(tn)).getSuperclass()));
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
      } else if (moduleEntry instanceof SourceModule) {
        TranslatorToCAst xlatorToCAst = getTranslatorToCAst(ast, (SourceModule) moduleEntry);

        CAstEntity fileEntity = xlatorToCAst.translateToCAst();

        if (fileEntity != null) {
          if (DEBUG) {
            CAstPrinter.printTo(fileEntity, new PrintWriter(System.err));
          }
          topLevelEntities.add(Pair.make(fileEntity, moduleEntry));

        } else {
          addMessage(moduleEntry, new Warning(Warning.SEVERE) {
            @Override
            public String getMsg() {
              return "parse error";
            }
          });
        }
      }
    } catch (final MalformedURLException e) {
      addMessage(moduleEntry, new Warning(Warning.SEVERE) {
        @Override
        public String getMsg() {
          return "Malformed URL issue: " + e.getMessage();
        }
      });
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
    for (Iterator<ModuleEntry> mes = module.getEntries(); mes.hasNext();) {
      translateModuleEntryToCAst(mes.next(), ast, topLevelEntities);
    }
  }

}
