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
package com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.AndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.MicroModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.ExternalModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.SystemServiceModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.UnknownTargetModel;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.AbstractTypeInNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

/**
 *  An {@link SSAContextInterpreter} that redirects functions that start Android-Components.
 *
 *  The Starter-Functions (listed in IntentStarters) are replaced by a Model that emulates Android Lifecycle
 *  based on their Target (Internal, External, ...): A wrapper around the single models is generated dynamically
 *  (by the models themselves) to resemble the signature of the replaced function.
 *
 *  Methods are replacement by generating a adapted Intermediate Representation of this function on every 
 *  occurrence of a call to it.
 * 
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextSelector
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.MicroModel
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.ExternalModel
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-14
 */
public class IntentContextInterpreter implements SSAContextInterpreter {
    private final IntentStarters intentStarters;
    private final IClassHierarchy cha;
    private final AnalysisOptions options;
    private final IAnalysisCacheView cache;

    public IntentContextInterpreter(IClassHierarchy cha, final AnalysisOptions options, final IAnalysisCacheView cache) {
        this.cha = cha;
        this.options = options;
        this.cache = cache;
        this.intentStarters = new IntentStarters(cha);
    }

    /**
     *  Read possible targets of the intents Infos.
     */
    private AndroidComponent fetchTargetComponent(final Intent intent, final IMethod method) {
        assert (method != null);
        assert (intentStarters.getInfo(method.getReference()) != null) : "No IntentStarter for Method " + method + " " + intent;
        if (intent.getComponent() != null) {
            return intent.getComponent();
        } else if (intent.getType() == Intent.IntentType.SYSTEM_SERVICE) {
            
            return AndroidComponent.UNKNOWN;
        } else {
            final Set<AndroidComponent> possibleTargets = intentStarters.getInfo(method.getReference()).getComponentsPossible(); 
            if (possibleTargets.size() == 1) {
                final Iterator<AndroidComponent> it = possibleTargets.iterator();
                return it.next();
            } else {
                // TODO: Go interactive and ask user?
                final Iterator<AndroidComponent> it = possibleTargets.iterator();
                final AndroidComponent targetComponent = it.next();
                return targetComponent;
            }
        }
    } 

    private static TypeReference getCaller(final Context ctx, final CGNode node) {
        if (ctx.get(ContextKey.CALLER) != null) {
            System.out.println("CALLER CONTEXT" + ctx.get(ContextKey.CALLER));
            return node.getMethod().getReference().getDeclaringClass();
        } else if (ctx.get(ContextKey.CALLSITE) != null) {
            System.out.println("CALLSITE CONTEXT" + ctx.get(ContextKey.CALLSITE));
            return node.getMethod().getReference().getDeclaringClass(); 
        } else if (ctx.get(ContextKey.RECEIVER) != null) {
            final AbstractTypeInNode aType = (AbstractTypeInNode) ctx.get(ContextKey.RECEIVER);
            return aType.getConcreteType().getReference();
        } else {
            return node.getMethod().getReference().getDeclaringClass(); // TODO: This may not necessarily fit!
        }
    }

