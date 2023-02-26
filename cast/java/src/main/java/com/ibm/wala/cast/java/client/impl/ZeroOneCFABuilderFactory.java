package com.ibm.wala.cast.java.client.impl;

import com.ibm.wala.cast.java.ipa.callgraph.AstJavaZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * @author Linghui Luo
 *     <p>A factory to create call graph builders using 0-1-CFA
 */
public class ZeroOneCFABuilderFactory {
  public AstJavaZeroXCFABuilder make(
      AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    Util.addDefaultSelectors(options, cha);
    Util.addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    return new AstJavaZeroXCFABuilder(
        cha,
        options,
        cache,
        null,
        null,
        ZeroXInstanceKeys.ALLOCATIONS
            | ZeroXInstanceKeys.SMUSH_MANY
            | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
            | ZeroXInstanceKeys.SMUSH_STRINGS
            | ZeroXInstanceKeys.SMUSH_THROWABLES);
  }
}
