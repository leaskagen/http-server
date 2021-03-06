package no.kristiania.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpServer {

    private final ServerSocket serverSocket;
    private Path rootDirectory;

    public HttpServer(int serverPort) throws IOException {
        serverSocket = new ServerSocket(serverPort);

        new Thread(this::handleClients).start();

    }

    // En thread som kan ta i mot request fra client og gi svar til client samtidig
    private void handleClients() {
        try { // Try metoden vil lukke connection til porten når den er ferdig med å gi svar
            while(true){
                handleClient();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void handleClient() throws IOException {
        Socket clientSocket = serverSocket.accept();

        String[] requestLine = HttpClient.readLine(clientSocket).split(" ");
        String requestTarget = requestLine[1];

        int questionPos = requestTarget.indexOf('?');
        String fileTarget;
        String query = null;
        if (questionPos != -1) {
            fileTarget = requestTarget.substring(0, questionPos);
            query = requestTarget.substring(questionPos+1);
        } else {
            fileTarget = requestTarget;
        }

        if( fileTarget.equals("/hello")){
            String yourName = "world";
            if (query != null) {
                yourName = query.split("=")[1];
            }
            String responseText = "<p>Hello " + yourName + "</p>";

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + responseText.length() + "\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Connection: close \r\n" +
                    "\r\n" +
                    responseText;

            clientSocket.getOutputStream().write(response.getBytes());
        } else {
            if(rootDirectory != null && Files.exists(rootDirectory.resolve(fileTarget.substring(1)))){
                String responseText = Files.readString(rootDirectory.resolve(fileTarget.substring(1)));

                String contentType = "text/plain";

                if ( fileTarget.endsWith(".html")){
                    contentType = "text/html";
                }
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: " + responseText.length() + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Connection: close \r\n" +
                        "\r\n" +
                        responseText;

                clientSocket.getOutputStream().write(response.getBytes());
            }


            String responseText = "File not found: " + requestTarget;

            String response = "HTTP/1.1 404 Not found\r\n" +
                    "Content-Length: " + responseText.length() + "\r\n" +
                    "\r\n" +
                    responseText;

            clientSocket.getOutputStream().write(response.getBytes());
        }

    }

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = new HttpServer(6969);
        httpServer.setRoot(Paths.get("."));
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public void setRoot(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }
}
