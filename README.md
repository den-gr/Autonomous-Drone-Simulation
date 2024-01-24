# Autonomous Drone Simulation
The repository contains two version of drone paths reproduction in [Alchemist](https://github.com/AlchemistSimulator) simulator.

## Prerequisites
This project requires a Java version capable of executing Gradle (Java 8+).

## Instruction
This project use [Gradle](https://gradle.org) to resolve dependencies and execute the simulator. To this end, it comes with a pre-configured gradle build.

### Importing the repository
The preferred way to import this repository is via the [Git SCM](https://git-scm.com):
- `git clone https://github.com/DenGuzawr22/Autonomous-Drone-Simulation.git`

Alternatively, the repository can be [downloaded as compressed archive](https://github.com/DenGuzawr22/Autonomous-Drone-Simulation/archive/refs/heads/master.zip). The archive should then get unpacked, and a terminal should be prepared pointing to the directory containing the build.gradle.kts file.


## Execution
Gradle can be launched by relying on the provided wrapper script. On Unix (Linux, MacOS X, WSL, etc.) or on Bash emulators (Git Bash, Cygwin 64): `./gradlew <taskName>` On Windows' cmd.exe or PowerShell: `gradlew.bat <taskName>` In the remainder of this guide, will be used the Unix syntax.

### Executing simulations
Tasks are named after the corresponding YAML file, with the pattern `run<name-of-simulation-file>`. Gradle supports shortened names: launching `./gradlew run00` is the same as `./gradlew run00-deployment-in-three-points`, unless there is some ambiguity.

Note that the first launch will be slow, since Gradle will download all the required files. They will get cached in the user's home folder (as per Gradle normal behavior) and thus subsequent execution will be much faster.

### Run a simulation with herds and drones
To execute `./gradlew run11` and then press `P` to start

### Run herd movement simulation:
To execute `./gradlew run00` and then press `P` to start.

Press `R` to slow down the simulation

### Run drone and zebras movement reconstruction
To execute `./gradlew run05` and then pres `P` to start

### Drone movement reconstruction: GPS Trace based version:
Simulation configuration: `app/src/main/yaml/03-gpsTrac.yml`    
1. To execute `./gradlew run03`
2. Click `R` to activate real-time mode and then click `P` to start the simulation

### Drone movement reconstruction: protelis based version

Simulation configuration: `app/src/main/yaml/02-protelis.yml`

1. To execute `./gradlew run02`
2. Click `P` to start the simulation


Technical note: this implementation use a custom action that ignores streets: `CustomTargetMapWalker` located in `app/src/main/java/`

### Using the graphical interface

Press <kbd>P</kbd> to start the simulation.
The available UI bindings are the following:

| Key binding             | Active         | Effect                                                                |
| ------------------------| -------------- | --------------------------------------------------------------------- |
| <kbd>L</kbd>            | always         | (En/Dis)ables the painting of links between nodes                     |
| <kbd>M</kbd>            | always         | (En/Dis)ables the painting of a marker on the closest node            |
| <kbd>Mouse pan</kbd>    | in normal mode | Moves around                                                          |
| <kbd>Mouse wheel</kbd>  | in normal mode | Zooms in/out                                                          |
| <kbd>Double click</kbd> | in normal mode | Opens a frame with the closest node information                       |
| <kbd>Right click</kbd>  | in normal mode | Enters screen rotation mode                                           |
| <kbd>P</kbd>            | always         | Plays/pauses the simulation                                           |
| <kbd>R</kbd>            | always         | Enables the real-time mode                                            |
| <kbd>Left arrow</kbd>   | always         | Speeds the simulation down (more calls to the graphics)               |
| <kbd>Right arrow</kbd>  | always         | Speeds the simulation up (less calls to the graphics)                 |
| <kbd>S</kbd>            | always         | Enters / exits the select mode (nodes can be selected with the mouse) |
| <kbd>O</kbd>            | in select mode | Selected nodes can be moved by drag and drop                          |
| <kbd>E</kbd>            | in select mode | Enters edit mode (to manually change node contents)                   |
