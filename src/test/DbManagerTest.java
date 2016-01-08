package test;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.junit.BeforeClass;
import org.junit.Test;

import db.DbManager;
import db.PathwayImpl;
import db.ProteinImpl;

public class DbManagerTest {

	static File dbfile;
	static String dbname;
	static DbManager dbm;
	static JFileChooser jfc;
	
	// PARAMETERS THAT MIGHT CHANGE
	long uid = 2352783018612310467L;
	String acc = "P04637";
	int pathsize = 17390,
		paths_containing_acc = 190,
		prots_in_path = 48;
	
	
	@BeforeClass
	public static void setUp() throws SQLException, InvocationTargetException, InterruptedException{		

		SwingUtilities.invokeAndWait(new Runnable() {	
			@Override
			public void run() {
				jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				jfc.setVisible(true);
				if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					dbfile = jfc.getSelectedFile();
					dbname = dbfile.getName();
					dbname = dbname.substring(0, dbname.indexOf("."));		
					System.out.println(dbname);
				}
			}
		});

		dbm = new DbManager(dbfile.getParentFile(), dbname);
	}
	
	@Test
	public void testAllPaths() throws SQLException {
		assertTrue(dbm.getAllPaths().size() == pathsize);
	}
	
	@Test
	public void testProtByAccession() throws SQLException{
		ProteinImpl prot = dbm.getProteinByAccession(acc);
		assertTrue(prot.getAcc().equalsIgnoreCase(acc));
		assertTrue(prot.getName().equalsIgnoreCase("Cellular tumor antigen p53"));
		assertTrue(prot.getGene_symbol().equalsIgnoreCase("TP53"));
		assertTrue(prot.getUniprot_id().equalsIgnoreCase("P53_HUMAN"));
	}
	
	@Test
	public void testPathByUID() throws SQLException{
		PathwayImpl path = dbm.getPathwayByUID(uid); 
		assertTrue(path.getId() == uid);
		assertTrue(path.getName().equalsIgnoreCase("C:transcriptional repressor complex"));
		assertTrue(path.getDb().equalsIgnoreCase("GO"));
		assertTrue(path.getOrganism().contains("Human"));
		assertTrue(path.getOrganism().contains("Homo sapiens"));
	}
	
	@Test
	public void testPathByParent() throws SQLException{
		Set<PathwayImpl> paths = dbm.getPathwaysByParent(null); 
		assertTrue(!paths.isEmpty());
		assertTrue(paths.size() == 39);	// This might change with DB version
	}
	
	@Test
	public void testPathByParent2() throws SQLException{
		Set<PathwayImpl> paths = dbm.getPathwaysByParent("Reactome#Pathway1175"); 
		assertTrue(!paths.isEmpty());
		assertTrue(paths.size() == 2);	// This might change with DB version
	}
	
	@Test
	public void testAllProts4Path() throws SQLException{
		assertTrue(dbm.getAllProtsInPath(uid).size() == prots_in_path);
	}
	
	@Test
	public void testAllPaths4Prot() throws SQLException{
		assertTrue(dbm.getAllPathsWithProtein(acc).size() == paths_containing_acc);
	}
	
	@Test
	public void testAllPeps4Prot() throws SQLException{
		assertTrue(dbm.getAllPepsInProtein(acc).size() == 65);
	}
	
	@Test
	public void testAllProtsContainingPep() throws SQLException{
		Set<String> s = dbm.getAllProtsContainingPepSeq("AHSSHLK");
		assertTrue(s.size() == 1);
		assertTrue(s.contains("P04637"));
		
		s = dbm.getAllProtsContainingPepSeq("EAPIDK");
		assertTrue(s.size() == 3);
		assertTrue(s.contains("O14950") && s.contains("P19105") && s.contains("P24844"));
		
		s = dbm.getAllProtsContainingPepSeq("AVKPKAAK");
		assertTrue(s.size() == 4);
		assertTrue(s.contains("P10412") && s.contains("P16401") 
				&& s.contains("P16403") && s.contains("Q02539"));
		
		s = dbm.getAllProtsContainingPepSeq("QENEEK");
		assertTrue(s.size() == 7);
	}

}
