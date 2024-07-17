package com.ibm.wala.util.nullability;

import org.jspecify.annotations.Nullable;

public class NullabilityUtil {

  public static class CastToNonNullFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  public static <T> T castToNonNull(@Nullable T obj) {
    if (obj == null) {
      throw new CastToNonNullFailedException();
    }
    return obj;
  }

  @SuppressWarnings("NullAway")
  public static <T extends @Nullable Object> T uncheckedNull() {
    return null;
  }
}
