package com.ibm.wala.ipa.cfg.exceptionpruning;

import com.ibm.wala.types.TypeReference;

/**
 * FilteredException represents either a single exception or an exception and all its subclasses.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public record FilteredException(TypeReference exception, boolean isSubclassFiltered) {
  public static final boolean FILTER_SUBCLASSES = true;

  public FilteredException(TypeReference exception) {
    this(exception, false);
  }

  /**
   * @deprecated Use {@link #exception()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public TypeReference getException() {
    return exception();
  }
}
