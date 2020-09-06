package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/** @author genli */
public class nObjBuilder extends ZeroXCFABuilder {

  public nObjBuilder(
      int n,
      IClassHierarchy cha,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      ContextSelector appContextSelector,
      SSAContextInterpreter appContextInterpreter,
      int instancePolicy) {

    super(
        Language.JAVA,
        cha,
        options,
        cache,
        appContextSelector,
        appContextInterpreter,
        instancePolicy);

    ContextSelector def = new DefaultContextSelector(options, cha);

    ContextSelector contextSelector =
        appContextSelector == null ? def : new DelegatingContextSelector(appContextSelector, def);

    ContextSelector nObjContextSelector = new nObjContextSelector(n, contextSelector);

    setContextSelector(nObjContextSelector);
  }
}
