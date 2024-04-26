# %%
import pandas as pd
import numpy as np
import gpxpy
import gpxpy.gpx
from datetime import datetime, timedelta
import math
import os

from utils import *

def update_alpha(a_v, v):
    return ALPHA * a_v + (1-ALPHA) * v

def update_compass_alpha(alpha_v, v):
    alpha_v_x, alpha_v_y = angle_to_vector(alpha_v)
    v_x, v_y = angle_to_vector(v)
    x = update_alpha(alpha_v_x, v_x)
    y = update_alpha(alpha_v_y, v_y)
    return vector_to_angle(x, y)


file_name = 'flight_12'
file_path = f'data/jflights_new_ids/{file_name}.json'
output_folder = "flights"
# output_folder = "flights_new_ids"


zebras_output_folder_name = f"{output_folder}/{file_name}_zebras/"
drone_output_folder_name = f"{output_folder}/{file_name}_drones/"
fov_output_folder_name = f"{output_folder}/{file_name}_fov/"
if not os.path.exists(zebras_output_folder_name):
    os.makedirs(zebras_output_folder_name)
if not os.path.exists(drone_output_folder_name):
    os.makedirs(drone_output_folder_name)
if not os.path.exists(fov_output_folder_name):
    os.makedirs(fov_output_folder_name)


df = pd.read_json(file_path)

coords_d_zb = df[["latitude", "longitude", "altitude", "animals", "compass_heading", "gimbal_pitch"]].values.tolist()


def create_gpx_segment(gpx):
    track = gpxpy.gpx.GPXTrack()
    gpx.tracks.append(track)
    segment = gpxpy.gpx.GPXTrackSegment()
    track.segments.append(segment)
    return segment

def get_gpx_point(coordinates, time):
    lon, lat = coordinates
    return gpxpy.gpx.GPXTrackPoint(round(lat, 5), round(lon, 5), time=time)
    

gpx_drone = gpxpy.gpx.GPX()
gpx_zebras = gpxpy.gpx.GPX()
gpx_fov = gpxpy.gpx.GPX()
start_time = datetime(2023, 1, 1)

segment_d = create_gpx_segment(gpx_drone)

POINT_IN_FOV = 3

fov_segments1 = []
fov_segments2 = []
for i in range(POINT_IN_FOV):
    fov_segments1.append(create_gpx_segment(gpx_fov))
    fov_segments2.append(create_gpx_segment(gpx_fov))


zebras_segments_dict = dict()

ids = set()
for animals in df["animals"]:
    for a in animals:
        ids.add(a[0])
print("The number of ids", len(ids))

for i in ids:
    zebras_segments_dict[i] = create_gpx_segment(gpx_zebras)

# Time increment for each point
time_increment = timedelta(seconds=1)


HORIZONTAL_ANGLE = 80.17 # New
# HORIZONTAL_ANGLE = 77.6
VERTICAL_ANGLE = 50.66 #48.6  #47.4715 #48.6
width, height = 3840, 2160
x_center = width / 2
y_center = height / 2

physical_focal_len = 8.4 #mm
# physical_pixel_size = 0.0024 #mm
physical_pixel_size = 0.003512625 #mm
ALPHA = 0

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


# coords_d_zb = coords_d_zb[23:24]

# Add each coordinate as a track point with time
for row in coords_d_zb:
    lat, lon, alt, animals, compass, gimbal_pitch = row

    alpha_compass = update_compass_alpha(alpha_compass, compass)
    compass = alpha_compass

    alpha_gimbal_pitch = update_alpha(alpha_gimbal_pitch, gimbal_pitch)
    alpha_alt = update_alpha(alpha_alt, alt)
    alpha_lat = update_alpha(alpha_lat, lat)
    alpha_lon = update_alpha(alpha_lon, lon)
    gimbal_pitch = alpha_gimbal_pitch
    alt = alpha_alt
    lat = alpha_lat
    lon = alpha_lon

    segment_d.points.append(get_gpx_point((lon, lat), start_time))

    fov_angle_1 = (compass + (HORIZONTAL_ANGLE / 2)) % 360
    fov_angle_2 = (compass - (HORIZONTAL_ANGLE / 2)) % 360
    offset = 1e-4


    for i, k in enumerate([1, 3, 5]):
        fov_p1 = rotate_point( lon, lat + offset * k, lon, lat, 360-fov_angle_1)
        fov_p2 = rotate_point( lon, lat + offset * k, lon, lat, 360-fov_angle_2)
        fov_segments1[i].points.append(get_gpx_point(fov_p1, start_time))
        fov_segments2[i].points.append(get_gpx_point(fov_p2, start_time))

    
    vertical_center_angle = 90 + gimbal_pitch

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

        y_offset = vertical_oposit_len / ((40075* math.cos(math.radians(lat))/360) * 1000)
        x_offset =  horisonal_oposit_len / 111320 
        if(x_dis > 0):
            x_offset = x_offset * -1


        point_zb = rotate_point( lon + x_offset, lat + y_offset, lon, lat, 360-compass) 
        zebras_segments_dict[id].points.append(get_gpx_point(point_zb, start_time))
    
    start_time += time_increment


# Serialize the GPX data to a file
with open(drone_output_folder_name + 'drone.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_drone.to_xml())

with open(zebras_output_folder_name + 'zebras.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_zebras.to_xml())

with open(fov_output_folder_name + 'fov.gpx', 'w') as gpx_file:
    gpx_file.write(gpx_fov.to_xml())

print("GPX file generated with time for each point.")

##############################################################################


# %%
