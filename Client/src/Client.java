package src;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client extends Observable {

    private boolean debug = true;

    private DatagramSocket ds;
    private int portToSend;
    private InetAddress ip;
    private int port;
    private int nextFileId = 0;

    private Socket socket;

    private BufferedReader reader;
    private BufferedWriter writer;

    private String host;
    private int hostPort;

    private boolean isAuthentificated = false;

    public Client(){}

    public Client(String ip, int port)
    {
        setIP(ip);
        this.port = port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setIP(String ip)
    {
        try {
            this.ip = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static int portScanner() {
        int portLibre = 0;
        for (int port = 2000; port <= 4000; port++) {
            try {
                DatagramSocket server = new DatagramSocket(port);
                if(portLibre == 0)
                    portLibre = port;
                server.close();
            } catch (SocketException ex) {
            }
        }
        return portLibre;
    }

    public boolean connectToHost(String host, int port){

        try{
            socket = new Socket();

            socket.connect(new InetSocketAddress(host, port));

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            readResponseLine();
            return isConnected();

        }catch (IOException e){
            return false;
        }

    }

    public boolean isConnected(){
         return socket != null && socket.isConnected();
    }

    public void disconnect(){

        try{
            socket.close();
        }catch(Exception E){
            System.out.println("Failed to disconnect.");
        }

        reader = null;
        writer = null;
        System.out.println("Disconnected from the host");
    }

    protected String readResponseLine() throws IOException{
        String response = reader.readLine();
        if (debug) {
            System.out.println("DEBUG [in] : " + response);
        }

        return response;
    }

    protected String sendCommand(String command) throws IOException {

        if (debug) {
            System.out.println("DEBUG [out]: " + command);
        }
        writer.write(command + "\n");
        writer.flush();
        return readResponseLine();
    }

    public boolean sendAPOP(String username, String pwd) throws IOException{

       String response =  sendCommand("APOP "+username+" "+pwd);

        if (response.startsWith("+OK")){
            if(debug){
                System.out.println("Authentification OK.");
            }
            isAuthentificated = true;
            return true;
        }else{
            if(debug){
                System.out.println("Authentification FAILED.");
            }
            isAuthentificated = false;
            return false;
        }
    }

    public void logout() throws IOException{
        sendCommand("QUIT");
        isAuthentificated = false;
    }

    public int getNumberOfNewMessages() throws IOException {
        String response = sendCommand("STAT");
        String[] values = response.split(" ");
        return Integer.parseInt(values[1]);
    }

    public void sendLIST(){

    }

    public void sendSTAT(){

    }

    public String retrieveMessage(int id){

        return new String();
    }


    protected Message getMessage(int i) throws IOException {
        String response = sendCommand("RETR " + i);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        String headerName = null;
// process headers
        do {
            if (response.startsWith("\t")) {
                continue; //no process of multiline headers
            }
            int colonPosition = response.indexOf(":");
            headerName = response.substring(0, colonPosition);
            String headerValue;
            if (headerName.length() > colonPosition) {
                headerValue = response.substring(colonPosition + 2);
            } else {
                headerValue = "";
            }
            List<String> headerValues = headers.get(headerName);
            if (headerValues == null) {
                headerValues = new ArrayList<String>();
                headers.put(headerName, headerValues);
            }
            headerValues.add(headerValue);
        }while ((response = readResponseLine()).length() != 0);
// process body
        StringBuilder bodyBuilder = new StringBuilder();
        while (!(response = readResponseLine()).equals(".")) {
            bodyBuilder.append(response + "\n");
        }
        return new Message(headers, bodyBuilder.toString());
    }

    public List<Message> getMessages() throws IOException {
        int numOfMessages = getNumberOfNewMessages();
        List<Message> messageList = new ArrayList<Message>();
        for (int i = 1; i <= numOfMessages; i++) {
            messageList.add(getMessage(i));
        }
        return messageList;
    }
}
