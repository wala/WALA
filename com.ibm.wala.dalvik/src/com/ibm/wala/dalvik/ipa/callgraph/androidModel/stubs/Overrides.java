/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs; 

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 *  Context Free overrides for the startComponent-Methods.
 *
 *  The context-sensitive Overrides may be found in the cfa-package mentioned below.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-28
 */
public class Overrides {
//    private static Logger logger = LoggerFactory.getLogger(Overrides.class);

    private final AndroidModel caller;
    private final IClassHierarchy cha;
    private final AnalysisOptions options;
    private final IAnalysisCacheView cache;

    public Overrides(AndroidModel caller, IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
        this.caller = caller;
        this.cha = cha;
        this.options = options;
        this.cache = cache;
    }

    protected static class StartComponentMethodTargetSelector implements MethodTargetSelector {
        protected MethodTargetSelector parent;
        protected MethodTargetSelector child;
        protected final HashMap<MethodReference, SummarizedMethod> syntheticMethods;

        /**
         *  @param  syntheticMethods    The Methods to override
         *  @param  child               Ask child if unable to resolve. May be null
         */
        public StartComponentMethodTargetSelector(HashMap<MethodReference, SummarizedMethod> syntheticMethods, MethodTargetSelector child) {
            //for (MethodReference mRef : syntheticMethods.keySet()) {
            //    
            //}

            this.syntheticMethods = syntheticMethods;
            this.parent = null;
            this.child = child;
        }

        /**
         *  The MethodTarget selector to ask before trying to resolve the Method with this one. 
         *
         *  @throws IllegalStateException if tried to set parent twice
         */
        public void setParent(MethodTargetSelector parent) {
            if (this.parent != null) {
                throw new IllegalStateException("Parent may only be set once");
            }
            this.parent = parent;
        }

        /**
         *  The MethodTarget selector to ask when the Method could not be resolved by this one. 
         *
         *  In order to be able to use this function you have to set null as child in the Constructor.
         *
         *  @throws IllegalStateException if tried to set parent twice
         */
        public void setChild(MethodTargetSelector child) {
            if (this.child != null) {
                throw new IllegalStateException("Child may only be set once");
            }
            this.child = child;
        }

       /**
         * {@inheritDoc}
         */
        @Override
        public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
            /*if (caller == null) {
                throw new IllegalArgumentException("caller is null");
            }*/
            if (site == null) {
                throw new IllegalArgumentException("site is null");
            }

            if (this.parent != null) {
                IMethod resolved = this.parent.getCalleeTarget(caller, site, receiver);
                if (resolved != null) {
                    return resolved;
                }
            }

            final MethodReference mRef = site.getDeclaredTarget();
            if (syntheticMethods.containsKey(mRef)) {
                if (caller != null) {   // XXX: Debug remove
                    // Context ctx = caller.getContext();

                    
                    
                    
                    
                }

                if (receiver == null) {
                    
                    //return null;
                    //throw new IllegalArgumentException("site is null");
                }

                return syntheticMethods.get(mRef);
            }

            if (this.child != null) {
                IMethod resolved = this.child.getCalleeTarget(caller, site, receiver);
                if (resolved != null) {
                    return resolved;
                }
            }

            return null;
        }

    }

    /**
     *  Generates methods in a MethodTargetSelector.
     *
     *  for all methods found in IntentStarters a synthetic method is generated and added to
     *  the MethodTargetSelector returned.
     *
     *  @return a MethodTargetSelector that overrides all startComponent-calls.
     *  TODO: Use delayed computation?
     */
    public MethodTargetSelector overrideAll() throws CancelException {
        final HashMap<MethodReference, SummarizedMethod> overrides = HashMapFactory.make();
        final Map<AndroidComponent, AndroidModel> callTo = new EnumMap<>(AndroidComponent.class);
        final IProgressMonitor monitor = AndroidEntryPointManager.MANAGER.getProgressMonitor();
        int monitorCounter = 0;

        { // Make Mini-Models to override to
            for (final AndroidComponent target: AndroidComponent.values()) {
                if (AndroidEntryPointManager.EPContainAny(target)) {
                    final AndroidModel targetModel = new UnknownTargetModel(this.cha, this.options, this.cache, target);
                    callTo.put(target, targetModel); 
                } else {
                    final AndroidModel targetModel = new ExternalModel(this.cha, this.options, this.cache, target);
                    callTo.put(target, targetModel); 
                }
            }
        }

        { // Fill overrides
            final IntentStarters starters = new IntentStarters(this.cha);
            final Set<Selector> methodsToOverride = starters.getKnownMethods(); 
            monitor.beginTask("Context-Free overrides", methodsToOverride.size());
            
            for (final Selector mSel : methodsToOverride) {
                monitor.subTask(mSel.getName().toString());
                final IntentStarters.StartInfo info = starters.getInfo(mSel);
                info.setContextFree();
                final TypeReference inClass = info.getDeclaringClass();

                if (inClass == null) {
                    System.err.println("Class does not exist for " + info + " in " + mSel);
                    continue;
                }

                final MethodReference overrideMe = MethodReference.findOrCreate(inClass, mSel);

                final Set<AndroidComponent> possibleTargets = info.getComponentsPossible();
                assert (possibleTargets.size() == 1);
                for (final AndroidComponent target: possibleTargets) {
                    final AndroidModel targetModel = callTo.get(target);

                    final SummarizedMethod override = targetModel.getMethodAs(overrideMe, this.caller.getMethod().getReference().getDeclaringClass(), 
                            info, /* callerNd = */ null); 
                    overrides.put(overrideMe, override);
                }
                monitor.worked(++monitorCounter);
            }
        }

        { // Generate the MethodTargetSelector to return
            
            // XXX: Are these necessary ?:
            //final BypassSyntheticClassLoader syntheticLoader = (BypassSyntheticClassLoader) cha.getLoader(
            //        new ClassLoaderReference (Atom.findOrCreateUnicodeAtom("Synthetic"), ClassLoaderReference.Java, null));
            //  syntheticLoader.registerClass(override.getDeclaringClass().getName(), override.getDeclaringClass());
            //  cha.addClass(override.getDeclaringClass());

            final StartComponentMethodTargetSelector MTSel = new StartComponentMethodTargetSelector(overrides, 
                    options.getMethodTargetSelector());

            monitor.done();
            return MTSel;
            
        }
    }
}
