package com.ibm.wala.ssa;

import com.ibm.wala.cfg.MinimalCFGAssert;
import java.util.Arrays;
import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;

/** Custom assertions for {@link IR} instances, for use in WALA's own tests. */
public class IRAssert extends AbstractObjectAssert<IRAssert, IR> {

  public IRAssert(IR actual) {
    super(actual, IRAssert.class);
  }

  @CheckReturnValue
  public static IRAssert assertThat(IR actual) {
    return new IRAssert(actual);
  }

  public ObjectAssert<SSAInstruction> instruction(int index) {
    isNotNull();
    SSAInstruction[] instructions = actual.getInstructions();
    Assertions.assertThat(index)
        .as("instruction index")
        .isBetween(0, Math.max(0, instructions.length - 1));
    return Assertions.assertThat(instructions[index])
        .as("instruction at index %d of %s", index, actual.getMethod());
  }

  public ListAssert<SSAInstruction> instructions() {
    isNotNull();
    return Assertions.assertThat(Arrays.asList(actual.getInstructions()))
        .as("instructions of %s", actual.getMethod());
  }

  public ListAssert<String> localNames(int instructionIndex, int valueNumber) {
    isNotNull();
    String[] names = actual.getLocalNames(instructionIndex, valueNumber);
    return Assertions.assertThat(names == null ? null : Arrays.asList(names))
        .as("local names for value %d at instruction %d", valueNumber, instructionIndex);
  }

  public AbstractIntegerAssert<?> constantInt(int valueNumber) {
    isNotNull();
    return Assertions.assertThat(actual.getSymbolTable().getIntValue(valueNumber))
        .as("constant value %d", valueNumber);
  }

  public MinimalCFGAssert<ISSABasicBlock> cfg() {
    isNotNull();
    return MinimalCFGAssert.assertThat(actual.getControlFlowGraph());
  }
}
