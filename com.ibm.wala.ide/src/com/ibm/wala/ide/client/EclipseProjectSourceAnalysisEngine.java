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
package com.ibm.wala.ide.client;

import java.io.IOException;

import com.ibm.wala.ide.plugin.CorePlugin;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;

/**
 * An {@link EclipseProjectAnalysisEngine} specialized for source code analysis
 */
abstract public class EclipseProjectSourceAnalysisEngine<P, I extends InstanceKey> extends EclipseProjectAnalysisEngine<P, I> {

  public static final String defaultFileExt = "java";

  /**
   * file extension for source files in this Eclipse project
   */
  final String fileExt;

  public EclipseProjectSourceAnalysisEngine(P project) {
    this(project, defaultFileExt);
  }

  public EclipseProjectSourceAnalysisEngine(P project, String fileExt) {
    super(project);
    this.fileExt = fileExt;
    try {
      setExclusionsFile((new EclipseFileProvider()).getFileFromPlugin(CorePlugin.getDefault(), "J2SEClassHierarchyExclusions.txt")
          .getAbsolutePath());
    } catch (IOException e) {
      try {
        setExclusionsFile((new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt", getClass().getClassLoader())
          .getAbsolutePath());
      } catch (IOException f) {
        Assertions.UNREACHABLE("Cannot find exclusions file");
      }
    }
  }

  /**
   * we don't provide a default implementation of this method to avoid
   * introducing a dependence on com.ibm.wala.cast from this project
   */
  @Override
  public abstract IAnalysisCacheView makeDefaultCache();
  
  protected abstract ClassLoaderReference getSourceLoader();

  @Override
  public AnalysisOptions getDefaultOptions(Iterable<Entrypoint> entrypoints) {
    AnalysisOptions options = new AnalysisOptions(getScope(), entrypoints);

    SSAOptions ssaOptions = new SSAOptions();
    ssaOptions.setDefaultValues(SymbolTable::getDefaultValue);

    options.setSSAOptions(ssaOptions);

    return options;
  }
}
