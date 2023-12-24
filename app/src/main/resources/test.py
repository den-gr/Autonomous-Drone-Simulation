# %%
import math
import matplotlib.pyplot as plt

step = 15
degrees_list = list(range(0, 361, step))
radians_list = [math.radians(degrees) for degrees in degrees_list]
sin_values = [math.sin(radians) for radians in radians_list]
cos_values = [math.cos(radians) for radians in radians_list]

plt.scatter(sin_values, cos_values, label='Points', color='red')

plt.title('Sin vs Cos')
plt.xlabel('Sin')
plt.ylabel('Cos')
plt.legend()
plt.grid(True)
plt.show()