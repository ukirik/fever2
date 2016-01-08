package test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import main.AnalysisParams;
import main.Dataset;
import main.Dataset.ANNOT_TYPE;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.Test;

public class DatasetTest {
	
	Dataset ds;
	AnalysisParams param = AnalysisParams.getInstance();
	ANNOT_TYPE[] annots = new ANNOT_TYPE[]{ANNOT_TYPE.ProteinID, ANNOT_TYPE.Fold_Ch, ANNOT_TYPE.Pval};
	String[] lines = new String[]{
		"A0AVT1;A0AVT1-2,0.704027237592198,0.183918203320098",
		"A1A528;O43264,0.991452584766591,0.987345450256645",
		"A1L0T0;E9PL44;E9PJS0,-1.26192737955577,0.477088483107683",
		"Q99798;A2A274,0.75343154724252,0.282035918469567"
	};
	
	Random r;
	
	@Before
	public void setup(){
		param.addParam(AnalysisParams.sepchar_key, 0);
		param.addParam(AnalysisParams.value_key, 0);
		ds = new Dataset();
		for(String str : lines)
			ds.addRow(str.split(","), annots);
		
	}
	
	@Test
	public void testBasicFunctionality() {

		assertTrue("Dataset size is not right", ds.getNbrOfRows() == 4);
		assertTrue(ds.getPeptideSeqs().isEmpty());
		assertTrue(ds.getProteinsIds().size() == 9);
		assertTrue(ds.getRows(new HashSet<Integer>(Arrays.asList(1,2,3))).size() == 3);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testPermutationRandomization(){
		param.addParam(AnalysisParams.rand_key, AnalysisParams.RANDMETHOD.PERMUTATION);
		ds.finalize();
		Dataset mock = ds.getMockDataset(1);
		
		assertTrue(ds.getNbrOfRows() == mock.getNbrOfRows());
		assertTrue(ds.getDataRows().size() == mock.getDataRows().size());
		assertTrue(ds.getPeptideSeqs().equals(mock.getPeptideSeqs()));
		assertTrue(ds.getProteinsIds().equals(mock.getProteinsIds()));
		
		double[] r1 = ds.getRatios();
		double[] r2 = mock.getRatios();
		Arrays.sort(r1);
		Arrays.sort(r2);
		
		assertTrue(Arrays.equals(r1, r2));		
		mock.getMockDataset(1);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testEmpiricalRandomization(){
		param.addParam(AnalysisParams.rand_key, AnalysisParams.RANDMETHOD.EMPIRICAL);
		ds.finalize();
		Dataset mock = ds.getMockDataset(2);
		
		assertTrue(ds.getNbrOfRows() == mock.getNbrOfRows());
		assertTrue(ds.getDataRows().size() == mock.getDataRows().size());
		assertTrue(ds.getPeptideSeqs().equals(mock.getPeptideSeqs()));
		assertTrue(ds.getProteinsIds().equals(mock.getProteinsIds()));
		
		assertThat(ds.getRatios(), IsNot.not(IsEqual.equalTo(mock.getRatios())));
		
		mock.getMockDataset(2);
		
	}

}
