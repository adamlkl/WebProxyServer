import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
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
	private static HttpsURLConnection cons;
	private Thread httpsClientToServer;
	
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
			cons = (HttpsURLConnection)url.openConnection();
			StringBuilder content;
	        try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(cons.getInputStream()))) {

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
	    	cons.disconnect();
	    }
	}
	
	//http://diptera.myspecies.info/;
	private void sendGETRequest(String URLlink){
		try {
	        URL myurl = new URL(URLlink);
	        HttpURLConnection con = (HttpURLConnection) myurl.openConnection();
	
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
		if (reqBlock[0].equalsIgnoreCase("GET")){
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
		else if (reqBlock[0].equalsIgnoreCase("link")){
			String reqURL = formatURL(reqBlock[1],"https://");
			if(ProxyServer.isBlocked(reqURL)){
				blockedURLRequested(reqURL);
				pw.println(reqURL + " site is blocked");
				return;
			}
			else{
				pw.println("Connecting to " + reqURL + "....");
				connectHTTPSRequest(reqURL);
				handleHTTPSRequest(reqURL);
				pw.println("Connection to " + reqURL +  " established");
			}
		}
	}
	
	/**
	 * Handles HTTPS requests between client and remote server
	 * @param urlString desired file to be transmitted over https
	 */
	private void handleHTTPSRequest(String urlString){
		// Extract the URL and port of remote 
		String url = urlString.substring(7);
		String pieces[] = url.split(":");
		url = pieces[0];
		int port  = Integer.valueOf(pieces[1]);

		try{
			// Only first line of HTTPS request has been read at this point (CONNECT *)
			// Read (and throw away) the rest of the initial data on the stream
			for(int i=0;i<5;i++){
				clientReq.readLine();
			}

			// Get actual IP associated with this URL through DNS
			InetAddress address = InetAddress.getByName(url);
			
			// Open a socket to the remote server 
			Socket proxyToServerSocket = new Socket(address, port);
			proxyToServerSocket.setSoTimeout(5000);

			// Send Connection established to the client
			String line = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
					"\r\n";
			serverRes.write(line);
			serverRes.flush();
			
			
			
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party


			//Create a Buffered Writer betwen proxy and remote
			BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

			// Create Buffered Reader from proxy and remote
			BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));


			// Create a new thread to listen to client and transmit to server
			ClientToServerHttpsTransmit clientToServerHttps = 
					new ClientToServerHttpsTransmit(client.getInputStream(), proxyToServerSocket.getOutputStream());
			
			httpsClientToServer = new Thread(clientToServerHttps);
			httpsClientToServer.start();
			
			
			// Listen to remote server and relay to client
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToServerSocket.getInputStream().read(buffer);
					if (read > 0) {
						client.getOutputStream().write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							client.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {
				
			}
			catch (IOException e) {
				e.printStackTrace();
			}


			// Close Down Resources
			if(proxyToServerSocket != null){
				proxyToServerSocket.close();
			}

			if(proxyToServerBR != null){
				proxyToServerBR.close();
			}

			if(proxyToServerBW != null){
				proxyToServerBW.close();
			}

			if(serverRes != null){
				serverRes.close();
			}
			
			
		} catch (SocketTimeoutException e) {
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			try{
				serverRes.write(line);
				serverRes.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} 
		catch (Exception e){
			System.out.println("Error on HTTPS : " + urlString );
			e.printStackTrace();
		}
	}
	
	/**
	 * Listen to data from client and transmits it to server.
	 * This is done on a separate thread as must be done 
	 * asynchronously to reading data from server and transmitting 
	 * that data to the client. 
	 */
	class ClientToServerHttpsTransmit implements Runnable{
		
		InputStream proxyToClientIS;
		OutputStream proxyToServerOS;
		
		/**
		 * Creates Object to Listen to Client and Transmit that data to the server
		 * @param proxyToClientIS Stream that proxy uses to receive data from client
		 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
		 */
		public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.proxyToClientIS = proxyToClientIS;
			this.proxyToServerOS = proxyToServerOS;
		}

		@Override
		public void run(){
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToClientIS.read(buffer);
					if (read > 0) {
						proxyToServerOS.write(buffer, 0, read);
						if (proxyToClientIS.available() < 1) {
							proxyToServerOS.flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException ste) {
				// TODO: handle exception
			}
			catch (IOException e) {
				System.out.println("Proxy to client HTTPS read timed out");
				e.printStackTrace();
			}
		}
	}
	
}
