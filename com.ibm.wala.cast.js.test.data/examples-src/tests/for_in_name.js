var f = function for_in_with_assign(b) {
  var a,d,n,e;
  n=1;
  for(e=arguments.length;n<e;n+=1)
    for(a in d=arguments[n],d)
      Object.prototype.hasOwnProperty.call(d,a)&&(b[a]=d[a]);
  return b;
}

f(new Object());
