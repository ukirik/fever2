package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;

import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.io.Resources;

import main.AnalysisParams;
import main.AnalysisResult;
import main.Dataset;
import main.Dataset.Data;
import main.DebugToolbox;
import main.ResourceUtils;
import db.DbManager;
import db.PathwayImpl;
import db.ProteinImpl;

public class ExportUtils {
	
	public static final Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	// --- PRIVATE FIELDS ---
	private static final double SCORE_SIG = 0.001;
	
	private static final DecimalFormat 
		pathformat = (DecimalFormat) DecimalFormat.getInstance(Locale.US),
		valformat = (DecimalFormat) DecimalFormat.getInstance(Locale.US),
		metaformat = (DecimalFormat) DecimalFormat.getInstance(Locale.US),
		timeformat = (DecimalFormat) DecimalFormat.getInstance(Locale.US),
		percformat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
	
	
	// --- PUBLIC METHODS ---
	
	public static void exportResultsToHTML(
			File top_folder, Dataset ds, 
			DbManager dbMan, Collection<AnalysisResult> res)
	{
		pathformat.applyPattern("0.000E0");
		valformat.applyPattern("0.00000");
		metaformat.applyPattern("#0.000");
		timeformat.applyPattern("#0.00");
		percformat.applyPattern("0.00%");
		
		try {
			HTMLReportExporter htmlExport = new HTMLReportExporter(top_folder, ds, dbMan, res);
//			htmlExport.loadRes();
			htmlExport.loadRes2();
			htmlExport.createIndexPage();
			htmlExport.createDatasetPage();
			htmlExport.createPathsPage();
			htmlExport.createGOTermsPage();
		} catch (IOException e) {
			System.err.println("Exporting to HTML failed due to I/O errors. Please see the logs");
			logger.severe(DebugToolbox.getStackTraceAsString(e));
		} catch (SQLException e) {
			System.err.println("Exporting to HTML failed due to DB errors. Please see the logs");
			logger.severe(DebugToolbox.getStackTraceAsString(e));		}
		
	}
	
	public static void exportTableTSV(File file, DefaultTableModel model){
		try {
			logger.info("Exporting data to TSV file: " + file.getName() + System.lineSeparator());
			DelimtedTextWriter w = new DelimtedTextWriter(file, DelimtedTextWriter.TAB);
			writeOutTableModel(w, model);
			
		} catch (IOException e) {
			System.err.println("Exporting table failed due to an I/O error, "
					+ "please check the logs for more information");
			
			logger.warning("Exporting failed due to an IOException: " 
					+ System.lineSeparator() 
					+ DebugToolbox.getStackTraceAsString(e)
					+ System.lineSeparator());
		}
	
	}
	
	public static void exportTableCSV(File file, DefaultTableModel model){
		try {
			logger.info("Exporting data to CSV file: " + file.getName() + System.lineSeparator());
			DelimtedTextWriter w = new DelimtedTextWriter(file, DelimtedTextWriter.COMMA);
			writeOutTableModel(w, model);
			
		} catch (IOException e) {
			System.err.println("Exporting table failed due to an I/O error, "
					+ "please check the logs for more information");
			
			logger.warning("Exporting failed due to an IOException: " 
					+ System.lineSeparator() 
					+ DebugToolbox.getStackTraceAsString(e)
					+ System.lineSeparator());
		}
	}
	
	// --- PRIVATE METHODS ---
	private static void writeOutTableModel(DelimtedTextWriter w, DefaultTableModel model){
		Vector<Vector> rows = model.getDataVector();
		String[] headers = new String[model.getColumnCount()];
		for(int i=0; i<headers.length; i++)
			headers[i] = model.getColumnName(i);
		
		w.writeRow(headers);
		for(Vector v : rows)
			w.writeRow(v.toArray());
		
		w.flush();
		w.close();	
	}	

	
	static class HTMLReportExporter{
		
		File topfolder;
		Dataset ds;
		DbManager dbMan;
		Collection<AnalysisResult> res;
		
		private static final String UNILINK = "http://www.uniprot.org/uniprot/";
		private static final String NOT_FOUND = "PHYSICAL_ENTITY_ID_NOT_FOUND";
		private static final String NO_ASSOC = "NO_PATHWAY_DATA";
		private static final String NBS = Character.toString((char)160);
		private static final String R_ARROW = Character.toString((char)8594);
		private static final String ALPHA = Character.toString((char)945);
		private static final String COPYRIGHT = Character.toString((char)169);


		private static final String 
			commentStr = System.lineSeparator()
					+ "Auto-generated report page for FEvER Pathway Analysis "  
					+ "created at: " + DebugToolbox.getDateTimeStamp() 
					+ System.lineSeparator() 
					+" Copyright 2010-2014 Ufuk Kirik, Dept. of Immunotechnology at Lund University"
					+ System.lineSeparator();
		
		
		public HTMLReportExporter(File top_folder, Dataset ds, DbManager dbm, Collection<AnalysisResult> res){
			this.topfolder = top_folder;
			this.ds = ds;
			this.dbMan = dbm;
			this.res = res;
		}
		
