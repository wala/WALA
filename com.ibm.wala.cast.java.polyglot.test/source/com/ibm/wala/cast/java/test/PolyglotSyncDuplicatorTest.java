package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.examples.ast.SynchronizedBlockDuplicator;
import com.ibm.wala.cast.java.translator.polyglot.IRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.JavaIRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.PolyglotJavaSourceAnalysisEngine;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.impl.CAstRewriter;
import com.ibm.wala.cast.tree.impl.CAstRewriterFactory;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class PolyglotSyncDuplicatorTest extends SyncDuplicatorTest {

  public PolyglotSyncDuplicatorTest(String name) {
    super(name);
  }

  protected JavaSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
	    JavaSourceAnalysisEngine engine = new PolyglotJavaSourceAnalysisEngine() {
	      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
	        return Util.makeMainEntrypoints(EclipseProjectPath.SOURCE_REF, cha, mainClassDescriptors);
	      }

	      public IRTranslatorExtension getTranslatorExtension() {
	        JavaIRTranslatorExtension ext = new JavaIRTranslatorExtension();
	        ext.setCAstRewriterFactory(new CAstRewriterFactory() {
		          public CAstRewriter createCAstRewriter(CAst ast) {
		            return new SynchronizedBlockDuplicator(ast, true, testMethod);
		          } 
		        });
	        return ext;
	      }

	    };
	    engine.setExclusionsFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
	    return engine;
	  }
  
}
