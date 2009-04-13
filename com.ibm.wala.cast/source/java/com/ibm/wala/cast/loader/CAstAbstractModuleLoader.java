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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

public abstract class CAstAbstractModuleLoader extends CAstAbstractLoader {

  public CAstAbstractModuleLoader(IClassHierarchy cha, IClassLoader parent) {
    super(cha, parent);
  }

  public CAstAbstractModuleLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  protected abstract TranslatorToCAst getTranslatorToCAst(CAst ast, URL sourceURL, String localFileName);

  protected abstract boolean shouldTranslate(CAstEntity entity);

  protected abstract TranslatorToIR initTranslator();

  protected void finishTranslation() {

  }

  public void init(final List<Module> modules) {
    final CAst ast = new CAstImpl();

    final Set<Pair> topLevelEntities = new LinkedHashSet<Pair>();

    final TranslatorToIR xlatorToIR = initTranslator();

    class TranslatorNestingHack {

      private void init(ModuleEntry moduleEntry) {
        if (moduleEntry.isModuleFile()) {
          init(moduleEntry.asModule());
        } else if (moduleEntry instanceof SourceFileModule) {
          File f = ((SourceFileModule) moduleEntry).getFile();
          String fn = f.toString();

          try {
            TranslatorToCAst xlatorToCAst = getTranslatorToCAst(ast, new URL("file://" + f), fn);

            CAstEntity fileEntity = xlatorToCAst.translateToCAst();

            if (fileEntity != null) {
              CAstPrinter.printTo(fileEntity, new PrintWriter(System.err));

              topLevelEntities.add(Pair.make(fileEntity, fn));
            }
          } catch (MalformedURLException e) {
            System.err.println(("unpected problems with " + f));
            e.printStackTrace(System.err);
            Assertions.UNREACHABLE();
          } catch (RuntimeException e) {
            System.err.println(("unpected problems with " + f));
            e.printStackTrace(System.err);
          }

        } else if (moduleEntry instanceof SourceURLModule) {
          java.net.URL url = ((SourceURLModule) moduleEntry).getURL();
          String fileName = ((SourceURLModule) moduleEntry).getName();
          String localFileName = fileName.replace('/', '_');

          try {
            File F = TemporaryFile.streamToFile(localFileName, ((SourceURLModule) moduleEntry).getInputStream());

            final TranslatorToCAst xlatorToCAst = getTranslatorToCAst(ast, url, localFileName);

            CAstEntity fileEntity = xlatorToCAst.translateToCAst();

            if (fileEntity != null) {
              System.err.println(CAstPrinter.print(fileEntity));

              topLevelEntities.add(Pair.make(fileEntity, fileName));
            }

            F.delete();
          } catch (IOException e) {
            System.err.println(("unexpected problems with " + fileName));
            e.printStackTrace(System.err);
            Assertions.UNREACHABLE();
          } catch (RuntimeException e) {
            System.err.println(("unexpected problems with " + fileName));
            e.printStackTrace(System.err);
          }
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
        System.err.println(("found type " + tn + " : " + types.get(tn) + " < " + ((IClass) types.get(tn)).getSuperclass()));
      } catch (Exception e) {
        System.err.println(e);
      }
    }

    finishTranslation();
  }

}
