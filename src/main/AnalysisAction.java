package main;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.special.Gamma;

import com.google.common.math.BigIntegerMath;

import main.Dataset.Data;
import db.DbManager;
import db.PathwayImpl;
import db.ProteinImpl;


public class AnalysisAction implements Runnable{
	
	private Dataset data;
	private DbManager dbManager;
	private PathwayImpl path;
	private Set<Integer> identifiedProts;
	private AnalysisResult res;
	private ConcurrentHashMap<PathwayImpl, AnalysisResult> analyzedPaths;
	private String threadName;
	private int proi = -1, pfound, ptotal;
	
	private AnalysisParams params = AnalysisParams.getInstance();
	private long t0,t_end;
		
	/*	FINALIZED FIELDS	*/

	public static final int N_SAMPLES = Dataset.N_REPL * 1000;
	public static final double MIN_PARAM_PVAL = 1D / N_SAMPLES;
	public static final double MIN_PSEA_PVAL = Double.valueOf("1E-10");
	public static final AtomicInteger duplicates = new AtomicInteger(0);
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());
	
	private static final double META_PAR_WEIGHT = 1.2D;
	private static final double META_NPAR_WEIGHT = 1D;
	
	public AnalysisAction(PathwayImpl p, Dataset ds, DbManager dbm, 
			ConcurrentHashMap<PathwayImpl, AnalysisResult>analyzedPaths)
	{
		this.data = ds;
		this.path = p;
		this.dbManager = dbm;
		this.analyzedPaths = analyzedPaths;
		this.res = new AnalysisResult(path);
		
		// Set debug level
		logger.setLevel(Level.FINE);
	}

	public static double getMIN_PARAM_PVAL() {
		return MIN_PARAM_PVAL;
	}

	public static double getMIN_PSEA_PVAL() {
		return MIN_PSEA_PVAL;
	}
	
	/** <p>Calls two different analyses and calculates the scores.<br/>
	 * 	1) Parametric: builds on the FEvER model, using subscores to evaluate different
	 * 	properties of pathways.<br/>
	 *  2) Non-param. enrichment: Uses a mainstream GSEA-like overrepresentation analysis, 
	 *  coupled with a dynamic programming approach to calculate exact probabilities for
	 *  enrichment significance.</p>
	 *  */
	@Override
	public void run() {
		
		this.threadName = Thread.currentThread().getName();
		
		// IF path is already processed, dont process again!!
		if(analyzedPaths.containsKey(path)){
			logger.finer("Duplicate pathway encountered..." + System.lineSeparator());
			duplicates.incrementAndGet();
			return;
		}
		
		// Take time for stats
		t0 = System.currentTimeMillis();
		
		/* 1. Check the coverage of the pathway, 
		 * if no proteins in this pathway are observed in the dataset
		 * there is nothing to do
		 */
		this.identifiedProts = getIdentifiedProteins();
		if(identifiedProts.size() == 0)
			logger.warning("Pathway: " + path.getName() + " not featured in dataset");
		else
		{
			
			double par_score, psea_score, meta_score;
			StringBuilder sb;					
			try{
				/* 	2. Calculate the PAR-enrichment and the significance 
				of the calculated PAR-score */
				if(path.getDb().equalsIgnoreCase("GO"))
					par_score = Double.NaN;
				else{
					sb = new StringBuilder("Enrichment calculated: ");
					
					long t = System.currentTimeMillis();
					double d = calcParEnrichment(data, true);
					long t1 = System.currentTimeMillis();
					sb.append(t1-t).append(" millis; ");
					
					par_score = calcParScore(d);
					long t2 = System.currentTimeMillis();
					sb.append("Score calculated: ")
						.append(t2-t1).append(" millis.")
						.append(System.lineSeparator());
					
					 logger.finest(sb.toString());
				}
				// 3. Calculate the PSEA (NPAR) score
				psea_score = calcPseaScore();
					
				// 4. Calculate the Meta score out of PAR & PSEA scores
				meta_score = path.getDb().equalsIgnoreCase("GO") ?
						Double.NaN : calcMetaScore(par_score, psea_score);
				
				// 5. Sanity check
				boolean param_check = par_score > 0 && par_score <= 1;
				boolean psea_check =  psea_score> 0 && psea_score <= 1;
				boolean meta_check = 0 <= meta_score && meta_score <= 100;
				
				if(!path.getDb().equalsIgnoreCase("GO") && 
				(param_check && psea_check && meta_check) == false){
					String msg = "param_check: " + param_check + "\t" +
							"psea_check: " + psea_check + "\t" +
							"meta_check: " + meta_check;
					
					logger.warning("Unfeasable scores in pathway '" + 
							path.getName() + "'\n" + msg + "; " +
							"pathway will be skipped...\n\t" +
							"PARAM: " +	par_score + ", " +
							"PSEA: " + psea_score + ", " + 
							"META: " + meta_score);
					
					System.err.println("Unexpected scores encountered. "
							+ "Please check the log file before evaluating your results!");
				}
				
				// 6. Dump the scores to log files
				DebugToolbox.submitScores(path.getName(), par_score, psea_score, meta_score);
				
				// 7. Add to collection of paths
				res.setProts(proi, pfound, ptotal);
				res.setScores(par_score, psea_score, meta_score);
				if(analyzedPaths.putIfAbsent(path, res) != null)
					duplicates.incrementAndGet();
			}
			catch(Exception e){
				logger.severe("Exception occured while calculating enrichment: " 
						+ System.lineSeparator() 
						+ DebugToolbox.getStackTraceAsString(e)
						+ System.lineSeparator());
			}
		}
		
		t_end = System.currentTimeMillis();
		DebugToolbox.submitProcTime(t_end - t0);
	
	}

	/**
	 * @return Gets the indices rows that represents the proteins that are a part of this pathway.
	 * <p>This methods returns indices rather than the actual data, since the mock datasets 
	 * that are used for calculating the enrichment significance use the same indices, 
	 * but different values.
	 */
	private Set<Integer> getIdentifiedProteins(){
		HashSet<Integer> pset = new HashSet<Integer>();
		Set<ProteinImpl> allProts = null;
		try {
			allProts = dbManager.getAllProtsInPath(path.getId());
			ptotal = allProts.size();

			for(ProteinImpl p : allProts){
				
				// Avoid iterating the dataset, if an accession is not contained within the Dataset
				if (!data.getProteinsIds().contains(p.getAcc()))
					continue;
				
				boolean hit = false;
				for(Data d : data.getDataRows()){
					if(d.getProteins().contains(p.getAcc())){
						if(hit)
							logger.warning("Multiple hits for accession: " + p.getAcc());
						
						pset.add(d.getUid());
						hit = true;
					}
				}
			}
			
		} catch (SQLException e) {
			logger.severe("Unexpected database error: " 
					+ System.lineSeparator() 
					+ DebugToolbox.getStackTraceAsString(e)
					+ System.lineSeparator());
			
			System.err.println("Database error occured, please check the logs for more information!");
		}
		
		pfound = pset.size();
		return pset;
		}
	
	/**
	 * Calculates the meta score, by <ul>
	 * <li> applying  the transform {@code -1 * log10(s)}, 
	 * where s stands for the expect value type score from 
	 * the parametric or non-parametric model. 
	 * <li> calculating the quotient of transformed scores 
	 * to transformed maximum possible scores. </ul> 
	 * 
	 * Parametric and non-parametric scores are weighted with
	 * the static fields {@code META_PAR_WEIGHT} and 
	 * {@code META_NPAR_WEIGHT}.
	 * @param par_score score from parametric model
	 * @param psea_score score from non-parametric (PSEA) model
	 * @return a meta score in range (0,100)
	 */
	private double calcMetaScore(double par_score, double psea_score) {
		double 	par = -1 * StrictMath.log10(par_score),
				npar = -1 * StrictMath.log10(psea_score),
				maxpar = -1 * StrictMath.log10(MIN_PARAM_PVAL),
				maxnpar = -1 * StrictMath.log10(MIN_PSEA_PVAL),
				n1 = StrictMath.pow(par, META_PAR_WEIGHT),
				n2 = StrictMath.pow(npar, META_NPAR_WEIGHT),
				d1 = StrictMath.pow(maxpar, META_PAR_WEIGHT),
				d2 = StrictMath.pow(maxnpar, META_NPAR_WEIGHT);
		
		return 	n1 * n2 / (d1 * d2) * 100D;
	}
	
	/**	Calculates subscores for the original Fever parametric analysis. 
	 * 	Steps:<br/> 
	 * 			1) Define a region of interest (ROI)<br/>
	 * 			2) Calculate S1: Significance of size of ROI and total amount of found proteins<br/>
	 * 			3) Calculate S2: A minus ambiguity score <br/>
	 * 			4) Calculate S3: A regulation score<br/>
	 * @param ds - the dataset this pathway is analyzed against
	 * @param dump - whether or not to dump the scores to score log.
	 * @return score the score associated with this pathway given the dataset
	 * */
	private double calcParEnrichment(Dataset ds, boolean dump_scores){
		// Get the intersection between the ROI and identified prots
		Set<Data> roi = ds.getROI();
		List<Data> idProts = ds.getRows(identifiedProts);		
		HashSet<Data> intersection = new HashSet<Data>(idProts);
		intersection.retainAll(roi);	
		
		int psig = intersection.size();
		if(!ds.isMock())
			if(proi < 0) proi = psig;
			else throw new RuntimeException("proi is being over-written! Old val = " + proi + " new val=" + psig);
		
		/*	Score 1: Calculates a reward score based 
			largely on how many proteins of a pathway is inside the ROI */
		double coverage = (double) psig / pfound; 
		double exponent = params.getKappaCoefficients(1);
		Double lgamma = Gamma.logGamma(psig * Math.pow(coverage, exponent) + 2); 
		Double  score1 = (pfound != 0) ?  params.getAlphaCoefficients(1) * lgamma : 0;
		
		/*	Score2: Calculates a penalty score based on the 
		*	total number of pathways associated with the proteins in the ROI. */
		Double score2 = 0D;
		for(Data d : intersection){
			for(String acc : d.getProteins()){
				try {
					score2 += dbManager.getAllPathsWithProtein(acc).size();
				} catch (SQLException e) {
					logger.severe("Unexpected database error: " 
							+ System.lineSeparator() 
							+ DebugToolbox.getStackTraceAsString(e)
							+ System.lineSeparator());
					
					System.err.println("Database error occured, please check the logs for more information!");
				}
			}
		}
		
		score2 = 
			(psig!=0 && score2 != 0) ? 
				-1 * params.getAlphaCoefficients(2) * StrictMath.log(score2 / psig) : 0;
					
				
		/*	Score3: Calculates a reward score based on regulation values of the identified proteins. 
		 * 	|fc| * (1-p)^k is the used metric here.
		 */
		Double score3 = 0D;
		double pval, fc, rho;
		for(Data d : idProts){
			pval = d.getPval();
			fc = d.getRatio();
			if(Double.isNaN(pval) || Double.isNaN(fc))
				continue;
			
			fc = (fc > 1) ? fc : (- 1/fc);
			rho = Math.abs(fc) * Math.pow(1-pval,params.getKappaCoefficients(2));
			score3 += rho;
		}
		
		/*	Check if psig == 0, then return 0 so that S3 cannot be 
		 * 	the only nonzero score (psig != 0 -> pf != 0)	*/
		//score3 =  (psig != 0) ?  params.getAlphaCoefficients(3) * score3 / pfound : 0;
		score3 =  params.getAlphaCoefficients(3) * score3 / pfound;
		double score_total = score1 + score2 + score3; 
		
		if(dump_scores)
			logger.fine("Path: " + path.getName() 
					+ System.lineSeparator()
					+ "in ROI:" + intersection.toString()
					+ " PAR scores: "+ score1 + ", " + score2 + ", " + score3
					+ System.lineSeparator());
			
		return score_total;
	}

	/**	Creates an empirical score distribution for a pathway, 
	 * 	then samples from that distribution to check the amount of data point
	 * 	that are greater than the given score value.
	 * 	@param score - a <code>double</code> value to be compared 
	 * 	to the empirical distribution 
	 * 
	 * */
	private double calcParScore(double enrichment){
		double z = 0;
		double[] rand_scores = new double[Dataset.N_REPL];
		for (int i=0; i < rand_scores.length; i++){
			Dataset mock = data.getMockDataset(i);
			rand_scores[i] = calcParEnrichment(mock,false);
		}
			
		EmpiricalDistribution edi = new EmpiricalDistribution(Dataset.EDI_BINS);
		edi.load(rand_scores);
		for (int i=0; i < N_SAMPLES ; i++){
			if (edi.getNextValue() >= enrichment)
				z++;
		}
				
		return (z == 0) ? MIN_PARAM_PVAL : z/N_SAMPLES ;
	}
	
	private double calcPseaScore(){
		List<Data> slist = data.getSortedData();
//		List<Data> idProts = identifiedProts;
		List<Data> idProts = data.getRows(identifiedProts);
		
		double z = 0;
		int m = slist.size();
		int l = idProts.size();
		int deltamax=0;
		
		// sum-zero game
		int reward = m - l;
		int penalty = l;
		int[] runsum = new int[m+1];
		int i = 1;
		
		logger.finer("PSEA runsum params: m=" + m + " l/pen= " + l + "rew=" + reward);
		
		// iterate over the sorted list to calculate the runsum
		runsum[0] = 0;
		for(Data d : slist){
			if(idProts.contains(d))
				runsum[i] = runsum[i-1] + reward;
			else
				runsum[i] = runsum[i-1] - penalty;
			
			deltamax = (deltamax > Math.abs(runsum[i])) ? deltamax : Math.abs(runsum[i]);
			i++;
		}
			
		/* NOTE:
		 * Using logGamma arithmetic to calculate the large factorials.
		 * Omitted for the sake of accuracy, BigIntegerMath from Guava 
		 * library is used to calculate the binomial coeffs, 
		 * it turns out to be just as fast if not faster.
		 */
//		double logTotalPaths = Gamma.logGamma(m+1) - Gamma.logGamma(l+1) - Gamma.logGamma(m-l+1);
//		double logNbrOfPaths = calcPaths_log(m,l,deltamax);
//		z = 1 - Math.exp(logNbrOfPaths - logTotalPaths);
			
		BigInteger totalPaths = BigIntegerMath.binomial(m, l);
		BigInteger nbrOfPaths = calcPaths(m, l, deltamax);
		BigDecimal ratio = new BigDecimal(nbrOfPaths).divide(new BigDecimal(totalPaths),15,RoundingMode.HALF_EVEN);
		z = 1 - ratio.doubleValue();
						
		if(z <= 0){
			logger.warning("'z' is found to be negative or zero for " 
							+ "pathway: " + path.getName()  
							+ ", with m: " + m + " l: " + l + " z: " + z
							+ System.lineSeparator());
			
			if(StrictMath.abs(z) < MIN_PSEA_PVAL)
				z = MIN_PSEA_PVAL;
		}
		
		if(Double.isInfinite(z)){
			logger.severe("'z' is found to be infinite while analyzing " 
							+ "pathway: " + path.getName() 
							+ ", with m: " + m + " l: " + l);
			
			throw new RuntimeException("Possible precision overflow. Please check the logfile for details.");
		}
		
		return (z > MIN_PSEA_PVAL) ? z : MIN_PSEA_PVAL;
//		return z;
	}
	
	/**	<p>Calculates the logarithm of the number of paths the running sum can take 
	 * 	that yield a maximum deviation from zero that is equal to 
	 * 	or greater than <code>max</code>.</p> 
	 * 
	 * 	<p>In order to keep track of the runsum statistic, this method
	 * 	uses a couple of <code>HashMap</code> objects, which keep the 
	 * 	information in pairs of (runsum score, log( # of ways to reach this score)).</p>
	 *  @param m - number of steps for the runsum
	 *  @param l - number of hits to occur during the runsum
	 *  @param max - maximum enrichment score 
	 * */
	
	private Double calcPaths_log(int m, int l, int max){
		double logAcceptedPaths = 0;
		HashMap<Integer,Double> thisStep = new HashMap<Integer,Double>();
		HashMap<Integer,Double>	nextStep = new HashMap<Integer,Double>();
		HashMap<Integer,Double>	tempSwap;
		int reward = m - l;
		int penalty = l;
		
		// intialize
		thisStep.put(0, StrictMath.log(1));
		
		// iterate m steps, and over the "states" of the runsum process
		for(int i=0;i<m;i++){
			for(Map.Entry<Integer,Double> entry : thisStep.entrySet()){
				int sumval = entry.getKey();
				Double logNbrOfWays = entry.getValue();
				
				int isHit = sumval + reward;
				int isMiss = sumval - penalty;
				
				// Manage isHit
				if(isHit < max){
					if(!nextStep.containsKey(isHit))
						nextStep.put(isHit, logNbrOfWays);
					else{
						Double temp = nextStep.get(isHit);
						Double log_sum = temp + StrictMath.log(1 + StrictMath.pow(StrictMath.E, logNbrOfWays-temp));
						nextStep.put(isHit, log_sum);
					}
				}else{	/* stepped over the maximality condition: skip this value	*/	}
					
					
				// Manage isMiss
				if(isMiss > -1 * max){
					if(!nextStep.containsKey(isMiss))
						nextStep.put(isMiss, logNbrOfWays);
					else{
						Double temp = nextStep.get(isMiss);
						Double log_sum = temp + StrictMath.log(1 + StrictMath.pow(StrictMath.E, logNbrOfWays-temp));
						nextStep.put(isMiss, log_sum);
					}
				}else{	/* stepped over the maximality condition: skip this value	*/	}
				
			}
			tempSwap = thisStep;
			thisStep = nextStep;
			nextStep = tempSwap;
			nextStep.clear();
				
			if(thisStep.isEmpty())
				return 0D;
				
		}
		
		// Any runsum that ends up at zero in m steps is feasible, thus:  
		logAcceptedPaths = thisStep.get(Integer.valueOf(0));
		
		if(Double.isInfinite(logAcceptedPaths)){
			StringBuilder sb = new StringBuilder("Precision overflow occured while analyzing path: ");
			sb.append(path.getName())
				.append(System.lineSeparator())
				.append("Runtime arguments for call calcPaths()")
				.append("\t-m=").append(m)
				.append(", l=").append(l)
				.append(", max=").append(max);
			logger.severe(sb.toString());
			throw new RuntimeException("Precision overflow occured, please check log files for more information...");
		}
		else
			return logAcceptedPaths;
		
	}
	
	/**	<p>Calculates the number of paths the running sum can take 
	 * 	that yield a maximum deviation from zero that is equal to 
	 * 	or greater than <code>max</code>.</p> 
	 * 
	 * 	<p>In order to keep track of the runsum statistic, this method
	 * 	uses a couple of <code>HashMap</code> objects, which keep the 
	 * 	information in pairs of (runsum score, # of ways to reach this score).</p>
	 *  @param m - number of steps for the runsum
	 *  @param l - number of hits to occur during the runsum
	 *  @param max - maximum enrichment score 
	 * */
	
	private BigInteger calcPaths(int m, int l, int max){
		HashMap<Integer,BigInteger> thisStep = new HashMap<Integer,BigInteger>();
		HashMap<Integer,BigInteger>	nextStep = new HashMap<Integer,BigInteger>();
		HashMap<Integer,BigInteger>	tempSwap;
		int reward = m - l;
		int penalty = l;
		
		// intialize
		thisStep.put(0, BigInteger.ONE);
		
		// iterate m steps, and over the "states" of the runsum process
		for(int i=0;i<m;i++){
			for(Map.Entry<Integer,BigInteger> entry : thisStep.entrySet()){
				int sumval = entry.getKey();
				BigInteger nbrOfWays = entry.getValue();
				
				int isHit = sumval + reward;
				int isMiss = sumval - penalty;
				
				// Manage isHit
				if(isHit < max){
					if(!nextStep.containsKey(isHit))
						nextStep.put(isHit, nbrOfWays);
					else{
						BigInteger temp = nextStep.get(isHit);
						nextStep.put(isHit, temp.add(nbrOfWays));
					}
				}else{	/* stepped over the maximality condition: skip this value	*/	}
					
					
				// Manage isMiss
				if(isMiss > -1 * max){
					if(!nextStep.containsKey(isMiss))
						nextStep.put(isMiss, nbrOfWays);
					else{
						BigInteger temp = nextStep.get(isMiss);
						nextStep.put(isMiss, temp.add(nbrOfWays));
					}
				}else{	/* stepped over the maximality condition: skip this value	*/	}
				
			}
			tempSwap = thisStep;
			thisStep = nextStep;
			nextStep = tempSwap;
			nextStep.clear();
				
			if(thisStep.isEmpty())
				return BigInteger.ZERO;
				
		}
		
		// Any runsum that ends up at zero in m steps is feasible, thus:  
		return thisStep.get(Integer.valueOf(0));
		
	}
	
}
