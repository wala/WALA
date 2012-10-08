package com.ibm.wala.cast.tree.rewrite;

import com.ibm.wala.cast.tree.CAst;

public interface 
    CAstRewriterFactory<C extends CAstRewriter.RewriteContext<K>, 
			K extends CAstRewriter.CopyKey<K>>  
{
  
  public CAstRewriter<C, K> createCAstRewriter(CAst ast);

}
