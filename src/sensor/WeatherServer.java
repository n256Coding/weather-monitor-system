/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sensor;

import java.io.PrintWriter;
import java.util.HashMap;
import com.n256.entity.*;
import com.n256.service.IConnectionToClient;
import com.n256.service.IConnectionToServer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Nishan
 */
public class WeatherServer extends UnicastRemoteObject implements IConnectionToServer{
    //This variable is used to store port number
    private static final int PORT = 3000;
    private static String serverVersion = "v1.0 (beta)";
    //ServerStatus object is used to keep details of server
    private static ServerStatus serverStatus = new ServerStatus();
    //sensors hashmap is used to store registered sensors. Key is location of sensor
    private static HashMap<String, SensorData> sensors = new HashMap<>();
    //This hashmap is used to determine activity of sensor in specific time period by compairing Date object.
    private static HashMap<String, Date> sensorActivityCount = new HashMap<>();
    //This is used to store client monitors which connected to this server
    private static volatile LinkedList<IConnectionToClient> clientMonitors = new LinkedList<>();
    
    //This is WeatherServer default constructor. This is defined because it must throw remote exception
    public WeatherServer() throws RemoteException
    {
        super();
    }

    public void start() {
        System.out.println("Weather server "+serverVersion+" has started. Handling your requests on the way");
    }

    //This is main method of this class
    public static void main(String[] args) throws IOException {
        System.out.println("\nWeather server "+serverVersion+" is about to start.");
        
        //This scanner object is used to get user inputs.
        Scanner scn = new Scanner(System.in);
        
        //Thread sleep is not compulsory (Optional). Just for fancy :-)
        try {
            Thread.sleep(800);
        } catch (InterruptedException ex) {
            System.out.println(ex.toString());
        }
        
        //Here each user input will get and assign into serverStatus object
        System.out.print("Enter Status for server: ");
        serverStatus.setStatus(scn.nextLine());
        System.out.print("Enter location of this server: ");
        serverStatus.setLocation(scn.nextLine());
        System.out.print("Enter owner of this server: ");
        serverStatus.setOwner(scn.nextLine());
        System.out.print("Enter a message of server admin: ");
        serverStatus.setMessage(scn.nextLine());
        System.out.print("Enter name of this server: ");
        serverStatus.setName(scn.nextLine());
        System.out.println("");
        System.out.println("OK, We have collected all information right now. \nNow server is configured and getting into up status.");
        
        //This Thread sleep also optional. ( Another fancy work ;-> )
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ex) {
            System.out.println(ex.toString());
        }
        
        //Initialize WeatherServer and ServerSocket
        WeatherServer server = null;
        ServerSocket socketListener = null;
        try
        {
            //Create weather server object because this object is used to register in RMI registry
            server = new WeatherServer();
            
            //Register server object in rmi regisry by using "rmi://localhost/MyServer" url
            Naming.rebind("rmi://localhost/MyServer", server);
            System.out.println("Server is listening on rmi://localhost/MyServer via RMI connection");
            
            //start server socket using given port. This will open server to clients to connect via socket
            socketListener = new ServerSocket(PORT);
        }
        catch(BindException ex)
        {
            //server cannot start because port is in use
            System.out.println("\nERROR: Server is already running on "+PORT+". Use another port or use current running server.");
            System.exit(0);
        }
        catch(ConnectException ex)
        {
            //Rmi registry is not working
            System.out.println("Cannot connect to RMI registry, Check is it working or not");
            System.exit(0);
        }
        catch(ServerException ex)
        {
            //RMI registry not started from project root
            System.out.println("Please start rmi registry from place where the server class stay.");
            System.exit(0);
        }
        
        System.out.println("Server is Ready to handle requests on port "+PORT+"...\n\n");
        
        //This is a internal class runs in different thread. This class will used to check
        //sensor is sent reading in some specific time period. If not recieved, send alerts to clients
        //Here we start that thread.
        new SensorActivityChecker().start();
        
        //This is a internal class runs in different thread. This class is responsible to send sensor readings to
        //each client monitors. Here we start that thread.
        new InformationSender().start();
        
        //This is a internal class runs in different thread. This class is responsible to check sensor reading value
        //gone into critical level. If values reached to critical level, All client monitors will informed by alert.
        //Here we start that thread.
        new SensorValuesChecker().start();
        
