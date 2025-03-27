/* IBM Confidential
 * OCO Source Materials
 * 5737-B16
 * © Copyright IBM Corp. 2025
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 */
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAUnspecifiedExprInstruction;
import com.ibm.wala.types.TypeReference;

/**
 * The conditional statement that will play a role of <code>SSAUnspecifiedInstruction</code>
 *
 * @param <T> The type of the payload.
 */
public class SSAUnspecifiedConditionalExprInstruction<T> extends SSAUnspecifiedExprInstruction<T> {

  /** */
  private String thenPhrase;

  /** */
  private String elsePhrase;

  /**
   * Create a new Uninterpreted Expression defining result as some un-parsed payload.
   *
   * @param iindex the instruction index
   * @param result the expression result's value number
   * @param resultType the type of the result
   * @param payload the payload to be placed in a CAstPrimitive node
   */
  public SSAUnspecifiedConditionalExprInstruction(
      int iindex,
      int result,
      TypeReference resultType,
      T payload,
      String thenPhrase,
      String elsePhrase) {
    super(iindex, result, resultType, payload);
    this.thenPhrase = thenPhrase;
    this.elsePhrase = elsePhrase;
  }

  /**
   * @return the phrase name
   */
  public String getThenPhrase() {
    return thenPhrase;
  }

  /**
   * @return the phrase name
   */
  public String getElsePhrase() {
    return elsePhrase;
  }
}
