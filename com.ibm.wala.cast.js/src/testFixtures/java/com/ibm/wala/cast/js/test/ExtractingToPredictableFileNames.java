package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.html.DomLessSourceExtractor;
import com.ibm.wala.cast.js.html.JSSourceExtractor;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ExtractingToPredictableFileNames implements AutoCloseable {

  private final boolean savedUseTempName = JSSourceExtractor.USE_TEMP_NAME;
  private final Path savedOutputFileDirectory = DomLessSourceExtractor.OUTPUT_FILE_DIRECTORY;

  public ExtractingToPredictableFileNames() {
    configure(false, Paths.get("build"));
  }

  @Override
  public void close() {
    configure(savedUseTempName, savedOutputFileDirectory);
  }

  private static void configure(boolean useTempName, Path outputFileDirectory) {
    JSSourceExtractor.USE_TEMP_NAME = useTempName;
    DomLessSourceExtractor.OUTPUT_FILE_DIRECTORY = outputFileDirectory;
  }
}
