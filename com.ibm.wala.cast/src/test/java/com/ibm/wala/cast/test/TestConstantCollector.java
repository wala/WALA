package com.ibm.wala.cast.test;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.rewrite.AstConstantFolder;
import com.ibm.wala.cast.util.AstConstantCollector;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.util.collections.EmptyIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.junit.Test;

public class TestConstantCollector {

  private final CAst ast = new CAstImpl();

  private static CAstEntity fakeEntity(CAstNode root) {
    return new CAstEntity() {

      @Override
      public int getKind() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public String getName() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getSignature() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String[] getArgumentNames() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CAstNode[] getArgumentDefaults() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public int getArgumentCount() {
        // TODO Auto-generated method stub
        return 0;
      }

      @Override
      public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
        return Collections.emptyMap();
      }

      @Override
      public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
        return EmptyIterator.instance();
      }

      @Override
      public CAstNode getAST() {
        return root;
      }

      @Override
      public CAstControlFlowMap getControlFlow() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CAstSourcePositionMap getSourceMap() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Position getPosition() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CAstNodeTypeMap getNodeTypeMap() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Collection<CAstQualifier> getQualifiers() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CAstType getType() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Collection<CAstAnnotation> getAnnotations() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Position getPosition(int arg) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Position getNamePosition() {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }

  private final CAstNode root1 =
      ast.makeNode(
          CAstNode.BLOCK_STMT,
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var1")),
              ast.makeConstant(15)));

  @Test
  public void testSegmentsRoot1() {
    Collection<Segments> x =
        CAstPattern.findAll(AstConstantCollector.simpleValuePattern, fakeEntity(root1));
    assert x.size() == 1;
  }

  @Test
  public void testRoot1() {
    Map<String, Object> x = AstConstantCollector.collectConstants(fakeEntity(root1));
    assert x.size() == 1;
  }

  private final CAstNode root2 =
      ast.makeNode(
          CAstNode.BLOCK_STMT,
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var1")),
              ast.makeConstant(15)),
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var1")),
              ast.makeConstant(14)));

  @Test
  public void testSegmentsRoot2() {
    Collection<Segments> x =
        CAstPattern.findAll(AstConstantCollector.simpleValuePattern, fakeEntity(root2));
    assert x.size() == 2;
  }

  private final CAstNode root3 =
      ast.makeNode(
          CAstNode.BLOCK_EXPR,
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var1")),
              ast.makeConstant(15)),
          ast.makeNode(
              CAstNode.BINARY_EXPR,
              CAstOperator.OP_ADD,
              ast.makeConstant(10),
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var1"))));

  public static final CAstPattern toCodePattern3 =
      CAstPattern.parse("BINARY_EXPR(*,\"10\",\"15\")");

  @Test
  public void testRoot3() {
    CAstEntity ce = fakeEntity(root3);
    CAstEntity nce = new AstConstantFolder().fold(ce);
    Collection<Segments> matches = CAstPattern.findAll(toCodePattern3, nce);
    assert matches.size() == 1;
  }

  private final CAstNode root4 =
      ast.makeNode(
          CAstNode.BLOCK_STMT,
          ast.makeNode(
              CAstNode.GLOBAL_DECL,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var1")),
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var2"))),
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var1")),
              ast.makeConstant(14)),
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var2")),
              ast.makeConstant(15)),
          ast.makeNode(
              CAstNode.ASSIGN,
              ast.makeNode(CAstNode.VAR, ast.makeConstant("var3")),
              ast.makeConstant(16)));

  @Test
  public void testRoot4() {
    Map<String, Object> x = AstConstantCollector.collectConstants(fakeEntity(root4));
    assert x.size() == 1 : x;
  }
}
