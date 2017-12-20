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

/*
 * CRTable.java
 *
 * Created on 10. Mai 2005, 09:05
 */

package com.ibm.wala.sourcepos;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * This class represents the CharacterRangeTable attribute.
 * 
 * @see CRTData
 * @see CRTFlags
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public final class CRTable extends PositionsAttribute {

  /** Stores the attribute name of this attribute */
  public static final String ATTRIBUTE_NAME = "CharacterRangeTable";

  private static final String WARN_CRT_ENTRIES_CONTRADICTORY = "CRT entries %1$s and %2$s are contradictory.";

  private static final String ERR_NO_CRT_ENTRY = "No CRT entry found for program counter %1$s.";

  /** Stores the CharacterRangeTable data */
  private CRTData[] crt;

  /**
   * Creates a new instance of CRTable.
   * 
   * @param data
   *          the byte array containing the attribute
   * @throws IOException
   *           An IOException is thrown if the attribute can't be read.
   */
  public CRTable(byte[] data) throws IOException {
    super(data);
    if (Debug.PRINT_CHARACTER_RANGE_TABLE) {
      Debug.info(this.toString());
    }
  }

  @Override
  protected final void readData(DataInputStream in) throws IOException {
    assert in != null;
    short crt_length = in.readShort();
    crt = new CRTData[crt_length];
    for (int i = 0; i < crt_length; i++) {
      short pc_start_index = in.readShort();
      short pc_end_index = in.readShort();
      int source_start_position = in.readInt();
      int source_end_position = in.readInt();
      short flags = in.readShort();
      try {
        crt[i] = new CRTData(pc_start_index, pc_end_index, source_start_position, source_end_position, flags);
      } catch (InvalidCRTDataException e) {
        LinkedList<Object> l = e.getData();
        if (l == null)
          l = new LinkedList<>();
        l.addFirst(i);
        Debug.warn(e.getMessage(), l.toArray());
      }
    }
  }

  /**
   * Returns the source positions for the given index in the code array of the
   * code attribute.
   * 
   * @param pc
   *          the index in the code array of the code attribute
   * @return the most precise source position range
   */
  public final Range getSourceInfo(int pc) {
    CRTData sourceInfo = null;
    int sourceInfoIndex = 0;
    for (int i = 0; i < crt.length; i++) {
      if ((crt[i] != null) && (crt[i].isInRange(pc))) {
        if ((sourceInfo != null) && !sourceInfo.matches(crt[i]))
          Debug.warn(WARN_CRT_ENTRIES_CONTRADICTORY, new Object[] { sourceInfoIndex, i });
        if ((sourceInfo == null) || crt[i].isMorePrecise(sourceInfo)) {
          sourceInfo = crt[i];
          sourceInfoIndex = i;
        }
      }
    }
    if (sourceInfo == null) {
      Debug.error(ERR_NO_CRT_ENTRY, pc);
      try {
        short short_pc = (short) (pc & 0xFFFF);
        sourceInfo = new CRTData(short_pc, short_pc, 0, 0, CRTFlags.CRT_SOURCE_INFO);
      } catch (InvalidCRTDataException e) {
        assert false;
      }
    }
    return sourceInfo.getSourceInfo();
  }

  @Override
  public String toString() {
    if (crt == null) {
      return "<undefined>";
    }

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < crt.length; i++) {
      sb.append(i + " -> ");
      sb.append(crt[i] == null ? "<null>" : crt[i]);
      sb.append("\n");
    }

    return sb.toString();
  }

}
