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
package com.ibm.wala.cast.java.translator.polyglot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Globals;
import polyglot.frontend.Source;
import polyglot.main.Options;
import polyglot.main.UsageError;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;

import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.classLoader.DirectoryTreeModule;
import com.ibm.wala.classLoader.FileModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * A SourceModuleTranslator whose implementation of loadAllSources() uses the PolyglotFrontEnd pseudo-compiler to generate DOMO IR
 * for the sources in the compile-time classpath.
 * 
 * @author rfuhrer
 */
public class PolyglotSourceModuleTranslator implements SourceModuleTranslator {
  private final ExtensionInfo fExtInfo;

  private String fClassPath;

  public PolyglotSourceModuleTranslator(AnalysisScope scope, IRTranslatorExtension extInfo, PolyglotSourceLoaderImpl sourceLoader) {
    fExtInfo = (ExtensionInfo) extInfo;
    computeClassPath(scope);
    extInfo.setSourceLoader(sourceLoader);
  }

  private void computeClassPath(AnalysisScope scope) {
    StringBuffer buf = new StringBuffer();

    ClassLoaderReference cl = findInnermostClassLoader(scope);

    while (cl != null) {
      List<Module> modules = scope.getModules(cl);

      for (Iterator<Module> iter = modules.iterator(); iter.hasNext();) {
        Module m = (Module) iter.next();

        if (buf.length() > 0)
          buf.append(File.pathSeparator);
        if (m instanceof JarFileModule) {
          JarFileModule jarFileModule = (JarFileModule) m;

          buf.append(jarFileModule.getAbsolutePath());
        } else if (m instanceof DirectoryTreeModule) {
          DirectoryTreeModule directoryTreeModule = (DirectoryTreeModule) m;

          buf.append(directoryTreeModule.getPath());
        } else if (m instanceof FileModule) {
          // do nothing
        } else
          Assertions.UNREACHABLE("Module entry is neither jar file nor directory: " + m);
      }
      cl = cl.getParent();
    }
    fClassPath = buf.toString();
  }

  private ClassLoaderReference findInnermostClassLoader(AnalysisScope scope) {
    Set<ClassLoaderReference> parentLoaders = new HashSet<ClassLoaderReference>();

    for (ClassLoaderReference loader : scope.getLoaders()) {
      ClassLoaderReference parent = loader.getParent();
      if (parent != null) {
        parentLoaders.add(parent);
      }
    }
    for (ClassLoaderReference child : scope.getLoaders()) {
      if (!parentLoaders.contains(child)) {
        return child;
      }
    }
    throw new IllegalStateException("No innermost class loader???");
  }

  @SuppressWarnings("unchecked")
  public void loadAllSources(Set modules) {
    Options opts = fExtInfo.getOptions();
    opts.assertions = true;
    Options.global = opts;
    try {
      opts.parseCommandLine(new String[] { "-cp", fClassPath }, new HashSet());
    } catch (UsageError e) {
      // Assertions.UNREACHABLE("Error parsing classpath spec???");
    }

    Compiler compiler = new PolyglotFrontEnd(fExtInfo);
    List<StreamSource> streams = new ArrayList<StreamSource>();

    // N.B.: 'modules' is a flat set of source file ModuleEntry's.
    for (Iterator it = modules.iterator(); it.hasNext();) {
      SourceFileModule entry = (SourceFileModule) it.next();

      assert entry.isSourceFile();

      if (skipSourceFile(entry)) {
        continue;
      }

      String filePath = entry.getAbsolutePath();

      try {
        StreamSource srcStream = new StreamSource(entry.getInputStream(), filePath);

        streams.add(srcStream);
      } catch (IOException e) {
        compiler.errorQueue().enqueue(
            new ErrorInfo(ErrorInfo.IO_ERROR, "Unable to open source file '" + entry.getName() + "'", Position.COMPILER_GENERATED));
      }
    }
    compiler.compile(streams);
    // At this point, DOMO now "knows" about all the source-originated stuff
  }

  /**
   * @return true if the given source file module should not be processed, e.g. because it is generated on behalf of some upstream
   *         source.
   */
  protected boolean skipSourceFile(SourceFileModule entry) {
    return false;
  }
}
