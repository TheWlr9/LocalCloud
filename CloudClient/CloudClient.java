//Maybe make the waiting times dynamic?
/**
 * @Title Will's cloud
 * @author William Leonardo Ritchie
 * 
 * @version 1.1.8
 */
import java.io.*;
import java.net.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.Dimension;

import graphics.WindowedGraphics;

public class CloudClient{
  final static private int PORT= 42843;
  final static private String ADDRESS= "192.168.1.101";
  final static private String FILE_PATH= "docs"+File.separator;
  final static private int SLEEP= 250;
  
  private static int bufferSize;
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
  
  final private static Font FILES_FONT= new Font(Font.MONOSPACED,Font.PLAIN,width/64);
  final private static int LEFT_FILES= width/10;
  final private static int TEXT_HEIGHT= height/38;
  final private static int FILES_BOX_X= width/3;
  final private static int FILES_BOX_Y= height/4;
  final private static int FILES_BOX_WIDTH= width/2;
  final private static double FILES_BOX_SPACING_MULTIPLIER= 1.5;
  
  final private static Font MSG_FONT= new Font(Font.MONOSPACED, Font.BOLD, width/20);
  final private static int MSG_X= FILES_BOX_X;
  final private static int MSG_Y= height/6;
  final private static int MSG_WIDTH= FILES_BOX_WIDTH;
  final private static int MSG_HEIGHT= height/10;
  
  final private static Font BUTTON_FONT= new Font(Font.MONOSPACED,Font.ITALIC,width/20);
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
  
  public static void main(String[] args){
    myWindow= new WindowedGraphics(width,height);
    fileChooser= new FileDialog(myWindow.getFrame());
    fileChooser.setMultipleMode(false); //To be changed at a later date?
    
    myWindow.setTitle(TITLE);
    
    try{
      serverSocket= new Socket(ADDRESS, PORT);
      
      inStream= serverSocket.getInputStream();
      stringInStream= new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
      
      outStream= serverSocket.getOutputStream();
      stringOutStream= new PrintWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
      
      input= new BufferedReader(new InputStreamReader(System.in));
    }
    catch(IOException e){
      System.err.println("ERROR: CloudClient.main: Error in setting up streams");
      e.printStackTrace();
    }
    
    try{
      //HERE WAS THE NETWORKING STUFF
      stringOutStream.println(serverSocket.getLocalAddress()); //Must send over the IP address first
      stringOutStream.flush();
      
      stringOutStream.println("getBufferSize");
      stringOutStream.flush();
      //Receive the buffer size
      bufferSize= Integer.parseInt(stringInStream.readLine());
      System.out.println("Buffer size: "+bufferSize);
      
      byteArray= new byte[bufferSize];
      
      stringOutStream.println("getNumOfFiles");
      stringOutStream.flush();
      int maxFilesUploaded= Integer.parseInt(stringInStream.readLine());
      System.out.println("FILES: "+maxFilesUploaded);
      cloudFilesNames= new String[maxFilesUploaded];
      
      updateCloudFilesNames();
      
      display();
      
      //Start the main activity "listener"
      while(myWindow.exists()){
        
        if(myWindow.isMousePressed()){
          double mouseX= myWindow.mouseX();
          double mouseY= myWindow.mouseY();
          boolean download= false;
          if(mouseX>(FILES_BOX_X-FILES_BOX_WIDTH/2) && mouseX<(FILES_BOX_X+FILES_BOX_WIDTH/2)){
            for(int i= 0; i<cloudFilesNames.length; i++){
              if(mouseY>(FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER-TEXT_HEIGHT/2) && mouseY<(FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER+TEXT_HEIGHT/2)){
                System.out.println("File chosen: "+cloudFilesNames[i]);
                
                Thread.sleep(200);
                
                fileChooser.setTitle("Choose a location to save the file to");
                fileChooser.setFile(cloudFilesNames[i]);
                fileChooser.setMode(FileDialog.SAVE);
                fileChooser.setVisible(true);
                
                if(fileChooser.getFile()!=null){
                  load();
                  
                  //Send download request to the server
                  stringOutStream.println("downloadFile"); //CONSTANT
                  
                  receiveFile(cloudFilesNames[i], fileChooser.getDirectory(), fileChooser.getFile());
                  
                  download= true;
                  
                  //Get the new number of files
                  stringOutStream.println("getNumOfFiles");
                  stringOutStream.flush();
                  cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
                  updateCloudFilesNames();
                  display();
                  
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
              
              stringOutStream.println("uploadFile"); //CONSTANT
              
              sendFile(fileChooser.getDirectory(), fileChooser.getFile());
              
              Thread.sleep(SLEEP);
              
              //Get the new number of files
              stringOutStream.println("getNumOfFiles");
              stringOutStream.flush();
              cloudFilesNames= new String[Integer.parseInt(stringInStream.readLine())];
              
              updateCloudFilesNames();
              display();
              
              clearMsg();
            }
          }
          
          //Thread.sleep(200); //This is so it only executes the block once per mouse press. (Essentially is mouse clicked.)
        }
      }
      stringOutStream.println("logoff");
      stringOutStream.flush();
    }
    catch(IOException e){
      System.err.println("ERROR: ClientSocket.main: Error in writing to server or reading from file");
      e.printStackTrace();
    }
    catch(NullPointerException e){
      System.err.println("ERROR: ClientSocket: main: Accessing messed up locations");
      e.printStackTrace();
    }
    catch(InterruptedException e){
      System.err.println("ERROR: ClientSocket: main: Interrupt encountered, cannot sleep");
    }
    catch(SecurityException e){
      System.err.println("ERROR: ClientSocket: main: Client attempting to access unauthorized files");
    }
    finally{
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
    
    System.out.println("Upload complete!");
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
    
    System.out.println("Download complete!");
  }
  
  private static void display(){
    myWindow.clear();
    
    displayCloudFilesNames();
    
    displayUploadButton();
  }
  
  private static void updateCloudFilesNames() throws IOException{
    stringOutStream.println("getFiles");
    stringOutStream.flush();
    for(int i= 0; i<cloudFilesNames.length; i++){
      cloudFilesNames[i]= stringInStream.readLine();
    }
  }
  private static void displayCloudFilesNames(){
    myWindow.setFont(FILES_FONT);
    //Draw the files uploaded
    for(int i= 0; i<cloudFilesNames.length; i++){
      myWindow.setPenColour(WindowedGraphics.BLUE);
      myWindow.filledRectangle(FILES_BOX_X,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,(FILES_BOX_WIDTH)/2,(TEXT_HEIGHT)/2);
      myWindow.setPenColour(WindowedGraphics.WHITE);
      myWindow.textLeft(LEFT_FILES,FILES_BOX_Y+i*TEXT_HEIGHT*FILES_BOX_SPACING_MULTIPLIER,cloudFilesNames[i]);
    }
  }
  
  private static void displayUploadButton(){
    //Draw the upload button
    myWindow.setPenColour(WindowedGraphics.BLACK);
    myWindow.rectangle(BUTTON_X,BUTTON_Y,(BUTTON_WIDTH)/2,(BUTTON_HEIGHT)/2);
    myWindow.setFont(BUTTON_FONT);
    myWindow.text(BUTTON_X,BUTTON_Y,"Upload");
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