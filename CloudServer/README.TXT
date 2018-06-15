 -------------------------------------------------------------

PROJECT TITLE: Cloud Server Application
PURPOSE OF PROJECT: To facilitate the transfer of files between computers within the house.
VERSION or DATE: 1.1.5/June 14 2018
HOW TO START THIS PROJECT: java ./CloudServer
                            OR
                           cloudServer.sh start
AUTHORS: William Ritchie
USER INSTRUCTIONS:
  Must provide a port number as a command line argument. The CloudClient code is set to port# 42843. If you want to use a different port, 
  then you must change the port# in the CloudClient.java code to match the preferred port#. If you are using the shell script to run the 
  server as a service, you must also manipulate the variables at the top of cloudServer.sh to your liking. (Including the desired port#).

CloudServer v1.1.5 or later, compatible with CloudClient v1.6.1 or later.

CloudServer v.1.0.0 to v.1.0.5, compatible with Cloudclient v.1.0.0 to v.1.5.1.
