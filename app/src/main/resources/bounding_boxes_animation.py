# %%
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import matplotlib.animation as animation
import pandas as pd

RAW_CSV = False
FRAME_WIDTH, FRAME_HEIGHT = 3840, 2160
output_path = 'data/flight.mp4'
FPS = 10

# TODO external file name input
if(RAW_CSV):
    file_path = 'data/12_01_23-DJI_0994.csv'
    df = pd.read_csv(file_path)
else:
    file_path = 'data/dataset.json'
    df = pd.read_json(file_path)

# Get Zebras data and drone postion grouped by frames (RAW CSV)
def get_drone_and_zebras_coords(df):
    dff = df.loc[:, ['frame', 'latitude', 'longitude', "altitude", "id", "xtl", "ytl", "xbr", "ybr", "behaviour"]]
    dff["box"] = dff.apply(lambda row: (row["id"], row["behaviour"], (row["xtl"], row["ytl"], row["xbr"], row["ybr"])), axis=1)
    dff = dff.groupby(["frame", "latitude", "longitude", "altitude"])["box"].apply(list).reset_index(name='animals')
    dff = dff[['latitude', 'longitude', 'altitude', "animals"]]

    return dff

if(RAW_CSV):
    boxes = get_drone_and_zebras_coords(df)
else:
    # Remove part of the video without animals
    # TODO make generic
    boxes = df[502:] 
    boxes = boxes[:-800]

#Animation creation
plt.rcParams['animation.ffmpeg_path'] = 'ffmpeg'
fig, ax = plt.subplots(figsize=(FRAME_WIDTH / 100, FRAME_HEIGHT / 100), dpi=100)
ax.set_xlim(0, FRAME_WIDTH)
ax.set_ylim(0, FRAME_HEIGHT)
ax.set_facecolor('white')
ax.axis("off")

# Animal rectangle
def create_rectangle(xtl, ytl, xbr, ybr): 
    return patches.Rectangle((xtl, ybr), xbr - xtl, ytl - ybr, linewidth=4, edgecolor='g', facecolor='none')

def box_value(f_id, value_name):
    return str(round(boxes.iloc[f_id][value_name],1))

def animate(f_id):
    for patch in ax.patches:
        patch.remove()
    for text in ax.texts:
        text.remove()
    
    for box in boxes.iloc[f_id-1]["animals"]:
        # Original coordinates has (0; 0) in top left corner
        # Matplotlib initial (0; 0) coordinates are in bottom left corner
        id, behaviour, b  = tuple(box)
        xtl, ytl, xbr, ybr = tuple(b)
        # invert top and bottom
        ytl = FRAME_HEIGHT - ytl 
        ybr = FRAME_HEIGHT - ybr
        ax.add_patch(create_rectangle(xtl, ytl, xbr, ybr))
        text_x = (xtl + xbr) / 2
        text_y = (ytl + ybr) / 2
        ax.text(text_x, text_y, str(int(id)), color='black', ha='center', va='center', fontsize=17)
        ax.text(text_x, ytl + 40, behaviour, color='black', ha='center', va='center', fontsize=17)
    coords = "(" + str(round(boxes.iloc[f_id]["latitude"], 6)) + "; " + str(round(boxes.iloc[f_id]["longitude"],6)) + ")"
    alt = " | Altitude: " + str(boxes.iloc[f_id]["altitude"]) + "m "
    orient = ""
    if(not RAW_CSV):
        orient =  "compass: " + box_value(f_id, "compass") + "°, pitch: " + box_value(f_id, "pitch") + "°, roll: " + box_value(f_id, "roll") + "°, g_pitch: " + box_value(f_id, "gimbal_pitch") + "°"
    ax.set_title( coords + alt + orient, fontsize=30)


ani = animation.FuncAnimation(fig, animate, frames=len(boxes))
FFwriter = animation.FFMpegWriter(fps=FPS)
ani.save(output_path, writer=FFwriter)