package gui;

import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import db.PathwayImpl;

class LowerResultsPanel extends JPanel{

	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	private static final long serialVersionUID = 6889152576965823991L;
	private JTextField filterText;
	private JButton exportButton;
	private TableRowSorter<DefaultTableModel> sorter;
	
	public LowerResultsPanel(RowSorter<? extends TableModel> trs){
		super();
		this.sorter = (TableRowSorter<DefaultTableModel>) trs;
		this.setLayout(new SpringLayout());
		initComponents();
	}
	
	public void setExportActionListener(ActionListener al){
		exportButton.addActionListener(al);
	}
	
	private void initComponents(){
		JLabel filter_label = new JLabel("Filter:", SwingConstants.TRAILING);
        filterText = new JTextField();
        
        //Whenever filterText changes, invoke newFilter.
        filterText.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        newFilter();
                    }
                    public void insertUpdate(DocumentEvent e) {
                        newFilter();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        newFilter();
                    }
                });
        
        
        exportButton = new JButton("Export..."); 
		
        filter_label.setLabelFor(filterText);
        add(filter_label);
        add(filterText);
		add(exportButton);

        SpringUtilities.makeCompactGrid(this, 1, 3, 5, 5, 5, 5);      
    
	}
	
	private void newFilter() {
        RowFilter<DefaultTableModel, Object> rf = null;
        //If current expression doesn't parse, don't update.
        try {
            rf = RowFilter.regexFilter("(?i)" + filterText.getText(), 0);
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(rf);
    }
}
