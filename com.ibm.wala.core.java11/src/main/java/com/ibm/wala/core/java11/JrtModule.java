package com.ibm.wala.core.java11;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.shrike.ShrikeClassReaderHandle;
import com.ibm.wala.core.util.strings.ImmutableByteArray;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

public class JrtModule implements Module {
  final Path root;

  public JrtModule(String module) throws IOException {
    root =
        java.nio.file.FileSystems.newFileSystem(
                java.net.URI.create("jrt:/"), java.util.Collections.emptyMap())
            .getPath("modules", module);
  }

  @Override
  public String toString() {
    return "[module " + root.toString() + "]";
  }

  private Stream<? extends ModuleEntry> rec(Path pp) {
    Module me = this;
    try {
      return Files.list(pp)
          .flatMap(
              s -> {
                if (Files.isDirectory(s)) {
                  return rec(s);
                } else {
                  return Stream.of(
                      new ModuleEntry() {

                        @Override
                        public String toString() {
                          StringBuilder sb = new StringBuilder("[");
                          if (isClassFile()) {
                            sb.append("class");
                          } else if (isSourceFile()) {
                            sb.append("source");
                          } else {
                            sb.append("file");
                          }
                          sb.append(" ").append(getName());
                          return sb.toString();
                        }

                        @Override
                        public String getName() {
                          return s.toString().substring(root.toString().length() + 1);
                        }

                        @Override
                        public boolean isClassFile() {
                          return getName().endsWith(".class");
                        }

                        @Override
                        public boolean isSourceFile() {
                          return getName().endsWith(".java");
                        }

                        @Override
                        public InputStream getInputStream() {
                          try {
                            return new ByteArrayInputStream(Files.readAllBytes(s));
                          } catch (IOException e) {
                            assert false : e;
                            return null;
                          }
                        }

                        @Override
                        public boolean isModuleFile() {
                          return false;
                        }

                        @Override
                        public Module asModule() {
                          assert false;
                          return null;
                        }

                        private String className = null;

                        @Override
                        public String getClassName() {
                          assert isClassFile();
                          if (className == null) {
                            ShrikeClassReaderHandle reader = new ShrikeClassReaderHandle(this);
                            try {
                              ImmutableByteArray name =
                                  ImmutableByteArray.make(reader.get().getName());
                              className = name.toString();
                            } catch (InvalidClassFileException e) {
                              e.printStackTrace();
                              assert false;
                            }
                          }
                          return className;
                        }

                        @Override
                        public Module getContainer() {
                          return me;
                        }
                      });
                }
              });
    } catch (IOException e) {
      assert false;
      return null;
    }
  }

  @Override
  public Iterator<? extends ModuleEntry> getEntries() {
    return rec(root).iterator();
  }
}
