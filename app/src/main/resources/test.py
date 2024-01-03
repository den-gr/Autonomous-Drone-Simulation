# %% 
import numpy as np
import matplotlib.pyplot as plt

def rotate_point(x, y, angle_degrees):
    # Convert angle from degrees to radians
    angle_radians = np.radians(angle_degrees)
    
    # Perform the rotation using a 2D rotation matrix
    x_rotated = x * np.cos(angle_radians) - y * np.sin(angle_radians)
    y_rotated = x * np.sin(angle_radians) + y * np.cos(angle_radians)
    
    return x_rotated, y_rotated

# Original point coordinates
x_original = 1
y_original = 0

# Rotation angle in degrees
rotation_angle = 45

# Rotate the point
x_rotated, y_rotated = rotate_point(x_original, y_original, rotation_angle)

# Plot the original and rotated points
plt.scatter([x_original, x_rotated], [y_original, y_rotated], color=['blue', 'red'], label=['Original Point', 'Rotated Point'])
plt.axhline(0, color='black',linewidth=0.5)
plt.axvline(0, color='black',linewidth=0.5)
plt.grid(color = 'gray', linestyle = '--', linewidth = 0.5)
plt.legend()
plt.title('Rotation of Point')
plt.xlabel('X-axis')
plt.ylabel('Y-axis')
plt.show()
