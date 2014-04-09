var a = [];
a[0] = function reachable() { print("reachable"); };
a[1] = function also_reachable() { print("also reachable"); };
a[2] = function reachable_too() { print("reachable, too"); };

var o = { toString: function() { return 2; } };

var pnames = [ "0", 1.0, o];

function invokeOnA(p) {
	var f = a[p];
	f();
}

for(var i=0;i<pnames.length;++i)
	invokeOnA(pnames[i]);
