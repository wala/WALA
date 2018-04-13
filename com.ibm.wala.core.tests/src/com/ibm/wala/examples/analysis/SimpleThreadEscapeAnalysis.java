/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.examples.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * <P>
 * A simple thread-level escape analysis: this code computes the set of classes of which some instance may be accessed
 * by some thread other than the one that created it.
 * </P>
 * 
 * <P>
 * The algorithm is not very bright; it is based on the observation that there are only three ways for an object to pass
 * from one thread to another.
 * <UL>
 * <LI> The object is stored into a static variable.
 * <LI> The object is stored into an instance field of a Thread
 * <LI> The object is reachable from a field of another escaping object.
 * </UL>
 * </P>
 * 
 * <P>
 * This observation is implemented in the obvious way:
 * <OL>
 * <LI> All static fields are collected
 * <LI> All Thread constructor parameters are collected
 * <LI> The points-to sets of these values represent the base set of escapees.
 * <LI> All object reachable from fields of these objects are added
 * <LI> This process continues until a fixpoint is reached
 * <LI> The abstract objects in the points-to sets are converted to types
 * <LI> This set of types is returned
 * </OL>
 * </P>
 * 
 * @author Julian Dolby
 */
public class SimpleThreadEscapeAnalysis extends AbstractAnalysisEngine<InstanceKey, CallGraphBuilder<InstanceKey>, Void> {

  private final Set<JarFile> applicationJarFiles;

  private final String applicationMainClass;

  /**
   * The two input parameters define the program to analyze: the jars of .class files and the main class to start from.
   */
  public SimpleThreadEscapeAnalysis(Set<JarFile> applicationJarFiles, String applicationMainClass) {
    this.applicationJarFiles = applicationJarFiles;
    this.applicationMainClass = applicationMainClass;
  }

