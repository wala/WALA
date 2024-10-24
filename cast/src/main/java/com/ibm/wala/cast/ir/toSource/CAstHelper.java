package com.ibm.wala.cast.ir.toSource;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.collections.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The helper class for some methods of loop */
public class CAstHelper {
  private static final CAst ast = new CAstImpl();

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

    Pair<Integer, CAstNode> countAndTest = countAndRemoveLeadingNegation(test);
    if (countAndTest.fst % 2 == 0) {
      return ast.makeNode(CAstNode.IF_STMT, countAndTest.snd, thenBranch, elseBranch);
    } else {
      return ast.makeNode(CAstNode.IF_STMT, countAndTest.snd, elseBranch, thenBranch);
    }
  }

  public static CAstNode makeIfStmt(CAstNode test, CAstNode thenBranch) {
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
  public static CAstNode stableRemoveLeadingNegation(CAstNode pred) {
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

  public static boolean containsOnlyGotoAndBreak(List<CAstNode> nodes) {
    boolean result = true;
    if (nodes.isEmpty()) return result;

    for (CAstNode nn : nodes) {
      if (nn.getKind() == CAstNode.GOTO) continue;
      if (nn.getKind() == CAstNode.BREAK) continue;
      if (nn.getKind() == CAstNode.BLOCK_STMT) {
        result = containsOnlyGotoAndBreak(nn.getChildren());
        if (!result) break;
        else continue;
      }
      // for other cases, return false
      result = false;
      break;
    }

    return result;
  }

  public static CAstNode makeAssignNode(String varName, int varValue) {
    return ast.makeNode(
        CAstNode.EXPR_STMT,
        ast.makeNode(
            CAstNode.ASSIGN,
            ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
            ast.makeConstant(varValue)));
  }

  public static Pair<CAstNode, CAstNode> generateInnerLoopJumpToHeader(
      Map<ISSABasicBlock, List<Loop>> jumpToTop,
      Map<ISSABasicBlock, List<Loop>> returnToParentHeader,
      Loop currentLoop,
      CAstNode bodyNode,
      String varName,
      LoopType loopType,
      CAstNode test) {
    // if this is the loop recorded in jumpToTop, then generate if-break node
    // check if it's middle loop, the ones that's not the outer most loop and not the inner most
    // loop
    List<CAstNode> jumpList = new ArrayList<>();
    if (jumpToTop.keySet().stream()
            .anyMatch(breaker -> jumpToTop.get(breaker).contains(currentLoop))
        || returnToParentHeader.keySet().stream()
            .anyMatch(
                breaker ->
                    returnToParentHeader.get(breaker).contains(currentLoop)
                        && !returnToParentHeader
                            .get(breaker)
                            .get(returnToParentHeader.get(breaker).size() - 1)
                            .equals(currentLoop))) {
      CAstNode ifCont =
          ast.makeNode(
              CAstNode.IF_STMT,
              ast.makeNode(
                  CAstNode.BINARY_EXPR,
                  CAstOperator.OP_NE,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                  ast.makeConstant(0)),
              ast.makeNode(CAstNode.BREAK));
      jumpList.addAll(bodyNode.getChildren());
      jumpList.add(ifCont);

      bodyNode = ast.makeNode(CAstNode.BLOCK_STMT, jumpList);
    } else
    // find out the top loop
    if (jumpToTop.keySet().stream()
            .anyMatch(breaker -> currentLoop.containsNestedLoop(jumpToTop.get(breaker).get(0)))
        || returnToParentHeader.keySet().stream()
            .anyMatch(
                breaker ->
                    currentLoop.containsNestedLoop(returnToParentHeader.get(breaker).get(0)))) {
      // if this is the parent loop contains the loops been jumped, insert jump assignment at the
      // beginning of the loop
      // and generate if !loopjump then break
      // TODO: first or last?
      CAstNode setFalse =
          ast.makeNode(
              CAstNode.EXPR_STMT,
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                  ast.makeConstant(0)));
      jumpList.add(setFalse);
      jumpList.addAll(bodyNode.getChildren());

      if (LoopType.WHILETRUE.equals(loopType)) {
        // change loop type in this case
        test =
            ast.makeNode(
                CAstNode.BINARY_EXPR,
                CAstOperator.OP_NE,
                ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                ast.makeConstant(0));
      } else {
        CAstNode ifCont =
            ast.makeNode(
                CAstNode.IF_STMT,
                ast.makeNode(
                    CAstNode.BINARY_EXPR,
                    CAstOperator.OP_EQ,
                    ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                    ast.makeConstant(0)),
                ast.makeNode(CAstNode.BREAK));
        jumpList.add(ifCont);
      }

      bodyNode = ast.makeNode(CAstNode.BLOCK_STMT, jumpList);
    }

    // if test has been changed then set loop type to be do while
    return Pair.make(test, bodyNode);
  }

  public static CAstNode generateInnerLoopJumpToOutside(
      Map<ISSABasicBlock, List<Loop>> jumpToOutside,
      Loop currentLoop,
      CAstNode bodyNode,
      String varName) {
    // if this is the loop recorded in jumpToOutside, then generate if-break node
    // check if it's middle loop, the ones that might be the outer most loop and not the inner most
    // loop
    List<CAstNode> jumpList = new ArrayList<>();
    if (jumpToOutside.keySet().stream()
        .anyMatch(breaker -> jumpToOutside.get(breaker).contains(currentLoop))) {
      // find out the top loop
      if (jumpToOutside.keySet().stream()
          .anyMatch(breaker -> jumpToOutside.get(breaker).get(0).equals(currentLoop))) {
        // if this is the parent loop contains the loops been jumped, insert jump assignment at the
        // beginning of the loop
        // and generate if !loopjump then break
        // TODO: first or last?
        CAstNode setFalse =
            ast.makeNode(
                CAstNode.EXPR_STMT,
                ast.makeNode(
                    CAstNode.ASSIGN,
                    ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                    ast.makeConstant(0)));
        jumpList.add(setFalse);
      }
      jumpList.addAll(bodyNode.getChildren());
      CAstNode ifCont =
          ast.makeNode(
              CAstNode.IF_STMT,
              ast.makeNode(
                  CAstNode.BINARY_EXPR,
                  CAstOperator.OP_NE,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                  ast.makeConstant(0)),
              ast.makeNode(CAstNode.BREAK));
      jumpList.add(ifCont);

      bodyNode = ast.makeNode(CAstNode.BLOCK_STMT, jumpList);
    }

    // return bodyNode in this case because it might be changed
    return bodyNode;
  }

  public static void generateInnerLoopJumpToHeaderTrue(
      Map<ISSABasicBlock, List<Loop>> jumpToTop,
      Map<ISSABasicBlock, List<Loop>> returnToParentHeader,
      BasicBlock branchBB,
      Loop loop,
      List<CAstNode> nodeBlock,
      String varName) {
    // If a loop breaker is found in jumpToTop, set ct_loop_jump=true
    // find out the inner most loop
    if ((jumpToTop.containsKey(branchBB)
            && !jumpToTop.get(branchBB).contains(loop)
            && jumpToTop.get(branchBB).stream().anyMatch(ll -> ll.containsNestedLoop(loop)))
        || (returnToParentHeader.containsKey(branchBB)
            && returnToParentHeader
                .get(branchBB)
                .get(returnToParentHeader.get(branchBB).size() - 1)
                .equals(loop))) {
      CAstNode setTrue =
          ast.makeNode(
              CAstNode.EXPR_STMT,
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                  ast.makeConstant(1)));
      // add it before break
      /*if (nodeBlock.get(nodeBlock.size() - 1).getKind() == CAstNode.BREAK)
        nodeBlock.add(nodeBlock.size() - 1, setTrue);
      else*/ nodeBlock.add(0, setTrue);
    }
  }

  public static void generateLoopJumpToOutsideTrue(
      Map<ISSABasicBlock, List<Loop>> jumpToOutside,
      BasicBlock branchBB,
      Loop loop,
      List<CAstNode> nodeBlock,
      String varName) {
    // If a loop breaker is found in jumpToOutside, set ct_loop_jump=true
    // find out the inner most loop
    if ((jumpToOutside.containsKey(branchBB)
        && !jumpToOutside.get(branchBB).contains(loop)
        && jumpToOutside.get(branchBB).stream().anyMatch(ll -> ll.containsNestedLoop(loop)))) {
      CAstNode setTrue =
          ast.makeNode(
              CAstNode.EXPR_STMT,
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varName)),
                  ast.makeConstant(1)));
      // add it before break
      if (nodeBlock.get(nodeBlock.size() - 1).getKind() == CAstNode.BREAK)
        nodeBlock.add(nodeBlock.size() - 1, setTrue);
      else nodeBlock.add(0, setTrue);
    }
  }
}
