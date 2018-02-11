# REST Service for projects management


## Description

This project includes a sample REST service written in Kotlin using the Vert.x microservice toolset.

## Run it

- Build with    `/.gradlew build`, creates JAR in build/libs
- Start with  `/.gradlew run`, executes `main` directly, no command line arguments can be passed
- Start with arguments by executing JAR from build/libs  with <br>
*java -jar <jarname> --server.port=8181*

## Configuration

The application can be configured with following arguments

|===
|Parameter | Description | Default

|server.port|Port of REST endpoint|8181
|mongo.host|Host name of running MongoDB instance|localhost
|mongo.port|Port of running MongoDB instance|27017
|mongo.dbname|Database name in MongoDB instance|projects_db

|===

All values may be overriden by passing the arguments as follows: <br>
*"--server.port=8181"*


### Profiles

The application may be started with "dev" profile which will make the application on an in-memory database. <br>
Simply pass the following argument when starting: *"--profile=dev"*


## REST-Interface

The REST endpoint is available at *localhost:<server.port>/projects* and speaks Json.

### Entities

*Entity* ``Project(_id: String, name: String)`` +
*Entity* ``Result(success: Boolean)``
*Entity* ``ErrorResponse(message: String, error: Int, context: String)``

### Methods

#### `GET`

* on */:projectId* returns
** found ``Project`` if available
** ``ErrorResponse(1000)`` with HTTP status 400 if no ``Project`` with ``projectId`` is available

* on */* returns all available ``Project`` entities as JSON array

#### `PUT

* on */* with ``Project`` as json request body returns
** *newly* saved ``Project`` if successful
** ``ErrorResponse(1000)`` with http status 400 if no ``Project`` with ``projectId`` is available

#### `POST`

* on */* with ``Project`` as json request body returns
** *uodated* ``Project`` if successful
** ``ErrorResponse(1001)`` with http status 400 if no ``Project`` with ``projectId`` is available

#### `DELETE`

* on */:projectId* returns
** ``Result`` entity if successful, i.e. corresponding `Project` can be found and deleted
** ``ErrorResponse(1000)`` with http status 400 if no ``Project`` with ``projectId`` is available

* on */* returns ``Result`` entity, all `Project`s will be removed
