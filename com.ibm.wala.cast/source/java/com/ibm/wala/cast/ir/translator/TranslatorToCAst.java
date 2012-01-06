package com.ibm.wala.cast.ir.translator;

import java.io.IOException;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstRewriter.CopyKey;
import com.ibm.wala.cast.tree.impl.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.impl.CAstRewriterFactory;

public interface TranslatorToCAst {
  public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory, boolean prepend);

  public CAstEntity translateToCAst() throws IOException;

}
