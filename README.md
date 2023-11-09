# Autonomic-Dron-Simulation
The repository contains two version of drone paths reproduction. 

The both versions are based on the coordinates extracted with a python script 
that is not included in this repository

### Trace based version:
`app/src/main/yaml/03-gpsTrac.yml`    
1. To execute `./gradlew run03`
2. Zoom to the points
3. Click `R` and then `P`

### Protelis based version

`app/src/main/yaml/02-protelis.yml`

1. To execute `./gradlew run02`
2. Zoom to the points
3. Click `P`

This code use a custom action that ignores streets: `CustomTargetMapWalker` located in `app/src/main/java/`