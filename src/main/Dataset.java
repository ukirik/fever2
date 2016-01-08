package main;

import gui.FeverMainFrame;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import main.AnalysisParams.RANDMETHOD;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import db.PathwayImpl;

public class Dataset implements Serializable{
	
	/**
	 * Generated serial version ID
	 */
	private static final long serialVersionUID = 8751535473398855957L;
	
	/**
	 * Boolean field to which indicates whether or not the dataset is original
	 */
	private final boolean isMock;
	
	/**
	 * The type (transformation) of values that this dataset holds,
	 * possible values are described in {@link AnalysisParams} object
	 */
	private final AnalysisParams.RVAL_TYPE valType;
	
	/**
	 * Used to keep track of the statistics of the ratios
	 */
	private final DescriptiveStatistics ratio_stats;
	
	public static final int EDI_BINS = 100;
	public static final int N_REPL = 1000;
	
	private LinkedList<Data> sortedRows;
	private List<Data> rows;
	private HashSet<Data> roi;
	private HashSet<String> uniprot_ids;
	private HashSet<String> peptide_seqs;
	private double[] ratios;
	private List<Dataset> mock_data;
	private volatile boolean finalized;
	private EmpiricalDistribution edi;
	private int row_counter;
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	
	
	/**
	 * Creates a empty, non-mock {@code Dataset}, which is to be populated row-by-row 
	 * later on, hence the created {@code Dataset} is not finalized.
	 */
	public Dataset(){
		this(false, false, new LinkedList<Data>(), new HashSet<String>(), new HashSet<String>());
	}
	
	protected Dataset(boolean isMock, boolean isFinal, 
						List<Data> rows, 
						HashSet<String> uniprot_ids, 
						HashSet<String> peptide_seqs) 
	{
		super();
		this.isMock = isMock;
		this.finalized = isFinal;
		this.rows = rows;
		this.uniprot_ids = uniprot_ids;
		this.peptide_seqs = peptide_seqs;
		this.valType = AnalysisParams.getInstance().getValueType();
		this.edi = new EmpiricalDistribution(EDI_BINS);
		this.ratio_stats = new DescriptiveStatistics();
		this.row_counter = isMock ? Integer.MIN_VALUE : 0;
	}

	public boolean addRow(Data d){
		this.rows.add(d);
		this.ratio_stats.addValue(d.getRatio());
		this.uniprot_ids.addAll(d.getProteins());
		this.peptide_seqs.addAll(d.getPeptides());
		return true;
	}
	
	public boolean addRow(String[] line, ANNOT_TYPE[] annots){
		
		// SANITY CHECKS
		if(finalized)
			throw new UnsupportedOperationException("Cannot add rows to a finalized dataset");
		if(isMock)
			throw new UnsupportedOperationException("Cannot add rows to a mock dataset");
		if(line.length != annots.length){
			StringBuilder msg = new StringBuilder("Array lengths do not match!");
			msg.append(System.lineSeparator())
				.append("row to be added: ").append(Arrays.toString(line))
				.append(System.lineSeparator())
				.append("annotations: ").append(Arrays.toString(annots));
			
			logger.severe(msg.toString());
			throw new IllegalArgumentException("Array lengths do not match!");
		}
		
		DescriptiveStatistics 
			s1 = new DescriptiveStatistics(), 
			s2 = new DescriptiveStatistics();
				
		double pval = Double.NaN, ratio = Double.NaN; 
		List<String> prot = null;
		List<String> pep = null; 
		String sepCh = AnalysisParams.getInstance().getIdSepChar();
		for(int i=0;i<annots.length;i++){
			switch(annots[i]){
			case ProteinID: 
				prot = Arrays.asList(line[i].split(sepCh));
				uniprot_ids.addAll(prot); 
				break;
			case PeptideSeq:
				pep = Arrays.asList(line[i].split(sepCh));
				peptide_seqs.addAll(pep); 
				break;
			case Intensity_S1:
				s1.addValue(valueToIntensity(line[i]));
				break;
			case Intensity_S2:
				s2.addValue(valueToIntensity(line[i]));
				break;
			case Ratio:
				ratio = valueToIntensity(line[i]);
				break;
			case Fold_Ch:
				double d = valueToIntensity(line[i]);
				ratio = d > 0 ? d : -1/d;
				break;
			case Pval:
				if(line[i].equalsIgnoreCase("") || line[i].equalsIgnoreCase("NA"))
					pval = missingVal;
				else
					pval = Double.parseDouble(line[i]);
			default: break;
			}
		}
		
		// Make sure ratio is set
		if(Double.isNaN(ratio))
			ratio = s1.getMean() / s2.getMean();
		
		// At least 2 observations for each sample to calculate a pval
		if(s1.getN() > 1 && s2.getN() > 1)
			pval = TestUtils.tTest(s1, s2);
		
		this.ratio_stats.addValue(ratio);
		if(Arrays.asList(annots).contains(ANNOT_TYPE.PeptideSeq))
			return rows.add(new Data(row_counter++, prot,pep,ratio,pval));
		else
			return rows.add(new Data(row_counter++, prot,ratio,pval));
	}
	
