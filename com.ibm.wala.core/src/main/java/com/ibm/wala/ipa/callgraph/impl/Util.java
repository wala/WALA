/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXContainerCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nObjBuilder;
import com.ibm.wala.ipa.callgraph.propagation.rta.BasicRTABuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.LambdaMethodTargetSelector;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Call graph utilities */
public class Util {
  /** TODO: Make these properties? */
  public static String nativeSpec = "natives.xml";

  /* BEGIN Custom change: change native spec */
  public static void setNativeSpec(String xmlFile) {
    nativeSpec = xmlFile;
  }

  public static String getNativeSpec() {
    return nativeSpec;
  }
  /* END Custom change: change native spec */
  /**
   * Set up an AnalysisOptions object with default selectors, corresponding to class hierarchy
   * lookup
   *
   * @throws IllegalArgumentException if options is null
   */
  public static void addDefaultSelectors(AnalysisOptions options, IClassHierarchy cha) {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    options.setSelector(
        new LambdaMethodTargetSelector(new ClassHierarchyMethodTargetSelector(cha)));
    options.setSelector(new ClassHierarchyClassTargetSelector(cha));
  }

  /**
   * Modify an options object to include bypass logic as specified by a an XML file.
   *
   * @throws IllegalArgumentException if scope is null
   * @throws IllegalArgumentException if cl is null
   * @throws IllegalArgumentException if options is null
   * @throws IllegalArgumentException if scope is null
   * @deprecated This method is being replaced. Please
   *     <p>Use {@link Util#addBypassLogic(AnalysisOptions, ClassLoader, String, IClassHierarchy)}
   *     instead
   */
  @Deprecated
  public static void addBypassLogic(
      AnalysisOptions options,
      @SuppressWarnings("unused") AnalysisScope scope,
      ClassLoader cl,
      String xmlFile,
      IClassHierarchy cha)
      throws IllegalArgumentException {
    addBypassLogic(options, cl, xmlFile, cha);
  }

  /**
   * Modify an options object to include bypass logic as specified by a an XML file.
   *
   * @throws IllegalArgumentException if scope is null
   * @throws IllegalArgumentException if cl is null
   * @throws IllegalArgumentException if options is null
   * @throws IllegalArgumentException if scope is null
   */
  public static void addBypassLogic(
      AnalysisOptions options, ClassLoader cl, String xmlFile, IClassHierarchy cha)
      throws IllegalArgumentException {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    if (cl == null) {
      throw new IllegalArgumentException("cl is null");
    }
    if (cha == null) {
      throw new IllegalArgumentException("cha cannot be null");
    }

    try (final InputStream s = cl.getResourceAsStream(xmlFile)) {
      XMLMethodSummaryReader summary = new XMLMethodSummaryReader(s, cha.getScope());
      addBypassLogic(options, cl, summary, cha);
    } catch (IOException e) {
      System.err.println("Could not close XML method summary reader: " + e.getLocalizedMessage());
      e.printStackTrace();
    }
  }

  /**
   * @deprecated Method will be replaced. Please
   *     <p>Use {@link Util#addBypassLogic(AnalysisOptions, ClassLoader, XMLMethodSummaryReader,
   *     IClassHierarchy)} instead
   */
  @Deprecated
  public static void addBypassLogic(
      AnalysisOptions options,
      @SuppressWarnings("unused") AnalysisScope scope,
      ClassLoader cl,
      XMLMethodSummaryReader summary,
      IClassHierarchy cha)
      throws IllegalArgumentException {
    addBypassLogic(options, cl, summary, cha);
  }

