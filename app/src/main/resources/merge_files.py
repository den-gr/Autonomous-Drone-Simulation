# %%
import pandas as pd
import numpy as np

PRODUCE_CSV = True

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

output_path = "data/"


df_v = pd.read_csv(video_data_path)
df_f = pd.read_csv(flight_data_path)

### Preproces video dataframe
df_video = df_v.loc[:, columns]

# df_video.loc[df_video['id'] != 1, 'id'] = 2 # ad-hoc id change

df_video[ANIMALS] = df_video.apply(lambda row: (row["id"], row["behaviour"], (row[XTL], row[YTL], row[XBR], row[YBR])), axis=1)
df_video = df_video.groupby(["frame", LATITUDE, LONGITUDE, ALTITUDE])[ANIMALS].apply(list).reset_index(name=ANIMALS)



def round_and_average(vec):
    return [round(sum(x) / len(x)) for x in zip(*vec)]

# Group multiple dataframe rows in a single df row
def group_video_df_rows(rows):
    rows_df = pd.DataFrame(rows)[ANIMALS].explode().values.tolist()
    df = pd.DataFrame(rows_df, columns=["id", "behaviour", 'box'])
    df_behavior = df.groupby(['id'])['behaviour'].first().reset_index()
    df_box = df.groupby(['id'])['box'].apply(round_and_average).reset_index()
    df = pd.merge(df_behavior, df_box, on='id')

    lon = rows[0][LONGITUDE]
    lat = rows[0][LATITUDE]
    if(lat > 90 or lat < -90):
        lat = lat/1000
    if(lon > 180 or lon < -180):
        lon = lon/1000

    return {
            FRAMES: [r['frame'] for r in rows],
            LATITUDE: lat,
            LONGITUDE: lon,
            ALTITUDE: rows[0][ALTITUDE],
            ANIMALS: df.values.tolist(),
        }

def have_same_metadata(row1, row2): 
    return (row1[LATITUDE] == row2[LATITUDE]
            and row1[LONGITUDE] == row2[LONGITUDE]
            and row1[ALTITUDE] == row2[ALTITUDE])

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
            # Skipping of the single-frame rows prevents some joins from getting stuck
            print("skip single frame row: " + str(i))
            i = i + 1
    return new_df
df_video = group_df_by_metadata(df_video)

### Process flight dataframe

df_f[ALTITUDE] = round(df_f["height_above_takeoff(feet)"] * 0.3048, 1)
df_f[LATITUDE] = df_f[LATITUDE].round(6)
df_f[LONGITUDE] = df_f[LONGITUDE].round(6)
df_flight = df_f[["time(millisecond)", "datetime(utc)", LATITUDE, LONGITUDE, ALTITUDE, " compass_heading(degrees)"," pitch(degrees)"," roll(degrees)", "gimbal_pitch(degrees)"]]

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
            "roll": row[" roll(degrees)"],
            "gimbal_pitch": row["gimbal_pitch(degrees)"]
        }

def are_same(row1, row2):
    alt_equal = np.isclose(row1[ALTITUDE], row2[ALTITUDE], atol=0.001, rtol=0)
    lat_equal = np.isclose(row1[LATITUDE], row2[LATITUDE], atol=1e-09, rtol=0)
    lon_equal = np.isclose(row1[LONGITUDE], row2[LONGITUDE], atol=1e-09, rtol=0)

    return ((np.isclose(row1[LATITUDE], row2[LATITUDE], atol=1e-06, rtol=1e-08) and alt_equal and lon_equal) 
            or 
            (np.isclose(row1[LONGITUDE], row2[LONGITUDE], atol=1e-06, rtol=1e-08) and alt_equal and lat_equal))
    

JOIN_POINT = 501 # flight dataset record when video recording beggin
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
result_df = result_df[JOIN_POINT:]

output_file_name = "dataset2"
path = output_path + output_file_name
if(PRODUCE_CSV):
    result_df.to_csv(path + ".csv", index=False)

result_df.to_json(path + ".json", index=False)
