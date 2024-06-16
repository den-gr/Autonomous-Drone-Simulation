# %%
import pandas as pd
import json
from dateutil import parser
from utils import create_folder_if_not_exists

LATITUDE = "latitude"
LONGITUDE = "longitude"
ALTITUDE = "altitude"
ALTITUDE_SEA = "altitude_above_seaLevel"

file_path = 'data/KABR-telemetry/kabr_telemetry_raw.csv'
df = pd.read_csv(file_path)

# Renaming, type fixing and value rounding.
df = df.rename(columns={
    "latitude_y":  LATITUDE, 
    "longitude_y": LONGITUDE, 
    " compass_heading(degrees)": "compass_heading",
    "gimbal_pitch(degrees)": "gimbal_pitch"
    })
df[ALTITUDE_SEA] = round(df["altitude_above_seaLevel(feet)"] * 0.3048, 1)
df["height_sonar"] = round(df["height_sonar(feet)"] * 0.3048, 1)
df["height_above_takeoff"] = round(df["height_above_takeoff(feet)"] * 0.3048, 1)

df[LATITUDE] = df[LATITUDE].round(6)
df[LONGITUDE] = df[LONGITUDE].round(6)

df['id'] = df['id'].astype(int)
df['xtl'] = df['xtl'].astype(int)
df['ytl'] = df['ytl'].astype(int)
df['xbr'] = df['xbr'].astype(int)
df['ybr'] = df['ybr'].astype(int)

# Remove currently not used columns
df = df.drop(columns=[
        # 'frame', 
        # 'id', 
        # 'date_time', 
        # 'latitude_y', 
        # 'longitude_y', 
        # 'altitude',
        'height_above_takeoff(feet)',
        'height_above_ground_at_drone_location(feet)',
        'ground_elevation_at_drone_location(feet)',
        'altitude_above_seaLevel(feet)', 
        'height_sonar(feet)', 
        'altitude(feet)',
        'speed(mph)', 
        'distance(feet)', 
        'mileage(feet)', 
        'satellites',
        'gpslevel', 
        'voltage(v)', 
        'max_altitude(feet)', 
        'max_ascent(feet)',
        'max_speed(mph)', 
        'max_distance(feet)', 
        ' xSpeed(mph)', 
        ' ySpeed(mph)',
        ' zSpeed(mph)', 
        # 'compass_heading', 
        ' pitch(degrees)',
        ' roll(degrees)', 
        'gimbal_heading(degrees)', 
        # 'gimbal_pitch(degrees)',
        'gimbal_roll(degrees)', 
        # 'xtl', 
        # 'ytl', 
        # 'xbr',
        # 'ybr', 
        # 'label',
        # 'behaviour', 
        'source', 
        'mission id'
        ]
    )

dff = df.copy()

# Includes real world elevation to the dataframe
def add_external_ground_elevation_column_to_df(df):
    with open('ground_seaLevel_map.json', 'r') as f:
        ground_json = json.load(f)
    ground_map = {eval(key): value for key, value in ground_json.items()}

    def add_ground_level(row):
        return ground_map[(row["latitude"], row["longitude"])]

    df["ground_elevation"] = df.apply(add_ground_level, axis=1)
    return df

dff = add_external_ground_elevation_column_to_df(dff)

# Calculates altitude based on real world ground elevation.
def calc_alt(row):
    return  round(row["altitude_above_seaLevel"] - row['ground_elevation'], 1)#- row["height_sonar"]

dff["above_ground_altitude"] = dff.apply(calc_alt, axis=1)

#The frames enumeration in each DJI clip starts from zero (each clip max 5 minutes), after concatenation of consecutive clips we should fix enumeration of frames
def fix_frames_enumeration(dataframe):
    new_df = dataframe.copy().reset_index(drop=True)
    prev_frame_value = new_df.loc[0]["frame"]
    offset = 0
    for index, row in dataframe.reset_index(drop=True).iterrows():
        current_frame_value = row["frame"]
        if(current_frame_value < prev_frame_value):
            offset = prev_frame_value + 1
            i = index
            while i < len(new_df):
                if(current_frame_value == new_df.loc[i]['frame']):
                    new_df.loc[i, 'frame'] = offset
                    i += 1
                else:
                    break
        prev_frame_value = new_df.loc[index]['frame']
    return new_df

def save_segment(segment, count):
    if(not segment['frame'].is_monotonic_increasing):
        segment = fix_frames_enumeration(segment)
        print(f"fix enumeration of sequence {count}")
    create_folder_if_not_exists("data/jflights")
    # segment.to_csv(f"data/flights/flight_{count}.csv", index=False) # Save files in CSV format
    segment.to_json(f"data/jflights/flight_{count}.json", index=False)

def get_drone_and_zebras_coords(df):
    new_df = df.copy()
    columns = [x for x in dff.columns.values if x not in ["id", "xtl", "ytl", "xbr", "ybr", "behaviour"]]
    new_df["box"] = new_df.apply(lambda row: (row["id"], row["behaviour"], (row["xtl"], row["ytl"], row["xbr"], row["ybr"])), axis=1)
    new_df = new_df.groupby(columns)["box"].apply(list).reset_index(name='animals')
    return new_df

dff = dff.sort_values(by=["date_time", 'frame']).reset_index(drop=True)
# %%
prevIndx = 0
count = 1
prev_time = parser.parse(dff.loc[0]['date_time'])
for index, row in dff.iterrows():
    if((parser.parse(row['date_time']) - prev_time).seconds > 2 and index > 1):
        sequence_distance = index-prevIndx
        if(sequence_distance > 5000): 
            df_segment = dff.loc[prevIndx: index - 1]
            if "Giraffe" in df_segment["label"].unique():
                print("ignore flight sequence with giraffes")
            else:
                print(f"Creating flight sequence {count}: records {sequence_distance}, range [{prevIndx}, {index - 1}] ")
                df_segment = get_drone_and_zebras_coords(df_segment).sort_values(by=["date_time", 'frame']).reset_index(drop=True)
                save_segment(df_segment, count)
                count = count + 1
        prevIndx = index
        
    prev_time = parser.parse(row['date_time'])
print("Last sequence")

df_segment = get_drone_and_zebras_coords(dff[prevIndx: dff.index[-1]]).sort_values(by=["date_time", 'frame']).reset_index(drop=True)
save_segment(df_segment, count)

# %% Optional
# dff.to_csv('data/kabr_telemetry_clean.csv', index=False)

# %% This code creates a json file with all unique coordinates in dataset and their coresponding ground elevation above sea level.
# Require lunching a docker server with a database (see https://www.opentopodata.org/datasets/aster/)

# altitude_map = {}
# def make_request(row):
#     # Modify this URL construction based on your requirement
#     url = f"http://localhost:5000/v1/aster30m?locations={row['latitude']},{row['longitude']}"
#     response = requests.get(url)
#     # Process the response as needed, or you can directly return it
#     ris = response.json()["results"][0]["elevation"]
#     altitude_map[(row['latitude'],row['longitude'])] = ris
#     return ris


# df[["latitude", "longitude"]].drop_duplicates().apply(make_request, axis=1)
# with open('ground_seaLevel_map.json', 'w') as f:
#     json.dump({f"{key}": value for key, value in altitude_map.items()}, f)