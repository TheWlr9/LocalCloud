import java.net.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.security.Key;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author William Ritchie
 * @version 2.1.0/September 29 2018
 */
public class StrmServer
{
    public final static int PORT= 42843;
    public final static int BUFFER_SIZE= 4096;
    public final static int MAX_FILES_UPLOADED= 5;
    public final static int MAX_FILES_PER_PAGE= 10;
    public final static int SLEEP= 125;
    public final static int TIMEOUT= 3000;
    
    //Key is censored for security reasons. This is not the key in the jar and downloadable tar.gz files.
    public final static String KEY_SEED= "0000000000000000";
    //KEY_SEED.length()==16 || KEY_SEED.length()==24 || KEY_SPEED.length()==32
    //KEY_SEED must equal the same as KEY_SEED on client
    final static Key KEY= new SecretKeySpec(KEY_SEED.getBytes(), "AES");
    
    private static String password= null;
    static String cloudPath= "docs"+File.separator;

    /**
     * Constructor for objects of class StrmServer
     */
    public StrmServer()
    {
        
    }
    
    public static void main(String[] args){
        //THIS IS RUN WHEN THE PROGRAM IS SHUTDOWN
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                try{
                    BufferedReader reader= new BufferedReader(new FileReader(new File("etc"+File.separator+"log.cfg")));
                    String PID= reader.readLine();
                    String path= reader.readLine(); //Get the path (I know it's ugly code...)
                    
                    reader.close();
                    
                    PrintWriter writer= new PrintWriter(new FileWriter(new File("etc"+File.separator+"log.cfg")));
                    writer.write("0"+"\n"+path);
                    writer.flush();
                    
                    writer.close();
                }
                catch(IOException ioe){
                    System.err.println("Error in resetting PID");
                }
            }
        });
        
        //Log the PID
        try{
            BufferedReader reader= new BufferedReader(new FileReader(new File("etc"+File.separator+"log.cfg")));
            String PID= reader.readLine();
            String path= reader.readLine(); //I know this is ugly code...
            
            reader.close();
            
            PrintWriter writer= new PrintWriter(new FileWriter(new File("etc"+File.separator+"log.cfg")));
            long pid= Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().substring(0,ManagementFactory.getRuntimeMXBean().getName().indexOf("@")));
            
            writer.write(String.valueOf(pid)+"\n"+path);
            writer.flush();
            
            writer.close();
        }
        catch(IOException e){
            System.err.println("Warning: Cannot find log file to place PID");
            System.err.println("Please make sure that you have control of when to stop the server");
        }
        
        if(args.length>1){
            password= args[0];
            cloudPath= args[1];
        }
        else if(args.length==1)
            password= args[0];
        else
            System.err.println("Please specify your password as a parameter!");
        
        Socket clientSocket= null;
        ServerSocket serverSocket= null;
        try{
            serverSocket= new ServerSocket(PORT);
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
    
    public static String getPassword(){
        return password;
    }
}

final class ServerThread extends Thread{
    private Socket s;
    OutputStream outStream;
    InputStream inStream;
    BufferedReader stringInStream;
    PrintWriter stringOutStream;
    
    //Cryptography
    private Cipher encryptCipher= null;
    private Cipher decryptCipher= null;
    
    byte[] byteArrayReceive= new byte[StrmServer.BUFFER_SIZE+16]; //BUFFER_SIZE should be the same size as the client's BUFFER_SIZE, plus 16 due to block padding
    byte[] byteArraySend= new byte[StrmServer.BUFFER_SIZE]; //BUFFER_SIZE should be the same as the client one
    boolean close;
    private File file= null;
    FileOutputStream fileOutStream= null;
    FileInputStream fileInStream= null;
    
    private static String filePath= StrmServer.cloudPath;//"docs"+File.separator;
    
    private static File parentDirectory= new File(filePath);
    //private static String password= "mudkip22";
    
    final private static String ERROR_MSG= "failed";
    final private static String SUCCESS_MSG= "success";
    final private static String PING= "ping";
    final private static String BUF_SIZE_REQ= "getBufferSize";
    final private static String NUM_FILES_REQ= "getNumOfFiles";
    final private static String FILES_REQ= "getFiles";
    final private static String UPLOAD= "uploadFile";
    final private static String DOWNLOAD= "downloadFile";
    final private static String DELETE= "delete";
    final private static String SHUTDOWN= "logoff";
    
    ServerThread(Socket s){
        this.s= s;
        
        this.s.setPerformancePreferences(0,0,1); //Prioritizes bandwidth
    }
    
