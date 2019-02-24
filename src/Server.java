import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

public class Server {
	/*
	public static void main(String args[]) throws IOException {
		final int portNumber = 81;
		System.out.println("Creating server socket on port " + portNumber);
		ServerSocket serverSocket = new ServerSocket(portNumber);
		while (true) {
			Socket socket = serverSocket.accept();
			OutputStream os = socket.getOutputStream();
			PrintWriter pw = new PrintWriter(os, true);
			pw.println("What's you name?");

			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String str = br.readLine();

			pw.println("Hello, " + str);
			pw.close();
			socket.close();

			System.out.println("Just said hello to:" + str);
		}
	}*/
	private static HttpURLConnection con;
/*
    public static void main(String[] args) throws MalformedURLException,
            ProtocolException, IOException {

        String url = "http://diptera.myspecies.info/";

        try {

            URL myurl = new URL(url);
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
            }

            System.out.println(content.toString());

        } finally {
            
            con.disconnect();
        }
    }
  */  
    public static void main(String[] args) throws Exception {
        String pageAddr = "http://diptera.myspecies.info/";
        URL url = new URL(pageAddr);
        String websiteAddress = url.getHost();

        String file = url.getFile();
        Socket clientSocket = new Socket(websiteAddress, 80);

        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket
            .getInputStream()));

        OutputStreamWriter outWriter = new OutputStreamWriter(clientSocket.getOutputStream());
        outWriter.write("GET " + file + " HTTP/1.0\r\n\n");
        outWriter.flush();
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        boolean more = true;
        String input;
        while (more) {
          input = inFromServer.readLine();
          if (input == null)
            more = false;
          else {
            out.write(input);
          }
        }
        out.close();
        clientSocket.close();
      }
    /*
	public static void main(String[] args) throws Exception {
        String httpsURL = "https://google.com/";
        URL myUrl = new URL(httpsURL);
        HttpsURLConnection conn = (HttpsURLConnection)myUrl.openConnection();
        InputStream is = conn.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String inputLine;
        while ((inputLine = br.readLine()) != null) {
            System.out.println(inputLine);
        }

        br.close();
    }*/
}