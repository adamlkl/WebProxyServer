import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.Certificate;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class ManagementConsole implements Runnable{
	
	Socket client;
	BufferedReader clientReq; 
	BufferedWriter serverRes;
	private Thread ClientToServerHTTPSConnection;
	
	public ManagementConsole(Socket clientSocket){
		this.client = clientSocket;
		try{			
			this.client.setSoTimeout(3000);
			clientReq = new BufferedReader(new InputStreamReader(client.getInputStream()));
			serverRes = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));			
		} catch (SocketTimeoutException e) {
			System.out.println("Socket " + client.getLocalPort() + " Error: Connection TimeOut");
            e.printStackTrace();
		} catch (IOException i) {
			i.printStackTrace();
		} 
	}
	
	class HTTPSTransmission implements Runnable{
		
		// input stream from proxy to client
		InputStream is;
		// output stream from proxy to server
		OutputStream os;
		int bufferSize;
		
		/**
		 * Creates Object to Listen to Client and Transmit that data to the server
		 * @param proxyToClientIS Stream that proxy uses to receive data from client
		 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
		 */
		public HTTPSTransmission(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.is = proxyToClientIS;
			this.os = proxyToServerOS;
			bufferSize = 4096;
		}

		@Override
		public void run(){
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[bufferSize];
				int read;
				do {
					read = is.read(buffer);
					if (read > 0) {
						os.write(buffer, 0, read);
						if (is.available() < 1) {
							os.flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {
				e.printStackTrace();
			}
			catch (IOException io) {
				System.out.println("Buffer Read TimeOut from Proxy to Client");
				io.printStackTrace();
			}
		}
	}
	
	// error message displayed to client when blocked page accessed 
	private void blockedURLRequested(String URL){
		try {
			String message = "Access to " + URL + " denied message: \n" + 
							 "HTTP/1.0 403 Access Forbidden\n" + 
							 "User-Agent: ProxyServer/1.0\n" + "\r\n";
			serverRes.write(message);
			serverRes.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void printCertificate(String https_url){
		try {
			URL url = new URL(https_url);
			HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
		    if(con!=null){
				
		       try {			  				
				  	System.out.println("Response Code : " + con.getResponseCode());
				  	System.out.println("Cipher Suite : " + con.getCipherSuite());
				  	System.out.println("\n");
				  				
				  	Certificate[] certs = con.getServerCertificates();
				  	for(Certificate cert : certs){
				  	   System.out.println("Cert Type : " + cert.getType());
				  	   System.out.println("Cert Hash Code : " + cert.hashCode());
				  	   System.out.println("Cert Public Key Algorithm : " 
				                                      + cert.getPublicKey().getAlgorithm());
				  	   System.out.println("Cert Public Key Format : " 
				                                      + cert.getPublicKey().getFormat());
				  	   System.out.println("\n");
			  	}
			  				
			  	} catch (SSLPeerUnverifiedException e) {
			  		e.printStackTrace();
			  	} catch (IOException e){
			  		e.printStackTrace();
			  	}
		    }
		} catch (MalformedURLException e) {
		     e.printStackTrace();
	    } catch (IOException e) {
		     e.printStackTrace();
	    }
	}
	
	// printing cert using httpssslurlconnection
	private void connectHTTPSRequest(String urlLink){
		String url = urlLink.substring(7);	
		String split[] = url.split(":");
		url = split[0];
		// print certificate of https website
		printCertificate("https://" + url);
		int port  = Integer.valueOf(split[1]);

		try{
			// Only first line of HTTPS request has been read at this point (CONNECT *)
			// Read (and throw away) the rest of the initial data on the stream
			for(int i=0;i<5;i++){
				clientReq.readLine();
			}

			// Get actual IP associated with this URL through DNS
			InetAddress linkAddr = InetAddress.getByName(url);
			
			// Open a socket for proxy to the remote server 
			Socket socket = new Socket(linkAddr, port);
			socket.setSoTimeout(10000);

			// Send Connection established to the client
			String res = "HTTP/1.0 200 Connection established\r\n" +
						 "Proxy-Agent: ProxyServer/1.0\r\n" +
						 "\r\n";
			
			serverRes.write(res);
			serverRes.flush();
			
			
			
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party


			//Create a Buffered Writer betwen proxy and remote
			BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

			// Create Buffered Reader from proxy and remote
			BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));



			// Create a new thread to listen to client and transmit to server
			HTTPSTransmission clientToServerHttps = 
					new HTTPSTransmission(client.getInputStream(), socket.getOutputStream());
			
			ClientToServerHTTPSConnection = new Thread(clientToServerHttps);
			ClientToServerHTTPSConnection.start();
			int bufferSize = 4096;
			
			// Listen to remote server and relay to client
			try {
				byte[] buffer = new byte[bufferSize];
				int read;
				do {
					read = socket.getInputStream().read(buffer);
					if (read > 0) {
						client.getOutputStream().write(buffer, 0, read);
						if (socket.getInputStream().available() < 1) {
							client.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {
				System.out.println("Socket Time Out Error");
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}


			// Close Down Resources
			if(socket != null){
				socket.close();
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
			System.out.println("Error on HTTPS : " + urlLink );
			e.printStackTrace();
		}
	}
	
	private void HandleGETRequestWithCachedCopy(String urlLink){
		File file = ProxyServer.getCachedPage(urlLink);
		String fileType = file.getName().substring(file.getName().lastIndexOf("."));
		
		try{
			if((fileType.contains(".jpeg")) || fileType.contains(".jpg") ||
					fileType.contains(".gif") || fileType.contains(".png")){
				
				BufferedImage img = ImageIO.read(file);
				
				// check if getting results to send response code to client
				if(img!=null){				
					String res = "HTTP/1.0 200 OK\n" +
								 "Proxy-agent: ProxyServer/1.0\n" +
								 "\r\n";
					serverRes.write(res);
					serverRes.flush();
					System.out.println("Image retrieval SUCCESS.");
					ImageIO.write(img, fileType.substring(1), client.getOutputStream());
				}
				
				// image read failed or not received from URL link server
				else{
					String res = "HTTP/1.0 404 NOT FOUND\n" +
								"Proxy-agent: ProxyServer/1.0\n" +
								"\r\n";
					serverRes.write(res);
					serverRes.flush();
					System.out.println("Cached Image retrieval FAILED.");
					return;
				}
			}
			
			else {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

				String res = "HTTP/1.0 200 OK\n" +
							 "Proxy-agent: ProxyServer/1.0\n" +
							 "\r\n";
				serverRes.write(res);
				serverRes.flush();

				String line;
				while((line = br.readLine()) != null){
					serverRes.write(line);
				}
				serverRes.flush();
				
				// Close resources
				if(br != null) br.close();	
			}
			
			if(serverRes!=null)serverRes.close();	
			
		} catch (IOException e) {
			System.out.println("Error Sending Cached file to client");
			e.printStackTrace();
		}
	}
	
	private void HandleGETRequestWithoutCachedCopy(String urlLink){
		
		// separate file name and file extension type
		String fileName = urlLink.substring(0,urlLink.lastIndexOf("."));
		String fileType = urlLink.substring(urlLink.lastIndexOf("."), urlLink.length());
		
		// trim off the http://www.
		fileName = fileName.substring(fileName.indexOf(".")+1);
		
		// remove illegal characters for filenames
		fileName = fileName.replace(".", "_");
		fileName = fileName.replace("/", "__");
		
		// replace the trailing / to .html as file extension type for saving
		if(fileType.contains("/")){
			fileType = fileType.replace(".", "_");
			fileType = fileType.replace("/", "__");
			fileType = fileType + ".html";
		}
		
		// combine the currect filename and extension type
		fileName = fileName + fileType;
		System.out.println("filename created: " + fileName);
		
		// check if we successfully created a file
		boolean caching = true;
		File file = null;
		BufferedWriter filebw = null; 
		
		try{
			// create an abstract pathname to store the cache
			file = new File("cache_files/"+fileName);
			
			// check if there is a previous version of cached file stored
			if(!file.exists()){
				file.createNewFile();
			} 
			
			// writer for copying output stream from GET request into file
			filebw = new BufferedWriter(new FileWriter(file));
		} catch(IOException io){
			caching = false;
			System.out.println("Create File Error, Aborting Caching");
			io.printStackTrace();
		}
		
		try{
			// handling image files 
			if((fileType.contains(".jpeg")) || fileType.contains(".jpg") ||
					fileType.contains(".gif") || fileType.contains(".png")){
				
				URL url = new URL(urlLink);
				BufferedImage img = ImageIO.read(url);
				
				// check if getting results to send response code to client
				if(img!=null && file!=null){
					ImageIO.write(img, fileType.substring(1), file);
					String res = "HTTP/1.0 200 OK\n" +
								"Proxy-agent: ProxyServer/1.0\n" +
								"\r\n";
					serverRes.write(res);
					serverRes.flush();
					System.out.println("Image retrieval SUCCESS.");
					ImageIO.write(img, fileType.substring(1), client.getOutputStream());
				}
				
				// image read failed or not received from URL link server
				else{
					String res = "HTTP/1.0 404 NOT FOUND\n" +
								"Proxy-agent: ProxyServer/1.0\n" +
								"\r\n";
					serverRes.write(res);
					serverRes.flush();
					System.out.println("Image retrieval FAILED.");
					return;
				}
			}
			
			// handling pure text files or html
			else{
				// create connection using url
				URL url = new URL(urlLink);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				
				// setting properties for connection
				con.setUseCaches(false);
				con.setDoOutput(true);
				con.setRequestProperty("Content-Type", 
						"application/x-www-form-urlencoded");
				con.setRequestProperty("Content-Language", "en-US"); 
				
				// print response code
			    if(con!=null){
					
			        try {	
					  	System.out.println("Response Code : " + con.getResponseCode());		  				
				  	} catch (SSLPeerUnverifiedException e) {
				  		e.printStackTrace();
				  	} catch (IOException e){
				  		e.printStackTrace();
				  	}
			    }
				
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String res = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				serverRes.write(res);
				
				String line;
	            StringBuilder content = new StringBuilder();
	
	            while ((line = in.readLine()) != null) {
	                content.append(line);
	                content.append(System.lineSeparator());
	                serverRes.write(line);
	                
	                if(caching)	filebw.write(line);          
	            }
	            // print traffic data in console
	            System.out.println(content.toString());
	            
	            // close buffered reader after operation
	            if(in!=null) in.close();		
			}
			
			if(caching){
				filebw.flush();
				ProxyServer.cachePage(urlLink, file);
			}
			
			if(filebw!=null)filebw.close();
			if(serverRes!=null)serverRes.close();
		}catch (MalformedURLException e){
	    	e.printStackTrace();
	    } catch (ProtocolException p){
	    	p.printStackTrace();
	    } catch(IOException i){
	    	i.printStackTrace();
	    }
	}
	
	@Override
	public void run(){
		String input;
		try {
			input = clientReq.readLine();
			System.out.println("Socket " + client.getLocalPort() + " Request: " + input);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Socket " + client.getLocalPort() +" ERROR: Can't read input requests from client");
			return;
		}

		String[] split = input.split(" ");
		String req = split[0];
		String url = split[1];
		
		if(!url.substring(0,4).equals("http")){
			url = "http://" + url;
		}
		try {
			System.out.println("host: " +new URL(url).getHost());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (ProxyServer.isBlocked(url)){
			System.out.println(url + " site connection is blocked");
			blockedURLRequested(url);
			return;
		}
		
		else{
			if(req.equals("CONNECT")){			
				System.out.println("Connecting to: " +url);
				connectHTTPSRequest(url);
			}
			
			else{
				if(ProxyServer.hasCachedCopy(url)){
					System.out.println("Cache copy found for HTTP GET" + url);
					long currentTime = System.currentTimeMillis();
					HandleGETRequestWithCachedCopy(url);
					long diff = System.currentTimeMillis() - currentTime;
					System.out.println("With Cache: " + diff);
				}
				else{
					System.out.println("Cache copy not found for HTTP GET" + url);
					long currentTime = System.currentTimeMillis();
					HandleGETRequestWithoutCachedCopy(url);
					long diff = System.currentTimeMillis() - currentTime;
					System.out.println("Without Cache: " + diff);
				}		
			}
		}
	}
	
	
}