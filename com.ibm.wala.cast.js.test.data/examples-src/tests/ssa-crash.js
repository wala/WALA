var f = function(arrangedSSIDs,accessPointSpecs,exactLocation)
{
    for (var i=0; i < accessPointSpecs.length; i++)
    {
	var definedSSID = accessPointSpecs[i].SSID;
	var definedMAC = accessPointSpecs[i].MAC;
	
	if (arrangedSSIDs[definedSSID])
	{
	    if (definedMAC)
	    {
		if (arrangedSSIDs[definedSSID][definedMAC])
		{
		    arrangedSSIDs[definedSSID][definedMAC].checked = true;
		    continue;
		}
		return false;
	    }
	    arrangedSSIDs[definedSSID].checked = true;
	    continue;
	}
	return false;
    }
    
    if (exactLocation)
    {
	for (var key in arrangedSSIDs)
	{			
	    var SSID = arrangedSSIDs[key];
	    if (!SSID.checked)
	    {
		var checkedMACs = false;
		for (var MAC in SSID)
		{
		    checkedMACs = true;
		    var obj = SSID[MAC];
		    if (!obj.checked) return false;
		}
		
		if (!checkedMACs) return false;
	    }
	}
    } 
    
    return true;
};

var g = function(transactionId) {
    var obj,http,i;
    try
    {
        http = new XMLHttpRequest();
        obj = { conn:http, tId:transactionId, xhr: true };
    }
    catch(e)
    {
        for(i=0; i<this._msxml_progid.length; ++i){
            try
            {
                http = new ActiveXObject(this._msxml_progid[i]);
                obj = { conn:http, tId:transactionId, xhr: true };
                break;
            }
            catch(e1){}
        }
    }
    finally
    {
        return obj;
    }
}

f();
g();
