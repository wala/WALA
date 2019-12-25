package com.ibm.wala.cast.tree.rewrite;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.util.collections.Pair;
import java.util.Map;
import java.util.function.Function;

public class PatternBasedRewriter extends CAstCloner {

  private final CAstPattern pattern;
  private final Function<Segments, CAstNode> rewrite;

  public PatternBasedRewriter(CAst ast, CAstPattern pattern, Function<Segments, CAstNode> rewrite) {
    super(ast, true);
    this.pattern = pattern;
    this.rewrite = rewrite;
  }

  @Override
  protected CAstNode copyNodes(
      CAstNode root,
      CAstControlFlowMap cfg,
      NonCopyingContext context,
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    final Pair<CAstNode, NoKey> pairKey = Pair.make(root, context.key());
    Segments s = CAstPattern.match(pattern, root);
    if (s != null) {
      CAstNode replacement = rewrite.apply(s);
      nodeMap.put(pairKey, replacement);
      return replacement;
    } else return copyNodes(root, cfg, context, nodeMap, pairKey);
  }
}
