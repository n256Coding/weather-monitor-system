/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sensor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.n256.entity.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.rmi.ConnectException;
import java.util.Scanner;

/**
 *
 * @author Nishan
 */
public class WeatherSensor {
    //Port of server to connect socket communication
    private static final int PORT = 3000;
    
    //Initialize socket object
    Socket socket = new Socket();
    
    //Declaring input and output streams of socket communication
    ObjectOutputStream streamToServer = null;
    ObjectInputStream streamFromServer = null;
    
    //This is to store location of this sensor
    String sensorLocation = "";
    
    //Default constructor of this class
    public WeatherSensor()
    {
    }
            
            
    public void enableSensor()
    {
        ServerStatus serverStatus = new ServerStatus();
        try 
        {
            System.out.println("Sensor enabled");
            System.out.print("Searching weather server on port "+PORT+"...");
            Thread.sleep(800);
            
            //Create socket object for localhost port
            socket = new Socket("localhost", PORT);
            System.out.println("Server detected, Communicating with server...");
            
            /*Initialize Output stream and input stream
                Here is used Object stream so I can send POJO (Plain Old Java Object)s via Socket
                Connection.
                ObjectOutputStream is used to send data
                ObjectInputStream is used to recieve data
            */
            streamToServer = new ObjectOutputStream(socket.getOutputStream());
            streamFromServer = new ObjectInputStream(socket.getInputStream());
            
            /*
            After connecting to object streams, User prompts to enter location of this sensor.
            because server will use location as key to identify that sensor.
            Here scn Scanner object used to read user's input where used as sensor location.
            and save location in sensorLocation class variable.
            */
            Scanner scn = new Scanner(System.in);
            SensorData dataToSend = new SensorData();
            System.out.print("Enter location of this sensor: ");
            sensorLocation = scn.nextLine();
            
            //dataToSend is a POJO to store information of weather sensor
            //Set sensor location in that object. Server need location to register sensor 
            //in server.
            dataToSend.setLocation(sensorLocation);
            
            /*
            reset() method of streamObject is used to clear data in current stream
            If this method not called, objectStream is sending same object again and again even we write new object
            */
            streamToServer.reset();
            
            //After clearing data in stream, We will write new object into stream
            streamToServer.writeObject(dataToSend);
            
            //After sending sensor data, server will analyse and send status message to sensor
            //Sensor is waiting untill that response
            //In here, ServerStatus is also a POJO used to store server data.
            //After reading server's output, Sensor will check server status.
            serverStatus = (ServerStatus) streamFromServer.readObject();
            
            //If server status is "sensorreject", That mean server not accept that sensor. 
            //Reason for the rejection is there is a sensor currently registred by location which we 
            //going to add. So that terminates the process of sensor with a message
            if(serverStatus.getStatus().equals("sensorreject"))
            {
                System.out.println("Server rejected the connection, Try again");
                return;
            }
            
            //If server status is not "sensorreject", That means server accepted the connection
            //So information in serverStatus object will displayed in sensor's console
            System.out.println("WEATHER SERVER---------------------");
            System.out.println(" * Server Name: "+serverStatus.getName());
            System.out.println(" * Server Location: "+serverStatus.getLocation());
            System.out.println(" * Admin's Message: "+serverStatus.getMessage());
            System.out.println(" * Server Status: "+serverStatus.getStatus());
            SensorData data = new SensorData();
        } 
        catch (ClassNotFoundException | InterruptedException | IOException ex) {
            System.out.println(ex.toString());
        }
        //Here we create SensorData object to send all sensor data
        SensorData dataToSend = new SensorData();
        System.out.println("Sensor enabled and working properly");
        
        //This random class is used to emulate IoT sensors. It will generate random numbers
        Random random = new Random();
        
        //Initial values of each sensor
        
        //double rainfall = 15;
        double rainfall = 50;
        //double temperature = 28;
        double temperature = 15;
        double airpressure = 50;
        double humidity = 70;
        
        //Following logic will be used to update sensor readings.
        //random.nextDouble() will generate number between 1.0 and 0.0
        while(true)
        {   
            if(random.nextDouble() < 0.5)
                rainfall -= 0.5;
            else
                rainfall += 0.5;
            if(random.nextDouble() < 0.5)
                temperature -= 0.5;
            else
                temperature += 0.5;
            if(random.nextDouble() < 0.5)
                airpressure -= 0.5;
            else 
                airpressure += 0.5;
            if(random.nextDouble() < 0.5)
                humidity -= 0.5;
            else
                humidity += 0.5;
            
            //Add new sensor readings one by one to SensorData object
            dataToSend.setLocation(sensorLocation);
            dataToSend.setRainfall(rainfall);
            dataToSend.setTemperature(temperature);
            dataToSend.setAirPressure(airpressure);
            dataToSend.setHumidity(humidity);
            
            try {
                //Clear data in objectStream
                streamToServer.reset();
                
                //Write updated object into stream
                streamToServer.writeObject(dataToSend);
                streamToServer.flush();
            }
            catch(SocketException ex)
            {
                //Socket exception occured, Inform user.
                System.out.println("Server was down, Connection lost.");
                return;
            }
            catch(ConnectException ex)
            {
                //This occur when refusing connection to server
                System.out.println("Connection refused to server");
            }
            catch(NullPointerException ex)
            {
                //NullPointerException will happen when server not running on PORT
                System.out.println("Server not found in port "+PORT);
                return;
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            
            //Show sensor readings in sensor console
            System.out.println("Rainfall: "+dataToSend.getRainfall()+", Temperature: "+dataToSend.getTemperature()+", Humidity: "+dataToSend.getHumidity()+", Air Pressure: "+dataToSend.getAirPressure());
            
            //This statement is in infinite loop. So to make delay in each iteration. we use this Thread.sleep method.
            //We will input time in miliseconds and parameter
            //Here we use 5 minute delay = 300000 miliseconds
            try {
                Thread.sleep(300000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /*
    This is the main method of this class.
    */
    public static void main(String[] args)
    {
        System.out.println("Starting weather sensor...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            System.out.println(ex.toString());
        }
        System.out.println("Enabling sensor ");
        WeatherSensor sensor = new WeatherSensor();
        sensor.enableSensor();
    }
}
