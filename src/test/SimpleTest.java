package test;

import java.io.IOException;
import main.Accession;
import main.ResourceUtils;

public class SimpleTest {

	public static void main(String[] args) throws IOException{
		for(String s : ResourceUtils.listFiles(Accession.class, "/res"))
			System.out.println(s);
	}
	
}