            try{
                //Here we have used unlimited loop to start new handler thread for each weather sensor
                //Each time sensor requesting to connect to server, new thread of handler object will started
                while(true)
                {                        
                    new Handler(socketListener.accept()).start();
                }
            }
            catch(SocketException ex)
            {
                System.out.println("Dev:"+ex.toString());
            }
            catch(Exception ex)
            {
                
            }
            finally{
                //socketListener.close();
            }            
    }

    @Override
    public MonitorProfileData getMonitorProfileData() throws RemoteException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /*
        Methods below are implementations of interface. those methods work are invoked by rmi as callback functions.
    */
    
    //This method will return serverStatus object
    @Override
    public ServerStatus getServerStatus() throws RemoteException {
        return serverStatus;
    }

    //This method will return total number of sensors in server
    @Override
    public int getCountOfSensors() throws RemoteException {
        return sensors.size();
    }

    //Return number of weather monitors connected to server
    @Override
    public int getCountOfMonitors() throws RemoteException {
        return clientMonitors.size();
    }

    //Clients (Weather Monitor) call this method, to register that client in server
    @Override
    public void registerInServer(IConnectionToClient client) throws RemoteException {
        synchronized(clientMonitors){
            clientMonitors.add(client);
            SensorData[] sensorDataArray = sensors.values().toArray(new SensorData[0]);
            for(IConnectionToClient aClient :  clientMonitors)
            {
                //get all sensorData objects onto array and send to client monitor via RMI connection.
                aClient.sensorList(sensorDataArray);
            }
        }
    }

    //This method will remove client's (Weather Monitor) data from server
    @Override
    public void removeFromServer(IConnectionToClient client) throws RemoteException {
        synchronized(clientMonitors){
            if(clientMonitors.contains(client))
            {
                clientMonitors.remove(client);               
            }
        }
    }

    //This is a internal class which used to handle weather sensors. 
    //See line 150 for usage of this class
    private static class Handler extends Thread {

        //Here we store socket which sensor trying to connect
        Socket socket = null;
        
        //We are communicating with server and sensor via socket communication with object stream
        //This is the stream objects we use for that purpose
        //streamFromSensor is used to send object to sensor
        //streamToSensor is used to recieve objects from sensor
        ObjectInputStream streamFromSensor = null;       
        ObjectOutputStream streamToSensor = null;
        
        //SensorData is the object we pass through connection to get sensor readings into server.
        SensorData sensorData = null;
        
        //Default constructor of this class
        public Handler() {
        }
        
        //Overloaded constructor to get socket as parameter. It will set accepted socket after accepting connection
        public Handler(Socket socket) {
            //System.out.println("Dev: socket has set in handler");
            this.socket = socket;
        }
        

        //This is the run method of thread. This method will call after thread is started.
        @Override
        public void run() {         
            try {
                //Sets the socket's objectOutputStream
                streamToSensor = new ObjectOutputStream(this.socket.getOutputStream());
                
                //Sets the socket's objectInputStream
                streamFromSensor = new ObjectInputStream(this.socket.getInputStream());
                               
            } catch (IOException ex) {
                System.out.println("Dev:"+ex.getLocalizedMessage());
            }
            
            System.out.println("Detected a new sensor request, Waiting for location of sensor..");
            //while (true) {
                try {
                    //Request sensor's location. (It will send by sensor as SensorData object)
                    sensorData = (SensorData) streamFromSensor.readObject();
                    
                    //Clear objects in output stream
                    streamToSensor.reset();
                    
                    //Check is there already location is available. If location is currently available, Reject sensor by sending 
                    //"sensorrejected" message
                    if(sensors.containsKey(sensorData.getLocation().toLowerCase().trim())){
                        ServerStatus tempStatus = new ServerStatus();
                        tempStatus.setStatus("sensorreject");
                        streamToSensor.writeObject(tempStatus);
                        System.out.println("Duplicate sensor location, Connection Rejected");
                        return;
                    }
                    else
                    {
                        //If location is not avaliable, add sensor into sensors hashmap
                        System.out.println("Sensor from "+sensorData.getLocation()+" has registered in server");
                        
                        //Here we will send serverStatus object (contains details of server) to sensor
                        streamToSensor.writeObject(serverStatus);
                            
                        //Retrive updated sensor readings
                        sensorData = (SensorData)streamFromSensor.readObject();
                        
                        //This synchronized scope is used to make this hashmap thread safe.
                        //So that when some thread is editing this hashmap(sensors). Other threads will not able to access it.
                        synchronized (sensors)
                        {
                            //Add sensor to sensors hashmap. Key is sensor location. Value is sensorData object
                            sensors.put(sensorData.getLocation().toLowerCase().trim(), sensorData);
                            
                            //This hashmap is used to identify sensor active time. Each time sensor sends data. Value (Date) of this hashmap will be updated
                            //So that we can know last active time of each sensor. We ideinty sensor by location (Key)
                            sensorActivityCount.put(sensorData.getLocation().toLowerCase().trim(), new Date());
                            
                            //Clear data of object output stream
                            streamToSensor.reset();                                                                                  
                            
                            updateSensorReadings();
                            
                        }
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            //}
            //After succesfull sensor registreation, Server will start loop to get sensor readings
            while(true)
            {
                try {
                    //read sensorData object from input stream
                    sensorData = (SensorData) streamFromSensor.readObject();                    
                    
                    //update hashmap with updated sensorData object.
                    sensors.put(sensorData.getLocation().toLowerCase(), sensorData);
                }
                catch(SocketException ex)
                {
                    //Inform user if socket connection is lost with sensor
                    System.out.println("Connection with "+sensorData.getLocation()+" sensor lost");
                    synchronized(sensors){
                        sensors.remove(sensorData.getLocation().toLowerCase());
                        
                        //Get all sensor values to sensorData array
                        updateSensorReadings();
                    }
                    break;
                }
                catch (IOException | ClassNotFoundException ex) {
                    System.out.println(ex.toString());
                }
            }
        }
    }
    
    //This class is used to check sensor is active in specific time period
    //See line 138 for start of this object thread
    private static class SensorActivityChecker extends Thread {
        
        //This run method will called when we start SensorActivityChecker thread
        @Override
        public void run()
        {
            //We have used infinite loop to check each sensor activity.
            while(true)
            {
                //Surf all items in sensorActivityCount hashmap. Here we have entry set to get one by on entry   
                for(Entry<String, Date> entry : sensorActivityCount.entrySet())
                {
                    //Check avilable date with current date. Get time difference in milisecond and compare with some value
                    //Example: 1 hour = 3600000
                    if((new Date().getTime() - entry.getValue().getTime()) > 3600000)
                    {                    
                        //If sensor not active within given time notify each monitors
                        //Client monitors are stored in clientMonitors linkedlist
                        //Get them one by one into IConnectionToClient object and call methods remotly using RMI connection
                        for(IConnectionToClient client : clientMonitors)
                        {
                            try {
                                //This method will inform client with faulty sensor details
                                client.inactiveSensorDetected(sensors.get(entry.getKey()));
                            }
                            catch(ConnectException ex)
                            {
                                //If connection is dropped with client, Inform user
                                System.out.println("Client monitor has left from server");
                                synchronized(clientMonitors){
                                    if(clientMonitors.contains(client))
                                    {
                                        clientMonitors.remove(client);
                                    }
                                }
                            }
                            catch (RemoteException ex) {
                                //Inform user if remoteException occured
                                System.out.println(ex.toString());
                            }
                            catch(Exception ex)
                            {
                                //Inform user if other exception occured
                                System.out.println("Unknown Error "+ex.getMessage());
                            }
                        }
                    }
                }
                //Set some delay to next check (Iteration)
                try
                {
                    Thread.sleep(3000);
                }
                catch(InterruptedException ex)
                {
                    System.out.println(ex.toString());
                }
            }
        }
        
    }
    
    //This class will check if sensor reading values goes into critical level. and inform clients (Monitors)
        private static class SensorValuesChecker extends Thread {
        @Override
        public void run()
        {
            //Initialize entryset to store sensors hashmap. See line 391
            Set<Map.Entry<String, SensorData>> sensorsEntrySet = null;
            
            while(true)
            {
                //Make some delay to start iteraion
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch(InterruptedException ex)
                    {
                        System.out.println(ex.toString());
                    }
                    
                    
                //This syncronized scope is used to force access only for single thread. So other thread will not be able to access
                //while using this by this thread.
                synchronized(sensors)
                {
                    sensorsEntrySet = sensors.entrySet();
                }
                
                //Iterate over each entry in entryset
                for(Entry<String, SensorData> entry : sensorsEntrySet)
                {
                    //Get rainfall and temperature from current entry value (This entry's value contains sensorData object)
                    double rainfall = entry.getValue().getRainfall();
                    double temperature = entry.getValue().getTemperature();
                    
                    //Iterate over each client monitor
                    for(IConnectionToClient client : clientMonitors)
                    {                                                          
                        try {
                            //call monitorCount method of client monitor via RMI connection. It will send number of connected monitors in server
                            client.monitorCount(clientMonitors.size());

                            //call sensorCount method of client monitor via RMI connection. It will send number of connected sensors in server
                            client.sensorCount(sensors.size());
                            
                            //If rainfall is grater than 20, Inform monitors by using RMI connection                           
                            if(rainfall > 20)
                            {
                                client.rainfallAlert(entry.getValue());
                            }
                            //If temperature is grater than 35 or less than 20, Inform monitors by using RMI connection
                            if(temperature > 35 || temperature < 20)
                            {
                                client.temperatureAlert(entry.getValue());
                            }
                        }
                        catch(ConnectException ex)
                        {
                            //Inform user if connection is between monitor and server lost
                            System.out.println("Client monitor has left from server");
                            
                            //Remove connection lost monitor from server
                            synchronized(clientMonitors){
                                if(clientMonitors.contains(client))
                                {
                                    clientMonitors.remove(client);
                                }
                            }
                        }
                        catch (RemoteException ex) {
                            System.out.println("Error: "+ex.toString());
                        }
                        catch(Exception ex)
                        {
                            System.out.println("Unknown Error "+ex.getMessage());
                        }
                    }
                    
                }
            }
        
        }
        }
    
    //This class is used to send regular sensor readings to Client monitors
    private static class InformationSender extends Thread{
            //This is run method of This class which runs by starting thread.
            @Override
            public void run()
            {
                //Do action withot stop (Using infinite loop)
                while(true)
                {
                    //Make some delay until server gather sensor data
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(WeatherServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    //Iterate over each client monitor
                    for(IConnectionToClient client : clientMonitors)
                    {
                        try {
                            //call monitorCount method of client monitor via RMI connection. It will send number of connected monitors in server
                            client.monitorCount(clientMonitors.size());
                            
                            //call sensorCount method of client monitor via RMI connection. It will send number of connected sensors in server
                            client.sensorCount(sensors.size());
                            
                            //get all sensorData objects onto array and send to client monitor via RMI connection.
                            SensorData[] sensorDataArray = sensors.values().toArray(new SensorData[0]);
                            client.sensorList(sensorDataArray);
                            
                        } catch (RemoteException ex) {
                            //Inform user if connection between server and client is lost
                            System.out.println("Error sending data to clients");
                        }
                    }
                    //Set some delay to next data refresh iteration.
                    //In our case monitor will informed in each 1 hour time period
                    //So 1 hour = 3600000miliseconds
                    try {
                        Thread.sleep(3600000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(WeatherServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            
            }
    }
    
    private static void updateSensorReadings()
    {
        //Iterate over each client monitor
                    for(IConnectionToClient client : clientMonitors)
                    {
                        try {
                            //call monitorCount method of client monitor via RMI connection. It will send number of connected monitors in server
                            client.monitorCount(clientMonitors.size());
                            
                            //call sensorCount method of client monitor via RMI connection. It will send number of connected sensors in server
                            client.sensorCount(sensors.size());
                            
                            //get all sensorData objects onto array and send to client monitor via RMI connection.
                            SensorData[] sensorDataArray = sensors.values().toArray(new SensorData[0]);
                            client.sensorList(sensorDataArray);
                            
                        } catch (RemoteException ex) {
                            //Inform user if connection between server and client is lost
                            System.out.println("Error sending data to clients");
                        }
                    }
    }
        
}
