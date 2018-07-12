//Maybe make the waiting times dynamic?
/**
 * @Title Will's cloud
 * @author William Leonardo Ritchie
 * 
 * @version 1.7.1
 * 
 * _1.7.1_
 * 		~Changed the timeout feature on the upload option to work MUCH better.
 * 
 * _1.6.1_
 * 		~Added a timeout feature while uploading a file to the cloud to protect against 
 * 			never properly stopping the thread on the server side, and causing a 
 * 			HUGE overheat in server CPU.
 * 
 * _1.5.1_
 * 		~Changed the IOException ERROR pop-up message to better explain the rror to the user.
 * 		~Fixed a program crashing bug involving invisible links and switching pages
 * 
 * _1.5_
 * 		~Added multiple pages so you can now store INFINITE files, and find them all!
 * 
 * _1.2_
 *      ~Added error pop-ups for user clarification
 */
import java.io.*;
import java.net.*;
import java.awt.Font;
import java.awt.Color;	
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.Dimension;
import javax.swing.JOptionPane;

import graphics.WindowedGraphics;

public class CloudClient{
	final static private String VERSION= "1.7.1";
	
  final static private int PORT= 42843;
  final static private String ADDRESS= "192.168.1.101";
  final static private String FILE_PATH= "docs"+File.separator;
  final static private int SLEEP= 250;
  
  final static private int TIMEOUT= 3000;
  final static private int MAX_FILES_PER_PAGE= 10;
  final static private String SUCCESS_MSG= "success";
  final static private String PING= "ping";
  final static private String BUF_SIZE_REQ= "getBufferSize";
  final static private String NUM_FILES_REQ= "getNumOfFiles";
  final static private String FILES_REQ= "getFiles";
  final static private String UPLOAD= "uploadFile";
  final static private String DOWNLOAD= "downloadFile";
  final static private String SHUTDOWN= "logoff";
  
  private static boolean escaping;
  private static long maxPingRecorded;
  private static int bufferSize;
  private static int pageNo;
  private static int numOfPages;
  private static Socket serverSocket= null;
  private static InputStream inStream= null;
  private static OutputStream outStream= null;
  private static BufferedReader input= null;
  private static PrintWriter stringOutStream= null;
  private static BufferedReader stringInStream= null;
  
  private static byte[] byteArray;
  private static String[] cloudFilesNames;
  
  private static File file= null;
  private static FileInputStream fileSend= null;
  private static FileOutputStream fileReceive= null;
  
  final private static Dimension SCREEN_SIZE= Toolkit.getDefaultToolkit().getScreenSize();
  final private static double SCREEN_WIDTH= SCREEN_SIZE.getWidth();
  final private static double SCREEN_HEIGHT= SCREEN_SIZE.getHeight();
  
  private static WindowedGraphics myWindow;
  final private static String TITLE= "Will's Cloud";
  private static int width= (int)(SCREEN_WIDTH/2);//1024;
  private static int height= (int)(3*SCREEN_HEIGHT/4);//768;
  
  private static FileDialog fileChooser;
  
  final private static Font FILES_FONT= new Font("Arial Black",Font.PLAIN,width/64);
  final private static int LEFT_FILES= width/10;
  final private static int TEXT_HEIGHT= height/38;
  final private static int FILES_BOX_X= width/3;
  final private static int FILES_BOX_Y= height/4;
  final private static int FILES_BOX_WIDTH= width/2;
  final private static double FILES_BOX_SPACING_MULTIPLIER= 1.5;
  
  final private static Font MSG_FONT= new Font("Arial Black", Font.BOLD, width/20);
  final private static int MSG_X= FILES_BOX_X;
  final private static int MSG_Y= height/6;
  final private static int MSG_WIDTH= FILES_BOX_WIDTH;
  final private static int MSG_HEIGHT= height/10;
  
  final private static Font BUTTON_FONT= new Font("Arial Black",Font.ITALIC,width/20);
  final private static int BUTTON_X= 3*width/4;
  final private static int BUTTON_Y= 9*height/10;
  final private static int BUTTON_WIDTH= width/5;
  final private static int BUTTON_HEIGHT= width/10;
  
  
  final private static int LOADING_X= MSG_X;
  final private static int LOADING_Y= MSG_Y/2;
  final private static int LOADING_WIDTH= width/2;
  final private static int LOADING_LEFT= LOADING_X-LOADING_WIDTH/2;
  final private static int LOADING_RIGHT= LOADING_X+LOADING_WIDTH/2;
  final private static int LOADING_HEIGHT= MSG_HEIGHT/2;
  
