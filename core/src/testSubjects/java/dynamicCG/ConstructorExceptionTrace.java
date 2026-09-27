/*
 * Copyright (c) 2026 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package dynamicCG;

public class ConstructorExceptionTrace {
  static class FailingBase {
    FailingBase() {
      throw new IllegalStateException();
    }
  }

  static class FailBefore extends FailingBase {
    FailBefore() {
      super();
    }
  }

  static class FailAfter {
    FailAfter() {
      throw new IllegalStateException();
    }
  }

  static void afterFailBefore() {}

  static void afterFailAfter() {}

  static void afterCaughtReturn() {}

  static void catchAndReturn() {
    try {
      new FailAfter();
    } catch (IllegalStateException e) {
      // The method exit is the first trace event after the constructor unwinds.
    }
  }

  public static void main(String[] args) {
    try {
      new FailBefore();
    } catch (IllegalStateException e) {
      afterFailBefore();
    }
    try {
      new FailAfter();
    } catch (IllegalStateException e) {
      afterFailAfter();
    }
    catchAndReturn();
    afterCaughtReturn();
  }
}
