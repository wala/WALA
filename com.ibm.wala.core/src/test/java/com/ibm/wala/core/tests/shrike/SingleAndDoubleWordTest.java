package com.ibm.wala.core.tests.shrike;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.PopInstruction;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("UnconstructableJUnitTestCase")
public class SingleAndDoubleWordTest extends WalaTestCase {
  private final String klass = "shrike/SingleAndDoubleWord";

  private final String testJarLocation;
  private OfflineInstrumenter instrumenter;
  private Path instrumentedJarLocation;
  private List<ClassInstrumenter> classInstrumenters;

  protected SingleAndDoubleWordTest(String testJarLocation) {
    this.testJarLocation = testJarLocation;
  }

  public SingleAndDoubleWordTest() {
    this(getClasspathEntry("testSubjects").split(File.pathSeparator)[0]);
  }

  @Before
  public void setOfflineInstrumenter() throws IOException {
    // Create a temporary file for the shrike instrumented output
    instrumentedJarLocation = Files.createTempFile("wala-test", ".jar");
    instrumentedJarLocation.toFile().deleteOnExit();

    // Initialize the OfflineInstrumenter loading the class file specified in
    // 'klass' above
    instrumenter = new OfflineInstrumenter();
    instrumenter.addInputClass(
        new File(testJarLocation), new File(testJarLocation + File.separator + klass + ".class"));
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
  public void testSingleWord() throws IOException, InvalidClassFileException {
    // Find the method data in which patches should be applied
    String signature = "L" + klass + ";.popSingleWord()V";
    MethodData methodData = getMethodData(signature);

    // Find the first PopInstruction in the method
    IInstruction[] instructions = methodData.getInstructions();
    Integer index = getFirstPopInstructionIndex(instructions);

    // Set the instruction to read the popped count
    PopInstruction popInstruction = (PopInstruction) instructions[index];

    // Assert correct read pop count (1) as expected by the method name
    Assert.assertEquals(1, popInstruction.getPoppedCount(), 0d);

    // Replace the original pop instruction with the same we just read.
    // We simply need to do anything here since only modified classes are
    // written to the jar again
    MethodEditor me = new MethodEditor(methodData);
    me.beginPass();
    // Replacing the original pop-Instruction forces shrike to re-write the
    // method instead of just copying the unchanged instructions
    me.replaceWith(
        index,
        new MethodEditor.Patch() {
          @Override
          public void emitTo(Output w) {
            w.emit(popInstruction);
          }
        });
    me.applyPatches();
    me.endPass();

    // Write the altered bytecode to jar file
    write();

    // Read the saved application from shrike again and verify that the
    // pop instruction has still the same value
    int poppedCount = getPopInstructionSize(signature, index);

    // Assure that we still pop 1 element
    Assert.assertEquals(1, poppedCount, 0d);
  }

  @Test
  public void testDoubleWord() throws IOException, InvalidClassFileException {
    // Find the method data in which patches should be applied
    String signature = "L" + klass + ";.popDoubleWord()V";
    MethodData methodData = getMethodData(signature);

    // Find the first PopInstruction in the method
    IInstruction[] instructions = methodData.getInstructions();
    Integer index = getFirstPopInstructionIndex(instructions);

    // Set the instruction to read the popped count
    PopInstruction popInstruction = (PopInstruction) instructions[index];

    // Assert correct read pop count (2) as expected by the method name
    Assert.assertEquals(2, popInstruction.getPoppedCount(), 0d);

    // Replace the original pop instruction with the same we just read.
    // We simply need to do anything here since only modified classes are
    // written to the jar again
    MethodEditor me = new MethodEditor(methodData);
    me.beginPass();
    // Replacing the original pop-Instruction forces shrike to re-write the
    // method instead of just copying the unchanged instructions
    me.replaceWith(
        index,
        new MethodEditor.Patch() {
          @Override
          public void emitTo(Output w) {
            w.emit(PopInstruction.make(2));
          }
        });
    me.applyPatches();
    me.endPass();

    // Write the altered bytecode to jar file
    write();

    // Read the saved application from shrike again and verify that the
    // pop instruction has still the same value
    int poppedCount = getPopInstructionSize(signature, index);

    // Assure that we still pop 2 elements
    Assert.assertEquals(2, poppedCount, 0d);
  }

  private void write() throws IllegalStateException, IOException, InvalidClassFileException {
    // Write all classes regardless if they were changed or not
    for (ClassInstrumenter ci2 : classInstrumenters) {
      ClassWriter cw = ci2.emitClass();
      instrumenter.outputModifiedClass(ci2, cw);
    }

    // Finally write the instrumented jar
    instrumenter.close();
  }

  private void setValidationInstrumenter() throws IOException {
    // Reuse the instrumenter variable to just read the previously instrumented
    // file.
    instrumenter = new OfflineInstrumenter();
    instrumenter.addInputJar(instrumentedJarLocation.toFile());
    instrumenter.beginTraversal();

    // To be able to reuse all classes from shrike save them in a new list
    classInstrumenters = new ArrayList<>();
    ClassInstrumenter ci = null;
    while ((ci = instrumenter.nextClass()) != null) {
      classInstrumenters.add(ci);
    }
  }

  private int getPopInstructionSize(String signature, int index)
      throws IllegalStateException, IOException, InvalidClassFileException {
    setValidationInstrumenter();

    // Find the method data which contains the altered method body
    MethodData methodData = getMethodData(signature);

    // Find the ConstantInstruction given by index. This one should be validated
    // to hold the new value
    IInstruction[] instructions = methodData.getInstructions();
    IInstruction instruction = instructions[index];

    // Check that the instruction type has not been changed
    Assert.assertTrue(instruction instanceof PopInstruction);

    // The type type should be the same as well
    PopInstruction instruction2 = (PopInstruction) instruction;

    return instruction2.getPoppedCount();
  }

  private Integer getFirstPopInstructionIndex(IInstruction[] instructions) {
    // Iterate all instructions to find the first which is a ConstantInstruction
    // and has the correct type
    for (int index = 0; index < instructions.length; index++) {
      IInstruction instruction = instructions[index];
      if (instruction instanceof PopInstruction) {
        return index;
      }
    }
    Assert.fail("No PopInstruction found");
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
    Assert.fail("Method data not found. Check the signature");
    return null;
  }
}
