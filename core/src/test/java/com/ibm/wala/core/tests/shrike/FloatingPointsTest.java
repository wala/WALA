package com.ibm.wala.core.tests.shrike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.shrike.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrike.shrikeBT.Constants;
import com.ibm.wala.shrike.shrikeBT.IInstruction;
import com.ibm.wala.shrike.shrikeBT.MethodData;
import com.ibm.wala.shrike.shrikeBT.MethodEditor;
import com.ibm.wala.shrike.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrike.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrike.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrike.shrikeCT.ClassReader;
import com.ibm.wala.shrike.shrikeCT.ClassWriter;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("UnconstructableJUnitTestCase")
public class FloatingPointsTest extends WalaTestCase {
  private final String klass = "shrike/FloatingPoints";

  private final String testJarLocation =
      getClasspathEntry(String.join(File.separator, "classes", "java", "testSubjects"));
  private OfflineInstrumenter instrumenter;
  private Path instrumentedJarLocation;
  private List<ClassInstrumenter> classInstrumenters;

  public FloatingPointsTest() {}

  @BeforeEach
  public void setOfflineInstrumenter(@TempDir final Path tmpDir) throws IOException {
    // Create a temporary file for the shrike instrumented output
    instrumentedJarLocation = Files.createTempFile(tmpDir, "wala-test", ".jar");

    // Initialize the OfflineInstrumenter loading the class file specified in
    // 'klass' above
    instrumenter = new OfflineInstrumenter();
    instrumenter.addInputClass(
        new File(testJarLocation), new File(testJarLocation + "/" + klass + ".class"));
    instrumenter.setPassUnmodifiedClasses(false);
    instrumenter.setOutputJar(instrumentedJarLocation.toFile());
    instrumenter.beginTraversal();

    // To be able to reuse all classes from shrike save them in a list
    classInstrumenters = new ArrayList<>();
    ClassInstrumenter ci = null;
    while ((ci = instrumenter.nextClass()) != null) {
      classInstrumenters.add(ci);
    }
  }

  @Test
  public void testDouble() throws IOException, InvalidClassFileException {
    double amountToAdd = 2.5d;

    // Find the method data in which patches should be applied
    String signature = "L" + klass + ";.doubble()V";
    MethodData methodData = getMethodData(signature);

    // Find the first ConstantInstruction of type double (D)
    IInstruction[] instructions = methodData.getInstructions();
    Integer index = getFirstConstantInstructionIndex(instructions, Constants.TYPE_double);

    // Since such an instruction was found, read the original value and add some
    // specified amount
    ConstantInstruction constantDoubleInstruction = (ConstantInstruction) instructions[index];
    double value = (double) constantDoubleInstruction.getValue();
    double newValue = value + amountToAdd;

    // Replace the original constant instruction with another which pushes the
    // new value (from above)
    MethodEditor me = new MethodEditor(methodData);
    me.beginPass();
    me.replaceWith(
        index,
        new MethodEditor.Patch() {
          @Override
          public void emitTo(Output w) {
            w.emit(ConstantInstruction.make(newValue));
          }
        });
    me.applyPatches();
    me.endPass();

    // Write the altered bytecode to jar file
    write();

    // Read the saved application from shrike again and verify that the
    // altered constant instruction has the new value
    double readValue =
        (double) getConstantInstructionValue(signature, index, Constants.TYPE_double);

    // And finally (and most important) compare the value
    assertThat(newValue).isEqualTo(readValue, offset(0d));
  }

