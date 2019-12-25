function f() {
	for(var p in arguments)
		arguments[p].g();
}
f.g = function g1() {};

var o = {
	f: f,
	g: function g2() { }
}

var oo = {
	g: function g3() { }
}

// at the IR level, this call has three arguments: (1) the function object for f, (2) o, (3), oo
// however, only the last one ends up in the arguments array
o.f(oo);

// make g1 and g2 reachable so we can check assertions
f.g();
o.g();