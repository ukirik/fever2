package main;

import java.util.logging.Logger;

import db.PathwayImpl;

public class Accession {

	private static final String iso = "-[0-9]+";
	private static final Accession INSTANCE = new Accession();
	private static final String
		pattern = "[OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z][0-9]([A-Z][A-Z0-9]{2}[0-9]){1,2}";
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	public static Accession getInstance() { 
		return INSTANCE;
	}
	
	public static boolean isValid(String str) {
		int dash = str.indexOf("-");
		if (dash < 0)
			return str.matches(pattern);
		else
			return str.substring(0, dash).matches(pattern) && 
					str.substring(dash, str.length()).matches(iso);
	}
	
	public static boolean isIsoform(String acc){
		return acc.contains("-") && isValid(acc); 
	}	
}
