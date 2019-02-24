import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class ProxyServer implements Runnable{
	static HashSet<String> BlockedList;
	static HashMap<String, File> CachedList;
	static ArrayList<Thread> ActiveThreadsList;
	private volatile boolean active = true;
	private ServerSocket ProxySocket;
	
	public ProxyServer(int portNumber, int maxQueue){
		//initialise data structures 
		BlockedList = new HashSet<String>();
		ActiveThreadsList = new ArrayList<Thread>();
		CachedList = new HashMap<String,File>();
		
		Thread serverthread = new Thread(this);
		serverthread.start();
		try {
			ProxySocket = new ServerSocket(portNumber, maxQueue);
			loadList();
			active = true;				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		// set up data structure 
		
	}
	
	private void loadList(){
		try{
			File file = new File("CachedURL.ser");
			
			if(!file.exists()){
				System.out.println("Error: CachedURL.ser does not exist");
				file.createNewFile();
			}
			
			else{
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				CachedList = (HashMap<String,File>)ois.readObject();
				ois.close();
				fis.close();
			}
			
			File file2 = new File("BlockedURL.ser");
			
			if(!file2.exists()){
				System.out.println("Error: BlockedURL.ser does not exist");
				file2.createNewFile();
			}
			
			else{
				FileInputStream fis2 = new FileInputStream(file2);
				ObjectInputStream ois2 = new ObjectInputStream(fis2);
				BlockedList = (HashSet<String>)ois2.readObject();
				ois2.close();
				fis2.close();
			}
					
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException io){
			io.printStackTrace();
		} catch (ClassNotFoundException cnf){
			cnf.printStackTrace();
		}
	}
	/*
	 * might not need this method depending on the client 
	 */
	public static void blockURl(String URL){
		BlockedList.add(URL);
		System.out.println("Blocked URL List:");
		Iterator<String> bList = BlockedList.iterator();
		while(bList.hasNext()){
			System.out.println(bList.next());
		}
	}
	
	// handle proper caching when new get request done
	private void caching(){
		
	}
	
	public static boolean isBlocked(String URL){
		return BlockedList.contains(URL);
	}
	
	// get cache webpage
	public static File getCachedPage(String URL){
		System.out.println("Checking cache copy for " + URL);
		return CachedList.get(URL);
	}
		
	//caching webpages 
	public static void cachePage(String URL, File file){
		CachedList.put(URL, file);	
	}
	
	//close server
	private void closeProxyServer(){
		active = false;
		
		// save all blocked sites and cache sites to txt.file
		try {
			
			System.out.println("Saving cached URL sites to CachedURL.ser ....");
			FileOutputStream cachedFile = new FileOutputStream("CachedURL.ser",false);
			ObjectOutputStream oos = new ObjectOutputStream(cachedFile);
            oos.writeObject(CachedList);
            oos.close();
            cachedFile.close();
            System.out.println("Saving cached URL sites to CachedURL.ser completed.");
            
            
            System.out.println("Saving blocked URL sites to BlockedURL.ser ....");
			FileOutputStream blockedFile = new FileOutputStream("BlockedURL.ser",false);
			ObjectOutputStream oos2 = new ObjectOutputStream(blockedFile);
            oos2.writeObject(BlockedList);
            oos2.close();
            blockedFile.close();
            System.out.println("Saving blocked URL sites to BlockedURL.ser completed.");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException io){
			io.printStackTrace();
		}
		closeThreads();
		try {
			ProxySocket.close();
		} catch (IOException e) {
			System.out.println("Error: Server Terminating Failed");
			e.printStackTrace();
		}
		System.out.println("Proxy Server Connection Terminated Successfully.");
	}
	
	private void closeThreads(){
		
		for(Thread clientThread : ActiveThreadsList){
			if(clientThread.isAlive()){
				System.out.println("Closing thread" + clientThread.getId());
				try {
					clientThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void listen(){		
		while(active){
			try {	
				Socket client = ProxySocket.accept();		
				System.out.println(client.getPort());
				Thread serviceThread = new Thread(new ManagementConsole(client));
				ActiveThreadsList.add(serviceThread);
				serviceThread.start();
			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy 
				System.out.println("Server closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	

	@Override
	public void run() {
		Scanner InputScanner = new Scanner(System.in);
		String instruction;
		
		while(active){
			System.out.println("How can I help you?");
			instruction = InputScanner.nextLine();
			//handle commands 
			if(instruction.equalsIgnoreCase("CL")){
				
			}
			
			else if(instruction.equalsIgnoreCase("BL")){
				System.out.println("Blocked URL List:");
				Iterator<String> bList = BlockedList.iterator();
				while(bList.hasNext()){
					System.out.println(bList.next());
				}
			}
			
			else if(instruction.equalsIgnoreCase("Help") || instruction.isEmpty()){
				
			}
			
			else if(instruction.equalsIgnoreCase("Quit")){
				System.out.println("Closing Proxy Server......");
				closeProxyServer();
			}		
			
			else if(instruction.substring(0,5).equalsIgnoreCase("Block")){
				BlockedList.add(instruction.substring(6));
				System.out.println("Blocking " + instruction.substring(6));
			}
			
			else if(instruction.substring(0,7).equalsIgnoreCase("Unblock")){
				BlockedList.remove(instruction.substring(8));
				System.out.println("Unblocking " + instruction.substring(8));
			}
		}
		InputScanner.close();
	}
	
	
	public static void main(String[] args){
		ProxyServer proxy = new ProxyServer(4000, 20);
		proxy.listen();
	}
	
}
