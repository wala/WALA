package com.ibm.wala.shrike.instrumentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.shrikeCT.SourceFileReader;

public class CodeScraper implements ClassFileTransformer {

  private static final String prefix = System.getProperty("java.io.tmpdir") + File.separator + "loggedClasses" + File.separator + System.currentTimeMillis();
  
  static {
    System.err.println("scraping to " + prefix);
    (new File(prefix)).mkdirs();
  }
  
  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) throws IllegalClassFormatException {
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
      if (className == null || sourceFile == null || !sourceFile.endsWith("java") || true) try {
        String log = prefix + File.separator + reader.getName() + ".class";
        (new File(log)).getParentFile().mkdirs();
        try (final FileOutputStream f = new FileOutputStream(log)) {
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
