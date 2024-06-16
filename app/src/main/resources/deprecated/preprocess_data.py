# %%
import pandas as pd
import random
import gpxpy
import gpxpy.gpx
from datetime import datetime, timedelta

# file_path = 'data/12_01_23-DJI_0994.csv'
file_path = 'data/Jan-12th-2023-12-04PM-Flight-Airdata.csv'

df = pd.read_csv(file_path)

def remove_noise(df, filter_window=3):
    cond1 = abs(df['latitude']) < filter_window
    cond2 = abs(df["longitude"] - 37) < filter_window
    condition = cond1 & cond2
    print("Noise coordinates:")
    print((df[~condition])[["latitude", "longitude"]])
    return df[condition]

df = remove_noise(df)

# Define exports functions
def get_drone_coordinates(df, skip):
    dff = df[["latitude", "longitude"]]
    dff = dff.round(6)
    dff = dff.drop_duplicates()
    ris = dff.values.tolist()

    arr = []
    k = 0
    for i in ris:
        if(k%skip == 0): # reduce number of coordinates
            arr.append(i)
        k = k + 1
    return arr

def get_drone_and_zebras_coords(df):
    dff = df[['frame', 'latitude', 'longitude', "id"]]
    dff = dff.groupby(["frame", "latitude", "longitude"])['id'].apply(list).reset_index(name='id_arrays')
    dff["id_arrays"] = dff["id_arrays"].apply(sorted)
    dff = dff[['latitude', 'longitude', "id_arrays"]]

    # dff['id_arrays_tuple'] = dff['id_arrays'].apply(tuple)
    # unique_df = dff.drop_duplicates(subset=['latitude', 'longitude', 'id_arrays_tuple'])
    # unique_df = unique_df.drop('id_arrays_tuple', axis=1)

    return dff.values.tolist()

# %%
coordinates = get_drone_coordinates(df, 4)
coordinates

# %% Export coordinates
coords_d_zb = get_drone_and_zebras_coords(df)

gpx_drone = gpxpy.gpx.GPX()
gpx_zebras = gpxpy.gpx.GPX()
start_time = datetime(2023, 1, 1)

track_d = gpxpy.gpx.GPXTrack()
gpx_drone.tracks.append(track_d)
segment_d = gpxpy.gpx.GPXTrackSegment()
track_d.segments.append(segment_d)

zebras_segments_dict = dict()
for i in df['id'].unique():
    track_zb = gpxpy.gpx.GPXTrack()
    gpx_zebras.tracks.append(track_zb)
    segment_zb = gpxpy.gpx.GPXTrackSegment()
    track_zb.segments.append(segment_zb)
    zebras_segments_dict[i] = segment_zb

def get_zb_offset():
    return random.random() / 10000

# Time increment for each point
time_increment = timedelta(seconds=5)

offsets = dict()
# Add each coordinate as a track point with time
for coordinate in coords_d_zb:
    lat, lon, zb_ids = coordinate
    point = gpxpy.gpx.GPXTrackPoint(lat, lon, time=start_time)
    segment_d.points.append(point)
    
    for id in zb_ids:
        offset = get_zb_offset()
        if(offsets.get(id) == None):
            offsets[id] = get_zb_offset()  * 10
        point_zb = gpxpy.gpx.GPXTrackPoint(lat, lon + offsets[id] + offset, time=start_time)
        zebras_segments_dict[id].points.append(point_zb)
    
    start_time += time_increment


# %%
# Serialize the GPX data to a file
with open('drone/drone.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_drone.to_xml())

with open('zebras/zebras.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_zebras.to_xml())

print("GPX file generated with time for each point.")