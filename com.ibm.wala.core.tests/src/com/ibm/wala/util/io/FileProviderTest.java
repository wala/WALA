package com.ibm.wala.util.io;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

public class FileProviderTest {

  @Test
  public void testValidFile() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///c:/my/File.jar");
    String expected = "/c:/my/File.jar";
    // exercise:
    String actual = FileProvider.filePathFromURL(url);
    // verify:
    assertEquals(expected, actual);
  }
  
  
  @Test
  public void testURLWithInvalidURIChars() throws MalformedURLException {
    // setup:
    URL url = new URL("file:///[Eclipse]/File.jar");
    String expected = "/[Eclipse]/File.jar";
    // exercise:
    String actual = FileProvider.filePathFromURL(url);
    // verify:
    assertEquals(expected, actual);
  }

  
}
