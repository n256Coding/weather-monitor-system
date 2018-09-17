/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.n256.monitor;


import com.n256.entity.MonitorProfileData;
import com.n256.entity.SensorData;
import com.n256.entity.ServerStatus;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import com.n256.service.*;
import java.awt.Frame;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Nishan
 */

/*
* This class is stay as middle man between WeatherServer and WeatherMonitor to solve a problem we got while
* developing this system. Problem is WeatherMonitor is GUI application. So it extends JFrame class. But to work
* with RMI registry, we need to extend our class with UnicastRemoteObject. But as Java not supports for multiple
* extends (inheritance), This class will recieve data from WeatherServer and then send them to WeatherMonitor
*/
public class WeatherClient extends UnicastRemoteObject implements IConnectionToClient{
    //This is a object to store information of WeatherMonitor. Looks like entity class
    private static MonitorProfileData monitorData = new MonitorProfileData();
    
    //Initialize interface for Access methods of WeatherServer
    IConnectionToServer serverConnection = null;
    
    //Initialize inteface to access methods in WeatherMonitor
    IMonitorUIListener monitorUI = null;
    
    private static int count = 0;
    
    //We have to make default constructor throw RemoteException.
    //So we declare it here.
    public WeatherClient() throws RemoteException
    {
        //Call constructor of super(parent) class
        super();        
    }
    
    //We use this overloaded constructor to get reference to WeatherMonitor into this class.
    //When we start weatherMonitor, We call this constructor and pass object of WeatherMonitor class as parameter.
    public WeatherClient(IMonitorUIListener monitorUI) throws RemoteException
    {
        //Set monitorUI object from parameter
        this.monitorUI = monitorUI;
        
        
        //create connection to server by calling this method
        createConnectionToServer();
        
    }
    
    //This method will create connection to server
    public boolean createConnectionToServer()
    {
        try {
            //Find server object in RMI registry using registration url.
            //in our example it is "//localhost/MyServer" which we give when staring the server
            serverConnection =  (IConnectionToServer) Naming.lookup("//localhost/MyServer");
            
            //We call registerInServer method in WeatherServer via RMI connection and pass object of this(WeatherClient) class
            serverConnection.registerInServer(this);
            
            //Return true if no exception
            return true;
            
        } catch (NotBoundException | MalformedURLException | RemoteException ex) {
            //Catch probable exception and return false
            System.out.println(ex.toString());
            return false;
        }
    }
    
    //We can use this method to get information of WeatherServer currently connected.
    //ServerStatus is object which stored information of server.
    public ServerStatus getServerData()
    {
        try {
            //we will call getServerStatus method of WeatherServer via RMI connection. It will return ServerStatus object
            //with details of Server.
            return serverConnection.getServerStatus();
        } catch (RemoteException ex) {
            
            //Catch if any exception while communicating and return null value.
            System.out.println(ex.toString());
            return null;
        }
    }
    
    //This method will return current connected monitor count in weather server.
    public int getMonitorCount(){
        try {
            //As above method, call getCountOfMonitors method in WeatherServer via RMI
            return serverConnection.getCountOfMonitors();
        } catch (RemoteException ex) {
            //Catch any exception while communication and return 0.
            Logger.getLogger(WeatherClient.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    //This method return sensor count in weather server
    public int getSensorCount()
    {
        try {
            //call getCountOfSensors method in WeatherServer via RMI
            return serverConnection.getCountOfSensors();
        } catch (RemoteException ex) {
            //Catch any exception while communication and return 0.
            Logger.getLogger(WeatherClient.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    /*
    * Except run() method, other @Override annotationed methods are implementaion of IConnectionToClient.
    * those methods will called by WeatherServer via RMI communication.
    * inside of that methods, this class call methods in IMonitorUIListener interface to send data to WeatherMonitor. because this is a middle man class
    */    


    //If reainfall level is reach to it's critical stage, Server will invoke this method
    @Override
    public void rainfallAlert(SensorData sensorData) throws RemoteException {
        
        //call informCriticalRainfall method in WeatherMonitor class (GUI application)
        monitorUI.informCriticalRainfall(sensorData);
    }

    //If themperature level is reached to it's critical stage, Server will invoke this method
    @Override
    public void temperatureAlert(SensorData sensorData) throws RemoteException {
        
        //call informCriticalTemperature method in WeatherMonitor class (GUI application)
        monitorUI.informCriticalTemperature(sensorData);
    }


    /*//This is run method which the implementation of runnable interface. It will runs infinitly
    //@Override
    public void run() {
        while(true)
        {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                System.out.println(ex.toString());
            }
        }
    }*/

    //If some sensor didn't send data (readings) in specific time period, Server will call this method via RMI
    @Override
    public void inactiveSensorDetected(SensorData sensor) throws RemoteException {
        
        //call informFaultSensor method of WeatherMonitor class along with sensorData
        monitorUI.informFaultSensor(sensor);
    }

    //Server will periodically call this method to send monitor count to WeatherMonitor
    @Override
    public void monitorCount(int monitorCount) throws RemoteException {
        
        //call updateMonitorCount method of WeatherMonitor class along with number of monitors
        monitorUI.updateMonitorCount(monitorCount);
    }

    //Like above method, This server will call this method to inform about sensor count in server
    @Override
    public void sensorCount(int sensorCount) throws RemoteException {
        
        //call updateSensorCount method of WeatherMonitor class along with number of sensors
        monitorUI.updateSensorCount(sensorCount);
    }

    //Server will call this method periodically to inform regular sensor reading to Monitors
    @Override
    public void sensorList(SensorData[] sensorData) throws RemoteException {
        try
        {     
            //Weather monitor has a table to show sensor reading of each location. Data for that table is created in here.
            //Here we create a defaultTableModel and pass it to monitor to show in table.
            
            //initlaize a DefaultTableModel
            DefaultTableModel table = new DefaultTableModel();
            
            //Add columns to default table model
            table.addColumn("Location");
            table.addColumn("Temperature");
            table.addColumn("Rainfall");
            table.addColumn("Humidity");
            table.addColumn("Air Pressure");
            
            //Add data rows to table
            for(int i=0; i<sensorData.length; i++)
            {
                table.addRow(new Object[]{sensorData[i].getLocation(), sensorData[i].getTemperature(), sensorData[i].getRainfall(), sensorData[i].getHumidity(), sensorData[i].getAirPressure()});
            }
            
            //Pass created table to monitor by calling updateSensorList method
            monitorUI.updateSensorList(table);
        }
        catch(Exception ex)
        {
            //Catch if there any exception.
            System.out.println(ex.toString());
        }
    }
    
}
