var left = {

    inner: function left_inner(x) {
    	return x+1;
    },

    outer: function left_outer(x) {
    	return this.inner(x+1);
    }

};

var right = {

    inner: function right_inner(x) {
    	return Math.abs(x);
    },

    outer: function right_outer(x) {
    	return this.inner(-x);
    }

};

var x = 3;
if (x > Math.random()) {
    x = left;
} else {
    x = right;
}
 
x.outer(7);

