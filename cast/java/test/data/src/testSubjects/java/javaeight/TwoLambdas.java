package javaeight;

import java.util.function.Function;

/* Class hook to test lambda functionality */
public class TwoLambdas {
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

    public int getXP1() {
      return x+1;
    }
    
  }

    public static void main(String[] args) {
        new TwoLambdas().doit();
    }

    int doit() {
      Function<String,Obj> x = i -> new Obj(Integer.valueOf(i));
      Function<String,Obj> y = i -> new Obj(Integer.valueOf(i)+1);
        return x.apply("1").getX() +  y.apply("1").getXP1(); 
    }
}