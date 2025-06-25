package javaeight;

import java.util.function.Function;

/* Class hook to test lambda functionality */
public class EmptyLambda {
    public static void main(String[] args) {
        
    }

    private void doit() {
        Function <Integer, Integer> x = i -> i + 1;
    }
}