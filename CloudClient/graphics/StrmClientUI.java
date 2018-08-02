package graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Toolkit;

import javax.swing.JOptionPane;

public class StrmClientUI {
  final static private String VERSION= "1.8.2";
  final static private String MOTD= "https://www.github.com/TheWlr9/Strm";

  final private static Dimension SCREEN_SIZE= Toolkit.getDefaultToolkit().getScreenSize();
  final private static double SCREEN_WIDTH= SCREEN_SIZE.getWidth();
  final private static double SCREEN_HEIGHT= SCREEN_SIZE.getHeight();

  private static WindowedGraphics myWindow;
  final private static String TITLE= "Will's Cloud";
  private static int width= (int)(SCREEN_WIDTH/2);//1024;
  private static int height= (int)(3*SCREEN_HEIGHT/4);//768;
  private int maxFilesPerPage;

  private static FileDialog fileChooser;

  final private static Font FILES_FONT= new Font("Arial Black",Font.PLAIN,width/64);
  final public static int LEFT_FILES= width/10;
  final public static int TEXT_HEIGHT= height/38;
  final public static int FILES_BOX_X= width/3;
  final public static int FILES_BOX_Y= height/4;
  final public static int FILES_BOX_WIDTH= width/2;
  final public static double FILES_BOX_SPACING_MULTIPLIER= 1.5;

  final private static Font MSG_FONT= new Font("Arial Black", Font.BOLD, width/20);
  final private static int MSG_X= FILES_BOX_X;
  final private static int MSG_Y= height/6;
  final private static int MSG_WIDTH= FILES_BOX_WIDTH;
  final private static int MSG_HEIGHT= height/10;

  final private static Font BUTTON_FONT= new Font("Arial Black",Font.ITALIC,width/20);
  final public static int BUTTON_X= 3*width/4;
  final public static int BUTTON_Y= (int)(9.5*height/10);
  final public static int BUTTON_WIDTH= width/5;
  final public static int BUTTON_HEIGHT= height/10;


  final private static int LOADING_X= MSG_X;
  final private static int LOADING_Y= MSG_Y/2;
  final private static int LOADING_WIDTH= width/2;
  final private static int LOADING_LEFT= LOADING_X-LOADING_WIDTH/2;
  final private static int LOADING_RIGHT= LOADING_X+LOADING_WIDTH/2;
  final private static int LOADING_HEIGHT= MSG_HEIGHT/2;

  final public static int PAGE_L_X= width/2-40;
  final public static int PAGE_R_X= width/2+40;
  final public static int PAGE_Y= (int)(height*(9.5/10.0));//height-50;
  final public static int PAGE_BUTTON_WIDTH= 60;
  final public static int PAGE_BUTTON_HEIGHT= 30;


  public StrmClientUI(int maxFilesPerPage) {
    this.maxFilesPerPage= maxFilesPerPage;
    
    myWindow= new WindowedGraphics(width,height);
    fileChooser= new FileDialog(myWindow.getFrame());
    fileChooser.setMultipleMode(false); //To be changed at a later date?

    myWindow.setTitle(TITLE+" "+VERSION);
  }

