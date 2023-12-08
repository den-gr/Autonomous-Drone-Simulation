# %%
import pandas as pd
import numpy as np
import random
import gpxpy
import gpxpy.gpx
from datetime import datetime, timedelta
import math
import statistics
# file_path = 'data/12_01_23-DJI_0994.csv'
file_path = 'merged.json'

df = pd.read_json(file_path)
df = df[502:]

def get_drone_and_zebras_coords(df):
    dff = df[['frame', 'latitude', 'longitude', "id"]]
    dff = dff.groupby(["frame", "latitude", "longitude"])['id'].apply(list).reset_index(name='id_arrays')
    dff["id_arrays"] = dff["id_arrays"].apply(sorted)
    dff = dff[['latitude', 'longitude', "id_arrays"]]

    # dff['id_arrays_tuple'] = dff['id_arrays'].apply(tuple)
    # unique_df = dff.drop_duplicates(subset=['latitude', 'longitude', 'id_arrays_tuple'])
    # unique_df = unique_df.drop('id_arrays_tuple', axis=1)

    return dff.values.tolist()

# %% Export coordinates
coords_d_zb = df[["latitude", "longitude", "altitude", "animals", "compass", "gimbal_pitch"]].values.tolist()

gpx_drone = gpxpy.gpx.GPX()
gpx_zebras = gpxpy.gpx.GPX()
start_time = datetime(2023, 1, 1)

track_d = gpxpy.gpx.GPXTrack()
gpx_drone.tracks.append(track_d)
segment_d = gpxpy.gpx.GPXTrackSegment()
track_d.segments.append(segment_d)

zebras_segments_dict = dict()

ids = set()
for animals in df["animals"]:
    for a in animals:
        ids.add(a[0])
print(ids)

for i in ids:
    track_zb = gpxpy.gpx.GPXTrack()
    gpx_zebras.tracks.append(track_zb)
    segment_zb = gpxpy.gpx.GPXTrackSegment()
    track_zb.segments.append(segment_zb)
    zebras_segments_dict[i] = segment_zb

# Time increment for each point
time_increment = timedelta(seconds=30)

def get_box_center(box):
    xtl, ytl, xbr, ybr = box
    x = round((xtl + xbr)/2)
    y = round((ytl + ybr)/2)
    return x, y

# GIMBAL_PITCH = -16.1
# MAIN_ANGLE = 90 + GIMBAL_PITCH
HORISONTAL_ANGLE = 77.6
VERTICAL_ANGLE = 48.6
width, height = 3840, 2160
x_center = width / 2
y_center = height / 2


def rotate_point(x, y, center_x, center_y, angle_degrees):
    # Convert degrees to radians
    angle_radians = math.radians(angle_degrees)

    # Translate the point to the origin
    translated_x = x - center_x
    translated_y = y - center_y

    # Rotate the translated point
    rotated_x = translated_x * math.cos(angle_radians) - translated_y * math.sin(angle_radians)
    rotated_y = translated_x * math.sin(angle_radians) + translated_y * math.cos(angle_radians)

    # Translate the rotated point back to the original position
    final_x = rotated_x + center_x
    final_y = rotated_y + center_y

    return final_x, final_y


last_altitudes = []
last_compasses = []

coords_d_zb = coords_d_zb[23:33]

# Add each coordinate as a track point with time
for row in coords_d_zb:
    lat, lon, alt, animals, compass, gimbal_pitch = row
    vertical_center_angle = 90 + gimbal_pitch
    point = gpxpy.gpx.GPXTrackPoint(lat, lon, time=start_time)
    segment_d.points.append(point)

    # last_altitudes.append(alt)

    # if(len(last_compasses) > 0 and abs(last_compasses[-1] - compass) > 90):
    #     last_compasses = [compass]
    # else:
    #     last_compasses.append(compass)
    # alt = round(statistics.mean(last_altitudes),1)
    # compass = round(statistics.mean(last_compasses))

    # if(len(last_altitudes) > 4):
    #     last_altitudes.pop(0)
    #     last_compasses.pop(0)
    
    for animal in animals:
        id, behaviour, box, = animal
        box_x, box_y = get_box_center(box)

        y_dis = y_center - box_y
        y_angle_from_center = (y_dis / y_center) * (VERTICAL_ANGLE/2)



        vertical_hypotenuse_len = alt / np.cos(np.deg2rad(vertical_center_angle + y_angle_from_center))
        vertical_oposit_len = np.tan(np.deg2rad(vertical_center_angle + y_angle_from_center)) * alt

        x_dis = x_center - box_x
        x_angle_from_center = (x_dis / x_center) * (HORISONTAL_ANGLE/2)
        horisonal_oposit_len = np.tan(np.deg2rad(abs(x_angle_from_center))) * vertical_hypotenuse_len

        y_offset = vertical_oposit_len * 10e-6
        x_offset = horisonal_oposit_len * 10e-6
        if(x_dis > 0):
            x_offset = x_offset * -1

        rotated_x, rotated_y = rotate_point( lon + x_offset, lat + y_offset, lon, lat, 360-compass)    
        # rotated_x, rotated_y = lon + x_offset, lat + y_offset    

        point_zb = gpxpy.gpx.GPXTrackPoint(round(rotated_y,7), round(rotated_x,7), time=start_time)
        zebras_segments_dict[id].points.append(point_zb)
    
    start_time += time_increment

# %%
# Serialize the GPX data to a file
with open('drone_m/drone.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_drone.to_xml())

with open('zebras_m/zebras.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_zebras.to_xml())

print("GPX file generated with time for each point.")