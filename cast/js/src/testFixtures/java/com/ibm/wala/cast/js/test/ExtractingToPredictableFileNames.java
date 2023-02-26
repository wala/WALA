package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.html.DomLessSourceExtractor;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Temporarily configures JavaScript source extractors to use predictable HTML file names and
 * locations.
 *
 * <p>Predictable names are good for use in automated regression tests that expect to match specific
 * file names in analyses results. This class implements {@link AutoCloseable}, making it suitable
 * for use in {@code try}-with-resources statements: the original settings for HTML file names and
 * locations will be restored when the {@code try}-with-resources statement concludes.
 */
public class ExtractingToPredictableFileNames implements AutoCloseable {

  /** Original {@link JSSourceExtractor#USE_TEMP_NAME} setting to be restored later */
  private final boolean savedUseTempName = JSSourceExtractor.USE_TEMP_NAME;

  /** Original {@link DomLessSourceExtractor#OUTPUT_FILE_DIRECTORY} setting to be restored later */
  private final Path savedOutputFileDirectory = DomLessSourceExtractor.OUTPUT_FILE_DIRECTORY;

  /**
   * Reconfigures {@link JSSourceExtractor} to not use temporary file names, and reconfigures {@link
   * DomLessSourceExtractor} to place HTML files in the {@code build} subdirectory of the current
   * working directory.
   */
  public ExtractingToPredictableFileNames() {
    configure(false, Paths.get("build"));
  }

  /**
   * Restores {@link JSSourceExtractor} and {@link DomLessSourceExtractor} settings for generated
   * HTML file names to those that were in place when this instance was created.
   */
  @Override
  public void close() {
    configure(savedUseTempName, savedOutputFileDirectory);
  }

  /**
   * Changes the current {@link JSSourceExtractor#USE_TEMP_NAME} and {@link
   * DomLessSourceExtractor#OUTPUT_FILE_DIRECTORY}.
   *
   * @param useTempName the new value for {@link JSSourceExtractor#USE_TEMP_NAME}
   * @param outputFileDirectory the new value for {@link
   *     DomLessSourceExtractor#OUTPUT_FILE_DIRECTORY}
   */
  private static void configure(boolean useTempName, Path outputFileDirectory) {
    JSSourceExtractor.USE_TEMP_NAME = useTempName;
    DomLessSourceExtractor.OUTPUT_FILE_DIRECTORY = outputFileDirectory;
  }
}
