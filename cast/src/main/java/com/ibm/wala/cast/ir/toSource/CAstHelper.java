package com.ibm.wala.cast.ir.toSource;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.util.collections.Pair;

/** The helper class for some methods of loop */
public class CAstHelper {

  /**
   * Remove redundant negation from test node.
   *
   * <ul>
   *   <li>Even or zero negation count: all negation can be removed.
   *   <li>Odd count: remove negation and flip branches.
   * </ul>
   *
   * @param test The test node for the if-stmt to be created. May contain leading negation.
   * @param thenBranch The 'true' branch of the if-stmt. May be flipped with the else branch if
   *     negation count is odd.
   * @param elseBranch The 'false' branch of the if-stmt. May be flipped with the then branch if
   *     negation count is odd.
   * @return A CAstNode of type IF_STMT equivalent to (if test thenBranch elseBranch), with leading
   *     negation removed from test and possible then/else branches swapped.
   */
  public static CAstNode makeIfStmt(CAstNode test, CAstNode thenBranch, CAstNode elseBranch) {
    CAst ast = new CAstImpl();
    Pair<Integer, CAstNode> countAndTest = countAndRemoveLeadingNegation(test);
    if (countAndTest.fst % 2 == 0) {
      return ast.makeNode(CAstNode.IF_STMT, countAndTest.snd, thenBranch, elseBranch);
    } else {
      return ast.makeNode(CAstNode.IF_STMT, countAndTest.snd, elseBranch, thenBranch);
    }
  }

  public static CAstNode makeIfStmt(CAstNode test, CAstNode thenBranch) {
    CAst ast = new CAstImpl();
    return ast.makeNode(CAstNode.IF_STMT, stableRemoveLeadingNegation(test), thenBranch);
  }

  /**
   * Remove redundant negation from test node.
   *
   * <ul>
   *   <li>Even or zero negation count: all negation can be removed.
   *   <li>Odd count: remove negation and flip branches.
   * </ul>
   *
   * @param pred The node from which negation should be removed.
   * @return The input node, with pairs of leading negation removed.
   */
  private static CAstNode stableRemoveLeadingNegation(CAstNode pred) {
    CAst ast = new CAstImpl();
    Pair<Integer, CAstNode> countAndPred = countAndRemoveLeadingNegation(pred);
    if (countAndPred.fst % 2 == 0) {
      return countAndPred.snd;
    } else /* odd negation count (at least one) */ {
      return ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, countAndPred.snd);
    }
  }

  /**
   * Remove redundant negation from test node.
   *
   * <ul>
   *   <li>Even or zero negation count: all negation can be removed.
   *   <li>Odd count: remove negation and flip branches.
   * </ul>
   *
   * @param pred The node from which negation should be removed.
   * @return The input node, with pairs of leading negation removed.
   */
  public static CAstNode stableRemoveLeadingNegation(CAst ast, CAstNode pred) {
    Pair<Integer, CAstNode> countAndPred = countAndRemoveLeadingNegation(pred);
    if (countAndPred.fst % 2 == 0) {
      return countAndPred.snd;
    } else /* odd negation count (at least one) */ {
      return ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, countAndPred.snd);
    }
  }

  /**
   * Counts leading negation and removes it from the input node. Then returns a pair with this
   * information.
   *
   * @param n The input node.
   * @return A pair with first element count, and second element n, but with all leading negation
   *     removed.
   */
  private static Pair<Integer, CAstNode> countAndRemoveLeadingNegation(CAstNode n) {
    int count = 0;
    CAstNode tmp = n;
    while (isLeadingNegation(tmp)) {
      count++;
      tmp = removeSingleNegation(tmp);
    }
    return Pair.make(count, tmp);
  }

  private static CAstNode removeSingleNegation(CAstNode n) {
    assert isLeadingNegation(n) : "Expected node with leading negation " + n;
    return n.getChild(1);
  }

  private static boolean isLeadingNegation(CAstNode n) {
    return n.getKind() == CAstNode.UNARY_EXPR
        && n.getChildCount() > 1
        && n.getChild(0) == CAstOperator.OP_NOT;
  }

  /**
   * Find out the variable name that's used in the test
   *
   * @param test The CAstNode of a test, usually it is the condition in if statement
   * @return The object of variable name
   */
  public static Object findVariableNameFromTest(CAstNode test) {
    Object varName = null;
    if (test.getChildCount() > 1
        && test.getChild(1).getChildCount() > 0
        && test.getChild(1).getChild(0).getValue() != null) {
      if (CAstNode.BINARY_EXPR == test.getChild(1).getKind()) {
        varName = test.getChild(1).getChild(1).getChild(0).getValue();
      } else varName = test.getChild(1).getChild(0).getValue();
    }
    return varName;
  }

  public static boolean hasVarAssigned(CAstNode node, String var) {
    CAstPattern jumpAssign = CAstPattern.parse("ASSIGN(VAR(\"" + var + "\"),**)");
    return !CAstPattern.findAll(jumpAssign, node).isEmpty();
  }
}
