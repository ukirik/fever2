package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import db.PathwayImpl;

public class TextFileParser extends AbstractInputFileParser {
	
	private String columnSeparator;
	private String[] columnHeaders;
	private BufferedReader reader;
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	
	public TextFileParser(File datafile) {
		this(datafile, TAB);
	}
	
	public TextFileParser(File datafile, String colSeparator){
		super(datafile);
		this.columnSeparator = colSeparator;
	}
	
	public TextFileParser(File datafile, int fileType){
		super(datafile);
		switch(fileType){
		case 0: this.columnSeparator = COMMA; break;
		case 1: this.columnSeparator = TAB; break;
		default: throw new RuntimeException("Unrecognized file type!");
		}
	}
	
	@Override
	public ArrayList<String[]> parse() {
		ArrayList<String[]> lines = new ArrayList<String[]>();
		try{
			reader = new BufferedReader(new FileReader(datafile));
			String line;
			String[] tokens;
			if(hasHeaders){
				columnHeaders = reader.readLine().split(columnSeparator);
				nrows++;
			}else
				columnHeaders = new String[0];
			
			while((line=reader.readLine()) != null && !line.equals("")){
				nrows++;
				if(ignoreQuotes)
					line = line.replaceAll(quotationMarksPattern, "");
				
				if(!line.matches(commentedRowPattern)){
					tokens = line.split(columnSeparator,-1);
				
					if(zerosAsMissingVals){
						for(int i=0; i < tokens.length; i++)
							tokens[i] = (tokens[i].equals("0")) ? "" : tokens[i];
					}
					
					lines.add(tokens);
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return lines;
	}
	
	public String[] getColumnHeaders(){
		return columnHeaders;
	}

}
