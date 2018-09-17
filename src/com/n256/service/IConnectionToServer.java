/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.n256.service;

import com.n256.entity.MonitorProfileData;
import com.n256.entity.ServerStatus;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Nishan
 */
public interface IConnectionToServer extends Remote{
    public MonitorProfileData getMonitorProfileData() throws RemoteException;
    public void registerInServer(IConnectionToClient client) throws RemoteException;
    public void removeFromServer(IConnectionToClient client) throws RemoteException;
    public ServerStatus getServerStatus() throws RemoteException;
    public int getCountOfSensors() throws RemoteException;
    public int getCountOfMonitors() throws RemoteException;
    
}
