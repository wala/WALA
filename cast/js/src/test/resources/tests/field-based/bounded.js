var m = function m() { return "hi"; } // 1
var x = function x() { return m; } // 2
var y = function y() { return x; } // 3
var z = y; // 4
var n = z(); // 5 -- bound 0
var o = n() // 6 -- bound 1
o.call(this); // 7 -- bound 2