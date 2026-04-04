package com.ibm.wala.core.java11;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.shrike.ShrikeClassReaderHandle;
import com.ibm.wala.core.util.strings.ImmutableByteArray;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.collections.ComposedIterator;

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

  private Iterator<? extends ModuleEntry> rec(Path pp) throws IOException {
    Module me = this;
    List<Path> elts;
    try (var x = Files.list(pp)) {
      elts = x.collect(Collectors.toList());
    }
    return new ComposedIterator<>(elts.iterator()) {

      @Override
      public Iterator<? extends ModuleEntry> makeInner(Path outer) {
        if (Files.isDirectory(outer)) {
          try {
            return rec(outer);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        } else {
          return Collections.singleton(
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
                      return outer.toString().substring(root.toString().length() + 1);
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
                        return new ByteArrayInputStream(Files.readAllBytes(outer));
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
                          ImmutableByteArray name = ImmutableByteArray.make(reader.get().getName());
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
                  })
              .iterator();
        }
      }
    };
  }

  @Override
  public Iterator<? extends ModuleEntry> getEntries() {
    try {
      return rec(root);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
