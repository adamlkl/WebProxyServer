"# WebProxyServer" <br />

## Purpose ##
The aim of this project is to implement a Web Proxy Server. <br />
<img src="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/image/CS3031_Proj1-1.jpg" /><br />
The pdf version of the problem specification can be found here <a href="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/CS3031_Proj1.pdf">here<a />.

## Introduction ##
In computer networks, a proxy server is a server that acts as the intermediary application connecting the clients and servers. It is typically used to receive information seeking requests from clients and direct them towards the target servers. The functionalities of proxy servers also encompasses processes such as comprehending and evaluating complex requests before simplifying them. Furthermore, the implementation of proxy servers also adds layers of structure and encapsulation to distributed systems in real world.
<br />
## Design ##
The web proxy server is divided into mainly two components: the web proxy server and a management console. This program is written using Java programming language.
<img src="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/image/design2.png" />
## Function ##
The main functionality of the Web Proxy Server revolves around establishing **HTTP** and **HTTPS** connections. In general, when a proxy server receives a request from web client, it will run through up to a maximum of 3 decision boxes to decide on its approach for the request based on its needs. But they will all in the end converge into one state, which is the proxy server relaying the results of the request to the web client, even if the message to be conveyed is an indication that the website requested is forbidden by the administrator.
<img src="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/image/flowchart.png" />
## Report ##
A more detailed documentation of the report can be found <a href="https://github.com/adamlkl/WebProxyServer/blob/master/documentation/report/WebProxyServerDocumentation.pdf">here<a />.
