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

public abstract class CAstAbstractModuleLoader extends CAstAbstractLoader {

  private static final boolean DEBUG = false;

  public CAstAbstractModuleLoader(IClassHierarchy cha, IClassLoader parent) {
    super(cha, parent);
  }

  public CAstAbstractModuleLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  protected abstract TranslatorToCAst getTranslatorToCAst(CAst ast, SourceModule M) throws IOException;

  protected abstract boolean shouldTranslate(CAstEntity entity);

  protected abstract TranslatorToIR initTranslator();

  protected File getLocalFile(SourceModule M) throws IOException {
    if (M instanceof SourceFileModule) {
      return ((SourceFileModule)M).getFile();
    } else {
        File f = File.createTempFile("module", ".txt");
        f.deleteOnExit();
        TemporaryFile.streamToFile(f.getAbsolutePath(), M.getInputStream());
        return f;
    }
  }
  
  protected void finishTranslation() {

  }

  public void init(final List<Module> modules) {
    final CAst ast = new CAstImpl();

    final Set<Pair> topLevelEntities = new LinkedHashSet<Pair>();

    final TranslatorToIR xlatorToIR = initTranslator();

    class TranslatorNestingHack {

      private void init(ModuleEntry moduleEntry) {
        try {
          if (moduleEntry.isModuleFile()) {
            init(moduleEntry.asModule());
          } else if (moduleEntry instanceof SourceModule) {
            TranslatorToCAst xlatorToCAst = getTranslatorToCAst(ast, (SourceModule)moduleEntry);

            CAstEntity fileEntity = xlatorToCAst.translateToCAst();

            if (fileEntity != null) {
              if(DEBUG){
                CAstPrinter.printTo(fileEntity, new PrintWriter(System.err));
              }
              topLevelEntities.add(Pair.make(fileEntity, moduleEntry.getName()));
             
            }  else {
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

      private void init(Module module) {
        for (Iterator mes = module.getEntries(); mes.hasNext();) {
          init((ModuleEntry) mes.next());
        }
      }

      private void init() {
        for (Iterator mes = modules.iterator(); mes.hasNext();) {
          init((Module) mes.next());
        }

        for (Iterator tles = topLevelEntities.iterator(); tles.hasNext();) {
          Pair p = (Pair) tles.next();
          if (shouldTranslate((CAstEntity) p.fst)) {
            xlatorToIR.translate((CAstEntity) p.fst, (String) p.snd);
          }
        }
      }
    }

    (new TranslatorNestingHack()).init();

    for (Iterator ts = types.keySet().iterator(); ts.hasNext();) {
      TypeName tn = (TypeName) ts.next();
      try {
        if(DEBUG){
          System.err.println(("found type " + tn + " : " + types.get(tn) + " < " + ((IClass) types.get(tn)).getSuperclass()));
        }
      } catch (Exception e) {
        System.err.println(e);
      }
    }

    finishTranslation();
  }

}
