
x = (function(e,t) {
    var n=[],r=!0,i=!1,o=void 0;
    try {
	for(var a,u=e[Symbol.iterator]();
	    !(r=(a=u.next()).done)&&(n.push(a.value),!t||n.length!==t);
	    r=!0);
    } catch(e) {
	i=!0,o=e
    }
    finally{
	try{
	    !r&&u.return&&u.return()
	} finally {
	    if(i)throw o
	}
    }
    return n
})

x()
