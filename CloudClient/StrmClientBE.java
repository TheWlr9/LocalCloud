//Maybe make the waiting times dynamic?
/**
 * @Title Will's cloud
 * @author William Leonardo Ritchie
 * 
 * @version 1.8.2
 * 
 * _1.8.2_
 * 		~Now asks the user if they want to delete the file off of the cloud after
 * 			downloading the file from the cloud.
 * 
 * _1.7.2_
 * 		~Added the timeout feature to the file download as well!
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

import graphics.StrmClientUI;

public class StrmClientBE{
  final static private int PORT= 42843;
  final static private String ADDRESS= "24.79.241.156";
  final static private int SLEEP= 250;
  
  final static private int TIMEOUT= 3000;
  final static private int MAX_FILES_PER_PAGE= 10;
  final static private String SUCCESS_MSG= "success";
  final static private String ERROR_MSG= "failed";
  final static private String PING= "ping";
  final static private String BUF_SIZE_REQ= "getBufferSize";
  final static private String NUM_FILES_REQ= "getNumOfFiles";
  final static private String FILES_REQ= "getFiles";
  final static private String UPLOAD= "uploadFile";
  final static private String DOWNLOAD= "downloadFile";
  final static private String DELETE= "delete";
  final static private String SHUTDOWN= "logoff";
  
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
  
  private static StrmClientUI graphics;
  
  private static boolean escaping;
  
  
  
  //FLAGS
  private static boolean setupError;
  
  public static void main(String[] args){
	  setupError= false;
	  escaping= false;
	  graphics= new StrmClientUI(MAX_FILES_PER_PAGE);
    
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
      
      graphics.popupError("Unable to access Raspberry Pi", "Error");
      
      setupError= true;
    }
    
    try{
    	pageNo= 1;
    	
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
      
      graphics.display(pageNo, numOfPages, cloudFilesNames); //Starting page #
      
      //Start the main activity "listener"
      while(graphics.exists()){
        
        if(graphics.isMousePressed()){
          double mouseX= graphics.mouseX();
          double mouseY= graphics.mouseY();
          boolean download= false;
          boolean upload= false;
          if(mouseX>(StrmClientUI.FILES_BOX_X-StrmClientUI.FILES_BOX_WIDTH/2) && mouseX<(StrmClientUI.FILES_BOX_X+StrmClientUI.FILES_BOX_WIDTH/2)){
            for(int i= 0; i<MAX_FILES_PER_PAGE; i++){
              if(mouseY>(StrmClientUI.FILES_BOX_Y+i*StrmClientUI.TEXT_HEIGHT*StrmClientUI.FILES_BOX_SPACING_MULTIPLIER-StrmClientUI.TEXT_HEIGHT/2) && mouseY<(StrmClientUI.FILES_BOX_Y+i*StrmClientUI.TEXT_HEIGHT*StrmClientUI.FILES_BOX_SPACING_MULTIPLIER+StrmClientUI.TEXT_HEIGHT/2)){
                System.out.println("File chosen: "+cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))]);
                
                Thread.sleep(200);
                
                graphics.saveFilePopup(cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))]);
                String savingName= graphics.getSavingName();
                
                if(savingName!=null){
                  graphics.load();
                  
                  //Send download request to the server
                  stringOutStream.println(DOWNLOAD); //CONSTANT
                  
                  receiveFile(cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))], graphics.getSavingDirectory(), savingName);
                  
                  download= true;
                  
                  //Ask to see whether the user wants to delete the file from the cloud now
                  if(graphics.popupDeleteFileConfirmation())
                	  delete(cloudFilesNames[i+(MAX_FILES_PER_PAGE*(pageNo-1))]);
                  
                  //Get the new number of files
                  stringOutStream.println(NUM_FILES_REQ);
                  stringOutStream.flush();
                  cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
                  updateCloudFilesNames();
                  if(cloudFilesNames.length%MAX_FILES_PER_PAGE==0)
                	  pageNo--;
                  graphics.display(pageNo, numOfPages, cloudFilesNames);
                  
                  graphics.clearMsg();
                }
                
                //Thread.sleep(200);
              }
            }
          }
          if(!download && mouseX>StrmClientUI.BUTTON_X-StrmClientUI.BUTTON_WIDTH/2 && mouseX<StrmClientUI.BUTTON_X+StrmClientUI.BUTTON_WIDTH/2 &&
          mouseY>StrmClientUI.BUTTON_Y-StrmClientUI.BUTTON_HEIGHT/2 && mouseY<StrmClientUI.BUTTON_Y+StrmClientUI.BUTTON_HEIGHT/2){
            Thread.sleep(200);
            
            graphics.uploadFilePopup();
            
            if(graphics.getUploadingName()!=null){
              System.out.println("File chosen: "+graphics.getUploadingDirectory()+graphics.getUploadingName());
              
              graphics.load();
              
              maxPingRecorded= maxPing();
              
              stringOutStream.println(UPLOAD); //CONSTANT
              
              sendFile(graphics.getUploadingDirectory(), graphics.getUploadingName());
              
              if(!escaping) {
            	  upload= true;
            	  
            	  Thread.sleep(SLEEP);
        	  
            	  //Get the new number of files
            	  stringOutStream.println(NUM_FILES_REQ);
            	  stringOutStream.flush();
            	  cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
            	  
            	  updateCloudFilesNames();
            	  graphics.display(pageNo, numOfPages, cloudFilesNames);
            	  
            	  graphics.clearMsg();
              }
            }
          }
          if(!download && !upload && mouseX>StrmClientUI.PAGE_L_X-StrmClientUI.PAGE_BUTTON_WIDTH/2 && mouseX<StrmClientUI.PAGE_R_X+StrmClientUI.PAGE_BUTTON_WIDTH/2
        		  && mouseY>StrmClientUI.PAGE_Y-StrmClientUI.PAGE_BUTTON_HEIGHT/2 && mouseY<StrmClientUI.PAGE_Y+StrmClientUI.PAGE_BUTTON_HEIGHT/2) {
        	  Thread.sleep(200);
        	  
        	  if(mouseX<StrmClientUI.PAGE_L_X+StrmClientUI.PAGE_BUTTON_WIDTH/2) {
        		  //The prev button has been hit
        		  
        		  if(pageNo>1) {
        			  pageNo--;
        			  
        			  graphics.display(pageNo, numOfPages, cloudFilesNames);
        		  }
        	  }
        	  else if(mouseX>StrmClientUI.PAGE_R_X-StrmClientUI.PAGE_BUTTON_WIDTH/2){
        		  //The next button has been hit
        		  
        		  if(pageNo<numOfPages) {
        			  pageNo++;
        			  
        			  graphics.display(pageNo, numOfPages, cloudFilesNames);
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
    		if(e instanceof SocketTimeoutException){ 
    			//Send receipt 
    			stringOutStream.println(ERROR_MSG);
    			stringOutStream.flush(); 
    			System.err.println("ERROR: ClientSocket.main: Packet loss, socket timeout raised"); 
    			e.printStackTrace(); 

    			graphics.popupError("Please try again!", "Error: Packet loss");	
    		} 
    		else if(e instanceof IOException) {
	    		System.err.println("ERROR: ClientSocket.main: Error in writing to server or reading from file");
	    		e.printStackTrace();
	    		
	    		graphics.popupError("Packet loss. Please try again", "Error");
	    	}
	    	else if(e instanceof NullPointerException) {
	    	      System.err.println("ERROR: ClientSocket: main: Accessing messed up locations");
	    	      e.printStackTrace();
	    	      
	    	      graphics.popupError("File does not exist", "Error");
	    	}
	    	else if(e instanceof InterruptedException) {
	    	      System.err.println("ERROR: ClientSocket: main: Interrupt encountered, cannot sleep");
	    	      
	    	      graphics.popupError("Ruched service; failsafe triggered", "Error");
	    	}
	    	else if(e instanceof SecurityException) {
	    	      System.err.println("ERROR: ClientSocket: main: Client attempting to access unauthorized files");
	    	      
	    	      graphics.popupError("You are unauthorized to access those files", "Error");
	    	}
	    	else {
	    		e.printStackTrace();
	    	}
    	}
    }
    finally{
    	if(graphics.exists()) {
    		graphics.close();
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
  
  /*
   * Receives a receipt at the end
   */
  private static void sendFile(String path, String fileName) throws IOException, InterruptedException, SecurityException{
	file= new File(path+fileName);
    if(fileSend!=null)
      fileSend.close();
    fileSend= new FileInputStream(file);
    
    System.out.println("Uploading file...");
    
    //Draw the empty loading buffer
    graphics.setupLoadingBar();
    
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
      
      graphics.updateLoadingBar((double)(totalBytesRead), (double)(file.length()));
    }
    
    outStream.flush();
    
    if(stringInStream.readLine().equals(SUCCESS_MSG))
    	System.out.println("Success!");
    else {
      graphics.popupError("Please try again", "Error: Packet loss");
    	
    	escaping= true;
    	
    	graphics.close();
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
    graphics.setupLoadingBar();
    
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
      graphics.updateLoadingBar((double)(totalBytesRead), (double)(sizeOfFile));
    }
    
    serverSocket.setSoTimeout(0);
    
    //Send receipt
    stringOutStream.println(SUCCESS_MSG);
    stringOutStream.flush();
    
    System.out.println("Download complete!");
  }
  
  public static void delete(String file) {
	  stringOutStream.println(DELETE);
	  stringOutStream.flush();
	  
	  stringOutStream.println(file); //Send the filename over
	  stringOutStream.flush();
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
  
  private static void updateCloudFilesNames() throws IOException{
    stringOutStream.println(FILES_REQ);
    stringOutStream.flush();
    for(int i= 0; i<cloudFilesNames.length; i++){
      cloudFilesNames[i]= stringInStream.readLine();
    }
    
    numOfPages= (int)Math.ceil((double)cloudFilesNames.length/(double)MAX_FILES_PER_PAGE);
  }
  
}