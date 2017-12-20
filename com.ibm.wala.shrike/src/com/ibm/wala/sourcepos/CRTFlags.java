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
 * CRTFlags.java
 *
 * Created on 7. Juni 2005, 11:28
 */

package com.ibm.wala.sourcepos;

import java.util.LinkedList;

/**
 * This class represents the flags which a entry in the CharacterRangeTable can
 * have. The flags are bitwise ORed.
 * 
 * @see CRTData
 * @see CRTable
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public final class CRTFlags {

  /** Stores the value of the {@code CRT_SOURCE_INFO} flag. */
  static final short CRT_SOURCE_INFO = 0x0200;

  /** Stores the names of the flags. */
  private static final String[] flagNames = { "CRT_STATEMENT", // 0x0001
      "CRT_BLOCK", // 0x0002
      "CRT_ASSIGNMENT", // 0x0004
      "CRT_FLOW_CONTROLLER", // 0x0008
      "CRT_FLOW_TARGET", // 0x0010
      "CRT_INVOKE", // 0x0020
      "CRT_CREATE", // 0x0040
      "CRT_BRANCH_TRUE", // 0x0080
      "CRT_BRANCH_FALSE", // 0x0100
      "CRT_SOURCE_INFO" }; // 0x0200

  private static final String WARN_INVALID_FLAG = "Error at CRT entry %1$s: invalid flag %2$s";

  /** Stores the flags. */
  private final short flags;

  /**
   * Creates a new instance of CRTFlags.
   * 
   * @param flags
   *          the flags
   * @throws InvalidCRTDataException
   *           An InvalidCRTDataException is thrown if the flags are not valid.
   */
  CRTFlags(short flags) throws InvalidCRTDataException {
    this.flags = flags;
    if (!isFlagValid())
      throw new InvalidCRTDataException(WARN_INVALID_FLAG, Integer.toHexString(flags));
  }

  /**
   * Returns the flag names of this instance.
   * 
   * @return An array of Strings containing the flag names.
   */
  public final String[] getFlagNames() {
    LinkedList<String> names = new LinkedList<>();
    int index = 0;
    short tFlags = flags;
    while (tFlags > 0) {
      if (tFlags % 2 == 1) {
        if (index < flagNames.length)
          names.add(flagNames[index]);
        else {
          // assert false
          // because exception was thrown in the constructor.
          names.add("UNKNOWN (" + Integer.toHexString(2 << index) + ")");
        }
      }
      tFlags >>= 1;
      ++index;
    }
    return names.toArray(new String[names.size()]);
  }

  /**
   * Tests whether the flags are valid.
   * 
   * @return whether the flags are valid.
   */
  private boolean isFlagValid() {
    return 0 < flags && flags < 2 << flagNames.length - 1;
  }

}
