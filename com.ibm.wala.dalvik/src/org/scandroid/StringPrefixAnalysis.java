package org.scandroid;
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.domain.LocalElement;
import org.scandroid.flow.functions.IFDSTaintFlowFunctionProvider;
import org.scandroid.flow.functions.TaintTransferFunctions;


import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.IFlowFunctionMap;
import com.ibm.wala.dataflow.IFDS.IMergeFunction;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationProblem;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.dataflow.IFDS.TabulationSolver;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.InterproceduralCFG;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;


public class StringPrefixAnalysis {	

    /**
     * @param args
     * @throws IOException
     * @throws CancelException
     * @throws IllegalArgumentException
     * @throws WalaException
     * @throws CloneNotSupportedException
     */
    public static void main(String[] args) throws IOException, IllegalArgumentException, CancelException, WalaException, CloneNotSupportedException {    	
        //    AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(args[0], new FileProvider().getFile(""));
        final AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("/Users/scubafuchs/Documents/wala_workspace/HelloAndroid/simple_program.jar:/Users/scubafuchs/working/android-sdk-mac_x86-1.5_r1/platforms/android-1.1/android.jar", new FileProvider().getFile("bin/Java60RegressionExclusions.txt"));
        ClassHierarchy cha = ClassHierarchy.make(scope);
        AnalysisCache cache = new AnalysisCache();

        IMethod m = cha.resolveMethod(StringStuff.makeMethodReference("adam.HelloWorld.main([Ljava/lang/String;)V"));
        IMethod mfoo = cha.resolveMethod(StringStuff.makeMethodReference("adam.HelloWorld.foo(Ljava/util/LinkedList;)V"));
        if (m == null) {
            Assertions.UNREACHABLE();
        }

        SSAOptions optionsIR = new AnalysisOptions().getSSAOptions();
        IR ir = cache.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, optionsIR);
        if (ir == null) {
            Assertions.UNREACHABLE();
        }

        Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha);
        AnalysisOptions optionsCFG = new AnalysisOptions(scope, entrypoints);
        LinkedList<Entrypoint> entries = new LinkedList<Entrypoint>();
        for (Entrypoint entry: entrypoints) {
            entries.add(entry);
        }
        entries.add(new DefaultEntrypoint(mfoo, cha));
        optionsCFG.setEntrypoints(entries);
        SSAPropagationCallGraphBuilder cgb = Util.makeVanillaZeroOneCFABuilder(optionsCFG, cache, cha, scope);
        CallGraph cg = cgb.makeCallGraph(optionsCFG);



        PointerAnalysis pa = cgb.getPointerAnalysis();
        OrdinalSetMapping<InstanceKey> keyMapping = pa.getInstanceKeyMapping();
        for (int i = 0; i < keyMapping.getSize(); i++) {
            InstanceKey ik = keyMapping.getMappedObject(i);
            if (ik instanceof AllocationSiteInNode) {
                System.out.println(i + " -> " + ik);
            }
        }
        InstanceKey newInMain = keyMapping.getMappedObject(13);

        System.out.println("=======================");
        System.out.println("Fields: " + newInMain.getConcreteType().getAllFields());

        PointerKey pkNewInMain = new InstanceFieldKey(newInMain, newInMain.getConcreteType().getField(Atom.findOrCreateAsciiAtom("s")));

        PointerKey pkBar = new StaticFieldKey(newInMain.getConcreteType().getField(Atom.findOrCreateAsciiAtom("bar")));


        System.out.println("PK: " + pkNewInMain);
        OrdinalSet<InstanceKey> keys = pa.getPointsToSet(pkNewInMain);
        for (Iterator<InstanceKey> i = keys.iterator(); i.hasNext();) {
            System.out.println("\t\t" + i.next());
        }

        System.out.println("PK: " + pkBar);
        keys = pa.getPointsToSet(pkBar);
        for (Iterator<InstanceKey> i = keys.iterator(); i.hasNext();) {
            System.out.println("\t\t" + i.next());
        }
        play(cg, pa, cache);
    }

    private static void play(CallGraph cg, PointerAnalysis pa, AnalysisCache cache) throws WalaException, CancelException, CloneNotSupportedException {
        Graph<CGNode> partialGraph = GraphSlicer.prune(cg, new Filter<CGNode>() {
            public boolean accepts(CGNode o) {
                return o.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
            }
        });
        Collection<CGNode> nodes = new HashSet<CGNode>();
        for(Iterator<CGNode> nIter = partialGraph.iterator(); nIter.hasNext();)
        {
            nodes.add(nIter.next());
        }
        PartialCallGraph pcg = PartialCallGraph.make(cg, cg.getEntrypointNodes(), nodes);

        InterproceduralCFG ipcfg = new InterproceduralCFG(pcg);

        runAnalysisCG(pcg, ipcfg, pa, cache);
    }


    private static void runAnalysisCG(PartialCallGraph pcg, InterproceduralCFG x, PointerAnalysis pa, AnalysisCache cache) throws CancelException, WalaException, CloneNotSupportedException {
        final InterproceduralCFG cfg = x;

        //    final Hashtable<CGNode, Vector<Integer>> varMaps = new Hashtable<CGNode, Vector<Integer>>();
        //    final Vector<String> mapInv = new Vector<String>();
        //    int count = 0;
        //    for (Iterator<CGNode> i = cfg.getCallGraph().iterator(); i.hasNext();) {
        //      CGNode proc = i.next();
        //      varMaps.put(proc, new Vector<Integer>());
        //      for (int j = 1; j <= proc.getIR().getSymbolTable().getMaxValueNumber(); j++) {
        //        varMaps.get(proc).add(count++);
        //        mapInv.add(proc.getMethod() + " :v" + j);
        //      }
        //    }
        //    for (int k = 0; k < mapInv.size(); k++) {
        //      System.out.println(k + " => " + mapInv.get(k));
        //    }

        final IFDSTaintDomain domain = new IFDSTaintDomain();
        final ISupergraph<BasicBlockInContext<IExplodedBasicBlock>,CGNode> graph =
                (ISupergraph) ICFGSupergraph.make(pcg, cache);

        //    pdfGraph(graph, "supergraph");
        final IFlowFunctionMap<BasicBlockInContext<IExplodedBasicBlock>> functionMap =
        		new TaintTransferFunctions<IExplodedBasicBlock>(domain, graph, pa);
        		//new IFDSTaintFlowFunctionProvider(domain, graph, pa);

        TabulationProblem<BasicBlockInContext<IExplodedBasicBlock>,CGNode,DomainElement> problem = new TabulationProblem<BasicBlockInContext<IExplodedBasicBlock>,CGNode,DomainElement>() {

            public TabulationDomain<DomainElement, BasicBlockInContext<IExplodedBasicBlock>> getDomain() {
                return domain;
            }

            public IFlowFunctionMap<BasicBlockInContext<IExplodedBasicBlock>> getFunctionMap() {
                return functionMap;
            }

            public IMergeFunction getMergeFunction() {
                return null;
            }

            public ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> getSupergraph() {
                return graph;
            }

            public Collection<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> initialSeeds() {
                LinkedList<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>> list = new LinkedList<PathEdge<BasicBlockInContext<IExplodedBasicBlock>>>();
                CGNode entryProc = cfg.getCallGraph().getEntrypointNodes().iterator().next();
                BasicBlockInContext<IExplodedBasicBlock>[] entryBlocks = graph.getEntriesForProcedure(entryProc);
                for(BasicBlockInContext<IExplodedBasicBlock> entryBlock: entryBlocks) {
                    for (int i = 0; i < entryProc.getIR().getNumberOfParameters(); i++) {
                        list.add(PathEdge.createPathEdge(entryBlock, 0, entryBlock, domain.getMappedIndex(new DomainElement(new LocalElement(i+1),null))));
                    }
                }
                return list;
            }

        };
        TabulationSolver<BasicBlockInContext<IExplodedBasicBlock>,CGNode,DomainElement> solver = TabulationSolver.make(problem);
        TabulationResult<BasicBlockInContext<IExplodedBasicBlock>,CGNode,DomainElement> result = solver.solve();
        System.out.println("Result:\n" + result.toString());
        System.out.println("Seeds: " + result.getSeeds());
    }

}
