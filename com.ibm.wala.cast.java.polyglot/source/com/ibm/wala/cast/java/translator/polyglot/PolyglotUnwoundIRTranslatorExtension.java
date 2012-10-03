package com.ibm.wala.cast.java.translator.polyglot;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.rewrite.AstLoopUnwinder;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;

public class PolyglotUnwoundIRTranslatorExtension 
    extends JavaIRTranslatorExtension 
{
  private final int unrollDepth;

  public PolyglotUnwoundIRTranslatorExtension(int unrollDepth) {
    this.unrollDepth = unrollDepth;
  }

  public CAstRewriterFactory<CAstRewriter.RewriteContext<AstLoopUnwinder.UnwindKey>,AstLoopUnwinder.UnwindKey> getCAstRewriterFactory() {
	  return new CAstRewriterFactory<CAstRewriter.RewriteContext<AstLoopUnwinder.UnwindKey>,AstLoopUnwinder.UnwindKey>() {
		  public AstLoopUnwinder createCAstRewriter(CAst ast) {
			  return new AstLoopUnwinder(ast, true, unrollDepth);
		  }  
	  };
  }
  
}
