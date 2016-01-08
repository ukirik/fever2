package test;

import static org.junit.Assert.*;
import main.Pair;

import org.junit.Before;
import org.junit.Test;

public class PairTest {
	
	Pair<String, String> pp1, pp2, pp3;
	
	@Before
	public void setup(){
		
		pp1 = Pair.of("Q9BW91", "P51665");
		pp2 = Pair.of("P51665", "Q9BW91");
		pp3 = Pair.of("P51661", "Q9BW91");
		
	}
	
	@Test
	public void test() {
		assertTrue(pp1.equals(pp2));
		assertTrue(pp2.equals(pp1));
		assertFalse(pp1.equals(pp3));
		assertFalse(pp2.equals(pp3));
	}

}
