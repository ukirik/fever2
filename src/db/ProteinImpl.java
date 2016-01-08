package db;

import java.io.Serializable;
import java.util.Set;
import java.util.logging.Logger;

import main.Accession;

public class ProteinImpl implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7741322745099399924L;
	private String acc, name, uniprot_id, gene_symbol;
	private Set<String> sec_ids;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

//	private Set<Pathway> associated_paths;
//	private Set<Peptide> associated_peptides;
	
	public ProteinImpl(String acc, String name, String uniprot_id,
			String gene_symbol, Set<String> sec_ids) {
		super();
		this.acc = acc;
		this.name = name;
		this.uniprot_id = uniprot_id;
		this.gene_symbol = gene_symbol;
		this.sec_ids = sec_ids;
//		this.associated_paths = associated_paths;
//		this.associated_peptides = associated_peptides;
	}
	
	public ProteinImpl(){
		this("","","","",null);
	}
	
	public boolean isValid(){
		return Accession.isValid(this.acc) && 
				!this.name.equalsIgnoreCase("") && 
				!this.gene_symbol.equalsIgnoreCase("") &&
				this.sec_ids != null && 
				validateSecondaryAcc(); //&&
//				this.associated_paths != null &&
//				this.associated_peptides != null;
	}
	
	/*	PRIVATE METHODS	*/
	private boolean validateSecondaryAcc(){
		boolean valid = true;
		for(String s : this.sec_ids){
			valid = valid && Accession.isValid(s);
		}
		return valid;
	}
	
	/*	GETTERS & SETTERS */
	public String getAcc() {
		return acc;
	}
	public void setId(String acc) {
		this.acc = acc;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getGene_symbol() {
		return gene_symbol;
	}
	public void setGene_symbol(String gene_symbol) {
		this.gene_symbol = gene_symbol;
	}
	public Set<String> getSec_ids() {
		return sec_ids;
	}
	public void setSec_ids(Set<String> sec_ids) {
		this.sec_ids = sec_ids;
	}
	public String getUniprot_id() {
		return uniprot_id;
	}

	public void setUniprot_id(String uniprot_id) {
		this.uniprot_id = uniprot_id;
	}
	//	public Set<Pathway> getAssociated_paths() {
//		return associated_paths;
//	}
//	public void setAssociated_paths(Set<Pathway> associated_paths) {
//		this.associated_paths = new HashSet<Pathway>(associated_paths);
//	}
//	public Set<Peptide> getAssociated_peptides() {
//		return associated_peptides;
//	}
//	public void setAssociated_peptides(Set<Peptide> associated_peptides) {
//		this.associated_peptides = new HashSet<Peptide>(associated_peptides);
//	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProteinImpl [acc=");
		builder.append(acc);
		builder.append(", name=");
		builder.append(name);
		builder.append(", uniprot_id=");
		builder.append(uniprot_id);
		builder.append(", gene_symbol=");
		builder.append(gene_symbol);
		builder.append(", sec_ids=");
		builder.append(sec_ids);
		builder.append("]");
		return builder.toString();
	}
	
}
