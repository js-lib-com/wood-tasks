# Clean Project
Clean building files on current project.

## Description
Remove __all__ files from project build directory. Be aware that this command does not attempt to ensure that _build.dir_ property is configured properly.

## Properties
Path to build directory is mandatory; if not provided by execution context task is aborted. It is critical to be sure _build.dir_ property refers to the right directory.
 
| Name      | Type   | Description                                    |
|-----------|--------|------------------------------------------------|
| build.dir | String | build directory path, relative to project root |  

## Parameters
This tasks does not require parameters.

## Author
Written by Iulian Rotaru. Last update June 2021.
