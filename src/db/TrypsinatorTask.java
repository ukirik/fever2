package db;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class TrypsinatorTask implements Callable<Set<TrypticPeptide>>{

	String seq;
	Trypsinator t;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	
	public TrypsinatorTask(String sequence) {
		this(new Trypsinator(), sequence);
	}
	
	public TrypsinatorTask(SProtEntry entry){
		this(new Trypsinator(), entry.getSequence());
	}
	
	public TrypsinatorTask(Trypsinator tryp, String sequence){
		t = tryp;
		seq = sequence;
	}
	
	@Override
	public Set<TrypticPeptide> call() {
		Set<TrypticPeptide> s = t.digestSequence(seq);
		return s;
	}	
}