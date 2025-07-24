package com.ibm.wala.core.util.collections;

import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Iterator2Iterable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

@SuppressWarnings("Convert2MethodRef")
public class Sweep {

  @Benchmark
  public void usingIterator2IterableMake(Blackhole blackHole) {
    for (Object item : Iterator2Iterable.make(EmptyIterator.instance())) {
      blackHole.consume(item);
    }
  }

  @Benchmark
  public void usingIterator2IterableOfLambda(Blackhole blackHole) {
    for (Object item : Iterator2Iterable.of(() -> EmptyIterator.instance())) {
      blackHole.consume(item);
    }
  }

  @Benchmark
  public void usingIterator2IterableOfMethodReference(Blackhole blackHole) {
    for (Object item : Iterator2Iterable.of(EmptyIterator::instance)) {
      blackHole.consume(item);
    }
  }

  @Benchmark
  public void usingLambda(Blackhole blackHole) {
    for (Object item : (Iterable<Object>) () -> EmptyIterator.instance()) {
      blackHole.consume(item);
    }
  }

  @Benchmark
  public void usingMethodReference(Blackhole blackHole) {
    for (Object item : (Iterable<Object>) EmptyIterator::instance) {
      blackHole.consume(item);
    }
  }

  @Benchmark
  public void usingForEachRemainingLambda(Blackhole blackHole) {
    EmptyIterator.instance().forEachRemaining(item -> blackHole.consume(item));
  }

  @Benchmark
  public void usingForEachRemainingMethodReference(Blackhole blackHole) {
    EmptyIterator.instance().forEachRemaining(blackHole::consume);
  }
}