  @Test
  public void testFloat() throws IOException, InvalidClassFileException {
    float amountToAdd = 2.5f;

    // Find the method data in which patches should be applied
    String signature = "L" + klass + ";.floatt()V";
    MethodData methodData = getMethodData(signature);

    // Find the first ConstantInstruction of type float (F)
    IInstruction[] instructions = methodData.getInstructions();
    Integer index = getFirstConstantInstructionIndex(instructions, Constants.TYPE_float);

    // Since such an instruction was found, read the original value and add some
    // specified amount
    ConstantInstruction constantDoubleInstruction = (ConstantInstruction) instructions[index];
    float value = (float) constantDoubleInstruction.getValue();
    float newValue = value + amountToAdd;

    // Replace the original constant instruction with another which pushes the
    // new value (from above)
    MethodEditor me = new MethodEditor(methodData);
    me.beginPass();
    me.replaceWith(
        index,
        new MethodEditor.Patch() {
          @Override
          public void emitTo(Output w) {
            w.emit(ConstantInstruction.make(newValue));
          }
        });
    me.applyPatches();
    me.endPass();

    // Write the altered bytecode to jar file
    write();

    // Read the saved application from shrike again and verify that the
    // altered constant instruction has the new value
    float readValue = (float) getConstantInstructionValue(signature, index, Constants.TYPE_float);

    // And finally (and most important) compare the value
    assertThat(readValue).isEqualTo(newValue, offset(0f));
  }

  private void write() throws IllegalStateException, IOException, InvalidClassFileException {
    try {
      // Write all modified classes
      for (ClassInstrumenter ci2 : classInstrumenters) {
        if (ci2.isChanged()) {
          ClassWriter cw = ci2.emitClass();
          instrumenter.outputModifiedClass(ci2, cw);
        }
      }
    } finally {
      // Finally write the instrumented jar
      instrumenter.close();
    }
  }

  private void setValidationInstrumenter() throws IOException {
    // Reuse the instrumenter variable to just read the previously instrumented
    // file.
    instrumenter = new OfflineInstrumenter();
    instrumenter.addInputJar(instrumentedJarLocation.toFile());
    instrumenter.beginTraversal();

    try {
      // To be able to reuse all classes from shrike save them in a new list
      classInstrumenters = new ArrayList<>();
      ClassInstrumenter ci = null;
      while ((ci = instrumenter.nextClass()) != null) {
        classInstrumenters.add(ci);
      }
    } finally {
      instrumenter.close();
    }
  }

  private Object getConstantInstructionValue(String signature, int index, String type)
      throws IllegalStateException, IOException, InvalidClassFileException {
    setValidationInstrumenter();

    // Find the method data which contains the altered method body
    MethodData methodData = getMethodData(signature);

    // Find the ConstantInstruction given by index. This one should be validated
    // to hold the new value
    IInstruction[] instructions = methodData.getInstructions();
    IInstruction instruction = instructions[index];

    // Check that the instruction type has not been changed
    // The type type should be the same as well
    ConstantInstruction instruction2 =
        assertThat(instruction).asInstanceOf(type(ConstantInstruction.class)).actual();
    assertThat(type).matches(t -> t.contentEquals(instruction2.getType()));

    return instruction2.getValue();
  }

  private Integer getFirstConstantInstructionIndex(IInstruction[] instructions, String type) {
    // Iterate all instructions to find the first which is a ConstantInstruction
    // and has the correct type
    for (int index = 0; index < instructions.length; index++) {
      IInstruction instruction = instructions[index];
      System.out.println(instruction);
      if (instruction instanceof ConstantInstruction) {
        ConstantInstruction constantInstruction = (ConstantInstruction) instruction;
        if (constantInstruction.getType().contentEquals(type)) {
          return index;
        }
      }
    }
    fail("No ConstantInstruction with type '" + type + "' found");
    return null;
  }

  private MethodData getMethodData(String signature) throws InvalidClassFileException {
    // Look up loaded methods and compare them by signature
    for (ClassInstrumenter ci : classInstrumenters) {
      ClassReader cr = ci.getReader();

      for (int m = 0; m < cr.getMethodCount(); m++) {
        MethodData md = ci.visitMethod(m);
        // TODO .toString() on MethodData might not be safe in the future to
        // generate a valid jvm signature. Build up the signature from fields
        // in MethodData and compare it afterwards.
        if (signature.contentEquals(md.toString())) {
          return md;
        }
      }
    }
    fail("Method data not found. Check the signature");
    return null;
  }
}