	public double valueToIntensity(String val){
		if(val.equalsIgnoreCase("") || val.equalsIgnoreCase("NA"))
			return missingVal;
		
		
		try{
			double d = Double.parseDouble(val);
			switch(valType){
				case RAW: break;
				case LOG2: 	d = StrictMath.pow(2,d); break;
				case LOGN: 	d = StrictMath.pow(StrictMath.E, d); break;
				case LOG10: d = StrictMath.pow(10,d); break;
				default: throw new RuntimeException("Unrecognized value type");
			}
			
			if(Double.isInfinite(d)){
				StringBuilder msg = new StringBuilder("Double precision overflow occurred: 'd' is infinite!!");
				msg.append(System.lineSeparator())
					.append("chosen value scale is ").append(valType)
					.append(System.lineSeparator())
					.append("value = ").append(val);
				
				logger.severe(msg.toString()  + System.lineSeparator());
				
				System.err.println("Data parsing error!!" +
						"Please make sure that you have selected the correct scale...");
				System.exit(FeverMainFrame.exitCodes.get(this.getClass()));			
			}
			else
				return d;
				
		} catch (NumberFormatException e){
			System.err.println("Data parsing error!!");					
			logger.severe("Expected: string representation of a numerical value, "
							+ "Found: " + val  + System.lineSeparator());
			System.err.println("Please make sure the datafile does not include any strings "
									+ "like 'N/A' or '-' for denoting missing values.");
	
			
			System.exit(FeverMainFrame.exitCodes.get(this.getClass()));			
		}
		
		// TODO: This should never happen!
		throw new RuntimeException("Assertion failed during dataset parsing...");
	}
	
	
	/**
	 * Prevents addition of new data points by altering the {@code finalized} state
	 * and creates the randomized/permuted data based on {@code AnalysisParams}.
	 */
	public synchronized void finalize(){
		if(this.finalized)
			throw new RuntimeException("Dataset already finalized!");
		
		// Generate the randomized values for mock datasets
		mock_data = new ArrayList<Dataset>(N_REPL);
		RandomDataGenerator rng = new RandomDataGenerator();
		int n = getNbrOfRows();
		
		logger.info("Generating mock data... " + System.lineSeparator());
//		DebugToolbox.dumpValues(getNumericRatios());
		
		//edi.load(ratios);
		edi.load(getNumericRatios());
		
		
//		for (SummaryStatistics stats : edi.getBinStats())
//			logger.info(stats.toString());
		
		Data d;
		List<Data> dlist; 
		double ratio, pval;
		
		// TODO: Either add log-normal or remove it as an alternative
		if(AnalysisParams.getInstance().getRandMethod() == RANDMETHOD.EMPIRICAL){	
			for (int j=0; j < N_REPL; j++){
				dlist =  new ArrayList<Data>(n);
				for(int i=0; i < n; i++){
					ratio = edi.getNextValue();
					pval = rng.nextUniform(0, 1);
					d = this.rows.get(i);
					dlist.add(new Data(d.getUid(), d.prot_ids, d.peptides, ratio, pval));
				}
				mock_data.add(new Dataset(true, true, dlist, uniprot_ids, peptide_seqs));				
			}
			logger.info(N_REPL + " mock values generated using Empirical Distribution method" + 
					System.lineSeparator());
		}
		// TODO: consider whether or not to add this feature
//		else if(AnalysisParams.getInstance().getRandMethod() == RANDMETHOD.LOGNORM){
//
//			logger.info(N_REPL + " mock values generated using Log-Normal Distribution method" + 
//					System.lineSeparator());
//		}
		else{
			for (int j=0; j < N_REPL; j++){
				int[] perm = rng.nextPermutation(n, n);
				dlist =  new ArrayList<Data>(n);
				for (int i=0; i < n ; i++){
					d = rows.get(perm[i]);
					ratio = d.getRatio();
					pval = d.getPval();
					d = this.rows.get(i);
					dlist.add(new Data(d.getUid(), d.prot_ids, d.peptides, ratio, pval));
				}
				mock_data.add(new Dataset(true, true, dlist, uniprot_ids, peptide_seqs));
			}
			logger.info(N_REPL + " mock values generated using Permutation method "  + 
					System.lineSeparator());
		}
		
		this.finalized = true;
	}
	
