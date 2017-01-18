Light Motion
============

This is a very light weight motion detection system for cheap chinese IP cameras with the following two key features:
* Streaming of h.264 over RTSP
* Low resolution snapshots as jpeg via http

The goal is to be able to do motion detection and full-framerate recording from at least 10
cameras on an aging Atom.

The h.264 stream is normally stored on disk in a circular buffer in small increments without any processing at all.

The low resolution snapshots are used to do motion detection with a few seconds of latency at the speed the CPU can
handle, so graceful degradation is possible.

The alternative to using the jpeg snapshots would be to stream one of the secondary
low-resolution / low-framerate h.264 streams from the camera, but decoding a fixed
framerate h.264 stream at 1-5 fps is much heavier than doing jpeg decoding at fractional fps.


Building And Running
--------------------

* Build executable jar using: mvn package
* Run the app server using: java -jar target/light-motion-0.0.1-SNAPSHOT.jar server server.yaml
* Open a browser and hit: http://localhost:8080/


Using IntelliJ IDEA
-------------------

* Import the project from the pom.xml
* The main class to start is dk.dren.lightmotion.Server
* This project uses Lombok to cut down on boilerplate, so be sure to install the appropriate lombok plugin for IDEA see: https://github.com/mplushnikov/lombok-intellij-plugin


Securing Cameras
----------------

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