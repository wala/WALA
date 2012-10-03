package com.ibm.wala.cast.js.translator;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.rewrite.AstLoopUnwinder;
import com.ibm.wala.cast.tree.rewrite.AstLoopUnwinder.UnwindKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;

public abstract class JavaScriptLoopUnwindingTranslatorFactory 
  implements JavaScriptTranslatorFactory 
{
  private final int unwindFactor;

  protected JavaScriptLoopUnwindingTranslatorFactory(int unwindFactor) {
    this.unwindFactor = unwindFactor;
  }

  JavaScriptLoopUnwindingTranslatorFactory() {
    this(3);
  }

  protected abstract TranslatorToCAst translateInternal(CAst Ast, SourceModule M, String N);

  public TranslatorToCAst make(CAst ast, final SourceModule M) {
	  String N;
	  if (M instanceof SourceFileModule) {
		  N = ((SourceFileModule) M).getClassName();
	  } else {
	      N = M.getName();
	  }

	  TranslatorToCAst xlator = translateInternal(ast, M, N);
	  xlator.addRewriter(new CAstRewriterFactory<CAstRewriter.RewriteContext<AstLoopUnwinder.UnwindKey>,AstLoopUnwinder.UnwindKey>() {
		public CAstRewriter<RewriteContext<UnwindKey>, UnwindKey> createCAstRewriter(CAst ast) {
			return new AstLoopUnwinder(ast, true, unwindFactor);
		}		  
	  }, false);
	  
	  return xlator;
  }
}

