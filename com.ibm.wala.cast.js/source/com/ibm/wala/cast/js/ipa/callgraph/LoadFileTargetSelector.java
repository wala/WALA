/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

public class LoadFileTargetSelector implements MethodTargetSelector {

  private final MethodTargetSelector base;

  private final JSSSAPropagationCallGraphBuilder builder;
  
  private final TypeReference loadFileRef = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName("Lprologue.js/loadFile"));
  
  private final MethodReference loadFileFunRef = AstMethodReference.fnReference(loadFileRef);
  
  private final HashSet<URL> loadedFiles = HashSetFactory.make();

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    IMethod target = base.getCalleeTarget(caller, site, receiver);
    if (target != null && target.getReference().equals(loadFileFunRef)) {

      Set<String> names = new HashSet<>();
      SSAInstruction call = caller.getIR().getInstructions()[caller.getIR().getCallInstructionIndices(site).intIterator().next()];
      if (call.getNumberOfUses() > 1) {
        LocalPointerKey fileNameV = new LocalPointerKey(caller, call.getUse(1));
        OrdinalSet<InstanceKey> ptrs = builder.getPointerAnalysis().getPointsToSet(fileNameV);
        for(InstanceKey k : ptrs) {
          if (k instanceof ConstantKey) {
            Object v = ((ConstantKey<?>)k).getValue();
            if (v instanceof String) {
              names.add((String)v);
            }
          }
        }

        if (names.size() == 1) {
          String str = names.iterator().next();
          try {
            JavaScriptLoader cl = (JavaScriptLoader) builder.getClassHierarchy().getLoader(JavaScriptTypes.jsLoader);
            URL url = new URL(builder.getBaseURL(), str);
            if(!loadedFiles.contains(url)) {
              // try to open the input stream for the URL.  if it fails, we'll get an IOException and fall through to default case
              try (InputStream inputStream = url.openConnection().getInputStream()) {
              }
              JSCallGraphUtil.loadAdditionalFile(builder.getClassHierarchy() , cl, url);
              loadedFiles.add(url);
              IClass script = builder.getClassHierarchy().lookupClass(TypeReference.findOrCreate(cl.getReference(), "L" + url.getFile()));
              return script.getMethod(AstMethodReference.fnSelector);
            }
          } catch (MalformedURLException e1) {
            // do nothing, fall through and return 'target'
          } catch (IOException e) {
            // do nothing, fall through and return 'target'
          } catch (RuntimeException e) {
            // do nothing, fall through and return 'target'
          }
        }
      }
    }

    return target;
  }

  public LoadFileTargetSelector(MethodTargetSelector base, JSSSAPropagationCallGraphBuilder builder) {
    super();
    this.base = base;
    this.builder = builder;
  }

}