		public void createGOTermsPage() throws SQLException, IOException 
		{
			File goPage = new File(topfolder, "go.html");
			File individualPages = new File(topfolder, "pathways");
			Document doc = initHTMLDoc();
			
			// Document BODY
			Element container = doc.body().appendElement("div").attr("id", "container");
			Element header = container.appendElement("div").attr("id", "header");
			header.appendElement("div").attr("id", "logo");
			
			addInfoTable(header, dbMan.getDBName(), res.size());
			addNavBar(container);
			
			container.appendElement("div").attr("id", "rightcol")
					.appendElement("iframe")
					.attr("name", "pathwayDetails")
					.attr("srcdoc","<p>Click on a GO term, to see the proteins that are annotated with that term</p>");
			
			Element content = container.appendElement("div").attr("id", "leftcol");
			content.appendElement("h2").text("GO Enrichment results");

			Element summary = content.appendElement("div").attr("id", "preamble");

			summary.appendElement("p")
					.text("Below is an interactive listing of the resultant GO terms from this analysis. "
							+ "Details on any particular term can be shown on the right with a click on the term description. "
							+ "Similarly you can re-sort the table by clicking the table header of the column you want to sort.");
			
			summary.appendElement("h5").text("Table legend:");
			Element legend = summary.appendElement("ul");
			legend.appendElement("li").append("Domain: denotes which one of the three GO domains this term belongs to: <br>"
												+ "<b>[BP]</b> Biological Process, "
												+ "<b>[CC]</b> Cellular Component, "
												+ "<b>[MF]</b> Molecular Function.");
			
			legend.appendElement("li").text("N-PAR Score: significance of non-parametric enrichment model [1E-10" + R_ARROW + "1]");
			
			summary.appendElement("button").attr("id", "goall").attr("type","button").attr("class","filter").attr("onclick","tableFilter(\"all\",GOTable);").text("ALL");
			summary.appendElement("button").attr("id", "gobp").attr("type","button").attr("class","filter").attr("onclick","tableFilter(\"bp\",GOTable);").text("BP");
			summary.appendElement("button").attr("id", "gocc").attr("type","button").attr("class","filter").attr("onclick","tableFilter(\"cc\",GOTable);").text("CC");
			summary.appendElement("button").attr("id", "gomf").attr("type","button").attr("class","filter").attr("onclick","tableFilter(\"mf\",GOTable);").text("MF");
			
			Element table = content.appendElement("table")
									.attr("id", "GOTable")
									.attr("class", "tablesorter");
			
			Element row;
			row = table.appendElement("thead").appendElement("tr");
			row.appendElement("th").attr("class", "header").text("GO Term");
			row.appendElement("th").attr("class", "header").text("Domain");
			row.appendElement("th").attr("class", "headerSortDown").text("N-PAR score");
			Element tbody = table.appendElement("tbody");

			int counter = 0;
			for(AnalysisResult r : res){
				counter++;
				if(!r.isGO())
					continue;
				
				String link = Integer.toString(counter) + ".html";			
				String domain, goTerm = r.getPath().getName().substring(2); 
				switch(r.getPath().getName().charAt(0)){
					case 'P': domain  = "BP"; break;
					case 'C': domain  = "CC"; break;
					case 'F': domain  = "MF"; break;
					default: throw new RuntimeException("Unrecognized GO term");
				}
				
				row = tbody.appendElement("tr").attr("class", domain.toLowerCase());
				row.appendElement("td")
					.appendElement("a")
					.attr("href", individualPages.getName() + File.separator + link)
					.attr("target", "pathwayDetails")
					.text(goTerm);
				
				row.appendElement("td").text(domain);
				row.appendElement("td").text(pathformat.format(r.getPsea_score()));
				
			}
			
			doc.body().append("<script>$(document).ready(function(){$(\"#GOTable\").tablesorter( {sortList: [[2,0]]} );});</script>");

			addFooter(container);
			writeOutToFile(doc, goPage);
			
		}
			
		public void createPathsPage() throws SQLException, IOException 
		{
			File pathPage = new File(topfolder, "paths.html");
			File individualPages = new File(topfolder, "pathways");
			individualPages.mkdir();
			
			Document doc = initHTMLDoc();
			
			// Document BODY
			Element container = doc.body().appendElement("div").attr("id", "container");
			Element header = container.appendElement("div").attr("id", "header");
			header.appendElement("div").attr("id", "logo");
			
			addInfoTable(header, dbMan.getDBName(), res.size());
			addNavBar(container);
			
			container.appendElement("div").attr("id", "rightcol")
				.appendElement("iframe")
				.attr("name", "pathwayDetails")
				.attr("srcdoc","<p>Click on a pathway name, to see the proteins that take part in that pathway</p>");

			Element content = container.appendElement("div").attr("id", "leftcol");
			content.appendElement("h2").text("Pathway results");

			Element summary = content.appendElement("div").attr("id", "preamble");

			summary.appendElement("p")
					.text("Below is an interactive listing of the resultant pathways from this analysis. "
							+ "Details on any particular pathway can be shown on the right by clicking the pathway name. "
							+ "Similarly you can re-sort the table by clicking the table header of the column you want to sort.");
			
			summary.appendElement("h5").text("Table legend:");
			Element legend = summary.appendElement("ul");
			legend.appendElement("li").text("PARAM Score: significance of parametric regulation model [1E-6" + R_ARROW + "1]");
			legend.appendElement("li").text("N-PAR Score: significance of non-parametric enrichment model [1E-10" + R_ARROW + "1]");
			legend.appendElement("li").text("Meta Score: consensus of significance scores [100" + R_ARROW + "0]");
			
			Element table = content.appendElement("table")
									.attr("id", "pathTable")
									.attr("class", "tablesorter");
			
			Element row;
			row = table.appendElement("thead").appendElement("tr");
			row.appendElement("th").attr("class", "header").text("Pathway name");
			row.appendElement("th").attr("class", "header").text("DB");
			row.appendElement("th").attr("class", "headerSortDown").text("PARAM score");
			row.appendElement("th").attr("class", "headerSortDown").text("N-PAR score");
			row.appendElement("th").attr("class", "headerSortUp").text("META score");
			Element tbody = table.appendElement("tbody");

			int counter = 0;
			for(AnalysisResult r : res){
				createIndividualPathPage(++counter, r, individualPages);
				if(r.isGO())
					continue;
				
				String link = Integer.toString(counter) + ".html";
				row = tbody.appendElement("tr");
				row.appendElement("td")
					.appendElement("a")
					.attr("href", individualPages.getName() + File.separator + link)
					.attr("target", "pathwayDetails")
					.text(r.getPath().getName());
				
				row.appendElement("td").text(r.getPath().getDb());
				row.appendElement("td").text(pathformat.format(r.getPar_score()));
				row.appendElement("td").text(pathformat.format(r.getPsea_score()));
				row.appendElement("td").text(metaformat.format(r.getMeta_score()));
				
			}
			
			doc.body().append("<script>$(document).ready(function(){$(\"#pathTable\").tablesorter( {sortList: [[2,0], [3,0], [4,1]]} );});</script>");
			addFooter(container);
			writeOutToFile(doc, pathPage);
		}

