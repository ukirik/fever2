package db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import main.Accession;

public class DbManager {

	private Connection conn;
	private PreparedStatement proteinQuery;
	private PreparedStatement pathUIDQuery;
	private PreparedStatement pathNameQuery;
	private PreparedStatement pathParentQuery;
	private PreparedStatement allProtsInPathQuery;
	private PreparedStatement allPathsWithProtQuery;
	private PreparedStatement allPepsInProteinQuery;
	private PreparedStatement allProtsContainingPepSeqQuery;

	private String dbName, connString;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	
	public DbManager(File parent, String dbname) throws SQLException{
		String path = "";
		if(parent != null)
			path = path + parent.getAbsolutePath() + System.getProperty("file.separator");
		
		dbName = dbname;
		init(path + dbname);
		
	}
	
	public DbManager(File dbFile) throws SQLException{
		dbName = dbFile.getName().substring(0, dbFile.getName().indexOf("."));
		String dbpath = dbFile.getAbsolutePath();
		if(dbpath.contains("."))
			dbpath = dbpath.substring(0, dbpath.indexOf('.'));
		init(dbpath);		
	}
	
	private void init(String path2File) throws SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append("jdbc:hsqldb:file:");
		sb.append(path2File);
		sb.append(";");
		
		sb.append("hsqldb.script_format=3").append(";");
		connString = sb.toString();
		
		logger.info("Attempting to connect to DB at " + connString + System.lineSeparator());
		conn = DriverManager.getConnection(connString, "SA", "");
		conn.setReadOnly(true);
		
		// Prepared statements that will be used over and over
		proteinQuery = conn.prepareStatement("select * from PROTEININFO where ACC=(?)");
		pathUIDQuery = conn.prepareStatement("select * from PATHWAYINFO where UID=(?)");
		pathNameQuery = conn.prepareStatement("select * from PATHWAYINFO where Name=(?) and DB=(?)");
		pathParentQuery = conn.prepareStatement("select * from PATHWAYINFO where Parent=(?)");
		
		allProtsInPathQuery = 
			conn.prepareStatement(
				"select ACC, UNIPROT_ID, PROTEININFO.NAME, GENE_SYMBOL from " + 
					"(select * from PATHWAYINFO where UID =(?)) " + 
						"inner join PATHWAYASSOC on UID = PATHWAY_UID " +
						"inner join PROTEININFO on ACCESSION = ACC");
		
		allPathsWithProtQuery = 
			conn.prepareStatement(
				"select UID, PATHWAYINFO.NAME, DB, ORGANISM from " + 
					"(select * from PROTEININFO where ACC = (?)) " + 
						"inner join PATHWAYASSOC on ACCESSION = ACC " +
						"inner join PATHWAYINFO on PATHWAY_UID = UID");
		
		allPepsInProteinQuery = 
			conn.prepareStatement(
					"select ACC, UNIPROT_ID, SEQUENCE, START, STOP, PTMS from " + 
						"(select ACC, UNIPROT_ID from PROTEININFO where ACC = (?)) " + 
							"inner join PEPTIDEINFO on ACC = PROTEIN ");
		
