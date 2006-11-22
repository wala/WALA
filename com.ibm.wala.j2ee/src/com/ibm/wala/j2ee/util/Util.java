// Licensed Materials - Property of IBM
// 5724-D15
// (C) Copyright IBM Corporation 2004. All Rights Reserved. 
// Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                             
// --------------------------------------------------------------------------- 

package com.ibm.wala.j2ee.util;

import java.util.Set;

import com.ibm.wala.analysis.typeInference.ReceiverTypeInferenceCache;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroContainerCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroOneContainerCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.rta.BasicRTABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.j2ee.BeanMetaData;
import com.ibm.wala.j2ee.CommandInterpreter;
import com.ibm.wala.j2ee.DeploymentMetaData;
import com.ibm.wala.j2ee.J2EEClassTargetSelector;
import com.ibm.wala.j2ee.J2EEContextSelector;
import com.ibm.wala.j2ee.J2EEMethodTargetSelector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author sfink
 */
public class Util {

  /**
   * @return an RTA Call Graph builder.
   * 
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param dmd
   *          deployment descriptor abstraction
   * @param warnings
   *          an object which tracks analysis warnings
   */
  public static CallGraphBuilder makeRTABuilder(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl, AnalysisScope scope,
      DeploymentMetaData dmd, WarningSet warnings) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    addDefaultJ2EEBypassLogic(options, scope, cl, cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    if (dmd != null) {
      ReceiverTypeInferenceCache typeInference = new ReceiverTypeInferenceCache(cha, options, warnings);
      addJ2EEBypassLogic(options, scope, dmd, cha, typeInference, warnings);
      appSelector = new J2EEContextSelector(typeInference, warnings);
      appInterpreter = new CommandInterpreter(cha, warnings);
    }

    return new BasicRTABuilder(cha, warnings, options, appSelector, appInterpreter);
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param dmd
   *          deployment descriptor abstraction
   * @param warnings
   *          an object which tracks analysis warnings
   * @return a 0-CFA Call Graph Builder.
   */
  public static CFABuilder makeZeroCFABuilder(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl, AnalysisScope scope,
      DeploymentMetaData dmd, WarningSet warnings) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    addDefaultJ2EEBypassLogic(options, scope, cl, cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    if (dmd != null) {
      ReceiverTypeInferenceCache typeInference = new ReceiverTypeInferenceCache(cha, options, warnings);
      addJ2EEBypassLogic(options, scope, dmd, cha, typeInference, warnings);
      appSelector = new J2EEContextSelector(typeInference, warnings);
      appInterpreter = new CommandInterpreter(cha, warnings);
    }

    return new ZeroXCFABuilder(cha, warnings, options, appSelector, appInterpreter, options.getReflectionSpec(),
        ZeroXInstanceKeys.NONE);
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param dmd
   *          deployment descriptor abstraction
   * @param warnings
   *          an object which tracks analysis warnings
   * @return a 1-CFA Call Graph Builder.
   */
  public static CallGraphBuilder makeOneCFABuilder(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, DeploymentMetaData dmd, WarningSet warnings) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    addDefaultJ2EEBypassLogic(options, scope, cl, cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    if (dmd != null) {
      ReceiverTypeInferenceCache typeInference = new ReceiverTypeInferenceCache(cha, options, warnings);
      addJ2EEBypassLogic(options, scope, dmd, cha, typeInference, warnings);
      appSelector = new J2EEContextSelector(typeInference, warnings);
      appInterpreter = new CommandInterpreter(cha, warnings);
    }

    CallGraphBuilder builder = new OneCFABuilder(cha, warnings, options, appSelector, appInterpreter, options.getReflectionSpec());
    return builder;
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param dmd
   *          deployment descriptor abstraction
   * @param warnings
   *          an object which tracks analysis warnings
   * @return a 0-1-CFA Call Graph Builder.
   * 
   * This version uses the DEDUCED_PLUS_STRINGSTUFF policy to avoid
   * disambiguating uninteresting types.
   */
  public static CFABuilder makeZeroOneCFABuilder(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl, AnalysisScope scope,
      DeploymentMetaData dmd, WarningSet warnings) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    addDefaultJ2EEBypassLogic(options, scope, cl, cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    if (dmd != null) {
      ReceiverTypeInferenceCache typeInference = new ReceiverTypeInferenceCache(cha, options, warnings);
      addJ2EEBypassLogic(options, scope, dmd, cha, typeInference, warnings);
      appSelector = new J2EEContextSelector(typeInference, warnings);
      appInterpreter = new CommandInterpreter(cha, warnings);
    }

    return new ZeroXCFABuilder(cha, warnings, options, appSelector, appInterpreter, options.getReflectionSpec(),
        ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_MANY | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
            | ZeroXInstanceKeys.SMUSH_STRINGS | ZeroXInstanceKeys.SMUSH_THROWABLES);
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param dmd
   *          deployment descriptor abstraction
   * @param warnings
   *          an object which tracks analysis warnings
   * @return a 0-1-CFA Call Graph Builder.
   * 
   * This version uses the ALL policy to disambiguate all allocation sites
   */
  public static CFABuilder makeZeroOneUnoptCFABuilder(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, DeploymentMetaData dmd, WarningSet warnings) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    addDefaultJ2EEBypassLogic(options, scope, cl, cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    if (dmd != null) {
      ReceiverTypeInferenceCache typeInference = new ReceiverTypeInferenceCache(cha, options, warnings);
      addJ2EEBypassLogic(options, scope, dmd, cha, typeInference, warnings);
      appSelector = new J2EEContextSelector(typeInference, warnings);
      appInterpreter = new CommandInterpreter(cha, warnings);
    }

    return new ZeroXCFABuilder(cha, warnings, options, appSelector, appInterpreter, options.getReflectionSpec(),
        ZeroXInstanceKeys.ALLOCATIONS);
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param dmd
   *          deployment descriptor abstraction
   * @param warnings
   *          an object which tracks analysis warnings
   * @return a 0-CFA Call Graph Builder augmented with extra logic for
   *         containers
   */
  public static CFABuilder makeZeroContainerCFABuilder(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, DeploymentMetaData dmd, WarningSet warnings) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    addDefaultJ2EEBypassLogic(options, scope, cl, cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    if (dmd != null) {
      ReceiverTypeInferenceCache typeInference = new ReceiverTypeInferenceCache(cha, options, warnings);
      addJ2EEBypassLogic(options, scope, dmd, cha, typeInference, warnings);
      appSelector = new J2EEContextSelector(typeInference, warnings);
      appInterpreter = new CommandInterpreter(cha, warnings);
    }

    return new ZeroContainerCFABuilder(cha, warnings, options, appSelector, appInterpreter, options.getReflectionSpec());
  }

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param cl
   *          classloader that can find WALA resources
   * @param scope
   *          representation of the analysis scope
   * @param dmd
   *          deployment descriptor abstraction
   * @param warnings
   *          an object which tracks analysis warnings
   * @return a 0-1-CFA Call Graph Builder augmented with extra logic for
   *         containers
   */
  public static CFABuilder makeZeroOneContainerCFABuilder(AnalysisOptions options, ClassHierarchy cha, ClassLoader cl,
      AnalysisScope scope, DeploymentMetaData dmd, WarningSet warnings) {

    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha, warnings);
    addDefaultJ2EEBypassLogic(options, scope, cl, cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    if (dmd != null) {
      ReceiverTypeInferenceCache typeInference = new ReceiverTypeInferenceCache(cha, options, warnings);
      addJ2EEBypassLogic(options, scope, dmd, cha, typeInference, warnings);
      appSelector = new J2EEContextSelector(typeInference, warnings);
      appInterpreter = new CommandInterpreter(cha, warnings);
    }

    return new ZeroOneContainerCFABuilder(cha, warnings, options, appSelector, appInterpreter, options.getReflectionSpec());
  }

  public static void addJ2EEBypassLogic(AnalysisOptions options, AnalysisScope scope, DeploymentMetaData dmd, ClassHierarchy cha,
      ReceiverTypeInferenceCache typeInference, WarningSet warn) {

    MethodTargetSelector ms = new J2EEMethodTargetSelector(scope, options.getMethodTargetSelector(), dmd, cha, typeInference, warn);
    options.setSelector(ms);

    ClassTargetSelector cs = new J2EEClassTargetSelector(options.getClassTargetSelector(), dmd, cha, cha.getLoader(scope
        .getLoader(Atom.findOrCreateUnicodeAtom("Synthetic"))));
    options.setSelector(cs);
  }

  /**
   * @param bean
   * @param cha
   *          governing class hierarchy
   * @return the Set of CMR fields for this bean, including inherited CMRs
   */
  public static Set<Object> getCMRFields(BeanMetaData bean, DeploymentMetaData dmd, ClassHierarchy cha) {
    Set<Object> result = HashSetFactory.make(5);
    TypeReference T = bean.getEJBClass();
    while (T != null) {
      BeanMetaData B = dmd.getBeanMetaData(T);
      if (B != null) {
        result.addAll(B.getCMRFields());
      }
      IClass klass = cha.lookupClass(T);
      if (Assertions.verifyAssertions) {
        Assertions._assert(klass != null);
      }
      try {
        IClass superKlass = klass.getSuperclass();
        T = (superKlass == null) ? null : superKlass.getReference();
      } catch (ClassHierarchyException e) {
        Assertions.UNREACHABLE();
      }
    }
    return result;
  }

  private static final String benignExtSpec = "benignext.xml";

  public static void addDefaultJ2EEBypassLogic(AnalysisOptions options, AnalysisScope scope, ClassLoader cl, ClassHierarchy cha) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultBypassLogic(options, scope, cl, cha);
    com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(options, scope, cl, benignExtSpec, cha);
  }

}
