package com.ibm.wala.ipa.cfg.exceptionpruning;

import java.util.Collection;

/**
 * To filter exceptions you can implement this interface and use it in
 * combination with {@link ExceptionFilter2EdgeFilter}. For more Details see
 * package-info.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 * @param <Instruction>
 */
public interface ExceptionFilter<Instruction> {
	/**
	 *
	 * @param instruction
	 * @return if the instruction does always throw an exception
	 */
	public boolean alwaysThrowsException(Instruction instruction);

	/**
	 *
	 * @param instruction
	 * @return a list of exceptions, which have to be filtered for the given
	 *         instruction
	 */
	public Collection<FilteredException> filteredExceptions(
			Instruction instruction);
}
