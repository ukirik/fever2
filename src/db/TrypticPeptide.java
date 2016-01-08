package db;

import java.util.logging.Logger;

public class TrypticPeptide { // implements Comparable<TrypticPeptide>{

	private String seq;
	private int start, stop;
	private String modifications;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	public TrypticPeptide(String seq, int start) {
		super();
		this.seq = seq;
		this.start = start;
		this.stop = start + seq.length();
	}

	public String getSeq() {
		return seq;
	}

	public int getStart() {
		return start;
	}

	public int getStop() {
		return stop;
	}

	public int getLength() {
		return seq.length();
	}

	public String getModifications() {
		return modifications;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((seq == null) ? 0 : seq.hashCode());
		result = prime * result + start;
		result = prime * result + stop;
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
		TrypticPeptide other = (TrypticPeptide) obj;
		if (seq == null) {
			if (other.seq != null)
				return false;
		} else if (!seq.equals(other.seq))
			return false;
		if (start != other.start)
			return false;
		if (stop != other.stop)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TryPep [seq=" + seq + ", mass= " + Trypsinator.calcPepMass(seq)
				+ ", start=" + start + ", stop=" + stop + "]";
	}
}
