package graph;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import db.DbManager;
import db.PathwayImpl;
import main.AnalysisResult;

public class PathwayGraph {
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());
	
	private DbManager dbman;
	private Map<PathwayImpl, AnalysisResult> results;
	private PathwayGraphNode root;

	public PathwayGraph(DbManager dbman, Map<PathwayImpl, AnalysisResult> results){
		this.dbman = dbman;
		this.results = results;
		this.root = new PathwayGraphNode(null);
		init();
	}
	
	public PathwayGraphNode getRoot(){
		return this.root;
	}
	
	public int getNbrOfNodes(){
		return this.root.size();
	}
	
	private void init(){
		try {
			Set<PathwayImpl> superpaths = dbman.getPathwaysByParent(null);
			for(PathwayImpl p : superpaths){
				root.addChild(traverseParent(p));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("PathwayGraph initialization failed; cannot get superpaths!");
		}
	}
	
	private PathwayGraphNode traverseParent(PathwayImpl path){
		String rdfid;
		PathwayGraphNode node = new PathwayGraphNode(results.get(path));

		try {
			rdfid = dbman.getPathwayRDFID(path.getId());
			Set<PathwayImpl> children = dbman.getPathwaysByParent(rdfid);
			if(!children.isEmpty()){
				for(PathwayImpl child : children)
					node.addChild(traverseParent(child)); 
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Cannot traverse children of pathway: " + path);
		}
		
		return node;
	}

}
