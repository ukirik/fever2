package test;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.junit.BeforeClass;
import org.junit.Test;

import db.DbManager;
import db.PathwayImpl;
import graph.PathwayGraph;
import main.AnalysisResult;

public class PathwayGraphTest {
	
	static File dbfile;
	static String dbname;
	static DbManager dbm;
	static JFileChooser jfc;
	static Map<PathwayImpl, AnalysisResult> mockresults;

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
		
		mockresults = new Map<PathwayImpl, AnalysisResult>(){

			@Override
			public int size() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean isEmpty() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean containsKey(Object key) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean containsValue(Object value) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public AnalysisResult get(Object key) {
				return new AnalysisResult((PathwayImpl)(key), 0.0001d, 0.1111d, 42d, 1, 2);
			}

			@Override
			public AnalysisResult put(PathwayImpl key, AnalysisResult value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public AnalysisResult remove(Object key) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void putAll(Map<? extends PathwayImpl, ? extends AnalysisResult> m) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void clear() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Set<PathwayImpl> keySet() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Collection<AnalysisResult> values() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<java.util.Map.Entry<PathwayImpl, AnalysisResult>> entrySet() {
				// TODO Auto-generated method stub
				return null;
			}	
		};
	}
	
	@Test
	public void test() {
		PathwayGraph pg = new PathwayGraph(dbm, mockresults);
		assertTrue("Testing number of nodes", pg.getNbrOfNodes() == pg.getRoot().getNodeCounter());
		assertTrue("Pg.nbrOfNodes = " + pg.getNbrOfNodes(), pg.getNbrOfNodes() == 1689);
	}

}
