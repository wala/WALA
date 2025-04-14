package com.ibm.wala.cast.ir.toSource;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.util.collections.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The helper class for some methods of loop */
public class CAstHelper {
  private static final CAst ast = new CAstImpl();

  // hard code the conditional statements for now
  private static final String[] supportedStatements =
      new String[] {
        "ADD",
        "COMPUTE",
        "DIVIDE",
        "MULTIPLY",
        "SUBTRACT",
        "START",
        "READ",
        "RETURN",
        "WRITE",
        "REWRITE",
        "DELETE",
        "UNSTRING",
        "STRING",
        "XML",
        "JSON",
        "CALL"
      };

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
   * @param inLoop If the CAsnNode is in a loop.
   * @return A CAstNode of type IF_STMT equivalent to (if test thenBranch elseBranch), with leading
   *     negation removed from test and possible then/else branches swapped.
   */
  public static List<CAstNode> makeIfStmt(
      CAstNode test, CAstNode thenBranch, CAstNode elseBranch, boolean inLoop) {
    List<CAstNode> result = new ArrayList<>();
    Pair<Integer, CAstNode> countAndTest = countAndRemoveLeadingNegation(test);
    CAstNode newTest = countAndTest.snd;
    if (countAndTest.fst % 2 != 0) {
      // switch then else branch
      CAstNode temp = elseBranch;
      elseBranch = thenBranch;
      thenBranch = temp;
    }

    if (isConditionalStatement(newTest)) {
      result.add(ast.makeNode(CAstNode.IF_STMT, newTest, thenBranch, elseBranch));
    } else {
      if ((inLoop && endingWithBreak(thenBranch)) || endingWithTermination(thenBranch)) {
        // move elseBranch after the if statement
        result.add(ast.makeNode(CAstNode.IF_STMT, newTest, thenBranch));
        if (elseBranch.getKind() == CAstNode.BLOCK_STMT) {
          result.addAll(elseBranch.getChildren());
        } else result.add(elseBranch);
      } else if ((inLoop && endingWithBreak(elseBranch)) || endingWithTermination(elseBranch)) {
        // Negation the test
        if (isLeadingNegation(newTest)) {
          newTest = stableRemoveLeadingNegation(newTest);
        } else {
          newTest = ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, newTest);
        }
        // move thenBranch after the if statement
        result.add(ast.makeNode(CAstNode.IF_STMT, newTest, elseBranch));
        if (thenBranch.getKind() == CAstNode.BLOCK_STMT) {
          result.addAll(thenBranch.getChildren());
        } else result.add(thenBranch);
      } else {
        result.add(ast.makeNode(CAstNode.IF_STMT, newTest, thenBranch, elseBranch));
      }
    }

