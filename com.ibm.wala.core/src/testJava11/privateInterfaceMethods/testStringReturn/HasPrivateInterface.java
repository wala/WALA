package testStringReturn;

public class HasPrivateInterface {
    public static String RetString(){
        return GetString();
    }

    private static String GetString(){//c as in speed of light
        return "Hello World!";
    }
}
