package graphics;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class StrmClientUI {
  final static private String VERSION= "beta-1.3.5";
  final static private String MOTD= "https://www.github.com/TheWlr9/Strm";
  final static public String CLOUD_ICON_PATH= "graphics/cloud-icon.png";

  private static WindowedGraphics myWindow;
  final private static String TITLE= "Strm";
  private static int width= (int)(500);//1024; //SCREEN_WIDTH/2
  private static int height= (int)(500);//768; //3*SCREEN_HEIGHT/4
  private int maxFilesPerPage;
  final private static int MAX_PASSWORD_LENGTH= 50;

  private static FileDialog fileChooser;
  
  public static int PASSIVE_TIMEOUT= 10; //The timeout for waiting for the next mouse press or keystroke etc...
  
  //All in RGB format
  public static final Color COLOUR_1= new Color(175,175,175);
  public static final Color COLOUR_2= new Color(100,100,100);

  final private static Font FILES_FONT= new Font("Arial Black",Font.PLAIN,width/54); ///64
  final public static int LEFT_FILES= width/10;
  final public static int TEXT_HEIGHT= height/28; ///38
  final public static int FILES_BOX_X= width/3;
  final public static int FILES_BOX_Y= height/4;
  final public static int FILES_BOX_WIDTH= width/2;
  final public static double FILES_BOX_SPACING_MULTIPLIER= 1.2; //1.5

  final private static Font MSG_FONT= new Font("Arial Black", Font.BOLD, width/20);
  final private static int MSG_X= FILES_BOX_X;
  final private static int MSG_Y= height/6;
  final private static int MSG_WIDTH= FILES_BOX_WIDTH;
  final private static int MSG_HEIGHT= height/10;

  final private static Font BUTTON_FONT= new Font("Arial Black",Font.ITALIC,width/20);
  final public static int UPLOAD_BUTTON_X= 7*width/8;
  final public static int UPLOAD_BUTTON_Y= (int)(9.5*height/10);
  final public static int UPLOAD_BUTTON_WIDTH= width/5;
  final public static int UPLOAD_BUTTON_HEIGHT=height/10;
  
  final public static int SETTINGS_BUTTON_X= width/4;
  final public static int SETTINGS_BUTTON_Y= (int)(9.5*height/10);
  final public static int SETTINGS_BUTTON_WIDTH= 2*width/5;
  final public static int SETTINGS_BUTTON_HEIGHT= height/10;


  final private static int LOADING_X= MSG_X;
  final private static int LOADING_Y= MSG_Y/2;
  final private static int LOADING_WIDTH= width/2;
  final private static int LOADING_LEFT= LOADING_X-LOADING_WIDTH/2;
  final private static int LOADING_RIGHT= LOADING_X+LOADING_WIDTH/2;
  final private static int LOADING_HEIGHT= MSG_HEIGHT/2;

  final public static int PAGE_L_X= 5*width/8-40;
  final public static int PAGE_R_X= 5*width/8+40;
  final public static int PAGE_Y= (int)(height*(9.5/10.0));//height-50;
  final public static int PAGE_BUTTON_WIDTH= 60;
  final public static int PAGE_BUTTON_HEIGHT= 30;
  
  final public static int REFRESH_BUTTON_X= width/2;
  final public static int REFRESH_BUTTON_Y= 7*height/10;
  final public static int REFRESH_BUTTON_WIDTH= (int)(1.2*width/5);
  final public static int REFRESH_BUTTON_HEIGHT= height/10;
  
  final public static int PASSWORD_BULLET_RADIUS= 10;
  
  
  
  
  private static int page;
  private static int numOfPages;
  private static String[] cloudFilesNames;


  public StrmClientUI(int maxFilesPerPage) {
    this.maxFilesPerPage= maxFilesPerPage;
    
    myWindow= new WindowedGraphics(width,height);
    fileChooser= new FileDialog(myWindow.getFrame());
    fileChooser.setMultipleMode(false); //To be changed at a later date?

    myWindow.setTitle(TITLE+" "+VERSION);
    ImageIcon i= new ImageIcon(CLOUD_ICON_PATH);
    myWindow.getFrame().setIconImage(i.getImage());
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
  public boolean popupPacketLossRetry() {
    return JOptionPane.showConfirmDialog(myWindow.getFrame(), "Would you like to try again?", "Error: Packet loss", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE)==JOptionPane.YES_OPTION;
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
   * Displays the password screen and modifies the password index of tender
   * @param tender The String[] that holds the IP address and the inputted password
   * Sets tender[1] to the inputted value, or an empty string if the user has closed the window.
   * @throws InterruptedException 
   */
  public void passwordScreen(String[] tender) throws InterruptedException {
    clear();
    
    myWindow.setFont(MSG_FONT);
    myWindow.text(width/2, height/2-height/5, "Enter \"back\" or enter password:");
    
    myWindow.setPenColour(myWindow.BLACK);
    
    char[] input= new char[MAX_PASSWORD_LENGTH];
    int letter= 0;
    boolean answer= false;
    int oldLength= 0;
    
    while(!answer && myWindow.exists()) {
      Thread.sleep(PASSIVE_TIMEOUT); //Calm down and wait a bit for before running the infiinite loop again
      
      if(myWindow.hasNextKeyTyped()) {
	char typed= myWindow.nextKeyTyped();
	if(typed!='\n' && typed!=8) { //Check to see if the character typed is not ENTER or BACKSPACE
	  input[letter]= typed;
  	  letter++;
  	  
  	  if(letter>=input.length)
  	    answer= true;
	}
	else if(typed=='\n') //ENTER
	  answer= true;
	else //BACKSPACE
	  if(letter>0) {
	    letter--;
	    input[letter]= 0;
	  }
      }
      
      if(oldLength!=letter) {
	//Then update the visuals
        clear();
        myWindow.text(width/2,  height/2-height/5, "Enter \"back\" or password:");
        
        for(int i= 0; i<letter; i++) 
  	  myWindow.filledCircle((width/2)+((i-letter/2)*20-((letter%2)*10)), height/2, PASSWORD_BULLET_RADIUS);
        
        oldLength= letter;
      }
    }
    
    if(myWindow.exists()) {
      myWindow.text(width/2, height-height/6, "Connecting...");
    
      tender[1]= (String.valueOf(input)).trim();
    }
    else
      tender[1]= "";
  }
  /**
   * Displays the IP address setting screen, and modifies the IP address index of tender (1)
   * @param tender The String[] holding the IP address and the password
   * Sets tender[0] to the inputted value, or and empty string if the window has been closed
   * @throws InterruptedException 
   */
  public void addressScreen(String[] tender) throws InterruptedException {
    clear();
    
    myWindow.setFont(MSG_FONT);
    myWindow.text(width/2, height/2-height/5, "Enter cloud address:");
    
    myWindow.setPenColour(myWindow.BLACK);
    
    char[] input= new char[50]; //Change to the new length
    int number= 0;
    boolean answer= false;
    int oldLength= 0;
    
    while(!answer && myWindow.exists()) {
      Thread.sleep(PASSIVE_TIMEOUT); //Wait a bit before executing
      
      if(myWindow.hasNextKeyTyped()) {
	char typed= myWindow.nextKeyTyped();
	if(typed!='\n' && typed!=8) { //Check to see if the character typed is not ENTER or BACKSPACE
	  input[number]= typed;
  	  number++;
  	  
  	  if(number>=input.length)
  	    answer= true;
	}
	else if(typed=='\n') //ENTER
	  answer= true;
	else //BACKSPACE
	  if(number>0) {
	    number--;
	    input[number]= 0;
	}
      }
      
      if(oldLength!=number) {
	//Then update the visuals
        clear();
        myWindow.text(width/2,  height/2-height/5, "Enter cloud address:");
        
        myWindow.text(width/2, height/2, String.valueOf(input).trim());
        
        oldLength= number;
      }
    }
    if(myWindow.exists())
      tender[0]= (String.valueOf(input)).trim();
    else
      tender[0]= "";
  }

  /**
   * 
   * @param page The current page number
   * @param numOfPages The number of pages currently
   * @param cloudFilesNames The list of cloud file names
   */
  public void display(int page, int numOfPages, String[] cloudFilesNames){
    this.page= page;
    this.numOfPages= numOfPages;
    this.cloudFilesNames= cloudFilesNames;
    
    
    clear();
    
    displayRefreshButton();

    displayCloudFilesNames(page, cloudFilesNames);

    displayUploadButton();
    
    displaySettingsButton();

    displayPageSelectionUI(page, numOfPages);
    
    displayMOTD();
  }
  
  /**
   * Draws the refresh button on screen
   */
  private void displayRefreshButton() {
    myWindow.setPenColour(WindowedGraphics.BLACK);
    
    //Draw the button
    myWindow.rectangle(REFRESH_BUTTON_X, REFRESH_BUTTON_Y, (REFRESH_BUTTON_WIDTH)/2, (REFRESH_BUTTON_HEIGHT)/2);
    myWindow.setFont(BUTTON_FONT);
    myWindow.text(REFRESH_BUTTON_X, REFRESH_BUTTON_Y, "Refresh");
  }

  /**
   * 
   * @param page The page number
   * @param cloudFilesNames The list of cloud file names
   */
  private void displayCloudFilesNames(int page, String[] cloudFilesNames){
    myWindow.setFont(FILES_FONT);
    //Draw the files uploaded
    if(cloudFilesNames.length>0) {
      for(int i= 0; i<cloudFilesNames.length; i++){
        myWindow.setPenColour(WindowedGraphics.BLUE);
        myWindow.filledRectangle(FILES_BOX_X,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,(FILES_BOX_WIDTH)/2,(TEXT_HEIGHT)/2);
        myWindow.setPenColour(WindowedGraphics.WHITE);
        myWindow.textLeft(LEFT_FILES,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,cloudFilesNames[i]);
      }
    }
  }

  private void displayUploadButton(){
    //Draw the upload button
    myWindow.setPenColour(WindowedGraphics.BLACK);
    myWindow.rectangle(UPLOAD_BUTTON_X,UPLOAD_BUTTON_Y,(UPLOAD_BUTTON_WIDTH)/2,(UPLOAD_BUTTON_HEIGHT)/2);
    myWindow.setFont(BUTTON_FONT);
    myWindow.text(UPLOAD_BUTTON_X,UPLOAD_BUTTON_Y,"Upload");
  }
  
  private void displaySettingsButton() {
    myWindow.setPenColour(WindowedGraphics.BLACK);
    myWindow.rectangle(SETTINGS_BUTTON_X, SETTINGS_BUTTON_Y, (SETTINGS_BUTTON_WIDTH)/2, (SETTINGS_BUTTON_HEIGHT)/2);
    myWindow.setFont(BUTTON_FONT);
    myWindow.text(SETTINGS_BUTTON_X, SETTINGS_BUTTON_Y, "Change Cloud");
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
  
  public void clear() {
    Color prevColour= myWindow.getPenColour();
    myWindow.clear();
    
    myWindow.setPenColour(COLOUR_1);
    myWindow.filledRectangle(width/2, height/2, (width)/2, (height)/2);
    
    myWindow.setPenColour(COLOUR_2);
    myWindow.picture(width/3, height/4, CLOUD_ICON_PATH);
    
    //Reset pen colour
    myWindow.setPenColour(prevColour);
  }
  
  public void clearLoading() {
    /*
    Color prevColor= myWindow.getPenColour();
    myWindow.setPenColour(myWindow.WHITE);
    
    myWindow.filledRectangle(LOADING_X, LOADING_Y, (LOADING_WIDTH)/2+LOADING_WIDTH/10, (LOADING_HEIGHT)/2+LOADING_HEIGHT/10);
    
    myWindow.setPenColour(prevColor);
    */
    
    //clear();
    display(page, numOfPages, cloudFilesNames);
  }
  

}
