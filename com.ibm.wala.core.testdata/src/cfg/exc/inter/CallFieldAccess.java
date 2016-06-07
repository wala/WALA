package cfg.exc.inter;

import cfg.exc.intra.B;
import cfg.exc.intra.FieldAccess;
import cfg.exc.intra.FieldAccessDynamic;

public class CallFieldAccess {
  static boolean unknown;
  public static void main(String[] args) {
    unknown = (args.length == 0);
    callIfException();
    callIfNoException();
    callDynamicIfException();
    callDynamicIfNoException();

  }
  
  static B callIfException() {
    return FieldAccess.testIf(unknown, new B(), null);
  }
  static B callIfNoException() {
    return FieldAccess.testIf(unknown, new B(), new B());
  }
  
  static B callDynamicIfException() {
    FieldAccessDynamic fad = new FieldAccessDynamic();
    return fad.testIf(unknown, new B(), null);
  }
  static B callDynamicIfNoException() {
    FieldAccessDynamic fad = new FieldAccessDynamic();
    return fad.testIf(unknown, new B(), new B());
  }

}
