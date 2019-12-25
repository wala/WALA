/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released by the University of
 * California under the terms listed below.
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 *
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package demandpa;

class TestNastyPtrs {

  static class A {

    A next0;

    A next1;

    A next2;

    A next3;

    A next4;

    A next5;

    A next6;

    A next7;

    A next8;

    A next9;
  }

  public static void main(String[] args) {

    A a0 = new A();
    A a1 = new A();
    A a2 = new A();
    A a3 = new A();
    A a4 = new A();
    A a5 = new A();
    A a6 = new A();
    A a7 = new A();
    A a8 = new A();
    A a9 = new A();
    a0.next0 = a1;
    a1.next0 = a2;
    a2.next0 = a3;
    a3.next0 = a4;
    a4.next0 = a5;
    a5.next0 = a6;
    a6.next0 = a7;
    a7.next0 = a8;
    a8.next0 = a9;

    A x = a0;
    while (args[0] != null) {
      x = x.next0;
    }
    x.next0 = x;
    x.next1 = x;
    x.next2 = x;
    x.next3 = x;
    x.next4 = x;
    x.next5 = x;
    x.next6 = x;
    x.next7 = x;
    x.next8 = x;
    x.next9 = x;

    DemandPATestUtil.testThisVar(x);
  }
}
