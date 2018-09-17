/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.n256.service;

import com.n256.entity.SensorData;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Nishan
 */
/*
This interface is used to expose methods we want to call in WeatherMonitor class to outside.
This interface is implemented in Weather Monitor class.
We use this interface in WeatherClient class.
*/
public interface IMonitorUIListener {
    public void informCriticalRainfall(SensorData sensorData);
    public void informCriticalTemperature(SensorData sensorData);
    public void informFaultSensor(SensorData sensorData);
    public void updateMonitorCount(int monitorCount);
    public void updateSensorCount(int sensorCount);
    public void updateSensorList(DefaultTableModel table);
}
