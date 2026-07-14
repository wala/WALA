package org.scandroid.synthmethod;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class DefaultSCanDroidOptionsTest {

  @Test
  void dumpString() {
    assertThat(
            DefaultSCanDroidOptions.dumpString(
                new DefaultSCanDroidOptions() {
                  @Override
                  public URI getClasspath() {
                    return URI.create("file:///dummy.apk");
                  }

                  @Override
                  public URI getAndroidLibrary() {
                    return URI.create("file:///dummy-android.jar");
                  }

                  @Override
                  public URI getSummariesURI() {
                    return URI.create("file:///dummy-summaries.xml");
                  }
                }))
        .isEqualTo(
            """
                DefaultSCanDroidOptions [\
                pdfCG()=false, \
                pdfPartialCG()=false, \
                pdfOneLevelCG()=false, \
                systemToApkCG()=false, \
                stdoutCG()=true, \
                includeLibrary()=true, \
                separateEntries()=false, \
                ifdsExplorer()=false, \
                addMainEntrypoints()=false, \
                useThreadRunMain()=false, \
                stringPrefixAnalysis()=false, \
                testCGBuilder()=false, \
                useDefaultPolicy()=false, \
                getClasspath()=file:///dummy.apk, \
                getFilename()=dummy.apk, \
                getAndroidLibrary()=file:///dummy-android.jar, \
                getReflectionOptions()=NONE, \
                getSummariesURI()=file:///dummy-summaries.xml, \
                classHierarchyWarnings()=false, \
                cgBuilderWarnings()=false\
                ]""");
  }
}
