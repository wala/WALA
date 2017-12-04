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
package com.ibm.wala.util.ref;

import java.lang.ref.WeakReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * For some reason (either a bug in our code that defeats soft references, or a
 * bad policy in the GC), leaving soft reference caches to clear themselves out
 * doesn't work. Help it out.
 * 
 * It's unfortunate that this class exists.
 */
public class ReferenceCleanser {
  
  private final static float OCCUPANCY_TRIGGER = 0.5f;

  private static WeakReference<IClassHierarchy> cha;

  private static WeakReference<AnalysisCacheImpl> cache;

  public static void registerClassHierarchy(IClassHierarchy cha) {
    ReferenceCleanser.cha = new WeakReference<>(cha);
  }

  private static IClassHierarchy getClassHierarchy() {
    IClassHierarchy result = null;
    if (cha != null) {
      result = cha.get();
    }
    return result;
  }

  public static void registerCache(IAnalysisCacheView cache) {
    if (cache instanceof AnalysisCacheImpl) {
      ReferenceCleanser.cache = new WeakReference<>((AnalysisCacheImpl) cache);
    }
  }

  private static AnalysisCacheImpl getAnalysisCache() {
    AnalysisCacheImpl result = null;
    if (cache != null) {
      result = cache.get();
    }
    return result;
  }

  /**
   * A debugging aid. TODO: move this elsewhere
   */
  public static void clearSoftCaches() {
    float occupancy = 1f - ((float)Runtime.getRuntime().freeMemory() / (float)Runtime.getRuntime().totalMemory());
    if (occupancy < OCCUPANCY_TRIGGER) {
      return;
    }
    AnalysisCacheImpl cache = getAnalysisCache();
    if (cache != null) {
      cache.getSSACache().wipe();
    }
    IClassHierarchy cha = getClassHierarchy();
    if (cha != null) {
      for (IClass klass : cha) {
        if (klass instanceof ShrikeClass) {
          ShrikeClass c = (ShrikeClass) klass;
          c.clearSoftCaches();
        } else {
          for (IMethod m : klass.getDeclaredMethods()) {
            if (m instanceof ShrikeCTMethod) {
              ((ShrikeCTMethod)m).clearCaches();
            }
          }
        }
      }
    }
  }

}
