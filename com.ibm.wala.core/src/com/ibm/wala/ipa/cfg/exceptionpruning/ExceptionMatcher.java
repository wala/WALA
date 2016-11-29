package com.ibm.wala.ipa.cfg.exceptionpruning;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;

/**
 * Helper class to check if an exception is part of a set of filtered
 * exceptions.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
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
	
	/**
	 * Returns all exceptions of thrownExceptions which are not filtered by filteredExceptions
	 * @param thrownExceptions
	 * @param filteredExceptions
	 * @param cha
	 * @return all exceptions of thrownExceptions which are not filtered by filteredExceptions
	 */
	public static Set<TypeReference> retainedExceptions(Collection<TypeReference> thrownExceptions,
      Collection<FilteredException> filteredExceptions, ClassHierarchy cha){
	   final ExceptionMatcher matcher = new ExceptionMatcher(thrownExceptions,
	        filteredExceptions, cha);
	    return matcher.getRetainedExceptions();
	}

	private Set<TypeReference> ignoreExact;
	private Set<TypeReference> ignoreSubclass;
	private final Set<TypeReference> retainedExceptions;
	private ClassHierarchy cha;

	private final boolean areAllExceptionsIgnored;

	private ExceptionMatcher(Collection<TypeReference> thrownExceptions,
			Collection<FilteredException> filteredExceptions, ClassHierarchy cha) {
		this.ignoreExact = new LinkedHashSet<>();
		this.ignoreSubclass = new LinkedHashSet<>();
		this.cha = cha;
		this.retainedExceptions = new LinkedHashSet<>();

		this.fillIgnore(filteredExceptions);

		this.computeRetainedExceptions(thrownExceptions);
		this.areAllExceptionsIgnored = this.retainedExceptions.isEmpty();

		this.free();
	}

	private void computeRetainedExceptions(Collection<TypeReference> thrownExceptions){
	   for (final TypeReference exception : thrownExceptions) {
	     if (!this.isFiltered(exception)) {
	       this.retainedExceptions.add(exception);
	     }	     
	   }
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

  public Set<TypeReference> getRetainedExceptions() {
    return retainedExceptions;
  }
}