		public void createDatasetPage() throws SQLException, IOException{
			File dataPage = new File(topfolder, "data.html");
			File protPages = new File(topfolder, "prots");
			protPages.mkdir();
			
			Document doc = initHTMLDoc();
			
			// Document BODY
			Element container = doc.body().appendElement("div").attr("id", "container");
			Element header = container.appendElement("div").attr("id", "header");
			header.appendElement("div").attr("id", "logo");
			
			addInfoTable(header, dbMan.getDBName(), res.size());
			addNavBar(container);
			
			container.appendElement("div").attr("id", "rightcol")
						.appendElement("iframe")
							.attr("name", "protDetails")
							.attr("srcdoc","<p>Click on a protein name, to see the associated annotations</p>");
			
			Element content = container.appendElement("div").attr("id", "leftcol");
			content.appendElement("h2").text("Dataset");
			
			Element summary = content.appendElement("div").attr("id", "preamble");
			
			summary.appendElement("p")
					.text("Below is an interactive listing of the variables in the dataset. "
							+ "Click on the accessions for UniProtKB pages, or on the protein names "
							+ "to see which the annotations for each protein.");
			
			summary.appendElement("button").attr("id", "prots_all").attr("type","button").attr("class","filter").attr("onclick","tableFilter(\"all\", datasetTable);").text("ALL");
			summary.appendElement("button").attr("id", "prots_roi").attr("type","button").attr("class","filter").attr("onclick","tableFilter(\"roi\", datasetTable);").text("ROI");
			
			Element table = content.appendElement("table")
									.attr("id", "datasetTable")
									.attr("class", "tablesorter");
			
			Element row;
			row = table.appendElement("thead").appendElement("tr");
			row.appendElement("th").attr("class", "header").text("Accession(s)");
			row.appendElement("th").attr("class", "header").text("Protein name");
			row.appendElement("th").attr("class", "header").text("Gene symbol");
			row.appendElement("th").attr("class", "headerSortUp").text("Ratio");
			row.appendElement("th").attr("class", "headerSortDown").text("p-value");
			Element tbody = table.appendElement("tbody");
			
			for(Data d : ds.getDataRows()){
				createIndividualProtPage(d, protPages);
				String link = Integer.toString(d.getUid()) + ".html";
				//String acc = d.getProteins().get(0);
				List<String> accs = d.getProteins();
				
				double ratio = d.getRatio(), p = d.getPval();
				boolean inroi = AnalysisParams.getInstance().isInROI(ratio, p);
				row = tbody.appendElement("tr");
				row.attr("class", inroi ? "roi" : "noroi");	
				addUniProtLinks(row, d);
							
				Iterator<ProteinImpl> it = dbMan.matchProteinGroup2DB(accs).iterator();
				ProteinImpl prot;	
				
				try{
					prot = it.next();
					row.appendElement("td")
						.appendElement("a")
							.attr("href", protPages.getName() + File.separator + link)
							.attr("target", "protDetails")
							.text(prot.getName());
				
					row.appendElement("td").text(prot.getGene_symbol());
				}
				catch(NoSuchElementException e){
					logger.warning("No hits for protein group: " + d.getProteins());
					row.appendElement("td").text(NOT_FOUND);
					row.appendElement("td").text("");
				}	
				
				row.appendElement("td").text(valformat.format(ratio));
				row.appendElement("td").text(valformat.format(p));
			}
			
			doc.body().appendElement("script")
						.text("$(document).ready("
								+ "function(){"
								+ "$(\"#datasetTable\").tablesorter( {sortList: [[3,1], [4,0]] } );"
								+ "});");
			
			addFooter(container);
			writeOutToFile(doc, dataPage);
			createDataVisPages();
		}
		
