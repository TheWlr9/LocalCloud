//Maybe make the waiting times dynamic?
/**
 * @Title Will's cloud
 * @author William Leonardo Ritchie
 * 
 * @version 2.3.5
 * 
 * _2.3.5_
 * 		~Added a refresh button! (FINALLY...)
 * 		~Fixed security issue
 * 
 * _2.2.4_
 * 		~Added strm.out output
 * 
 * _2.2.3_
 * 		~Changed the layout of the code and tidied it up a little...
 * 		~Added static window size!
 * 
 * _2.2.2_
 * 		~Added ENCRYPTION FINALLY
 * 
 * _2.1.2_
 * 		~SEVERLY REDUCED CPU USAGE
 * 
 * _2.1.1_
 * 		~Fixed bug with improperly terminating the program during after typing and then 
 * 			exiting the window on the password or address screen
 * 		~Added some more graphics (background + cloud overlay)
 * 
 * _2.0.0_
 * 		~~~BETA!!!~~~
 * 		~Added password window
 * 		~Added cloud IP address selector window
 * 		~Added memory for cloud IP address
 * 		~Jar files!
 * 		~Added "back" option in password screen
 * 		~Made it possible to enter IPv6 addresses
 * 
 * _1.9.2_
 * 		~Highly optimized file transfer! Reduced risk of losing data! A safer journey!
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
 * 		~Changed the IOException ERROR pop-up message to better explain the error to the user.
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
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import graphics.StrmClientUI;

public class StrmClientBE{
  final static private String DEBUG= "-d";
  final static public String OUT_FILE_PATH= "etc"+File.separator+"strm.out";
  
  final static private int PORT= 42843;
  final static private int SLEEP= 250;
  
  final static private String KEY_SEED= "****************";
  //KEY_SEED.length()==16 || KEY_SEED.length()==24 || KEY_SEED.length()==32
  //KEY_SEED must equal the same as KEY_SEED on server
  final static private Key key= new SecretKeySpec(KEY_SEED.getBytes(), "AES") ;
  
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
  //Now cryptography
  private static Cipher encryptCipher= null;
  private static Cipher decryptCipher= null;
  
  private static String address;
  private static String password;
  private static byte[] byteArraySend;
  private static byte[] byteArrayReceive;
  private static String[] cloudFilesNames; //All of the files available
  private static String[] pageFilesNames; //The files currently displayed on screen
  private static String[] tenderInfo; //This is used to store the IP address and the password
  
  private static File file= null;
  private static FileInputStream fileSend= null;
  private static FileOutputStream fileReceive= null;
  
  private static File configFile= null;
  
  private static PrintStream strmOut= null;
  
  private static StrmClientUI graphics;
  
  private static boolean debug;
  private static boolean escaping;
  
  
  
  
  public static void main(String[] args){    
    if(args.length>=1 && args[0].equals(DEBUG)) 
      debug= true;
    else
      debug= false;
      
    	  escaping= false;
	  graphics= new StrmClientUI(MAX_FILES_PER_PAGE);
	  tenderInfo= new String[2];
	  
	  try {
	    encryptCipher= Cipher.getInstance("AES"); //Must be constant for security reasons
	    decryptCipher= Cipher.getInstance("AES"); //Must be constant for security reasons
	    encryptCipher.init(Cipher.ENCRYPT_MODE, key);
	    decryptCipher.init(Cipher.DECRYPT_MODE, key);
	    
	    openSettings();
	  } catch (InterruptedException e1) {
	    System.err.println("Got interrupted right at the beginning while setting up maybe?");
	    e1.printStackTrace();
	  } catch (NoSuchAlgorithmException e) {
	    System.err.println("Encryption algorithm DNE");
	    e.printStackTrace();
	  } catch (NoSuchPaddingException e) {
	    e.printStackTrace();
	  } catch (InvalidKeyException e) {
	    System.err.println("Wrong type of key");
	    e.printStackTrace();
	  }
	  
	  try{
	    pageNo= 1;
	    
      //HERE WAS THE NETWORKING STUFF
      
      
      startService();
      
      //Start the main activity "listener"
      while(graphics.exists()){
	Thread.sleep(StrmClientUI.PASSIVE_TIMEOUT); //Wait a bit so you give the CPU a bit of a rest
        
        if(graphics.isMousePressed()){
          double mouseX= graphics.mouseX();
          double mouseY= graphics.mouseY();
          if(mouseX>StrmClientUI.REFRESH_BUTTON_X-StrmClientUI.REFRESH_BUTTON_WIDTH/2 && mouseX<StrmClientUI.REFRESH_BUTTON_X+StrmClientUI.REFRESH_BUTTON_WIDTH/2
		  && mouseY>StrmClientUI.REFRESH_BUTTON_Y-StrmClientUI.REFRESH_BUTTON_HEIGHT/2 && mouseY<StrmClientUI.REFRESH_BUTTON_Y+StrmClientUI.REFRESH_BUTTON_HEIGHT/2) {
            updateCloudFilesNames();
            if(pageNo>numOfPages)
              pageNo--;
            graphics.display(pageNo, numOfPages, pageFilesNames);
          }
          else if(mouseX>StrmClientUI.SETTINGS_BUTTON_X-StrmClientUI.SETTINGS_BUTTON_WIDTH/2 && mouseX<StrmClientUI.SETTINGS_BUTTON_X+StrmClientUI.SETTINGS_BUTTON_WIDTH/2
		  && mouseY>StrmClientUI.SETTINGS_BUTTON_Y-StrmClientUI.SETTINGS_BUTTON_HEIGHT/2 && mouseY<StrmClientUI.SETTINGS_BUTTON_Y+StrmClientUI.SETTINGS_BUTTON_HEIGHT/2) {
            stringOutStream.println(SHUTDOWN);
            stringOutStream.flush();
            
            //Reset the configuration IP address file
            PrintWriter writer= new PrintWriter(new FileWriter(configFile));
            writer.write("0.0.0.0");
            writer.flush();
            writer.close();
            
            tenderInfo[0]= null;
            tenderInfo[1]= null;
            
            openSettings();
            
            startService();
          }
          else if(mouseX>StrmClientUI.PAGE_L_X-StrmClientUI.PAGE_BUTTON_WIDTH/2 && mouseX<StrmClientUI.PAGE_R_X+StrmClientUI.PAGE_BUTTON_WIDTH/2
		  && mouseY>StrmClientUI.PAGE_Y-StrmClientUI.PAGE_BUTTON_HEIGHT/2 && mouseY<StrmClientUI.PAGE_Y+StrmClientUI.PAGE_BUTTON_HEIGHT/2) {
            Thread.sleep(200);
            
            if(mouseX<StrmClientUI.PAGE_L_X+StrmClientUI.PAGE_BUTTON_WIDTH/2) {
              //The prev button has been hit
              
              if(pageNo>1) {
        	pageNo--;
        	
        	updatePageFilesNames();
        	
        	graphics.display(pageNo, numOfPages, pageFilesNames);
              }
            }
            else if(mouseX>StrmClientUI.PAGE_R_X-StrmClientUI.PAGE_BUTTON_WIDTH/2){
              //The next button has been hit
              
              if(pageNo<numOfPages) {
        	pageNo++;
        	
        	updatePageFilesNames();
        	
        	graphics.display(pageNo, numOfPages, pageFilesNames);
              }
            }
          }
          else if(mouseX>(StrmClientUI.FILES_BOX_X-StrmClientUI.FILES_BOX_WIDTH/2) && mouseX<(StrmClientUI.FILES_BOX_X+StrmClientUI.FILES_BOX_WIDTH/2)){
            for(int i= 0; i<pageFilesNames.length; i++){
              //System.out.println(i);
              if(mouseY>(StrmClientUI.FILES_BOX_Y+i*StrmClientUI.TEXT_HEIGHT*StrmClientUI.FILES_BOX_SPACING_MULTIPLIER-StrmClientUI.TEXT_HEIGHT/2) && mouseY<(StrmClientUI.FILES_BOX_Y+i*StrmClientUI.TEXT_HEIGHT*StrmClientUI.FILES_BOX_SPACING_MULTIPLIER+StrmClientUI.TEXT_HEIGHT/2)){
                System.out.println("File chosen: "+pageFilesNames[i]);
                
                Thread.sleep(200);
                
                graphics.saveFilePopup(pageFilesNames[i]);
                String savingName= graphics.getSavingName();
                String fileToSave= pageFilesNames[i];
                String savingDirectory= graphics.getSavingDirectory();
                
                if(savingName!=null){
                  if(download(fileToSave, savingDirectory, savingName)) {
                    
                    //Ask to see whether the user wants to delete the file from the cloud now
                    if(graphics.popupDeleteFileConfirmation())
                  	  delete(pageFilesNames[i]);
                    
                    //Get the new number of files
                    stringOutStream.println(NUM_FILES_REQ);
                    stringOutStream.flush();
                    cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
                    updateCloudFilesNames();
                    if((pageNo-1)*MAX_FILES_PER_PAGE>=cloudFilesNames.length)
                  	  pageNo--;
                    graphics.display(pageNo, numOfPages, pageFilesNames);
                  }
                  graphics.clearLoading();
                }
                
                break;
              }
            }
          }
          else if(mouseX>StrmClientUI.UPLOAD_BUTTON_X-StrmClientUI.UPLOAD_BUTTON_WIDTH/2 && mouseX<StrmClientUI.UPLOAD_BUTTON_X+StrmClientUI.UPLOAD_BUTTON_WIDTH/2 &&
          mouseY>StrmClientUI.UPLOAD_BUTTON_Y-StrmClientUI.UPLOAD_BUTTON_HEIGHT/2 && mouseY<StrmClientUI.UPLOAD_BUTTON_Y+StrmClientUI.UPLOAD_BUTTON_HEIGHT/2){
            Thread.sleep(200);
            
            graphics.uploadFilePopup();
            
            if(graphics.getUploadingName()!=null){
              System.out.println("File chosen: "+graphics.getUploadingDirectory()+graphics.getUploadingName());
              
              upload(graphics.getUploadingDirectory(), graphics.getUploadingName());
              
              if(!escaping) {
            	  Thread.sleep(SLEEP);
        	  
            	  //Get the new number of files
            	  stringOutStream.println(NUM_FILES_REQ);
            	  stringOutStream.flush();
            	  cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
            	  
            	  updateCloudFilesNames();
            	  if(pageNo<=0)
            	    pageNo= 1;
            	  if(numOfPages<=0)
            	    numOfPages= 1;
            	  graphics.display(pageNo, numOfPages, pageFilesNames);
            	  
            	  graphics.clearLoading();
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
    finally{
    	if(graphics.exists()) {
    		graphics.close();
    	}
    	
    	if(stringOutStream!=null)
    	  stringOutStream.close();
    	
    	disconnect();
    }
  }
  
  
  private static void upload(String path, String filename) throws IOException, InterruptedException {
    graphics.load();
    
    maxPingRecorded= maxPing();
    
    stringOutStream.println(UPLOAD); //CONSTANT
    stringOutStream.flush();
    
    sendFile(path, filename);
    
    fileSend.close();
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
    
    //Need to send the file name to the server
    stringOutStream.println(fileName);
    stringOutStream.flush();
    
    //Send the size of the file in bytes to the server
    stringOutStream.println(file.length());
    stringOutStream.flush();
    
    Thread.sleep(SLEEP);
    
    if(sendEncryptedBytes()) //This calls the method to send the file
    	System.out.println("Success!");
    else {
      System.err.println("Error, packet loss.");
      
      if(graphics.popupPacketLossRetry())
	upload(path, fileName);
    }
    
    //Maybe delete the file now?
  }
  private static void receiveFile(String remoteFileName, String localPath, String localFileName) throws IOException, SecurityException, InterruptedException, SocketTimeoutException{
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
    
    receiveDecryptedBytes(); //This receives the encrypted bytes from the connection
  }
  private static boolean download(String remoteFileName, String localPath, String localFileName) throws SecurityException, IOException, InterruptedException {
    graphics.load();
    
    //Send download request to the server
    stringOutStream.println(DOWNLOAD); //CONSTANT
    
    try {
      receiveFile(remoteFileName, localPath, localFileName);
      
      fileReceive.close(); //Close the file stream
      
      return true; //Return successful
    }
    catch(SocketTimeoutException ste) {
      System.err.println("Error, packet loss.");
      
      serverSocket.setSoTimeout(0); //Reset the socket timeout
      
      stringOutStream.println(ERROR_MSG);
      stringOutStream.flush();
      
      if(fileReceive!=null)
	fileReceive.close();
      file.delete();
      
      if(graphics.popupPacketLossRetry()) {
	graphics.clearLoading();
	return download(remoteFileName, localPath, localFileName);
      }
      else
	return false; //Return unsuccessful
    }
  }
  
  /**
   * This method sends the bytes stored in byteArraySend, encrypted over the connection.
   * @return Whether or not the method was successful
   * @throws IOException If there is a problem with reading the file.
   */
  private static boolean sendEncryptedBytes() throws IOException {
    int bytesRead;
    int totalBytesRead= 0;
    byte[] cipherText= null;
  //Write the byte stream to the server
    while((bytesRead= fileSend.read(byteArraySend))!=-1) {
      try {
	cipherText= encryptCipher.doFinal(byteArraySend,0,bytesRead);
      } catch (Exception e) {
	System.err.println("Error in encrypting the file");
	e.printStackTrace();
      }
      
      //Write the ciphertext to outStream (OutputStream)
      outStream.write(cipherText);
      //outStream.write(byteArray,0,bytesRead);
      
      if(!stringInStream.readLine().equals(SUCCESS_MSG))
	return false;
      totalBytesRead+= bytesRead;
      
      graphics.updateLoadingBar((double)(totalBytesRead), (double)(file.length()));
    }
    
    outStream.flush();
    
    return true;
  }
  /**
   * This method writes the decrypted byteArrayReceive array, into fileReceive, into file.
   * @throws NumberFormatException When the size of the file is larger than signed int MAX SIZE
   * @throws IOException When the file no longer exists for some odd reason.
   */
  private static void receiveDecryptedBytes() throws NumberFormatException, IOException {
    long sizeOfFile= Long.parseLong(stringInStream.readLine());
    long totalBytesRead= 0;
    //Write the byte stream to the file
    int bytesRead= 0;
    int bufferBytesRead= 0;
    byte[] plainText= null;
    
    while(totalBytesRead<sizeOfFile){
      //Used to be inStream.read(byteArray)
      
      bytesRead= 0;
      
      while(encryptCipher.getOutputSize((int)(sizeOfFile-totalBytesRead))!=bytesRead && (bufferBytesRead= inStream.read(byteArrayReceive,  bytesRead,  byteArrayReceive.length-bytesRead))>0)
	bytesRead+= bufferBytesRead;
      
      try {
	//Store the plaintext into plainText (byte[])
	plainText= decryptCipher.doFinal(byteArrayReceive,0,bytesRead);
      }
      catch(Exception e) {
	System.err.println("Error in decrypting");
	e.printStackTrace();
      }
      
      //Write the plaintext to the file
      fileReceive.write(plainText);
      totalBytesRead+= plainText.length;
      
      if(totalBytesRead%bufferSize==0 || totalBytesRead==sizeOfFile) {
	//Send the success message
	stringOutStream.println(SUCCESS_MSG);
	stringOutStream.flush();
      }
      
      //Fill the loading buffer
      graphics.updateLoadingBar((double)(totalBytesRead), (double)(sizeOfFile));
    }
    
    serverSocket.setSoTimeout(0);
    
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
   
    updatePageFilesNames();
  }
  private static void updatePageFilesNames() {
    pageFilesNames= new String[Math.min(MAX_FILES_PER_PAGE, cloudFilesNames.length-((pageNo-1)*MAX_FILES_PER_PAGE))];
    short j= 0;
    for(int i= (pageNo-1)*MAX_FILES_PER_PAGE; i<cloudFilesNames.length && i<MAX_FILES_PER_PAGE*pageNo; i++){ //The second condition is: i<(pageNo-1)*MAX_FILES_PER_PAGE+MAX_FILES_PER_PAGE
      pageFilesNames[j]= cloudFilesNames[i]; //Deep copy
      
      j++; //Increment the second pointer in the page of files array
    }
  }
  
  private static void startService() throws NumberFormatException, IOException  {
    stringOutStream.println(BUF_SIZE_REQ);
    stringOutStream.flush();
    
    //Receive the buffer size
    bufferSize= Integer.parseInt(stringInStream.readLine());
    System.out.println("Buffer size: "+bufferSize);
    
    byteArraySend= new byte[bufferSize];
    byteArrayReceive= new byte[bufferSize+16];
    
    stringOutStream.println(NUM_FILES_REQ);
    stringOutStream.flush();
    int maxFilesUploaded= Integer.parseInt(stringInStream.readLine());
    System.out.println("FILES: "+maxFilesUploaded);
    cloudFilesNames= new String[maxFilesUploaded];
    
    updateCloudFilesNames();
    
    //A check, in case the # of files has changed since last service initiation and pageNo!=1
    if((pageNo-1)*MAX_FILES_PER_PAGE>=cloudFilesNames.length)
      pageNo--;
    
    graphics.display(pageNo, numOfPages, pageFilesNames); //Starting page #
  }
  
  private static void openSettings() throws InterruptedException {
    disconnect();
    
    BufferedReader reader= null;
    PrintWriter writer= null;
    configFile= new File("settings.cfg");
    try {
      reader= new BufferedReader(new FileReader(configFile));
    }
    catch(FileNotFoundException e) {
      //UNREACHABLE
      e.printStackTrace();
    }
    
    try {
      String result= reader.readLine();
      reader.close();
      
      if(!result.equals("0.0.0.0"))
	tenderInfo[0]= result;
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    if(tenderInfo[0]==null)
      graphics.addressScreen(tenderInfo); //Set the IP address
    if(!tenderInfo[0].isEmpty()) { //A check to see whether the window has closed
      //No need to check if password is null, it will ALWAYS be null at this point
      graphics.passwordScreen(tenderInfo); //Set the password
      if(!tenderInfo[1].isEmpty()) { //A check to see whether the window has closed
	try {
	  address= tenderInfo[0];
	  password= tenderInfo[1];
	  
	  connect();
	  writer= new PrintWriter(new FileWriter(configFile));
	  writer.write(tenderInfo[0]);
	  writer.flush();
	  
	  writer.close();
	}
	catch(Exception e) {
	  if(e instanceof IOException || e instanceof SocketException) {
	    graphics.popupError("Unable to access Server", "Error");
	    
	    //Reset the IP address and the password
	    tenderInfo[0]= null;
	    tenderInfo[1]= null;
	    
	    try {
	      writer= new PrintWriter(new FileWriter(configFile));
	      writer.write("0.0.0.0");
	      writer.flush();
	      writer.close();
	    } catch (IOException e1) {
	      e1.printStackTrace();
	    }
	    
	    
	    openSettings();
	  }
	  else if(e instanceof PasswordException){
	    graphics.popupError("Invalid password", "Strm");
	    
	    //Reset just the password
	    tenderInfo[1]= null;
	    
	    openSettings();
	  }
	  else {
	    e.printStackTrace();
	  }
	  //Otherwise... UNRECOGNIZED EXCEPTION!!
	}
      }
      else { //The window has closed
	System.exit(0);
      }
    }
    else { //The window has closed
      System.exit(0);
    }
  }
  
  private static void connect() throws IOException, PasswordException{
    if(!debug) {
      try {
	strmOut= new PrintStream(new FileOutputStream(OUT_FILE_PATH), true);
	System.setOut(strmOut);
      } catch (FileNotFoundException e) {
	System.err.println("Output file not found; using default System.out");
      }
    }
    
    if(password.toLowerCase().equals("back"))
      throw new IOException();
    
    //ADDRESS STUFF
    serverSocket= new Socket();
    serverSocket.connect(new InetSocketAddress(address, PORT), TIMEOUT+2000); //5 seconds
    
    serverSocket.setPerformancePreferences(0, 0, 1); //Prioritizes bandwidth
    serverSocket.setTrafficClass(0x18); //Prioritizes high throughput and low delay
    
    inStream= serverSocket.getInputStream();
    stringInStream= new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
    
    outStream= serverSocket.getOutputStream();
    stringOutStream= new PrintWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
    
    input= new BufferedReader(new InputStreamReader(System.in));
    
    stringOutStream.println(serverSocket.getLocalAddress()); //Must send over the IP address first
    stringOutStream.flush();
    
    
    //PASSWORD STUFF
    stringOutStream.println(password);
    stringOutStream.flush();
    if(!stringInStream.readLine().equals(SUCCESS_MSG))
      throw new PasswordException("Invalid password");
  }
  
  private static void disconnect() {
    try{
      if(strmOut!=null)
	strmOut.close();
      
      if(stringInStream!=null)
	stringInStream.close();
      if(inStream!=null)
        inStream.close();
      
      if(stringOutStream!=null) {
	stringOutStream.println(SHUTDOWN);
	stringOutStream.flush();
	stringOutStream.close();
      }
      if(outStream!=null)
	outStream.close();
      
      if(serverSocket!=null) {
	System.out.println("\nClosing connection...");
        serverSocket.close();
        System.out.println("Closed connection.");
      }
      
      if(fileSend!=null)
        fileSend.close();
      
      if(fileReceive!=null)
        fileReceive.close();
      
      if(input!=null)
        input.close();
    }
    catch(IOException e){
      System.err.println("ERROR: ClientCloud.main: Error in terminating streams");
    }
  }
  
}