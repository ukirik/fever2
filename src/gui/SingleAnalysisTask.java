package gui;

import io.ExportUtils;
import io.TextFileParser;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import com.google.common.base.Joiner;

import db.DbManager;
import db.PathwayImpl;
import db.ProteinImpl;
import main.Accession;
import main.AnalysisParams;
import main.AnalysisResult;
import main.ConcurrencyService;
import main.Dataset;
import main.DebugToolbox;
import main.Dataset.ANNOT_TYPE;
import main.Dataset.Data;

public class SingleAnalysisTask implements Runnable {

	final InternalAnalysisFrame frame;
	final AnalysisParams param = AnalysisParams.getInstance();
	boolean isExported = false;
	Dataset ds;
	DbManager dbMan;
	ConcurrencyService conserv;
	TextFileParser tfp;
	Map<PathwayImpl, AnalysisResult> results;
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	public SingleAnalysisTask(InternalAnalysisFrame f) {
		frame = f;
	}

	@Override
	public void run() {
		FileInputDialog dialog = new FileInputDialog(new JFrame(), true);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				File datafile = ((FileInputDialog) e.getWindow()).getFile();
				int filetype = ((FileInputDialog) e.getWindow()).getFileType();
				boolean headerFlag = ((FileInputDialog) e.getWindow()).getHeaderFlag();
				boolean ignoreFlag = ((FileInputDialog) e.getWindow()).getIgnoreFlag();
				boolean parseZeroFlag = ((FileInputDialog) e.getWindow()).getZeroTreatFlag();
				
				try {
					logger.info("Reading in datafile: "
							+ datafile.getCanonicalPath()
							+ System.lineSeparator());
					
					param.addParam(AnalysisParams.infile_key,datafile.getCanonicalPath());
					param.addParam(AnalysisParams.filetype_key, filetype);
					tfp = new TextFileParser(datafile, filetype);
					tfp.setHeaderFlag(headerFlag);
					tfp.setIgnoreFlag(ignoreFlag);
					tfp.setZeroFlag(parseZeroFlag);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

		List<String[]> data = tfp.parse();
		String[] headers = tfp.getColumnHeaders();
		logger.info("File read successfully, dataset size: " + data.size()
				+ System.lineSeparator());

		// Create data preview, analysis continues from SwingWorker created in
		// the action listener
		dataPreview(data.toArray(new String[0][0]), headers, frame.getTabbedPane());
	}
	
	public void exportAction(){
		
		File f, topfolder = new File("html");
		if(!topfolder.exists() || !topfolder.isDirectory())
			topfolder.mkdir();
		
		f = new File(topfolder, frame.getTitle());
		if(f.exists()){
			int response = JOptionPane.showConfirmDialog(frame, 
					"A previous analysis with the same name exists. "
					+ "Choose OK to overwrite, CANCEL otherwise...", 
					"Overwrite?", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.WARNING_MESSAGE);
			
			if(response == JOptionPane.CANCEL_OPTION){
				JFileChooser jfc = new JFileChooser(topfolder); 
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				response = jfc.showSaveDialog(null);
				if(response == JFileChooser.APPROVE_OPTION)
					f = jfc.getSelectedFile();
			}
		}
		
		if(!f.exists() || !f.isDirectory())
			f.mkdir();
		
		ExportUtils.exportResultsToHTML(f, ds, dbMan, results.values());
		logger.info("Analysis exported to HTML format for future investigation.");
		this.isExported = true;

	}

	private void dataPreview(final String[][] data, String[] headers, final JTabbedPane comp) {
		// Take care of column headers
		if (headers.length == 0) {
			headers = new String[data[1].length];
			for (int i = 0; i < headers.length; i++)
				headers[i] = "C" + i;
		}

		// Column annotations
		final Dataset.ANNOT_TYPE[] annots = new Dataset.ANNOT_TYPE[headers.length];
		final JComboBox<?>[] combos = new JComboBox[annots.length];

		// the upper part of the panel
		final PreviewPanel descPanel = new PreviewPanel(frame);
		final ParamPanel paramPanel = new ParamPanel();
		final JPanel upperContainer = new JPanel(new BorderLayout());
		paramPanel.setVisible(false);

		descPanel.setParamButtonAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean b = paramPanel.isVisible();
				paramPanel.setVisible(!b);
			}
		});

		upperContainer.add(descPanel, BorderLayout.NORTH);
		upperContainer.add(paramPanel, BorderLayout.SOUTH);

		// Define table model
		DataPreviewTableModel model = new DataPreviewTableModel(data, headers);
		final JTable table = new JTable(model);
		table.getColumnModel().getColumn(0).setPreferredWidth(25);
		table.setTableHeader(new JTableHeader(table.getColumnModel()){
			//Implement table header tool tips.
				private static final long serialVersionUID = -7015589028339208609L;
				public String getToolTipText(MouseEvent e) {
	                java.awt.Point p = e.getPoint();
	                int index = columnModel.getColumnIndexAtX(p.x);
	                return table.getColumnName(index);				
	            }
		    });
		
		for(int i=0; i<headers.length; i++)
			table.getColumnModel().getColumn(i).setMinWidth(60);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);


		// create the combo boxes for column annotation
		final JPanel comboPanel = new JPanel();
		comboPanel.setBorder(new EmptyBorder(3, 0, 3, 0));
		comboPanel.add(new JLabel("Columns:"));
		for (int i = 0; i < combos.length; i++) {
			final JComboBox<?> box = new JComboBox<Object>(Dataset.ANNOT_TYPE.values());
			final int colIndex = i;
			box.setMinimumSize(new Dimension(60, box.getMinimumSize().height));
			box.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					int colType = box.getSelectedIndex();
					table.getColumnModel().getColumn(colIndex+1)
							.setCellRenderer(new CellColorRenderer(colType));
					table.repaint();
				}
			});

			comboPanel.add(box);
			combos[i] = box;
		}
		
		final JPanel middlePanel = new JPanel(new BorderLayout());
		middlePanel.add(comboPanel, BorderLayout.NORTH);
		middlePanel.add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel lowerPanel = new JPanel(new BorderLayout());
		final JButton analyzeButton = new JButton("Analyze Dataset!");
		lowerPanel.add(analyzeButton, BorderLayout.LINE_END);
		final JPanel container = new JPanel(new BorderLayout());
		container.add(upperContainer, BorderLayout.NORTH);
		container.add(new JScrollPane(middlePanel), BorderLayout.CENTER);
		container.add(lowerPanel, BorderLayout.SOUTH);

		comp.addTab("Preview", container);

		// ANALYSIS TRIGGER
		analyzeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				
				for (int i = 0; i < annots.length; i++) {
					annots[i] = (ANNOT_TYPE) combos[i].getSelectedItem();
				}
				
				// Validate the column annotations
				List<ANNOT_TYPE> annotlist = Arrays.asList(annots);
				if (!annotlist.contains(ANNOT_TYPE.Ratio) &&
					!annotlist.contains(ANNOT_TYPE.Fold_Ch) &&	
					!(annotlist.contains(ANNOT_TYPE.Intensity_S1) && 
							annotlist.contains(ANNOT_TYPE.Intensity_S2))) {
					JOptionPane
							.showMessageDialog(
									null,
									"<html>Quatification column(s) not selected! <p>"
											+ "<p> Please make sure you have selected a <code>ratio</code> column "
											+ " or <p> several columns indicating observations for "
											+ "<code>sample1</code> and <code>sample2</code>...",
									"Missing annotation!",
									JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Validate DB selection
				if (descPanel.getDBManager() == null) {
					JOptionPane
							.showMessageDialog(
									null,
									"<html>No database not selected for analysis! <p>"
											+ "<p> Please select and load a database..",
									"Database error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				dbMan = descPanel.getDBManager();
				analyzeButton.setEnabled(false);
				DebugToolbox.startExecTimer();
				
				// Activate progress indicator
				frame.getMainFrame().activateInfiGlass();

				SwingWorker<Map<PathwayImpl,AnalysisResult>, Void> worker = new SwingWorker<Map<PathwayImpl,AnalysisResult>, Void>() {
					@Override
					protected Map<PathwayImpl,AnalysisResult> doInBackground() {
						try {
							// register parameters
							param.addParam(AnalysisParams.value_key,descPanel.getValueTypeComboIndex());
							param.addParam(AnalysisParams.sepchar_key,descPanel.getSepCharComboIndex());
							paramPanel.registerParams();

							StringBuilder sb = new StringBuilder("Data preview completed, initiating analysis...");
							sb.append(System.lineSeparator())
								.append("... column annotations: ")
								.append(Arrays.toString(annots));
							logger.info(sb.toString() + System.lineSeparator());

							// Create dataset; to be passed on to SwingWorker which will
							// execute the analysis
							ds = new Dataset();
							
							String[] line;
							for (int i=0; i < data.length; i++){
								line = data[i];
								// If ignore button is clicked, skip row..
								if(!(Boolean) table.getValueAt(i, 0))
									ds.addRow(line, annots);
							}
							
							System.out.println("Dataset parsed...");
							logger.info("Dataset parsing complete "
									+ System.lineSeparator() 
									+ ds.toString()
									+ System.lineSeparator());
							
							visualizeDataset();

							conserv = new ConcurrencyService(ds, dbMan);
							conserv.serve();
							//DebugToolbox.dumpScores();		

						} catch (InterruptedException e) {
							logger.severe("Concurrency service interrupted"
									+ System.lineSeparator()
									+ DebugToolbox.getStackTraceAsString(e)
									+ System.lineSeparator());
							System.err.println("Interrupt exception!!");
						}
						return conserv.getAnalyzedPaths();
					}

					@Override
					protected void done() {
						try{
							results = get();
							visualizeResults();	
						}
						catch (InterruptedException ignore) {}
				        catch (java.util.concurrent.ExecutionException e) {
				            String why = null;
				            Throwable cause = e.getCause();
				            if (cause != null) {
				                why = cause.getMessage();
				            } else {
				                why = e.getMessage();
				            }
				            System.err.println("Error analysing data: " + why);
				            e.printStackTrace();
				        } catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						logger.info("#DEBUG: Conserv should have been terminated by now..." + System.lineSeparator());
						frame.getMainFrame().deactivateInfiGlass();
						DebugToolbox.stopExecTimer();
						
					}
				};

				worker.execute();

			}
		});
	}
	
	private void visualizeDataset(){
		/* VISUALIZE DATASET
		 * 
		 * 1. new tab to list the proteins in the dataset
		 * 2. show if in ROI, fold change and p-values
		 * 3. sortable
		 */
		
		ArrayList<Object[]> datarows = new ArrayList<Object[]>();
		ArrayList<Object> row;
		Joiner j = Joiner.on(";");
		Set<Data> roi = ds.getROI();
		for(Data d : ds.getDataRows()){
			row = new ArrayList<Object>();
			row.add(j.join(d.getProteins()));
			row.add(d.getRatio());
			row.add(d.getPval());
			row.add(Boolean.toString(roi.contains(d)));
			datarows.add(row.toArray());
		}

		final UneditableTableModel datadata = new UneditableTableModel(
				datarows.toArray(new Object[0][0]), 
				new Object[]{"Accession(s)", "Ratio", "p-value", "In ROI?"})
		{

			private static final long serialVersionUID = -5454412467460359803L;

			@Override
			public Class<?> getColumnClass(int i){
				if(i == 0)
					return String.class;
				else if (i < 3)
					return Double.class;
				else if (i == 3)
					return String.class;
				else
					throw new IllegalArgumentException("Unfeasable index");
			}
		};
		
		JTable table = new JTable(datadata){

			private static final long serialVersionUID = -9143184489530193900L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);

				c.setBackground(getBackground());
				int modelRow = convertRowIndexToModel(row);
				
				// TODO: hard-coding the column indices is pretty bad practice,
				// try to fix this in a better way
				Object val3 = getModel().getValueAt(modelRow, 3);
				Object val1 = getModel().getValueAt(modelRow, 1);
				Double fc = val1 != null ? (Double) val1 : Double.NaN;
				fc = fc > 1 ? fc : -1/fc;
				
				if(!Boolean.getBoolean((String) val3)){
					c.setForeground(null);
					c.setFont(null);
				}
				else if (fc > 0D){
					c.setForeground(ColorUtil.FIREBRICK); 
					c.setFont(getFont().deriveFont(Font.BOLD));
				}
				else {
					c.setFont(getFont().deriveFont(Font.BOLD));
					c.setForeground(ColorUtil.AZURE);
				}
				
				return c;
			}
		};
		table.setAutoCreateRowSorter(true);
		RowSorter<? extends TableModel> trs = table.getRowSorter();
		LowerResultsPanel lrp = new LowerResultsPanel(trs);
		lrp.setExportActionListener(getExportActionListener(datadata,"dataset"));
		
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent me) {
		        JTable table =(JTable) me.getSource();
		        Point p = me.getPoint();
		        int row_index = table.rowAtPoint(p);
		        if (me.getClickCount() == 2) {
		           int ncol = table.getColumnCount();
		           Object[] row = new Object[ncol];
		           for(int i=0; i<ncol; i++){
		        	   row[i] = table.getValueAt(row_index, i);
		           }	           
		           visualizeDataRow(row);
		        }
		    }
		});
		
		JPanel container = new JPanel(new BorderLayout());
		JLabel desc = new JLabel(
				"<html><pProteins in the dataset are listed below:</p></html>");
		
		desc.setBorder(new EmptyBorder(5, 5, 5, 5));
		container.add(desc, BorderLayout.NORTH);
		container.add(new JScrollPane(table), BorderLayout.CENTER);
		container.add(lrp, BorderLayout.SOUTH);
		
		JTabbedPane tabs = frame.getTabbedPane();
		tabs.addTab("Dataset", container);
