package com.ibm.wala.util.nullability;

import org.jspecify.annotations.Nullable;

/** Utility methods for targeted suppressions of NullAway warnings. */
public class NullabilityUtil {

  /** Exception thrown when a {@link #castToNonNull(Object)} call fails. */
  public static class CastToNonNullFailedException extends RuntimeException {

    private static final long serialVersionUID = -4118612881206393972L;
  }

  /**
   * Casts a reference to a {@code @NonNull} type, throwing an exception if the reference is null.
   *
   * @param obj the reference
   * @return the reference itself
   * @throws CastToNonNullFailedException if {@code obj} is null
   */
  public static <T> T castToNonNull(@Nullable T obj) throws CastToNonNullFailedException {
    if (obj == null) {
      throw new CastToNonNullFailedException();
    }
    return obj;
  }

  /**
   * Returns a null reference with a {@code @NonNull} type. Useful, e.g., where a method is allowed
   * to return null, but the NullAway type system can only express a {@code @NonNull} return type.
   *
   * @return a null reference
   */
  @SuppressWarnings("NullAway")
  public static <T extends @Nullable Object> T uncheckedNull() {
    return null;
  }
}
