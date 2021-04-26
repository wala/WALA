/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.SetOfClasses;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * A serializable version of {@link AnalysisScope}. Note: any information about the array class
 * loader is lost using this representation.
 */
public class ShallowAnalysisScope implements Serializable {

  /* Serial version */
  private static final long serialVersionUID = -3256390509887654321L;

  private final SetOfClasses exclusions;

  // example for a line: "Primordial,Java,jarFile,primordial.jar.model"
  private final List<String> moduleLinesList;

  // example for a line: "Synthetic, Java, loaderImpl,
  // com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader"
  // may be empty
  private final List<String> ldrImplLinesList;

  public ShallowAnalysisScope(
      SetOfClasses exclusions, List<String> moduleLinesList, List<String> ldrImplLinesList) {
    if (moduleLinesList == null) {
      throw new IllegalArgumentException("null moduleLinesList");
    }
    if (ldrImplLinesList == null) {
      throw new IllegalArgumentException("null ldrImplLinesList");
    }
    this.exclusions = exclusions;
    this.moduleLinesList = moduleLinesList;
    this.ldrImplLinesList = ldrImplLinesList;
  }

  public AnalysisScope toAnalysisScope() throws IOException {
    AnalysisScope analysisScope = AnalysisScope.createJavaAnalysisScope();
    analysisScope.setExclusions(exclusions);

    for (String moduleLine : moduleLinesList) {
      AnalysisScopeReader.instance.processScopeDefLine(
          analysisScope, this.getClass().getClassLoader(), moduleLine);
    }

    for (String ldrLine : ldrImplLinesList) {
      AnalysisScopeReader.instance.processScopeDefLine(
          analysisScope, this.getClass().getClassLoader(), ldrLine);
    }

    return analysisScope;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (String moduleLine : moduleLinesList) {
      result.append(moduleLine);
      result.append('\n');
    }
    return result.toString();
  }
}
