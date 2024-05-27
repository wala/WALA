package com.ibm.wala.util.nullability;

import org.jspecify.annotations.Nullable;

public class NullabilityUtil {

  public static class CastToNonNullFailedException extends RuntimeException {}

  public static <T> T castToNonNull(@Nullable T obj) {
    if (obj == null) {
      throw new CastToNonNullFailedException();
    }
    return obj;
  }
}
