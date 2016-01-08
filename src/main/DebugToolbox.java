package main;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import db.PathwayImpl;

	/**	This singleton class holds routines generally used for debugging. 
	Most of the methods are switch controlled with a boolean debug parameter.
*/
public class DebugToolbox {
	
	private static final DebugToolbox INSTANCE = new DebugToolbox();
	private static final String F_EPOCH = "2012-04-03";
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());
	
	private static DecimalFormat  df,sf;
	private static List<String> score_log;
	private static final String nL = System.getProperty("line.separator");
	private static final int DEF_CAP = 1 << 15;
	private static long exectime; 
	private static ArrayList<Long> proctimes;
	private static SummaryStatistics procstats;
	private static volatile boolean exectime_running;
	
	private DebugToolbox(){
		
		proctimes = new ArrayList<Long>();
		procstats = new SummaryStatistics();
		score_log = new LinkedList<String>();
		df = new DecimalFormat("##0.000");
		sf = new DecimalFormat("#0.00E0");
	}
	
	private static String formatLongBytes(long bytes){
		double kbs = Math.round(bytes/1024);
		DecimalFormat df = new DecimalFormat("#,##0");
		return df.format(kbs);
	}
	
	/*	PUBLIC METHODS	*/
	
	public static DebugToolbox getInstance(){
		return INSTANCE;
	}
	
	public static String getSessionID(){
		try{
			SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
			long time = System.currentTimeMillis() - sdf.parse(F_EPOCH).getTime();
			return Long.toString(time);
		}
		catch(ParseException e){
			e.printStackTrace();
			throw new RuntimeException("Cannot generate session id!");
		}
	}
	
	public static String getTimeStamp(){
		Timestamp t = new Timestamp(System.currentTimeMillis());
		String temp = t.toString();
		String stamp = temp.substring(temp.indexOf(" "), temp.indexOf("."));
		
		return stamp;
	}
	
	public static String getDateTimeStamp(){
		Timestamp t = new Timestamp(System.currentTimeMillis());
		String temp = t.toString();
		String stamp = temp.substring(0, temp.indexOf("."));
		
		return stamp;
	}
		
	public void getMemoryInfo(boolean toLog){
		Runtime rt = Runtime.getRuntime();
		StringBuilder memInfo = new StringBuilder(nL);
		long totMem = rt.totalMemory(),freeMem = rt.freeMemory();
		memInfo.append("-MEMINFO: tot = " + formatLongBytes(totMem) + "KB" + "\t");
		memInfo.append("free = " + formatLongBytes(freeMem) + "KB" + nL);
		
		if(toLog)
			logger.info(memInfo.toString() + System.lineSeparator());
		else
			System.out.println(memInfo.toString());
	}
	
	public static void startExecTimer(){
		exectime = System.currentTimeMillis();
		exectime_running = true;
	}
	
	public static void stopExecTimer(){
		exectime = System.currentTimeMillis() - exectime;
		exectime_running = false;
	}
	
	public static String getExecTime(){
		if(exectime_running)
			stopExecTimer();
		
		StringBuilder result = new StringBuilder();
	    long timeInMillis = exectime;
	    
	    final int days = (int) ( timeInMillis / ( 24L * 60 * 60 * 1000 ) );
	    int remdr = (int) ( timeInMillis % ( 24L * 60 * 60 * 1000 ) );

	    final int hours = remdr / ( 60 * 60 * 1000 );
	    remdr %= 60 * 60 * 1000;

	    final int mins = remdr / ( 60 * 1000 );
	    remdr %= 60 * 1000;

	    final int secs = remdr / 1000;
	    final int ms = remdr % 1000;
	    
	    result.append(days > 0 ? days+":" : "0:");
	    result.append(hours > 0 ? (hours >= 10 ? hours+":" : "0"+hours+":") : "00:");
	    result.append(mins > 0 ? (mins >= 10 ? mins+":" : "0"+mins+":") : "00:");
	    result.append(secs > 0 ? (secs >= 10 ? secs+":" : "0"+secs+":") : "00:");
	    result.append(ms);
	    return result.toString();
	}
	
	public synchronized static void submitProcTime(long t){
		proctimes.add(t);
		procstats.addValue(t);
	}
	
	public synchronized static void submitScores(String pathName, double par, double npar, double meta){
		StringBuilder sb = new StringBuilder();
		sb.append(pathName).append(" ")
			.append("[PAR = ").append(sf.format(par)).append("\t")
			.append("NPAR = ").append(sf.format(npar)).append("\t")
			.append("META = ").append(df.format(meta)).append("]");
		score_log.add(sb.toString());
	}
	
	public static void dumpMemoryInfo(boolean toLogFile){
		
		Runtime rt = Runtime.getRuntime();
		StringBuilder memInfo = new StringBuilder();
		long totMem = rt.totalMemory();
		long freeMem = rt.freeMemory();
		memInfo.append("-MEMINFO: tot = ")
				.append(formatLongBytes(totMem))
				.append("KB \t")
				.append("free = ")
				.append(formatLongBytes(freeMem))
				.append("KB").append(nL);
		
		if(toLogFile)
			logger.info(memInfo.toString());
		else 
			System.out.println(memInfo.toString());
		
	}
	
	public static synchronized void dumpValues(double[] vals){
		StringBuilder s = new StringBuilder("Values: ");
		s.append(nL);
		int i=0, col_width = 15;
		for(double d : vals){
			if(i==col_width){
				s.append(nL);
				i = 0;
			}
			i++;
			s.append(df.format(d)).append(", ");
		}
		int ind = s.length();
		s.delete(ind-2, ind);
		logger.info(s.toString());
	}
	
	public static void dumpScores(){
		StringBuilder s = new StringBuilder(DEF_CAP);
		s.append("-LOGGED SCORES: [PAR\tNPAR\tMETA]").append(nL);
		for(String score : score_log)
			s.append(score).append(nL);
		
		logger.info(s.toString());
	}
	
	public static ArrayList<Long> getProctimes(){
		return proctimes;
	}
	
	public static double getMeanProcTime(){
		return procstats.getMean();
	}
	
	public static String getMeanProcTimeAsString(){
		return df.format(getMeanProcTime());
	}
	
	public static double getProcTimeStdDev(){
		return procstats.getStandardDeviation();
	}
	
	public static int getNbrProcTimes(){
		if(proctimes.size() != procstats.getN()){
			System.err.println("Unexpected error: "
					+ "proctimes.size (" + proctimes.size() + ")"
					+ " is not equal to "
					+ "procstats.N (" + procstats.getN() + ")");
		}
			
		
		return proctimes.size();
	}
	
	/**	Offers a conditional quit
	 * @param condition - a boolean condition on which the execution quits or not
	 * @param note - an optional string  
	 */
	public static void cq(boolean condition, String note){
		if (condition){
			note = (note!=null) ? note : "";
			System.err.println(note);
			System.exit(0);
		}
	}
	
	/**	
	 * Pauses the execution for a given amount of milliseconds.
	 * This method is used as a wrapper for Thread.sleep() for debug purposes. 
	 * @param n - amount of millis to sleep
	 */	
	public static void pauseProcess(long n){
		pauseProcess(true, n);
	}
	
	/** 
	 * Pauses the execution for a given amount of milliseconds, if the given condition is met.
	 * This method is used as a wrapper for Thread.sleep() for debugging purposes.
	 * @param cond - a boolean condition
	 * @param n - amount of millis to sleep
	 */
	public static void pauseProcess(boolean cond, long n){
		// TODO: implement this method
	}
	
	/** 
	 * Stops the execution indefinitely!
	 * This method is used as a wrapper for Thread.wait() for debugging purposes.
	 * @param note - a String holding any message aimed to the user
	 */
	public static void haltProcess(String note){
		haltProcess(true,note);
	}
	
	/** 
	 * Stops the execution indefinitely!
	 * This method is used as a wrapper for Thread.wait() for debugging purposes.
	 * @param cond - a boolean condition
	 */
	public static void haltProcess(boolean cond){
		haltProcess(cond, "");
	}
	
	/** 
	 * Stops the execution indefinitely!
	 * This method is used as a wrapper for Thread.wait() for debugging purposes.
	 * @param cond - a boolean condition
	 * @param note - a String holding any message aimed to the user
	 */
	public static void haltProcess(boolean cond, String note){
		// TODO: implement this method
	}
	
	public static String getStackTraceAsString(Exception e){
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}