  @Override
  protected CallGraphBuilder<InstanceKey> getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
    return Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
  }

  /**
   * Given a root path, add it to the set if it is a jar, or traverse it recursively if it is a directory.
   */
  private void collectJars(File f, Set<JarFile> result) throws IOException {
    if (f.isDirectory()) {
      File[] files = f.listFiles();
      for (File file : files) {
        collectJars(file, result);
      }
    } else if (f.getAbsolutePath().endsWith(".jar")) {
      try (final JarFile jar = new JarFile(f, false)) {
        result.add(jar);
      }
    }
  }

  /**
   * Collect the set of JarFiles that constitute the system libraries of the running JRE.
   */
  private JarFile[] getSystemJars() throws IOException {
    String javaHomePath = "garbage";
    Set<JarFile> jarFiles = HashSetFactory.make();

    // first, see if wala.properties has been set up
    try {
      Properties p = WalaProperties.loadProperties();
      javaHomePath = p.getProperty(WalaProperties.J2SE_DIR);
    } catch (WalaException e) {
      // no luck.
    }

    // if not, try assuming the running JRE looks normal
    File x = new File(javaHomePath);
    if (!(x.exists() && x.isDirectory())) {
      javaHomePath = System.getProperty("java.home");

      if (!javaHomePath.endsWith(File.separator)) {
        javaHomePath = javaHomePath + File.separator;
      }

      javaHomePath = javaHomePath + "lib";
    }

    // find jars from chosen JRE lib path
    collectJars(new File(javaHomePath), jarFiles);

    return jarFiles.toArray(new JarFile[jarFiles.size()]);
  }

  /**
   * Take the given set of JarFiles that constitute the program, and return a set of Module files as expected by the
   * WALA machinery.
   */
  private Set<JarFileModule> getModuleFiles() {
    Set<JarFileModule> result = HashSetFactory.make();
    for (JarFile jarFile : applicationJarFiles) {
      result.add(new JarFileModule(jarFile));
    }

    return result;
  }

  /**
   * The heart of the analysis.
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  public Set<IClass> gatherThreadEscapingClasses() throws IOException, IllegalArgumentException,
      CancelException {

    //
    // set the application to analyze
    //
    setModuleFiles(getModuleFiles());

    //
    // set the system jar files to use.
    // change this if you want to use a specific jre version
    //
    setJ2SELibraries(getSystemJars());

    //
    // the application and libraries are set, now build the scope...
    //
    buildAnalysisScope();

    //
    // ...and the class hierarchy
    //
    IClassHierarchy cha = buildClassHierarchy();
    assert cha != null : "failed to create class hierarchy";
    setClassHierarchy(cha);

    //
    // entrypoints are where analysis starts
    //
    Iterable<Entrypoint> roots = Util.makeMainEntrypoints(getScope(), cha, applicationMainClass);

    //
    // analysis options controls aspects of call graph construction
    //
    AnalysisOptions options = getDefaultOptions(roots);

    //
    // build the call graph
    //
    buildCallGraph(cha, options, true, null);

    //
    // extract data for analysis
    //
    CallGraph cg = getCallGraph();
    PointerAnalysis<? extends InstanceKey> pa = getPointerAnalysis();

    //
    // collect all places where objects can escape their creating thread:
    // 1) all static fields
    // 2) arguments to Thread constructors
    //
    Set<PointerKey> escapeAnalysisRoots = HashSetFactory.make();
    HeapModel heapModel = pa.getHeapModel();

    // 1) static fields
    for (IClass cls : cha) {
      Collection<IField> staticFields = cls.getDeclaredStaticFields();
      for (IField sf : staticFields) {
        if (sf.getFieldTypeReference().isReferenceType()) {
          escapeAnalysisRoots.add(heapModel.getPointerKeyForStaticField(sf));
        }
      }
    }

    // 2) instance fields of Threads
    // (we hack this by getting the 'this' parameter of all ctor calls;
    // this works because the next phase will add all objects transitively
    // reachable from fields of types in these pointer keys, and all
    // Thread objects must be constructed somewhere)
    Collection<IClass> threads = cha.computeSubClasses(TypeReference.JavaLangThread);
    for (IClass cls : threads) {
      for (IMethod m : cls.getDeclaredMethods()) {
        if (m.isInit()) {
          Set<CGNode> nodes = cg.getNodes(m.getReference());
          for (CGNode n : nodes) {
            escapeAnalysisRoots.add(heapModel.getPointerKeyForLocal(n, 1));
          }
        }
      }
    }

    // 
    // compute escaping types: all types flowing to escaping roots and
    // all types transitively reachable through their fields.
    //
    Set<InstanceKey> escapingInstanceKeys = HashSetFactory.make();

    //
    // pass 1: get abstract objects (instance keys) for escaping locations
    //
    for (PointerKey root : escapeAnalysisRoots) {
      OrdinalSet<? extends InstanceKey> objects = pa.getPointsToSet(root);
      for (InstanceKey obj : objects) {
        escapingInstanceKeys.add(obj);
      }
    }

    //
    // passes 2+: get fields of escaping keys, and add pointed-to keys
    //
    Set<InstanceKey> newKeys = HashSetFactory.make();
    do {
      newKeys.clear();
      for (InstanceKey key : escapingInstanceKeys) {
        IClass type = key.getConcreteType();
        if (type.isReferenceType()) {
          if (type.isArrayClass()) {
            if (((ArrayClass) type).getElementClass() != null) {
              PointerKey fk = heapModel.getPointerKeyForArrayContents(key);
              OrdinalSet<? extends InstanceKey> fobjects = pa.getPointsToSet(fk);
              for (InstanceKey fobj : fobjects) {
                if (!escapingInstanceKeys.contains(fobj)) {
                  newKeys.add(fobj);
                }
              }
            }
          } else {
            Collection<IField> fields = type.getAllInstanceFields();
            for (IField f : fields) {
              if (f.getFieldTypeReference().isReferenceType()) {
                PointerKey fk = heapModel.getPointerKeyForInstanceField(key, f);
                OrdinalSet<? extends InstanceKey> fobjects = pa.getPointsToSet(fk);
                for (InstanceKey fobj : fobjects) {
                  if (!escapingInstanceKeys.contains(fobj)) {
                    newKeys.add(fobj);
                  }
                }
              }
            }
          }
        }
      }
      escapingInstanceKeys.addAll(newKeys);
    } while (!newKeys.isEmpty());

    //
    // get set of types from set of instance keys
    //
    Set<IClass> escapingTypes = HashSetFactory.make();
    for (InstanceKey key : escapingInstanceKeys) {
      escapingTypes.add(key.getConcreteType());
    }

    return escapingTypes;
  }

  /**
   * This main program shows one example use of thread escape analysis: producing a set of fields to be monitored for a
   * dynamic race detector. The idea is that any field might have a race with two exceptions: final fields do not have
   * races since there are no writes to them, and volatile fields have atomic read and write semantics provided by the
   * VM. Hence, this piece of code produces a list of all other fields.
   * @throws CancelException
   * @throws IllegalArgumentException
   */
  public static void main(String[] args) throws IOException, IllegalArgumentException, CancelException {
    String mainClassName = args[0];

    Set<JarFile> jars = HashSetFactory.make();
    for (int i = 1; i < args.length; i++) {
      jars.add(new JarFile(args[i], false));
    }

    Set<IClass> escapingTypes = (new SimpleThreadEscapeAnalysis(jars, mainClassName)).gatherThreadEscapingClasses();

    for (IClass cls : escapingTypes) {
      if (!cls.isArrayClass()) {
        for (IField f : cls.getAllFields()) {
          if (!f.isVolatile() && !f.isFinal()) {
            System.err.println(f.getReference());
          }
        }
      }
    }
  }
}
