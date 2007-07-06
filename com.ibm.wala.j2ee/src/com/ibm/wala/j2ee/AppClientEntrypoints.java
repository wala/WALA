/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.j2ee;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jst.j2ee.commonarchivecore.internal.ApplicationClientFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.helpers.ArchiveManifest;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.j2ee.util.TopLevelArchiveModule;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.Warnings;

/**
 * 
 * Representation of entrypoints gleaned from the descriptor for an
 * ApplicationClient module.
 * 
 * @author sfink
 */
public class AppClientEntrypoints implements Iterable<Entrypoint> {

  static final boolean DEBUG = false;

  /**
   * Map: MethodReference -> Entrypoint
   */
  private HashMap<MethodReference, Entrypoint> entrypoints = HashMapFactory.make();

  /**
   * Governing class hierarchy
   */
  private final IClassHierarchy cha;

  /**
   * Governing analysis scope
   */
  private final AnalysisScope scope;
  
  /**
   * @param scope
   *          scope of analysis
   * @param cha
   *          loaded class hierarchy
   * @throws IllegalArgumentException  if scope is null
   */
  public AppClientEntrypoints(J2EEAnalysisScope scope, IClassHierarchy cha) {
    if (scope == null) {
      throw new IllegalArgumentException("scope is null");
    }
    this.cha = cha;
    this.scope = scope;
    ClassLoaderReference loader = scope.getApplicationLoader();
    for (Iterator<Module> it = scope.getModules(loader).iterator(); it.hasNext();) {
      Module M = (Module) it.next();
      if (M instanceof TopLevelArchiveModule) {
        addEntrypointsRecursive((TopLevelArchiveModule) M, loader);
      }
    }
  }

  /**
   * Recursively traverse a module and add entrypoints from ApplicationClient
   * archives
   * 
   * @param T
   *          a WCCM archive
   * @param loader
   *          governing class loader
   */
  @SuppressWarnings("restriction")
  private void addEntrypointsRecursive(TopLevelArchiveModule T, ClassLoaderReference loader) {
    if (T.getType() == TopLevelArchiveModule.APPLICATION_CLIENT_FILE) {
      addEntrypoints((ApplicationClientFile) T.materializeArchive());
    } else {
      for (Iterator<ModuleEntry> it = T.getEntries(); it.hasNext();) {
        ModuleEntry E = (ModuleEntry) it.next();
        if (E.isModuleFile()) {
          Module M = E.asModule();
          if (M instanceof TopLevelArchiveModule) {
            addEntrypointsRecursive((TopLevelArchiveModule) M, loader);
          }
        }
      }
    }
  }

  @SuppressWarnings("restriction")
  private void addEntrypoints(ApplicationClientFile file) {
    ArchiveManifest manifest = file.getManifest();
    String mainClass = manifest.getMainClass();
    if (DEBUG) {
      Trace.println("AppClientEntrypoints: add for file " + file);
    }
    if (mainClass == null) {
      if (DEBUG) {
        Trace.println("AppClientEntrypoints:WARNING: mainClass is null");
      }
      return;
    }
    final Atom mainMethod = Atom.findOrCreateAsciiAtom("main");
    TypeName mainName = TypeName.string2TypeName("L" + mainClass.replace('.', '/'));
    final TypeReference T = TypeReference.findOrCreate(scope.getApplicationLoader(), mainName);
    final MethodReference Main = MethodReference.findOrCreate(T, mainMethod, Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V"));
    if (DEBUG) {
      Trace.println("AppClientEntrypoints: create entrypoint " + Main);
    }
    IClass klass = cha.lookupClass(T);
    if (klass == null) {
      Warnings.add(LoadFailure.create(T));
      return;
    }
    IMethod m = cha.resolveMethod(klass,Main.getSelector());
    if (m == null) {
      Warnings.add(LoadFailure.create(Main));
      return;
    }
    entrypoints.put(Main, new DefaultEntrypoint(m, cha));
  }
 /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.Entrypoints#iterator()
   */
  public Iterator<Entrypoint> iterator() {
    return entrypoints.values().iterator();
  }

  /**
   * @param m
   * @return true iff m is an entrypoint recorded by this class
   */
  public boolean contains(MemberReference m) {
    return entrypoints.keySet().contains(m);
  }

}
