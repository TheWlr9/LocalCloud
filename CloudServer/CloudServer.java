import java.net.*;
import java.io.*;

/**
 * @author William Ritchie
 * @version 1.2.6/June 21 2018
 */
public class CloudServer
{
    public final static int BUFFER_SIZE= 4096;
    public final static int MAX_FILES_UPLOADED= 5;
    public final static int MAX_FILES_PER_PAGE= 10;
    public final static int SLEEP= 125;
    public final static int TIMEOUT= 3000;

    /**
     * Constructor for objects of class CloudServer
     */
    public CloudServer()
    {
        
    }
    
    public static void main(String[] args){
        if(args.length!=1)
            System.err.println("ERROR. Please specify port number");
        else{
            int port= Integer.parseInt(args[0]);
            
            if(port==0)
                System.err.println("ERROR: main: port read failure");
            else{
                Socket clientSocket= null;
                ServerSocket serverSocket= null;
                try{
                    serverSocket= new ServerSocket(port);
                }
                catch(IOException e){
                    System.err.println("ERROR: main: Error in initializing sockets");
                }
                
                while(true){
                    try{
                        clientSocket= serverSocket.accept();
                        
                        ServerThread serverThread= new ServerThread(clientSocket);
                        serverThread.start();
                    }
                    catch(Exception e){
                        System.err.println("ERROR: main: Error in setting up client connection or starting the server thread for the client socket");
                    }
                }
            }   
        }
    }
}

final class ServerThread extends Thread{
    private Socket s;
    OutputStream outStream;
    InputStream inStream;
    BufferedReader stringInStream;
    PrintWriter stringOutStream;
    byte[] byteArray= new byte[CloudServer.BUFFER_SIZE]; //BUFFER_SIZE should be the same size as the client's BUFFER_SIZE
    boolean close;
    private File file= null;
    FileOutputStream fileOutStream= null;
    FileInputStream fileInStream= null;
    
    private static String FILE_PATH= "docs"+File.separator;
    
    private static File parentDirectory= new File(FILE_PATH);
    
    final private static String ERROR_MSG= "failed";
    final private static String SUCCESS_MSG= "success";
    final private static String PING= "ping";
    final private static String BUF_SIZE_REQ= "getBufferSize";
    final private static String NUM_FILES_REQ= "getNumOfFiles";
    final private static String FILES_REQ= "getFiles";
    final private static String UPLOAD= "uploadFile";
    final private static String DOWNLOAD= "downloadFile";
    final private static String SHUTDOWN= "logoff";
    
    ServerThread(Socket s){
        this.s= s;
    }
    
