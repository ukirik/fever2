package gui;

import java.awt.Color;


/**
 * Common color utilities.
 * Originally written by Jacob Dreyer. Retrieved from 
 * <a href="http://www.java2s.com/Code/Java/2D-Graphics-GUI/Commoncolorutilities.htm">
 * Java2s.com</a>.
 * 
 * <p>Modified by Ufuk Kirik for <a href="http://quantitativeproteomics.org/fever">FEvER project</a></p>
 * @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
 * @author <a href="mailto:ufuk.kirik@immun.lth.se">Ufuk Kirik</a>
 */   
public class ColorUtil
{
  /**
   * Blend two colors.
   * 
   * @param color1  First color to blend.
   * @param color2  Second color to blend.
   * @param ratio   Blend ratio. 0.5 will give even blend, 1.0 will return
   *                color1, 0.0 will return color2 and so on.
   * @return        Blended color.
   */
  public static Color blend (Color color1, Color color2, double ratio)
  {
    float r  = (float) ratio;
    float ir = (float) 1.0 - r;

    float rgb1[] = new float[3];
    float rgb2[] = new float[3];    

    color1.getColorComponents (rgb1);
    color2.getColorComponents (rgb2);    

    Color color = new Color (rgb1[0] * r + rgb2[0] * ir, 
                             rgb1[1] * r + rgb2[1] * ir, 
                             rgb1[2] * r + rgb2[2] * ir);
    
    return color;
  }


  
  /**
   * Make an even blend between two colors.
   * 
   * @param c1     First color to blend.
   * @param c2     Second color to blend.
   * @return       Blended color.
   */
  public static Color blend (Color color1, Color color2)
  {
    return ColorUtil.blend (color1, color2, 0.5);
  }
  


  /**
   * Make a color darker.
   * 
   * @param color     Color to make darker.
   * @param fraction  Darkness fraction.
   * @return          Darker color.
   */
  public static Color darker (Color color, double fraction)
  {
    int red   = (int) Math.round (color.getRed()   * (1.0 - fraction));
    int green = (int) Math.round (color.getGreen() * (1.0 - fraction));
    int blue  = (int) Math.round (color.getBlue()  * (1.0 - fraction));

    if (red   < 0) red   = 0; else if (red   > 255) red   = 255;
    if (green < 0) green = 0; else if (green > 255) green = 255;
    if (blue  < 0) blue  = 0; else if (blue  > 255) blue  = 255;    

    int alpha = color.getAlpha();

    return new Color (red, green, blue, alpha);
  }

  

  /**
   * Make a color lighter.
   * 
   * @param color     Color to make lighter.
   * @param fraction  Darkness fraction.
   * @return          Lighter color.
   */
  public static Color lighter (Color color, double fraction)
  {
    int red   = (int) Math.round (color.getRed()   * (1.0 + fraction));
    int green = (int) Math.round (color.getGreen() * (1.0 + fraction));
    int blue  = (int) Math.round (color.getBlue()  * (1.0 + fraction));

    if (red   < 0) red   = 0; else if (red   > 255) red   = 255;
    if (green < 0) green = 0; else if (green > 255) green = 255;
    if (blue  < 0) blue  = 0; else if (blue  > 255) blue  = 255;    

    int alpha = color.getAlpha();

    return new Color (red, green, blue, alpha);
  }


  
  /**
   * Return the hex name of a specified color.
   * 
   * @param color  Color to get hex name of.
   * @return       Hex name of color: "rrggbb".
   */
  public static String getHexName (Color color)
  {
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();
    
    String rHex = Integer.toString (r, 16);
    String gHex = Integer.toString (g, 16);
    String bHex = Integer.toString (b, 16);        

    return (rHex.length() == 2 ? "" + rHex : "0" + rHex) +
           (gHex.length() == 2 ? "" + gHex : "0" + gHex) +
           (bHex.length() == 2 ? "" + bHex : "0" + bHex);
  }



  /**
   * Return the "distance" between two colors. The rgb entries are taken
   * to be coordinates in a 3D space [0.0-1.0], and this method returnes
   * the distance between the coordinates for the first and second color.
   * 
   * @param   r1, g1, b1  First color.
   * @param   r2, g2, b2  Second color.
   * @return  Distance bwetween colors.
   */
  public static double colorDistance (double r1, double g1, double b1,
                                      double r2, double g2, double b2)
  {
    double a = r2 - r1;
    double b = g2 - g1;
    double c = b2 - b1;
    
    return Math.sqrt (a*a + b*b + c*c);
  }
  
  
  
  /**
   * Return the "distance" between two colors.
   * 
   * @param color1  First color [r,g,b].
   * @param color2  Second color [r,g,b].
   * @return        Distance bwetween colors.
   */
  public static double colorDistance (double[] color1, double[] color2)
  {
    return ColorUtil.colorDistance (color1[0], color1[1], color1[2],
                                    color2[0], color2[1], color2[2]);
  }


  
  /**
   * Return the "distance" between two colors.
   * 
   * @param color1  First color.
   * @param color2  Second color.
   * @return        Distance between colors.
   */
  public static double colorDistance (Color color1, Color color2)
  {
    float rgb1[] = new float[3];
    float rgb2[] = new float[3];    

    color1.getColorComponents (rgb1);
    color2.getColorComponents (rgb2);    

    return ColorUtil.colorDistance (rgb1[0], rgb1[1], rgb1[2],
                                    rgb2[0], rgb2[1], rgb2[2]);
  }


  
  /**
   * Check if a color is more dark than light. Useful if an entity of
   * this color is to be labeled: Use white label on a "dark" color and
   * black label on a "light" color.
   *
   * @param r,g,b  Color to check.
   * @return       True if this is a "dark" color, false otherwise.
   */
  public static boolean isDark (double r, double g, double b)
  {
    // Measure distance to white and black respectively
    double dWhite = ColorUtil.colorDistance (r, g, b, 1.0, 1.0, 1.0);
    double dBlack = ColorUtil.colorDistance (r, g, b, 0.0, 0.0, 0.0);

    return dBlack < dWhite;
  }



  /**
   * Check if a color is more dark than light. Useful if an entity of
   * this color is to be labeled: Use white label on a "dark" color and
   * black label on a "light" color.
   *
   * @param color  Color to check.
   * @return       True if this is a "dark" color, false otherwise.
   */
  public static boolean isDark (Color color)
  {
    float r = color.getRed()   / 255.0f;
    float g = color.getGreen() / 255.0f;
    float b = color.getBlue()  / 255.0f;

    return isDark (r, g, b);
  }
  
  
  /*
   * Several named custom colors
   */
  public static final Color 
  	CARBONGRAY = new Color(77,77,77),
	PEACHRED = new Color(241,106,112),
  	FIREBRICK = new Color(178, 34, 34),
  	AZURE = new Color(0, 127, 255),
	LIME= new Color(34, 178, 34);
  
  /**
   * WHITE-RED-DARKBROWN gradient
   */
  public static final Color[] WRB_GRADIENT = 
		  new Color[]{
	    	new Color(255,245,240),
	    	new Color(254,224,210),
	    	new Color(252,187,161),
	    	new Color(252,146,114),
	    	new Color(251,106,74),
	    	new Color(239,59,44),
	    	new Color(203,24,29),
	    	new Color(165,15,21),
	    	new Color(103,0,13)
	    };
}
