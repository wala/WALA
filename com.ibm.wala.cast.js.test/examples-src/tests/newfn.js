
var fun1 = new Function("a", "b", "c", "return a+b+c");

var fun2 = new Function("a, b, c", "return a+b+c");

var fun3 = new Function("a, b", "c", "return a+b+c");

var x = fun1(5, 5, 6);

var y = fun2(5, 7, 1);

var z = fun3(3, 5, 2);
