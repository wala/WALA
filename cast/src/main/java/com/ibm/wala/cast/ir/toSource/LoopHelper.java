package com.ibm.wala.cast.ir.toSource;

import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SSAUnspecifiedExprInstruction;
import com.ibm.wala.ssa.SSAUnspecifiedInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.IteratorUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/** The helper class for some methods of loop */
public class LoopHelper {

  private static boolean isForLoop(SymbolTable ST, Loop loop) {
    boolean isForLoop = false;
    if (loop.getLoopHeader().equals(loop.getLoopControl()) && loop.getLoopBreakers().size() < 2) {
      // A for-loop is targeting for PERFORM n TIMES and PERFORM VARYING for now
      // The loopHeader and loopControl are the same
      // The loopHeader should contains 3 or more than 3 instructions (based on current samples)
      // The last few instructions in loopHeader should follow a rule on both type and relationship
      // And the second last instruction in loop body should be incremental
      List<SSAInstruction> headerInsts =
          IteratorUtil.streamify(loop.getLoopHeader().iterator()).collect(Collectors.toList());

      int index = headerInsts.size() - 1;

      int nextUse = -1;

      while (index >= 0) {
        SSAInstruction currentInst = headerInsts.get(index);
        if (currentInst instanceof SSAConditionalBranchInstruction) {
          nextUse = currentInst.getUse(0);
        } else {
          if (currentInst instanceof SSAUnaryOpInstruction
              || currentInst instanceof SSABinaryOpInstruction) {
            if (nextUse == currentInst.getDef()) {
              nextUse = currentInst.getUse(0);
            } else {
              break;
            }
          } else if (currentInst instanceof SSAPhiInstruction) {
            if (nextUse == currentInst.getDef()) {
              isForLoop =
                  currentInst.getNumberOfUses() > 1
                      && ST.isConstant(currentInst.getUse(1))
                      && incrementalAtLast(loop, currentInst.getUse(0));
            }
          }
        }

        index--;
      }
    }
    return isForLoop;
  }

  private static boolean incrementalAtLast(Loop loop, int use) {
    List<SSAInstruction> lastInsts =
        IteratorUtil.streamify(loop.getLastBlock().iterator()).collect(Collectors.toList());
    return lastInsts.size() > 1
        && lastInsts.get(lastInsts.size() - 2) instanceof SSABinaryOpInstruction
        && lastInsts.get(lastInsts.size() - 2).getDef() == use;
  }

