package com.ibm.wala.ipa.cfg.exceptionpruning.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis;
import com.ibm.wala.analysis.arraybounds.ArrayOutOfBoundsAnalysis.UnnecessaryCheck;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

/**
 * Adapter for using {@link ArrayOutOfBoundsAnalysis}. This filter is filtering
 * ArrayOutOfBoundException, which can not occur.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ArrayOutOfBoundFilter implements ExceptionFilter<SSAInstruction> {
	private final ArrayOutOfBoundsAnalysis analysis;

	public ArrayOutOfBoundFilter(ArrayOutOfBoundsAnalysis analysis) {
		this.analysis = analysis;
	}

	@Override
	public boolean alwaysThrowsException(SSAInstruction instruction) {
		return false;
	}

	@Override
	public Collection<FilteredException> filteredExceptions(
			SSAInstruction instruction) {
		final UnnecessaryCheck unnecessary = this.analysis
				.getBoundsCheckNecessary().get(instruction);
		if (unnecessary == UnnecessaryCheck.BOTH) {

			final LinkedList<FilteredException> result = new LinkedList<>();
			result.add(new FilteredException(
					TypeReference.JavaLangArrayIndexOutOfBoundsException));

			return result;
		} else {

			return Collections.emptyList();
		}
	}
}