	/**
	 * This method is intended to be used to get mock data such that, either: 
	 * <ul>
	 * <li> the protein/peptide labels and ratio-pvalue pairs in this Dataset are permuted, or
	 * <li> the ratio values are sampled from an empiricial distribution based on the real data, 
	 * while the pvalues sampled from a uniform distribution U(0,1). 
	 * @param {@code i} the ith set of mock data values
	 * @return {@code null} if the {@link Dataset} is not finalized, 
	 * a new {@link Dataset} instance otherwise.  
	 * @throws UnsupportedOperationException if this method is called on a mock dataset
	 * @throws RuntimeException if the method is called more than {@code N_REPL} times
	 */
	public Dataset getMockDataset(int index){
		if(this.isMock)
			throw new UnsupportedOperationException("Mockception: Attempting to access mock data of mock data");
		if(!this.isFinalized())
			return null;
		if(index == N_REPL)
			throw new IllegalArgumentException("rand_counter index: " + index);
		
		return mock_data.get(index);
	}
	
	public synchronized LinkedList<Data> getSortedData(){
		if(sortedRows != null)
			return sortedRows;
		
		sortedRows = new LinkedList<Data>(this.rows);
		switch(AnalysisParams.getInstance().getSortMethod()){
		case FOLDCHANGE: 
			Collections.sort(sortedRows, new Comparator<Data>() {
				@Override
				public int compare(Data arg0, Data arg1) {
					double r1 = arg0.getRatio();
					double r2 = arg1.getRatio();
					
					r1 = r1 < 1 ? -1/ r1 : r1;
					r2 = r2 < 1 ? -1/ r2 : r2;
					
					return Double.compare(r1, r2);
				}
			});
			logger.info("Dataset sorted using Fold Change metric" + System.lineSeparator());
			break; 
			
		case PVALUE:
			Collections.sort(sortedRows, new Comparator<Data>() {
				@Override
				public int compare(Data arg0, Data arg1) {
					return Double.compare(arg0.getPval(), arg1.getPval());
				}
			});
			logger.info("Dataset sorted using P-value metric" + System.lineSeparator());
			break;
			
		case COMB_NONLINEAR:
			Collections.sort(sortedRows, new Comparator<Data>() {
				@Override
				public int compare(Data arg0, Data arg1) {
					double r1 = arg0.getRatio();
					double r2 = arg1.getRatio();
					double exponent = AnalysisParams.getInstance().getKappaCoefficients(2);
					
					r1 = r1 < 1 ? -1/ r1 : r1;
					r2 = r2 < 1 ? -1/ r2 : r2;
				
					double s1 = Math.abs(r1) * Math.pow((1-arg0.getPval()),exponent);
					double s2 = Math.abs(r2) * Math.pow((1-arg1.getPval()),exponent);
			
					return Double.compare(s1, s2);
				}
			});
			logger.info("Dataset sorted using Combined Non-linaer metric" + System.lineSeparator());

			break;
		}
		
		return sortedRows;
	}
	
	public synchronized boolean isFinalized(){
		return this.finalized;
	}
	
	public synchronized boolean isMock(){
		return this.isMock;
	}
	
	public synchronized Set<Data> getROI(){
		if(roi != null)
			return roi;
		
		roi = new HashSet<Dataset.Data>();
		double p_threshold = AnalysisParams.getInstance().getSignLevelThreshold();
		double r_threshold = AnalysisParams.getInstance().getRegLevelThreshold();
		
		double p,r;
		for(Data d : this.rows){
			r = d.getRatio();
			p = d.getPval();
			
			boolean isSig = (p_threshold == AnalysisParams.THRESHOLD_NOT_SET) ||
							p_threshold > p;
							
			boolean isReg = (r_threshold == AnalysisParams.THRESHOLD_NOT_SET) ||
							r_threshold <= ((r > 1) ? r : (1/r));
			
			if(isSig && isReg)
				roi.add(d);
		}
		return Collections.unmodifiableSet(roi);
	}
	
	public double[] getRatios(){
		if(this.ratios != null)
			return this.ratios;
		
		ratios = new double[rows.size()];
		int i=0;
		for(Data d: rows){
			ratios[i] = d.getRatio();
			i++;
		}
		
		return ratios;
	}
	
	public double[] getNumericRatios(){
		ArrayList<Double> rlist = new ArrayList<Double>();
		Double val;
		for(Data d: rows){
			val = d.getRatio();
			if(Double.isNaN(val) || Double.isInfinite(val))
				continue;
			else
				rlist.add(val);
		}
		
		double[] ratios = new double[rlist.size()];
		int i=0;
		for(Double d: rlist){
			ratios[i] = d;
			i++;
		}
		
		return ratios;
		
	}
	
	public List<Data> getDataRows(){
		return Collections.unmodifiableList(this.rows);
	}
	
	public List<Data> getRows(Set<Integer> indices){
		ArrayList<Data> rows = new ArrayList<Data>(indices.size());
		for(Integer i : indices){
			rows.add(this.rows.get(i));
		}
		return rows;
	}
	
