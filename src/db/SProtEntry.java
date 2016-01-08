package db;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import main.Accession;

public class SProtEntry {
	
	private String acc, uid, name, gene, organism, seq;
	private Date last_update;
	private Set<FuncAnnotation> annotations;
	private Set<String> secondary_accs;
	private Set<PTMAnnotation> ptms;
	protected enum VALIDITY {VALID, INVALID, EMPTY};
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	
	public SProtEntry(String acc, String uid, String name, String gene, 
			String organism, String seq, Date last_update, Set<String> secondary_accs,
			Set<PTMAnnotation> ptms, Set<FuncAnnotation> annotations) {
		super();
		this.acc = acc;
		this.uid = uid;
		this.name = name;
		this.gene = gene;
		this.seq = seq;
		this.organism = organism;
		this.last_update = last_update;
		this.secondary_accs = secondary_accs;
		this.ptms = ptms;
		this.annotations = annotations;
	}

	public SProtEntry(){
		this("","","","","","",null,null,null,null);
	}
	
	public boolean isValid(){
		return 
			Accession.isValid(this.acc) &&
			!this.uid.equals("") && 
			!this.name.equals("") &&
			!this.organism.equals("") &&
			!this.seq.equals("") &&
			this.last_update != null &&
			this.secondary_accs != null &&
			this.ptms != null &&
			this.annotations != null;
	}
	
	public String getAcc() {
		return this.acc;
	}

	public void setAcc(String acc) {
		this.acc = acc;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getGeneSymbols() {
		return gene;
	}

	public void setGeneSymbols(String gene) {
		this.gene = gene;
	}

	public String getOrganism() {
		return organism;
	}

	public void setOrganism(String organism) {
		this.organism = organism;
	}
	
	public String getSequence() {
		return seq;
	}

	public void setSequence(String sequence) {
		this.seq = sequence;
	}

	public Date getLast_update() {
		return last_update;
	}

	public void setLast_update(Date last_update) {
		this.last_update = last_update;
	}
	
	public Set<String> getSecondaryAccs(){
		return this.secondary_accs;
	}
	
	public void setSecondaryAccs(Set<String> s){
		this.secondary_accs = s;
	}
	
	public Set<PTMAnnotation> getPTMs(){
		return this.ptms;
	}
	
	public void setPTMs(Set<PTMAnnotation> s){
		this.ptms = s;
	}	
		
	public Set<FuncAnnotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Set<FuncAnnotation> annotations) {
		this.annotations = annotations;
	}
	

	@Override
	public String toString() {
		return "SProtEntry [acc=" + acc + ", uid=" + uid + ", annotations="
				+ annotations + ", secondary_accs=" + secondary_accs + "]";
	}

	/*
	 * Public fields relating to PTM annotations 
	 */
	public static enum PTMType {pS, pT, pY};
	private static final HashMap<String, PTMType> PTM;
	static{
		PTM = new HashMap<String, PTMType>();
		PTM.put("Phosphoserine", PTMType.pS);
		PTM.put("Phosphothreonine", PTMType.pT);
		PTM.put("Phosphotyrosine", PTMType.pY);
	}
	
	public static PTMType getType(String s){
		return PTM.get(s);
	}
	
	public void addPTMAnnotation(PTMType type, int pos) {			
		this.ptms.add(new PTMAnnotation(type, pos));	
	}
	
	public class PTMAnnotation{
		private PTMType type;
		private int position;
		
		public PTMAnnotation(PTMType type, int position) {
			super();
			this.type = type;
			this.position = position;
		}

		public PTMType getType() {
			return type;
		}

		public int getPosition() {
			return position;
		}

		@Override
		public String toString() {
			return "PTMAnnotation [type=" + type + ", position=" + position
					+ "]";
		}

	}
	
	public class FuncAnnotation{
		
		private String name, id, source;

		public FuncAnnotation(String name, String id, String source) {
			super();
			this.name = name;
			this.id = id;
			this.source = source;
		}

		public String getName() {
			return name;
		}

		public String getId() {
			return id;
		}

		public String getSource() {
			return source;
		}

		@Override
		public String toString() {
			return "FuncAnnotation [name=" + name + ", id=" + id + ", source="
					+ source + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
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
			FuncAnnotation other = (FuncAnnotation) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}

		private SProtEntry getOuterType() {
			return SProtEntry.this;
		}
	
	}

	
}
