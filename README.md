# SYSC3303 Project: Iteration 1
### Group 1

This program simulates a coordinated fire response system using threads in Java, where drones extinguish fires based on real-time event data.

## Overview: 
#### FireIncidentSubsystem:
Each fire zone has its own FireIncidentSubsystem.
Reads fire events from an input file (Sample_event_file.csv).
Sends fire event data to the Scheduler.

#### Scheduler:
Manages fire event queues in FIFO order (first-come, first-served).
Assigns fire incidents to available drones.
Removes fire events when fully extinguished.

#### DroneSubsystem:
Drones take off, travel, and extinguish fires in assigned zones.

# Files

Scheduler.java
- Manages fire events and assigns them to drones.
- Stores FireIncidentSubsystems for each zone.
  
FireIncidentSubsystem.java
- Reads fire events from Sample_event_file.csv.
- Sends fire events to the Scheduler.
  
DroneSubsystem.java
- Handles drone flight, water dropping, and return to base.
- Works with the Scheduler to extinguish fires efficiently.
  
FireEvent.java
- Data structure representing an active fire event.
- Stores zone details, severity, and timestamp.


# How to Run
Open the submitted "Source Code" Folder in IntelliJ

We have included a test file for running the simulation in the input folder

To run the Simulation: 
1. Control + Shift + R
2. Right Click FireIncidentSimulation.java and press run
3. Click the run button in the top right corner of the IDE

If you would like to use another input file, add it to the input folder and change the file name in FireIncidentSimulation
```
String fireIncidentFile = "input//your_file_here.csv
```




