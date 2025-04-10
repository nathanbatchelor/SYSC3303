# SYSC3303 Project: Iteration 5
### Group 1

This project simulates a coordinated fire response system using a multithreaded Java application. In this iteration, the simulation now features a graphical user interface (GUI) for real‐time visualization of the fire incident map, drone statuses, and simulation metrics. 
The system creates multiple zones each with its own incident subsystem and coordinates a fleet of drones to extinguish fires, handle faults, and log performance metrics.

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
Drones take off, travel, and extinguish fires in assigned zones. Handle faults, can be turned on or off due to fault type

#### MapUI
Draws the map with zones, active/inactive fires, and drone positions

#### DroneStatusPanel
Displays real-time status information for each drone

#### LegendPanel
Offers an easy-to-read legend for the MapUI.

#### MetricsLogger
Records various simulation metrics.

#### FireIncidentSimulation
Main entry point to launch the simulation.

# Files

Scheduler.java
- Manages fire events and assigns them to drones.
- Stores FireIncidentSubsystems for each zone.
- Manages its own States
  
FireIncidentSubsystem.java
- Reads fire events from Sample_event_file.csv.
- Sends fire events to the Scheduler.
- Manages its own States
  
DroneSubsystem.java
- Handles drone flight, water dropping, and return to base.
- Works with the Scheduler to extinguish fires efficiently.
  
FireEvent.java
- Data structure representing an active fire event.
- Stores zone details, severity, and timestamp.

MapUI.java
- Provides a GUI of the simulation

DroneStatusPanel.java
- Shows drone details including position, current state (IDLE, ON_ROUTE, DROPPING_AGENT, RETURNING, or FAULT), remaining firefighting agent, and battery life.
- Refreshes periodically to ensure up-to-date monitoring

LegendPanel.java
- Displays color codes and symbols representing zones, fire statuses, and drone states for better understanding of the simulation display.

MetricsLogger.java
- Logs simulation duration, distances traveled by drones, and task execution times.
- Exports a summary of metrics to a file (e.g., simulation_metrics.txt) after the simulation ends.

FireIncidentSimulation.java
- Initializes the GUI components (MapUI, DroneStatusPanel, LegendPanel).
- Starts the Scheduler and multiple DroneSubsystem threads.

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




