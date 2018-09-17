# Weather Monitor System
This is a Java RMI based weather monitor system. This system uses weather sensor data fullfill the goal. As the demo, here we have used some dummy sensors (java application) but can be replaced with real sensors.

### Technologies used
* Java programming language
* Java RMI (Remote Method Invocation)

### System requirements
* Java JDK 8
* Netbeans IDE (Tested on version 8.2)

### Steps to run the system
Clone or download this project in to your local machine. Then open command prompt (or Terminal) and navigate into src folder(directory) in project folder. Issue following command
```sh
$ start rmiregistry
```

##### Open project in netbean
#### Run WeatherServer.java
    This is the main server of this system. There is only one server instances should be within the system. Input data requested by program in console (You can type any thing)

#### Run WeatherSensor.java
    This class will be used to emulate a real weather sensor. For demonstration, it will generate some random values as sensor readings. To start a sensor, it needs the location (Usually the the city which going to place the sensor) of the sensor. So, in server, sensor will identified using the name of location. Input any location name in console to start a sensor. You can create any number of sensors. Just simply run multiple instances of WeatherSensor.java file. Make sure to give unique location name for each sensor.
	- Location: Kurunegala

#### Run MainLogin.java
    Enter Credentials
	- Username: admin
	- Password: 123
	
#### Click "Log into Monitor" button.
    System will show WeatherMonitor GUI application.
    Enter Name and Location of monitor.
    - Name:     Monitor1
    - Location: Malabe

#### Click "Commit" button.
    Then you will be able to see realtime weather readings of sensors in different locations.