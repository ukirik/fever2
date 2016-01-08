// add parser through the tablesorter addParser method 
$.tablesorter.addParser({ 
// set a unique id 
id: 'scinot', 
is: function(s) { 
    // IGNORED: return false so this parser is not auto detected 
    return /[+\-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+\-]?\d+)?/.test(s); 
}, 
format: function(s) { 
    // format your data for normalization 
    //var arr = s.split("[eE]");
    //var exponent = Math.abs(arr[1]);
    //var mantissa = arr[0].split();
    return $.tablesorter.formatFloat(s);

	
}, 
// set type, either numeric or text 
type: 'numeric' 
});
