package com.ibm.wala.cast.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.cast.ir.translator.AbstractCodeEntity;
import com.ibm.wala.cast.ir.translator.ExposedNamesCollector;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Tests for {@link ExposedNamesCollector}. */
public class TestExposedNamesCollector {

  /** A node kind that {@link com.ibm.wala.cast.tree.visit.CAstVisitor} has no case for. */
  private static final int CUSTOM_KIND = CAstNode.SUB_LANGUAGE_BASE + 1;

  /** Factory for the nodes of the test ASTs. */
  private final CAst ast = new CAstImpl();

  /** A code entity whose AST and scoped entities a test sets directly. */
  private static class TestEntity extends AbstractCodeEntity {

    /** The entity's kind, e.g., {@link CAstEntity#FUNCTION_ENTITY}. */
    private final int kind;

    /** The entity's name. */
    private final String name;

    /** The names of the entity's arguments. */
    private final String[] argumentNames;

    /**
     * Creates an entity with no AST.
     *
     * @param kind the entity's kind, e.g., {@link CAstEntity#FUNCTION_ENTITY}
     * @param name the entity's name
     * @param argumentNames the names of the entity's arguments
     */
    TestEntity(int kind, String name, String[] argumentNames) {
      super(CAstType.DYNAMIC);
      this.kind = kind;
      this.name = name;
      this.argumentNames = argumentNames;
    }

    @Override
    public int getKind() {
      return kind;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String[] getArgumentNames() {
      return argumentNames;
    }

    @Override
    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    @Override
    public int getArgumentCount() {
      return argumentNames.length;
    }

    @Override
    public Collection<CAstQualifier> getQualifiers() {
      return Collections.emptySet();
    }

    @Override
    public Position getPosition(int arg) {
      return null;
    }

    @Override
    public Position getNamePosition() {
      return null;
    }
  }

  /**
   * A function nested under a node kind the visitor has no case for still exposes the names it
   * reads from its enclosing entity.
   */
  @Test
  public void testFunctionUnderUnknownKind() {
    TestEntity inner = new TestEntity(CAstEntity.FUNCTION_ENTITY, "inner", new String[] {"self"});
    inner.setAst(ast.makeNode(CAstNode.RETURN, ast.makeNode(CAstNode.VAR, ast.makeConstant("x"))));

    CAstNode fn = ast.makeNode(CAstNode.FUNCTION_EXPR, ast.makeConstant(inner));
    TestEntity outer = new TestEntity(CAstEntity.SCRIPT_ENTITY, "outer", new String[0]);
    outer.setAst(
        ast.makeNode(
            CAstNode.BLOCK_STMT,
            ast.makeNode(
                CAstNode.DECL_STMT,
                ast.makeConstant(new CAstSymbolImpl("x", CAstType.DYNAMIC)),
                ast.makeConstant(1)),
            ast.makeNode(CUSTOM_KIND, ast.makeNode(CAstNode.BLOCK_EXPR, fn))));
    outer.addScopedEntity(fn, inner);

    ExposedNamesCollector collector = new ExposedNamesCollector();
    collector.run(outer);

    assertThat(collector.getEntity2ExposedNames()).containsKey(outer);
    assertThat(collector.getEntity2ExposedNames().get(outer)).containsExactly("x");
  }
}
