# %%
import pandas as pd
import random
import gpxpy
import numpy as np
import gpxpy.gpx
from datetime import datetime, timedelta

LATITUDE = "latitude"
LONGITUDE = "longitude"
ALTITUDE = "altitude"
IDS = "ids"
FRAMES = "frames"
XTL = "xtl"
YTL = "ytl"
XBR = "xbr"
YBR = "ybr"
ANIMALS = "animals"

columns = ["frame", LATITUDE, LONGITUDE, ALTITUDE, "id", XTL, YTL, XBR, YBR, "behaviour"]


video_data_path = 'data/12_01_23-DJI_0994.csv'
flight_data_path = 'data/Jan-12th-2023-12-04PM-Flight-Airdata.csv'

df_v = pd.read_csv(video_data_path)
df_f = pd.read_csv(flight_data_path)

### Preproces
df_video = df_v.loc[:, columns]
df_video[ANIMALS] = df_video.apply(lambda row: (row["id"], row["behaviour"], (row["xtl"], row["ytl"], row["xbr"], row["ybr"])), axis=1)
df_video = df_video.groupby(["frame", LATITUDE, LONGITUDE, ALTITUDE])[ANIMALS].apply(list).reset_index(name=ANIMALS)
df_video = df_video[2:]
def have_same_metadata(row1, row2): # at moment joining process is based on latitude, longitude and altitude
    return (row1[LATITUDE] == row2[LATITUDE]
            and row1[LONGITUDE] == row2[LONGITUDE]
            and row1[ALTITUDE] == row2[ALTITUDE])

def round_and_average(vec):
    return [round(sum(x) / len(x)) for x in zip(*vec)]
# Group multiple dataframe rows in a single df row
def group_video_df_rows(rows):
    rows_df = pd.DataFrame(rows)['animals'].explode().values.tolist()
    df = pd.DataFrame(rows_df, columns=["id", "behaviour", 'box'])
    df_behavior = df.groupby(['id'])['behaviour'].first().reset_index()
    df_box = df.groupby(['id'])['box'].apply(round_and_average).reset_index()
    # print(df.values.tolist())
    df = pd.merge(df_behavior, df_box, on='id')

    return {
            FRAMES: [r['frame'] for r in rows],
            LATITUDE: rows[0][LATITUDE],
            LONGITUDE: rows[0][LONGITUDE],
            ALTITUDE: rows[0][ALTITUDE],
            ANIMALS: df.values.tolist(),
            # "xtl": round(np.mean([r["xtl"] for r in rows]))
        }
# Group multiple video frame rows in a single row
def group_df_by_metadata(df):
    new_df = pd.DataFrame()
    i = 0
    while i+2 < len(df): #ignore last two records
        row1 = df.iloc[i]
        row2 = df.iloc[i+1]
        row3 = df.iloc[i+2]
        if(have_same_metadata(row1, row2)):
            if(have_same_metadata(row2, row3)):
                new_row = group_video_df_rows([row1, row2, row3])  # three frame -> one row
                i = i + 3
            else:
                new_row = group_video_df_rows([row1, row2]) # two frame -> one row
                i = i + 2
            new_df = pd.concat([new_df, pd.DataFrame([new_row])], ignore_index=True)
        else:
            # Skipping of the single-frame rows prevents some joining from getting stuck
            print("skip single frame row: " + str(i))
            i = i + 1
    return new_df
    

df_video = group_df_by_metadata(df_video)


df_f[ALTITUDE] = round(df_f["height_above_takeoff(feet)"] * 0.3048, 1)
df_f[LATITUDE] = df_f[LATITUDE].round(6)
df_f[LONGITUDE] = df_f[LONGITUDE].round(6)
df_flight = df_f[["time(millisecond)", "datetime(utc)", LATITUDE, LONGITUDE, ALTITUDE, " compass_heading(degrees)"," pitch(degrees)"," roll(degrees)"]]

# %%
def make_new_row(row, frames=[], animals=[]):
    return {
            "time": row["time(millisecond)"],
            "datetime": row["datetime(utc)"],
            LATITUDE: row[LATITUDE],
            LONGITUDE: row[LONGITUDE],
            ALTITUDE: row[ALTITUDE],
            FRAMES: frames,
            ANIMALS: animals,
            "compass": row[" compass_heading(degrees)"],
            "pitch": row[" pitch(degrees)"],
            "roll": row[" roll(degrees)"]
        }

def are_same(row1, row2):
    return (np.isclose(row1[LATITUDE], row2[LATITUDE], atol=1e-06, rtol=1e-08) 
            and np.isclose(row1[LONGITUDE], row2[LONGITUDE], atol=1e-06, rtol=1e-08) 
            and np.isclose(row1[ALTITUDE], row2[ALTITUDE], atol=0.11, rtol=0))
    

JOIN_POINT = 502 # flight dataset record when video recording beggin
i = 0
result_df = pd.DataFrame()
for index, row in df_flight.iterrows():
    if(index < JOIN_POINT or i >= len(df_video)):
        new_row = make_new_row(row)
    else:
        if(not are_same(row, df_video.iloc[i]) 
           and not are_same(row, df_video.iloc[i+1])):
            new_row = make_new_row(row)
        else:
            if(not are_same(row, df_video.iloc[i]) # skip a row
               and are_same(row, df_video.iloc[i+1])):
                print("skip: ", i)
                i = i + 1
            frames = df_video.iloc[i][FRAMES]
            animals = df_video.iloc[i][ANIMALS]
            new_row = make_new_row(row, frames=frames, animals=animals)
            i = i + 1

    result_df = pd.concat([result_df, pd.DataFrame([new_row])],ignore_index=True)
result_df
result_df.to_csv("merged.csv", index=False)
result_df.to_json("merged.json", index=False)