    public void run(){
        System.out.println("\n***");
        try{
            outStream= s.getOutputStream();
            stringOutStream= new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
            
            inStream= s.getInputStream();
            stringInStream= new BufferedReader(new InputStreamReader(s.getInputStream()));
        }
        catch(IOException e){
            System.err.println("ERROR: ServerThread.run: Failed to create streams");
        }
        try{
            //Must receive connection address FIRST
            System.out.println("Connection made with "+stringInStream.readLine());
            
            close= false;
            String line= null;
            
            while(!close){
                while(line==null)
                    line= stringInStream.readLine();
                line= line.trim();
                
                /**
                 * @return The sent buffer
                 */
                if(line.equals(PING)){
                    byte[] dummy= new byte[CloudServer.BUFFER_SIZE];
                    inStream.read(dummy);
                    outStream.write(dummy);
                    outStream.flush();
                }
                /**
                 * @return The byte array size
                 */
                else if(line.equals(BUF_SIZE_REQ)){
                    System.out.println("Transmitting buffer size");
                    stringOutStream.println(CloudServer.BUFFER_SIZE);
                    stringOutStream.flush();
                }
                /**
                 * @return The number of files uploaded to the cloud
                 */
                else if(line.equals(NUM_FILES_REQ)){
                    stringOutStream.println(numOfFiles());
                    stringOutStream.flush();
                }
                /**
                 * @return A String[] of the file names uploaded to the cloud
                 */
                else if(line.equals(FILES_REQ)){
                    System.out.println("Accessing files\n");
                    
                    String[] filesNames= parentDirectory.list();
                    for(String name : filesNames)
                        stringOutStream.println(name);
                    stringOutStream.flush();
                }
                /**
                 * @param Requires the file name
                 * @param Requires the size of the file being uploaded in bytes
                 * @return A confirmation string once completed
                 *
                 * @throws SocketTimeoutException if there is packet loss.
                 *
                 * Sends a receipt after operation.
                 */
                else if(line.equals(UPLOAD)){
                    file= new File(FILE_PATH+stringInStream.readLine()); //Read in the name of the file
                    
                    file.createNewFile();
                    
                    if(fileOutStream!=null)
                        fileOutStream.close();
                    fileOutStream= new FileOutputStream(file);
                    
                    receiveFile();
                }
                /**
                 * @param Requires the file name
                 * Sends the size of the file in bytes before sending the file.
                 *
                 * Must receive a receipt at the end of the operation.
                 */
                else if(line.equals(DOWNLOAD)){
                    file= new File(FILE_PATH+stringInStream.readLine()); //Read in the name of the file
                    if(fileInStream!=null)
                        fileInStream.close();
                    fileInStream= new FileInputStream(file);
                    
                    System.out.println("File selected: "+file.getName());
                    
                    sendFile();
                }
                else if(line.equals(SHUTDOWN)){
                    close= true;
                    System.out.println("Graceful logoff attempt");
                }
                else{
                    System.out.println("Error: Invalid request:"+line);
                }
                
                line= null;
            }
        }
        catch(NullPointerException e){
            System.out.println("Connection terminated");
        }
        catch(InterruptedException e){
            System.out.println("Error in sleeping");
        }
        catch(SocketTimeoutException ste){
            stringOutStream.println(ERROR_MSG);
            stringOutStream.flush();
            
            System.out.println("Packet loss");
        }
        catch(IOException e){
            System.out.println("ERROR: ServerThread.java: run: Error in setup with client, or reading/writing files");
        }
        finally{
            try{
                System.out.println("\nClosing connection...");
                if(outStream!=null)
                    outStream.close();
                if(inStream!=null){
                    inStream.close();
                    if(stringInStream!=null)
                        stringInStream.close();
                }
                if(fileOutStream!=null)
                    fileOutStream.close();
                if(s!=null)
                    s.close();
                System.out.println("Connection closed");
            }
            catch(IOException e){
                System.err.println("ERROR: ServerThread.run: Error in terminating streams");
            }
            System.out.println("***\n");
        }
    }
    
    private int numOfFiles() throws IOException{
        //Send the buffer size
        String[] files= parentDirectory.list();
        return files.length;
    }
    
    private void sendFile() throws IOException, InterruptedException{
        System.out.println("Sending file...");
        int bytesRead= 0;
        
        //Send the size of the file in bytes
        stringOutStream.println(file.length());
        stringOutStream.flush();
        
        Thread.sleep(CloudServer.SLEEP);
        
        while((bytesRead= fileInStream.read(byteArray))!=-1)
            outStream.write(byteArray,0,bytesRead);
        
        outStream.flush();
        
        if(stringInStream.readLine().equals(SUCCESS_MSG)){
            System.out.println("Success!");
            
            //Delete the file now
            file.delete();
        }
        else
            close= true;
    }
    private void receiveFile() throws SocketTimeoutException, IOException{
        System.out.println("Receiving file...");
        
        long sizeOfFile= Long.parseLong(stringInStream.readLine());
        long totalBytesRead= 0;
        int bytesRead= 0;
        
        s.setSoTimeout(CloudServer.TIMEOUT); //Sets the timeout feature for the socket
        
        while(totalBytesRead<sizeOfFile){
            bytesRead= inStream.read(byteArray);
            fileOutStream.write(byteArray,0,bytesRead);
            totalBytesRead+= bytesRead;
        }
        
        s.setSoTimeout(0); //Disables the timeout feature for the socket
        
        stringOutStream.println(SUCCESS_MSG);
        stringOutStream.flush();
        
        System.out.println("File received!");
    }
}
