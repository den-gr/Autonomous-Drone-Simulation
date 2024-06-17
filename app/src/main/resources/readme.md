# Drones mission reconstruction

## 1. Data Exploration
The script `explore.py` is responsible for preprocessing the original data from the KABR-telemetry dataset.

- **Dataset Path:** `data/KABR-telemetry/kabr_telemetry_raw.csv`
- **Output:** The script generates 14 JSON files in the `data/jflights` directory. Each file contains data from a single flight mission that is sufficiently long and uninterrupted.


## 2. Fix Zebras' IDs
The KABR-telemetry dataset often has issues with individual IDs. The `rewrite_animals_ids.py` script addresses the most problematic ones.

- **Input:** Files from `data/jflights` produced in the previous step.
- **Output:** New elaborated flight files are saved in the `data/jflights_new_ids/` directory.


## 3. (Optional) Create Animation
To generate a video animation of bounding boxes, run the `bounding_boxes_animation.py` script. 

Add the flight IDs of interest to the `ids` list variable.

**Note:** The process of generating animations is typically quite slow.


## 4. Create GPS Traces
The `prepare_gpx.py` script takes the data from `data/jflights_new_ids/` and approximates the coordinates of visible individuals using a geometrical approach.

- **Outputs:**
  - `drone.gpx`: Contains the GPS trace of the drone.
  - `zebras.gpx`: Contains multiple GPS traces for each identified individual.
  - `fov.gpx`: Contains virtual traces that visualize the direction and size of the Field of View (FOV) of the drone.

### Stable Version
This script allows you to use a "stable version" of reconstruction by modifying the `STABLE_VERSION` boolean variable inside the script.

- **Stable Version Details:** The stable version calculates the mean movement vector of all individuals that do not exhibit moving behaviors (behaviors other than 'Walk', 'Running', or 'Trotting'). These movements are considered noise and are removed from all individuals' data. This adjustment has no effect if all individuals in a sequence of frames are moving.

**Note:** Use the stable version only if there are no zooms or other anomalies during the flight mission.

## 5. (Optional) Run reconstruction in Alchemist
In the project directory, there is already a configuration file `app\src\main\yaml\06-new-reconstruction.yml` that allows running and visualizing the flight reconstruction. 

You may need to modify some variables:
```yml
    flightID: 
        type: ArbitraryVariable
        parameters: [11] # modify this value to specify the flight id that should be executed
    flights_folder:
        language: kotlin
        # modify the directory name to "flights_stable" for be able use the other versions.
        formula: "flights"
```

To execute the simulation run `./gradlew run06` 

How to interact with Alchemist you can read in the main project README.

## Example Usage:
1. Ensure the dataset is in the correct path.
2. Run `explore.py` to preprocess the data.
3. Run `rewrite_animals_ids.py` to fix the IDs.
4. (Optional) Run `bounding_boxes_animation.py` to create animations.
5. Run `prepare_gpx.py` to create the GPS traces.
6. Run `./gradlew run06` to run a reconstruction in Alchemist





