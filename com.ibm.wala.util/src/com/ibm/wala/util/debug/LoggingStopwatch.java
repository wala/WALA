package com.ibm.wala.util.debug;

/**
 * A stop watch that prints log messages.
 * 
 * @author mschaefer
 *
 */
public class LoggingStopwatch {
  private long start;

  /**
   * Start the stopwatch.
   */
  public void start() {
    this.start = System.nanoTime();
  }

  /**
   * Mark the completion of a task, print the time it took to complete,
   * and optionally restart the stopwatch.
   * 
   * @param msg message to print
   * @param reset whether to restart the stopwatch
   * @return the elapsed time in milliseconds
   */
  public long mark(String msg, boolean reset) {
    long end = System.nanoTime();
    long elapsed = (end-start)/1000000;
    System.out.println(msg + ": " + elapsed + "ms");
    if(reset)
      start();
    return elapsed;
  }
  
  /**
   * Convenience method that invokes {@link #mark(String, boolean)} with {@code true}
   * as its second argument.
   */
  public long mark(String msg) {
    return mark(msg, true);
  }
}
