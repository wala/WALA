Function.prototype.useCall = function f1(){
	var self = this;
	return function f1_inner(a, b){
	    self.call(this, a, b);
		return this;
	};
};


Function.prototype.extend = function f2(key, value){
	this[key] = value;
}.useCall();

Number.extend('foobaz', function f3(min, max){
	return min;
});

var r = Number.foobaz(3, 4);
