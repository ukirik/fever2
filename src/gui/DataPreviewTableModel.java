package gui;

import java.util.Arrays;
import java.util.logging.Logger;

import db.PathwayImpl;

public class DataPreviewTableModel extends UneditableTableModel {


	private static final long serialVersionUID = -4167265406524086603L;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());
	
	public DataPreviewTableModel(String[][] data, String[] headers) {
//		super(data,headers);
		super();
		
		Boolean[] bools = new Boolean[data.length];
		Arrays.fill(bools, false);
		addColumn("Ignore row?", bools);

		for (int cind = 0; cind < headers.length; cind++){
			String[] coldata = new String[data.length];

			for(int rind=0; rind < data.length ; rind++){
				if(data[rind].length != headers.length){
					throw new RuntimeException(
						"Assertion fails; row: " + rind 
						+ " does not have the same length as headers! "
						+ "Likely reason is that one of the columns include a "
						+ "delimeter e.g. a comma..."
						+ System.lineSeparator()
						+ "Row: " + Arrays.toString(data[rind]) 
						+ System.lineSeparator()
						+ "Headers: " + Arrays.toString(headers));
				}
			
				coldata[rind] = data[rind][cind];
			}
			addColumn(headers[cind], coldata);
		}
	
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex){
		if (columnIndex < 0)
			throw new IllegalArgumentException("Column index cannot be less than zero");
		else if(columnIndex == 0)
			return Boolean.class;
		else 
			return String.class;
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		if(column == 0)
			return true;
		else
			return false;
	}
}
