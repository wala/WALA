/*
 * PositionsAttribute.java
 *
 * Created on 23. Mai 2005, 19:31
 */

package com.ibm.wala.sourcepos;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * This is the super class of all position attributes.
 * 
 * @author Siegfried Weber
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
abstract class PositionsAttribute {

  /**
   * Creates a new instance of PositionsAttribute.
   * 
   * @param data
   *          the byte array containing the attribute
   * @throws IOException
   *           An IOException is thrown if the attribute can't be read or
   *           {@code data} is null.
   */
  PositionsAttribute(byte[] data) throws IOException {
    if (data == null)
      throw new IOException();
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
    readData(in);
  }

  /**
   * Reads the attribute data from the input stream.
   * 
   * @param in
   *          the input stream
   * @throws IOException
   *           if the input stream cannot be read.
   */
  protected abstract void readData(DataInputStream in) throws IOException;
}