    /**
     *  Generates an adapted IR of the managed functions on each call.
     *
     *  @param  node    The function to create the IR of
     *  @throws IllegalArgumentException on a node of null
     */
    @Override
    public IR getIR(CGNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        assert understands(node);   // Should already have been checked before
        {
            // TODO: CACHE!
            final Context ctx = node.getContext();
            final TypeReference callingClass = getCaller(ctx, node);

            if (ctx.get(Intent.INTENT_KEY) != null) {
                try { // Translate CancelException to IllegalStateException
                final Intent inIntent = (Intent) ctx.get(Intent.INTENT_KEY);                // Intent without overrides
                final Intent intent = AndroidEntryPointManager.MANAGER.getIntent(inIntent); // Apply overrides
                final IMethod method = node.getMethod();

                final AndroidModel model;
                final IntentStarters.StartInfo info;
                Intent.IntentType type = intent.getType();
                if (intent.getAction().equals(Intent.UNBOUND)) {
                    type = Intent.IntentType.UNKNOWN_TARGET;
                }
                { // Fetch model and info
                    switch (type) {
                        case INTERNAL_TARGET:
                            info = intentStarters.getInfo(method.getReference());
                            
                            model = new MicroModel(this.cha, this.options, this.cache, intent.getAction());
                            
                            break;
                        case SYSTEM_SERVICE:
                            info = new IntentStarters.StartInfo(
                                    node.getMethod().getReference().getDeclaringClass(),
                                    EnumSet.of(Intent.IntentType.SYSTEM_SERVICE),
                                    EnumSet.of(AndroidComponent.SERVICE),
                                    new int[] {1} );

                            model = new SystemServiceModel(this.cha, this.options, this.cache, intent.getAction());
                            
                            break;
                        case EXTERNAL_TARGET:
                            info = intentStarters.getInfo(method.getReference());

                            model = new ExternalModel(this.cha, this.options, this.cache, fetchTargetComponent(intent,method));
                            
                            break;
                        case STANDARD_ACTION:
                                    // TODO!
                            // In Order to correctly evaluate a standard-action we would also have to look
                            // at the URI of the Intent.
                        case UNKNOWN_TARGET:
                            info = intentStarters.getInfo(method.getReference());

                            model = new UnknownTargetModel(this.cha, this.options, this.cache, fetchTargetComponent(intent, method));
                            
                            break;
                        case IGNORE:
                            
                            return null;
                        default:
                            throw new java.lang.UnsupportedOperationException("The Intent-Type " + intent.getType() + " is not known to IntentContextInterpreter");
                            // return method.makeIR(ctx, this.options.getSSAOptions());
                    }

                    assert (info != null) : "IntentInfo is null! Every Starter should have an StartInfo...";
                } // of model and info

                final SummarizedMethod override = model.getMethodAs(method.getReference(), callingClass, info, node);
                return override.makeIR(ctx, this.options.getSSAOptions());
                } catch (CancelException e) {
                    throw new IllegalStateException("The operation was canceled.", e);
                }
            } else {
                // This should _not_ happen: IntentContextSelector should always create an IntentContext.
                //
                final IMethod method = node.getMethod();
                final IntentStarters.StartInfo info = intentStarters.getInfo(method.getReference());
                assert (info != null) : "IntentInfo is null! Every Starter should have an StartInfo... - Method " + method.getReference();
                final Intent intent = new Intent(Intent.UNBOUND);
                final AndroidComponent targetComponent = fetchTargetComponent(intent, method);

                try {
                    final UnknownTargetModel model = new UnknownTargetModel(this.cha, this.options, this.cache, targetComponent);
                    final SummarizedMethod override = model.getMethodAs(method.getReference(), callingClass, intentStarters.getInfo(method.getReference()), node);
                    return override.makeIR(ctx, this.options.getSSAOptions());
                } catch (CancelException e) {
                    throw new IllegalStateException("The operation was canceled.", e);
                }
            }
        }
    }

    @Override
    public IRView getIRView(CGNode node) {
      return getIR(node);
    }

    /**
     *  If the function associated with the node is handled by this class.
     *
     *  @throws IllegalArgumentException if the given node is null
     */
    @Override 
    public boolean understands(CGNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        final MethodReference target = node.getMethod().getReference();
        return (
                intentStarters.isStarter(target) 
        );
    }

    @Override
    public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        assert understands(node);   // Should already have been checked before
        {
            final IR ir = getIR(node); // Speeeed
            return ir.iterateNewSites();
        }
    }

    @Override
    public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        assert understands(node);   // Should already have been checked before
        {
            
            final IR ir = getIR(node); // Speeeed
            return ir.iterateCallSites();
        }
    }

    //
    // Satisfy the rest of the interface
    //
    @Override
    public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode node) {
        assert understands(node);
        return getIR(node).getControlFlowGraph();
    }

    @Override
    public int getNumberOfStatements(CGNode node) {
        assert understands(node);
        return getIR(node).getInstructions().length;
    }

    @Override
    public DefUse getDU(CGNode node) {
        assert understands(node);
        return new DefUse(getIR(node));
    }

    @Override
    public boolean recordFactoryType(CGNode node, IClass klass) {
        //this.
        return false;
    }

    @Override
    public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
        assert understands(node);
        
        final SSAInstruction[] statements = getIR(node).getInstructions();

        return CodeScanner.getFieldsWritten(statements).iterator();
    }

    @Override
    public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
        assert understands(node);
        
        final SSAInstruction[] statements = getIR(node).getInstructions();

        return CodeScanner.getFieldsRead(statements).iterator();
    }
}
