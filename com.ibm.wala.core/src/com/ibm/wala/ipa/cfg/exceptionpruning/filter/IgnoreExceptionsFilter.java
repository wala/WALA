package com.ibm.wala.ipa.cfg.exceptionpruning.filter;

import java.util.Collection;
import java.util.LinkedList;

import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

/**
 * For filtering specific exceptions.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class IgnoreExceptionsFilter implements ExceptionFilter<SSAInstruction> {
	private final Collection<FilteredException> toBeIgnored;

	/**
	 * All given exceptions and subclasses will be ignored.
	 * 
	 * @param toBeIgnored
	 */
	public IgnoreExceptionsFilter(Collection<TypeReference> toBeIgnored) {
		this.toBeIgnored = new LinkedList<>();

		this.addAll(toBeIgnored);
	}

	/**
	 * The given exception and subclasses will be ignored.
	 */
	public IgnoreExceptionsFilter(TypeReference toBeIgnored) {
		this.toBeIgnored = new LinkedList<>();

		final LinkedList<TypeReference> list = new LinkedList<>();
		list.add(toBeIgnored);

		this.addAll(list);
	}

	private void addAll(Collection<TypeReference> toBeIgnored) {
		for (final TypeReference ignored : toBeIgnored) {
			this.toBeIgnored.add(new FilteredException(ignored,
					FilteredException.FILTER_SUBCLASSES));
		}
	}

	@Override
	public boolean alwaysThrowsException(SSAInstruction instruction) {
		return false;
	}

	@Override
	public Collection<FilteredException> filteredExceptions(
			SSAInstruction instruction) {
		return this.toBeIgnored;
	}
}
