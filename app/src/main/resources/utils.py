import math
import numpy as np
import os

DF_BEHAVIOR_INDEX = 1
DF_BOX_INDEX = 2

### COMMON

def create_folder_if_not_exists(folder_path):
    if not os.path.exists(folder_path):
        os.makedirs(folder_path)
        print(f"Folder '{folder_path}' created.")

def get_box_center(box):
    xtl, ytl, xbr, ybr = box
    x = round((xtl + xbr)/2, 1)
    y = round((ytl + ybr)/2, 1)
    return x, y


### Rewrite ids -------------

def create_distance_matrix(active_animals, records):
    matrix = np.empty((len(active_animals), len(records)))
    for k, active_animal in enumerate(active_animals):
        for j, record in enumerate(records):
            record_coords = get_box_center(record[DF_BOX_INDEX])
            matrix[k, j] = active_animal.distance_to(record_coords)
    return matrix


def remove_duplicate_records(records):
    seen = set()
    duplicate_indexes = []
    for i, r in enumerate(records):
        id = r[0]
        if id in seen:
            duplicate_indexes.append(i)
        else: 
            seen.add(id)

    return filter_by_indexes(records, duplicate_indexes)


def filter_by_indexes(list, indexes):
    return [elem for index, elem in enumerate(list) if index not in indexes]

class Animal:
    def __init__(self, id, bounding_box, behavior):
        self.id = id
        self._bounding_box = bounding_box
        self.behavior = behavior

    @property
    def bounding_box(self):
        return self._bounding_box

    @bounding_box.setter
    def bounding_box(self, new_bounding_box):
        self._bounding_box = new_bounding_box

    def _get_coordinates(self):
        return get_box_center(self.bounding_box)

    def distance_limit(self):
        xtl, ytl, xbr, ybr = self.bounding_box
        hor = (xbr - xtl)/2
        ver = (ybr - ytl)/2
        return  math.sqrt(hor**2 + ver**2)
    
    def distance_to(self, coordinates):
        x1, y1 = self._get_coordinates()
        x2, y2 = coordinates
        return math.sqrt((x2 - x1)**2 + (y2 - y1)**2)

    def get_export(self):
        return [self.id, self.behavior, self.bounding_box]


### GPX  --------------------

def angle_to_vector(angle_degrees):
    angle_radians = math.radians(angle_degrees)
    x = math.cos(angle_radians)
    y = math.sin(angle_radians)
    magnitude = math.sqrt(x ** 2 + y ** 2)
    if magnitude != 0:
        x /= magnitude
        y /= magnitude
    return (x, y)

def vector_to_angle(x, y):
    angle_radians = math.atan2(y, x)
    angle_degrees = math.degrees(angle_radians)
    angle_degrees %= 360
    return angle_degrees




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

def get_distance(point1, point2):
    x1, y1 = point1
    x2, y2 = point2
    distance = math.sqrt((x2 - x1)**2 + (y2 - y1)**2)
    return distance * 111320