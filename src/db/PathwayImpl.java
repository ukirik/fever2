package db;

import java.io.Serializable;
import java.util.logging.Logger;

public class PathwayImpl implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4806289997052966302L;	
	private long id;
	private String name, db, organism;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

//	private Set<Protein> assoc_proteins;

	public PathwayImpl(long id, String name, String db, String organism) {
		super();
		this.id = id;
		this.name = name;
		this.db = db;
		this.organism = organism;
//		this.assoc_proteins = assoc_proteins;
	}
	
	public PathwayImpl(){
		this(0L, "", "", "");
	}
	
	public boolean isValid(){
		return this.id != 0L && 
				!this.name.equalsIgnoreCase("") && 
				!this.db.equalsIgnoreCase("") &&
				!this.organism.equalsIgnoreCase(""); // &&
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDb() {
		return db;
	}
	public void setDb(String db) {
		this.db = db;
	}
	public String getOrganism() {
		return organism;
	}
	public void setOrganism(String organism) {
		this.organism = organism;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PathwayImpl [id=");
		builder.append(id);
		builder.append(", name=");
		builder.append(name);
		builder.append(", db=");
		builder.append(db);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((db == null) ? 0 : db.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((organism == null) ? 0 : organism.hashCode());
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
		PathwayImpl other = (PathwayImpl) obj;
		if (db == null) {
			if (other.db != null)
				return false;
		} else if (!db.equals(other.db))
			return false;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (organism == null) {
			if (other.organism != null)
				return false;
		} else if (!organism.equals(other.organism))
			return false;
		return true;
	} 
	
}
