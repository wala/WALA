package com.ibm.wala.cast.tree.rewrite;

import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.DelegatingEntity;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NonCopyingContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.Rewrite;
import com.ibm.wala.cast.util.AstConstantCollector;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AstConstantFolder {
  protected boolean skip(@SuppressWarnings("unused") CAstNode n) {
    return false;
  }

  static class AssignSkipContext extends NonCopyingContext {
    private final Set<CAstNode> skip = HashSetFactory.make();
  }

  public CAstEntity fold(CAstEntity ce) {
    Map<String, Object> constants = AstConstantCollector.collectConstants(ce);
    if (constants.isEmpty()) {
      return ce;
    } else {
      Rewrite nce =
          new CAstCloner(new CAstImpl(), new AssignSkipContext(), true) {

            @Override
            protected CAstNode copyNodes(
                CAstNode root,
                CAstControlFlowMap cfg,
                NonCopyingContext c,
                Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {

              if (root.getKind() == CAstNode.ASSIGN) {
                ((AssignSkipContext) c).skip.add(root.getChild(0));
              }

              if (root.getKind() == CAstNode.GLOBAL_DECL) {
                for (int i = 0; i < root.getChildCount(); i++) {
                  ((AssignSkipContext) c).skip.add(root.getChild(i));
                }
              }
              if (root.getKind() == CAstNode.VAR
                  && !skip(root)
                  && constants.containsKey(root.getChild(0).getValue())
                  && !((AssignSkipContext) c).skip.contains(root)) {
                return Ast.makeConstant(constants.get(root.getChild(0).getValue()));
              } else {
                return super.copyNodes(root, cfg, c, nodeMap);
              }
            }
          }.rewrite(
              ce.getAST(),
              ce.getControlFlow(),
              ce.getSourceMap(),
              ce.getNodeTypeMap(),
              ce.getAllScopedEntities(),
              ce.getArgumentDefaults());
      return new DelegatingEntity(ce) {

        @Override
        public CAstNode getAST() {
          return nce.newRoot();
        }

        @Override
        public CAstControlFlowMap getControlFlow() {
          return nce.newCfg();
        }

        @Override
        public CAstSourcePositionMap getSourceMap() {
          return nce.newPos();
        }

        @Override
        public CAstNodeTypeMap getNodeTypeMap() {
          return nce.newTypes();
        }

        @Override
        public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
          return nce.newChildren();
        }

        @Override
        public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
          Collection<CAstEntity> children = nce.newChildren().get(construct);
          return children == null ? EmptyIterator.instance() : children.iterator();
        }
      };
    }
  }
}
