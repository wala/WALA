package com.ibm.wala.classLoader;

import static com.ibm.wala.util.collections.IteratorAssertions.assertExhausted;
import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.util.collections.Iterator2Collection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;

class JarModuleIteratorTest {

  @Test
  void emptyStreamModuleIteratorThrowsOnExhaustion() throws IOException {
    try (JarStreamModule module = streamModule()) {
      assertExhausted(module.getEntries());
    }
  }

  @Test
  void streamModuleIteratorThrowsAfterLastEntry() throws IOException {
    try (JarStreamModule module = streamModule("Example.java")) {
      Iterator<ModuleEntry> iterator = module.getEntries();

      assertThat(iterator.next().getName()).isEqualTo("Example.java");
      assertExhausted(iterator);
    }
  }

  @Test
  void streamModuleIteratorReturnsEveryEntryBeforeExhaustion() throws IOException {
    try (JarStreamModule module = streamModule("First.java", "Last.java")) {
      Iterator<ModuleEntry> iterator = module.getEntries();

      assertThat(Iterator2Collection.toList(iterator))
          .extracting(ModuleEntry::getName)
          .containsExactlyInAnyOrder("First.java", "Last.java");
      assertExhausted(iterator);
    }
  }

  @Test
  void emptyNestedModuleIteratorThrowsOnExhaustion() throws IOException {
    assertExhausted(nestedModule().getEntries());
  }

  @Test
  void nestedModuleIteratorThrowsAfterLastEntry() throws IOException {
    Iterator<ModuleEntry> iterator = nestedModule("Example.java").getEntries();

    assertThat(iterator.next().getName()).isEqualTo("Example.java");
    assertExhausted(iterator);
  }

  @Test
  void nestedModuleIteratorReturnsEveryEntryBeforeExhaustion() throws IOException {
    Iterator<ModuleEntry> iterator = nestedModule("First.java", "Last.java").getEntries();

    assertThat(Iterator2Collection.toList(iterator))
        .extracting(ModuleEntry::getName)
        .containsExactlyInAnyOrder("First.java", "Last.java");
    assertExhausted(iterator);
  }

  private static JarStreamModule streamModule(String... names) throws IOException {
    return new JarStreamModule(new ByteArrayInputStream(jarBytes(names)));
  }

  private static AbstractNestedJarFileModule nestedModule(String... names) throws IOException {
    byte[] bytes = jarBytes(names);
    return new AbstractNestedJarFileModule(Collections::emptyIterator) {
      @Override
      protected InputStream getNestedContents() {
        return new ByteArrayInputStream(bytes);
      }
    };
  }

  private static byte[] jarBytes(String... names) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (JarOutputStream output = new JarOutputStream(bytes)) {
      for (String name : names) {
        output.putNextEntry(new JarEntry(name));
        output.write(0);
        output.closeEntry();
      }
    }
    return bytes.toByteArray();
  }
}
