/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.n256.service;

import com.n256.entity.SensorData;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Nishan
 */
public interface IConnectionToClient extends Remote{
    //Normal information
    public void monitorCount(int monitorCount) throws RemoteException;
    public void sensorCount(int sensorCount) throws RemoteException;
    public void sensorList(SensorData[] sensorData) throws RemoteException;
    
    //Criticl Warning messages
    public void rainfallAlert(SensorData sensorData) throws RemoteException;
    public void temperatureAlert(SensorData sensorData) throws RemoteException;
    
    //Warn inactive sensor
    public void inactiveSensorDetected(SensorData sensor) throws RemoteException;
}