		public void createIndexPage() throws UnsupportedEncodingException, FileNotFoundException{
			AnalysisParams param = AnalysisParams.getInstance();
			File indexPage = new File(topfolder, "index.html");
			
			Document doc = initHTMLDoc();
			
			// Document BODY
			Element container = doc.body().appendElement("div").attr("id", "container");
			Element header = container.appendElement("div").attr("id", "header");
			header.appendElement("div").attr("id", "logo");
			
			addInfoTable(header, dbMan.getDBName(), res.size());
			addNavBar(container);
			
			// Calculate paths that are significant with both methods
			AnalysisResult p;
			int paths = 0, go_terms = 0, sig_paths = 0;
			Iterator<AnalysisResult> it = res.iterator();
			while(it.hasNext()){
				p = it.next();
				if(p.isGO())
					go_terms++;
				else 
					paths++;
				
				if (p.getPar_score() < SCORE_SIG && p.getPsea_score() < SCORE_SIG)
					sig_paths++;
			}
			
			Element content = container.appendElement("div").attr("id", "content");
			content.appendElement("h2").text("Summary of the analysis");
			
			Element summary = content.appendElement("div").attr("id", "summary");
			
			summary.appendElement("p")
					.text("Analysis was successfully compeleted. " 
							+ "Below are some of the highlights, given in form of a short summary");
			
			Element ul = summary.appendElement("ul");
			ul.appendElement("li").text("Out of " + ds.getNbrOfRows() + " data points in the input datafile "
											+ ds.getROI().size() + " were in the defined region of interest.");
			
			ul.appendElement("li").text("A total of " + paths + " pathways, and " 
										+ go_terms + " GO terms were associated with the given data");
			
			ul.appendElement("li").text(sig_paths + " pathways have scored " + SCORE_SIG + " or better, by both models");
			ul.appendElement("li").text("Mean process time per annotation is " 
										+ timeformat.format(DebugToolbox.getMeanProcTime()));
			
			Element paramsDiv = content.appendElement("div").attr("id", "params");
			paramsDiv.appendElement("p").text("Below are the parameters used for this analysis:");
			Element codebox = paramsDiv.appendElement("div").attr("id", "codebox").appendElement("table");
			Element row;
			row = codebox.appendElement("tr");
			row.appendElement("th").text("Parameter name");
			row.appendElement("th").text("Given value");
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text(ALPHA + "_1");
			row.appendElement("td").attr("val", "title").text(Double.toString(param.getAlphaCoefficients(1)));
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text(ALPHA + "_2");
			row.appendElement("td").attr("val", "title").text(Double.toString(param.getAlphaCoefficients(2)));
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text(ALPHA + "_3");
			row.appendElement("td").attr("val", "title").text(Double.toString(param.getAlphaCoefficients(3)));
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text("Fold change");
			row.appendElement("td").attr("val", "title").text(Double.toString(param.getRegLevelThreshold()));
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text("p-value");
			row.appendElement("td").attr("val", "title").text(Double.toString(param.getSignLevelThreshold()));
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text("Non-linearity factor K_1");
			row.appendElement("td").attr("val", "title").text(Double.toString(param.getKappaCoefficients(1)));
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text("Non-linearity factor K_2");
			row.appendElement("td").attr("val", "title").text(Double.toString(param.getKappaCoefficients(2)));
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text("Randomization method");
			row.appendElement("td").attr("val", "title").text(param.getRandMethod().toString());
			
			row = codebox.appendElement("tr");
			row.appendElement("td").attr("class", "title").text("Sorting method");
			row.appendElement("td").attr("val", "title").text(param.getSortMethod().toString());
			
			addFooter(container);
//			System.out.println(doc);
			writeOutToFile(doc, indexPage);
		}
		
		private void loadRes2() throws IOException{
			File htmlRes = new File(topfolder, "res");
			if(!htmlRes.exists() || !htmlRes.isDirectory())
				htmlRes.mkdir();
			
			FileOutputStream fos;
			URL file_url;
			final String prefix = "res";
			
			for(String filename : ResourceUtils.listFiles(ExportUtils.class, prefix)){
				if(filename.contains("/"))
					continue;
				
				file_url = ClassLoader.getSystemClassLoader().getResource(prefix + "/" + filename);
				fos = new FileOutputStream(new File(htmlRes, filename));
				Resources.copy(file_url, fos);
				fos.close();
			}
		}
		
