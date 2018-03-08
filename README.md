# CLJ Nifi Config

A series of scripts written in clojure specifically for configuring Nifi. Cleaner and more maintainable then using sed. utilizes the clj CLI tool and deps.edn for pulling necessary dependencies.

## Setup

java version 1.8.0 and the CLJ CLI tool.

## Usage

In your script, set the environment variables as described in the nifi documentation [seen here](https://github.com/apache/nifi/blob/master/nifi-commons/nifi-properties/src/test/resources/NiFiProperties/conf/nifi.properties)

## Example

```bash
# first, set your nifi properties path
 CLJ_NIFI_PROPERTIES_PATH=${NIFI_HOME}/conf/nifi.properties
 
 # then, set whichever env vars you would like to change
 NIFI_WEB_HTTP_HOST=localhost
 NIFI_WEB_HTTP_PORT=8080
 # ... etc

 # now run the ./set-properties.clj script and it will set your nifi.properties file according to how you configured it.
 set-properties.clj

 # you're done, the new configuration will be applied at the path specified in CLJ_NIFI_PROPERTIES_PATH
```