  public static void addBypassLogic(
      AnalysisOptions options, ClassLoader cl, XMLMethodSummaryReader summary, IClassHierarchy cha)
      throws IllegalArgumentException {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    if (cl == null) {
      throw new IllegalArgumentException("cl is null");
    }
    if (cha == null) {
      throw new IllegalArgumentException("cha cannot be null");
    }

    MethodTargetSelector ms =
        new BypassMethodTargetSelector(
            options.getMethodTargetSelector(),
            summary.getSummaries(),
            summary.getIgnoredPackages(),
            cha);
    options.setSelector(ms);

    ClassTargetSelector cs =
        new BypassClassTargetSelector(
            options.getClassTargetSelector(),
            summary.getAllocatableClasses(),
            cha,
            cha.getLoader(cha.getScope().getLoader(Atom.findOrCreateUnicodeAtom("Synthetic"))));
    options.setSelector(cs);
  }

  /**
   * @return set of all eligible Main classes in the class hierarchy
   * @throws IllegalArgumentException if scope is null
   * @deprecated
   *     <p>Use {@link Util#makeMainEntrypoints(IClassHierarchy)} instead
   */
  @Deprecated
  public static Iterable<Entrypoint> makeMainEntrypoints(
      @SuppressWarnings("unused") AnalysisScope scope, IClassHierarchy cha) {

    return makeMainEntrypoints(cha);
  }

  /**
   * @return set of all eligible Main classes in the class hierarchy
   * @throws IllegalArgumentException if scope is null
   */
  public static Iterable<Entrypoint> makeMainEntrypoints(IClassHierarchy cha) {
    return makeMainEntrypoints(cha.getScope().getApplicationLoader(), cha);
  }

  public static Iterable<Entrypoint> makeMainEntrypoints(
      ClassLoaderReference clr, IClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    final Atom mainMethod = Atom.findOrCreateAsciiAtom("main");
    final HashSet<Entrypoint> result = HashSetFactory.make();
    for (IClass klass : cha) {
      if (klass.getClassLoader().getReference().equals(clr)) {
        MethodReference mainRef =
            MethodReference.findOrCreate(
                klass.getReference(),
                mainMethod,
                Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V"));
        IMethod m = klass.getMethod(mainRef.getSelector());
        if (m != null) {
          result.add(new DefaultEntrypoint(m, cha));
        }
      }
    }
    return result::iterator;
  }

  /**
   * @deprecated
   *     <p>Use {@link Util#makeMainEntrypoints(IClassHierarchy, String)}
   */
  @Deprecated
  public static Iterable<Entrypoint> makeMainEntrypoints(
      @SuppressWarnings("unused") AnalysisScope scope,
      final IClassHierarchy cha,
      String className) {
    return makeMainEntrypoints(scope, cha, new String[] {className});
  }

  /** @return Entrypoints for a set of J2SE Main classes */
  public static Iterable<Entrypoint> makeMainEntrypoints(
      final IClassHierarchy cha, String className) {
    return makeMainEntrypoints(cha, new String[] {className});
  }

  /**
   * @deprecated Please
   *     <p>Use {@link Util#makeMainEntrypoints(IClassHierarchy, String[])}
   */
  @Deprecated
  public static Iterable<Entrypoint> makeMainEntrypoints(
      @SuppressWarnings("unused") final AnalysisScope scope,
      final IClassHierarchy cha,
      final String[] classNames) {
    return makeMainEntrypoints(cha, classNames);
  }

  /** @return Entrypoints for a set of J2SE Main classes */
  public static Iterable<Entrypoint> makeMainEntrypoints(
      final IClassHierarchy cha, final String[] classNames) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    return makeMainEntrypoints(cha.getScope().getApplicationLoader(), cha, classNames);
  }

