package db;

import java.io.Serializable;

@Deprecated
public class PeptideImpl implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8362385936995013150L;
	private long id;
	private String seq;
//	private Set<Protein> assoc_proteins;
	
	public PeptideImpl(long id, String seq) {
		super();
		this.id = id;
		this.seq = seq;
//		this.assoc_proteins = assoc_proteins;
	}
	
	public PeptideImpl(){
		this(0L, "");
	}
	
	public boolean isValid(){
		return this.id != 0L && 
				!this.seq.equalsIgnoreCase(""); // && 
//				this.assoc_proteins != null && 
//				validateAssociatedProteins();				
	}

	/*	PRIVATE METHODS	*/
	
//	private boolean validateAssociatedProteins() {
//		boolean valid = true;
//		for (Protein p : this.assoc_proteins){
//			valid = valid && p.isValid();
//		}
//		return valid;
//	}
	
	/*	GETTERS & SETTERS */
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getSeq() {
		return seq;
	}
	public void setSeq(String seq) {
		this.seq = seq;
	}
//	public Set<Protein> getAssoc_proteins() {
//		return assoc_proteins;
//	}
//	public void setAssoc_proteins(Set<Protein> assoc_proteins) {
//		this.assoc_proteins = new HashSet<Protein>(assoc_proteins);
//	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PeptideImpl [id=");
		builder.append(id);
		builder.append(", seq=");
		builder.append(seq);
		builder.append("]");
		return builder.toString();
	}

}
