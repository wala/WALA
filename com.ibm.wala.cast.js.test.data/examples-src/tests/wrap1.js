Function.prototype.wrap = function f_wrap(){
	var self = this;
	return function wrapper(x, y, z){
	    return self(x, y, z);
	};
};


var extend = function f_extend(thi$, key, value){
	thi$[key] = value;
}.wrap();

extend(Number, 'g', function i_am_reachable(x){ return x+19; });

Number.g(23);
