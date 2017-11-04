ONVIF: Protocol of the damned
=============================

ONVIF came into existence in the bad old days before REST and JSON, so we have to deal with
the over designed SOAP brain damage.

There doesn't seem to be any non-brain-damaged APIs for talking SOAP, so in stead I have
implemented the three very simple SOAP calls as raw xml files that the arguments are
interpolated into as plain strings, I know it's bad form to write xml without a proper xml
writer, but damn it, it's simple and it works.