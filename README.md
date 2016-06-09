logback-gelf
============

A [Logback](http://logback.qos.ch/) appender that encodes logs to
[GELF](https://www.graylog.org/resources/gelf/) and transports them
to [Graylog](https://www.graylog.org/) servers.

Support for dropwizard logging.


Dependency information
-----------------------------------

Latest version:

Gradle 

	compile "com.travelguru:logback-gelf:0.6"
	
Maven 

	<dependency>
	  <groupId>com.travelguru</groupId>
	  <artifactId>logback-gelf</artifactId>
	  <version>0.6</version>
	</dependency>

Features
--------

* Append via TCP or UDP (with chunking) to a remote graylog server
* MDC k/v converted to fields
* Fields may have types
* Auto include logger_name
* Auto include Markers
* Auto include Thread name
* Static fields (E.g facility)
* Very Few dependencies (Logback and GSON)

Configuring Logback
---------------------


The minimal possible logback.xml you can write is something like.

```xml
<configuration>
    <appender name="GELF UDP APPENDER" class="com.travelguru.logbackgelf.GelfUDPAppender">
        <encoder class="com.travelguru.logbackgelf.GZIPEncoder">
            <layout class="com.travelguru.logbackgelf.GelfLayout"/>
        </encoder>
    </appender>
   <root level="debug">
    <appender-ref ref="GELF UDP APPENDER" />
  </root>
</configuration>
```

A more complete example that shows how you would overwrite many
default values:

```xml
<configuration>
    <!--Use TCP instead of UDP-->
    <appender name="GELF TCP APPENDER" class="com.travelguru.logback.net.SocketEncoderAppender">
        <remoteHost>somehost.com</remoteHost>
        <port>12201</port>
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="com.travelguru.logbackgelf.GelfLayout">
                <!--An example of overwriting the short message pattern-->
                <shortMessageLayout class="ch.qos.logback.classic.PatternLayout">
                    <pattern>%ex{short}%.100m</pattern>
                </shortMessageLayout>
                <!-- Use HTML output of the full message. Yes, any layout can be used (please don't actually do this)-->
                <fullMessageLayout class="ch.qos.logback.classic.html.HTMLLayout">
                    <pattern>%relative%thread%mdc%level%logger%msg</pattern>
                </fullMessageLayout>
                <useLoggerName>true</useLoggerName>
                <useThreadName>true</useThreadName>
                <useMarker>true</useMarker>
                <host>Test</host>
                <additionalField>ipAddress:_ip_address</additionalField>
                <additionalField>requestId:_request_id</additionalField>
                <includeFullMDC>true</includeFullMDC>
                <fieldType>requestId:long</fieldType>
                <!--Facility is not officially supported in GELF anymore, but you can use staticFields to do the same thing-->
                <staticField class="com.travelguru.logbackgelf.Field">
                  <key>_facility</key>
                  <value>GELF</value>
                </staticField>
            </layout>
        </encoder>
    </appender>

    <root level="debug">
        <appender-ref ref="GELF TCP APPENDER" />
    </root>
</configuration>
```

## GelfLayout

`com.travelguru.logbackgelf.GelfLayout`

This is where most configuration resides, since it's the part that
actually converts a log event into a GELF compatible JSON string.

* **useLoggerName**: If true, an additional field call "_loggerName"
  will be added to each gelf message. Its contents will be the fully
  qualified name of the logger. e.g: `com.company.Thingo`. Default:
  `false`
* **useThreadName**: If true, an additional field call "_threadName"
  will be added to each gelf message. Its contents will be the name of
  the thread. Default: `false`
* **host** The hostname of the host from which the log is being sent.
  Displayed under `source` on web interface. Default:
  `getLocalHostName()`
* **useMarker**: If true, and the user has set a
   [slf4j Marker](http://slf4j.org/api/org/slf4j/Marker.html) on their
   log, then the marker.toString() will be added to the gelf message
   as the field "_marker". Default: `false`
* **shortMessageLayout**: The
  [Layout](http://logback.qos.ch/manual/layouts.html) used to create
  the gelf `short_message` field. Shows up in the message column of
  the log summary in the web interface. Default: `"%ex{short}%.100m"`
  ([PatternLayout](http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout))
* **fullMessageLayout**: The
  [Layout](http://logback.qos.ch/manual/layouts.html) used to create
  the gelf `full_message` field. Shows up in the message field of the
  log details in the web interface. Default: `"%rEx%m"`
  ([PatternLayout](http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout))
* **additionalFields**: See additional fields below. Default: empty
* **fieldType**: See field type conversion below. Default: empty
  (fields sent as string)
* **staticFields**: See static fields below. Note, now that facility
  is deprecated, use this to set a facility Default: empty
* **staticAdditionalFields**: _deprecated_. Use staticFields. Default:
  empty
* **includeFullMDC**: See additional fields below. Default: `false`

## Transports

Both UDP and TCP transports are supported. UDP is the recommended
graylog transport.

### UDP

UDP can be configured using the
`com.travelguru.logbackgelf.GelfUDPAppender` appender. Once messages reach
a certain size, they will be chunked according to the
[gelf spec](https://www.graylog.org/resources/gelf/). A maximum of
128 chunks can be sent per log. If the encoded log is bigger than
that, the log will be dropped. Assuming the default 512 max packet
size, this allows for 65536 bytes (64kb) total per log message
(unzipped).

* **remoteHost**: The remote graylog server host to send log messages
  to (DNS or IP). Default: `"localhost"`
* **port**: The remote graylog server port. Default: `12201`
* **maxPacketSize**: The maximum number of bytes per datagram packet.
  Once the limit is reached, packets will be chunked. Default: `512`

**GZIP**

For UDP, you have the option of Gzipping the Gelf JSON before sending
over UDP. To do this, replace the
`ch.qos.logback.core.encoder.LayoutWrappingEncoder` encoder with the
`com.travelguru.logbackgelf.GZIPEncoder` encoder. E.g

```xml
<appender name="GELF UDP APPENDER" class="com.travelguru.logbackgelf.GelfUDPAppender">
    <encoder class="com.travelguru.logbackgelf.GZIPEncoder">
        <layout class="com.travelguru.logbackgelf.GelfLayout"/>
    </encoder>
</appender>
```

Remember, The GZIP encoder should NOT be used with TCP

### TCP

TCP transport can be configured using the
`com.travelguru.logback.net.SocketEncoderAppender` appender. Unfortunately,
the built in Logback [Socket
Appender](http://logback.qos.ch/manual/appenders.html#SocketAppender)
doesn't give you control of how logs are encoded before being sent
over TCP, which is why you have to use this appender. Note
that due to an unresolved [Graylog
issue](https://github.com/Graylog2/graylog2-server/issues/127), GZIP
is not supported when using TCP.

```xml
<appender name="GELF TCP APPENDER" class="com.travelguru.logback.net.SocketEncoderAppender">
    <port>12201</port>
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="com.travelguru.logbackgelf.GelfLayout">
            ....
        </layout>
    </encoder>
</appender>
```

* **remoteHost**: The remote graylog server host to send log messages
  to (DNS or IP). Default: `"localhost"`
* **port**: The remote graylog server port. Required.
* **maxPacketSize**: The number of logs to keep in memory while the
  graylog server can't be reached. Default: `128`
* **acceptConnectionTimeout**: Milliseconds to wait for a connection
  to be established to the server before failing. Default: `1000`

Extra features
-----------------

## Additional Fields

Additional Fields are extra k/v pairs that can be added to the GELF
json, and thus searched as structured data using graylog. In the slf4j
world, [MDC](http://logback.qos.ch/manual/mdc.html) (Mapped Diagnostic
Context) is an excellent way of programmatically adding fields to your
GELF messages.

Let's take an example of adding the ip address of the client to every
logged message. To do this we add the ip address as a key/value to the
MDC so that the information persists for the length of the request,
and then we inform logback-gelf to look out for this mapping every
time a message is logged.

1.  Store IP address in MDC

```java
// Somewhere in server code that wraps every request
...
org.slf4j.MDC.put("ipAddress", getClientIpAddress());
...
```

2.  Inform logback-gelf of MDC mapping

```xml
<layout class="com.travelguru.logbackgelf.GelfLayout">
    <additionalField>ipAddress:_ip_address</additionalField>
</layout>
```

If the property `includeFullMDC` is set to true, all fields from the
MDC will be added to the gelf message. Any key, which is not listed as
`additionalField` will be prefixed with an underscore. Otherwise the
field name will be obtained from the corresponding `additionalField`
mapping.

If the property `includeFullMDC` is set to false (default value) then
only the keys listed as `additionalField` will be added to a gelf
message.

### Static Fields

Use static additional fields when you want to add a static key value
pair to every GELF message. Key is the additional field key (and
should thus begin with an underscore). The value is a static string.

Now that the GELF `facility` is deprecated, this is how you add a
static facility. StaticFields replace staticAdditionalFields

E.g in the appender configuration:

```xml
<layout class="com.travelguru.logbackgelf.GelfLayout">
  <staticField class="com.travelguru.logbackgelf.Field">
    <key>_facility</key>
    <value>GELF</value>
  </staticField>
  <staticField class="com.travelguru.logbackgelf.Field">
    <key>_node_name</key>
    <value>www013</value>
  </staticField>
</layout>
```

### Static Additional Fields (deprecated)

Static Additional fields have been deprecated and superceded by
staticFields. While they offered a more concise way of expressing the
key/value pair, it was impossible to include a colon in the value.
staticFields are fully structured and don't have this problem.

### Field type conversion

You can configure a specific field to be converted to a numeric type.
Key is the additional field key as inserted into the MDC, value is the
type to convert to. Currently supported types are ``int``, ``long``, ``float`` and ``double``.

```xml
<layout class="com.travelguru.logbackgelf.GelfLayout">
    <additionalField>requestId:_request_id</additionalField>
    <fieldType>requestId:long</fieldType>
</layout>
```

If the conversion fails, logback-gelf will leave the field value alone
(i.e.: send it as String) and print the stacktrace


