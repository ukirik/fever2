/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FileInputDialog.java
 *
 * Created on Mar 10, 2014, 3:28:46 PM
 */
package gui;

import java.awt.Point;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import db.PathwayImpl;

/**
 *
 * @author ufuk
 */
public class FileInputDialog extends javax.swing.JDialog{

	private static final long serialVersionUID = -182707555023671884L;
	
	public static Logger logger = Logger.getLogger(PathwayImpl.class.getName());

	/** Creates new form FileInputDialog */
    public FileInputDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    public File getFile(){
    	return f;
    }
    
    public int getFileType(){
    	switch(fileTypeCombo.getSelectedIndex()){
    	case 0: return CSV;
    	case 1: return TSV;
    	default: return UNDEF;
    	}
    }
    
    public boolean getHeaderFlag(){
    	return colHeaderCheckBox.isSelected();
    }
    
    public boolean getIgnoreFlag(){
    	return ignoreQuotesCheckBox.isSelected();
    }
    
    public boolean getZeroTreatFlag(){
        return treatZerosCheckBox.isSelected();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        descriptionText = new javax.swing.JLabel();
        textField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        fileTypeCombo = new javax.swing.JComboBox();
        colHeaderCheckBox = new javax.swing.JCheckBox();
        ignoreQuotesCheckBox = new javax.swing.JCheckBox();
        treatZerosCheckBox = new javax.swing.JCheckBox();
        helpButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Selecting input file");

        descriptionText.setText("Please choose an input file to analyze");

        browseButton.setText("Browse...");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        nextButton.setText("Next ->");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        fileTypeCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Comma-separated (csv)", "Tab-separated (tsv, txt)" }));

        colHeaderCheckBox.setSelected(true);
        colHeaderCheckBox.setText("includes column headers");

        ignoreQuotesCheckBox.setSelected(true);
        ignoreQuotesCheckBox.setText("ignore quotation marks");

        treatZerosCheckBox.setSelected(true);
        treatZerosCheckBox.setText("Treat 0s as missing vals");

        helpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/info.png"))); // NOI18N
        helpButton.setToolTipText("Need help? ");
        helpButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        helpButton.setBorderPainted(false);
        helpButton.setContentAreaFilled(false);
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(colHeaderCheckBox)
                            .addComponent(ignoreQuotesCheckBox)
                            .addComponent(treatZerosCheckBox)
                            .addComponent(fileTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(textField)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(descriptionText)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 2, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nextButton)
                            .addComponent(browseButton))
                        .addGap(6, 6, 6))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(helpButton)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(descriptionText)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(textField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(helpButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(browseButton)
                    .addComponent(fileTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(nextButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(colHeaderCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ignoreQuotesCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(treatZerosCheckBox)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
		JFileChooser jfc = new JFileChooser();
		Point loc = getLocation();
		loc.translate(20, 20);
		jfc.setLocation(loc);
		jfc.setVisible(true);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);

		if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			try {
				f = jfc.getSelectedFile();
				textField.setText(f.getCanonicalPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}//GEN-LAST:event_browseButtonActionPerformed
	
	private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		setVisible(false);
	}//GEN-LAST:event_nextButtonActionPerformed

    private void helpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButtonActionPerformed
        FeverGuide.showFileInputHelp();
    }//GEN-LAST:event_helpButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JCheckBox colHeaderCheckBox;
    private javax.swing.JLabel descriptionText;
    private javax.swing.JComboBox fileTypeCombo;
    private javax.swing.JButton helpButton;
    private javax.swing.JCheckBox ignoreQuotesCheckBox;
    private javax.swing.JButton nextButton;
    private javax.swing.JTextField textField;
    private javax.swing.JCheckBox treatZerosCheckBox;
    // End of variables declaration//GEN-END:variables
    
    File f;
    public static final int CSV = 0, TSV = 1, UNDEF = -1; 
}