var x = function() {
    try {
        y;
    } catch (e) {
        for (0; this.p.q; ) {}
    } finally {
        return;
    }
};

x();

var document = { };

x = function( fn ) {
    var div = document.createElement("div");
    
    try {
	return fn( div );
    } catch (e) {
	return false;
    } finally {
	// release memory in IE
	div = null;
    }
};

x(document);
