Dropwizard+AngularJS
====================

This is an example project that shows a couple of simple things
that can be done with some of my favoite tools:
* Dropwizard
* AngularJS
* Project Lombok


Building And Running
--------------------

* Build executable jar using: mvn package
* Run the app server using: java -jar target/dropwizard-angular-seed-0.0.1-SNAPSHOT.jar server server.yaml
* Open a browser and hit: http://localhost:8080/


Using IntelliJ IDEA
-------------------

* Import the project from the pom.xml
* The main class to start is dk.dren.dwa.Server
* This project uses Lombok to cut down on boilerplate, so be sure to install the appropriate lombok plugin for IDEA see: https://github.com/mplushnikov/lombok-intellij-plugin


Using Eclipse
-------------

* Build eclipse project using: mvn eclipse:eclipse
* The main class to start is dk.dren.dwa.Server
* To get eclipse to work with lombok, run java -jar lombok.jar and point the installer at your eclipse dir.
* I could not get the AngularJS-eclipse plugin to work, so I stopped using Eclipse, but you might have better luck-


ASCII art Banner
----------------

The banner.txt was generated with:
http://patorjk.com/software/taag/#p=display&h=3&f=Big&t=DropWizard%0A%2B%0AAngularJS


React
-----

After getting the basics of Angular done, I started looking at React.io, but I would like to 
avoid npm and node forever, so I'll be running babel-standalone on Nashhorn.