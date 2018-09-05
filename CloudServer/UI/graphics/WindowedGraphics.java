package graphics; 

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.swing.*;

/**
 * Standard drawing library. This class provides a basic capability for
 *  creating drawings with your programs. It uses a simple graphics model that
 *  allows you to create drawings consisting of points, lines, and curves
 *  in a window on your computer and to save the drawings to a file.
 *  
 *  <i>Standard draw</i>. This class provides a basic capability for
 *  creating drawings with your programs. It uses a simple graphics model that
 *  allows you to create drawings consisting of points, lines, and curves
 *  in a window on your computer and to save the drawings to a file.
 *  <p>
 *  For additional documentation, see <a href="http://introcs.cs.princeton.edu/15inout">Section 1.5</a> of
 *  <i>Introduction to Programming in Java: An Interdisciplinary Approach</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 *  @author William Ritchie
 *  @version 1.2.5
 *  
 *  Modified by: William Ritchie
 *  ~ Made everything instantiable. No more class methods!
 *  ~ Removed many un-needed methods in particular some image rotational methods and text rotational methods
 *  ~ Made the scaling much better, only allowed to have a minimum of 0
 *  ~ Added some boolean methods for mouse and keyboard interactions
 * 
 *  ~ Fixed scale methods
 *  
 *  Bugs:
 *  ~Resizing the window during execution makes the mouse methods go wack.
 *   |Temporary fixes: Don't resize the window during execution (consider frame.setResizeable(false);,
 *                     or if you're not using mouse methods, don't worry about it.
 */

public class WindowedGraphics implements MouseListener, MouseMotionListener, KeyListener {
 
    // pre-defined colors
    public static final Color BLACK      = Color.BLACK;
    public static final Color BLUE       = Color.BLUE;
    public static final Color CYAN       = Color.CYAN;
    public static final Color DARK_GRAY  = Color.DARK_GRAY;
    public static final Color GRAY       = Color.GRAY;
    public static final Color GREEN      = Color.GREEN;
    public static final Color LIGHT_GRAY = Color.LIGHT_GRAY;
    public static final Color MAGENTA    = Color.MAGENTA;
    public static final Color ORANGE     = Color.ORANGE;
    public static final Color PINK       = Color.PINK;
    public static final Color RED        = Color.RED;
    public static final Color WHITE      = Color.WHITE;
    public static final Color YELLOW     = Color.YELLOW;
 
    final private static double DEFAULT_XMIN = 0.0;
    final private static double DEFAULT_XMAX = 1.0;
    final private static double DEFAULT_YMIN = 0.0;
    final private static double DEFAULT_YMAX = 1.0;
 final private static int DEFAULT_SIZE= 128;
 final private static double DEFAULT_PEN_SIZE= 1;//0.002;
 final private static Font DEFAULT_FONT= new Font(Font.MONOSPACED, Font.PLAIN, 16);
 
 private boolean defer;
 
 Object keyLock= new Object();
 Object mouseLock= new Object();
 
 private JFrame frame;
 private Graphics2D onScreen, offScreen;
 private BufferedImage onScreenImage;
 private BufferedImage offScreenImage;
 
 private int width, height= DEFAULT_SIZE;
 private double xmin,xmax,ymin,ymax;
 private Color penColour;
 private double penRadius;
 private Font font;
 private String title;
 
 private boolean mousePressed;
 private double mouseX, mouseY;
 
 private LinkedList<Character> keysTyped = new LinkedList<Character>();
 private TreeSet<Integer> keysDown = new TreeSet<Integer>();

