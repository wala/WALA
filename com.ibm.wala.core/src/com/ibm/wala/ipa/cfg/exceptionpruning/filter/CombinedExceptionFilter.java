package com.ibm.wala.ipa.cfg.exceptionpruning.filter;

import java.util.Collection;
import java.util.LinkedList;

import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;
import com.ibm.wala.ssa.SSAInstruction;

/**
 * Use this class to combine multiple {@link ExceptionFilter}
 *
 * @author Stephan Gocht <stephan@gobro.de>
 */
public class CombinedExceptionFilter implements ExceptionFilter<SSAInstruction> {
	private final Collection<ExceptionFilter<SSAInstruction>> exceptionFilter;

	public CombinedExceptionFilter() {
		this.exceptionFilter = new LinkedList<>();
	}

	public CombinedExceptionFilter(
			Collection<ExceptionFilter<SSAInstruction>> exceptionFilter) {
		this.exceptionFilter = exceptionFilter;
	}

	public boolean add(ExceptionFilter<SSAInstruction> e) {
		return this.exceptionFilter.add(e);
	}

	public boolean addAll(
			Collection<? extends ExceptionFilter<SSAInstruction>> c) {
		return this.exceptionFilter.addAll(c);
	}

	@Override
	public boolean alwaysThrowsException(SSAInstruction instruction) {
		boolean result = false;

		for (final ExceptionFilter<SSAInstruction> filter : this.exceptionFilter) {
			result |= filter.alwaysThrowsException(instruction);
		}

		return result;
	}

	@Override
	public Collection<FilteredException> filteredExceptions(
			SSAInstruction instruction) {
		final LinkedList<FilteredException> result = new LinkedList<>();
		for (final ExceptionFilter<SSAInstruction> filter : this.exceptionFilter) {
			result.addAll(filter.filteredExceptions(instruction));
		}
		return result;
	}
}
