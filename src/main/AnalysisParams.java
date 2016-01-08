package main;

import gui.FeverMainFrame;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import db.PathwayImpl;

/**
 * A collection of parameters used throughout the analysis, contained in a 
 * singleton object for easier referencing. 
 * <p> This object implements {@code Serializable} in order to provide
 * persistence for future versions.
 * @author Ufuk Kirik
 *
 */
public class AnalysisParams implements Serializable{
		
	private static final long serialVersionUID = -8383495391791585408L;
	private static final AnalysisParams INSTANCE = new AnalysisParams();
	private static final double COEFF_SUM = 1D;
	private static final double EPSILON = Double.valueOf("1E-6");
	private final RVAL_TYPE[] valtypes;

	
	public static enum SEPCHAR {SEMICOLON, COMMA, COLON};
	public static enum FILETYPE {CSV, TSV};
	public static enum RVAL_TYPE {RAW, LOG2 , LOGN, LOG10};
	public static enum RANDMETHOD {EMPIRICAL, LOGNORM, PERMUTATION};
	public static enum SORTMETHOD {FOLDCHANGE, PVALUE, COMB_NONLINEAR};
	
	public static final double THRESHOLD_NOT_SET = Double.NaN;
	public static final int NPARAMS = 15;
	public static final int NTHR_NOT_SET = Runtime.getRuntime().availableProcessors();
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());


	/**	Adds the given parameter by updating it's value in the underlying {@code Hashtable}
	 * @param key - a {@code String} used as a key with which given parameter will be stored
	 * @param param - the value of the parameter to be stored
	 * @return {@code false} if the given key is not a recognized parameter key, 
	 * or if the given key was already associated with another value. {@code true} otherwise.  
	 * */
	public synchronized boolean addParam(String key, Object param){
		if(!table.containsKey(key))
			return false;
		
		switch(key){
		case infile_key: 
			return parseInputFile((String)param); 
		case filetype_key: 
			return parseFileType((int)param);
		case sepchar_key: 
			return parseSepChar((int)param);
		case sort_key: 
			if(param == null){
				JOptionPane.showMessageDialog(null, "Unknown sorting method", 
						"error", JOptionPane.ERROR_MESSAGE);
				quitExecution("Unknown randomization method");
			}
			
			return table.put(sort_key, (SORTMETHOD)param) == null;
			
		case rand_key:
			if(param == null){
				JOptionPane.showMessageDialog(null, "Unknown randomization method", 
						"error", JOptionPane.ERROR_MESSAGE);
				quitExecution("Unknown randomization method");
			}
			
			return table.put(rand_key, (RANDMETHOD)param) == null;
			 
		case value_key:
			return parseValueType((int)param);
		case nthreads_key: 
			return parseNbrOfThreads((String)param); 
		default: 
			return parseDoubleValueParam(key, (String)param);
		}		
	}

