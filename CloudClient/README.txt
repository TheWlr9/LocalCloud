# Instructions:
  Steps:
    1. Must have CloudServer running on a machine (Host) with either a static IP address, or something that can pass as a static IP
       address.
    2. Then, you must edit the code in CloudClient.java where the value of ADDRESS is that of the Host (as a String).
    3. Then, on any machine (Client) that can access the same LAN that the Host is connected to, run CloudClient whenever you want to use 
       the cloud.
  Actions:
    - Clicking on the "Upload" button in the bottom right of the Client window, will upload any file you desire to the cloud, as long as 
      it is not restricted. This does not remove the file from the client, it only uploads a copy of the file to the cloud.
    - Clicking on a file in the Client window will download that file from the cloud into the Client (you get to choose where it gets 
      downloaded). This will remove the file from the cloud.
