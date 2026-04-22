package com.ibm.wala.core.java11;

import com.ibm.wala.core.util.config.AnalysisScopeReader;

/**
 * @deprecated Use {@link AnalysisScopeReader}; support for {@code jrt} entries is now built in.
 */
@Deprecated(forRemoval = true, since = "1.7.2")
public class Java9AnalysisScopeReader extends AnalysisScopeReader {

  public static final Java9AnalysisScopeReader instance = new Java9AnalysisScopeReader();
}
