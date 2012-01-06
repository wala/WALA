function f() {
	arguments[0].g();
}
f.g = function g1() {};

var o = {
	f: f,
	g: function g2() { }
}

var oo = {
	g: function g3() { }
}

o.f(oo);