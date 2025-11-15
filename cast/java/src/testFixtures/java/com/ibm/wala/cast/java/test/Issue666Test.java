package com.ibm.wala.cast.java.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class Issue666Test extends IRTests {

  @Test
  public void testPeekErrorCase() throws CancelException, IOException {
    Pair<CallGraph, ?> result =
        runTest(
            singleTestSrc("PeekErrorCase"),
            rtJar,
            simpleTestEntryPoint("PeekErrorCase"),
            emptyList,
            true,
            null);

    assertThat(result).isNotNull();

    MethodReference cm =
        MethodReference.findOrCreate(
            TypeReference.findOrCreate(
                JavaSourceAnalysisScope.SOURCE, TypeName.string2TypeName("LPeekErrorCase")),
            Atom.findOrCreateUnicodeAtom("start"),
            Descriptor.findOrCreateUTF8(Language.JAVA, "()V"));

    assertThat(cm).isNotNull();
  }
}
