function foo(x) {
	function bar() {
		x = function i_am_reachable() {}
	}
	bar();
	return x;	
}

var y = foo(null);
y();
