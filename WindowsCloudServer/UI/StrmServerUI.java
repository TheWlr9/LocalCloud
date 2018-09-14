
/**
 * This is the UI class for Strm Server
 * 
 * @author Will Ritchie
 * @version 1.0.2
 */

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.awt.Font;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.*;

import graphics.*;

public class StrmServerUI
{
    private static WindowedGraphics ui;
    private static Process serverProcess;
    
    private static int serverPid;
    private static String ipAddress;
    
    final private static String LOG_FILE= "etc"+File.separator+"log.cfg";
    private static String cloudPath= "docs"+File.separator;
    
    final private static int PASSIVE_TIMEOUT= 50; //The timeout time for waiting for the next mousepress, keystroke, etc...
    
    final public static int WIDTH= 500;
    final public static int HEIGHT= 500;
    final private static String TITLE= "Strm Server Control Panel beta-1.0.2";
    final private static String ICON_NAME= "graphics"+File.separator+"cloud-icon.png";
    
    final private static Font MAIN_MSG_FONT= new Font("Arial Black",Font.BOLD, WIDTH/25);
    
    final private static Font BUTTON_OFF_FONT= new Font("Arial Black", Font.ITALIC, WIDTH/20);
    final private static Font BUTTON_ON_FONT= new Font("Arial Black", Font.ITALIC, WIDTH/20);
    final public static int BUTTON_X= WIDTH/2;
    final public static int BUTTON_Y= 5*HEIGHT/6;
    final public static int BUTTON_WIDTH= WIDTH/5;
    final public static int BUTTON_HEIGHT= HEIGHT/8;
    
    final private static Font IV6P_ADDRESS_FONT= new Font ("Arial Black", Font.BOLD, WIDTH/24);
    final private static Font IV4P_ADDRESS_FONT= new Font("Arial Black", Font.BOLD, WIDTH/12);
    final public static int ADDRESS_X= WIDTH/2; //CHANGE
    final public static int ADDRESS_Y= HEIGHT/2; //CHANGE
    
