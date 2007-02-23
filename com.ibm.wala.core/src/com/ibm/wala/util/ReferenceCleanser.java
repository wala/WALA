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
package com.ibm.wala.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.cha.ClassHierarchy;

/**
 * 
 * For some reason (either a bug in our code that defeats soft references, or a
 * bad policy in the GC), leaving soft reference caches to clear themselves out
 * doesn't work. Help it out.
 * 
 * It's unfortunate that this class exits.
 * 
 * @author sfink
 * 
 */
public class ReferenceCleanser {

  private static WeakReference<ClassHierarchy> cha;

  private static WeakReference<AnalysisOptions> options;

  public static void registerClassHierarchy(ClassHierarchy cha) {
    ReferenceCleanser.cha = new WeakReference<ClassHierarchy>(cha);
  }

  private static ClassHierarchy getClassHierarchy() {
    ClassHierarchy result = null;
    if (cha != null) {
      result = cha.get();
    }
    return result;
  }

  /**
   * @param options
   */
  public static void registerAnalysisOptions(AnalysisOptions options) {
    ReferenceCleanser.options = new WeakReference<AnalysisOptions>(options);
  }

  private static AnalysisOptions getAnalysisOptions() {
    AnalysisOptions result = null;
    if (options != null) {
      result = options.get();
    }
    return result;
  }

  /**
   * A debugging aid. TODO: move this elsewhere
   */
  public static void clearSoftCaches() {
    if (getAnalysisOptions() != null) {
      getAnalysisOptions().getSSACache().wipe();
      getAnalysisOptions().getCFGCache().wipe();
    }
    ClassHierarchy cha = getClassHierarchy();
    if (cha != null) {
      for (IClass klass : cha) {
        if (klass instanceof ShrikeClass) {
          ShrikeClass c = (ShrikeClass) klass;
          c.clearSoftCaches();
        } else {
          for (Iterator it2 = klass.getDeclaredMethods().iterator(); it2.hasNext(); ) {
            IMethod m = (IMethod)it2.next();
            if (m instanceof ShrikeCTMethod) {
              ((ShrikeCTMethod)m).clearCaches();
            }
          }
        }
      }
    }
    // System.gc();
    // System.gc();
    // System.gc();

  }

}