    return result;
  }

  public static boolean isConditionalStatement(CAstNode test) {
    if (CAstNode.PRIMITIVE == test.getKind()) {
      String testStr = test.getChild(0).getValue().toString().toUpperCase();
      for (int i = 0; i < supportedStatements.length; i++) {
        if (testStr.startsWith(supportedStatements[i] + " ")) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean endingWithBreak(CAstNode block) {
    if (isBreak(block)) return true;
    else if (block.getKind() == CAstNode.BLOCK_STMT && block.getChildCount() > 0) {
      if (endingWithBreak(block.getChild(block.getChildCount() - 1))) return true;
    }
    return false;
  }

  private static boolean isBreak(CAstNode node) {
    return node.getKind() == CAstNode.BREAK;
  }

  public static boolean endingWithTermination(CAstNode block) {
    if (isTermination(block)) return true;
    else if (block.getKind() == CAstNode.BLOCK_STMT && block.getChildCount() > 0) {
      if (endingWithTermination(block.getChild(block.getChildCount() - 1))) return true;
    }
    return false;
  }

  private static boolean isTermination(CAstNode node) {
    // TODO: non-local exit should be considered includes THROW
    return node.getKind() == CAstNode.RETURN || node.getKind() == CAstNode.THROW;
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

  public static CAstNode removeSingleNegation(CAstNode n) {
    assert isLeadingNegation(n) : "Expected node with leading negation " + n;
    return n.getChild(1);
  }

  public static boolean isLeadingNegation(CAstNode n) {
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

  public static Pair<CAstNode, CAstNode> generateInnerLoopJumpToHeaderOrTail(
      Map<ISSABasicBlock, List<Loop>> jumpToTop,
      Map<ISSABasicBlock, List<Loop>> returnToParentHeader,
      Map<ISSABasicBlock, List<Loop>> jumpToOutside,
      Map<ISSABasicBlock, List<Loop>> returnToOutsideTail,
      Loop currentLoop,
      CAstNode bodyNode,
      String varNameJump,
      String varNameBreak,
      LoopType loopType,
      CAstNode test) {
    // prepare for the result
    List<CAstNode> jumpList = new ArrayList<>();
    jumpList.addAll(bodyNode.getChildren());

    // generate jump to break first
    // if this is the loop recorded in jumpToOutside, then generate if-break node
    // check if it's middle loop, the ones that might be the outer most loop and not the inner most
    // loop
    CAstNode generateJumpToTailIfTest = null;
    boolean isOtherLoopJumpToTail =
        isOtherLoopJumpToTail(jumpToOutside, returnToOutsideTail, currentLoop);
    if (isOtherLoopJumpToTail) {
      // find out the top loop
      boolean isTopLoopJumpToTail =
          isTopLoopJumpToTail(jumpToOutside, returnToOutsideTail, currentLoop);
      if (isTopLoopJumpToTail) {
        // if this is the parent loop contains the loops been jumped, insert jump assignment at the
        // beginning of the loop
        // and generate if !loopjump then break
        // TODO: first or last?
        CAstNode setFalse =
            ast.makeNode(
                CAstNode.EXPR_STMT,
                ast.makeNode(
                    CAstNode.ASSIGN,
                    ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameBreak)),
                    ast.makeConstant(0)));
        jumpList.add(0, setFalse);
      }
      generateJumpToTailIfTest =
          ast.makeNode(
              CAstNode.BINARY_EXPR,
              CAstOperator.OP_NE,
              ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameBreak)),
              ast.makeConstant(0));
    }

    // if this is the loop recorded in jumpToTop, then generate if-break node
    // check if it's middle loop, the ones that's not the outer most loop and not the inner most
    // loop
    boolean isMiddleLoopJumpToHeader =
        isMiddleLoopJumpToHeader(jumpToTop, returnToParentHeader, currentLoop);
    boolean isTopLoopJumpToHeader =
        isTopLoopJumpToHeader(jumpToTop, returnToParentHeader, currentLoop);
    boolean needToGenerateJumpToTail = true;
    if (isMiddleLoopJumpToHeader) {
      CAstNode ifCondTest =
          ast.makeNode(
              CAstNode.BINARY_EXPR,
              CAstOperator.OP_NE,
              ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameJump)),
              ast.makeConstant(0));
      if (generateJumpToTailIfTest != null) {
        // TODO generate and with test in line 194
        ifCondTest =
            ast.makeNode(
                CAstNode.BINARY_EXPR, CAstOperator.OP_REL_OR, generateJumpToTailIfTest, ifCondTest);
        needToGenerateJumpToTail = false;
      }
      CAstNode ifCont = ast.makeNode(CAstNode.IF_STMT, ifCondTest, ast.makeNode(CAstNode.BREAK));

      addAfterLoop(jumpList, ifCont, true);
    } else
    // find out the top loop
    if (isTopLoopJumpToHeader) {
      // if this is the parent loop contains the loops been jumped, insert jump assignment at the
      // beginning of the loop
      // and generate if !loopjump then break
      // TODO: first or last?
      CAstNode setFalse =
          ast.makeNode(
              CAstNode.EXPR_STMT,
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameJump)),
                  ast.makeConstant(0)));
      jumpList.add(0, setFalse);

      if (LoopType.WHILETRUE.equals(loopType)) {
        // change loop type in this case
        test =
            ast.makeNode(
                CAstNode.BINARY_EXPR,
                CAstOperator.OP_NE,
                ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameJump)),
                ast.makeConstant(0));
        // TODO: the combination of test is not working very well, disable it for now
        //        if (generateJumpToTailIfTest != null) {
        //          // TODO generate and with test in line 194
        //          test =
        //              ast.makeNode(
        //                  CAstNode.BINARY_EXPR, CAstOperator.OP_REL_AND, test,
        // not(generateJumpToTailIfTest));
        //          needToGenerateJumpToTail = false;
        //        }
      } else {
        CAstNode ifCondTest =
            ast.makeNode(
                CAstNode.BINARY_EXPR,
                CAstOperator.OP_EQ,
                ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameJump)),
                ast.makeConstant(0));
        if (generateJumpToTailIfTest != null) {
          // TODO generate and with test in line 194
          ifCondTest =
              ast.makeNode(
                  CAstNode.BINARY_EXPR,
                  CAstOperator.OP_REL_OR,
                  generateJumpToTailIfTest,
                  ifCondTest);
          needToGenerateJumpToTail = false;
        }
        CAstNode ifCont = ast.makeNode(CAstNode.IF_STMT, ifCondTest, ast.makeNode(CAstNode.BREAK));
        addAfterLoop(jumpList, ifCont, true);
      }
    }

    if (needToGenerateJumpToTail && generateJumpToTailIfTest != null) {
      CAstNode ifCont =
          ast.makeNode(CAstNode.IF_STMT, generateJumpToTailIfTest, ast.makeNode(CAstNode.BREAK));
      addAfterLoop(jumpList, ifCont, true);
    }

    // always restore bodyNode
    bodyNode = ast.makeNode(CAstNode.BLOCK_STMT, jumpList);

    // return bodyNode and test in this case because it might be changed
    return Pair.make(test, bodyNode);
  }

  private static boolean addAfterLoop(
      List<CAstNode> jumpList, CAstNode ifCont, boolean appendToLast) {
    int i = 0;
    for (i = jumpList.size() - 1; i >= 0; i--) {
      // TODO: only check the first child for now
      if (CAstNode.BLOCK_STMT == jumpList.get(i).getKind() && jumpList.get(i).getChildCount() > 0) {
        // add jump to header block right after the loop
        if (CAstNode.LOOP == jumpList.get(i).getChild(0).getKind()) {
          jumpList.add(i + 1, ifCont);
          return true;
        } else {
          List<CAstNode> cc = new ArrayList<>();
          cc.addAll(jumpList.get(i).getChildren());
          if (addAfterLoop(cc, ifCont, false)) {
            // recreate block statement
            jumpList.set(i, ast.makeNode(CAstNode.BLOCK_STMT, cc));
            return true;
          }
        }
      } else if (CAstNode.IF_STMT == jumpList.get(i).getKind()) {
        // TODO: only check if statement for now
        List<CAstNode> childList = new ArrayList<>();
        childList.addAll(jumpList.get(i).getChildren());

        List<CAstNode> thenBlock = new ArrayList<>();
        thenBlock.addAll(childList.get(1).getChildren());
        // TODO: check what will happen if remove another layer of block
        if (thenBlock.size() == 1
            && CAstNode.BLOCK_STMT == thenBlock.get(0).getKind()
            && thenBlock.get(0).getChildCount() > 0
            && CAstNode.LOOP != thenBlock.get(0).getChild(0).getKind()) {
          thenBlock.clear();
          thenBlock.addAll(childList.get(1).getChild(0).getChildren());
        }

        List<CAstNode> elseBlock = new ArrayList<>();
        if (childList.size() > 2) {
          elseBlock.addAll(childList.get(2).getChildren());
          // TODO: check what will happen if remove another layer of block
          if (elseBlock.size() == 1
              && CAstNode.BLOCK_STMT == elseBlock.get(0).getKind()
              && elseBlock.get(0).getChildCount() > 0
              && CAstNode.LOOP != elseBlock.get(0).getChild(0).getKind()) {
            elseBlock.clear();
            elseBlock.addAll(childList.get(2).getChild(0).getChildren());
          }
        }
        boolean result = addAfterLoop(elseBlock, ifCont, false);
        // if it is not added, try another one
        if (!result) {
          result = addAfterLoop(thenBlock, ifCont, false);
        }

        if (result) {
          // recreate if statement
          jumpList.set(
              i,
              ast.makeNode(
                  CAstNode.IF_STMT,
                  childList.get(0),
                  ast.makeNode(CAstNode.BLOCK_STMT, thenBlock),
                  ast.makeNode(CAstNode.BLOCK_STMT, elseBlock)));
          return true;
        }
      }
    }
    if (appendToLast) {
      // if loop can not found and the caller wants to append it anyways, then add it
      jumpList.add(ifCont);
      return true;
    }
    return false;
  }

  private static boolean isTopLoopJumpToHeader(
      Map<ISSABasicBlock, List<Loop>> jumpToTop,
      Map<ISSABasicBlock, List<Loop>> returnToParentHeader,
      Loop loop) {
    return jumpToTop.keySet().stream()
            .anyMatch(breaker -> loop.containsNestedLoop(jumpToTop.get(breaker).get(0)))
        || returnToParentHeader.keySet().stream()
            .anyMatch(breaker -> loop.containsNestedLoop(returnToParentHeader.get(breaker).get(0)));
  }

  private static boolean isMiddleLoopJumpToHeader(
      Map<ISSABasicBlock, List<Loop>> jumpToTop,
      Map<ISSABasicBlock, List<Loop>> returnToParentHeader,
      Loop loop) {
    return jumpToTop.keySet().stream().anyMatch(breaker -> jumpToTop.get(breaker).contains(loop))
        || returnToParentHeader.keySet().stream()
            .anyMatch(
                breaker ->
                    returnToParentHeader.get(breaker).contains(loop)
                        && !returnToParentHeader
                            .get(breaker)
                            .get(returnToParentHeader.get(breaker).size() - 1)
                            .equals(loop));
  }

  private static boolean isInnerMostLoopJumpToHeader(
      Map<ISSABasicBlock, List<Loop>> jumpToTop,
      Map<ISSABasicBlock, List<Loop>> returnToParentHeader,
      BasicBlock branchBB,
      Loop loop) {
    return (jumpToTop.containsKey(branchBB)
            && !jumpToTop.get(branchBB).contains(loop)
            && jumpToTop.get(branchBB).stream().anyMatch(ll -> ll.containsNestedLoop(loop)))
        || (returnToParentHeader.containsKey(branchBB)
            && returnToParentHeader
                .get(branchBB)
                .get(returnToParentHeader.get(branchBB).size() - 1)
                .equals(loop));
  }

  private static boolean isOtherLoopJumpToTail(
      Map<ISSABasicBlock, List<Loop>> jumpToOutside,
      Map<ISSABasicBlock, List<Loop>> returnToOutsideTail,
      Loop loop) {
    return jumpToOutside.keySet().stream()
            .anyMatch(breaker -> jumpToOutside.get(breaker).contains(loop))
        || returnToOutsideTail.keySet().stream()
            .anyMatch(breaker -> returnToOutsideTail.get(breaker).contains(loop));
  }

  private static boolean isTopLoopJumpToTail(
      Map<ISSABasicBlock, List<Loop>> jumpToOutside,
      Map<ISSABasicBlock, List<Loop>> returnToOutsideTail,
      Loop loop) {
    return jumpToOutside.keySet().stream()
            .anyMatch(breaker -> jumpToOutside.get(breaker).get(0).equals(loop))
        || returnToOutsideTail.keySet().stream()
            .anyMatch(breaker -> returnToOutsideTail.get(breaker).get(0).equals(loop));
  }

  private static boolean isInnerMostLoopJumpToTail(
      Map<ISSABasicBlock, List<Loop>> jumpToOutside,
      Map<ISSABasicBlock, List<Loop>> returnToOutsideTail,
      BasicBlock branchBB,
      Loop loop) {
    return (jumpToOutside.containsKey(branchBB)
            && !jumpToOutside.get(branchBB).contains(loop)
            && jumpToOutside.get(branchBB).stream().anyMatch(ll -> ll.containsNestedLoop(loop)))
        || (returnToOutsideTail.containsKey(branchBB)
            && !returnToOutsideTail.get(branchBB).contains(loop)
            && returnToOutsideTail.get(branchBB).stream()
                .anyMatch(ll -> ll.containsNestedLoop(loop)));
  }

  public static void generateInnerLoopJumpToHeaderOrTailTrue(
      Map<ISSABasicBlock, List<Loop>> jumpToTop,
      Map<ISSABasicBlock, List<Loop>> returnToParentHeader,
      Map<ISSABasicBlock, List<Loop>> jumpToOutside,
      Map<ISSABasicBlock, List<Loop>> returnToOutsideTail,
      BasicBlock branchBB,
      Loop loop,
      List<CAstNode> nodeBlock,
      String varNameHeader,
      String varNameTail,
      boolean isDebug) {
    // If a loop breaker is found in jumpToTop, set ct_loop_jump=true
    // find out the inner most loop
    boolean isInnerMostLoopJumpToHeader =
        isInnerMostLoopJumpToHeader(jumpToTop, returnToParentHeader, branchBB, loop);

    // If a loop breaker is found in jumpToOutside or returnToOutsideTail, set ct_loop_jump=true
    // find out the inner most loop
    boolean isInnerMostLoopJumpToTail =
        isInnerMostLoopJumpToTail(jumpToOutside, returnToOutsideTail, branchBB, loop);

    if (isInnerMostLoopJumpToHeader && isInnerMostLoopJumpToTail) {
      if (isDebug)
        System.err.println(
            "This is the case to out setTrue in different branches, which will be handed in visitConditionalBranch");
    } else if (isInnerMostLoopJumpToHeader) {
      CAstNode setTrue =
          ast.makeNode(
              CAstNode.EXPR_STMT,
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameHeader)),
                  ast.makeConstant(1)));
      // add it before break
      if (nodeBlock.get(nodeBlock.size() - 1).getKind() == CAstNode.BREAK)
        nodeBlock.add(nodeBlock.size() - 1, setTrue);
      else nodeBlock.add(0, setTrue);
    } else if (isInnerMostLoopJumpToTail) {
      CAstNode setTrue =
          ast.makeNode(
              CAstNode.EXPR_STMT,
              ast.makeNode(
                  CAstNode.ASSIGN,
                  ast.makeNode(CAstNode.VAR, ast.makeConstant(varNameTail)),
                  ast.makeConstant(1)));
      // add it before break
      if (nodeBlock.get(nodeBlock.size() - 1).getKind() == CAstNode.BREAK)
        nodeBlock.add(nodeBlock.size() - 1, setTrue);
      else nodeBlock.add(0, setTrue);
    }
  }

  public static CAstNode seekAssignmentAndRemove(List<CAstNode> nodes, String varName) {
    CAstNode assignNode = null;
    int i = 0;
    for (i = nodes.size() - 1; i >= 0; i--) {
      if (CAstNode.EXPR_STMT == nodes.get(i).getKind()) {
        if (nodes.get(i).getChildCount() > 0
            && CAstNode.ASSIGN == nodes.get(i).getChild(0).getKind()
            && nodes.get(i).getChild(0).getChildCount() > 0
            && CAstNode.VAR == nodes.get(i).getChild(0).getChild(0).getKind()
            && nodes.get(i).getChild(0).getChild(0).getChildCount() > 0
            && varName.equals(nodes.get(i).getChild(0).getChild(0).getChild(0).getValue())) {
          assignNode = nodes.remove(i);
        }
      } else if (nodes.get(i).getChildCount() > 0) {
        // try to seek in it's children
        List<CAstNode> children = new ArrayList<>();
        children.addAll(nodes.get(i).getChildren());
        assignNode = seekAssignmentAndRemove(children, varName);
        // if it's found, try to update the child list to remove it
        if (assignNode != null) {
          nodes.set(i, ast.makeNode(nodes.get(i).getKind(), children));
        }
      }

      if (assignNode != null) break;
    }
    return assignNode;
  }

  public static boolean needsExitParagraph(SSAGotoInstruction inst) {
    // check if it's a jump from middle to the end
    if (inst.getTarget() == -1) {
      // goto -1 means EXIT PARAGRAPH
      return true;
    }
    return false;

    // TODO: I though there are other cases that EXIT PARAGRAPH should be needed but test result
    // shows that these conditionals are not needed. I'll keep them as comment for now in case if
    // needed in future
    //    if ((inst.iIndex() + 1) == inst.getTarget()) {
    //      // goto next instruction then ignore this goto
    //      return false;
    //    }
    //
    //    ISSABasicBlock targetBlock = cfg.getBlockForInstruction(inst.getTarget());
    //    Collection<ISSABasicBlock> succBlock =
    //        cfg.getNormalSuccessors(cfg.getBlockForInstruction(inst.iIndex()));
    //    if (succBlock.size() == 1
    //        && succBlock.iterator().next().equals(targetBlock)
    //        && targetBlock.getFirstInstructionIndex() == targetBlock.getLastInstructionIndex()
    //        && targetBlock.getLastInstruction() instanceof SSAReturnInstruction) {
    //      // skip the case where it'll move on to RETURN
    //      return false;
    //    }
    //    return targetBlock.getLastInstructionIndex() == inst.getTarget()
    //        && targetBlock.getLastInstruction() instanceof SSAReturnInstruction;
  }

  public static CAstNode createExitParagraph() {
    // If goto instruction will go to -1 that should be an EXIT PARAGRAPGH
    return ast.makeNode(
        CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.GOTO, ast.makeConstant("EXIT PARAGRAPH")));
    // List<CAstNode> args = new ArrayList<>();
    // args.add(ast.makeConstant("EXIT PARAGRAPH"));
    // return ast.makeNode(CAstNode.PRIMITIVE, args.toArray(new
    // CAstNode[args.size()]));
  }
}
