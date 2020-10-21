function f_wrap() {
	var y = null;
	return function wrapper() {
		return { set: function set(p) {
			        y = p;
		          }, get: function get() {
			        return y;
		          }
		       };
	}
	
}

var w = f_wrap();

var x = w();

var s = function i_am_reachable() {};

x.set(s);

var t = x.get();

t();
