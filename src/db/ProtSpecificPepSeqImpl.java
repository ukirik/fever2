package db;

import java.util.List;
import java.util.logging.Logger;

public class ProtSpecificPepSeqImpl extends GenericPepSeqImpl {
	
	String prot_acc;
	int start, stop;
	List<String> ptms;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	public ProtSpecificPepSeqImpl(
			String seq, String acc, 
			int start, int stop, List<String> ptms){
		
		super(seq);
		this.prot_acc = acc;
		this.start = start;
		this.stop = stop;
		this.ptms = ptms;
	}

	public String getProt_acc() {
		return prot_acc;
	}

	public int getStart() {
		return start;
	}

	public int getStop() {
		return stop;
	}

	public List<String> getPtms() {
		return ptms;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((prot_acc == null) ? 0 : prot_acc.hashCode());
		result = prime * result + ((ptms == null) ? 0 : ptms.hashCode());
		result = prime * result + start;
		result = prime * result + stop;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProtSpecificPepSeqImpl other = (ProtSpecificPepSeqImpl) obj;
		if (prot_acc == null) {
			if (other.prot_acc != null)
				return false;
		} else if (!prot_acc.equals(other.prot_acc))
			return false;
		if (ptms == null) {
			if (other.ptms != null)
				return false;
		} else if (!ptms.equals(other.ptms))
			return false;
		if (start != other.start)
			return false;
		if (stop != other.stop)
			return false;
		return true;
	}
	
	
}
