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
package com.ibm.wala.cast.java.client.polyglot;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.polyglot.IRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.JavaIRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotClassLoaderFactory;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.ide.plugin.CorePlugin;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ide.util.EclipseProjectPath;
import com.ibm.wala.ide.util.EclipseProjectPath.Loader;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;

/**
 * An {@link EclipseProjectAnalysisEngine} specialized for source code analysis with CAst
 */
public class EclipseProjectSourceAnalysisEngine extends EclipseProjectAnalysisEngine {

  public static final String defaultFileExt = "java";

  /**
   * file extension for source files in this Eclipse project
   */
  final String fileExt;

  public EclipseProjectSourceAnalysisEngine(IJavaProject project) throws IOException, CoreException {
    this(project, defaultFileExt);
  }

  public EclipseProjectSourceAnalysisEngine(IJavaProject project, String fileExt) throws IOException, CoreException {
    super(project);
    this.fileExt = fileExt;
    try {
      setExclusionsFile((new EclipseFileProvider()).getFileFromPlugin(CorePlugin.getDefault(), "J2SEClassHierarchyExclusions.txt")
          .getAbsolutePath());
    } catch (IOException e) {
      Assertions.UNREACHABLE("Cannot find exclusions file");
    }
  }

  @Override
  public AnalysisCache makeDefaultCache() {
    return new AnalysisCache(AstIRFactory.makeDefaultFactory());
  }

  protected AnalysisScope makeSourceAnalysisScope() {
    return new JavaSourceAnalysisScope();
  }

  @Override
  public void buildAnalysisScope() {
    try {
      scope = makeSourceAnalysisScope();
      if (getExclusionsFile() != null) {
        scope.setExclusions(FileOfClasses.createFileOfClasses(new File(getExclusionsFile())));
      }
      EclipseProjectPath epath = getEclipseProjectPath();

      for (Module m : epath.getModules(Loader.PRIMORDIAL, true)) {
        scope.addToScope(scope.getPrimordialLoader(), m);
      }
      ClassLoaderReference app = scope.getApplicationLoader();
      ClassLoaderReference src = ((JavaSourceAnalysisScope) scope).getSourceLoader();
      for (Module m : epath.getModules(Loader.APPLICATION, true)) {
        if (m instanceof SourceDirectoryTreeModule) {
          scope.addToScope(src, m);
        } else {
          scope.addToScope(app, m);
        }
      }
      for (Module m : epath.getModules(Loader.EXTENSION, true)) {
        if (!(m instanceof BinaryDirectoryTreeModule))
          scope.addToScope(app, m);
      }
      /*
       * ClassLoaderReference src = ((JavaSourceAnalysisScope)scope).getSourceLoader(); for (Module m :
       * epath.getModules(Loader.APPLICATION, false)) { scope.addToScope(src, m); }
       */

    } catch (IOException e) {
      Assertions.UNREACHABLE(e.toString());
    }
  }

  public IRTranslatorExtension getTranslatorExtension() {
    return new JavaIRTranslatorExtension();
  }

  protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions, IRTranslatorExtension extInfo) {
    return new PolyglotClassLoaderFactory(exclusions, extInfo);
  }

  @Override
  public IClassHierarchy buildClassHierarchy() {
    IClassHierarchy cha = null;
    ClassLoaderFactory factory = getClassLoaderFactory(scope.getExclusions(), getTranslatorExtension());

    try {
      cha = ClassHierarchy.make(getScope(), factory);
    } catch (ClassHierarchyException e) {
      System.err.println("Class Hierarchy construction failed");
      System.err.println(e.toString());
      e.printStackTrace();
    }
    return cha;
  }

  @Override
  protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
    return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha);
  }

  @Override
  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> entrypoints) {
    AnalysisOptions options = new AnalysisOptions(getScope(), entrypoints);

    SSAOptions ssaOptions = new SSAOptions();
    ssaOptions.setDefaultValues(new SSAOptions.DefaultValues() {
      public int getDefaultValue(SymbolTable symtab, int valueNumber) {
        return symtab.getDefaultValue(valueNumber);
      }
    });

    options.setSSAOptions(ssaOptions);

    return options;
  }
}
