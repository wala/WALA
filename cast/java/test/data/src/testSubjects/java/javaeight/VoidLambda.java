package javaeight;

import java.util.function.Consumer;

public class VoidLambda {

  static class Obj {
    private int x;

    public Obj(int x) {
      setX(x);
    }

    public void setX(int x) {
      this.x = x;
    }

    public int getX() {
      return x;
    }
    
  }

  Obj var;
  
  public static void main(String... args) {
    new VoidLambda().doit();
  }
  
  public int doit() {
    VoidLambda hack = this;
    Consumer<Integer> x = (i) -> { hack.var = new Obj(i); };
    x.accept(0);
    return var.getX();
  }
}
