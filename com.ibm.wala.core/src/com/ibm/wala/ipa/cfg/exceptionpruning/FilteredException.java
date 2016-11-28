package com.ibm.wala.ipa.cfg.exceptionpruning;

import com.ibm.wala.types.TypeReference;

/**
 * FilteredException represents either a single exception or an exception and
 * all its subclasses.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class FilteredException {
	public static final boolean FILTER_SUBCLASSES = true;
	private final TypeReference exception;

	private final boolean isSubclassFiltered;

	public FilteredException(TypeReference exception) {
		this(exception, false);
	}

	public FilteredException(TypeReference exception, boolean isSubclassFiltered) {
		super();
		this.exception = exception;
		this.isSubclassFiltered = isSubclassFiltered;
	}

	public TypeReference getException() {
		return this.exception;
	}

	public boolean isSubclassFiltered() {
		return this.isSubclassFiltered;
	}
}