		private void loadRes() throws IOException{
	 		// get styling resources from the jar file 
			File htmlRes = new File(topfolder, "res");
			if(!htmlRes.exists() || !htmlRes.isDirectory())
				htmlRes.mkdir();
			
			File cssfile = new File(htmlRes, "style.css"),
			 color_file = new File(htmlRes, "colorbrewer.js"),
			 bubble_file = new File(htmlRes, "bubble.html"),
			 volcano_file = new File(htmlRes, "volcano.html"),
			 bullet_file = new File(htmlRes,"i.gif"), 
			 banner_file = new File(htmlRes, "banner.png"),
			 bg_file = new File(htmlRes,"body_bg.png"),
			 img_bg = new File(htmlRes,"button-bg.png"),
			 hov_bg = new File(htmlRes,"button-hover-bg.png"),
			 visu_img = new File(htmlRes,"visu.png"),
			 pc_img = new File(htmlRes,"pc.png"),
			 asc_gif = new File(htmlRes,"asc.gif"),
			 desc_gif = new File(htmlRes,"desc.gif"),
			 bg_gif = new File(htmlRes,"bg.gif");
			
			URL css_url = ExportUtils.class.getResource("/res/style.css");
			URL bullet_url = ExportUtils.class.getResource("/res/i.gif");
			URL banner_url = ExportUtils.class.getResource("/res/banner.png");
			URL bg_url = ExportUtils.class.getResource("/res/body_bg.png");
			URL image1_url = ExportUtils.class.getResource("/res/button-bg.png");
			URL image2_url = ExportUtils.class.getResource("/res/button-hover-bg.png");
			URL visu_url = ExportUtils.class.getResource("/res/visu.png");
			URL pc_url = ExportUtils.class.getResource("/res/pc.png");
			URL asc_url = ExportUtils.class.getResource("/res/asc.gif");
			URL desc_url = ExportUtils.class.getResource("/res/desc.gif");
			URL bggif_url = ExportUtils.class.getResource("/res/bg.gif");
			
			FileOutputStream fos = new FileOutputStream(cssfile);
			Resources.copy(css_url, fos);
			fos = new FileOutputStream(bullet_file);
			Resources.copy(bullet_url, fos);
			fos = new FileOutputStream(banner_file);
			Resources.copy(banner_url, fos);
			fos = new FileOutputStream(bg_file);
			Resources.copy(bg_url, fos);
			fos = new FileOutputStream(img_bg);
			Resources.copy(image1_url, fos);
			fos = new FileOutputStream(hov_bg);
			Resources.copy(image2_url, fos);
			fos = new FileOutputStream(visu_img);
			Resources.copy(visu_url, fos);
			fos = new FileOutputStream(pc_img);
			Resources.copy(pc_url, fos);
			fos = new FileOutputStream(asc_gif);
			Resources.copy(asc_url, fos);
			fos = new FileOutputStream(desc_gif);
			Resources.copy(desc_url, fos);
			fos = new FileOutputStream(bg_gif);
			Resources.copy(bggif_url, fos);
						
			// scripts and finish head
			URL sort_script = ExportUtils.class.getResource("/res/scripts.js");
			URL func_script = ExportUtils.class.getResource("/res/jquery.tablesorter.min.js");
			URL scinot_parser = ExportUtils.class.getResource("/res/jquery.tablesorter.scinot.js");
			
			File div_js = new File(htmlRes,"scripts.js");
			File jquery_sorttable = new File(htmlRes,"jquery.tablesorter.min.js");
			File jquery_st_scinot = new File(htmlRes, "jquery.tablesorter.scinot.js");
			
			fos = new FileOutputStream(div_js);
			Resources.copy(sort_script, fos);
			
			fos = new FileOutputStream(jquery_sorttable);
			Resources.copy(func_script, fos);
			
			fos = new FileOutputStream(jquery_st_scinot);
			Resources.copy(scinot_parser, fos);
	 	}
		
		private void createIndividualPathPage(int counter, AnalysisResult r, File parent) throws IOException, SQLException {
			// Begin creating the document
			Document doc = Document.createShell("");
			doc.prependChild(new Comment(commentStr,""));
			
			// Document HEAD
			doc.head().appendElement("meta").attr("http-equiv", "Content-Type").attr("content", "text/html; charset=utf-8");
			
			// Document BODY
			doc.body().attr("style", "font-family: Helvetica, arial, sans-serif; font-size: 0.8em;");
			Element container = doc.body().appendElement("div").attr("id", "container");
			Element summary = container.appendElement("div").attr("id", "summary");
			summary.appendElement("h4").text(r.getPath().getName());
			summary.appendElement("p").text("Below are the proteins in this pathway");
			
			Element table = container.appendElement("table").attr("class", "pathTable");
			table.attr("style", "font-size: 0.75em; padding-top:");
			Element row;
			row = table.appendElement("tr").attr("style", "text-align:left;");
			row.appendElement("th").text("Accession");
			row.appendElement("th").text("GeneSymbol");
			row.appendElement("th").text("Ratio");
			row.appendElement("th").text("p-val");
			
			for(ProteinImpl prot : dbMan.getAllProtsInPath(r.getPath().getId())){
				Data d = ds.getProteinData(prot.getAcc());
				double ratio = (d != null) ? d.getRatio() : Double.NaN,
						pval = (d != null) ? d.getPval() : Double.NaN;
				
				boolean inRoI = ds.getROI().contains(d);
				String style;
				if(!inRoI)
					style = "inherit;";
				else if (ratio < 1)
					style = "font-color: #00c;";
				else 
					style = "font-color: #c00;";
				
				row = table.appendElement("tr").attr("style", style);
				row.appendElement("td").text(prot.getAcc());
				row.appendElement("td").text(prot.getGene_symbol());
				row.appendElement("td").text(valformat.format(ratio));
				row.appendElement("td").text(valformat.format(pval));
			}
			
			File f = new File(parent,Integer.toString(counter) + ".html");
			f.createNewFile();
			
			Element footer = container.appendElement("div").attr("id", "footer");
			footer.attr("style", "font-size: 0.75em; float: left; border-top: #c00 solid 1px;");
			writeOutToFile(doc, f);
		}
		
