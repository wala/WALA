package com.ibm.wala.dalvik.drivers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.test.callGraph.DalvikCallGraphTestBase;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;

public class APKCallGraphDriver {
  private static int timeout = -1;

	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, IOException, CancelException {
		File apk = new File(args[0]);
		try {
		  timeout = Integer.parseInt(args[1]);
		  System.err.println("timeout is " + timeout);
		} catch (Throwable e) {
		  // no timeout specified
		}
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
			CallGraph CG;
			if (timeout == -1) {
				CG = DalvikCallGraphTestBase.makeAPKCallGraph(apk.getAbsolutePath()).fst;
			} else {
				final long startTime = System.currentTimeMillis();
				IProgressMonitor pm = new IProgressMonitor() {
					private boolean cancelled = false;
					
					@Override
					public void beginTask(String task, int totalWork) {
						// TODO Auto-generated method stub	
					}

					@Override
					public void subTask(String subTask) {
						// TODO Auto-generated method stub
					}

					@Override
					public void cancel() {
						cancelled = true;
					}

					@Override
					public boolean isCanceled() {
						if (System.currentTimeMillis() - startTime > timeout) {
							cancelled = true;
						}
						return cancelled;
					}

					@Override
					public void done() {
						// TODO Auto-generated method stub					
					}

					@Override
					public void worked(int units) {
						// TODO Auto-generated method stub
					}

					@Override
					public String getCancelMessage() {
						return "timeout";
					}	
				};
				CG = DalvikCallGraphTestBase.makeAPKCallGraph(apk.getAbsolutePath(), pm).fst;
			}
			System.err.println("Analyzed " + apk + " in " + (System.currentTimeMillis() - time));
			
			Set<IMethod> code = HashSetFactory.make();
			for(CGNode n : CG) {
				code.add(n.getMethod());
			}
			for(IMethod m : code) {
				System.err.println(m);
			}
		} catch (Throwable e) {
			e.printStackTrace(System.err);
		}
	}
	
}
