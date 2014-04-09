var a = [];
a[0] = function reachable1() { print("reachable"); };
a["0"]();

var b = [];
b[1] = function reachable2() { print("also reachable"); };
b[1.0]();

var c = [];
c["2"] = function reachable3() { print("reachable, too"); };
c[2]();

var d = [];
d[3] = function reachable4() { print("reachable4"); };
var o = { toString: function() { return 3; } };
d[o]();