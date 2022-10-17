# Create Icons
Create size variants for application icons, generating surrogate icon if the base icon is missing.

## Description
Generated icon is a circle filled with _background color_ and a text with _text color_. Text is the application alias and formed from first two characters of the application name.

There are multiple icon sizes, but all with aspect ratio 1:1. Current implemented sizes: 128, 144, 152, 192, 256 and 512.

## Properties
Path to assets directory is optional; if not provided by execution context it is prompted from user on parameters. IMagick path is mandatory. 
 
| Name                 | Type   | Description                                     |
|----------------------|--------|-------------------------------------------------|
| assets.dir           | String | assets directory path, relative to project root |  
| imagick.convert.path | String | fully qualified system path for IMagick binary  |

## Parameters
All parameters are mandatory but they have default values. Color can be either a color name supported by [IMagick](https://imagemagick.org/) image processor or hash RGB color with format #rrggbb.

| Name             | Type    | Description                                                 |
|------------------|---------|-------------------------------------------------------------|
| Assets Dir       | String  | assets directory path, if not provided by execution context | 
| Background Color | String  | color used to fill generated circle                         |
| Text Color       | String  | color for application alias                                 |
| Sphere Effect    | Boolean | generate 3D effect                                          |

## Author
Written by Iulian Rotaru. Last update June 2021.
