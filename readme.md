Light Motion
============

This is a very light weight motion detection system for cheap chinese IP cameras with the following two key features:
* Streaming of full-resolution (25 fps 1080p) video as h.264 over RTSP
* Streaming of low-resolution (1 fps/ 640x352) video as h.264 over RTSP

It seems most, if not all of the currently available cameras have the ability to handle a serving both a high-res
 and a low-res stream at once.

The goal of LM is to be able to do motion detection and full-framerate recording from at least 10
cameras on an aging Atom, with a nice HTML based UI, so no client software needs to be developed or installed.

The h.264 stream is normally stored on disk in a circular buffer in small increments without any processing in LM
 at all, in fact the streaming to disk is taken care of by ffmpeg, which simply shuffles
 the bits from network to disk without decoding.

The low resolution stream is turned into ppm snapshots by a separate ffmpeg process
 which are used to do motion detection with a few seconds of latency at the speed
 the CPU can handle, so graceful degradation is possible, though the ffmpeg process 
 will have to decode the low-res steam into ppm snapshots, but that happens at a low 
 framerate, so it ought to be possible even on quite weedy CPUs.
 
If the CPU is unable to handle its motion detection duties at the speed the images come in,
 they are dropped from the queue, so we can deal with really slow or heavily loaded systems. 

If the CPU is wimpy enough to not be able to decode 1 fps low-res video from the needed number of cameras, then
an alternative low-res mode to fetch jpeg snapshots from the cameras, this costs more CPU per frame but allows
an arbitrarily low FPS or FPM as the case might be.

Creating the database
---------------------

LM stores all historic data in a postgresql database, so to get going you need to create a database:

* Install postgresql
* Create the lm user and use it to create the lm database
* Change the password, user name and database url in the server.yaml file if you aren't using the default.
* Create the databse using:
```
java -jar target/light-motion-0.0.1-SNAPSHOT.jar db migrate server.yaml
```


Building And Running
--------------------

* Build executable jar using: mvn package
* Run the app server using: java -jar target/light-motion-0.0.1-SNAPSHOT.jar server server.yaml
* Open a browser and hit: http://localhost:8080/


Using IntelliJ IDEA
-------------------

The community edition of IDEA is quite usable for this project, but the ultimate edition is
a lot better if you plan on working with the frontend code due to the addition of the webstorm
features (javascript).

I highly recommend IDEA ultimate edition, in fact, for what it's worth, it's the only
 non-open source piece of software that I don't detest.
 
* Import the project from the pom.xml
* The main class to start is dk.dren.lightmotion.Server
* This project uses Lombok to cut down on boilerplate, so be sure to install the appropriate lombok plugin for IDEA see: https://github.com/mplushnikov/lombok-intellij-plugin


Securing Chinesium Cameras
--------------------------

Even though the web interface on cheap/chinese cameras demand a login
(admin/admin is a fairly common default) to allow access, I have found that the RTSP
interface is not similarly secured, so anyone can stream from the camera without credentials.

The ONVIF interface forces the client to hash the password, so the camera has to store the password
to verify it, which is bad practitce.

Bottom line:
* Cheap chinese ONVIF cameras are an insecure abomination out of the box
* There is no way to really secure these cameras, so you have to rely on network segregation to keep the adversaries at bay.
* If you choose a password, it will be stored and leaked when the camera is stolen.
* RTSP is insecure, so never put these cameras on an untrusted network.

Make sure that all the ddns/p2p "features" are turned off and firewall the cameras
from the Internet, user-accessible LAN and each other.

Warning: Even if the chinesium cameras you get don't do any of the insecure ddns/p2p stuff,
  there's no guarantee that the next batch will be as well behaved, so do not skimp on 
  network security, segregate the camera network so they can't gain Internet access, ever.

External Dependencies
---------------------

All the java libraries are included in the executable jar file, so you don't need to mess with those,
however some external programs are needed that you will have to provide

* **java**: Java 8 is required to run the code 
* **ffmpeg**: Light motion doesn't touch the RTSP streams directly, in stead it relies on the excellent ffmpeg tool.
* **postgresql**: All data is stored in a postgresql database

If you're running ubuntu server, then run:
```
   sudo apt-get install ffmpeg postgresql openjdk-8-jdk-headless
```

Docker
------

Eventually I'll build a docker image that includes all the dependencies and a ready-to-run
LightMotion server, but for now you have to do that yourself, sorry.   
