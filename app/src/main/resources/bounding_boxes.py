# %%
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np
import matplotlib.animation as animation
from mpl_toolkits.axes_grid1 import make_axes_locatable

import pandas as pd
import random

file_path = 'data/12_01_23-DJI_0994.csv'

df = pd.read_csv(file_path)

def remove_noise(df, filter_window=3):
    cond1 = abs(df['latitude']) < filter_window
    cond2 = abs(df["longitude"] - 37) < filter_window
    condition = cond1 & cond2
    print("Noise coordinates:")
    print((df[~condition])[["latitude", "longitude"]])
    return df[condition]

df = remove_noise(df)

def get_drone_and_zebras_coords(df):
    dff = df[['frame', 'latitude', 'longitude', "altitude", "id", "xtl", "ytl", "xbr", "ybr", "behaviour"]]
    dff["box"] = dff.apply(lambda row: (row["xtl"], row["ytl"], row["xbr"], row["ybr"], row["id"], row["behaviour"]), axis=1)
    # print(dff["box"])
    dff = dff.groupby(["frame", "latitude", "longitude", "altitude"])["box"].apply(list).reset_index(name='boxes')
    # dff["id_arrays"] = dff["id_arrays"].apply(sorted)
    dff = dff[['latitude', 'longitude', 'altitude', "boxes"]]

    return dff.values.tolist()


boxes = get_drone_and_zebras_coords(df)



# %% craetion Animation

width, height = 3840, 2160
plt.rcParams['animation.ffmpeg_path'] = 'ffmpeg'
fig, ax = plt.subplots(figsize=(width / 100, height / 100), dpi=100)

ax.set_xlim(0, width)
ax.set_ylim(0, height)
ax.set_facecolor('white')
# ax.get_legendremove()
ax.axis("off")

def plot_bounding_box2(xtl, ytl, xbr, ybr): 
    return patches.Rectangle((xtl, ybr), xbr - xtl, ytl - ybr, linewidth=4, edgecolor='g', facecolor='none')

box = boxes[0][3][0]
ax.add_patch(plot_bounding_box2(box[0], box[1], box[2], box[3]))

def animate(f_id):
    for patch in ax.patches:
        patch.remove()
    for text in ax.texts:
        text.remove()
    
    for box in boxes[f_id][3]:
        xtl, ytl, xbr, ybr, id, behaviour = box
        ytl = height - ytl
        ybr = height - ybr
        rect = plot_bounding_box2(xtl, ytl, xbr, ybr)
        ax.add_patch(rect)
        text_x = (xtl + xbr) / 2
        text_y = (ytl + ybr) / 2
        ax.text(text_x, text_y, str(int(id)), color='black', ha='center', va='center', fontsize=17, bbox=dict(facecolor='white', edgecolor='white', boxstyle='round,pad=0.5'))
        ax.text(text_x, ytl + 40, behaviour, color='black', ha='center', va='center', fontsize=17, bbox=dict(facecolor='white', edgecolor='white', boxstyle='round,pad=0.5'))
    ax.set_title("(" + str(boxes[f_id][0]) + "; " + str(boxes[f_id][1]) + ") | Altitude: " + str(boxes[f_id][2]), fontsize=30)


ani = animation.FuncAnimation(fig, animate, frames=len(boxes))
FFwriter = animation.FFMpegWriter(fps=60)
ani.save('drone_bonded_boxes_60pfs.mp4', writer=FFwriter)