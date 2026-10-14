/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Tests that an {@code AstDynamicPropertyClass} can be queried from several threads at once.
 *
 * <p>Looking up a field name that a dynamic-property class does not declare creates the field, in
 * the topmost class of its hierarchy. So {@code getField} writes the same map that {@code
 * getDeclaredInstanceFields} iterates, and a lookup on any class in the hierarchy can modify the
 * root's map while another thread iterates it.
 */
public class DynamicPropertyClassConcurrencyTest {

  private static final TypeName ROOT = TypeName.string2TypeName("LRoot");
  private static final TypeName SUB = TypeName.string2TypeName("LSub");

  private static final int EXISTING_FIELDS = 64;
  private static final int WRITERS = 4;
  private static final int READERS = 4;
  private static final int NAMES_PER_WRITER = 20_000;

  /**
   * Field lookups that create fields, racing with iteration over the declared fields, neither throw
   * nor hide a field that existed before the race.
   */
  @Test
  public void testConcurrentLookupAndIteration() throws Exception {
    TestLoader loader = new TestLoader();
    IClass root = loader.new CoreClass(ROOT, null, loader, null);
    IClass sub = loader.new CoreClass(SUB, ROOT, loader, null);

    Set<Atom> existing = new HashSet<>();
    for (int i = 0; i < EXISTING_FIELDS; i++) {
      Atom name = Atom.findOrCreateUnicodeAtom("existing" + i);
      root.getField(name);
      existing.add(name);
    }

    ExecutorService executor = Executors.newFixedThreadPool(WRITERS + READERS);
    CountDownLatch start = new CountDownLatch(1);
    AtomicBoolean writing = new AtomicBoolean(true);
    ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<String> missing = new ConcurrentLinkedQueue<>();
    List<Future<?>> writers = new ArrayList<>();
    List<Future<?>> readers = new ArrayList<>();

    try {
      for (int w = 0; w < WRITERS; w++) {
        int writer = w;
        writers.add(
            executor.submit(
                () -> {
                  try {
                    start.await();
                    for (int i = 0; i < NAMES_PER_WRITER; i++)
                      sub.getField(Atom.findOrCreateUnicodeAtom("w" + writer + "_" + i));
                  } catch (Throwable t) {
                    failures.add(t);
                  }
                }));
      }

      for (int r = 0; r < READERS; r++) {
        readers.add(
            executor.submit(
                () -> {
                  try {
                    start.await();
                    while (writing.get()) {
                      Set<Atom> seen = new HashSet<>();
                      for (IField field : root.getDeclaredInstanceFields())
                        seen.add(field.getName());
                      for (Atom name : existing)
                        if (!seen.contains(name)) missing.add(name.toString());
                    }
                  } catch (Throwable t) {
                    failures.add(t);
                  }
                }));
      }

      start.countDown();
      for (Future<?> writer : writers) writer.get(2, TimeUnit.MINUTES);
      writing.set(false);
      for (Future<?> reader : readers) reader.get(1, TimeUnit.MINUTES);
    } finally {
      writing.set(false);
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    assertThat(failures).isEmpty();
    assertThat(missing).isEmpty();

    Collection<IField> fields = root.getDeclaredInstanceFields();
    assertThat(fields).hasSize(EXISTING_FIELDS + WRITERS * NAMES_PER_WRITER);
  }

  /** Concurrent lookups of one new name all return the same field. */
  @Test
  public void testConcurrentLookupsOfOneNameAgree() throws Exception {
    TestLoader loader = new TestLoader();
    IClass root = loader.new CoreClass(ROOT, null, loader, null);
    IClass sub = loader.new CoreClass(SUB, ROOT, loader, null);
    ExecutorService executor = Executors.newFixedThreadPool(WRITERS);

    try {
      for (int i = 0; i < 2_000; i++) {
        Atom name = Atom.findOrCreateUnicodeAtom("shared" + i);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<IField>> lookups = new ArrayList<>();
        for (int w = 0; w < WRITERS; w++)
          lookups.add(
              executor.submit(
                  () -> {
                    start.await();
                    return sub.getField(name);
                  }));
        start.countDown();
        IField first = lookups.get(0).get(1, TimeUnit.MINUTES);
        for (Future<IField> lookup : lookups)
          assertThat(lookup.get(1, TimeUnit.MINUTES)).isSameAs(first);
        assertThat(root.getField(name)).isSameAs(first);
      }
    } finally {
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.MINUTES);
    }
  }

  /** A minimal concrete {@link CAstAbstractModuleLoader}, enough to create classes. */
  private static final class TestLoader extends CAstAbstractModuleLoader {

    TestLoader() {
      super(null, null);
    }

    @Override
    public Language getLanguage() {
      return JavaLanguage.get();
    }

    @Override
    public ClassLoaderReference getReference() {
      return ClassLoaderReference.Application;
    }

    @Override
    public SSAInstructionFactory getInstructionFactory() {
      return JavaLanguage.get().instructionFactory();
    }

    @Override
    protected TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry m, List<Module> modules) {
      throw new UnsupportedOperationException("not needed for this test");
    }

    @Override
    protected boolean shouldTranslate(CAstEntity entity) {
      return false;
    }

    @Override
    protected TranslatorToIR initTranslator(Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
      throw new UnsupportedOperationException("not needed for this test");
    }
  }
}
