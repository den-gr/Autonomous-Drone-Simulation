# %%
import pandas as pd
import numpy as np

from importlib import reload
from utils import *

output_folder_name = f"data/jflights_new_ids/"
create_folder_if_not_exists(output_folder_name)

ids = range(1, 15)
# ids = [1]

for flight_id in ids:
    file_path = f'data/jflights/flight_{flight_id}.json'

    df = pd.read_json(file_path)
    df_original = df.copy()

    df['animals2'] = None
    id_counter = 1
    active_animals = []
    records = df['animals'][0]
    for record in records:
        animal = Animal(id_counter, record[DF_BOX_INDEX], record[DF_BEHAVIOR_INDEX])
        active_animals.append(animal)
        id_counter += 1
        mylist= []
        for active in active_animals:
            mylist.append(active.get_export())
        df.at[0, 'animals2'] = mylist


    for idx in range(1, len(df)):
        records = df['animals'][idx]
        records = remove_duplicate_records(records)
        distance_matrix = create_distance_matrix(active_animals, records)
        # print(distance_matrix)

        records_idxs = set([i for i in range(len(records))])
        animals_idxs = set([i for i in range(len(active_animals))])
        # todo remove animals id if they not present in records

        new_active_animals = []
        couples = []
        for r in range(len(records)):
            closest_animal_indx = np.argmin(distance_matrix[:, r])
            closest_record_indx = np.argmin(distance_matrix[closest_animal_indx, :])
            dis = distance_matrix[closest_animal_indx, r]
            animal =  active_animals[closest_animal_indx]
            if(closest_record_indx == r and dis < animal.distance_limit()): 
                if(closest_record_indx in records_idxs):
                    records_idxs.remove(closest_record_indx)
                    animals_idxs.remove(closest_animal_indx)
                    animal.bounding_box = records[r][DF_BOX_INDEX]
                else:
                    print("Attention! The closest suitable record is already utilized!")

        if(len(records_idxs) == 1 and len(animals_idxs) == 1):
            #give last chance
            active_animals[list(animals_idxs)[0]].bounding_box = records[list(records_idxs)[0]][DF_BOX_INDEX]
        else:
            for i in records_idxs:
                active_animals.append(Animal(id_counter, records[i][DF_BOX_INDEX], records[i][DF_BEHAVIOR_INDEX]))
                id_counter +=1
            active_animals = filter_by_indexes(active_animals, animals_idxs)
        
        mylist = []
        for active in active_animals:
            mylist.append(active.get_export())
        df.at[idx, 'animals2'] = mylist

    df.drop(columns=['animals'], inplace=True)
    df.rename(columns={'animals2': "animals"}, inplace=True)
    df.to_json(output_folder_name + f"flight_{flight_id}.json")
    # df.to_csv(output_folder_name + f"flight_{flight_id}.csv")
    print("finish", flight_id)
# %%
