# %%
import pandas as pd
import numpy as np
import gpxpy
import gpxpy.gpx
from datetime import datetime, timedelta
import math


def update_alpha(a_v, v):
    return alpha * a_v + (1-alpha) * v

def update_alpha_comass(a_v, v):
    if(a_v < 90 and v > 180):
        alpha = update_alpha(a_v, v-360)
        if(alpha < 0):
            alpha = 360 + alpha
    elif(a_v > 180 and v < 90):
        alpha = update_alpha(a_v, 360+v)
        if(alpha > 360):
            alpha = alpha - 360
    else:
        alpha = update_alpha(a_v, v)
    return alpha

def get_box_center(box):
    xtl, ytl, xbr, ybr = box
    x = round((xtl + xbr)/2)
    y = round((ytl + ybr)/2)
    return x, y

def rotate_point(x, y, center_x, center_y, angle_degrees):
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



file_path = 'data/dataset.json'
# file_path = 'data/test.json'
zebras_output_folder_name = "zebras_m/"
drone_output_folder_name = "drone_m/"

df = pd.read_json(file_path)

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


HORIZONTAL_ANGLE = 80.17 #77.6
VERTICAL_ANGLE = 50.66 #48.6  #47.4715 #48.6
width, height = 3840, 2160
x_center = width / 2
y_center = height / 2

physical_focal_len = 8.4 #mm
# physical_pixel_size = 0.0024 #mm
physical_pixel_size = 0.003512625 #mm


last_altitudes = []
last_compasses = []

lat, lon, alt, animals, compass, gimbal_pitch = coords_d_zb[0]
alpha_lat = lat
alpha_lon = lon
alpha_alt = alt

alpha_x = dict()
alpha_y = dict()
alpha_compass = compass
alpha_gimbal_pitch = gimbal_pitch

alpha = 0.98
# coords_d_zb = coords_d_zb[23:24]

# Add each coordinate as a track point with time
for row in coords_d_zb:
    lat, lon, alt, animals, compass, gimbal_pitch = row
    alpha_gimbal_pitch = update_alpha(alpha_gimbal_pitch, gimbal_pitch)
    gimbal_pitch = alpha_gimbal_pitch


    vertical_center_angle = 90 + gimbal_pitch

    alpha_compass = update_alpha_comass(alpha_compass, compass)
    compass = alpha_compass

    alpha_alt = update_alpha(alpha_alt, alt)
    alpha_lat = update_alpha(alpha_lat, lat)
    alpha_lon = update_alpha(alpha_lon, lon)
    alt = alpha_alt
    lat = alpha_lat
    lon = alpha_lon

    point = gpxpy.gpx.GPXTrackPoint(lat, lon, time=start_time)
    segment_d.points.append(point)
    
    
    for animal in animals:
        id, behaviour, box, = animal
        box_x, box_y = get_box_center(box)

        if(id not in alpha_x.keys()):
            alpha_x[id] = box_x
            alpha_y[id] = box_y
        alpha_x[id] = update_alpha(alpha_x[id], box_x)
        alpha_y[id] = update_alpha(alpha_y[id], box_y)
        box_x = alpha_x[id]
        box_y = alpha_y[id]

        y_dis = y_center - box_y
        x_dis = x_center - box_x

        # y_angle_from_center = np.rad2deg(np.arctan((y_dis * physical_pixel_size)/(physical_focal_len)))
        y_angle_from_center = (y_dis / y_center) * (VERTICAL_ANGLE/2)

            
        vertical_hypotenuse_len = alt / np.cos(np.deg2rad(vertical_center_angle + y_angle_from_center))
        vertical_oposit_len = np.tan(np.deg2rad(vertical_center_angle + y_angle_from_center)) * alt


        ### deprecated
        # vertical_oposit_blind_len = np.tan(np.deg2rad(vertical_center_angle - VERTICAL_ANGLE/2 )) * alt
        # vertical_oposit_len = (alt * 7.58 * 1.425)/(8.4*2160)*(height-box_y) *(1/np.cos(np.deg2rad(vertical_center_angle + y_angle_from_center)))
        # vertical_oposit_len = vertical_oposit_len + vertical_oposit_blind_len

    
        x_angle_from_center = (x_dis / x_center) * (HORIZONTAL_ANGLE/2)
        horisonal_oposit_len = np.tan(np.deg2rad(abs(x_angle_from_center))) * vertical_hypotenuse_len

        y_offset = vertical_oposit_len * 10e-6
        x_offset = horisonal_oposit_len * 10e-6
        if(x_dis > 0):
            x_offset = x_offset * -1

        rotated_x, rotated_y = rotate_point( lon + x_offset, lat + y_offset, lon, lat, 360-compass)    

        point_zb = gpxpy.gpx.GPXTrackPoint(round(rotated_y,7), round(rotated_x,7), time=start_time)
        zebras_segments_dict[id].points.append(point_zb)
    
    start_time += time_increment


# Serialize the GPX data to a file
with open(drone_output_folder_name + 'drone.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_drone.to_xml())

with open(zebras_output_folder_name + 'zebras.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_zebras.to_xml())

print("GPX file generated with time for each point.")

##############################################################################


# %%
