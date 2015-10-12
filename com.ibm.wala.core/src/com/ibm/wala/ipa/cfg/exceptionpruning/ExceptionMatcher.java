package com.ibm.wala.ipa.cfg.exceptionpruning;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;

/**
 * Helper class to check if an exception is part of a set of filtered
 * exceptions.
 *
 * @author Stephan Gocht <stephan@gobro.de>
 *
 */
public class ExceptionMatcher {
	/**
	 * @param thrownExceptions
	 * @param filteredExceptions
	 * @param cha
	 * @return true, iff thrownExceptions is part of filteredExceptions
	 */
	public static boolean isFiltered(
			Collection<TypeReference> thrownExceptions,
			Collection<FilteredException> filteredExceptions, ClassHierarchy cha) {
		final ExceptionMatcher matcher = new ExceptionMatcher(thrownExceptions,
				filteredExceptions, cha);
		return matcher.areAllExceptionsIgnored();
	}

	private Set<TypeReference> ignoreExact;
	private Set<TypeReference> ignoreSubclass;
	private ClassHierarchy cha;

	private final boolean areAllExceptionsIgnored;

	private ExceptionMatcher(Collection<TypeReference> thrownExceptions,
			Collection<FilteredException> filteredExceptions, ClassHierarchy cha) {
		this.ignoreExact = new HashSet<>();
		this.ignoreSubclass = new HashSet<>();
		this.cha = cha;

		this.fillIgnore(filteredExceptions);

		this.areAllExceptionsIgnored = this
				.allExceptionsIgnored(thrownExceptions);

		this.free();
	}

	private boolean allExceptionsIgnored(
			Collection<TypeReference> thrownExceptions) {
		boolean allExceptionsIgnored = true;
		for (final TypeReference exception : thrownExceptions) {
			allExceptionsIgnored &= this.isFiltered(exception);
			if (!allExceptionsIgnored) {
				break;
			}
		}
		return allExceptionsIgnored;
	}

	private boolean areAllExceptionsIgnored() {
		return this.areAllExceptionsIgnored;
	}

	private void fillIgnore(Collection<FilteredException> filteredExceptions) {
		for (final FilteredException filteredException : filteredExceptions) {
			final TypeReference exception = filteredException.getException();

			this.ignoreExact.add(exception);
			if (filteredException.isSubclassFiltered()) {
				this.ignoreSubclass.add(exception);
			}
		}
	}

	private void free() {
		this.ignoreExact = null;
		this.ignoreSubclass = null;
		this.cha = null;
	}

	/**
	 * Check if the exception itself is filtered or if it is derived from a
	 * filtered exception.
	 *
	 * @param exception
	 * @return if the exception is filtered
	 */
	private boolean isFiltered(TypeReference exception) {
		boolean isFiltered = false;
		if (this.ignoreExact.contains(exception)) {
			isFiltered = true;
		} else {
			for (final TypeReference ignoreException : this.ignoreSubclass) {
				final IClass exceptionClass = this.cha.lookupClass(exception);
				final IClass ignoreClass = this.cha
						.lookupClass(ignoreException);
				if (this.cha.isAssignableFrom(ignoreClass, exceptionClass)) {
					isFiltered = true;
					break;
				}
			}
		}

		return isFiltered;
	}
}