  private static boolean isWhileLoop(PrunedCFG<SSAInstruction, ISSABasicBlock> cfg, Loop loop) {
    if (loop.getLoopHeader().equals(loop.getLoopControl())) {
      boolean notWhileLoop = false;

      // If loopHeader and loopControl are the same, check if there are any other instructions
      // before Conditional Branch, if no, it is a while loop
      // For now it is simply check by instruction type
      // It should be checking `result` of the current instruction should be val1 of the
      // next instruction
      List<SSAInstruction> headerInsts =
          IteratorUtil.streamify(loop.getLoopHeader().iterator()).collect(Collectors.toList());
      for (SSAInstruction inst : headerInsts) {
        if (inst.iIndex() < 0) continue;
        if (inst instanceof SSAUnaryOpInstruction) {
          continue;
        }
        if (inst instanceof SSABinaryOpInstruction) {
          continue;
        }
        if (inst instanceof SSAConditionalBranchInstruction) {
          continue;
        }
        // TODO: this is a temporary change especially this one
        // to help identify if there are only instructions related with test
        if (inst instanceof SSAUnspecifiedExprInstruction) {
          continue;
        }
        notWhileLoop = true;
        break;
      }

      if (!notWhileLoop) {
        // check loop exits
        if (loop.getLoopExits().size() > 1) {
          // if there are more than one loop exit and there are some instructions after loop
          // control, it should not be a while loop
          Collection<ISSABasicBlock> loopExits = cfg.getNormalSuccessors(loop.getLoopControl());
          loopExits.retainAll(loop.getLoopExits());
          assert (loopExits.size() > 0);
          List<SSAInstruction> exitInsts =
              IteratorUtil.streamify(loopExits.iterator().next().iterator())
                  .collect(Collectors.toList());
          for (SSAInstruction inst : exitInsts) {
            if (inst.iIndex() < 0) continue;
            // TODO: need to check if any other case should be placed here
            if (inst instanceof SSAReturnInstruction) {
              continue;
            }
            if (inst instanceof SSAUnspecifiedInstruction
                && ((SSAUnspecifiedInstruction<?>) inst).getPayload() != null
                && "STOP RUN"
                    .equalsIgnoreCase(
                        ((SSAUnspecifiedInstruction<?>) inst).getPayload().toString())) {
              continue;
            }
            notWhileLoop = true;
            break;
          }

          if (!notWhileLoop) {
            // if all loop exits normal successor are the same, it's while loop
            List<ISSABasicBlock> nextBBs =
                loop.getLoopExits().stream()
                    .map(ex -> cfg.getNormalSuccessors(ex))
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());
            return nextBBs.size() < 2;
          } else {
            return false;
          }
        } else {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isDoLoop(PrunedCFG<SSAInstruction, ISSABasicBlock> cfg, Loop loop) {
    // If loopControl successor is loopHeader then it's a do loop, no matter they are the same block
    // or different
    boolean doLoop = false;
    Iterator<ISSABasicBlock> succ = cfg.getSuccNodes(loop.getLoopControl());
    while (succ.hasNext()) {
      ISSABasicBlock nextBB = succ.next();
      // Find the branch of loop control which will remain in the loop
      if (loop.getAllBlocks().contains(nextBB)) {
        // It should be a goto chunk in this case, otherwise it's whiletrue loop
        List<SSAInstruction> nextInsts =
            IteratorUtil.streamify(nextBB.iterator()).collect(Collectors.toList());
        if (!gotoChunk(nextInsts)) {
          break;
        }

        Iterator<ISSABasicBlock> nextSucc = cfg.getSuccNodes(nextBB);
        while (nextSucc.hasNext()) {
          if (loop.getLoopHeader().equals(nextSucc.next())) {
            doLoop = true;
            break;
          }
        }
      }
    }
    return doLoop;
  }

  /**
   * Determine loop type based on what's in the loop
   *
   * @param cfg The control flow graph
   * @param loop The loop for type
   * @return The loop type
   */
  public static LoopType getLoopType(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg, SymbolTable ST, Loop loop) {
    if (loop.getLoopHeader().equals(loop.getLoopControl())) {
      // check if it's for loop
      // For now a for-loop refers PERFORM n TIMES
      if (isForLoop(ST, loop)) return LoopType.FOR;

      // usually for loop will be detected as while loop too, so that check for-loop first
      if (isWhileLoop(cfg, loop)) return LoopType.WHILE;
    }

    // TODO: check unsupported loop types or add a loop type of ugly loop
    if (isDoLoop(cfg, loop)) return LoopType.DOWHILE;
    else return LoopType.WHILETRUE;
  }

  /**
   * @param chunk A list of instructions
   * @return If the chunk of instructions only has goto instruction
   */
  public static boolean gotoChunk(List<SSAInstruction> chunk) {
    return chunk.size() == 1 && chunk.iterator().next() instanceof SSAGotoInstruction;
  }

  /**
   * Find out the loop that contains the instruction It'll try to find the inner loop first and then
   * outer loop (backwards)
   *
   * @param cfg The control flow graph
   * @param instruction The instruction to be used to look for a loop
   * @param loops All the loops that's in the control flow graph
   * @return The loop that contains the instruction. It can be null if no loop can be found
   */
  public static Loop getLoopByInstruction(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg,
      SSAInstruction instruction,
      Map<ISSABasicBlock, Loop> loops) {
    if (instruction.iIndex() < 0) return null;
    Optional<Loop> result =
        loops.values().stream()
            .sorted(
                (a, b) -> {
                  return b.getLoopHeader().getNumber() - a.getLoopHeader().getNumber();
                })
            .filter(
                loop ->
                    loop.getAllBlocks().contains(cfg.getBlockForInstruction(instruction.iIndex())))
            .findFirst();
    return result.isPresent() ? result.get() : null;
  }

  /**
   * Find out the loop that the given chunk belongs to and not the loop that's provided It should
   * scan from first to last since the skipped loops are provided
   *
   * @param cfg The control flow graph
   * @param chunk The instructions to be used to check
   * @param loops All the loops that's in the control flow graph
   * @param skipLoop The loops that should bypass
   * @return Loop The loop which the given chunk belongs to, or null control
   */
  public static Loop findLoopByChunk(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg,
      List<SSAInstruction> chunk,
      Map<ISSABasicBlock, Loop> loops,
      List<Loop> skipLoop) {
    // Find out the first instruction in the chunk
    Optional<SSAInstruction> first = chunk.stream().filter(inst -> inst.iIndex() > 0).findFirst();

    if (!first.isPresent()) {
      return null;
    }

    // Find out the loop
    Optional<Loop> result =
        loops.values().stream()
            .sorted(
                (a, b) -> {
                  return a.getLoopHeader().getNumber() - b.getLoopHeader().getNumber();
                })
            .filter(
                loop ->
                    (skipLoop == null || !skipLoop.contains(loop))
                        && loop.getAllBlocks()
                            .contains(cfg.getBlockForInstruction(first.get().iIndex())))
            .findFirst();
    return result.isPresent() ? result.get() : null;
  }

  // check if the assignment is in both inner and outer loop
  public static boolean containsInNestedLoop(
      Loop loop, Map<ISSABasicBlock, Loop> loops, ISSABasicBlock assignmentBlock) {
    return loop.containsNestedLoop()
        && loops.values().stream()
            .anyMatch(
                p -> {
                  return p.getAllBlocks().contains(assignmentBlock) && loop.containsNestedLoop(p);
                });
  }

  /**
   * Find out if the given chunk is in the loop and before the conditional branch of the loop
   * control
   *
   * @param cfg The control flow graph
   * @param chunk The instructions to be used to check
   * @param loops All the loops that's in the control flow graph
   * @return True if the given chunk is in the loop and before the conditional branch of the loop
   *     control
   */
  public static boolean shouldMoveAsLoopBody(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg,
      SymbolTable ST,
      List<SSAInstruction> chunk,
      Map<ISSABasicBlock, Loop> loops,
      List<Loop> skipLoop) {
    // Find out the first instruction in the chunk
    Optional<SSAInstruction> first = chunk.stream().filter(inst -> inst.iIndex() > 0).findFirst();

    if (!first.isPresent()) {
      return false;
    }

    // Find out the loop
    Loop loop = findLoopByChunk(cfg, chunk, loops, skipLoop);
    if (loop == null) return false;

    ISSABasicBlock currentBB = cfg.getBlockForInstruction(first.get().iIndex());
    // If the block is after loop control, return false
    if (currentBB.getNumber() > loop.getLoopControl().getNumber()) {
      return false;
    } else if (currentBB.getNumber() < loop.getLoopControl().getNumber()) {
      if (isAssignment(chunk)) {
        // if it is assignment, for perf-with-goto-1-4.cbl, it should be in outer loop but not in
        // inner loop
        // this is a temp solution as we can't tell other clue to make it happen
        if (containsInNestedLoop(loop, loops, currentBB)) return true;
        return false;
      } else {
        // If the block is before loop control, return true
        return true;
      }
    } else {
      if (isAssignment(chunk)) {
        // if it is loop control, assignment should be ignored
        // except the last assignment for for-loop
        if (chunk.size() == 1
            && chunk.get(0) instanceof AssignInstruction
            && LoopType.FOR.equals(getLoopType(cfg, ST, loop))) {
          int def = ((AssignInstruction) chunk.get(0)).getDef();
          List<SSAInstruction> controlInsts =
              IteratorUtil.streamify(currentBB.iterator()).collect(Collectors.toList());
          SSAInstruction op = controlInsts.get(controlInsts.size() - 2);
          if (op instanceof SSAUnaryOpInstruction) {
            op = controlInsts.get(controlInsts.size() - 3);
          }
          if (op instanceof SSABinaryOpInstruction) {
            for (int i = 0; i < ((SSABinaryOpInstruction) op).getNumberOfUses(); i++) {
              if (((SSABinaryOpInstruction) op).getUse(i) == def) {
                return true;
              }
            }
          }
        }
        return false;
      }
      return true;
    }
  }

  // Check if the given chunk contains any instruction that's part of conditional branch
  public static boolean isConditional(List<SSAInstruction> chunk) {
    return chunk.stream().anyMatch(inst -> inst instanceof SSAConditionalBranchInstruction);
  }

  // Check if the given chunk contains any instruction that's an assignment generated by phi node
  private static boolean isAssignment(List<SSAInstruction> chunk) {
    return chunk.stream().allMatch(inst -> inst instanceof AssignInstruction);
  }

  /**
   * Check if the given instruction is part of loop control It will check inner loop first
   *
   * @param cfg The control flow graph
   * @param inst The instruction to be used to check
   * @param loops All the loops that's in the control flow graph
   * @return True if the given instruction is part of loop control
   */
  public static boolean isLoopControl(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg,
      SSAInstruction inst,
      Map<ISSABasicBlock, Loop> loops) {
    return inst.iIndex() > 0
        ? loops.values().stream()
            .sorted(
                (a, b) -> {
                  return b.getLoopHeader().getNumber() - a.getLoopHeader().getNumber();
                })
            .map(loop -> loop.getLoopControl())
            .anyMatch(control -> control.equals(cfg.getBlockForInstruction(inst.iIndex())))
        : false;
  }

  /**
   * Check if the given instruction is part of loop control
   *
   * @param cfg The control flow graph
   * @param chunk The chunk to be used to check
   * @param loop The loop that is currently translating
   * @return True if the given instruction is part of loop control
   */
  public static boolean isLoopControl(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg, List<SSAInstruction> chunk, Loop loop) {
    return chunk.stream()
        .anyMatch(inst -> loop.getLoopControl().equals(cfg.getBlockForInstruction(inst.iIndex())));
  }

  /**
   * In some cases operations on test can be merged, e.g. while loop In other cases these operations
   * should be separated
   *
   * @return True if it's conditional branch of a while loop
   */
  public static boolean shouldMergeTest(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg,
      SymbolTable ST,
      SSAInstruction inst,
      Map<ISSABasicBlock, Loop> loops) {
    if ((inst instanceof SSAConditionalBranchInstruction)) {
      Loop loop = getLoopByInstruction(cfg, inst, loops);
      return loop != null
          && loop.getLoopControl().equals(cfg.getBlockForInstruction(inst.iIndex()))
          && LoopType.WHILE.equals(getLoopType(cfg, ST, loop));
    }
    return false;
  }

  public static ISSABasicBlock getLoopSuccessor(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg, ISSABasicBlock controlBB, Loop loop) {
    // Lisa check if this method should be updated
    //    assert loopControls.containsKey(controlBB);
    //  Set<ISSABasicBlock> loopNodes = loopControls.get(controlBB);
    //  Graph<ISSABasicBlock> loopGraph = GraphSlicer.prune(cfg, n -> loopNodes.contains(n));
    //
    //  ISSABasicBlock loopBB = null;
    //  for (ISSABasicBlock nextBB : cfg.getNormalSuccessors(controlBB)) {
    //    if (DFS.getReachableNodes(loopGraph, Collections.singleton(nextBB))
    //        .contains(controlBB)) {
    //      assert loopBB == null;
    //      loopBB = nextBB;
    //    }
    //  }
    //
    //  assert loopBB != null;
    //  return loopBB;
    assert (loop != null);
    ISSABasicBlock loopBB = null;
    for (ISSABasicBlock nextBB : cfg.getNormalSuccessors(controlBB)) {
      if (loop.getAllBlocks().contains(nextBB)) {
        assert loopBB == null;
        loopBB = nextBB;
      }
    }
    return loopBB;
  }

  public static List<HashMap<ISSABasicBlock, List<Loop>>> updateLoopRelationship(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg, Map<ISSABasicBlock, Loop> loops) {
    // collect loop break and the jumps, key: loopBreaker,
    // value: the loops been jumped, not including the inner loop and outer loop
    HashMap<ISSABasicBlock, List<Loop>> jumpToTop = HashMapFactory.make();
    // collect loop break and the jumps, key: loopBreaker,
    // value: the loop that has middle loop part which will go to header of the parent
    HashMap<ISSABasicBlock, List<Loop>> returnToParentHeader = HashMapFactory.make();
    // collect loop break and the jumps, key: loopBreaker,
    // value: the loops been jumped, not includes the inner loop, but includes the outer most loop
    HashMap<ISSABasicBlock, List<Loop>> jumpToOutside = HashMapFactory.make();
    // collect loop break and the jumps, key: loopBreaker,
    // value: the loop, usually is the top one, that will jump to outside tail
    HashMap<ISSABasicBlock, List<Loop>> returnToOutsideTail = HashMapFactory.make();
    // collect loop break and the jumps, key: loopBreaker,
    // value: the nest loop hierarchy that share same loop control
    HashMap<ISSABasicBlock, List<Loop>> sharedLoopControl = HashMapFactory.make();

    // if there are only one loop, there wont be any nested loops
    if (loops.size() < 2)
      return Arrays.asList(jumpToTop, jumpToOutside, sharedLoopControl, returnToParentHeader);

    // order loops by header from large to small
    List<Loop> sortedLoops =
        loops.values().stream()
            .sorted(
                (a, b) -> {
                  return b.getLoopHeader().getNumber() - a.getLoopHeader().getNumber();
                })
            .collect(Collectors.toList());

    HashMap<Loop, Loop> childParentMap = HashMapFactory.make();
    for (int i = 0; i < sortedLoops.size() - 1; i++) {
      ISSABasicBlock header = sortedLoops.get(i).getLoopHeader();
      // Per each loop, check if it's header belongs to another loop, if yes, mark them as nested
      for (int j = i + 1; j < sortedLoops.size(); j++) {
        if (sortedLoops.get(j).getAllBlocks().contains(header)) {
          sortedLoops.get(j).addLoopNested(sortedLoops.get(i));
          childParentMap.put(sortedLoops.get(i), sortedLoops.get(j));
          break;
        }
      }
    }

    sortedLoops.forEach(
        ll -> {
          // check if there are any loop has no loop breakers
          if (ll.getLoopBreakers().size() < 1) {
            System.out.println("Unsupported: no loop breakers - " + ll);
            return;
          }
          // for most cases, no need to check loop breakers for top level loops
          if (!childParentMap.containsKey(ll)) {
            for (ISSABasicBlock loopExit : ll.getLoopExits()) {
              // There's a case where the top loop breaker will jump to the tail of outside
              ISSABasicBlock loopBreaker = ll.getLoopBreakerByExit(loopExit);
              Optional<Entry<Loop, Loop>> nestedLoop =
                  childParentMap.entrySet().stream()
                      .filter(
                          entry ->
                              entry.getValue().equals(ll)
                                  && entry.getKey().getLoopExits().contains(loopBreaker))
                      .findFirst();
              if (!ll.isLastBlock(loopBreaker) && nestedLoop.isPresent()) {
                assert !returnToOutsideTail.containsKey(loopExit);
                List<Loop> jumpPath = new ArrayList<>();
                jumpPath.add(ll);
                jumpPath.add(nestedLoop.get().getKey());
                returnToOutsideTail.put(loopExit, jumpPath);
              }
            }
            return;
          }

          // start to check loop breakers to see if there are any jump more than one layers
          // since each loop breaker only has one loop exit, use loop exit here to ease the search
          for (ISSABasicBlock loopExit : ll.getLoopExits()) {
            // first, loop exit should not belongs to it's direct parent
            if (!childParentMap.get(ll).getAllBlocks().contains(loopExit)) {
              // second, find out the parent and add it into jumpPath
              Loop currentLoop = childParentMap.get(ll);
              // create jump path from outer loop to inner loop
              List<Loop> jumpPath = new ArrayList<>();
              jumpPath.add(currentLoop);
              // third, try to find out the loops been jumped over besides currentLoop
              while (childParentMap.containsKey(currentLoop)) {
                if (!childParentMap.get(currentLoop).getAllBlocks().contains(loopExit)) {
                  currentLoop = childParentMap.get(currentLoop);
                  jumpPath.add(0, currentLoop);
                } else {
                  break;
                }
              }
              if (!childParentMap.containsKey(jumpPath.get(0))) {
                // if the jumpPath includes top ones
                if (jumpPath.get(0).getLoopControl().equals(ll.getLoopBreakerByExit(loopExit))) {
                  sharedLoopControl.put(ll.getLoopBreakerByExit(loopExit), jumpPath);
                } else {
                  jumpToOutside.put(ll.getLoopBreakerByExit(loopExit), jumpPath);
                }
              } else {
                jumpToTop.put(ll.getLoopBreakerByExit(loopExit), jumpPath);
              }

              System.out.println(
                  "This is an example of jump from inner loop to outer-most" + jumpPath);
            } else if ((cfg.getNormalSuccessors(loopExit)
                        .contains(childParentMap.get(ll).getLoopHeader())
                    && !childParentMap
                        .get(ll)
                        .getLoopControl()
                        .equals(ll.getLoopBreakerByExit(loopExit))
                    && childParentMap.get(ll).isLastBlockOfMiddlePart(loopExit))
                || (ll.getLoopExits().size() > 1
                    && gotoHeader(cfg, childParentMap.get(ll), loopExit)
                    // TODO: not sure how to tell a regular return to top by PERFORM and GOTO top
                    && ll.getLoopControl().equals(childParentMap.get(ll).getLoopControl()))) {
              // there's a case where level 2 loop jump to the header of level 1 loop from loop
              // breaker which is other than loop control, then it
              // should be similar to jumpToTop
              // create jump path from outer loop to inner loop
              // For indirect jump, need to check all successors and the number of loop exits
              assert !returnToParentHeader.containsKey(ll.getLoopBreakerByExit(loopExit));
              returnToParentHeader.put(
                  ll.getLoopBreakerByExit(loopExit), Collections.singletonList(ll));
            }
          }
        });

    // The value only contains the middle loops who will be jumped over
    System.out.println("====loop jumps to top:\n" + jumpToTop);
    System.out.println("====loop jumps to outside:\n" + jumpToOutside);
    // The value will contain the loops that share same loop control which means outer loop will be
    // included while inner loop not
    System.out.println("====loop shared control:\n" + sharedLoopControl);
    // The value will contain the loop that will jump back to it's parent header, so that inner loop
    // and middle loop(if any, usually only one value for this case) will be included
    System.out.println("====loop return to parent header from middle:\n" + returnToParentHeader);
    // The value will contain the loop that will jump back to it's parent tail, so that top loop
    // will be the only element in the path
    System.out.println("====loop return to outside tail from inner:\n" + returnToOutsideTail);
    return Arrays.asList(jumpToTop, jumpToOutside, sharedLoopControl, returnToParentHeader);
  }

  private static boolean gotoHeader(
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg, Loop loop, ISSABasicBlock block) {
    // check if all branches will goto loop header
    boolean result = true;
    Collection<ISSABasicBlock> nextBBs = cfg.getNormalSuccessors(block);
    for (ISSABasicBlock next : nextBBs) {
      if (loop.getLoopHeader().equals(next)) {
        continue;
      } else if (loop.getAllBlocks().contains(next)) {
        result = gotoHeader(cfg, loop, next);
      } else {
        result = false;
      }
    }
    return result;
  }
}
