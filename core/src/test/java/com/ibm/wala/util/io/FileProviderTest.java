/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.io;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.util.PlatformUtil;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class FileProviderTest {

  private void checkFile(String actual, String expected, String expectedPatternOnWindows) {
    if (PlatformUtil.onWindows()) {
      assertThat(actual).matches(expectedPatternOnWindows);
    } else {
      assertThat(actual).isEqualTo(expected);
    }
  }

  @Test
  public void testValidFile() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///c:/my/File.jar");
    String expected = "/c:/my/File.jar";
    // exercise:
    String actual = new FileProvider().filePathFromURL(url);
    // verify:
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testURLWithInvalidURIChars() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///[Eclipse]/File.jar");
    // exercise:
    String actual = new FileProvider().filePathFromURL(url);
    // verify:
    checkFile(actual, "/[Eclipse]/File.jar", "\\A/[A-Z]:/\\[Eclipse]/File.jar\\z");
  }

  @Test
  public void testURLWithSpace() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///With%20Space/File.jar");
    // exercise:
    String actual = new FileProvider().filePathFromURL(url);
    // verify:
    checkFile(actual, "/With Space/File.jar", "\\A/[A-Z]:/With Space/File\\.jar\\z");
  }
}