    public static void main(String[] args){
        //Setup
        ui= new WindowedGraphics(WIDTH,HEIGHT);
        ui.setTitle(TITLE);
        ImageIcon icon= new ImageIcon(ICON_NAME);
        ui.getFrame().setIconImage(icon.getImage());
        
        ipAddress= getIP();
        
        File infoFile= new File(LOG_FILE);
        BufferedReader reader= null;
        PrintWriter writer= null;
        
        try{
            reader= new BufferedReader(new FileReader(infoFile));
        }
        catch(FileNotFoundException e){
            //Should be unreachable
            e.printStackTrace();
        }
        
        try{
            serverPid= Integer.parseInt(reader.readLine());
            
            cloudPath= reader.readLine();
            
            reader.close();
        }
        catch(IOException e){
            e.printStackTrace();
            
            serverPid= 0;
        }
        
        //Start drawing some things
        if(serverPid!=0){
            drawOn();
        }
        else{
            drawOff();
        }
        
        //Start the listener code:
        while(ui.exists()){
            try{
                Thread.sleep(PASSIVE_TIMEOUT);
            }
            catch(InterruptedException e){
                System.err.println("Error in sleeping");
                e.printStackTrace();
            }
            
            if(ui.isMousePressed()){
                double mouseX= ui.mouseX();
                double mouseY= ui.mouseY();
                
                try{
                    Thread.sleep(200); //This is for limiting the UI from registering ghost clicks
                }
                catch(InterruptedException e){
                    e.printStackTrace();
                }
                
                if(mouseX<BUTTON_X+BUTTON_WIDTH/2 && mouseX>BUTTON_X-BUTTON_WIDTH/2 &&
                mouseY>BUTTON_Y-BUTTON_HEIGHT/2 && mouseY<BUTTON_Y+BUTTON_HEIGHT/2){
                    //If the button has been clicked
                    if(serverPid!=0){ //User wants to stop the server
                        try{
                            //Stop the server
                            infoFile= new File(LOG_FILE);
                            
                            Runtime.getRuntime().exec("taskkill /F /PID "+serverPid); //WINDOWS
                            
                            serverPid= 0;
                            
                            reader= new BufferedReader(new FileReader(infoFile));
                            String PID= reader.readLine();
                            String path= reader.readLine(); //I know this is bad code...
                            
                            reader.close();
                            
                            writer= new PrintWriter(new FileWriter(infoFile));
                            
                            writer.write(String.valueOf(serverPid)+"\r\n"+path);//writer.write(info);
                            writer.flush();
                            
                            writer.close();
                        }
                        catch(IOException e){
                            e.printStackTrace();
                        }
                        
                        drawOff();
                    }
                    else{ //User wants to start the server
                        if(ipAddress!=null){
                            String password= JOptionPane.showInputDialog(ui.getFrame(), "What do you want the password to be?", "Choose your password", JOptionPane.QUESTION_MESSAGE);
                            while(password==null)
                                password= JOptionPane.showInputDialog(ui.getFrame(), "Password length must be >0 and <31", "Choose your password", JOptionPane.QUESTION_MESSAGE);
                            
                            //Start the server
                            try{
                                serverProcess= Runtime.getRuntime().exec("javaw -jar -server StrmServerBE.jar "+password+" "+cloudPath);
                                //serverProcess= new ProcessBuilder("java", "-jar", "-server", "StrmServer.jar"+" "+password).start();
                                
                                Thread.sleep(500);
                                infoFile= new File(LOG_FILE);
                                
                                reader= new BufferedReader(new FileReader(infoFile));
                                
                                serverPid= Integer.parseInt(reader.readLine());
                                
                                reader.close();
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                            
                            drawOn();
                        }
                        else{
                            //Not connected to any LANs
                            JOptionPane.showMessageDialog(ui.getFrame(), "Not connected to any LANs.\nPlease exit and connect to a LAN then try again.", "Error", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        }
    }
    
    private static void drawOff(){
        ui.clear();
        
        drawMainMsg("Your cloud is currently stopped");
        drawButton("Start", BUTTON_OFF_FONT);
    }
    private static void drawOn(){
        //Maybe add nice small green light icon beside the text?
        ui.clear();
        
        drawMainMsg("Success! Your cloud address is:");
        drawAddress();
        drawButton("Stop", BUTTON_ON_FONT);
        
        ui.setFont(new Font("Arial Black", Font.PLAIN, WIDTH/30));
        ui.text(WIDTH/2, 3*HEIGHT/5, "(You can close this window now if your want)");
    }
    
    private static void drawMainMsg(String msg){
        ui.setFont(MAIN_MSG_FONT);
        
        ui.text(WIDTH/2, HEIGHT/5, msg);
    }
    private static void drawButton(String msg, Font chosenFont){
        ui.setFont(chosenFont);
        
        ui.rectangle(BUTTON_X, BUTTON_Y, (BUTTON_WIDTH)/2, (BUTTON_HEIGHT)/2);
        ui.text(BUTTON_X, BUTTON_Y, msg);
    }
    private static void drawAddress(){
        if(ipAddress.length()>15){
            ui.setFont(IV6P_ADDRESS_FONT);
            ipAddress= ipAddress.substring(0,ipAddress.indexOf("%"));
            ui.text(ADDRESS_X, ADDRESS_Y, ipAddress);
        }
        else{
            ui.setFont(IV4P_ADDRESS_FONT);
            ui.text(ADDRESS_X, ADDRESS_Y, ipAddress);
        }
    }
    
    public static String getIP(){
        ArrayList<String> ips= new ArrayList<String>();
        try{
            Enumeration<NetworkInterface> netInt= NetworkInterface.getNetworkInterfaces();
            while(netInt.hasMoreElements()){
                NetworkInterface connection= netInt.nextElement();
                if(!connection.isLoopback() && connection.isUp()){
                    Enumeration<InetAddress> addresses= connection.getInetAddresses();
                    while(addresses.hasMoreElements()){
                        InetAddress address= addresses.nextElement();
                        System.out.println("Host address: "+address.getHostAddress()+", Host name: "+address.getHostName());
                        ips.add(address.getHostAddress());
                        //if(!address.getHostAddress().equals(address.getHostName()))
                            //return address.getHostAddress();
                    }
                }
            }
        }
        catch(SocketException e){
            e.printStackTrace();
        }
        
        //Iterate through the array of IPs, and find one that is in IPv4 format
        if(!ips.isEmpty()){
            for(String ip : ips)
                if(ip.length()<=15)
                    return ip;
            
            //Otherwise return the first IPv6 address
            return ips.get(0);
        }
        

        return null; //If there is a failure
    }
}
