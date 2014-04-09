var m = function f() {
	var x = {};
	function g() {
		x["foo"] = {};
		f();
	}
	g();
}

m();