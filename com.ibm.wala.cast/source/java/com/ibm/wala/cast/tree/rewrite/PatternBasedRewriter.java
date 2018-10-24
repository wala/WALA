package com.ibm.wala.cast.tree.rewrite;

import java.util.Map;
import java.util.function.Function;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.util.collections.Pair;

public class PatternBasedRewriter extends CAstBasicRewriter<CAstBasicRewriter.NonCopyingContext> {

  private final CAstPattern pattern;
  private final Function<Segments,CAstNode> rewrite;
  
  public PatternBasedRewriter(CAst ast, CAstPattern pattern, Function<Segments,CAstNode> rewrite) {
    super(ast, new NonCopyingContext(), true);
    this.pattern = pattern;
    this.rewrite = rewrite;
  }

  @Override
  protected CAstNode copyNodes(CAstNode root, CAstControlFlowMap cfg, NonCopyingContext context,
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    final Pair<CAstNode, NoKey> pairKey = Pair.make(root, context.key());
    Segments s = CAstPattern.match(pattern, root);
    if (s != null) {
      CAstNode replacement = rewrite.apply(s);
      nodeMap.put(pairKey, replacement);
      return replacement;
    } else if (root instanceof CAstOperator) {
      nodeMap.put(pairKey, root);
      return root;
    } else if (root.getValue() != null) {
      CAstNode copy = Ast.makeConstant(root.getValue());
      assert !nodeMap.containsKey(pairKey);
      nodeMap.put(pairKey, copy);
      return copy;
    } else {
      CAstNode newChildren[] = new CAstNode[root.getChildCount()];

      for (int i = 0; i < root.getChildCount(); i++) {
        newChildren[i] = copyNodes(root.getChild(i), cfg, context, nodeMap);
      }

      CAstNode copy = Ast.makeNode(root.getKind(), newChildren);
      assert !nodeMap.containsKey(pairKey);
      nodeMap.put(pairKey, copy);
      return copy;
    }
  }

  
}
