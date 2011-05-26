var obj = {
  repr:function(o){
    if(typeof (o)=="undefined"){
	return "undefined";
    }else{
	if(o===null){
	    return "null";
	}
    }
    try{
	if(typeof (o.__repr__)=="function"){
	    return o.__repr__();
	}else{
	    if(typeof (o.repr)=="function"&&o.repr!=arguments.callee){
		return o.repr();
	    }
	}
	return MochiKit.Base.reprRegistry.match(o);
    }
    catch(e){
	if(typeof (o.NAME)=="string"&&(o.toString==Function.prototype.toString||o.toString==Object.prototype.toString)){
	    return o.NAME;
	}
    }
    try{
	var _d8=(o+"");
    }
    catch(e){
	return "["+typeof (o)+"]";
    }
    if(typeof (o)=="function"){
	o=_d8.replace(/^\s+/,"");
	var idx=o.indexOf("{");
	if(idx!=-1){
	    o=o.substr(0,idx)+"{...}";
	}
    }
    return _d8;
  }
};

obj.repr(obj);
