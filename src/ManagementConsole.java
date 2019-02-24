import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;

public class ManagementConsole implements Runnable{

	Socket client;
	BufferedReader clientReq; 
	BufferedWriter serverRes;
	PrintWriter pw;
	OutputStream os;
	private static HttpURLConnection con;
	
	public ManagementConsole(Socket clientSocket){
		this.client = clientSocket;
		try{
			
			this.client.setSoTimeout(30000);
			clientReq = new BufferedReader(new InputStreamReader(client.getInputStream()));
			serverRes = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			os = clientSocket.getOutputStream();
			pw = new PrintWriter(os, true);
			
		} catch (SocketTimeoutException e) {
			pw.println("Error: Connection TimeOut");
            e.printStackTrace();
		} catch (IOException i) {
			i.printStackTrace();
		} 
	}
	
	private String formatURL(String URL, String connectionType){
		String newURL="";
		int len = connectionType.length();
		if(!URL.substring(0,len).equals(connectionType)){
			newURL = connectionType+ URL;
			pw.println("Formatted " + connectionType + " url: " + newURL);
		}
		else newURL += URL;
		return newURL;
	}
	
	private String createFileName(String URL){
		String filename = "";
		
		filename += URL.substring(URL.lastIndexOf("."),URL.length());
		if(filename.contains("/")){
			filename.replace(".", "_");
			filename.replace("/", "__");
			filename += ".html";
		}
		String cutURL = URL.substring(0,URL.lastIndexOf("."));
		cutURL = cutURL.substring(cutURL.indexOf(0)+1);
		cutURL.replace(".", "_");
		cutURL.replace("/", "__");
		
		filename = cutURL + filename;
		return filename;
	}
	
	private void blockedURLRequested(String URL){
		pw.println("Access to " + URL + " denied message: ");
		String message = "HTTP/1.0 403 Access Forbidden\n" + 
				 		 "User-Agent: ProxyServer/1.0\n" + "\r\n";
		pw.println(message);
	}
	
	private void connectHTTPSRequest(String HTTPSRequestURL){
		URL url;
		try{
			url = new URL(HTTPSRequestURL);
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
			StringBuilder content;
	        try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }
	        System.out.println(content.toString());
	        //pw.println(content.toString());
	        serverRes.write(content.toString());
		} catch (MalformedURLException e) {
		     e.printStackTrace();
	    } catch (IOException e) {
		     e.printStackTrace();
	    } finally{
	    	con.disconnect();
	    }
	}
		
	//http://diptera.myspecies.info/;
	private void sendGETRequest(String URLlink){
		try {
	        URL myurl = new URL(URLlink);
	        con = (HttpURLConnection) myurl.openConnection();
	
	        con.setRequestMethod("GET");
	        
	        StringBuilder content;
	
	        try (BufferedReader in = new BufferedReader(
	                new InputStreamReader(con.getInputStream()))) {
	
	            String line;
	            content = new StringBuilder();
	
	            while ((line = in.readLine()) != null) {
	                content.append(line);
	                content.append(System.lineSeparator());
	            }
	            in.close();
	        }	
	        
	        // send results
	        System.out.println(content.toString());	    
	        // pw.println(content.toString());
	        serverRes.write(content.toString());
	        serverRes.flush();
	        
	        // now we want tp save the files
	        String filename = createFileName(URLlink);
	    } catch (MalformedURLException e){
	    	e.printStackTrace();
	    } catch (ProtocolException p){
	    	p.printStackTrace();
	    } catch(IOException i){
	    	i.printStackTrace();
	    } finally {
	        con.disconnect();
	    }    
	}
	
	/*
	private String sendGETRequest(String URLlink){
		StringBuilder content = new StringBuilder();
		int targetPort = 80;

		try {
			URL url = new URL(URLlink);
			if(url.getPort()!=-1) targetPort = url.getPort();
			if(url.getProtocol().equalsIgnoreCase("https")){
				System.err.println("Sorry. I only take in http urls.");
			}
			Socket s = new Socket(url.getHost(),targetPort);
			OutputStream os = s.getOutputStream();
			
	        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))){
	        	
         	String line;
         	while ((line = br.readLine()) != null) {
         		content.append(line);
                content.append(System.lineSeparator());
	        }
	        } finally {
	        	s.close();
	        }
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException io){
			System.err.println(io);
			io.printStackTrace();
		} 
		System.out.println(content.toString());
		return content.toString();       
	}*/
	
	@Override
	public void run() {
		String req;
		pw.println("How can I help you?");
		try {
			req = clientReq.readLine();
			System.out.println(req);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("ERROR: Can't read input requests from client");
			return;
		}
		
		String [] reqBlock = req.split(" ");
		
		// handles http connection 
		if (reqBlock[0].equalsIgnoreCase("Connect")){
			// this must be case sensitive 
			String reqURL = formatURL(reqBlock[1],"http://");		
			if(ProxyServer.isBlocked(reqURL)){
				blockedURLRequested(reqURL);
				pw.println(reqURL + " site is blocked");
				return;
			}
			else{
				if(ProxyServer.getCachedPage(reqURL)!=null){
					System.out.println("Cache copy found for " + reqURL);
					pw.println("Cached Page Found!!! Sending copy......");
				}
				else{
					System.out.println("Cache copy not found for " + reqURL);
					pw.println("Cached Page not Found!!! Initiating GET request for " + reqURL + "......");
					sendGETRequest(reqURL);
				}
			}
		}
		
		// handling https connection
		else if (reqBlock[0].equalsIgnoreCase("Connects")){
			String reqURL = formatURL(reqBlock[1],"https://");
			if(ProxyServer.isBlocked(reqURL)){
				blockedURLRequested(reqURL);
				pw.println(reqURL + " site is blocked");
				return;
			}
			else{
				pw.println("Connecting to " + reqURL + "....");
				connectHTTPSRequest(reqURL);
				pw.println("Connection to " + reqURL +  " established");
			}
		}
	}
}
