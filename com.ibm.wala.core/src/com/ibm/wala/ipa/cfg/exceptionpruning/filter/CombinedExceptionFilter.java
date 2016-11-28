package com.ibm.wala.ipa.cfg.exceptionpruning.filter;

import java.util.Collection;
import java.util.LinkedList;

import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;

/**
 * Use this class to combine multiple {@link ExceptionFilter}
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public class CombinedExceptionFilter<Instruction> implements ExceptionFilter<Instruction> {
	private final Collection<ExceptionFilter<Instruction>> exceptionFilter;

	public CombinedExceptionFilter() {
		this.exceptionFilter = new LinkedList<>();
	}

	public CombinedExceptionFilter(
			Collection<ExceptionFilter<Instruction>> exceptionFilter) {
		this.exceptionFilter = exceptionFilter;
	}

	public boolean add(ExceptionFilter<Instruction> e) {
		return this.exceptionFilter.add(e);
	}

	public boolean addAll(
			Collection<? extends ExceptionFilter<Instruction>> c) {
		return this.exceptionFilter.addAll(c);
	}

	@Override
	public boolean alwaysThrowsException(Instruction instruction) {
		boolean result = false;

		for (final ExceptionFilter<Instruction> filter : this.exceptionFilter) {
			result |= filter.alwaysThrowsException(instruction);
		}

		return result;
	}

	@Override
	public Collection<FilteredException> filteredExceptions(
			Instruction instruction) {
		final LinkedList<FilteredException> result = new LinkedList<>();
		for (final ExceptionFilter<Instruction> filter : this.exceptionFilter) {
			result.addAll(filter.filteredExceptions(instruction));
		}
		return result;
	}
}
