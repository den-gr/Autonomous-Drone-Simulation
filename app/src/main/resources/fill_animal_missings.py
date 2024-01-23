# %%
import pandas as pd
import numpy as np

file_path = 'data/dataset2.json'
output_folder_name = "zebras2/"

ANIMALS = "animals"
id_index = 0
behaviour_index = 1
box_index = 2

df = pd.read_json(file_path)


def get_animal_ids(df):
    ids = set()
    for animals in df[ANIMALS]:
        for a in animals:
            ids.add(a[0])
    return ids
ids = get_animal_ids(df)

# input and array of with animals on a single record
def get_animals_ids(animals):
    return [animal[id_index] for animal in animals]

def get_next_animal_appearans_index(animal_id, index, df):
    while index < len(df):
        index = index + 1
        if(animal_id in get_animals_ids(df.iloc[index][ANIMALS])):
            for internal_inx, animal in enumerate(df.iloc[index][ANIMALS]):
                if(animal_id == animal[id_index]):
                    return index, internal_inx


def interpolate_points(x1, y1, x2, y2, n, round_f=0):
    delta_x = (x2 - x1) / (n + 1)
    delta_y = (y2 - y1) / (n + 1)

    return [(round(x1 + i * delta_x, round_f), round(y1 + i * delta_y, round_f)) for i in range(1, n + 1)]


df = df.reset_index(drop=True)

df_copy = df.copy()

for i, row in df[1:-1].iterrows():
    prev_animals = df.iloc[i-1][ANIMALS]
    animals = row[ANIMALS]
    for p_a in prev_animals:
        a_ids = get_animals_ids(animals)
        prev_a_id = p_a[id_index]
        if(prev_a_id not in a_ids and p_a[behaviour_index] != "Out of Frame"):
            if prev_a_id in get_animal_ids(df[i:]):
                print(i)
                next_i, internal_idx = get_next_animal_appearans_index(prev_a_id, i, df)
                xtl, ytl, xbr, ybr = p_a[box_index]
                n_xtl, n_ytl, n_xbr, n_ybr = df.iloc[next_i][ANIMALS][internal_idx][box_index]

                tl_points = interpolate_points(xtl, ytl, n_xtl, n_ytl, next_i - i)
                br_points = interpolate_points(xbr, ybr, n_xbr, n_ybr, next_i - i)
                for k in range(next_i - i):
                    xtl, ytl = tl_points[k]
                    xbr, ybr = br_points[k]
                    df_copy.loc[i+k, ANIMALS].append([prev_a_id, "Missing", [xtl, ytl, xbr, ybr]])

                
df_copy.to_json("test.json", index=False)

