package com.ibm.wala.cast.util;

import java.io.IOException;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

public final class BOMInputStreamFactory {
  private BOMInputStreamFactory() {}

  public static BOMInputStream make(java.io.InputStream origin) throws IOException {
    return BOMInputStream.builder()
        .setInputStream(origin)
        .setInclude(false)
        .setByteOrderMarks(
            ByteOrderMark.UTF_8,
            ByteOrderMark.UTF_16LE,
            ByteOrderMark.UTF_16BE,
            ByteOrderMark.UTF_32LE,
            ByteOrderMark.UTF_32BE)
        .get();
  }
}
