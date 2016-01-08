package db;

import java.util.logging.Logger;

public class GenericPepSeqImpl {
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	String sequence;
	int length;
	
	public GenericPepSeqImpl(String seq){
		this.sequence = seq;
		this.length = seq.length();
	}

	public String getSequence() {
		return sequence;
	}

	public int getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + length;
		result = prime * result
				+ ((sequence == null) ? 0 : sequence.hashCode());
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
		GenericPepSeqImpl other = (GenericPepSeqImpl) obj;
		if (length != other.length)
			return false;
		if (sequence == null) {
			if (other.sequence != null)
				return false;
		} else if (!sequence.equals(other.sequence))
			return false;
		return true;
	}

}
