var o = { h: function() {} };

function f(g) {
	g();
	o.h(function() {
		g;
	});
}

function g() {}

f(g);