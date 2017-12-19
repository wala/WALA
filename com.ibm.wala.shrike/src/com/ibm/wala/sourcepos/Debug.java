/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.sourcepos;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 * 
 */
public final class Debug {

  public static final boolean PRINT_CHARACTER_RANGE_TABLE = false;

  private Debug() {
  }

  public enum LogLevel {
    DEBUG(0), INFO(1), WARN(2), ERROR(3);

    private final Integer priority;

    private LogLevel(int priority) {
      this.priority = priority;
    }

    public boolean isHigherPriority(LogLevel l) {
      return priority > l.priority;
    }
  }

  private static PrintStream OUT_STREAM = System.out;

  private static final Map<LogLevel, LogStream> logStreams = new HashMap<>();

  private static Set<LogLevel> allowed = new HashSet<>();
  static {
    for (int i = 0; i < LogLevel.values().length; i++) {
      allowed.add(LogLevel.values()[i]);
    }
  }

  public static void setLogFile(String file) throws FileNotFoundException {
    if (file != null && file.equals("console")) {
      OUT_STREAM = System.out;
    } else if (file != null) {
      OUT_STREAM = new PrintStream(file);
    } else {
      OUT_STREAM = null;
    }
  }

  /**
   * Set to log all events with the given or higher priority
   * 
   * @param level
   */
  public static void setMinLogLevel(LogLevel level) {
    for (LogLevel l : LogLevel.values()) {
      if (l == level || l.isHigherPriority(level)) {
        allow(l);
      } else {
        ignore(l);
      }
    }
  }

  public static void noLogging() {
    for (LogLevel l : LogLevel.values()) {
      ignore(l);
    }
  }

  public static void fullLogging() {
    for (LogLevel l : LogLevel.values()) {
      allow(l);
    }
  }

  public static void error(String str, Object... obj) {
    log(LogLevel.ERROR, str + "\n", obj);
  }

  public static void warn(String str, Object... obj) {
    log(LogLevel.WARN, str + "\n", obj);
  }

  public static void info(String str, Object... obj) {
    log(LogLevel.INFO, str + "\n", obj);
  }

  public static void debug(String str, Object... obj) {
    log(LogLevel.DEBUG, str + "\n", obj);
  }

  public static void error(String str) {
    log(LogLevel.ERROR, str + "\n");
  }

  public static void warn(String str) {
    log(LogLevel.WARN, str + "\n");
  }

  public static void logTime() {
    Date date = new Date();
    log(LogLevel.INFO, "Current time: " + date + "\n");
  }

  public static void info(String str) {
    log(LogLevel.INFO, str + "\n");
  }

  public static void appendInfo(String str) {
    if (OUT_STREAM != null && allowed.contains(LogLevel.INFO)) {
      OUT_STREAM.print(str);
    }
  }

  public static void debug(String str) {
    log(LogLevel.DEBUG, str + "\n");
  }

  public static void error(Throwable t) {
    log(LogLevel.ERROR, t);
  }

  public static void warn(Throwable t) {
    log(LogLevel.WARN, t);
  }

  public static void info(Throwable t) {
    log(LogLevel.INFO, t);
  }

  public static void debug(Throwable t) {
    log(LogLevel.DEBUG, t);
  }

  public static void allow(LogLevel level) {
    allowed.add(level);
  }

  public static void ignore(LogLevel level) {
    allowed.remove(level);
  }

  private static void log(LogLevel level, Throwable exc) {
    StringWriter sw = new StringWriter();
    exc.printStackTrace(new PrintWriter(sw));
    log(level, sw.toString());
  }

  private static void log(LogLevel level, String str) {
    if (OUT_STREAM != null && allowed.contains(level)) {
      OUT_STREAM.print("[" + level + "] " + str);
    }
  }

  private static void log(LogLevel level, String str, Object... obj) {
    if (OUT_STREAM != null && allowed.contains(level)) {
      OUT_STREAM.format("[" + level + "] " + str, obj);
    }
  }

  private static final class LogStream extends PrintStream {

    private final LogLevel level;

    LogStream(OutputStream out, LogLevel level) {
      super(out);
      this.level = level;
    }

    @Override
    public void print(String str) {
      Debug.log(level, str);
    }

    @Override
    public void println(String str) {
      Debug.log(level, str + "\n");
    }
  }

  public static PrintStream getStream(LogLevel level) {
    LogStream logStream = logStreams.get(level);
    if (OUT_STREAM != null && logStream == null) {
      logStream = new LogStream(OUT_STREAM, level);
      logStreams.put(level, logStream);
    }

    return logStream;
  }
}