		private void createIndividualProtPage(Data d, File parent) throws SQLException, IOException{
			
			// Begin creating the document
			Document doc = Document.createShell("");
			doc.prependChild(new Comment(commentStr,""));
			
			// Document HEAD
			doc.head().appendElement("meta").attr("http-equiv", "Content-Type").attr("content", "text/html; charset=utf-8");
			
			// Document BODY
			doc.body().attr("style", "font-family: Helvetica, arial, sans-serif; font-size: 0.8em;");
			Element container = doc.body().appendElement("div").attr("id", "container");
			Element summary = container.appendElement("div").attr("id", "summary");
			summary.appendElement("h4").text(d.getProteins().toString());
			summary.appendElement("p").text("Below are the pathways/GO-terms associated with this data point");
			
			Element table = container.appendElement("table").attr("class", "protTable");
			table.attr("style", "font-size: 0.75em; padding-top:");
			Element row;
			row = table.appendElement("tr").attr("style", "text-align:left;");
			row.appendElement("th").text("Annotation");
			row.appendElement("th").text("DB");
			row.appendElement("th").text("PARAM");
			row.appendElement("th").text("N-PAR");
			row.appendElement("th").text("META");
			
			for(String acc : d.getProteins()){
				for(PathwayImpl path : dbMan.getAllPathsWithProtein(acc)){
					for(AnalysisResult r : res){
						if(r.getPath().getId() == path.getId()){
							
							double par = r.getPar_score(), 
									psea = r.getPsea_score(),
									meta = r.getMeta_score();
									
							String par_style = (par < SCORE_SIG) ? "font-color: #c00;" : "inherit;",
									psea_style = (psea < SCORE_SIG) ? "font-color: #c00;" : "inherit;";
							
							row = table.appendElement("tr");
							row.appendElement("td").text(r.getPath().getName());
							row.appendElement("td").text(r.getPath().getDb());
							row.appendElement("td").attr("style", par_style).text(pathformat.format(par));
							row.appendElement("td").attr("style", psea_style).text(pathformat.format(psea));
							row.appendElement("td").text(metaformat.format(meta));
						}
					}
				}
			}
			
			File f = new File(parent,Integer.toString(d.getUid()) + ".html");
			f.createNewFile();
			
			Element footer = container.appendElement("div").attr("id", "footer");
			footer.attr("style", "font-size: 0.75em; float: left; border-top: #c00 solid 1px;");
			writeOutToFile(doc, f);
			
		}
		
		private void createDataVisPages() throws UnsupportedEncodingException, FileNotFoundException, SQLException{
			File resFolder = new File(topfolder, "res");
			File jsonFile = new File(resFolder, "datagraph.json");
			JSONExporter json = new JSONExporter(dbMan);
			writeOutToFile(json.dataset2JSON(ds), jsonFile);
			
			File page = new File(topfolder, "datagraph.html");
			
			Document doc = initHTMLDoc();
			
			// Document BODY
			doc.body().appendElement("script").text("function setURL(url){document.getElementById('graph').src = url;}");
			Element container = doc.body().appendElement("div").attr("id", "container");
			Element header = container.appendElement("div").attr("id", "header");
			header.appendElement("div").attr("id", "logo");
			
			addInfoTable(header, dbMan.getDBName(), res.size());
			addNavBar(container);
			
			container.appendElement("h2").text("Dataset visualization");	
			Element summary = container.appendElement("div").attr("id", "preamble");		
			summary.appendElement("p").text("Below are interactive graphs of the data points in this dataset.");
			Element ul = summary.appendElement("ul");
			ul.appendElement("li").text("The bubble graph gives an intuitive overview of the dataset. "
							+ "The coloring of the bubbles denote the ratios (or lack thereof) where as the size and opacity "
							+ "show the significance of a particular data point in the dataset.");
			
			ul.appendElement("li").text("The volcano graph shows the spread of the data on a coordinate plane.");
			
			summary.appendElement("button")
					.attr("id", "viz_bubble")
					.attr("type","button")
					.attr("class","filter")
					.attr("onclick","setURL('res/bubble.html')")
					.text("Bubble");
			
			summary.appendElement("button")
					.attr("id", "viz_volcano")
					.attr("type","button")
					.attr("class","filter")
					.attr("onclick","setURL('res/volcano.html')")
					.text("Volcano");
			
			container.appendElement("div").attr("id", "frame")
						.appendElement("iframe")
						.attr("id", "graph")
						.attr("width", "960")
						.attr("height", "700")
						.attr("style", "border-style: none;");
			
			addFooter(container);
			writeOutToFile(doc, page);
		}
		
		private void addUniProtLinks(Element row, Data d){
			Element cell = row.appendElement("td");
			for(String acc : d.getProteins())
				cell.appendElement("a")
					.attr("href", UNILINK + acc)
					.attr("target", "blank")
					.text(acc + " ");
		}
		
		private void addInfoTable(Element header, String dbname, int ds_size){
			Element table = header.appendElement("div").attr("id", "info").appendElement("table");
			Element row = table.appendElement("tr");
			row.appendElement("th").text("Filename:");
			row.appendElement("td").text(AnalysisParams.getInstance().getDataFile().getName()); 
			
			row = table.appendElement("tr");
			row.appendElement("th").text("DB:");
			row.appendElement("td").text(dbname);
			
			row = table.appendElement("tr");
			row.appendElement("th").text("# of annotations:");
			row.appendElement("td").text(Integer.toString(ds_size));
			
			row = table.appendElement("tr");
			row.appendElement("th").text("Date:");
			row.appendElement("td").text(DebugToolbox.getDateTimeStamp());
			
			row = table.appendElement("tr");
			row.appendElement("th").text("Runtime:");
			row.appendElement("td").text(DebugToolbox.getExecTime());
		}
		
