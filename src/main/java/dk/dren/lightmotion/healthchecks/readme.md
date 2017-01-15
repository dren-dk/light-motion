Healthchecks package
====================

Health checks live here, your application should have health checks for every single important bit of functionality
that the application relies on, like:

* If you're talking to a database, a health check needs to ping it
* If you store files on disk, then a health check should check for free space and writeability
* Talk to an external service? Have a check keep an eye on availability and latency.

The more checks you implement, the less mysterious errors there will be to make operations hate you.