//	public synchronized Object getParam(String key){
//		return table.get(key);
//	}	
	public synchronized File getDataFile(){
		return (File)table.get(infile_key);
	}	
	public synchronized FILETYPE getFileType(){
		return (FILETYPE)table.get(filetype_key);
	}
	public synchronized String getColumnSepChar(){
		switch ((FILETYPE) table.get(filetype_key)){
		case CSV: return ",";
		case TSV: return "\t";
		default: return null;
		}
	}
	public synchronized String getIdSepChar(){
		switch ((SEPCHAR) table.get(sepchar_key)){
			case SEMICOLON: return ";";
			case COMMA: return ",";
			case COLON: return ":";
			default: return ";";
		}
	}
	public synchronized double getRegLevelThreshold(){
		return (double)table.get(reglevel_key);
	}
	public synchronized double getSignLevelThreshold(){
		return (double)table.get(signlevel_key);
	}
	public synchronized double[] getAlphaCoefficients(){
		return new double[]{
			(double)table.get(a1_key),
			(double)table.get(a2_key),
			(double)table.get(a3_key)
		};
	}
	public synchronized double getAlphaCoefficients(int i){
		switch(i){
		case 1:	return (double)table.get(a1_key);
		case 2:	return (double)table.get(a2_key);
		case 3:	return (double)table.get(a3_key);
		default:	
			throw new IllegalArgumentException("Argument 'i' can only be 1,2 or 3");
		}
	}
	public synchronized double[] getKappaCoefficients(){
		return new double[]{
			(double)table.get(k1_key),
			(double)table.get(k2_key)
		};
	}
	public synchronized double getKappaCoefficients(int i){
		switch(i){
		case 1:	return (double)table.get(k1_key);
		case 2:	return (double)table.get(k2_key);
		default:	
			throw new IllegalArgumentException("Argument 'i' can only be 1 or 2");
		}
	}
	public synchronized RANDMETHOD getRandMethod(){
		return (RANDMETHOD)table.get(rand_key);
	}
	public synchronized SORTMETHOD getSortMethod(){
		return (SORTMETHOD)table.get(sort_key);
	}
	public synchronized RVAL_TYPE getValueType(){
		return (RVAL_TYPE)table.get(value_key);
	}
	public synchronized int getNbrOfThreads(){
		return (int)table.get(nthreads_key);
	}
	
	public synchronized boolean isInROI(double ratio, double pval){
		boolean pvalSignificant = pval < getSignLevelThreshold();
		boolean ratioSignificant = ratio > getRegLevelThreshold() ||
									1/ratio > getRegLevelThreshold();
											
		return ratioSignificant && pvalSignificant;
	}
	
	/**
	 * Checks if the parameters held in this object are valid.
	 * More specifically the control mechanisms are:
	 * <ol>
	 * <li> number of keys is correct, see {@code NPARAMS}
	 * <li> all keys expected keys have non-null values
	 * <li> &alpha;-coefficients sum up to 1.0 (or close enough, see {@code EPSILON}) 
	 * <li> p-value threshold should be in range (0,1)
	 * <li> regulation level threshold should be positive and larger than 1.0
	 * </ol>
	 * @return {@code true} if all above criteria are correct, {@code false} otherwise
	 */
	public synchronized boolean isValid(){
		Set<String> keys = table.keySet();
		
		// # of parameters should be correct!
		if(keys.size() != NPARAMS)
			return false;
		
		// all keys must be non-null
		for(String key : keys){
			if(table.get(key) == null)
				return false;
		}
		
		// alpha coeffs must add up to 1.0
		double alpha_sum = (double)(table.get(a1_key)) +
							(double)(table.get(a2_key)) + 
							(double)(table.get(a3_key));
		
		if(Math.abs(alpha_sum - COEFF_SUM) > EPSILON)
			return false;
		
		// p-value threshold must be in range (0,1)
		double pval = (double)(table.get(signlevel_key));
		if (pval <= 0 || pval >= 1){
			return false;
		}
		
		// reglevel should be larger than 1.0
		double reg = (double)(table.get(reglevel_key));
		if (reg <= 1){
			return false;
		}
		
		return true;
	}
	
	public static AnalysisParams getInstance(){
		return INSTANCE;
	}
	
	// PRIVATE METHODS // 
	private boolean parseDoubleValueParam(String key, String param) {
		if(!table.keySet().contains(key)){
			quitExecution("The parameter " + key + " is not recognized");
		}
			
		double val = THRESHOLD_NOT_SET;
		try{
			val = Double.parseDouble(param);
		}catch(NumberFormatException e){
			String msg = "the given value for " + key + " is not numeric!"; 
			JOptionPane.showMessageDialog(null, msg , 
					"Error!", JOptionPane.ERROR_MESSAGE);
			quitExecution("");
		}
		return table.put(key,val) == null;
	}

	private boolean parseNbrOfThreads(String param) {
		int n;
		try{
			n = Integer.parseInt(param);
		}catch(NumberFormatException e){
			n = NTHR_NOT_SET;
			JOptionPane.showMessageDialog(null, 
					"# of threads set to an non-numeric value "
					+ "optimal number of threads will be used!", 
					"Notice!", JOptionPane.WARNING_MESSAGE);
		}
		
		if (n < 0){
			n = NTHR_NOT_SET;
			JOptionPane.showMessageDialog(null, 
					"# of threads set to a negative value; "
					+ "optimal number of threads will be used!", 
					"Notice!", JOptionPane.INFORMATION_MESSAGE);
		}else if (n == 0){
			n = NTHR_NOT_SET;
		} 
		return table.put(nthreads_key, n) == null;	
	}
	
