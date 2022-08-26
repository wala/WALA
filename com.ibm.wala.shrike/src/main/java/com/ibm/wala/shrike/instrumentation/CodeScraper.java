package com.ibm.wala.shrike.instrumentation;

import com.ibm.wala.shrike.shrikeCT.ClassReader;
import com.ibm.wala.shrike.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrike.shrikeCT.SourceFileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public class CodeScraper implements ClassFileTransformer {

  private static final Path prefix;

  static {
    try {
      prefix = Files.createTempDirectory("loggedClasses");
      prefix.toFile().deleteOnExit();
    } catch (final IOException problem) {
      throw new RuntimeException(problem);
    }
    System.err.println("scraping to " + prefix);
  }

  @Override
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer)
      throws IllegalClassFormatException {
    try {
      String sourceFile = null;
      ClassReader reader = new ClassReader(classfileBuffer);
      AttrIterator attrs = new ClassReader.AttrIterator();
      reader.initClassAttributeIterator(attrs);
      for (; attrs.isValid(); attrs.advance()) {
        if (attrs.getName().equals("SourceFile")) {
          SourceFileReader file = new SourceFileReader(attrs);
          int index = file.getSourceFileCPIndex();
          sourceFile = reader.getCP().getCPUtf8(index);
        }
      }
      if (className == null || sourceFile == null || !sourceFile.endsWith("java") || true)
        try {
          Path log = prefix.resolve(reader.getName() + ".class");
          try (final OutputStream f = Files.newOutputStream(log)) {
            f.write(classfileBuffer);
          }
        } catch (IOException e) {
          assert false : e;
        }

      return classfileBuffer;
    } catch (InvalidClassFileException e1) {
      e1.printStackTrace();
      throw new IllegalClassFormatException(e1.getLocalizedMessage());
    }
  }

  public static void premain(Instrumentation inst) {
    inst.addTransformer(new CodeScraper());
  }
}
