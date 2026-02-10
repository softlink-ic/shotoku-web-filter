# shotoku-web-filter
Servlet filter to read files from specified filesystem directory instead of deployed WAR. Useful for hot-reloading files during development.

Developed from source of [Shotoku Web Filter](https://web.archive.org/web/20071015051556/http://labs.jboss.com/wiki/ShotokuWebFilter), as the original JAR has been lost to the sands of time and we needed to add some support for POM filtering of served files. The original library was licensed under LGPL 2.1, so source code is provided for our modifications.

Mirror of original source located [here](https://github.com/driedtoast/shotoku_rebirth/blob/master/shotoku-web/src/java/org/jboss/shotoku/web/ResourcesFilter.java) by a stroke of incredible luck - the original source code has _also_ been lost to the sands of time.
