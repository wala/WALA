package com.ibm.wala.types.generics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class TypeSignatureTest {

  @Test
  void basicTypeArguments() {
    assertThat(
        TypeSignature.parseForTypeSignatures("<Ljava/lang/String;ILjava/lang/Object;>"),
        arrayContaining(is("Ljava/lang/String;"), is("I"), is("Ljava/lang/Object;")));
  }

  @Test
  void multiDimArray() {
    assertThat(
        TypeSignature.parseForTypeSignatures("<[[Ljava/lang/String;[[[J>"),
        arrayContaining(is("[[Ljava/lang/String;"), is("[[[J")));
  }

  @Test
  void wildcards() {
    assertThat(
        TypeSignature.parseForTypeSignatures("<B*J>"), arrayContaining(is("B"), is("*"), is("J")));
    assertThat(
        TypeSignature.parseForTypeSignatures("<+Ljava/lang/Object;>"),
        arrayContaining(is("+Ljava/lang/Object;")));
    assertThat(
        TypeSignature.parseForTypeSignatures("<-Ljava/lang/Double;BB>"),
        arrayContaining(is("-Ljava/lang/Double;"), is("B"), is("B")));
  }

  @Test
  void testAllGenericMethodSigs()
      throws IOException, ClassHierarchyException, InvalidClassFileException {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, "J2SEClassHierarchyExclusions.txt");
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    for (IClass klass : cha) {
      for (IMethod m : klass.getDeclaredMethods()) {
        if (m instanceof ShrikeCTMethod) {
          ShrikeCTMethod method = (ShrikeCTMethod) m;
          MethodTypeSignature methodTypeSignature = method.getMethodTypeSignature();
          if (methodTypeSignature != null) {
            String typeSigStr = methodTypeSignature.toString();
            for (int i = 0; i < typeSigStr.length(); i++) {
              if ((typeSigStr.charAt(i) == '<' && i != 0) || typeSigStr.charAt(i) == '(') {
                // parsing will automatically end at the matching '>' or ')'
                // this is just testing for crashes
                TypeSignature.parseForTypeSignatures(typeSigStr.substring(i));
              }
            }
          }
        }
      }
    }
  }
}
