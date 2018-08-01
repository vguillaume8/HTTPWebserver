import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;


// Each client will be managed in a dedicated thread
public class HttpWebServer implements  Runnable{

    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";

    // port to listen to connection
    static final int PORT = 8080;

    // verbose mode
    static final boolean verbose = true;

    // Client Connection via Socket Class
    private Socket connect;

    public HttpWebServer(Socket c){
        connect = c;

    }

    public static void main(String[] args){

        try{

            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + "...\n");

            // we listen until user halts server execution
            while(true){
                HttpWebServer myServer = new HttpWebServer(serverConnect.accept());

                if(verbose){
                    System.out.println("Connection opened. (" + new Date() + ")");
                }

                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }
        } catch (IOException e){
            System.out.println("Server Connection error : " + e.getMessage());
        }
    }


    @Override
    public void run(){

        // we manage our particular client connection
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try{

            // we read characters from the client via input
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));

            // we get character output stream to client (for headers)
            out = new PrintWriter(connect.getOutputStream());

            // get binary output stream to client (for requested data)
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            // get first line of the request from the client
            String input = in.readLine();

            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);

            // we get the HTTP method of the client
            String method = parse.nextToken().toUpperCase();

            // we get the file requested
            fileRequested = parse.nextToken().toLowerCase();

            // we only support GET and HEAD methods, we check
            if(!method.equals("GET") && !method.equals("HEAD")){
                if(verbose){
                    System.out.println("501 Not Implemented: " + method + "method");
                }

                // we return the not supported file to the client
                File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";

                // read content to return to client
                byte[] fileData = readFileData(file, fileLength);

                // we send HTTP Headers with data to client
                out.println("HTTP/1.1 501 Not Implemeneted");
                out.println("Server: Java HTTP Server from Vince : 1.0");
                out.println("Date: " + new Data());
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);

                // blank line between headers and content, very important!
                out.println();

                // flush character output stream buffer
                out.flush();

                // file
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();

            } else{

                // GET or HEAD method
                if(fileRequested.endsWith("/")){
                    fileRequested += DEFAULT_FILE;
                }

                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);

                // GET method so we return content
                if(method.equals("GET")){

                    byte[] fileData = readFileData(file, fileLength);

                    // send HTTP Headers
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Java HTTP Server from Vince : 1.0");
                    out.println("Data: " + new Date());;
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);

                    // blank line between headers and content, very important!
                    out.println();

                    // flush character output stream buffer
                    out.flush();

                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();


                }

                if(verbose){
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }


            }
        } catch (FileNotFoundException fnfe){

            try{
                // error
                fileNotFound(out, dataOut, fileRequested);

            } catch(IOException ioe){
                System.out.println("Error with file not found");
            }

        } catch(IOException ioe){
            System.out.println("Server error : " + ioe);

        } finally {

            try{

                in.close();
                out.close();
                dataOut.close();

                // we close socket connection
                connect.close();

            } catch(Exception e){
                System.out.println("Error closing stream : " + e.getMessage());
            }

            if(verbose){
                System.out.println("Connection closed.\n");
            }
        }
    }




}
