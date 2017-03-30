Merge circular chunks buffer with recordings and make MovieMaker a pure deletion operation
------------------------------------------------------------------------------------------

As we don't produce concatenated video files any more, there's no reason to keep the chunks buffer 
separate from the recordings, so let's just keep the files in the same buffer forever.

In stead of keeping chunks in a separate, short, buffer, keep the chunks for a much longer, configurable mandatory retention time.

Once the mandatory retention time has passed, delete the chunks that are not near a detected event.


MPEG-DASH player
----------------

Set up a dash.js based player to play back the video in the buffer.
http://cdn.dashjs.org/latest/jsdoc/index.html

* Each sequence around an event should probably be a separate mpd
* The last mandatory retention time should be set up as a live stream with the appropriate amount of retention. https://github.com/Dash-Industry-Forum/dash.js/issues/53 https://github.com/Dash-Industry-Forum/dash.js/issues/1472


Record lowres video too
-----------------------

Store the lowres stream along side the highres and tell the player about it via the mpd:
https://github.com/Dash-Industry-Forum/dash.js/issues/1647

This way we can start out streaming at low-res and upgrade the stream to full-res once it has been cached.


