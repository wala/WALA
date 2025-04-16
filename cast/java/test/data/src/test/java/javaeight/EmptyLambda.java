package javaeight;

import java.util.function.Function;

/* Class hook to test lambda functionality */
public class EmptyLambda {
    public static void main(String[] args) {
        new EmptyLambda().doit();
    }

    String doit() {
        Function<String,String> x = i -> i + "y";
        return x.apply("x");
    }
}