	public int getNbrOfRows(){
		return this.rows.size();
	}
	
	public Data getProteinData(String acc){
		if(!this.uniprot_ids.contains(acc))
			return null;
		
		for(Data d : this.rows){
			if(d.getProteins().contains(acc))
				return d;
		}
		
		return null;
	}
	
	public Set<String> getProteinsIds(){
		return Collections.unmodifiableSet(this.uniprot_ids);
	}
	
	public Set<String> getPeptideSeqs(){
		return Collections.unmodifiableSet(this.peptide_seqs);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Dataset:[");
		builder.append("\n- isMock=");
		builder.append(isMock);
		builder.append("\n- valType=");
		builder.append(valType);
		builder.append("\n- nbrOfRows=");
		builder.append(rows.size());

		builder.append("\n- nbrOfProteins=");
		builder.append(uniprot_ids.size());
		builder.append("\n- nbrOfPeptides=");
		builder.append(peptide_seqs.size());
		builder.append("\n- roi=");
		builder.append(getROI().size());
		builder.append(" (");
		builder.append( new DecimalFormat("#0.00%")
							.format(roi.size() / (float) rows.size()));
		builder.append(")");
		
		builder.append("\n- finalized=");
		builder.append(finalized);
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * Inner utility class representing a single row of data, a data-point.
	 * @author Ufuk Kirik
	 *
	 */
	public class Data{
		
		final private int uid;
		final private List<String> prot_ids;
		final private List<String> peptides;
		final private double ratio,pval;
		
		public Data(int id, List<String> prots, double ratio, double pval) {
			if(prots == null)
				throw new IllegalArgumentException("UniProt accesion(s) cannot be null!");
			if(ratio < 0)
				throw new IllegalArgumentException("Ratio cannot be less than zero!");
			if(pval < 0 || pval > 1)
				throw new IllegalArgumentException("P-value cannot be outside range (0,1)!");
			
			this.uid = id;
			this.prot_ids = prots;
			this.peptides = new ArrayList<String>();
			this.ratio = ratio;
			this.pval = pval;
		}
		
		public Data(int id, List<String> prots, List<String> peps, double ratio, double pval) {
			if(prots == null)
				throw new IllegalArgumentException("UniProt accesion(s) cannot be null!");
			if(peps == null)
				throw new IllegalArgumentException("Peptide sequence(s) cannot be null!");
			if(ratio < 0)
				throw new IllegalArgumentException("Ratio cannot be less than zero!");
			if(pval < 0 || pval > 1)
				throw new IllegalArgumentException("P-value cannot be outside range (0,1)!");
			
			this.uid = id;
			this.prot_ids = prots;
			this.peptides = peps;
			this.ratio = ratio;
			this.pval = pval;
		}		
		public int getUid() {
			return uid;
		}
		public List<String> getProteins() {
			return Collections.unmodifiableList(prot_ids);
		}
		public List<String> getPeptides() {
			return peptides;
		}
		public double getRatio() {
			return ratio;
		}
		public double getPval() {
			return pval;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((peptides == null) ? 0 : peptides.hashCode());
			result = prime * result + ((prot_ids == null) ? 0 : prot_ids.hashCode());
			long temp;
			temp = Double.doubleToLongBits(pval);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(ratio);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Data other = (Data) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (peptides == null) {
				if (other.peptides != null)
					return false;
			} else if (!peptides.equals(other.peptides))
				return false;
			if (prot_ids == null) {
				if (other.prot_ids != null)
					return false;
			} else if (!prot_ids.equals(other.prot_ids))
				return false;
			if (Double.doubleToLongBits(pval) != Double
					.doubleToLongBits(other.pval))
				return false;
			if (Double.doubleToLongBits(ratio) != Double
					.doubleToLongBits(other.ratio))
				return false;
			return true;
		}
		private Dataset getOuterType() {
			return Dataset.this;
		}
		@Override
		public String toString() {
			return "Data [prot=" + prot_ids + ", pep=" + peptides + ", ratio=" + ratio
					+ ", pval=" + pval + "]";
		}
	}
	
	
	public class DataFoldChangeComparator<E> implements Comparator<Data> {

		@Override
		public int compare(Data o1, Data o2) {
			// Implements an absolute-value fold change comparison model
			
			double r1 = o1.getRatio();
			double r2 = o2.getRatio();
			
			r1 = r1 < 1 ? -1/ r1 : r1;
			r2 = r2 < 1 ? -1/ r2 : r2;
			
			return Double.compare(r1, r2);
		}

	}
	
	public static final double missingVal = Double.NaN;
	
	public static enum ANNOT_TYPE 
		{Ignore, ProteinID, PeptideSeq, Intensity_S1, Intensity_S2, Ratio, Fold_Ch, Pval}
}