//	private boolean parseValueType(int param) {
//		RVAL_TYPE valtype = null;
//		switch(param){
//		case 0: 
//			valtype = valtypes[param]; break;
//		case 1:
//			valtype = RVAL_TYPE.LOGN; break;
//		case 2:	
//			valtype = RVAL_TYPE.LOG10; break;
//		case 3: 
//			valtype = RVAL_TYPE.RAW; break;
//		default:
//				JOptionPane.showMessageDialog(null, 
//						"Unknown value type for quantification values", 
//						"error", JOptionPane.ERROR_MESSAGE);
//				quitExecution("Unknown value type");
//		}
//		return table.put(value_key, valtype) == null;
//	}
	
	private boolean parseValueType(int param){
		RVAL_TYPE valtype = null;
		if (param < valtypes.length)
			valtype = valtypes[param];
		else{
			JOptionPane.showMessageDialog(
					null, 
					"Unknown value type for quantification values", 
					"error", JOptionPane.ERROR_MESSAGE);
			quitExecution("Unknown value type");
		}
		
		return table.put(value_key, valtype) == null;
	}

	private boolean parseSepChar(int param) {
		SEPCHAR ch = null;
		SEPCHAR[] s = SEPCHAR.values();
		if(param < s.length)
			ch = s[param];
		else{
			JOptionPane.showMessageDialog(null, "Unsupported separation char!", "error", JOptionPane.ERROR_MESSAGE);
			quitExecution("Unsupported separation char!");
		}
		return table.put(sepchar_key, ch) == null;
	}

	private boolean parseFileType(int param) {
		FILETYPE ft = null;
		switch(param){
		case 0: 
			ft = FILETYPE.CSV; break;
		case 1:
			ft = FILETYPE.TSV; break;
		default:
				JOptionPane.showMessageDialog(null, "Unrecognized filetype!", "error", JOptionPane.ERROR_MESSAGE);
				quitExecution("Unrecognized filetype!");
		}
		return table.put(filetype_key, ft) == null;
	}

	private boolean parseInputFile(String param) {
		File f = new File(param);
		if(!f.exists() || !f.canRead()){
			JOptionPane.showMessageDialog(null, 
					"Datafile does not exist, or is not readable",
					"error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return table.put(infile_key, f) == null;
		
	}
	
	private void quitExecution(String msg){
		System.err.println(msg);
		System.exit(FeverMainFrame.exitCodes.get(this.getClass()));		
	}
	
	private final HashMap<String,Object> table;
	public static final String infile_key =  "inputfile";
	public static final String filetype_key =  "filetype";
	public static final String sepchar_key =  "sepchar";
	public static final String reglevel_key =  "reglevel";
	public static final String signlevel_key =  "signlevel";
	public static final String a1_key =  "alpha1";
	public static final String a2_key =  "alpha2";
	public static final String a3_key =  "alpha3";
	public static final String k1_key =  "kappa1";
	public static final String k2_key =  "kappa2";
	public static final String sort_key =  "sortmethod";
	public static final String rand_key =  "randmethod";
	public static final String value_key = "value_type";
	public static final String nthreads_key =  "nthreads";
	
	/**
	 * Private constructor which initializes all accepted keys to null
	 */
	private AnalysisParams(){
		table = new HashMap<String,Object>(32);
		table.put(infile_key, null);
		table.put(filetype_key, null);
		table.put(sepchar_key, null);
		table.put(reglevel_key, null);
		table.put(signlevel_key, null);
		table.put(a1_key, null);
		table.put(a2_key, null);
		table.put(a3_key, null);
		table.put(k1_key, null);
		table.put(k2_key, null);
		table.put(rand_key, null);
		table.put(sort_key, null);
		table.put(value_key, null);
		table.put(nthreads_key, null);
		valtypes = RVAL_TYPE.values();
	}

}