//		tabs.setTabComponentAt(tabs.getTabCount() - 1, new ButtonTabComponent(tabs));	
		
	}
	
	private void visualizeDataRow(Object[] row){
		String acc = (String) row[0];
		double ratio = (double) row[1], pval = (double) row[2];
		
		try {
			Set<PathwayImpl> paths = dbMan.getAllPathsWithProtein(acc);
			ProteinImpl prot = dbMan.getProteinByAccession(acc);
			
			double par, npar, meta;
			ArrayList<Object[]> pathdata = new ArrayList<Object[]>(); 
			for(PathwayImpl path : paths){
				if(!results.containsKey(path))
					System.out.println(acc + " - " + path.getName());
				else
				{ 	
				AnalysisResult res = results.get(path);
				meta = res.getMeta_score();
				par = res.getPar_score();
				npar = res.getPsea_score();
				pathdata.add(new Object[]{
					path.getName(), path.getDb(), 
					res.getROInCoverage(), par, npar, meta
					});
				}
			}
			
			final DefaultTableModel model = new DefaultTableModel(
					pathdata.toArray(new Object[0][0]), 
					new Object[]{"Path name", "DB", "Coverage", 
						"PARAM score", "N-PAR score", "META score"})
			{
	
				private static final long serialVersionUID = -4316702634850060149L;

				@Override
				public Class<?> getColumnClass(int i){
					if(i >= 0 && i <= 2)
						return String.class;
					else if (i < 6)
						return Double.class;
					else
						throw new IllegalArgumentException("Unfeasable index");
				}
			};
			
			JTable table = new JTable(model);
			table.setAutoCreateRowSorter(true);
			RowSorter<? extends TableModel> trs = table.getRowSorter();
			LowerResultsPanel lrp = new LowerResultsPanel(trs);
			lrp.setExportActionListener(getExportActionListener(model,acc));
			DecimalFormat df = new DecimalFormat("0.00E0");
			table.getColumn("PARAM score").setCellRenderer(new FormattingRenderer(df));
			table.getColumn("N-PAR score").setCellRenderer(new FormattingRenderer(df));
			table.getColumn("META score")
						.setCellRenderer(
								new FormattingRenderer(
										new DecimalFormat("#0.000")));
			
			JPanel container = new JPanel(new BorderLayout());
			JLabel desc = new JLabel(
					"<html><p><i>" + prot.getUniprot_id() + "</i></p>"
					+ "<p>ratio: " + ratio + ", pval: " + pval + "</p>"
					+ "<p>Below are all the pathways associated with this protein.");
			
			desc.setBorder(new EmptyBorder(5, 5, 5, 5));
			container.add(desc, BorderLayout.NORTH);
			container.add(new JScrollPane(table), BorderLayout.CENTER);
			container.add(lrp, BorderLayout.SOUTH);
			
			JTabbedPane tabs = frame.getTabbedPane();
			tabs.addTab(acc, container);
			tabs.setTabComponentAt(tabs.getTabCount() - 1, new ButtonTabComponent(tabs));
			
		} catch(SQLException e){
			logger.severe("Unexpected database error: " 
					+ System.lineSeparator() 
					+ DebugToolbox.getStackTraceAsString(e)
					+ System.lineSeparator());
			
			System.err.println("Database error occured, please check the logs for more information!");
		}	
	}
	
	private void visualizeResults() throws SQLException{
		/* PROCESS RESULTS
		 * 1. new tab to list the pathways (non-GO)
		 * 2. new tab to list GO annotations
		 */
		
		ArrayList<Object[]> path_data = new ArrayList<Object[]>();
		ArrayList<Object[]> go_data = new ArrayList<Object[]>(); 
		ArrayList<Object> row;
		for(Entry<PathwayImpl, AnalysisResult> e : results.entrySet()){
			row = new ArrayList<Object>();
			AnalysisResult res = e.getValue();
			PathwayImpl path =  e.getKey();
			
			// Filtering out GO terms
			if(path.getDb().equalsIgnoreCase("GO")){
				char onto = path.getName().charAt(0);
				String goTerm = path.getName().substring(2); 
				row.add(goTerm);
				switch(onto){
					case 'P': row.add("Biological process"); break;
					case 'C': row.add("Cellular component"); break;
					case 'F': row.add("Molecular function"); break;
					default: throw new RuntimeException("Unrecognized GO term");
				}
				
				row.add(res.getCoverage());
				row.add(Double.valueOf(res.getPsea_score()));
				go_data.add(row.toArray());
			}
			else{
				row.add(path.getName());
				row.add(path.getDb());
				row.add(res.getROInCoverage());
				row.add(Double.valueOf(res.getPar_score()));
				row.add(Double.valueOf(res.getPsea_score()));
				row.add(Double.valueOf(res.getMeta_score()));
				path_data.add(row.toArray());	
			}
		}
		
		/* ============================================
		 * Creates the pathway results tab
		 * ============================================
		 */
		final DefaultTableModel path_model = 
				new UneditableTableModel(
					path_data.toArray(new Object[0][0]), 
					new Object[]{"Pathway name", "DB", "Coverage", "PARAM score", "N-PAR score", "META score"})
					{
						private static final long serialVersionUID = -3707659138320929751L;
						@Override
						public Class<?> getColumnClass(int i){
							if(i >= 0 && i <= 2)
								return String.class;
							else if (i > 2 && i <= 5)
								return Double.class;
							else
								throw new IllegalArgumentException("Unfeasable index");
						}
			
					};
		
		final JTable path_table = new JTable(path_model);
		path_table.setAutoCreateRowSorter(true);
		
		//path_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		DecimalFormat df = new DecimalFormat("0.00E0");
		path_table.getColumn("PARAM score").setCellRenderer(new FormattingRenderer(df));
		path_table.getColumn("N-PAR score").setCellRenderer(new FormattingRenderer(df));
		path_table.getColumn("META score")
					.setCellRenderer(
							new FormattingRenderer(
									new DecimalFormat("#0.000")));

		
		JPanel path_container = new JPanel(new BorderLayout());
		JLabel path_desc = new JLabel("Below are the results of analysis");
		path_desc.setBorder(new EmptyBorder(5, 5, 5, 5));
		path_container.add(path_desc, BorderLayout.NORTH);
		path_container.add(new JScrollPane(path_table), BorderLayout.CENTER);
		
		RowSorter<? extends TableModel> trs = path_table.getRowSorter();
		LowerResultsPanel lp1 = new LowerResultsPanel(trs);
		lp1.setExportActionListener(getExportActionListener(path_model,"allPaths"));
		path_container.add(lp1, BorderLayout.SOUTH);
		frame.getTabbedPane().addTab("Pathways", path_container);
		
		// Default sorting
		int meta_index = path_table.getColumn("META score").getModelIndex();
				
		path_table.getRowSorter().toggleSortOrder(meta_index);
		path_table.getRowSorter().toggleSortOrder(meta_index);
		
		path_table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent me) {
		        JTable table =(JTable) me.getSource();
		        Point p = me.getPoint();
		        int row_index = table.rowAtPoint(p);
		        if (me.getClickCount() == 2) {
		           int ncol = path_table.getColumnCount();
		           Object[] row = new Object[ncol];
		           for(int i=0; i<ncol; i++){
		        	   row[i] = table.getValueAt(row_index, i);
		           }
		           
		           visualizePathwayContents(row);
		        }
		    }
		});
				
		/* ============================================
		 * Creates the GO-enrichment tab
		 * ============================================
		 */
		final DefaultTableModel go_model = 
				new UneditableTableModel(
						go_data.toArray(new Object[0][0]), 
						new Object[]{"GO term", "Ontology", "Coverage", "Enrichment score"})
		{

			private static final long serialVersionUID = 1837783229618227509L;

			@Override
			public Class<?> getColumnClass(int i){
				if(i >= 0 && i <= 2)
					return String.class;
				else if (i == 3)
					return Double.class;
				else
					throw new IllegalArgumentException("Unfeasable index");
			}
		};
		
		final JTable go_table = new JTable(go_model);
		go_table.setAutoCreateRowSorter(true);
		//go_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);	
		go_table.getColumn("Enrichment score").setCellRenderer(new GraphicFormatter(df));
		
		JPanel go_container = new JPanel(new BorderLayout());
		JLabel go_desc = new JLabel("Below are GO terms relevant for the dataset");
		go_desc.setBorder(new EmptyBorder(5, 5, 5, 5));
		go_container.add(go_desc, BorderLayout.NORTH);
		go_container.add(new JScrollPane(go_table), BorderLayout.CENTER);
		RowSorter<? extends TableModel> trs2 = go_table.getRowSorter();
		LowerResultsPanel lp2 = new LowerResultsPanel(trs2);
		go_container.add(lp2, BorderLayout.SOUTH);
		lp2.setExportActionListener(getExportActionListener(go_model, "GO"));
		frame.getTabbedPane().addTab("GO Enrichment", go_container);

	}
	
	private void visualizePathwayContents(Object[] rowdata){
		
		// TABNAME is truncated pathName (to 15 chars)
		String pathName = (String) rowdata[0], db = (String) rowdata[1];
		String tabName = pathName.length() > 15 ? pathName.substring(0, 12) + "..." : pathName;
		
		double par = (double) rowdata[3], 
				npar = (double) rowdata[4], 
				meta = (double) rowdata[5];
		try{
			PathwayImpl path = dbMan.getPathwayByName(pathName, db);
			Set<ProteinImpl> allProts = dbMan.getAllProtsInPath(path.getId());
			Set<Integer> pset = new HashSet<Integer>();
			
			ArrayList<Object[]> table_data = new ArrayList<Object[]>();
			String acc,gene;
			Set<String> missedProts = new HashSet<String>(),
						foundProts = new HashSet<String>(); 
			
			Set<String> templist = new HashSet<String>();
			for(ProteinImpl prot : allProts){
				acc = prot.getAcc();
				gene = prot.getGene_symbol();
				if (!ds.getProteinsIds().contains(acc)){
					table_data.add(new Object[]{acc, gene, null, null, ""});
					missedProts.add(acc);
				}
				else{
					boolean hit = false;
					for(Data d : ds.getDataRows()){
						if(d.getProteins().contains(acc)){
							if(hit)
								logger.warning("Multiple rows containing accession: " + acc);
							
							pset.add(d.getUid());
							foundProts.add(acc);
							hit = true;
						}
					}
				}	
			}	
			
			StringBuilder sb_acc, sb_gene;
			for(Data row : ds.getRows(pset)){
				sb_acc = new StringBuilder();
				sb_gene = new StringBuilder();
				String temp;
				for(String accession : row.getProteins()){
					temp = Accession.isIsoform(accession) ? 
						accession.substring(0, accession.indexOf("-")) : accession;
					
					ProteinImpl p = dbMan.getProteinByAccession(temp);
					if(p != null)
						sb_gene.append(p.getGene_symbol()).append(";");
					sb_acc.append(accession).append(";");
				}
				
				sb_acc.deleteCharAt(sb_acc.length() -1 );
				sb_gene.deleteCharAt(sb_gene.length() -1 );
				double ratio = row.getRatio(), pval = row.getPval();
				String inROI = param.isInROI(ratio, pval) ? "true" : "false";
				table_data.add(new Object[]{sb_acc.toString(), sb_gene.toString(), ratio, pval, inROI});
			}
			
			if(foundProts.size() + missedProts.size() != allProts.size()){
				System.err.println("Assertion fails for path: "
						+ pathName + System.lineSeparator()
						+ "discovered = " + foundProts.size()
						+ " missed = " + missedProts.size()
						+ " total = " + allProts.size());
				
				templist.addAll(foundProts);
				templist.addAll(missedProts);
				List<String> sb2 = new LinkedList<String>();
				for(ProteinImpl prot : allProts){
					sb2.add(prot.getAcc());
				}
				
				logger.warning("All: " + sb2.toString() + System.lineSeparator() 
						+ "mis/disc: " + Collections.unmodifiableCollection(templist).toString());
				
				sb2.removeAll(templist);
				logger.warning("Diff: " + sb2.toString());
			}
			
			Collections.reverse(table_data);
			
			// TODO: add additional data from ProteinInfo tabel on dbMan
			final DefaultTableModel pathdata = new DefaultTableModel(
					table_data.toArray(new Object[0][0]), 
					new Object[]{"Accession(s)", "Gene Symbol", "Ratio", "p-value", "inROI"})
			{
	
				private static final long serialVersionUID = -4316702634850060149L;

				@Override
				public Class<?> getColumnClass(int i){
					if(i == 0 || i == 1)
						return String.class;
					else if (i < 4)
						return Double.class;
					else if(i == 4)
						return String.class;
					else
						throw new IllegalArgumentException("Unfeasable index");
				}
			};
			
			JTable table = new JTable(pathdata){

				private static final long serialVersionUID = 4748090126102580385L;

				@Override
				public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
				{
					Component c = super.prepareRenderer(renderer, row, column);

					//  Color row based on a cell value
					if (!isRowSelected(row))
					{
						c.setBackground(getBackground());
						int modelRow = convertRowIndexToModel(row);
						
						// TODO: hard-coding the column indices is pretty bad practise,
						// try to fix this in a better
						Object val1 = getModel().getValueAt(modelRow, 2);
						Object val2 = getModel().getValueAt(modelRow, 3);
						Double ratio = val1 != null ? (Double) val1 : Double.NaN;
						Double pval = val2 != null ? (Double) val2 : Double.NaN;
						
						if(Double.isNaN(ratio) || Double.isNaN(pval)){
							c.setForeground(null);
							c.setFont(null);
						}
						else if (param.isInROI(ratio, pval) && ratio > 1){
							c.setForeground(ColorUtil.FIREBRICK); 
							c.setFont(getFont().deriveFont(Font.BOLD));
						}
						else if(param.isInROI(ratio, pval) && ratio < 1){
							c.setFont(getFont().deriveFont(Font.BOLD));
							c.setForeground(ColorUtil.AZURE);
						}
						else {
							c.setForeground(null);
							c.setFont(null);
						}
					}

					return c;
				}
			};
			table.setAutoCreateRowSorter(true);
			RowSorter<? extends TableModel> trs = table.getRowSorter();
			LowerResultsPanel lrp = new LowerResultsPanel(trs);
			lrp.setExportActionListener(getExportActionListener(pathdata,tabName));
			
			JPanel container = new JPanel(new BorderLayout());
			JLabel desc = new JLabel(
					"<html><p><i>" + pathName + "</i><br>"
					+ "<b>META:</b> " + meta + " <b>PAR:</b> " + par + " <b>NPAR:</b> " + npar + "<br></p>"
					+ "<p>Proteins defined to be participate in this pathway are listed below. "
					+ "Missing values indicate that the particular protein was not found in the dataset. "
					+ "Up/Down-regulated proteins that fall within the selected ROI criteria "
					+ "are marked with red/blue colors respectively.</p>"
					+ "<p>Note that the ratios are given RAW, "
					+ "<b>even if </b> the input data were originally log-transformed.</p></html>");
			
			desc.setBorder(new EmptyBorder(5, 5, 5, 5));
			container.add(desc, BorderLayout.NORTH);
			container.add(new JScrollPane(table), BorderLayout.CENTER);
			container.add(lrp, BorderLayout.SOUTH);
			
			JTabbedPane tabs = frame.getTabbedPane();
			tabs.addTab(tabName, container);
			tabs.setTabComponentAt(tabs.getTabCount() - 1, new ButtonTabComponent(tabs));
		}
		catch(SQLException e){
			logger.severe("Unexpected database error: " 
					+ System.lineSeparator() 
					+ DebugToolbox.getStackTraceAsString(e)
					+ System.lineSeparator());
			
			System.err.println("Database error occured, please check the logs for more information!");
		}
	}
	
	private ActionListener getExportActionListener(final DefaultTableModel model, final String exportName){
		return new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				File file;
				JFileChooser jfc = new JFileChooser();
				jfc.setFileFilter(new FileNameExtensionFilter("TEXT FILES", "txt", "tsv", "csv"));
				jfc.setSelectedFile(new File(exportName + ".txt"));
				int returnVal = jfc.showSaveDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            file = jfc.getSelectedFile();
		            if(file.getName().endsWith("csv"))
		            	ExportUtils.exportTableCSV(file, model);
		            else
		            	ExportUtils.exportTableTSV(file, model);
		            
		        } else {
		        	logger.info("Export command cancelled by user." + System.lineSeparator());
		        }
			}
		};
	}
}