  final private static int PAGE_L_X= width/2-40;
  final private static int PAGE_R_X= width/2+40;
  final private static int PAGE_Y= height-50;
  final private static int PAGE_BUTTON_WIDTH= 60;
  final private static int PAGE_BUTTON_HEIGHT= 30;
  
  //FLAGS
  private static boolean setupError;
  
  public static void main(String[] args){
	  setupError= false;
	  
    myWindow= new WindowedGraphics(width,height);
    fileChooser= new FileDialog(myWindow.getFrame());
    fileChooser.setMultipleMode(false); //To be changed at a later date?
    
    myWindow.setTitle(TITLE+" "+VERSION);
    
    try{
      serverSocket= new Socket(ADDRESS, PORT);
      
      serverSocket.setPerformancePreferences(0, 0, 1); //Prioritizes bandwidth
      serverSocket.setTrafficClass(0x18); //Prioritizes high throughput and low delay
      
      inStream= serverSocket.getInputStream();
      stringInStream= new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
      
      outStream= serverSocket.getOutputStream();
      stringOutStream= new PrintWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
      
      input= new BufferedReader(new InputStreamReader(System.in));
    }
    catch(IOException e){
      System.err.println("ERROR: CloudClient.main: Error in setting up streams");
      e.printStackTrace();
      
      JOptionPane.showMessageDialog(myWindow.getFrame(), "Unable to access Raspberry Pi", "Error", JOptionPane.ERROR_MESSAGE);
      
      setupError= true;
    }
    
    try{
    	pageNo= 1;
    	
    	escaping= false;
    	
      //HERE WAS THE NETWORKING STUFF
      stringOutStream.println(serverSocket.getLocalAddress()); //Must send over the IP address first
      stringOutStream.flush();
      
      stringOutStream.println(BUF_SIZE_REQ);
      stringOutStream.flush();
      //Receive the buffer size
      bufferSize= Integer.parseInt(stringInStream.readLine());
      System.out.println("Buffer size: "+bufferSize);
      
      byteArray= new byte[bufferSize];
      
      stringOutStream.println(NUM_FILES_REQ);
      stringOutStream.flush();
      int maxFilesUploaded= Integer.parseInt(stringInStream.readLine());
      System.out.println("FILES: "+maxFilesUploaded);
      cloudFilesNames= new String[maxFilesUploaded];
      
      updateCloudFilesNames();
      
      display(pageNo); //Starting page #
      
      //Start the main activity "listener"
      while(myWindow.exists()){
        
        if(myWindow.isMousePressed()){
          double mouseX= myWindow.mouseX();
          double mouseY= myWindow.mouseY();
          boolean download= false;
          boolean upload= false;
          if(mouseX>(FILES_BOX_X-FILES_BOX_WIDTH/2) && mouseX<(FILES_BOX_X+FILES_BOX_WIDTH/2)){
            for(int i= 0; i<MAX_FILES_PER_PAGE; i++){
              if(mouseY>(FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER-TEXT_HEIGHT/2) && mouseY<(FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER+TEXT_HEIGHT/2)){
                System.out.println("File chosen: "+cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))]);
                
                Thread.sleep(200);
                
                fileChooser.setTitle("Choose a location to save the file to");
                fileChooser.setFile(cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))]);
                fileChooser.setMode(FileDialog.SAVE);
                fileChooser.setVisible(true);
                
                if(fileChooser.getFile()!=null){
                  load();
                  
                  //Send download request to the server
                  stringOutStream.println(DOWNLOAD); //CONSTANT
                  
                  receiveFile(cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))], fileChooser.getDirectory(), fileChooser.getFile());
                  
                  download= true;
                  
                  //Get the new number of files
                  stringOutStream.println(NUM_FILES_REQ);
                  stringOutStream.flush();
                  cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
                  updateCloudFilesNames();
                  if(cloudFilesNames.length%MAX_FILES_PER_PAGE==0)
                	  pageNo--;
                  display(pageNo);
                  
                  clearMsg();
                }
                
                //Thread.sleep(200);
              }
            }
          }
          if(!download && mouseX>BUTTON_X-BUTTON_WIDTH/2 && mouseX<BUTTON_X+BUTTON_WIDTH/2 &&
          mouseY>BUTTON_Y-BUTTON_HEIGHT/2 && mouseY<BUTTON_Y+BUTTON_HEIGHT/2){
            Thread.sleep(200);
            
            fileChooser.setTitle("Select a file to upload");
            fileChooser.setMode(FileDialog.LOAD);
            fileChooser.setVisible(true);
            
            if(fileChooser.getFile()!=null){
              System.out.println("File chosen: "+fileChooser.getDirectory()+fileChooser.getFile());
              
              load();
              
              maxPingRecorded= maxPing();
              
              stringOutStream.println(UPLOAD); //CONSTANT
              
              sendFile(fileChooser.getDirectory(), fileChooser.getFile());
              
              if(!escaping) {
            	  upload= true;
            	  
            	  Thread.sleep(SLEEP);
        	  
            	  //Get the new number of files
            	  stringOutStream.println(NUM_FILES_REQ);
            	  stringOutStream.flush();
            	  cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
            	  
            	  updateCloudFilesNames();
            	  display(pageNo);
            	  
            	  clearMsg();
              }
            }
          }
          if(!download && !upload && mouseX>PAGE_L_X-PAGE_BUTTON_WIDTH/2 && mouseX<PAGE_R_X+PAGE_BUTTON_WIDTH/2
        		  && mouseY>PAGE_Y-PAGE_BUTTON_HEIGHT/2 && mouseY<PAGE_Y+PAGE_BUTTON_HEIGHT/2) {
        	  Thread.sleep(200);
        	  
        	  if(mouseX<PAGE_L_X+PAGE_BUTTON_WIDTH/2) {
        		  //The prev button has been hit
        		  
        		  if(pageNo>1) {
        			  pageNo--;
        			  
        			  display(pageNo);
        		  }
        	  }
        	  else if(mouseX>PAGE_R_X-PAGE_BUTTON_WIDTH/2){
        		  //The next button has been hit
        		  
        		  if(pageNo<numOfPages) {
        			  pageNo++;
        			  
        			  display(pageNo);
        		  }
        	  }
          }
          
          //Thread.sleep(200); //This is so it only executes the block once per mouse press. (Essentially is mouse clicked.)
        }
      }
      if(!escaping) {
    	  stringOutStream.println(SHUTDOWN);
    	  stringOutStream.flush();
      }
    }
    catch(Exception e){
    	if(!setupError) {
	    	if(e instanceof IOException) {
	    		System.err.println("ERROR: ClientSocket.main: Error in writing to server or reading from file");
	    		e.printStackTrace();
	    		
	    		JOptionPane.showMessageDialog(myWindow.getFrame(), "Packet loss. Please try again", "Error", JOptionPane.ERROR_MESSAGE);
	    	}
	    	else if(e instanceof NullPointerException) {
	    	      System.err.println("ERROR: ClientSocket: main: Accessing messed up locations");
	    	      e.printStackTrace();
	    	      
	    	      JOptionPane.showMessageDialog(myWindow.getFrame(), "File does not exist", "Error", JOptionPane.ERROR_MESSAGE);
	    	}
	    	else if(e instanceof InterruptedException) {
	    	      System.err.println("ERROR: ClientSocket: main: Interrupt encountered, cannot sleep");
	    	      
	    	      JOptionPane.showMessageDialog(myWindow.getFrame(), "Rushed service; failsafe triggered", "Error", JOptionPane.ERROR_MESSAGE);
	    	}
	    	else if(e instanceof SecurityException) {
	    	      System.err.println("ERROR: ClientSocket: main: Client attempting to access unauthorized files");
	    	      
	    	      JOptionPane.showMessageDialog(myWindow.getFrame(), "You are unauthorized to access those files", "Error", JOptionPane.ERROR_MESSAGE);
	    	}
	    	else {
	    		e.printStackTrace();
	    	}
    	}
    }
    finally{
    	if(myWindow.exists()) {
    		myWindow.close();
    	}
    	
      System.out.println("\nClosing connection...");
      try{
        if(inStream!=null)
          inStream.close();
        
        if(outStream!=null){
          outStream.close();
          if(stringOutStream!=null)
            stringOutStream.close();
        }
        
        if(serverSocket!=null)
          serverSocket.close();
        
        if(fileSend!=null)
          fileSend.close();
        
        if(fileReceive!=null)
          fileReceive.close();
        
        if(input!=null)
          input.close();
       System.out.println("Closed connection.");
      }
      catch(IOException e){
        System.err.println("ERROR: ClientCloud.main: Error in terminating streams");
      }
    }
  }
  
  private static void sendFile(String path, String fileName) throws IOException, InterruptedException, SecurityException{
	file= new File(path+fileName);
    if(fileSend!=null)
      fileSend.close();
    fileSend= new FileInputStream(file);
    
    System.out.println("Uploading file...");
    
    //Draw the empty loading buffer
	myWindow.rectangle(LOADING_X, LOADING_Y, (LOADING_WIDTH)/2, (LOADING_HEIGHT)/2);
	
	long waitTime= maxPingRecorded/2;
    int bytesRead= 0;
    int totalBytesRead= 0;
    
    //Need to send the file name to the server
    stringOutStream.println(fileName);
    stringOutStream.flush();
    
    //Send the size of the file in bytes to the server
    stringOutStream.println(file.length());
    stringOutStream.flush();
    
    Thread.sleep(SLEEP);
    
    //Write the byte stream to the server
    while((bytesRead= fileSend.read(byteArray))!=-1) {
      outStream.write(byteArray,0,bytesRead);
      
      totalBytesRead+= bytesRead;
      
      myWindow.setPenColour(WindowedGraphics.BLUE);
      myWindow.filledRectangle(LOADING_LEFT+((double)(totalBytesRead)/(double)(file.length()))*(LOADING_RIGHT-LOADING_LEFT)/2, LOADING_Y, (((double)(totalBytesRead)/(double)(file.length()))*(LOADING_RIGHT-LOADING_LEFT))/2, (LOADING_HEIGHT)/2);
    }
    
    outStream.flush();
    
    if(stringInStream.readLine().equals(SUCCESS_MSG))
    	System.out.println("Success!");
    else {
    	JOptionPane.showMessageDialog(myWindow.getFrame(), "Please try again", "Error: Packet loss", JOptionPane.ERROR_MESSAGE);
    	
    	escaping= true;
    	
    	myWindow.close();
    }
    
    //Maybe delete the file now?
  }
  private static void receiveFile(String remoteFileName, String localPath, String localFileName) throws IOException, SecurityException, InterruptedException{
    file= new File(localPath+localFileName);
    if(fileReceive!=null)
      fileReceive.close();
    fileReceive= new FileOutputStream(file);
    file.createNewFile();
    
    System.out.println("Downloading file...");
    
    //Draw the empty loading buffer
    myWindow.rectangle(LOADING_X, LOADING_Y, (LOADING_WIDTH)/2, (LOADING_HEIGHT)/2);
    
    serverSocket.setSoTimeout(TIMEOUT);
    
    //Need to send the file name to the server
    stringOutStream.println(remoteFileName);
    stringOutStream.flush();
    
    long sizeOfFile= Long.parseLong(stringInStream.readLine());
    long totalBytesRead= 0;
    //Write the byte stream to the file
    int bytesRead= 0;
    while(totalBytesRead<sizeOfFile){
      bytesRead= inStream.read(byteArray);
      fileReceive.write(byteArray,0,bytesRead);
      totalBytesRead+= bytesRead;
      
      //Fill the loading buffer
      myWindow.setPenColour(WindowedGraphics.BLUE);
      myWindow.filledRectangle(LOADING_LEFT+((double)(totalBytesRead)/(double)(sizeOfFile))*(LOADING_RIGHT-LOADING_LEFT)/2, LOADING_Y, (((double)(totalBytesRead)/(double)(sizeOfFile))*(LOADING_RIGHT-LOADING_LEFT))/2, (LOADING_HEIGHT)/2);
    }
    
    serverSocket.setSoTimeout(0);
    
    //Send receipt
    stringOutStream.println(SUCCESS_MSG);
    stringOutStream.flush();
    
    System.out.println("Download complete!");
  }
  
  public static long maxPing() throws IOException, InterruptedException {
	  byte[] dummy= new byte[bufferSize];
	  int maxNumOfAttempts= 3;
	  int numOfAttempts;
  
	  numOfAttempts= 0;
	  
	  stringOutStream.println(PING);
	  stringOutStream.flush();
	  
	  outStream.write(dummy);
	  outStream.flush();
	  
	  double startTime= System.currentTimeMillis();
	  
	  while(!stringInStream.ready() && numOfAttempts<maxNumOfAttempts) {
		  Thread.sleep(SLEEP);
		  numOfAttempts++;
	  }
	  if(numOfAttempts<maxNumOfAttempts)
		  inStream.read(dummy);
	  else
		  startTime= System.currentTimeMillis();
	  
	  return (long)(System.currentTimeMillis()-startTime);
  }
  
  private static void display(int page){
    myWindow.clear();
    
    displayCloudFilesNames(page);
    
    displayUploadButton();
    
    displayPageSelectionUI();
  }
  
  private static void updateCloudFilesNames() throws IOException{
    stringOutStream.println(FILES_REQ);
    stringOutStream.flush();
    for(int i= 0; i<cloudFilesNames.length; i++){
      cloudFilesNames[i]= stringInStream.readLine();
    }
    
    numOfPages= (int)Math.ceil((double)cloudFilesNames.length/(double)MAX_FILES_PER_PAGE);
  }
  private static void displayCloudFilesNames(int page){
    myWindow.setFont(FILES_FONT);
    //Draw the files uploaded
    for(int i= 0; i<cloudFilesNames.length-((pageNo-1)*MAX_FILES_PER_PAGE) && i<MAX_FILES_PER_PAGE; i++){
      myWindow.setPenColour(WindowedGraphics.BLUE);
      myWindow.filledRectangle(FILES_BOX_X,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,(FILES_BOX_WIDTH)/2,(TEXT_HEIGHT)/2);
      myWindow.setPenColour(WindowedGraphics.WHITE);
      myWindow.textLeft(LEFT_FILES,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))]);
    }
  }
  
  private static void displayUploadButton(){
    //Draw the upload button
    myWindow.setPenColour(WindowedGraphics.BLACK);
    myWindow.rectangle(BUTTON_X,BUTTON_Y,(BUTTON_WIDTH)/2,(BUTTON_HEIGHT)/2);
    myWindow.setFont(BUTTON_FONT);
    myWindow.text(BUTTON_X,BUTTON_Y,"Upload");
  }
  
  private static void displayPageSelectionUI() {
	  myWindow.setFont();
	  
	  //Draw the page control management
	  myWindow.setPenColour(WindowedGraphics.BLACK);
	  
	  myWindow.text(width/2, height-100, "Displaying page "+pageNo+" of "+(numOfPages));
	  
	  myWindow.text(PAGE_R_X, PAGE_Y, "NEXT");
	  myWindow.rectangle(PAGE_R_X, PAGE_Y, (PAGE_BUTTON_WIDTH)/2, (PAGE_BUTTON_HEIGHT)/2);
	  
	  myWindow.text(PAGE_L_X, PAGE_Y, "PREV");
	  myWindow.rectangle(PAGE_L_X, PAGE_Y, (PAGE_BUTTON_WIDTH)/2, (PAGE_BUTTON_HEIGHT)/2);
  }
  
  private static void load(){
    //LOADING SCREEN
    myWindow.setPenColour(WindowedGraphics.BLACK);
    myWindow.setFont(MSG_FONT);
    myWindow.textLeft(LEFT_FILES,MSG_Y,"Loading...");
  }
  
  private static void clearMsg(){
    Color prevColour= myWindow.getPenColour();
    myWindow.setPenColour(WindowedGraphics.WHITE);
    
    myWindow.filledRectangle(MSG_X,MSG_Y,(MSG_WIDTH)/2,(MSG_HEIGHT)/2);
    
    myWindow.setPenColour(prevColour);
  }
}
