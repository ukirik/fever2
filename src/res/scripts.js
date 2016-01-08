function tableFilter(c, id) {
	var rows = $(id).find("tbody>tr");
	var r, rowtype, classnames;
	for (var i=0; i < rows.length; ++i)
	{
		r = rows[i];
		rowtype = r.className;
		if(c == "all" || rowtype == "")
			r.style.display = "table-row";
		else 
		{
			if (rowtype.split(" ").indexOf(c) <  0)
				r.style.display = "none";
			else
				r.style.display = "table-row";			
		}
	}
}

function tabSwitch(active, number, tab_prefix, content_prefix) {
	for (var i=1; i < number+1; i++) {
		document.getElementById(content_prefix+i).style.display = 'none';
		document.getElementById(tab_prefix+i).className = '';
	}

	document.getElementById(content_prefix+active).style.display = 'block';
	document.getElementById(tab_prefix+active).className = 'active';
}

function HideContent(d) {
	document.getElementById(d).style.display="none";
}

function ShowContent(d) {
	document.getElementById(d).style.display="block";
}

function ReverseDisplay(d) {
	if(document.getElementById(d).style.display=="none"){
		document.getElementById(d).style.display="block"; 
	}
	else {
		document.getElementById(d).style.display="none"; 
	}
}

function showGraph(type){
	if(type == "bubble")
		data_bubble();
	else if (type == "hex")
		data_hex();
	else
		window.alert("unrecognized visualization option: " + type);
}

function data_bubble(){
	var diameter = 960,
	    format = d3.format(".4f");

	var bubble = d3.layout.pack()
	    .sort(null)
	    .size([diameter, diameter])
	    .padding(1.5)
	    .value(function(d) { return d.pval == 1 ? 0.5 : -1* Math.log(d.pval) });

	var svg = d3.select("#graph").append("svg")
	    .attr("width", diameter)
	    .attr("height", diameter)
	    .attr("class", "bubble");

	d3.json("datagraph.json", function(datagraph) {
	  var node = svg.selectAll(".node")
	      .data(bubble.nodes(datagraph)
	      .filter(function(d) { return !d.children; }))
	    .enter().append("g")
	      .attr("class", "node")
	      .attr("id", function(d) { return d.acc; })
	      .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });

	var color = d3.scale.quantile()
	    .domain(d3.extent(datagraph.children, function(d) { return Math.sqrt(d.ratio); }))
	    .range(colorbrewer.RdBu[9].reverse());

	  node.append("title").text(function(d) { 
	    return d.acc + " - " + 
	            "ratio: " + format(d.ratio) + " " +
	            "pval: " + format(d.pval); 
	          }); 
	  
	var dval_range = d3.extent(datagraph.children, function(d){return d.value; });

	node.append("circle")
	      .attr("r", function(d) { return d.r; })
	      .style("fill", function(d) { return d.ratio == null ? "none" : color(d.ratio);})
	      .style("stroke", function(d) { return d.ratio == null ? "#151515" : "none";})
	      .style("stroke-dasharray", "2,2")
	      .style("opacity", function(d) {
	        return d.ratio != null ? 
	        	0.6 + (d.value - dval_range[0])/(dval_range[1] - dval_range[0]) :
	        	1.0;
	      });
	});

	d3.select(self.frameElement).style("height", diameter + "px");

}