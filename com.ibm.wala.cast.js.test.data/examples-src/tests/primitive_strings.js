function f1() {
	return "abc".concat("def");
}

function f2() {
	return new String("abc").concat(new String("def"));
}

f1();
f2();
