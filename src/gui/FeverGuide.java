package gui;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

public class FeverGuide {
	
	private static final JFrame helpFrame = new JFrame();
	private static final JTextPane helpText = new JTextPane();
	private static final int WID = 480, HEI = 1080;
	private static final ImmutableMap<String, String> helpFiles = 
			ImmutableMap.of(
				"input.file", "bin/res/htmldocs/input.html",
				"preview", "bin/res/htmldocs/preview.html"
				);

	public static void showFileInputHelp() {
		try {
			Desktop.getDesktop().browse(new File(helpFiles.get("input.file")).toURI());
		} catch (IOException e) {
			e.printStackTrace();
		}
		//showFrame("input.file");
	}
	
	public static void showPreviewHelp() {
		try {
			Desktop.getDesktop().browse(new File(helpFiles.get("preview")).toURI());
		} catch (IOException e) {
			e.printStackTrace();
		}
//		showFrame("preview");
	}
	
	private static void showFrame(String helpFileName){
		String text;
		try {
			text = Files.toString(new File(helpFiles.get(helpFileName)), Charset.defaultCharset());
			helpText.setContentType("text/html");
			helpText.setText(text);
			helpText.setVisible(true);
			helpText.setEditable(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		helpFrame.add(new JScrollPane(helpText));
		helpFrame.setTitle("FEvER user guide");
		helpFrame.setFocusable(true);
		helpFrame.setPreferredSize(new Dimension(WID, HEI));
		helpFrame.setVisible(true);
		helpFrame.pack();
		helpFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}

}