 public WindowedGraphics(int widthGiven, int heightGiven) {
   title= "WindowedGraphics";
  defer= false;
  penColour= BLACK;
  setSize(widthGiven,heightGiven);
  
  frame= new JFrame();
  offScreenImage= new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
  onScreenImage= new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
  onScreen= onScreenImage.createGraphics();
  offScreen= offScreenImage.createGraphics();
  
  if(widthGiven==heightGiven)
    setScale(0,widthGiven);
  else
    setScale(0,100);
  
  setPenRadius(); //Must be called after offScreen is initialized
  clear();
  
  
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        
     // frame stuff
        ImageIcon icon = new ImageIcon(onScreenImage);
        JLabel draw = new JLabel(icon);

        frame.setContentPane(draw);
        
        frame.setSize(width,height);
        
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY); //VALUE_RENDER_SPEED
        offScreen.addRenderingHints(hints);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle(title);
        frame.requestFocusInWindow();
        frame.setVisible(true);
        frame.pack();
        draw.addMouseListener(this);
        draw.addMouseMotionListener(this);
        frame.addKeyListener(this);
        //frame.setIconImage();
        setFont();
        setPenRadius();
        setPenColour();
        

 }
 
    /**
     * Set the x-scale to be the default (between 0.0 and 1.0).
     */
    public void setXscale() { setXscale(DEFAULT_XMAX); }

    /**
     * Set the y-scale to be the default (between 0.0 and 1.0).
     */
    public void setYscale() { setYscale(DEFAULT_YMAX); }

    /**
     * @param max the maximum value of the x-scale
     */
    public void setXscale(double max) {
            xmin = DEFAULT_XMIN;
            xmax = max;
    }

    /**
     * @param max the maximum value of the y-scale
     */
    public void setYscale(double max) {
            ymin = DEFAULT_YMIN;
            ymax = max;
    }
    
    /**
     * Set the x-scale and y-scale
     * @param min the minimum value of the x- and y-scales
     * @param max the maximum value of the x- and y-scales
     */
    public void setScale(double min, double max) {
        double size = max - min;
        xmin = min;
        xmax = max;
        ymin = min;
        ymax = max;
    }
    
    // helper functions that scale from user coordinates to screen coordinates and back
    private double scaleX(double x) { return x;} //width  * (x - xmin) / (xmax - xmin); }
    private double scaleY(double y) { return y;} //height * (ymax - y) / (ymax - ymin); }
    private double factorX(double w) { return w;} // * width  / Math.abs(xmax - xmin);  }
    private double factorY(double h) { return h;} // * height / Math.abs(ymax - ymin);  }
    private double userX(double x) { return xmin + x * (xmax - xmin) / width;    }
    private double userY(double y) { return ymax - y * (ymax - ymin) / height;   }
 
 public void setPenRadius() {
  setPenRadius(DEFAULT_PEN_SIZE);
 }
 
 public void setPenRadius(double size) {
  //Maybe add more options to the pen
        if (size < 0) throw new IllegalArgumentException("pen radius must be nonnegative");
        penRadius = size;
        float scaledPenRadius = (float) (size*2); //* DEFAULT_SIZE);
        BasicStroke stroke = new BasicStroke(scaledPenRadius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        // BasicStroke stroke = new BasicStroke(scaledPenRadius);
        offScreen.setStroke(stroke);
 }
 
 public void setPenColour() {
  setPenColour(BLACK);
 }
 
    public void setPenColor(int red, int green, int blue) {
        if (red   < 0 || red   >= 256) throw new IllegalArgumentException("amount of red must be between 0 and 255");
        if (green < 0 || green >= 256) throw new IllegalArgumentException("amount of green must be between 0 and 255");
        if (blue  < 0 || blue  >= 256) throw new IllegalArgumentException("amount of blue must be between 0 and 255");
        setPenColour(new Color(red, green, blue));
    }
 
 public void setPenColour(Color colour) {
        penColour = colour;
        offScreen.setColor(penColour);
    }
 
 public Color getPenColour(){
   return penColour;
 }
 
    public void setFont() { setFont(DEFAULT_FONT); }

    public void setFont(Font f) { font = f; }
 
 public void setSize() {
  setSize(DEFAULT_SIZE,DEFAULT_SIZE);
 }
 
 public void setSize(int width, int height) {
  if (width < 1 || height < 1) throw new IllegalArgumentException("width and height must be positive");
  this.width= width;
  xmax= scaleX(width);
  this.height=height;
  ymax= height;
 }
 
  public void setTitle(String title) {
  this.title= title;
  
  frame.setTitle(title);
 }
 
 public void clear() {
  clear(WHITE);
 }
 
 public void clear(Color color) {
  offScreen.setColor(color);
  offScreen.fillRect(0,0,width,height);
  offScreen.setColor(penColour);
  
  draw();
 }
 
    /**
     * Display on screen, pause for t milliseconds, and turn on
     * <em>animation mode</em>: subsequent calls to
     * drawing methods such as <tt>line()</tt>, <tt>circle()</tt>, and <tt>square()</tt>
     * will not be displayed on screen until the next call to <tt>show()</tt>.
     * This is useful for producing animations (clear the screen, draw a bunch of shapes,
     * display on screen for a fixed amount of time, and repeat). It also speeds up
     * drawing a huge number of shapes (call <tt>show(0)</tt> to defer drawing
     * on screen, draw the shapes, and call <tt>show(0)</tt> to display them all
     * on screen at once).
     * @param t number of milliseconds
     */
    public void show(int t) {
        defer = false;
        draw();
        try { Thread.sleep(t); }
        catch (InterruptedException e) { System.out.println("Error sleeping"); }
        defer = true;
    }

    /**
     * Display on-screen and turn off animation mode:
     * subsequent calls to
     * drawing methods such as <tt>line()</tt>, <tt>circle()</tt>, and <tt>square()</tt>
     * will be displayed on screen when called. This is the default.
     */
    public void show() {
        defer = false;
        draw();
    }

 
    private void draw() {
        if (defer) return;
        onScreen.drawImage(offScreenImage, 0, 0, null);
        frame.repaint();
    }
    
    
    /**
     * 
     * @return whether the current frame exists
     */
    public boolean exists() {
     return frame.isDisplayable();
    }
    
    /**
     * Closes the window
     */
    public void close() {
    	if(exists())
    		frame.dispose();
    }
    
    /**
     * @return the frame that is being used by this window
     */
    public JFrame getFrame(){
      return frame;
    }
    
    
    
    /*************************************************************************
     *  Drawing geometric shapes.
     *************************************************************************/

     /**
      * Draw a line from (x0, y0) to (x1, y1).
      * @param x0 the x-coordinate of the starting point
      * @param y0 the y-coordinate of the starting point
      * @param x1 the x-coordinate of the destination point
      * @param y1 the y-coordinate of the destination point
      */
     public void line(double x0, double y0, double x1, double y1) {
         offScreen.draw(new Line2D.Double(scaleX(x0), scaleY(y0), scaleX(x1), scaleY(y1)));
         draw();
     }

     /**
      * Draw one pixel at (x, y).
      * @param x the x-coordinate of the pixel
      * @param y the y-coordinate of the pixel
      */
     private void pixel(double x, double y) {
         offScreen.fillRect((int) Math.round(scaleX(x)), (int) Math.round(scaleY(y)), 1, 1);
     }

     /**
      * Draw a point at (x, y).
      * @param x the x-coordinate of the point
      * @param y the y-coordinate of the point
      */
     public void point(double x, double y) {
         double xs = scaleX(x);
         double ys = scaleY(y);
         double r = penRadius;
         float scaledPenRadius = (float) (r * DEFAULT_SIZE);

         // double ws = factorX(2*r);
         // double hs = factorY(2*r);
         // if (ws <= 1 && hs <= 1) pixel(x, y);
         if (scaledPenRadius <= 1) pixel(x, y);
         else offScreen.fill(new Ellipse2D.Double(xs - scaledPenRadius/2, ys - scaledPenRadius/2,
                                                  scaledPenRadius, scaledPenRadius));
         draw();
     }

     /**
      * Draw a circle of radius r, centered on (x, y).
      * @param x the x-coordinate of the center of the circle
      * @param y the y-coordinate of the center of the circle
      * @param r the radius of the circle
      * @throws IllegalArgumentException if the radius of the circle is negative
      */
     public void circle(double x, double y, double r) {
         if (r < 0) throw new IllegalArgumentException("circle radius must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*r);
         double hs = factorY(2*r);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.draw(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }

     /**
      * Draw filled circle of radius r, centered on (x, y).
      * @param x the x-coordinate of the center of the circle
      * @param y the y-coordinate of the center of the circle
      * @param r the radius of the circle
      * @throws IllegalArgumentException if the radius of the circle is negative
      */
     public void filledCircle(double x, double y, double r) {
         if (r < 0) throw new IllegalArgumentException("circle radius must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*r);
         double hs = factorY(2*r);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.fill(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }


     /**
      * Draw an ellipse with given semimajor and semiminor axes, centered on (x, y).
      * @param x the x-coordinate of the center of the ellipse
      * @param y the y-coordinate of the center of the ellipse
      * @param semiMajorAxis is the semimajor axis of the ellipse
      * @param semiMinorAxis is the semiminor axis of the ellipse
      * @throws IllegalArgumentException if either of the axes are negative
      */
     public void ellipse(double x, double y, double semiMajorAxis, double semiMinorAxis) {
         if (semiMajorAxis < 0) throw new IllegalArgumentException("ellipse semimajor axis must be nonnegative");
         if (semiMinorAxis < 0) throw new IllegalArgumentException("ellipse semiminor axis must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*semiMajorAxis);
         double hs = factorY(2*semiMinorAxis);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.draw(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }

     /**
      * Draw an ellipse with given semimajor and semiminor axes, centered on (x, y).
      * @param x the x-coordinate of the center of the ellipse
      * @param y the y-coordinate of the center of the ellipse
      * @param semiMajorAxis is the semimajor axis of the ellipse
      * @param semiMinorAxis is the semiminor axis of the ellipse
      * @throws IllegalArgumentException if either of the axes are negative
      */
     public void filledEllipse(double x, double y, double semiMajorAxis, double semiMinorAxis) {
         if (semiMajorAxis < 0) throw new IllegalArgumentException("ellipse semimajor axis must be nonnegative");
         if (semiMinorAxis < 0) throw new IllegalArgumentException("ellipse semiminor axis must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*semiMajorAxis);
         double hs = factorY(2*semiMinorAxis);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.fill(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }


     /**
      * Draw an arc of radius r, centered on (x, y), from angle1 to angle2 (in degrees).
      * @param x the x-coordinate of the center of the circle
      * @param y the y-coordinate of the center of the circle
      * @param r the radius of the circle
      * @param angle1 the starting angle. 0 would mean an arc beginning at 3 o'clock.
      * @param angle2 the angle at the end of the arc. For example, if
      *        you want a 90 degree arc, then angle2 should be angle1 + 90.
      * @throws IllegalArgumentException if the radius of the circle is negative
      */
     public void arc(double x, double y, double r, double angle1, double angle2) {
         if (r < 0) throw new IllegalArgumentException("arc radius must be nonnegative");
         while (angle2 < angle1) angle2 += 360;
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*r);
         double hs = factorY(2*r);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.draw(new Arc2D.Double(xs - ws/2, ys - hs/2, ws, hs, angle1, angle2 - angle1, Arc2D.OPEN));
         draw();
     }

     /**
      * Draw a square of side length 2r, centered on (x, y).
      * @param x the x-coordinate of the center of the square
      * @param y the y-coordinate of the center of the square
      * @param r radius is half the length of any side of the square
      * @throws IllegalArgumentException if r is negative
      */
     public void square(double x, double y, double r) {
         if (r < 0) throw new IllegalArgumentException("square side length must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*r);
         double hs = factorY(2*r);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.draw(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }

     /**
      * Draw a filled square of side length 2r, centered on (x, y).
      * @param x the x-coordinate of the center of the square
      * @param y the y-coordinate of the center of the square
      * @param r radius is half the length of any side of the square
      * @throws IllegalArgumentException if r is negative
      */
     public void filledSquare(double x, double y, double r) {
         if (r < 0) throw new IllegalArgumentException("square side length must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*r);
         double hs = factorY(2*r);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.fill(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }


     /**
      * Draw a rectangle of given half width and half height, centered on (x, y).
      * @param x the x-coordinate of the center of the rectangle
      * @param y the y-coordinate of the center of the rectangle
      * @param halfWidth is half the width of the rectangle
      * @param halfHeight is half the height of the rectangle
      * @throws IllegalArgumentException if halfWidth or halfHeight is negative
      */
     public void rectangle(double x, double y, double halfWidth, double halfHeight) {
         if (halfWidth  < 0) throw new IllegalArgumentException("half width must be nonnegative");
         if (halfHeight < 0) throw new IllegalArgumentException("half height must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*halfWidth);
         double hs = factorY(2*halfHeight);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.draw(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }

     /**
      * Draw a filled rectangle of given half width and half height, centered on (x, y).
      * @param x the x-coordinate of the center of the rectangle
      * @param y the y-coordinate of the center of the rectangle
      * @param halfWidth is half the width of the rectangle
      * @param halfHeight is half the height of the rectangle
      * @throws IllegalArgumentException if halfWidth or halfHeight is negative
      */
     public void filledRectangle(double x, double y, double halfWidth, double halfHeight) {
         if (halfWidth  < 0) throw new IllegalArgumentException("half width must be nonnegative");
         if (halfHeight < 0) throw new IllegalArgumentException("half height must be nonnegative");
         double xs = scaleX(x);
         double ys = scaleY(y);
         double ws = factorX(2*halfWidth);
         double hs = factorY(2*halfHeight);
         if (ws <= 1 && hs <= 1) pixel(x, y);
         else offScreen.fill(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
         draw();
     }


     /**
      * Draw a polygon with the given (x[i], y[i]) coordinates.
      * @param x an array of all the x-coordindates of the polygon
      * @param y an array of all the y-coordindates of the polygon
      */
     public void polygon(double[] x, double[] y) {
         int N = x.length;
         GeneralPath path = new GeneralPath();
         path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
         for (int i = 0; i < N; i++)
             path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
         path.closePath();
         offScreen.draw(path);
         draw();
     }

     /**
      * Draw a filled polygon with the given (x[i], y[i]) coordinates.
      * @param x an array of all the x-coordindates of the polygon
      * @param y an array of all the y-coordindates of the polygon
      */
     public void filledPolygon(double[] x, double[] y) {
         int N = x.length;
         GeneralPath path = new GeneralPath();
         path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
         for (int i = 0; i < N; i++)
             path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
         path.closePath();
         offScreen.fill(path);
         draw();
     }
     
     
     
     /*************************************************************************
      *  Drawing images.
      *************************************************************************/
     
     //Could add more rotational stuff and width, height stuff.

      // get an image from the given filename
      private Image getImage(String filename) {

          // to read from file
          ImageIcon icon = new ImageIcon(filename);

          // try to read from URL
          if ((icon == null) || (icon.getImageLoadStatus() != MediaTracker.COMPLETE)) {
              try {
                  URL url = new URL(filename);
                  icon = new ImageIcon(url);
              } catch (Exception e) { /* not a url */ }
          }

          // in case file is inside a .jar
          if ((icon == null) || (icon.getImageLoadStatus() != MediaTracker.COMPLETE)) {
              URL url = WindowedGraphics.class.getResource(filename);
              if (url == null) throw new IllegalArgumentException("image " + filename + " not found");
              icon = new ImageIcon(url);
          }

          return icon.getImage();
      }

      /**
       * Draw picture (gif, jpg, or png) centered on (x, y).
       * @param x the center x-coordinate of the image
       * @param y the center y-coordinate of the image
       * @param s the name of the image/picture, e.g., "ball.gif"
       * @throws IllegalArgumentException if the image is corrupt
       */
      public void picture(double x, double y, String s) {
          Image image = getImage(s);
          double xs = scaleX(x);
          double ys = scaleY(y);
          int ws = image.getWidth(null);
          int hs = image.getHeight(null);
          if (ws < 0 || hs < 0) throw new IllegalArgumentException("image " + s + " is corrupt");

          offScreen.drawImage(image, (int) Math.round(xs - ws/2.0), (int) Math.round(ys - hs/2.0), null);
          draw();
      }
      
      /**
       * Draw picture (gif, jpg, or png) centered on (x, y), rescaled to w-by-h.
       * @param x the center x coordinate of the image
       * @param y the center y coordinate of the image
       * @param s the name of the image/picture, e.g., "ball.gif"
       * @param w the width of the image
       * @param h the height of the image
       * @throws IllegalArgumentException if the width height are negative
       * @throws IllegalArgumentException if the image is corrupt
       */
      public void picture(double x, double y, String s, double w, double h) {
          Image image = getImage(s);
          double xs = scaleX(x);
          double ys = scaleY(y);
          if (w < 0) throw new IllegalArgumentException("width is negative: " + w);
          if (h < 0) throw new IllegalArgumentException("height is negative: " + h);
          double ws = factorX(w);
          double hs = factorY(h);
          if (ws < 0 || hs < 0) throw new IllegalArgumentException("image " + s + " is corrupt");
          if (ws <= 1 && hs <= 1) pixel(x, y); //Kind of for fun lol!!!
          else {
              offScreen.drawImage(image, (int) Math.round(xs - ws/2.0),
                                         (int) Math.round(ys - hs/2.0),
                                         (int) Math.round(ws),
                                         (int) Math.round(hs), null);
          }
          draw();
      }
      
      
      
      
      /*************************************************************************
       *  Drawing text.
       *************************************************************************/
      
      //Could add more text rotational stuff and text alignment stuff

       /**
        * Write the given text string in the current font, centered on (x, y).
        * @param x the center x-coordinate of the text
        * @param y the center y-coordinate of the text
        * @param s the text
        */
       public void text(double x, double y, String s) {
           offScreen.setFont(font);
           FontMetrics metrics = offScreen.getFontMetrics();
           double xs = scaleX(x);
           double ys = scaleY(y);
           int ws = metrics.stringWidth(s);
           int hs = metrics.getDescent();
           offScreen.drawString(s, (float) (xs - ws/2.0), (float) (ys + hs));
           draw();
       }


       /**
        * Write the given text string in the current font, left-aligned at (x, y).
        * @param x the x-coordinate of the text
        * @param y the y-coordinate of the text
        * @param s the text
        */
       public void textLeft(double x, double y, String s) {
           offScreen.setFont(font);
           FontMetrics metrics = offScreen.getFontMetrics();
           double xs = scaleX(x);
           double ys = scaleY(y);
           int hs = metrics.getDescent();
           offScreen.drawString(s, (float) (xs), (float) (ys + hs));
           draw();
       }
     
     
 
 
 /*************************************************************************
  *       Mouse stuff
  *************************************************************************/
 
 
 public double mouseX() {
  synchronized(mouseLock) {
   return mouseX;
  }
 }
 public double mouseY() {
  synchronized(mouseLock) {
   return mouseY;
  }
 }
 
 public boolean isMousePressed() {
  synchronized(mouseLock) {
   return mousePressed;
  }
 }
 
 public void mouseClicked(MouseEvent arg0) {
  // TODO Auto-generated method stub
  synchronized(mouseLock) {
   
  }
 }

 public void mouseEntered(MouseEvent arg0) {
  // TODO Auto-generated method stub
  synchronized(mouseLock) {
   
  }
 }

 public void mouseExited(MouseEvent arg0) {
  // TODO Auto-generated method stub
  synchronized(mouseLock) {
   
  }
 }

 public void mousePressed(MouseEvent arg0) {
  synchronized(mouseLock) {
   mousePressed= true;
  }
 }

 public void mouseReleased(MouseEvent arg0) {
  // TODO Auto-generated method stub
  synchronized(mouseLock) {
   mousePressed= false;
  }
 }

 @Override
 public void mouseDragged(MouseEvent arg0) {
  // TODO Auto-generated method stub
  synchronized(mouseLock) {
   mousePressed= true;
   mouseX= arg0.getX();
   mouseY= arg0.getY();
  }
 }

 @Override
 public void mouseMoved(MouseEvent arg0) {
  // TODO Auto-generated method stub
  synchronized(mouseLock) {
   mouseX= arg0.getX();
   mouseY= arg0.getY();
  }
 }

 
 
 /******************************************************************
  *        Keyboard stuff
  ******************************************************************/
 
 
 
 /**
     * Has the user typed a key?
     * @return true if the user has typed a key, false otherwise
     */
    public boolean hasNextKeyTyped() {
     synchronized(keyLock) {
      return !keysTyped.isEmpty();
     }
    }

    /**
     * What is the next key that was typed by the user? This method returns
     * a Unicode character corresponding to the key typed (such as 'a' or 'A').
     * It cannot identify action keys (such as F1
     * and arrow keys) or modifier keys (such as control).
     * @return the next Unicode key typed
     */
    public char nextKeyTyped() {
     synchronized(keyLock) {
      return keysTyped.removeLast();
     }
    }

    /**
     * Is the keycode currently being pressed? This method takes as an argument
     * the keycode (corresponding to a physical key). It can handle action keys
     * (such as F1 and arrow keys) and modifier keys (such as shift and control).
     * See <a href = "http://download.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html">KeyEvent.java</a>
     * for a description of key codes.
     * @return true if keycode is currently being pressed, false otherwise
     */
    public boolean isKeyPressed(int keycode) {
     synchronized(keyLock) {
      return keysDown.contains(keycode);
     }
    }


    /**
     * This method cannot be called directly.
     */
    public void keyTyped(KeyEvent e) {
     synchronized(keyLock) {
      keysTyped.addFirst(e.getKeyChar());
     }
    }

    /**
     * This method cannot be called directly.
     */
    public void keyPressed(KeyEvent e) {
     synchronized(keyLock){
      keysDown.add(e.getKeyCode());
     }
    }

    /**
     * This method cannot be called directly.
     */
    public void keyReleased(KeyEvent e) {
     synchronized(keyLock) {
      keysDown.remove(e.getKeyCode());
     }
    }
}

