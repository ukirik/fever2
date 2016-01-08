package io;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import db.PathwayImpl;

public abstract class AbstractInputFileParser {

	public static final String TAB = "\t", COMMA = ",";
	public static final String commentedRowPattern = "^[#%/]";
	public static final String quotationMarksPattern = "[\"\']";
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	
	protected File datafile;
	protected boolean hasHeaders, ignoreQuotes, zerosAsMissingVals;
	protected int rejectedRows = 0;
	protected int nrows, ncols;
	protected List<String> lines;
	
	public AbstractInputFileParser(File datafile){
		this.datafile = datafile;
		this.hasHeaders = true;
		this.ignoreQuotes = true;
	}
	
	public AbstractInputFileParser(File datafile, boolean headerFlag, boolean ignoreFlag){
		this.datafile = datafile;
		this.hasHeaders = headerFlag;
		this.ignoreQuotes = ignoreFlag;
	}
	
	public void setHeaderFlag(boolean flag){
		this.hasHeaders = flag;
	}
	
	public void setIgnoreFlag(boolean flag){
		this.ignoreQuotes = flag;
	}
	
	public void setZeroFlag(boolean flag){
		this.zerosAsMissingVals = flag;
	}
	
	public abstract List<String[]> parse();
	
	public int getNbrOfRows(){
		return nrows;
	}
	
	public int getNbrOfCols(){
		return ncols;
	}
}
