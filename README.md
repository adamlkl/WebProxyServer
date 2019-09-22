# WebProxyServer #

## Purpose ##
The aim of this project is to implement a Web Proxy Server. The pdf version of the problem specification can be found here <a href="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/CS3031_Proj1.pdf">here</a>.

## Introduction ##
In computer networks, a proxy server is a server that acts as the intermediary application connecting the clients and servers. It is typically used to receive information seeking requests from clients and direct them towards the target servers. The functionalities of proxy servers also encompasses processes such as comprehending and evaluating complex requests before simplifying them. Furthermore, the implementation of proxy servers also adds layers of structure and encapsulation to distributed systems in real world.
<br />
## Design ##
The web proxy server is divided into mainly two components: the web proxy server and a management console. This program is written using Java programming language.
<img src="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/image/design2.png" />
The functionalities of the web proxy server should include the following aspects: 
* Respond to HTTP & HTTPS requests, and should display each request on a management console. It should forward the request to the Web server and relay the response to the browser. 
* Handle websocket connections. 
* Dynamically block selected URLs via the management console. 
* Efficiently cache requests locally and thus save bandwidth. You must gather timing and bandwidth data to prove the ef?ciency of your proxy. 
* Handle multiple requests simultaneously by implementing a threaded server.
## Function ##
The main functionality of the Web Proxy Server revolves around establishing **HTTP** and **HTTPS** connections. In general, when a proxy server receives a request from web client, it will run through up to a maximum of 3 decision boxes to decide on its approach for the request based on its needs. But they will all in the end converge into one state, which is the proxy server relaying the results of the request to the web client, even if the message to be conveyed is an indication that the website requested is forbidden by the administrator.
<img src="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/image/flowchart.png" />

### Proxy Server Implementation ###
#### Setting up ####
This program make extensive use of TCP sockets provided in Java.net.sockets library to handle connections. The proxy web server is implemented using server sockets that is capable of accepting multiple socket connections from clients and handles them simultaneously,thus allowing multi-threading. When the web proxy server socket receive a connection request from a client socket, it creates a thread to handle their request and the thread is then put into a thread pool for concurrent management during the process. Every time when the proxy web server is turned on, it will load back the cached data and blocked websites into their respective data structures, as a method of continuing its from previous state.

To take in input request from and output messages as response to web clients and web servers, the proxy server make use of buffered readers and writers. Buffered Reader allows the web server to read lines of request and response from both sides and extract important information for further processing. On the other hand after the further processing, it will output the result of it using Buffered Writer, which is a versatile application in dealing with sending response code to sockets and writing data retrieved from socket output streams on a file.

#### SendingHTTPRequest ####
There two situations that can happened when dealing with HTTPrequest, that is if there is a cached copy available for the requested URL site or not. To deal with situation where there are no cached copies available typically during compulsory cache misses,the proxy server ?rst createsa?leusing the url host name as the file name and the url extension as the ?le type extension. Then, depending on the type of requested data, whether if is an image or a text based page, the proxy server will set up http url connection to forward the GET request to the site and attempt to extract its data and relay it to the web client. At the same time, it will also try to save a copy of the visited site using a separate buffer writer or ImageIO if it is an image to store a copy of the result data into the file createdearlier. The web cache is implemented using HashMap with the url as key and the file as its corresponding values, due to its resemblance to a normal cache, where the url act as the memory address and the ?le act as the data stored at that particular address. Suppose if the request was a failure, then a response code of 404 NOT FOUND will be transmitted to the client, otherwise a response code of HTTP 200 will be transmitted instead. HTTPUrlConnection is being prioritized over using sockets here due to its versatility to extract even the last modi?ed date of a web page that can be used to implement the proper functionality of a web cache.

Now, since there is a copy of the visited site preserved in the proxy server already, when the same site is being visited again, all that is needed to do is to ?nd the ?le name of the saved copy in the cache using the url of the requested site as key to access the cache Hash Map data structure. If there is hit, the contents of the file will be extracted using buffered reader and transmitted to client using buffered writer. This further reduces the time needed to transmit data as there is no need to set up a connection for retrieving data.

#### SendingHTTPSRequest ####
Sending a HTTPS request can be tricky. This is because HTTPS connections make use of secure sockets **(SSL)** making data transfer between clients and servers encrypted. To overcome this problem,the proxy server needs to know how to deal with the encrypted data. The approach used in this context is called **HTTP Tunneling method**, which allow us to send encrypted data over public network. This is how it works. When the proxy server receives a CONNECT Request, it attempts to extract the destination url from the inputs and created a socket connection to the remote web server. A response code of 200 is sent to client as confirmation of the connection being established. Now all there is left id to create a thread that allows the continuous transmissions of data between client and server through the proxy server simultaneously, thus enabling web socket connections application. However, since the data input and output stream are encrypted, it will be difficult to cache the data.

## Report ##
A more detailed documentation of the report can be found <a href="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/report/WebProxyServerDocumentation.pdf">here<a />.
