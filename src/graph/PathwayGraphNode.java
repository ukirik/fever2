package graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import db.PathwayImpl;
import main.AnalysisResult;

public class PathwayGraphNode {
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	// Unique node id
	private static int nodeCounter;
	private int node_id;
	
	// Contained pathway
	private AnalysisResult path; 
	
	// Subpathways
	private Set<PathwayGraphNode> children;
	
	public PathwayGraphNode(AnalysisResult res){
		this.node_id = nodeCounter++;
		this.path = res;
		this.children = new HashSet<PathwayGraphNode>();
	}
	
	public boolean isRoot(){
		return this.path == null;
	}
	
	public int getNodeID(){
		return this.node_id;
	}
	
	public boolean addChild(PathwayGraphNode n){
		return this.children.add(n);
	}
	
	public int getNbrOfChildren(){
		return this.children.size();
	}
	
	public Set<PathwayGraphNode> getChildren(){
		return Collections.unmodifiableSet(this.children);
	}
	
	public int size(){
		int size = 1;
		for (PathwayGraphNode child : this.children)
			size += child.size();
		return size;
	}

	public int getNodeCounter(){
		return nodeCounter;
	}
}
