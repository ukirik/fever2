package gui;

import java.awt.Dimension;
import java.util.logging.Logger;

import javax.swing.JInternalFrame;
import javax.swing.JTabbedPane;

import db.PathwayImpl;

public class InternalAnalysisFrame extends JInternalFrame {

	private static int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;
	private static final long serialVersionUID = -3919432700337689110L;

	public static final Logger logger = Logger.getLogger(PathwayImpl.class.getName());

    private JTabbedPane tabs;
	private DisabledGlassPane disGlass;

    public InternalAnalysisFrame() {
        this("Analysis #" + (++openFrameCount));
    }
    
    public InternalAnalysisFrame(String title){
    	super(title,true,true,true);
		disGlass = new DisabledGlassPane();
    	init();
    }
    
    public JTabbedPane getTabbedPane(){
    	return this.tabs;
    }
    
    public FeverMainFrame getMainFrame(){
    	return (FeverMainFrame) getTopLevelAncestor();
    }
    
    public void activateDisGlass(String msg){
		setGlassPane(disGlass);
    	this.disGlass.activate(msg);
    }
        
    public void deactivateDisGlass(){
    	this.disGlass.deactivate();
    }
    
    private void init(){
    	//...Create the GUI and put it in the window...
    	tabs  = new JTabbedPane();
    	tabs.setPreferredSize(new Dimension(800, 600));
    	setContentPane(tabs);
    	
    	pack();

        //Set the window's location.
        setLocation(xOffset*openFrameCount, yOffset*openFrameCount);
    }
    
}
