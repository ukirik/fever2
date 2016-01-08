package db;

import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Trypsinator {

	/**
	 * Sequences already processed is stored in this set to speed up the
	 * searches.
	 */
	private final HashSet<String> processed_seqs;

	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	/**
	 * whether or not KP/RP count as missed cleavage
	 */
	private final boolean IGNORE_PRO_MISSCLEAVAGE = true;

	public Trypsinator() {
		this.processed_seqs = new HashSet<String>(65536);
	}

	public Set<TrypticPeptide> digestSequence(String seq) {
		return getMassSortedView(digestSequence(seq, 0,
				new HashSet<TrypticPeptide>()));
	}

	private Set<TrypticPeptide> digestSequence(String seq, int relPosition,
			Set<TrypticPeptide> accumulator) {

		if (!this.processed_seqs.add(seq))
			return new HashSet<TrypticPeptide>();

		StringBuilder pep = new StringBuilder();
//		TODO: does the following actually do any good?
//		if (calcPepMass(pep.toString()) > MAX_PEP_MASS)
//			return new HashSet<TrypticPeptide>();

		for (int i = 0; i < seq.length(); i++) {
			char aa = seq.charAt(i);
			pep.append(aa);
			if (aa == LYS || aa == ARG) {
				/*
				 * If cut occurs: 1) add left hand side peptide, note that
				 * relPosition starts from 0 (thus +1) 2) digest right hand side
				 * recursively
				 */
				addItem(accumulator, new TrypticPeptide(pep.toString(),
						relPosition + 1));
				addItem(accumulator,
						digestSequence(seq.substring(i + 1, seq.length()),
								relPosition + (i + 1), accumulator));

				/*
				 * If cut does not occur (ie miscleavage): just keep iterating
				 * down the peptide sequence
				 */

			}
		}

		// Try to add C-term peptide at the end of the sequence
		addItem(accumulator, new TrypticPeptide(pep.toString(), relPosition + 1));
		return accumulator;
	}

	private void addItem(Set<TrypticPeptide> set, TrypticPeptide pep) {
		/*
		 * 1) Peptides need to be at least 6 AA long 
		 * 2) Peptides need to be shorter than 75 AAs or lighter than 4000 Da  
		 * 3) Peptides can have at most 1 miscleavage(s) i.e. at most 2 [KR] 
		 * 4) Peptides cannot have unexpected characters such as 
		 * digits or non-descriptive AA chars such as 'X'
		 */

		// if(set.contains(pep))
		// System.err.println("[" + pep + "]: exists already!");

		// boolean crit1 = pep.getLength() >= MIN_PEP_LENGTH;
		// boolean crit2 = calcPepMass(pep.getSeq()) <= MAX_PEP_MASS;
		// boolean crit3 = countKR(pep.getSeq().toCharArray()) <= MAX_MISCLEAVAGES +1;
		// boolean crit4 = pep.getSeq().matches(VALID);
		//
		if (pep.getLength() >= MIN_PEP_LENGTH
				&& pep.getLength() <= MAX_PEP_LENGTH
				&& calcPepMass(pep.getSeq()) <= MAX_PEP_MASS
				&& countKR(pep.getSeq().toCharArray()) <= MAX_MISCLEAVAGES + 1
				&& pep.getSeq().matches(VALID))
			set.add(pep);
	}

	private void addItem(Set<TrypticPeptide> set, Set<TrypticPeptide> peptides) {
		for (TrypticPeptide pep : peptides)
			addItem(set, pep);
	}

	private int countKR(char[] pep) {
		int n = 0;
		for (int i = 0; i < pep.length; i++) {
			char c = pep[i];
			if (c == ARG || c == LYS) {
				n++;
				if (IGNORE_PRO_MISSCLEAVAGE && i + 1 < pep.length
						&& pep[i + 1] == PRO)
					n--;
			}

		}
		return n;
	}

	private TreeSet<TrypticPeptide> getMassSortedView(Set<TrypticPeptide> set) {
		TreeSet<TrypticPeptide> view = new TreeSet<TrypticPeptide>(
				new Comparator<TrypticPeptide>() {
					@Override
					public int compare(TrypticPeptide arg0, TrypticPeptide arg1) {
						return Double.compare(calcPepMass(arg0.getSeq()),
								calcPepMass(arg1.getSeq()));
					}
				});
		view.addAll(set);
		return view;
	}

	static private final char LYS = 'K', ARG = 'R', PRO = 'P';
	static private final String VALID = "[ARNDCEQGHILKMFPSTWYVOU]+";
	static private final int MAX_MISCLEAVAGES = 1;
	static private final int MIN_PEP_LENGTH = 6;
	static private final int MAX_PEP_LENGTH = 75;
	static private final double MAX_PEP_MASS = 4000D;
	static private final double[] AA_MASS = getAAMass();

	public static double calcPepMass(String pep) {
		double total_mass = 18.0105646; // H20
		for (char aa : pep.toCharArray()) {
			total_mass += AA_MASS[aa];
		}
		return total_mass;
	}
	
	private static double[] getAAMass() {
		double[] aaMasses = new double[128];

		aaMasses['G'] = 57.0214636;
		aaMasses['A'] = 71.0371136;
		aaMasses['S'] = 87.0320282;
		aaMasses['P'] = 97.0527636;
		aaMasses['V'] = 99.0684136;
		aaMasses['T'] = 101.0476782;
		aaMasses['C'] = 103.0091854;
		aaMasses['L'] = 113.0840636;
		aaMasses['I'] = 113.0840636;
		aaMasses['X'] = 113.0840636;
		aaMasses['N'] = 114.0429272;
		aaMasses['O'] = 114.0793126;
		aaMasses['B'] = 114.5349350;
		aaMasses['D'] = 115.0269428;
		aaMasses['Q'] = 128.0585772;
		aaMasses['K'] = 128.0949626;
		aaMasses['Z'] = 128.5505850;
		aaMasses['E'] = 129.0425928;
		aaMasses['M'] = 131.0404854;
		aaMasses['H'] = 137.0589116;
		aaMasses['F'] = 147.0684136;
		aaMasses['R'] = 156.1011106;
		aaMasses['Y'] = 163.0633282;
		aaMasses['W'] = 186.0793126;
		// non-standard AAs
		aaMasses['U'] = 150.953636;
		aaMasses['O'] = 237.147727;

		return aaMasses;
	}
}
