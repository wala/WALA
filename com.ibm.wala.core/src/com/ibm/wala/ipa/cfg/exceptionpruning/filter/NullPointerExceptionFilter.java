package com.ibm.wala.ipa.cfg.exceptionpruning.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import com.ibm.wala.analysis.nullpointer.IntraproceduralNullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.NullPointerState.State;
import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

/**
 * Adapter for {@link IntraproceduralNullPointerAnalysis}. This filter is filtering
 * NullPointerException, which can not occur.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class NullPointerExceptionFilter implements
ExceptionFilter<SSAInstruction> {
	private final IntraproceduralNullPointerAnalysis analysis;

	public NullPointerExceptionFilter(IntraproceduralNullPointerAnalysis analysis) {
		this.analysis = analysis;
	}

	@Override
	public boolean alwaysThrowsException(SSAInstruction instruction) {
		return this.analysis.nullPointerExceptionThrowState(instruction) == State.NULL;
	}

	@Override
	public Collection<FilteredException> filteredExceptions(
			SSAInstruction instruction) {
		if (this.analysis.nullPointerExceptionThrowState(instruction) == State.NOT_NULL) {
			final LinkedList<FilteredException> result = new LinkedList<>();
			result.add(new FilteredException(
					TypeReference.JavaLangNullPointerException));
			return result;
		} else {
			return Collections.emptyList();
		}
	}
}