		private void addNavBar(Element container){
			Element ul = container.appendElement("div").attr("id", "navBar").appendElement("ul");
			ul.appendElement("li").appendElement("a")
									.attr("href", "index.html")
									.attr("title", "Summary of the analysis")
									.text("Summary");

			ul.appendElement("li").appendElement("a")
									.attr("href", "data.html")
									.attr("title", "An interactive list of the dataset")
									.text("Dataset");
			
			ul.appendElement("li").appendElement("a")
									.attr("href", "datagraph.html")
									.attr("title", "An interactive visualization of the dataset")
									.text("visualize");

			ul.appendElement("li").appendElement("a")
									.attr("href", "paths.html")
									.attr("title", "An interactive list of the resultant pathways")
									.text("Pathways");
			
			ul.appendElement("li").appendElement("a")
									.attr("href", "go.html")
									.attr("title", "An interactive list of the resultant GO terms")
									.text("GO Terms");
			
		}
		
		private Document initHTMLDoc(){
			// Begin creating the document
			Document doc = Document.createShell("");
			doc.prependChild(new Comment(commentStr,""));
			
			// Document HEAD
			doc.head().appendElement("meta").attr("http-equiv", "Content-Type").attr("content", "text/html; charset=utf-8");
			doc.head().appendElement("title").text("FEvER Pathway Analysis Results");
			doc.head().appendElement("link")
						.attr("href", "res/style.css")
						.attr("rel", "stylesheet")
						.attr("type", "text/css");
			
			// Link scripts from CDNJS
			doc.head().appendElement("script")
				.attr("type","text/javascript")
				.attr("src", "http://cdnjs.cloudflare.com/ajax/libs/d3/3.5.5/d3.min.js");
			doc.head().appendElement("script")
				.attr("type","text/javascript")
				.attr("src", "http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.3/jquery.min.js");
			doc.head().appendElement("script")
				.attr("type","text/javascript")
				.attr("src", "http://cdnjs.cloudflare.com/ajax/libs/jquery.tipsy/1.0.2/jquery.tipsy.min.js");
			doc.head().appendElement("script")
				.attr("type","text/javascript")
				.attr("src", "http://cdnjs.cloudflare.com/ajax/libs/jquery.tablesorter/2.19.1/js/jquery.tablesorter.min.js");
			doc.body().appendElement("script")
				.attr("type","text/javascript")
				.attr("src", "http://d3js.org/colorbrewer.v1.min.js");

			doc.head().appendElement("script")
				.attr("type","text/javascript").attr("src", "res/scripts.js");
			doc.head().appendElement("script")
				.attr("type","text/javascript").attr("src", "res/jquery.tablesorter.scinot.js");
			
			return doc;			
		}
		
		private void addFooter(Element container){
			Element footer = container.appendElement("div").attr("id", "footer");
			footer.appendElement("p").text("This page is auto-generated by FEvER Analysis Software");
			footer.appendElement("p").text("Copyright Ufuk Kirik, " + COPYRIGHT + " 2010-2014.");
		}
		
		private void writeOutToFile(Object doc, File f) throws UnsupportedEncodingException, FileNotFoundException{
			// TODO: warn if file exists
			// TODO: make sure file is rewritten if exists
			
			PrintWriter out = 
					new PrintWriter(
						new BufferedWriter(
							new OutputStreamWriter(
								new FileOutputStream(f), "UTF-8")));

			out.println(doc);
			out.flush();	
			out.close();
		}
	}
	
	static class JSONExporter{
		private static final String 
			QM = "\"", 
			COL = ":", 
			COMMA = ", ",
			nL = System.getProperty("line.separator");
		
		HashSet<String> nodes, links, paths;
		
		private DbManager dbMan;
		
		public JSONExporter(DbManager dbMan){
			this.dbMan = dbMan;
			
//			nodes = new HashSet<String>();
//			links = new HashSet<String>();
//			paths = new HashSet<String>();
//			extractLinksAndNodes(graphData, nodes, links, paths);
			
//			System.out.println("# of nodes: " +  nodes.size() 
//					+ " links: " + links.size() 
//					+ " paths: " + paths.size());
		}
		
//		@Deprecated
//		public String exportGraph2Json(){
//			StringBuilder sb = new StringBuilder("{");
//			sb.append(nL);
//			sb.append("\"nodes\"").append(" : ").append(set2String(nodes));
//			sb.append(COMMA);
//			sb.append(nL);
//			sb.append("\"paths\"").append(" : ").append(set2String(paths));
//			sb.append(COMMA);
//			sb.append(nL);
//			sb.append("\"links\"").append(" : ").append(set2String(links));
//			sb.append(nL);
//			sb.append("}");
//			return sb.toString();
//		}
		
		public String dataset2JSON(Dataset ds) throws SQLException{
			StringBuilder datasetJSON = new StringBuilder("{");
			datasetJSON.append(nL).append("\"children\" : ").append("[");
			StringBuilder alt_ids;
			List<String> accs;
			String protStr, jsonSeparator = "";
			Iterator<Data> dit = ds.getDataRows().iterator();
			Data d;
			while(dit.hasNext()){
				ProteinImpl prot = null;
				d = dit.next();
				alt_ids = new StringBuilder();
				double ratio = d.getRatio(), pval = d.getPval();
				accs = d.getProteins();
				
				
				Iterator<ProteinImpl> it = dbMan.matchProteinGroup2DB(accs).iterator();
				try{
					prot = it.next();
					String prefix = "";
					for(String s : accs){
						if(!s.equalsIgnoreCase(prot.getAcc())){
							alt_ids.append(prefix);
							prefix = ";";
							alt_ids.append(s);
						}
					}
	
					protStr =  prot2Json(prot,ratio, pval, alt_ids.toString());
					datasetJSON.append(jsonSeparator);
					datasetJSON.append(nL);
					datasetJSON.append(protStr);
					jsonSeparator = ",";
				}
				catch(NoSuchElementException e){
					logger.warning("No hits for protein group: " + d.getProteins());
				}	
			}
			datasetJSON.append(nL).append("]");
			datasetJSON.append(nL).append("}");
			return datasetJSON.toString();
		}
		
