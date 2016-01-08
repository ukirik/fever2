package gui;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import db.PathwayImpl;

public class CellColorRenderer extends DefaultTableCellRenderer{

	/**
	 * Generated serial ID
	 */
	private static final long serialVersionUID = -5447755640349375736L;
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	
	static Color protIDColor = new Color(0.25f, 0.75f, 0.75f, 0.25f);
	static Color pepColor = new Color(0.75f, 0.25f, 0.75f, 0.25f);
	static Color sample1Color = new Color(0.40f, 0.10f, 0.65f, 0.25f);
	static Color sample2Color = new Color(0.05f, 0.05f, 0.95f, 0.25f);
	static Color ratioColor = new Color(0.25f, 0.0f, 0.75f, 0.25f);
	static Color fcColor = new Color(0.25f, 0.0f, 0.75f, 0.25f);
	static Color pvalColor = new Color(0.75f, 0.25f, 0.25f, 0.25f);
	static Color ignoreColor = new Color(0.66f, 0.66f, 0.66f, 0.25f);
	static Color[] columnColors = { 
			ignoreColor, protIDColor, pepColor, sample1Color, sample2Color, ratioColor, fcColor, pvalColor
	};
	
	Color bgColor;
	
	public CellColorRenderer(int index) {
		if(index == -1)
			bgColor = ignoreColor;
		else if (index > -1 && index < columnColors.length)
			bgColor = columnColors[index];
		else
			throw new IllegalArgumentException();
	}
	
	@Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        c.setBackground(bgColor);
        return c;
    }
	
}
