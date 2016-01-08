package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import db.PathwayImpl;

public class DelimtedTextWriter extends PrintWriter {

	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());
	public static final int COMMA = 1, TAB = 2;
	
	private final String sep;
	private StringBuilder sb;
	
	public DelimtedTextWriter(String path, int delim) throws IOException {
		this(new File(path), delim);
	}
	
	public DelimtedTextWriter(File f, int delim) throws IOException{
		super(new BufferedWriter(new FileWriter(f)));
		switch(delim){
		case COMMA: sep = ","; break;
		case TAB: sep = "\t"; break;
		default: throw new IllegalArgumentException("Unrecognized delimiter!");
		}
	}

	public void writeRow(Object[] row){
		sb = new StringBuilder();
		String token;
		for(Object o : row){
			token = (o != null) ? o.toString() : ""; 
			sb.append(token);
			sb.append(sep);
		}
		sb.deleteCharAt(sb.length()-1);
		println(sb.toString());
	}
}