  /**
   * 
   * @param msg The message of the error popup
   * @param title The title of the error popup
   */
  public void popupError(String msg, String title) {
    JOptionPane.showMessageDialog(myWindow.getFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
  }
  /**
   * 
   * @return True if user selected YES
   */
  public boolean popupDeleteFileConfirmation() {
    return JOptionPane.showConfirmDialog(myWindow.getFrame(), "Would you like to remove the file from the cloud?", "Remove file?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)==JOptionPane.YES_OPTION;
  }
  
  /**
   * 
   * @param fileToSave The file name to save
   */
  public void saveFilePopup(String fileToSave) {
    fileChooser.setTitle("Choose a location to save the file to");
    fileChooser.setFile(fileToSave);
    fileChooser.setMode(FileDialog.SAVE);
    fileChooser.setVisible(true);
  }
  public String getSavingName() {
    return fileChooser.getFile();
  }
  public String getSavingDirectory() {
    return fileChooser.getDirectory();
  }
  
  public void uploadFilePopup() {
    fileChooser.setTitle("Select a file to upload");
    fileChooser.setMode(FileDialog.LOAD);
    fileChooser.setVisible(true);
  }
  public String getUploadingName() {
    return fileChooser.getFile();
  }
  public String getUploadingDirectory() {
    return fileChooser.getDirectory();
  }

  /**
   * 
   * @param page The current page number
   * @param numOfPages The number of pages currently
   * @param cloudFilesNames The list of cloud file names
   */
  public void display(int page, int numOfPages, String[] cloudFilesNames){
    myWindow.clear();

    displayCloudFilesNames(page, cloudFilesNames);

    displayUploadButton();

    displayPageSelectionUI(page, numOfPages);
    
    displayMOTD();
  }

  /**
   * 
   * @param page The page number
   * @param cloudFilesNames The list of cloud file names
   */
  private void displayCloudFilesNames(int page, String[] cloudFilesNames){
    myWindow.setFont(FILES_FONT);
    //Draw the files uploaded
    for(int i= 0; i<cloudFilesNames.length-((page-1)*maxFilesPerPage) && i<maxFilesPerPage; i++){
      myWindow.setPenColour(WindowedGraphics.BLUE);
      myWindow.filledRectangle(FILES_BOX_X,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,(FILES_BOX_WIDTH)/2,(TEXT_HEIGHT)/2);
      myWindow.setPenColour(WindowedGraphics.WHITE);
      myWindow.textLeft(LEFT_FILES,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,cloudFilesNames[i+(maxFilesPerPage*(page-1))]);
    }
  }

  private void displayUploadButton(){
    //Draw the upload button
    myWindow.setPenColour(WindowedGraphics.BLACK);
    myWindow.rectangle(BUTTON_X,BUTTON_Y,(BUTTON_WIDTH)/2,(BUTTON_HEIGHT)/2);
    myWindow.setFont(BUTTON_FONT);
    myWindow.text(BUTTON_X,BUTTON_Y,"Upload");
  }

  /**
   * 
   * @param page The page number
   * @param numOfPages The maximum number of pages currently
   */
  private void displayPageSelectionUI(int page, int numOfPages) {
    myWindow.setFont();

    //Draw the page control management
    myWindow.setPenColour(WindowedGraphics.BLACK);

    myWindow.text(width/2, height*(8.5/10.0), "Displaying page "+page+" of "+(numOfPages));
    myWindow.text(PAGE_R_X, PAGE_Y, "NEXT");
    myWindow.rectangle(PAGE_R_X, PAGE_Y, (PAGE_BUTTON_WIDTH)/2, (PAGE_BUTTON_HEIGHT)/2);

    myWindow.text(PAGE_L_X, PAGE_Y, "PREV");
    myWindow.rectangle(PAGE_L_X, PAGE_Y, (PAGE_BUTTON_WIDTH)/2, (PAGE_BUTTON_HEIGHT)/2);
  }
  
  private void displayMOTD() {
    myWindow.text(width/2, (4.0/5.0)*height, MOTD);
  }
  
  public void setupLoadingBar() {
    myWindow.rectangle(LOADING_X, LOADING_Y, (LOADING_WIDTH)/2, (LOADING_HEIGHT)/2);
  }
  public void updateLoadingBar(double numerator, double denominator) {
    myWindow.setPenColour(WindowedGraphics.BLUE);
    myWindow.filledRectangle(LOADING_LEFT+(numerator/denominator)*(LOADING_RIGHT-LOADING_LEFT)/2, LOADING_Y, ((numerator/denominator)*(LOADING_RIGHT-LOADING_LEFT))/2, (LOADING_HEIGHT)/2);
  }
  
  public boolean exists() {
    return myWindow.exists();
  }
  
  public void close() {
    myWindow.close();
  }
  
  public boolean isMousePressed() {
    return myWindow.isMousePressed();
  }
  public double mouseX() {
    return myWindow.mouseX();
  }
  public double mouseY() {
    return myWindow.mouseY();
  }
  
  public void load(){
    //LOADING SCREEN
    myWindow.setPenColour(WindowedGraphics.BLACK);
    myWindow.setFont(MSG_FONT);
    myWindow.textLeft(LEFT_FILES,MSG_Y,"Loading...");
  }
  
  public void clearMsg(){
    Color prevColour= myWindow.getPenColour();
    myWindow.setPenColour(WindowedGraphics.WHITE);
    
    myWindow.filledRectangle(MSG_X,MSG_Y,(MSG_WIDTH)/2,(MSG_HEIGHT)/2);
    
    myWindow.setPenColour(prevColour);
  }
  

}
