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


## 4. Create GPS traces



## Example Usage:
- Ensure the dataset is in the correct path.
- Run `explore.py` to preprocess the data.
- Run `rewrite_animals_ids.py` to fix the IDs.
- (Optional) Run `bounding_boxes_animation.py` to create animations.