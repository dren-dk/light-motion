This is an example project that shows a couple of simple things
that can be done with some of my favoite tools:
* Dropwizard
*

This project uses Lombok to cut down on boilerplate, so be sure to install
the appropriate lombok plugin in your IDE, for IDEA see:
https://github.com/mplushnikov/lombok-intellij-plugin


Build eclipse project using:
mvn eclipse:eclipse

Or build IDEA project using:
mvn idea:idea


Build executeable jar using:
mvn package

Run the app server using:
java -jar foo.jar server server.yaml

Once the server has started hit up:
http://localhost:8080/hello/text/World

The REST API is documented and usable via swagger at:
http://localhost:8080/swagger/

The index page lives at:
http://localhost:8080/static/

