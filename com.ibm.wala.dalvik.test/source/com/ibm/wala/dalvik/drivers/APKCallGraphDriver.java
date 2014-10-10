package com.ibm.wala.dalvik.drivers;

import java.io.IOException;

import com.ibm.wala.dalvik.test.callGraph.DalvikCallGraphTestBase;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class APKCallGraphDriver {

	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
		CallGraph CG = DalvikCallGraphTestBase.makeAPKCallGraph(args[0]).fst;
		System.err.println(CG);
		for(CGNode n : CG) {
			System.err.println(n);
			System.err.println(n.getIR());
		}
	}
	
}