  /**
   * @return Entrypoints for a set of J2SE Main classes
   * @throws IllegalArgumentException if classNames == null
   * @throws IllegalArgumentException if (classNames != null) and (0 &lt; classNames.length) and
   *     (classNames[0] == null)
   * @throws IllegalArgumentException if classNames.length == 0
   */
  public static Iterable<Entrypoint> makeMainEntrypoints(
      final ClassLoaderReference loaderRef, final IClassHierarchy cha, final String[] classNames)
      throws IllegalArgumentException {

    if (classNames == null) {
      throw new IllegalArgumentException("classNames == null");
    }
    if (classNames.length == 0) {
      throw new IllegalArgumentException("classNames.length == 0");
    }
    if (classNames[0] == null && 0 < classNames.length) {
      throw new IllegalArgumentException("(0 < classNames.length) and (classNames[0] == null)");
    }

    for (String className : classNames) {
      if (className.indexOf('L') != 0) {
        throw new IllegalArgumentException("Expected class name to start with L " + className);
      }
      if (className.indexOf('.') > 0) {
        Assertions.productionAssertion(
            false, "Expected class name formatted with /, not . " + className);
      }
    }

    return () -> {
      final Atom mainMethod = Atom.findOrCreateAsciiAtom("main");
      return new Iterator<Entrypoint>() {
        private int index = 0;

        @Override
        public void remove() {
          Assertions.UNREACHABLE();
        }

        @Override
        public boolean hasNext() {
          return index < classNames.length;
        }

        @Override
        public Entrypoint next() {
          TypeReference T =
              TypeReference.findOrCreate(loaderRef, TypeName.string2TypeName(classNames[index++]));
          MethodReference mainRef =
              MethodReference.findOrCreate(
                  T, mainMethod, Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V"));
          return new DefaultEntrypoint(mainRef, cha);
        }
      };
    };
  }

  /** create a set holding the contents of an {@link Iterator} */
  public static <T> Set<T> setify(Iterator<? extends T> x) {
    if (x == null) {
      throw new IllegalArgumentException("Null x");
    }
    Set<T> y = HashSetFactory.make();
    while (x.hasNext()) {
      y.add(x.next());
    }
    return y;
  }

  /**
   * @throws IllegalArgumentException if subG is null
   * @throws IllegalArgumentException if supG is null
   */
  public static <T> void checkGraphSubset(Graph<T> supG, Graph<T> subG) {
    if (supG == null) {
      throw new IllegalArgumentException("supG is null");
    }
    if (subG == null) {
      throw new IllegalArgumentException("subG is null");
    }
    Set<T> nodeDiff = setify(subG.iterator());
    nodeDiff.removeAll(setify(supG.iterator()));
    if (!nodeDiff.isEmpty()) {
      System.err.println("supergraph: ");
      System.err.println(supG);
      System.err.println("subgraph: ");
      System.err.println(subG);
      System.err.println("nodeDiff: ");
      for (T t : nodeDiff) {
        System.err.println(t.toString());
      }
      Assertions.productionAssertion(nodeDiff.isEmpty(), "bad superset, see tracefile\n");
    }

    for (T m : subG) {
      Set<T> succDiff = setify(subG.getSuccNodes(m));
      succDiff.removeAll(setify(supG.getSuccNodes(m)));
      if (!succDiff.isEmpty()) {
        Assertions.productionAssertion(
            succDiff.isEmpty(), "bad superset for successors of " + m + ':' + succDiff);
      }

      Set<T> predDiff = setify(subG.getPredNodes(m));
      predDiff.removeAll(setify(supG.getPredNodes(m)));
      if (!predDiff.isEmpty()) {
        System.err.println("supergraph: ");
        System.err.println(supG);
        System.err.println("subgraph: ");
        System.err.println(subG);
        System.err.println("predDiff: ");
        for (T t : predDiff) {
          System.err.println(t.toString());
        }
        Assertions.UNREACHABLE("bad superset for predecessors of " + m + ':' + predDiff);
      }
    }
  }

  /**
   * @return an RTA Call Graph builder.
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @deprecated
   *     <p>Use {@link Util#makeRTABuilder(AnalysisOptions, IAnalysisCacheView, IClassHierarchy)}
   */
  @Deprecated
  public static CallGraphBuilder<InstanceKey> makeRTABuilder(
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {

    return makeRTABuilder(options, cache, cha);
  }

  /**
   * @return an RTA Call Graph builder.
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   */
  public static CallGraphBuilder<InstanceKey> makeRTABuilder(
      AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {

    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, cha.getScope(), Util.class.getClassLoader(), cha);

    return new BasicRTABuilder(cha, options, cache, null, null);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @return a 0-CFA Call Graph Builder.
   * @deprecated
   *     <p>Use {@link Util#makeZeroCFABuilder(Language, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeZeroCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeZeroCFABuilder(l, options, cache, cha);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @return a 0-CFA Call Graph Builder.
   */
  public static SSAPropagationCallGraphBuilder makeZeroCFABuilder(
      Language l, AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    return makeZeroCFABuilder(l, options, cache, cha, null, null);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @param customSelector user-defined context selector, or null if none
   * @param customInterpreter user-defined context interpreter, or null if none
   * @return a 0-CFA Call Graph Builder.
   * @throws IllegalArgumentException if options is null
   * @deprecated
   *     <p>Use {@link Util#makeZeroCFABuilder(Language, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy, ContextSelector, SSAContextInterpreter)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeZeroCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope,
      ContextSelector customSelector,
      SSAContextInterpreter customInterpreter) {

    return makeZeroCFABuilder(l, options, cache, cha, customSelector, customInterpreter);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param customSelector user-defined context selector, or null if none
   * @param customInterpreter user-defined context interpreter, or null if none
   * @return a 0-CFA Call Graph Builder.
   * @throws IllegalArgumentException if options is null
   */
  public static SSAPropagationCallGraphBuilder makeZeroCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      ContextSelector customSelector,
      SSAContextInterpreter customInterpreter) {

    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);

    return ZeroXCFABuilder.make(
        l, cha, options, cache, customSelector, customInterpreter, ZeroXInstanceKeys.NONE);
  }

  /**
   * @return a 0-1-CFA Call Graph Builder.
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @deprecated
   *     <p>Use {@link Util#makeZeroOneCFABuilder(Language, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeZeroOneCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeZeroOneCFABuilder(l, options, cache, cha, null, null);
  }

  /**
   * @return a 0-1-CFA Call Graph Builder.
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   */
  public static SSAPropagationCallGraphBuilder makeZeroOneCFABuilder(
      Language l, AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    return makeZeroOneCFABuilder(l, options, cache, cha, null, null);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @param customSelector user-defined context selector, or null if none
   * @param customInterpreter user-defined context interpreter, or null if none
   * @return a 0-1-CFA Call Graph Builder.
   * @throws IllegalArgumentException if options is null
   * @deprecated
   *     <p>Use {@link Util#makeVanillaZeroOneCFABuilder(Language, AnalysisOptions,
   *     IAnalysisCacheView, IClassHierarchy, ContextSelector, SSAContextInterpreter)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeVanillaZeroOneCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView analysisCache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope,
      ContextSelector customSelector,
      SSAContextInterpreter customInterpreter) {

    return makeVanillaZeroOneCFABuilder(
        l, options, analysisCache, cha, customSelector, customInterpreter);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param customSelector user-defined context selector, or null if none
   * @param customInterpreter user-defined context interpreter, or null if none
   * @return a 0-1-CFA Call Graph Builder.
   * @throws IllegalArgumentException if options is null
   */
  public static SSAPropagationCallGraphBuilder makeVanillaZeroOneCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView analysisCache,
      IClassHierarchy cha,
      ContextSelector customSelector,
      SSAContextInterpreter customInterpreter) {

    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);

    return ZeroXCFABuilder.make(
        l,
        cha,
        options,
        analysisCache,
        customSelector,
        customInterpreter,
        ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC);
  }

  /**
   * @return a 0-1-CFA Call Graph Builder.
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @deprecated
   *     <p>use {@link Util#makeVanillaZeroOneCFABuilder(Language, AnalysisOptions,
   *     IAnalysisCacheView, IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeVanillaZeroOneCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView analysisCache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeVanillaZeroOneCFABuilder(l, options, analysisCache, cha, null, null);
  }

  /**
   * @return a 0-1-CFA Call Graph Builder.
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   */
  public static SSAPropagationCallGraphBuilder makeVanillaZeroOneCFABuilder(
      Language l, AnalysisOptions options, IAnalysisCacheView analysisCache, IClassHierarchy cha) {
    return makeVanillaZeroOneCFABuilder(l, options, analysisCache, cha, null, null);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @param customSelector user-defined context selector, or null if none
   * @param customInterpreter user-defined context interpreter, or null if none
   * @return a 0-1-CFA Call Graph Builder.
   * @throws IllegalArgumentException if options is null
   * @deprecated
   *     <p>Use {@link Util#makeZeroOneCFABuilder(Language, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy, ContextSelector, SSAContextInterpreter)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeZeroOneCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope,
      ContextSelector customSelector,
      SSAContextInterpreter customInterpreter) {
    return makeZeroOneCFABuilder(l, options, cache, cha, customSelector, customInterpreter);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param customSelector user-defined context selector, or null if none
   * @param customInterpreter user-defined context interpreter, or null if none
   * @return a 0-1-CFA Call Graph Builder.
   * @throws IllegalArgumentException if options is null
   */
  public static SSAPropagationCallGraphBuilder makeZeroOneCFABuilder(
      Language l,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      ContextSelector customSelector,
      SSAContextInterpreter customInterpreter) {

    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);

    return ZeroXCFABuilder.make(
        l,
        cha,
        options,
        cache,
        customSelector,
        customInterpreter,
        ZeroXInstanceKeys.ALLOCATIONS
            | ZeroXInstanceKeys.SMUSH_MANY
            | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
            | ZeroXInstanceKeys.SMUSH_STRINGS
            | ZeroXInstanceKeys.SMUSH_THROWABLES);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @return a 0-CFA Call Graph Builder augmented with extra logic for containers
   * @throws IllegalArgumentException if options is null
   * @deprecated
   *     <p>Use {@link Util#makeZeroContainerCFABuilder(AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeZeroContainerCFABuilder(
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeZeroContainerCFABuilder(options, cache, cha);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @return a 0-CFA Call Graph Builder augmented with extra logic for containers
   * @throws IllegalArgumentException if options is null
   */
  public static SSAPropagationCallGraphBuilder makeZeroContainerCFABuilder(
      AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {

    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;

    return new ZeroXContainerCFABuilder(
        cha, options, cache, appSelector, appInterpreter, ZeroXInstanceKeys.NONE);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @param scope representation of the analysis scope
   * @return a 0-1-CFA Call Graph Builder augmented with extra logic for containers
   * @throws IllegalArgumentException if options is null
   * @deprecated
   *     <p>Use {@link Util#makeZeroOneCFABuilder(Language, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeZeroOneContainerCFABuilder(
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeZeroOneContainerCFABuilder(options, cache, cha);
  }

  /**
   * @deprecated
   *     <p>Use {@link Util#makeZeroOneContainerCFABuilder(AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy, ContextSelector, SSAContextInterpreter)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeZeroOneContainerCFABuilder(
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope,
      ContextSelector appSelector,
      SSAContextInterpreter appInterpreter) {

    return makeZeroOneContainerCFABuilder(options, cache, cha, appSelector, appInterpreter);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @return a 0-1-CFA Call Graph Builder augmented with extra logic for containers
   * @throws IllegalArgumentException if options is null
   */
  public static SSAPropagationCallGraphBuilder makeZeroOneContainerCFABuilder(
      AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    return makeZeroOneContainerCFABuilder(options, cache, cha, null, null);
  }

  public static SSAPropagationCallGraphBuilder makeZeroOneContainerCFABuilder(
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      ContextSelector appSelector,
      SSAContextInterpreter appInterpreter) {

    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);

    return new ZeroXContainerCFABuilder(
        cha,
        options,
        cache,
        appSelector,
        appInterpreter,
        ZeroXInstanceKeys.ALLOCATIONS
            | ZeroXInstanceKeys.SMUSH_MANY
            | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
            | ZeroXInstanceKeys.SMUSH_STRINGS
            | ZeroXInstanceKeys.SMUSH_THROWABLES);
  }

  /**
   * make a {@link CallGraphBuilder} that uses call-string context sensitivity, with call-string
   * length limited to n, and a context-sensitive allocation-site-based heap abstraction.
   *
   * @deprecated
   *     <p>Use {@link Util#makeNCFABuilder(int, AnalysisOptions,
   *     IAnalysisCacheView,IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeNCFABuilder(
      int n,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeNCFABuilder(n, options, cache, cha);
  }

  /**
   * make a {@link CallGraphBuilder} that uses call-string context sensitivity, with call-string
   * length limited to n, and a context-sensitive allocation-site-based heap abstraction.
   */
  public static SSAPropagationCallGraphBuilder makeNCFABuilder(
      int n, AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    SSAPropagationCallGraphBuilder result =
        new nCFABuilder(
            n,
            Language.JAVA.getFakeRootMethod(cha, options, cache),
            options,
            cache,
            appSelector,
            appInterpreter);
    // nCFABuilder uses type-based heap abstraction by default, but we want allocation sites
    result.setInstanceKeys(
        new ZeroXInstanceKeys(
            options,
            cha,
            result.getContextInterpreter(),
            ZeroXInstanceKeys.ALLOCATIONS
                | ZeroXInstanceKeys.SMUSH_MANY
                | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
                | ZeroXInstanceKeys.SMUSH_STRINGS
                | ZeroXInstanceKeys.SMUSH_THROWABLES));
    return result;
  }

  /**
   * make a {@link CallGraphBuilder} that uses object context sensitivity, with allocation-string
   * length limited to n
   *
   * @deprecated
   *     <p>Use {@link Util#makeNObjBuilder(int, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeNObjBuilder(
      int n,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeNObjBuilder(n, options, cache, cha);
  }

  /**
   * make a {@link CallGraphBuilder} that uses object context sensitivity, with allocation-string
   * length limited to n
   */
  public static SSAPropagationCallGraphBuilder makeNObjBuilder(
      int n, AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    SSAPropagationCallGraphBuilder result =
        new nObjBuilder(
            n,
            cha,
            options,
            cache,
            appSelector,
            appInterpreter,
            ZeroXInstanceKeys.ALLOCATIONS
                | ZeroXInstanceKeys.SMUSH_MANY
                | ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
                | ZeroXInstanceKeys.SMUSH_STRINGS
                | ZeroXInstanceKeys.SMUSH_THROWABLES);
    return result;
  }

  /**
   * make a {@link CallGraphBuilder} that uses object context sensitivity, with allocation-string
   * length limited to n
   *
   * @deprecated
   *     <p>Use {@link Util#makeVanillaNObjBuilder(int, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeVanillaNObjBuilder(
      int n,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeVanillaNCFABuilder(n, options, cache, cha);
  }

  /**
   * make a {@link CallGraphBuilder} that uses object context sensitivity, with allocation-string
   * length limited to n
   */
  public static SSAPropagationCallGraphBuilder makeVanillaNObjBuilder(
      int n, AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    SSAPropagationCallGraphBuilder result =
        new nObjBuilder(
            n, cha, options, cache, appSelector, appInterpreter, ZeroXInstanceKeys.ALLOCATIONS);
    return result;
  }

  /**
   * @deprecated
   *     <p>Use {@link Util#makeVanillaNCFABuilder(int, AnalysisOptions, IAnalysisCacheView,
   *     IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeVanillaNCFABuilder(
      int n,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {
    return makeVanillaNCFABuilder(n, options, cache, cha);
  }

  /**
   * make a {@link CallGraphBuilder} that uses call-string context sensitivity, with call-string
   * length limited to n, and a context-sensitive allocation-site-based heap abstraction. Standard
   * optimizations in the heap abstraction like smushing of strings are disabled.
   */
  public static SSAPropagationCallGraphBuilder makeVanillaNCFABuilder(
      int n, AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {
    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    SSAPropagationCallGraphBuilder result =
        new nCFABuilder(
            n,
            Language.JAVA.getFakeRootMethod(cha, options, cache),
            options,
            cache,
            appSelector,
            appInterpreter);
    // nCFABuilder uses type-based heap abstraction by default, but we want allocation sites
    result.setInstanceKeys(
        new ZeroXInstanceKeys(
            options,
            cha,
            result.getContextInterpreter(),
            ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC));
    return result;
  }

  /**
   * @deprecated
   *     <p>Use {@link Util#makeVanillaZeroOneContainerCFABuilder(AnalysisOptions,
   *     IAnalysisCacheView, IClassHierarchy)}
   */
  @Deprecated
  public static SSAPropagationCallGraphBuilder makeVanillaZeroOneContainerCFABuilder(
      AnalysisOptions options,
      IAnalysisCacheView cache,
      IClassHierarchy cha,
      @SuppressWarnings("unused") AnalysisScope scope) {

    return makeVanillaZeroOneContainerCFABuilder(options, cache, cha);
  }

  /**
   * @param options options that govern call graph construction
   * @param cha governing class hierarchy
   * @return a 0-1-CFA Call Graph Builder augmented with extra logic for containers
   * @throws IllegalArgumentException if options is null
   */
  public static SSAPropagationCallGraphBuilder makeVanillaZeroOneContainerCFABuilder(
      AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha) {

    if (options == null) {
      throw new IllegalArgumentException("options is null");
    }
    addDefaultSelectors(options, cha);
    addDefaultBypassLogic(options, Util.class.getClassLoader(), cha);
    ContextSelector appSelector = null;
    SSAContextInterpreter appInterpreter = null;
    options.setUseConstantSpecificKeys(true);

    return new ZeroXContainerCFABuilder(
        cha, options, cache, appSelector, appInterpreter, ZeroXInstanceKeys.ALLOCATIONS);
  }
  /**
   * @deprecated
   *     <p>Use {@link Util#addDefaultBypassLogic(AnalysisOptions, ClassLoader, IClassHierarchy)}
   */
  @Deprecated
  public static void addDefaultBypassLogic(
      AnalysisOptions options,
      @SuppressWarnings("unused") AnalysisScope scope,
      ClassLoader cl,
      IClassHierarchy cha) {
    addDefaultBypassLogic(options, cl, cha);
  }

  public static void addDefaultBypassLogic(
      AnalysisOptions options, ClassLoader cl, IClassHierarchy cha) {
    if (nativeSpec == null) return;
    if (cl.getResourceAsStream(nativeSpec) != null) {
      addBypassLogic(options, cl, nativeSpec, cha);
    } else {
      // try to load from filesystem
      try (final BufferedInputStream bIn =
          new BufferedInputStream(new FileInputStream(nativeSpec))) {
        XMLMethodSummaryReader reader = new XMLMethodSummaryReader(bIn, cha.getScope());
        addBypassLogic(options, cl, reader, cha);
      } catch (FileNotFoundException e) {
        System.err.println("Could not load natives xml file from: " + nativeSpec);
        e.printStackTrace();
      } catch (IOException e) {
        System.err.println("Could not close natives xml file " + nativeSpec);
        e.printStackTrace();
      }
    }
  }
}
