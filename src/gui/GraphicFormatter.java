package gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.table.DefaultTableCellRenderer;

import db.PathwayImpl;
import main.AnalysisAction;

public class GraphicFormatter extends DefaultTableCellRenderer implements Icon {

	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	private static final long serialVersionUID = -1026092827512139569L;
	private static final double EPS = AnalysisAction.MIN_PSEA_PVAL;
	private static final int MAX_WIDTH = 120;
    private static final int HEIGHT = 6;
    
    DecimalFormat df;

    public GraphicFormatter(DecimalFormat df) {
        this.df = df;
        this.setIcon(this);
        //this.setHorizontalAlignment(JLabel.RIGHT);
        this.setForeground(this.getBackground());
    }

    @Override
    protected void setValue(Object value) {
        setText((value == null) ? "" : df.format(value));
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        
        double val = Double.valueOf(this.getText());
        if(val == 0D)
        	val += EPS;
        
        val = -Math.log10(val);
        double max = -Math.log10(AnalysisAction.MIN_PSEA_PVAL);
        double score = (val / max) * 9 - EPS;
        int width = (int)(val/max * MAX_WIDTH);
        int col_index = (int) score;
        
        g2d.setColor(ColorUtil.WRB_GRADIENT[col_index]);
        g2d.fillRect(x, y, width, HEIGHT);
        //int r = d / 2;
        //g2d.fillOval(x + HALF - r, y + HALF - r, d, d);
    }

    @Override
    public int getIconWidth() {
        return MAX_WIDTH;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }
}