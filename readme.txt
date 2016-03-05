This is an example project that shows a couple of simple things
that can be done with some of my favoite tools:
* Dropwizard
* AngularJS
* Project Lombok

Build executable jar using:
mvn package

Run the app server using:
java -jar target/dropwizard-angular-seed-0.0.1-SNAPSHOT.jar server server.yaml

Once the server has started hit up the front page to read the rest:
http://localhost:8080/


Build eclipse project using:
mvn eclipse:eclipse

If you're using IDEA, just import the project from the pom.xml, but this project uses Lombok to cut down on boilerplate,
so be sure to install the appropriate lombok plugin for IDEA see:
https://github.com/mplushnikov/lombok-intellij-plugin

The main class to start in IDEA or Eclipse is dk.dren.dwa.Server

The banner.txt was generated with:
http://patorjk.com/software/taag/#p=display&h=3&f=Big&t=DropWizard%0A%2B%0AAngularJS