		allProtsContainingPepSeqQuery =
			conn.prepareStatement(
					"select ACC, UNIPROT_ID, SEQUENCE, START, STOP, PTMS from " + 
						"(select * from PEPTIDEINFO where SEQUENCE = (?)) " + 
							"inner join PROTEININFO on ACC = PROTEIN ");
	}
	
	public String getDBName(){
		return dbName;
	}
	
	/**
	 * Retrieves all pathways stored in the database
	 * @return a set containing {@code PathwayImpl} instances if the DB contains any, {@code null} otherwise
	 * @throws SQLException
	 */
	public Set<PathwayImpl> getAllPaths() throws SQLException{
		logger.finer("Attempting to query all pathways"  + System.lineSeparator());
		Statement s = conn.createStatement();
		ResultSet set = s.executeQuery("select * from PATHWAYINFO");
		Set<PathwayImpl> paths = new HashSet<PathwayImpl>();
		PathwayImpl path;
		
		if(set.isBeforeFirst()){
			while(set.next()){
				path = new PathwayImpl(
						set.getLong("uid"), 
						set.getString("name"), 
						set.getString("db"), 
						set.getString("organism"));
				paths.add(path);				
			}
		}
		return paths;
	}
	
	/**
	 * Retrieves ant protein hits associated with the given uniprot accession
	 * @param acc
	 * @return a ProteinImpl if the accession exists in the DB, null otherwise
	 * @throws SQLException
	 */
	public ProteinImpl getProteinByAccession(String acc) throws SQLException{
		logger.finer("Attempting to query protein accession: " + acc  + System.lineSeparator());
		if(!Accession.isValid(acc)){
			logger.severe("given accession " 
					+ "(  '" + acc + "' ) "  
					+ "is not valid! Accession ignored..."
					+ System.lineSeparator());

			//throw new IllegalArgumentException("'" + acc + "' is not a valid accession");			
		}
		
		proteinQuery.setString(1, acc);
		ResultSet set = proteinQuery.executeQuery();
		ProteinImpl prot = null;
		
		if(set.isBeforeFirst()){
			set.next();
			prot = new ProteinImpl(
				set.getString("acc"),
				set.getString("name"),
				set.getString("uniprot_id"),
				set.getString("gene_symbol"),
				new HashSet<String>());
			
			if(set.next()){
				logger.severe("Multiple protein hits for the same accession" + System.lineSeparator());
				throw new RuntimeException("Multiple protein hits for the same accession");
			}
		}
		
		if (prot == null)
			logger.info("A protein with accession: " + acc + " was not found in the database!");
		
		return prot;
	}
	
	public Set<ProteinImpl> matchProteinGroup2DB(List<String> accs) throws SQLException{
		LinkedHashSet<ProteinImpl> results = new LinkedHashSet<ProteinImpl>();
		ProteinImpl prot;

		for (String acc : accs){
			prot = getProteinByAccession(acc);
			if (prot != null)
				results.add(prot);
		}
		
		return results;
	}
	
	/**
	 * Retrieves pathway with the given UID
	 * @param uid
	 * @return a {@code PathwayImpl} instance
	 * @throws SQLException
	 */
	public PathwayImpl getPathwayByUID(long uid) throws SQLException {
		logger.finer("Attempting to query pathway by uid: " + uid  + System.lineSeparator());
		pathUIDQuery.setLong(1, uid);
		ResultSet set = pathUIDQuery.executeQuery();
		PathwayImpl path = null;
		
		if(set.isBeforeFirst()){
			set.next();
			path = new PathwayImpl(
					set.getLong("uid"), 
					set.getString("name"), 
					set.getString("db"), 
					set.getString("organism"));
			
			if(set.next()){
				logger.severe("Multiple pathway hits for the same uid" + System.lineSeparator());
				throw new RuntimeException("Multiple hits for the same uid!");
			}
		}
		return path;
	}
	
	/**
	 * Retrieves pathway with the given UID
	 * @param name
	 * @param db
	 * @return a {@code PathwayImpl} instance
	 * @throws SQLException
	 */
	public PathwayImpl getPathwayByName(String pathName, String db) throws SQLException {
		logger.finer("Attempting to query pathway by name: " 
						+ pathName  + " from db " + db + System.lineSeparator());
		
		pathNameQuery.setString(1, pathName);
		pathNameQuery.setString(2, db);
		ResultSet set = pathNameQuery.executeQuery();
		PathwayImpl path = null;
		
		if(set.isBeforeFirst()){
			set.next();
			path = new PathwayImpl(
					set.getLong("uid"), 
					set.getString("name"), 
					set.getString("db"), 
					set.getString("organism"));
			
			if(set.next()){
				logger.severe("Multiple pathway hits for the same name and db" + System.lineSeparator());
				throw new RuntimeException("Multiple hits for the same uid!");
			}
		}
		return path;
		
	}
	
	public Set<PathwayImpl> getPathwaysByParent(String parentRDF) throws SQLException{
		Set<PathwayImpl> paths = new HashSet<PathwayImpl>();
		ResultSet set;
		if(parentRDF != null){
			pathParentQuery.setString(1, parentRDF);
			set = pathParentQuery.executeQuery();
		}
		else{
			set = conn.createStatement().executeQuery("select * from PathwayInfo where Parent is null");
		}
		 
		PathwayImpl path = null;	
		if(set.isBeforeFirst()){
			while(set.next()){
				path = new PathwayImpl(
						set.getLong("uid"),
						set.getString("name"),
						set.getString("db"),
						set.getString("organism"));
				
				paths.add(path);
			}		
		}
		return paths;
	}
	
	public String getPathwayRDFID(long uid) throws SQLException{
		logger.finer("Attempting to query pathway by uid: " + uid  + System.lineSeparator());
		pathUIDQuery.setLong(1, uid);
		ResultSet set = pathUIDQuery.executeQuery();
		String rdfid = null;
		
		if(set.isBeforeFirst()){
			set.next();
			rdfid = set.getString("rdfid");
			if(set.next()){
				logger.severe("Multiple pathway hits for the same uid" + System.lineSeparator());
				throw new RuntimeException("Multiple hits for the same uid!");
			}
		}
		return rdfid;
	}
	
	/**
	 * Retrieves all proteins in pathway
	 * @param uid unique ID for the queried pathway
	 * @return a set of {@code ProteinImpl} instances that are associated with the given pathway
	 * @throws SQLException
	 */
	public Set<ProteinImpl> getAllProtsInPath(long uid) throws SQLException{
		logger.finer("Attempting to query all proteins in pathway uid: " + uid + 
				System.lineSeparator());

		Set<ProteinImpl> prots = new HashSet<ProteinImpl>();
		allProtsInPathQuery.setLong(1, uid);
		ResultSet set = allProtsInPathQuery.executeQuery();
		ProteinImpl prot = null;
		
		if(set.isBeforeFirst()){
			while(set.next()){
				prot = new ProteinImpl(
						set.getString("acc"),
						set.getString("name"),
						set.getString("uniprot_id"),
						set.getString("gene_symbol"),
						new HashSet<String>());
				
				prots.add(prot);
			}
			
		}
		return prots;		
	}
	
	/**
	 * Retrieves all pathways in the DB that are associated with the given
	 * UniProt accession. 
	 * @param acc UniProt accession
	 * @return a set of {@code PathwayImpl} instances that represent the pathways
	 * @throws SQLException
	 */
	public Set<PathwayImpl> getAllPathsWithProtein(String acc) throws SQLException{
		logger.finer("Attempting to query all pathways containing: " + acc + 
				System.lineSeparator());

		Set<PathwayImpl> paths = new HashSet<PathwayImpl>();
		allPathsWithProtQuery.setString(1, acc);
		ResultSet set = allPathsWithProtQuery.executeQuery();
		PathwayImpl path = null;
		
		if(set.isBeforeFirst()){
			while(set.next()){
				path = new PathwayImpl(
						set.getLong("uid"),
						set.getString("name"),
						set.getString("db"),
						set.getString("organism"));
				
				paths.add(path);
			}		
		}
		return paths;
	}
	
	/**
	 * Retrieves all the peptide sequences in the DB that are
	 * associated with the given UniProt accession.
	 * @param acc - UniProt accession
	 * @return a set of {@code ProtSpecificPepSeq} instances that 
	 * are associated with this particular protein.
	 * @throws SQLException
	 */
	public Set<ProtSpecificPepSeqImpl> getAllPepsInProtein(String acc) throws SQLException{
		Set<ProtSpecificPepSeqImpl> peps = new HashSet<ProtSpecificPepSeqImpl>();
		allPepsInProteinQuery.setString(1, acc);
		ResultSet set = allPepsInProteinQuery.executeQuery();
		ProtSpecificPepSeqImpl pep = null;
		
		if(set.isBeforeFirst()){
			while(set.next()){
				String ptms_text = set.getString("ptms");
				pep = new ProtSpecificPepSeqImpl(
						set.getString("sequence"),
						set.getString("acc"),
						set.getInt("start"),
						set.getInt("stop"),
						Arrays.asList(ptms_text.split(";")));
				peps.add(pep);
			}		
		}
		return peps;
	}
	
	/**
	 * Retrieves <b> only </b> the accessions of all proteins 
	 * that include this particular peptide sequence. 
	 * @param seq - peptide seqeunce
	 * @return a set of {@code String}s that represent the accessions 
	 * of the proteins which contain the given peptide seqeunce. 
	 * @throws SQLException
	 */
	public Set<String> getAllProtsContainingPepSeq(String seq) throws SQLException{
		Set<String> accs = new HashSet<String>();
		allProtsContainingPepSeqQuery.setString(1, seq);
		ResultSet set = allProtsContainingPepSeqQuery.executeQuery();
		
		if(set.isBeforeFirst()){
			while(set.next()){
				accs.add(set.getString("acc"));
			}		
		}
		return accs;
		
	}
	
	/**
	 * Retrieves all protein specific peptide sequence matches, the returned 
	 * objects have protein association, start-stop coordinates together with 
	 * any potential PTMs.
	 * @param seq
	 * @return a set of {@code ProtSpecificPepSeq} instances that match the given sequence 
	 * @throws SQLException
	 */
	public Set<ProtSpecificPepSeqImpl> getAllProtSpecificPepSeqs(String seq) throws SQLException{
		Set<ProtSpecificPepSeqImpl> peps = new HashSet<ProtSpecificPepSeqImpl>();
		allProtsContainingPepSeqQuery.setString(1, seq);
		ResultSet set = allProtsContainingPepSeqQuery.executeQuery();
		ProtSpecificPepSeqImpl pep = null;
		
		if(set.isBeforeFirst()){
			while(set.next()){
				String ptms_text = set.getString("ptms");
				pep = new ProtSpecificPepSeqImpl(
						set.getString("sequence"),
						set.getString("acc"),
						set.getInt("start"),
						set.getInt("stop"),
						Arrays.asList(ptms_text.split(";")));
				peps.add(pep);
			}		
		}
		return peps;
	}

	
	public void closeResources() throws SQLException{
		proteinQuery.closeOnCompletion();
		pathUIDQuery.closeOnCompletion();
		allPathsWithProtQuery.closeOnCompletion();
		allProtsInPathQuery.closeOnCompletion();
		allPepsInProteinQuery.closeOnCompletion();
		allProtsContainingPepSeqQuery.closeOnCompletion();
		conn.close();
	}

}
