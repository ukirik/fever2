/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author ufuk
 */
public class NewClass {
    public static void main(String[] args){
//       JOptionPane.showMessageDialog(null, "E13!","Error",
//            JOptionPane.ERROR_MESSAGE);
       
       JDialog jd = new JDialog(new JFrame(), "ERROR");
       jd.setSize(400, 300);
       jd.getContentPane().setBackground(Color.MAGENTA);
       jd.getContentPane().add(new JLabel("E13!"), BorderLayout.CENTER);
       jd.setVisible(true);
    }

}
