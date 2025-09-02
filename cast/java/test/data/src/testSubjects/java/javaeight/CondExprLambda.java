package javaeight;

import java.util.function.Function;

/* Class hook to test lambda functionality */
public class CondExprLambda {
    public static void main(String[] args) {
        new CondExprLambda().doit();
    }

    int doit() {
      Function<String,String> x = i -> "z".equals(i)? i: i + "y";
        return x.apply("x").length();
    }
}