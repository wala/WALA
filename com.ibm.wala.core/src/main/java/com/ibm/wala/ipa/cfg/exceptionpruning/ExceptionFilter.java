package com.ibm.wala.ipa.cfg.exceptionpruning;

import java.util.Collection;

/**
 * To filter exceptions you can implement this interface and use it in combination with {@link
 * ExceptionFilter2EdgeFilter}. For more Details see package-info.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public interface ExceptionFilter<Instruction> {
  /** @return if the instruction does always throw an exception */
  public boolean alwaysThrowsException(Instruction instruction);

  /** @return a list of exceptions, which have to be filtered for the given instruction */
  public Collection<FilteredException> filteredExceptions(Instruction instruction);
}
