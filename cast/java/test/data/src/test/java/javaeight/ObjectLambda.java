package javaeight;

import java.util.function.Function;

/* Class hook to test lambda functionality */
public class ObjectLambda {
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
    
  }
    public static void main(String[] args) {
        new ObjectLambda().doit();
    }

    int doit() {
        Function<String,Obj> x = i -> new Obj(Integer.valueOf(i));
        return x.apply("1").getX();
    }
}