# %%
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import matplotlib.animation as animation

import pandas as pd

file_path = 'data/12_01_23-DJI_0994.csv'

df = pd.read_csv(file_path)

def remove_noise(df, filter_window=3, print=False):
    LONGITUDE_CENTER = 37
    cond1 = abs(df['latitude']) < filter_window
    cond2 = abs(df["longitude"] - LONGITUDE_CENTER) < filter_window
    condition = cond1 & cond2
    if(print):
        print("Noise coordinates:")
        print((df[~condition])[["latitude", "longitude"]])
    return df[condition]

df = remove_noise(df)

# Get Zebras data and drone postion grouped by frames
def get_drone_and_zebras_coords(df):
    dff = df.loc[:, ['frame', 'latitude', 'longitude', "altitude", "id", "xtl", "ytl", "xbr", "ybr", "behaviour"]]
    dff["box"] = dff.apply(lambda row: (row["xtl"], row["ytl"], row["xbr"], row["ybr"], row["id"], row["behaviour"]), axis=1)
    dff = dff.groupby(["frame", "latitude", "longitude", "altitude"])["box"].apply(list).reset_index(name='boxes')
    dff = dff[['latitude', 'longitude', 'altitude', "boxes"]]

    return dff.values.tolist()

boxes = get_drone_and_zebras_coords(df)

# %% Animation creation

plt.rcParams['animation.ffmpeg_path'] = 'ffmpeg'
width, height = 3840, 2160
fig, ax = plt.subplots(figsize=(width / 100, height / 100), dpi=100)
ax.set_xlim(0, width)
ax.set_ylim(0, height)
ax.set_facecolor('white')
ax.axis("off")

def create_rectangle(xtl, ytl, xbr, ybr): 
    return patches.Rectangle((xtl, ybr), xbr - xtl, ytl - ybr, linewidth=4, edgecolor='g', facecolor='none')

def animate(f_id):
    for patch in ax.patches:
        patch.remove()
    for text in ax.texts:
        text.remove()
    
    for box in boxes[f_id-1][3]:
        # Original coordinates has (0; 0) in top left corner
        # Matplotlib initial (0; 0) coordinates are in bottom left corner
        xtl, ytl, xbr, ybr, id, behaviour = box
        # invert top and bottom
        ytl = height - ytl 
        ybr = height - ybr
        ax.add_patch(create_rectangle(xtl, ytl, xbr, ybr))
        text_x = (xtl + xbr) / 2
        text_y = (ytl + ybr) / 2
        ax.text(text_x, text_y, str(int(id)), color='black', ha='center', va='center', fontsize=17)
        ax.text(text_x, ytl + 40, behaviour, color='black', ha='center', va='center', fontsize=17)
    ax.set_title("(" + str(boxes[f_id][0]) + "; " + str(boxes[f_id][1]) + ") | Altitude: " + str(boxes[f_id][2]) + "m", fontsize=30)


ani = animation.FuncAnimation(fig, animate, frames=len(boxes))
FFwriter = animation.FFMpegWriter(fps=60)
ani.save('data/drone_bonded_boxes_60pfs.mp4', writer=FFwriter)