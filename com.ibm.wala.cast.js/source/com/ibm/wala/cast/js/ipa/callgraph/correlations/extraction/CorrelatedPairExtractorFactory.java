/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import com.ibm.wala.cast.js.ipa.callgraph.correlations.CorrelationFinder;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.CorrelationSummary;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NoKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

public class CorrelatedPairExtractorFactory implements CAstRewriterFactory<NodePos, NoKey> {
  private final Map<IMethod, CorrelationSummary> summaries;

  public CorrelatedPairExtractorFactory(URL entryPoint) throws ClassHierarchyException, IOException {
    this(new CorrelationFinder().findCorrelatedAccesses(entryPoint));
  }
  
  public CorrelatedPairExtractorFactory(Collection<? extends SourceModule> scripts) throws ClassHierarchyException, IOException {
    this(new CorrelationFinder().findCorrelatedAccesses(scripts));
  }
  
  public CorrelatedPairExtractorFactory(SourceModule[] scripts) throws ClassHierarchyException, IOException {
    this(new CorrelationFinder().findCorrelatedAccesses(scripts));
  }
  
  public CorrelatedPairExtractorFactory(Map<IMethod, CorrelationSummary> summaries) {
    this.summaries = summaries;
  }

  public ClosureExtractor createCAstRewriter(CAst ast) {
    ExtractionPolicyFactory policyFactory = new ExtractionPolicyFactory() {
      @Override
      public ExtractionPolicy createPolicy(CAstEntity entity) {
        CorrelatedPairExtractionPolicy policy = CorrelatedPairExtractionPolicy.make(entity, summaries);
        assert policy != null;
        return policy;
      }
    };
    return new ClosureExtractor(ast, policyFactory);
  }
}