package com.ibm.wala.cast.ir.toSource;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** A loop is a set of nodes in loop parts that share the same loop header */
public class Loop {

  private ISSABasicBlock loopHeader;

  private Set<LoopPart> parts;

  private Set<Loop> nestedLoops = HashSetFactory.make();

  /**
   * The conditional branch which will control continue the loop or exit the loop It is usually the
   * first conditional branch in a loop (the last for do loop)
   */
  private ISSABasicBlock loopControl;

  /** All blocks of the loop part */
  private Set<ISSABasicBlock> allBlocks;

  /**
   * The blocks that have one control-edge to a block in the loop and one that is not in the loop is
   * called loop breaker This set will contain loop control to ease development The second value in
   * the pair is loop exit Which is the successors of loop breakers that go out of the loop
   */
  private Set<Pair<ISSABasicBlock, ISSABasicBlock>> loopBreakers;

  public Loop(ISSABasicBlock header) {
    assert (header != null);
    this.loopHeader = header;
    parts = HashSetFactory.make();
  }

  public ISSABasicBlock getLoopHeader() {
    return loopHeader;
  }

  public void addLoopPart(LoopPart part) {
    assert (part != null);
    assert loopHeader.equals(part.getLoopHeader());
    parts.add(part);
    reorg();
  }

  public void addLoopNested(Loop loop) {
    assert (loop != null);
    assert !loopHeader.equals(loop.getLoopHeader());
    nestedLoops.add(loop);
    reorg();
  }

  private void reorg() {
    // first, collect all blocks, should contain everything in parts and nested loops
    Set<ISSABasicBlock> all = HashSetFactory.make();
    if (nestedLoops != null && nestedLoops.size() > 0) {
      all.addAll(
          nestedLoops.stream()
              .map(loop -> loop.getAllBlocks())
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.toSet()));
    }
    all.addAll(
        parts.stream()
            .map(part -> part.getAllBlocks())
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toSet()));
    allBlocks =
        HashSetFactory.make(
            all.stream()
                .distinct()
                .sorted(
                    (a, b) -> {
                      return a.getFirstInstructionIndex() - b.getFirstInstructionIndex();
                    })
                .collect(Collectors.toSet()));

    // second, find out all loop breaks
    Set<Pair<ISSABasicBlock, ISSABasicBlock>> breakers =
        parts.stream()
            .map(part -> part.getLoopBreakersExits())
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toSet());
    Set<Pair<ISSABasicBlock, ISSABasicBlock>> shouldBeRemoved = HashSetFactory.make();
    breakers.forEach(
        pair -> {
          // If loop exit is in allBlocks then this should no longer be loop exit
          if (allBlocks.contains(pair.snd)) {
            shouldBeRemoved.add(pair);
          }
        });
    breakers.removeAll(shouldBeRemoved);
    loopBreakers = HashSetFactory.make(breakers);

    loopControl =
        getLoopBreakers().stream().min(Comparator.comparing(ISSABasicBlock::getNumber)).get();
  }

  public ISSABasicBlock getLoopControl() {
    assert (parts.size() > 0);
    return loopControl;
  }

  public Set<ISSABasicBlock> getLoopBreakers() {
    assert (parts.size() > 0);
    assert (loopBreakers != null);
    return loopBreakers.stream().map(pair -> pair.fst).collect(Collectors.toSet());
  }

  public Set<ISSABasicBlock> getAllBlocks() {
    assert (parts.size() > 0);
    return allBlocks;
  }

  public Set<ISSABasicBlock> getLoopExits() {
    assert (parts.size() > 0);
    assert (loopBreakers != null);
    return loopBreakers.stream().map(pair -> pair.snd).collect(Collectors.toSet());
  }

  /**
   * When there are more than one loop part, the last block will be the one for the loop instead of
   * the loop part
   *
   * @return If the given block is the last block of the loop
   */
  public boolean isLastBlock(ISSABasicBlock block) {
    assert (parts.size() > 0);
    return getLastBlock().equals(block);
  }

  /**
   * When there are more than one loop part, the last block will be the one for the loop instead of
   * the loop part
   *
   * @return The last block of the loop
   */
  public ISSABasicBlock getLastBlock() {
    assert (parts.size() > 0);
    return allBlocks.stream().max(Comparator.comparing(ISSABasicBlock::getNumber)).get();
  }

  @Override
  public String toString() {
    return "Loop: "
        + Arrays.toString(
            allBlocks.stream()
                .map(block -> block.getNumber())
                .collect(Collectors.toList())
                .toArray());
  }

  public boolean containsNestedLoop() {
    return nestedLoops.size() > 0;
  }

  public boolean containsNestedLoop(Loop loop) {
    return nestedLoops.contains(loop);
  }

  public ISSABasicBlock getLoopBreakerByExit(ISSABasicBlock exit) {
    return loopBreakers.stream()
        .filter(pair -> exit.equals(pair.snd))
        .map(pair -> pair.fst)
        .findFirst()
        .get();
  }

  public boolean isLastBlockOfMiddlePart(ISSABasicBlock lastBlock) {
    if (parts.size() > 1) {
      List<ISSABasicBlock> allLastBlocks =
          parts.stream()
              .map(
                  pp ->
                      pp.getAllBlocks().stream()
                          .max(Comparator.comparing(ISSABasicBlock::getNumber))
                          .get())
              .collect(Collectors.toList());
      return allLastBlocks.contains(lastBlock)
          && !allLastBlocks.stream()
              .max(Comparator.comparing(ISSABasicBlock::getNumber))
              .get()
              .equals(lastBlock);
    }
    return false;
  }
}
