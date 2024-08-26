package com.ibm.wala.cast.ir.toSource;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.collections.Pair;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A loop part is a set of all nodes in a control-flow cycle (allowing repetitions in the cycle) in
 * the CFG
 *
 * <p>It's read from source AST
 */
public class LoopPart {

  /** Header of the loop */
  private ISSABasicBlock loopHeader;

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

  public ISSABasicBlock getLoopHeader() {
    return loopHeader;
  }

  public void setLoopHeader(ISSABasicBlock loopHeader) {
    this.loopHeader = loopHeader;
  }

  public ISSABasicBlock getLoopControl() {
    return loopControl;
  }

  public void setLoopControl(ISSABasicBlock loopControl) {
    this.loopControl = loopControl;
  }

  public Set<ISSABasicBlock> getAllBlocks() {
    return allBlocks;
  }

  public void setAllBlocks(Set<ISSABasicBlock> allBlocks) {
    this.allBlocks = allBlocks;
  }

  public Set<ISSABasicBlock> getLoopBreakers() {
    assert (loopBreakers != null);
    return loopBreakers.stream().map(pair -> pair.fst).collect(Collectors.toSet());
  }

  public Set<Pair<ISSABasicBlock, ISSABasicBlock>> getLoopBreakersExits() {
    assert (loopBreakers != null);
    return loopBreakers;
  }

  public void setLoopBreakers(Set<Pair<ISSABasicBlock, ISSABasicBlock>> loopBreakers) {
    this.loopBreakers = loopBreakers;
  }

  public Set<ISSABasicBlock> getLoopExits() {
    assert (loopBreakers != null);
    return loopBreakers.stream().map(pair -> pair.snd).collect(Collectors.toSet());
  }
}
