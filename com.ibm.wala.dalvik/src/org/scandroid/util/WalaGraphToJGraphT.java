/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Steve Suh           <suhsteve@gmail.com>
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

package org.scandroid.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.intset.IntSet;


import org.jgrapht.*;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.*;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.flow.types.FlowType;

public class WalaGraphToJGraphT {
    //private UndirectedGraph<CGNode, DefaultEdge> jgrapht;
	private DirectedGraph<CGNode, DefaultEdge> jgrapht;
    private DijkstraShortestPath<CGNode, DefaultEdge> shortestpath;
    
    @SuppressWarnings("rawtypes")
	public <E extends ISSABasicBlock>
    WalaGraphToJGraphT(TabulationResult<BasicBlockInContext<E>, CGNode, DomainElement> flowResult,
            IFDSTaintDomain<E> domain,
            FlowType source, ISupergraph<BasicBlockInContext<E>, CGNode> graph, CallGraph cg) {

        //jgrapht = new Pseudograph<CGNode, DefaultEdge>(DefaultEdge.class);
        jgrapht = new DirectedPseudograph<CGNode, DefaultEdge>(DefaultEdge.class);

        HashSet<DomainElement> deSet = new HashSet<DomainElement>();
        for (int i = 1; i < domain.getSize(); i++) {
        	DomainElement de = domain.getMappedObject(i);
        	if (de.taintSource.equals(source)){
        		deSet.add(de);
        	}
        }
        
        Iterator<BasicBlockInContext<E>> bbI = graph.iterator();               
        while (bbI.hasNext()) {
        	BasicBlockInContext<E> block = bbI.next();
            IntSet resultSet = flowResult.getResult(block);
            for (DomainElement de: deSet) {
            	if (resultSet.contains(domain.getMappedIndex(de)) && !jgrapht.containsVertex(block.getNode()))
            	{
            		jgrapht.addVertex(block.getNode());
            	}
            }
        }
        
        
        Iterator<CGNode>cgI = cg.iterator();
        while (cgI.hasNext()) {
            CGNode currNode = cgI.next();
        	if (jgrapht.containsVertex(currNode)) {
        		for (Iterator<CGNode> succI = cg.getSuccNodes(currNode); succI.hasNext(); ) {
        			CGNode succ = succI.next();
        			if (jgrapht.containsVertex(succ)) {
        				jgrapht.addEdge(currNode, succ);
        				jgrapht.addEdge(succ, currNode);
        			}
        		}
//        		for (Iterator<CGNode> predI = loader.cg.getPredNodes(currNode); predI.hasNext(); ) {
//        			CGNode pred = predI.next();
//        			if (jgrapht.containsVertex(pred))
//        				jgrapht.addEdge(currNode, pred);
//        		}
        	}
        }
    }

    public void calcPath(CGNode startNode, CGNode endNode) {
        shortestpath = new DijkstraShortestPath<CGNode, DefaultEdge>(jgrapht, startNode, endNode);
    }

    public GraphPath<CGNode, DefaultEdge> getPath() {
        return shortestpath.getPath();
    }
    
    public List<DefaultEdge> getPathEdgeList() {
    	return shortestpath.getPathEdgeList();
    }

//    public UndirectedGraph<CGNode, DefaultEdge> getJGraphT() {
//        return jgrapht;
//    }

    public DirectedGraph<CGNode, DefaultEdge> getJGraphT() {
        return jgrapht;
    }


}
