package gui;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableCellRenderer;

import db.PathwayImpl;

public class FormattingRenderer extends DefaultTableCellRenderer{

	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	private static final long serialVersionUID = 4532831325971426911L;
	private final DecimalFormat f;
	
	public FormattingRenderer (DecimalFormat formatter){
		this.f = formatter;
	}
		
	@Override
	public void setValue(Object value){
		setText((value == null) ? "" : f.format(value));
	}
}
