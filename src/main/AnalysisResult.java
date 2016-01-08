package main;

import java.util.logging.Logger;

import db.PathwayImpl;

public class AnalysisResult {

	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());
	
	private PathwayImpl path;
	private double par_score, psea_score, meta_score;
	private int protsInROI, protsIdentified, protsTotal;
	private boolean scoresSet, protStatsSet;
	
	public AnalysisResult(PathwayImpl p){
		this.path = p;
		this.scoresSet = false;
		this.protStatsSet = false;
	}

	public AnalysisResult(PathwayImpl path, 
			double par_score, double psea_score, double meta_score, 
			int protsInROI, int protsIdentified) {
		super();
		this.path = path;
		this.par_score = par_score;
		this.psea_score = psea_score;
		this.meta_score = meta_score;
		this.protsInROI = protsInROI;
		this.protsIdentified = protsIdentified;
		this.scoresSet = true;
		this.protStatsSet = true;

	}
	
	public boolean setScores(double s1, double s2, double meta){
		if(scoresSet)
			return false;
		
		this.par_score = s1;
		this.psea_score = s2;
		this.meta_score = meta;
		scoresSet = true;
		return true;
	}

	public boolean setProts(int roi, int identified, int total){
		if(protStatsSet)
			return false;
		
		this.protsInROI = roi;
		this.protsIdentified = identified;
		this.protsTotal = total;
		protStatsSet = true;
		return true;
	}

	public PathwayImpl getPath() {
		return path;
	}

	public double getPar_score() {
		return par_score;
	}

	public double getPsea_score() {
		return psea_score;
	}

	public double getMeta_score() {
		return meta_score;
	}

	public int getProtsInROI() {
		return protsInROI;
	}

	public int getProtsIdentified() {
		return protsIdentified;
	}

	public int getProtsTotal() {
		return protsTotal;
	}
	
	public String getROInCoverage(){
		StringBuilder sb = new StringBuilder();
		sb.append(protsInROI).append("/")
			.append(protsIdentified).append("/")
			.append(protsTotal);
		return sb.toString();
	}
	
	public String getCoverage(){
		StringBuilder sb = new StringBuilder();
		sb.append(protsIdentified).append("/")
			.append(protsTotal);
		return sb.toString();
	}
	
	public boolean isGO(){
		return this.path.getDb().equalsIgnoreCase("GO");
	}
	
}