    public void run(){
        System.out.println("\n***");
        try{
            outStream= s.getOutputStream();
            stringOutStream= new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
            encryptCipher= Cipher.getInstance("AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE,StrmServer.KEY);
            
            inStream= s.getInputStream();
            stringInStream= new BufferedReader(new InputStreamReader(s.getInputStream()));
            decryptCipher= Cipher.getInstance("AES");
            decryptCipher.init(Cipher.DECRYPT_MODE,StrmServer.KEY);
        }
        catch(Exception e){
            System.err.println("ERROR: ServerThread.run: Failed to create streams");
        }
        try{
            //Must receive connection address FIRST
            System.out.println("Connection made with "+stringInStream.readLine());
            
            /*
             * Password checking
             */
            boolean prematureClose= false;
            String inputtedPwd= stringInStream.readLine();
            while(!StrmServer.getPassword().equals(inputtedPwd) && !inputtedPwd.equals(SHUTDOWN)){
                stringOutStream.println(ERROR_MSG);
                stringOutStream.flush();
                
                inputtedPwd= stringInStream.readLine();
            }
            if(inputtedPwd.equals(SHUTDOWN)){
                //Report the invalid password occurence
                stringOutStream.println(ERROR_MSG);
                stringOutStream.flush();
                
                stringOutStream.close();
                outStream.close();
                
                stringInStream.close();
                inStream.close();
                
                s.close();
                
                prematureClose= true;
            }
            if(!prematureClose){
                stringOutStream.println(SUCCESS_MSG);
                stringOutStream.flush();
            }
            
            close= false;
            String line= null;
            
            if(prematureClose)
                close= true;
            
            while(!close){
                while(line==null)
                    line= stringInStream.readLine();
                line= line.trim();
                
                /**
                 * @return The sent buffer
                 */
                if(line.equals(PING)){
                    byte[] dummy= new byte[StrmServer.BUFFER_SIZE];
                    inStream.read(dummy);
                    outStream.write(dummy);
                    outStream.flush();
                }
                /**
                 * @return The byte array size
                 */
                else if(line.equals(BUF_SIZE_REQ)){
                    System.out.println("Transmitting buffer size");
                    stringOutStream.println(StrmServer.BUFFER_SIZE);
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
                 */
                else if(line.equals(UPLOAD)){
                    download();
                }
                /**
                 * @param Requires the file name
                 * Sends the size of the file in bytes before sending the file.
                 */
                else if(line.equals(DOWNLOAD)){
                    file= new File(filePath+stringInStream.readLine()); //Read in the name of the file
                    if(fileInStream!=null)
                        fileInStream.close();
                    fileInStream= new FileInputStream(file);
                    
                    System.out.println("File selected: "+file.getName());
                    
                    sendFile();
                }
                /**
                 * @param Filename The name of the file to be deleted
                 * Deletes the file from this database
                 */
                else if(line.equals(DELETE)){
                    file= new File(filePath+stringInStream.readLine()); //Read in file name parameter
                    if(file!=null)
                        file.delete(); //Delete the file
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
            e.printStackTrace();
        }
        finally{
            try{
                System.out.println("\nClosing connection...");
                if(outStream!=null){
                    outStream.close();
                    if(stringOutStream!=null)
                        stringOutStream.close();
                }
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
    
    private void download() throws IOException{
        try{
            file= new File(filePath+stringInStream.readLine()); //Read in the name of the file
            
            file.createNewFile();
            
            if(fileOutStream!=null)
                fileOutStream.close();
            fileOutStream= new FileOutputStream(file);
        
            receiveFile();
        }
        catch(SocketTimeoutException ste){
            System.out.println("Error, packet loss.");
            
            s.setSoTimeout(0); //Disables the timeout
            
            stringOutStream.println(ERROR_MSG); //Send the result back to the client
            stringOutStream.flush();
            
            if(fileOutStream!=null)
                fileOutStream.close();
            file.delete();
        }
    }
    
    private void sendFile() throws IOException, InterruptedException{
        System.out.println("Sending file...");
        int bytesRead= 0;
        boolean failed= false;
        byte[] cipherText= null;
        
        //Send the size of the file in bytes
        stringOutStream.println(file.length());
        stringOutStream.flush();
        
        Thread.sleep(StrmServer.SLEEP);
        
        while((bytesRead= fileInStream.read(byteArraySend))!=-1){
            try{
                cipherText= encryptCipher.doFinal(byteArraySend,0,bytesRead);
            }
            catch(Exception e){
                System.err.println("Error in encrypting file");
                e.printStackTrace();
            }
            
            //Now write the encrypted data to the client
            outStream.write(cipherText);
            
            if(!stringInStream.readLine().equals(SUCCESS_MSG)){
                failed= true;
                break;
            }
        }
        
        outStream.flush();
        
        if(!failed)
            System.out.println("Success!");
        else
            System.out.println("FAILURE");
    }
    private void receiveFile() throws SocketTimeoutException, IOException{
        System.out.println("Receiving file...");
        
        long sizeOfFile= Long.parseLong(stringInStream.readLine());
        long totalBytesRead= 0;
        int bytesRead= 0;
        int bufferBytesRead= 0;
        byte[] plainText= null;
        
        s.setSoTimeout(StrmServer.TIMEOUT); //Sets the timeout feature for the socket
        
        while(totalBytesRead<sizeOfFile){
            //Used to be inStream.read(byteArrayReceive)
            bytesRead= 0;
            
            //Read into inStream (InputStream) the encrypted buffer
            while(encryptCipher.getOutputSize((int)(sizeOfFile-totalBytesRead))!=bytesRead && (bufferBytesRead= inStream.read(byteArrayReceive,bytesRead,byteArrayReceive.length-bytesRead))>0)
                bytesRead+= bufferBytesRead; //All of this is just to read in the correct amount of bytes
            
            try{
                //Store the plaintext into plainText (byte[])
                plainText= decryptCipher.doFinal(byteArrayReceive,0,bytesRead);
            }
            catch(Exception e){
                System.err.println("Error in decrypting");
                e.printStackTrace();
            }
            
            //Write the plaintext to the file
            fileOutStream.write(plainText);
            totalBytesRead+= plainText.length;
            
            if(totalBytesRead%StrmServer.BUFFER_SIZE==0 || totalBytesRead>=sizeOfFile){
                //Send the success message
                stringOutStream.println(SUCCESS_MSG);
                stringOutStream.flush();
            }
        }
        
        s.setSoTimeout(0); //Disables the timeout feature for the socket
        
        System.out.println("File received!");
    }
}
