package javaeight;

import java.util.function.Function;

/* Class hook to test lambda functionality */
public class LexicalLambda {
  private static class Obj {
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

    public int getXP1() {
      return x+1;
    }
    
  }
    public static void main(String[] args) {
        new LexicalLambda().doit();
    }

    int doit() {
      Obj v = new Obj(3);
      Function<String,Obj> x = i -> new Obj(Integer.valueOf(i + v.getXP1()));
      return x.apply("1").getX();
    }
}