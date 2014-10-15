package com.ibm.wala.dalvik.drivers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import com.ibm.wala.dalvik.test.callGraph.DalvikCallGraphTestBase;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class APKCallGraphDriver {

	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
		File apk = new File(args[0]);
		recurseApks(apk);
	}

	protected static void recurseApks(File apk) throws IOException,
			ClassHierarchyException, CancelException {
		if (apk.isDirectory()) {
			for(File f : apk.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith("apk") || new File(dir, name).isDirectory();
				}	
			})) {
				recurseApks(f);
			}
		} else {
			doApk(apk);
		}
	}

	protected static void doApk(File apk) throws IOException,
			ClassHierarchyException, CancelException {
		System.err.println("Analyzing " + apk + "...");
		try {
			long time = System.currentTimeMillis();
			CallGraph CG = DalvikCallGraphTestBase.makeAPKCallGraph(apk.getAbsolutePath()).fst;
			System.err.println("Analyzed " + apk + " in " + (System.currentTimeMillis() - time));
			System.err.println(CG);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
		/*
		for(CGNode n : CG) {
			System.err.println(n);
			System.err.println(n.getIR());
		}
		*/
	}
	
}
