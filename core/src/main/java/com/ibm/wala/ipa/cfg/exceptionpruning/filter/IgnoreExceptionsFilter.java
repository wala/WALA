package com.ibm.wala.ipa.cfg.exceptionpruning.filter;

import com.ibm.wala.ipa.cfg.exceptionpruning.ExceptionFilter;
import com.ibm.wala.ipa.cfg.exceptionpruning.FilteredException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * For filtering specific exceptions.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public class IgnoreExceptionsFilter implements ExceptionFilter<SSAInstruction> {
  private final Collection<FilteredException> toBeIgnored;

  /** All given exceptions and subclasses will be ignored. */
  public IgnoreExceptionsFilter(Collection<TypeReference> toBeIgnored) {
    this.toBeIgnored = new ArrayList<>();

    this.addAll(toBeIgnored);
  }

  /** The given exception and subclasses will be ignored. */
  public IgnoreExceptionsFilter(TypeReference toBeIgnored) {
    this.toBeIgnored = new ArrayList<>();

    this.addAll(Collections.singletonList(toBeIgnored));
  }

  private void addAll(Collection<TypeReference> toBeIgnored) {
    for (final TypeReference ignored : toBeIgnored) {
      this.toBeIgnored.add(new FilteredException(ignored, FilteredException.FILTER_SUBCLASSES));
    }
  }

  @Override
  public boolean alwaysThrowsException(SSAInstruction instruction) {
    return false;
  }

  @Override
  public Collection<FilteredException> filteredExceptions(SSAInstruction instruction) {
    return this.toBeIgnored;
  }
}
