package testStringReturn;

public class HasPrivateInterface {
    public static String string(){
        return getString();
    }

    private static String getString(){//c as in speed of light
        return "Hello World!";
    }
}
