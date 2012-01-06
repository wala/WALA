Function.prototype.wrap = function f_wrap(){
	var self = this;
	return function wrapper(x, y, z){
	    return (function wrapper_inner() { self(x, y, z); })();
	};
};


Function.prototype.extend = function f_extend(thi$, key, value){
	thi$[key] = value;
}.wrap();

Number.extend(Number, 'g', function i_am_reachable(x){ return x+19; });

print(Number.g(23));
