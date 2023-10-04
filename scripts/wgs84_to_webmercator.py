# Source: https://gis.stackexchange.com/a/189697
from pyproj import Proj, transform

# UQ bounding box test
# min: -27.502944163432616, 153.00600502724035
# max: -27.489617778602657, 153.0184252962559

LAT = -27.489617778602657
LONG = 153.0184252962559

P3857 = Proj(init='epsg:3857')
P4326 = Proj(init='epsg:4326')

x,y = transform(P4326, P3857, LONG, LAT)

print(f"Transformed: {x}, {y}")
