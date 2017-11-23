package com.ibm.wala.cast.js.examples.hybrid;

import java.util.Map;

import com.ibm.wala.cast.ipa.callgraph.AstCFAPointerKeys;
import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder.AstPointerAnalysisImpl.AstImplicitPointsToSetVisitor;
import com.ibm.wala.cast.ipa.callgraph.CrossLanguageCallGraph;
import com.ibm.wala.cast.ipa.callgraph.CrossLanguageContextSelector;
import com.ibm.wala.cast.ipa.callgraph.CrossLanguageInstanceKeys;
import com.ibm.wala.cast.ipa.callgraph.CrossLanguageSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph.JSFakeRoot;
import com.ibm.wala.cast.js.ipa.callgraph.JSSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSSSAPropagationCallGraphBuilder.JSConstraintVisitor;
import com.ibm.wala.cast.js.ipa.callgraph.JSSSAPropagationCallGraphBuilder.JSInterestingVisitor;
import com.ibm.wala.cast.js.ipa.callgraph.JSSSAPropagationCallGraphBuilder.JSPointerAnalysisImpl.JSImplicitPointsToSetVisitor;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructorContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructorInstanceKeys;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptFunctionApplyContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptScopeMappingInstanceKeys;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.util.TargetLanguageSelector;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;

public class JavaJavaScriptHybridCallGraphBuilder extends CrossLanguageSSAPropagationCallGraphBuilder {

  public JavaJavaScriptHybridCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
    super(cha, options, cache, new AstCFAPointerKeys());
    globalObject = new GlobalObjectKey(cha.lookupClass(JavaScriptTypes.Root));
    
    SSAContextInterpreter contextInterpreter = makeDefaultContextInterpreters(null, options, cha);
    setContextInterpreter( contextInterpreter );

    ContextSelector def = new DefaultContextSelector(options, cha);
    Map<Atom,ContextSelector> languageSelectors = HashMapFactory.make();
    languageSelectors.put(JavaScriptTypes.jsName, 
      new JavaScriptFunctionApplyContextSelector(new JavaScriptConstructorContextSelector(def)));
    languageSelectors.put(Language.JAVA.getName(), def);
    setContextSelector(new CrossLanguageContextSelector(languageSelectors));
    
    Map<Atom,InstanceKeyFactory> instanceKeys = HashMapFactory.make();
    instanceKeys.put(
      JavaScriptTypes.jsName,
      new JavaScriptScopeMappingInstanceKeys(cha, this, new JavaScriptConstructorInstanceKeys(new ZeroXInstanceKeys(
          options, cha, contextInterpreter, ZeroXInstanceKeys.ALLOCATIONS))));
    instanceKeys.put(
        Language.JAVA.getName(),
        new ZeroXInstanceKeys(options, cha, contextInterpreter, ZeroXInstanceKeys.NONE));
    setInstanceKeys(new CrossLanguageInstanceKeys(instanceKeys));
 }

  private final GlobalObjectKey globalObject;

  @Override
  public GlobalObjectKey getGlobalObject(Atom language) {
    assert language.equals(JavaScriptTypes.jsName);
    return globalObject;
  }

  @Override
  protected TargetLanguageSelector<ConstraintVisitor, CGNode> makeMainVisitorSelector() {
    return (language, construct) -> {
      if (JavaScriptTypes.jsName.equals(language)) {
        return new JSConstraintVisitor(JavaJavaScriptHybridCallGraphBuilder.this, construct);
      } else {
        return new ConstraintVisitor(JavaJavaScriptHybridCallGraphBuilder.this, construct);
      }
    };
  }

  @Override
  protected TargetLanguageSelector<InterestingVisitor, Integer> makeInterestingVisitorSelector() {
    return (language, construct) -> {
      if (JavaScriptTypes.jsName.equals(language)) {
        return new JSInterestingVisitor(construct);
      } else {
        return new InterestingVisitor(construct);
      }
    };
  }

  @Override
  protected TargetLanguageSelector<AstImplicitPointsToSetVisitor, LocalPointerKey> makeImplicitVisitorSelector(
      CrossLanguagePointerAnalysisImpl analysis) {
    return (language, construct) -> {
      if (JavaScriptTypes.jsName.equals(language)) {
        return new JSImplicitPointsToSetVisitor((AstPointerAnalysisImpl) getPointerAnalysis(), construct);
      } else {
        return new AstImplicitPointsToSetVisitor((AstPointerAnalysisImpl) getPointerAnalysis(), construct);
      }
    };
  }

  @Override
  protected TargetLanguageSelector<AbstractRootMethod, CrossLanguageCallGraph> makeRootNodeSelector() {
    return (language, construct) -> {
      if (JavaScriptTypes.jsName.equals(language)) {
        return new JSFakeRoot(getClassHierarchy(), getOptions(), getAnalysisCache());
      } else {
        return new FakeRootMethod(getClassHierarchy(), getOptions(), getAnalysisCache());
      }
    };
  }

  @Override
  protected boolean useObjectCatalog() {
    return true;
  }

  
  @Override
  protected AbstractFieldPointerKey fieldKeyForUnknownWrites(AbstractFieldPointerKey fieldKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected boolean sameMethod(CGNode opNode, String definingMethod) {
    if (JavaScriptLoader.JS.equals(opNode.getMethod().getDeclaringClass().getClassLoader().getLanguage())) {
      return definingMethod.equals(opNode.getMethod().getReference().getDeclaringClass().getName().toString());
    } else {
      return false;
    }
  }

  @Override
  protected void processCallingConstraints(CGNode caller, SSAAbstractInvokeInstruction instruction, CGNode target,
      InstanceKey[][] constParams, PointerKey uniqueCatchKey) {
    if (JavaScriptLoader.JS.equals(caller.getMethod().getDeclaringClass().getClassLoader().getLanguage())) {
      JSSSAPropagationCallGraphBuilder.processCallingConstraintsInternal(this, caller, instruction, target, constParams, uniqueCatchKey);      
    } else {
      super.processCallingConstraints(caller, instruction, target, constParams, uniqueCatchKey);
    }
  }

  
}