		public String pathwayHierarchy2JSON(Collection<PathwayImpl> paths){
			StringBuilder hierarchyJSON = new StringBuilder("{");
			
			
			return hierarchyJSON.toString();
		}

		private String prot2Json(ProteinImpl p, double ratio, double pval, String accs){
			StringBuilder sb = new StringBuilder("{");
			sb.append(nL);
			sb.append(attr("acc", p.getAcc()));
			sb.append(COMMA).append(nL);
			sb.append(attr("uid", p.getUniprot_id()));
			sb.append(COMMA).append(nL);
			sb.append(attr("sym", p.getGene_symbol()));
			sb.append(COMMA).append(nL);
			sb.append(attr("name", p.getName()));
			sb.append(COMMA).append(nL);
			sb.append(attr("alt_ids", accs));
			sb.append(COMMA).append(nL);
			sb.append(attr("ratio", ratio));
			sb.append(COMMA).append(nL);
			sb.append(attr("pval", pval));
			sb.append(nL);
			sb.append("}");
			return sb.toString();	
		}
		
		private String path2Json(PathwayImpl p, Collection<PathwayImpl> children){
			StringBuilder sb = new StringBuilder("{");
			sb.append(nL);
			sb.append(attr("id", Long.toString(p.getId())));
			sb.append(COMMA).append(nL);
			sb.append(attr("name", p.getName()));
			sb.append(COMMA).append(nL);
			sb.append(attr("db", p.getDb()));
			sb.append(COMMA).append(nL);
			sb.append(attr("children", children)); // TODO: implement this properly
			sb.append("}");
			return sb.toString();	
		}
		
//		private void extractLinksAndNodes(
//				Multimap<Pair<String, String>, PathwayImpl> data, 
//				HashSet<String> nodes, 
//				HashSet<String> links,
//				HashSet<String> paths)
//		{
//			StringBuilder node1, node2, link, pathstr;
//			Data d1,d2;
////			Pair<String,String> pp;
////			PathwayImpl path;
////			for(Entry<Pair<String, String>, PathwayImpl> e : data.entries()){
////				pp = e.getKey();
////				path = e.getValue();
//			
//			for(Pair<String, String> pp : data.keySet()){
//				Collection<PathwayImpl> values = data.get(pp);
//				d1 = dataset.getProteinData(pp.first);
//				d2 = dataset.getProteinData(pp.second);
//				if(d1 == d2)
//					continue;
//				
//				node1 = new StringBuilder("{");
//				node2 = new StringBuilder("{");				
//				
//				// create nodes
//				node1.append(attr("name", pp.first));
//				node1.append(COMMA);
//				node1.append(attr("ratio", d1.getRatio()));
//				node1.append(COMMA);
//				node1.append(attr("pval", d1.getPval()));
//				node1.append("}");
//				nodes.add(node1.toString());
//				
//				node2.append(attr("name", pp.second));
//				node2.append(COMMA);
//				node2.append(attr("ratio", d2.getRatio()));
//				node2.append(COMMA);
//				node2.append(attr("pval", d2.getPval()));
//				node2.append("}");
//				nodes.add(node2.toString());			
//
//				HashSet<String> pids = new HashSet<String>();
//				for(PathwayImpl path : values){
//					pids.add(QM + Long.toHexString(path.getId()) + QM);
//	
//					// create path, includes edge info
//					pathstr = new StringBuilder("{");
//					pathstr.append(attr("pid", Long.toHexString(path.getId())));
//					pathstr.append(COMMA);
//					pathstr.append(attr("name", path.getName()));
//					pathstr.append("}");
//					paths.add(pathstr.toString());
//				}
//				
//				// create edge
//				link = new StringBuilder("{");
//				link.append(attr("source", pp.first));
//				link.append(COMMA);
//				link.append(attr("target", pp.second));
//				link.append(COMMA);
//				link.append(attr("weight", pids.size()));
//				link.append(COMMA);
//				link.append("\"pid\"").append(" : ").append(set2String(pids,false));
//				link.append("}");
//				links.add(link.toString());
//			}
//		}
		
		private String set2String(Set<String> s){
			return set2String(s,true);
		}
		
		private String set2String(Set<String> s, boolean addNewLines) {
			String itemSeparator = addNewLines ? nL : "";
			
	        Iterator<String> i = s.iterator();
	        if (! i.hasNext())
	            return "[]";

	        StringBuilder sb = new StringBuilder();
	        sb.append('[').append(itemSeparator);
	        for (;;) {
	            String str = i.next();
	            sb.append(str);
	            if (! i.hasNext())
	                return sb.append(itemSeparator).append(']').toString();
	            sb.append(",").append(itemSeparator);
	        }
	    }
		
		private String attr(String key, Object value){
			StringBuilder sb = new StringBuilder();
			sb.append(QM).append(key).append(QM).append(COL);
			
			if (value instanceof String)
				sb.append(QM).append(value).append(QM);
			else if (value instanceof Long)
				sb.append(valformat.format(value));
			else if(value instanceof Double)
				sb.append(Double.isNaN((double) value) ?  "null" : Double.toString((double) value));
			else 
				sb.append(value);
			
			return sb.toString();
		}
	}
}
