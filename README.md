This is your new Play application
=================================

This file will be packaged with your application, when using `activator dist`.

### Backend application actions

#### List all available actions

````
GET /
````

Lists all available actions

#### Account

Currently works with Rackspace identity account only.

````
POST /app/auth          controllers.Login.create()
````

Authenticates user's credentials

* Posts user id and password from request
* Checks local cache to see if user id and password combination already exists
* Posts to configured identity endpoint to retrieve credential information
* Returns user name, tenant, token, and expired date
* Returns HTTP 401 if unauthenticated
* Returns HTTP 500 if errored

````
GET /app/auth           controllers.Login.index()
````

Retrieves current user's auth information

* Checks local cache for user information
* Returns user name, tenant, token, and expired date
* Returns HTTP 401 if not found

#### Home page

User's home page.  Used to view created (running and stopped) Repose instances

````
GET /app/repose         controllers.Repose.list()
````

Retrieve all created Repose instances

* Retrieves Repose swarm cluster from Carina
* Retrieves all running and stopped Repose containers
* Retrieves metadata for each container from the local cache (time created, version, filter list, last perf link) mapped by container id

````
PUT /app/repose/:id     controllers.Repose.edit()
````

Update created Repose instance running status (start/stop)

* Retrieve repose instance id from local cache
* Update status

````
DELETE /app/repose/:id  controllers.Repose.delete()
````

Delete created Repose instance

* Delete repose instance id from cache
* Remove running Repose container from Carina

#### Create instance page

Create Repose instance

Workflow:

1. Get all available Repose versions
2. Select one version
3. Get all filters for that version
4. Multiselect all wanted filters for that version
5. Return json metadata for each selected filter
6. Fill in required and optional data for each filter
7. Select third party dependencies
    a. For auth filters, it would be the identity URI (default responder)
    b. For origin service, it would be the origin URI (default responder)
    c. For valkyrie filters, it would be the monitoring URI (default responder)
8. Create Repose
9. Convert all json data per filter to xml
10. Create volume container with xmls in /etc/repose
11. Link to repose container that installs specified version
12. Link to identity container if default is used and identity filters specified
13. Link to origin container if default is used
14. Link to monitoring container if default is used and valkyrie filters specified
15. Start Repose container and return container id

````
POST /app/repose        controllers.Repose.create()
````

Start new Repose instance

Required request data should have JSON list of all required json filter requests, boolean for whether default responders are to be used, and Repose version

* Retrieves Repose swarm cluster from Carina (creates if one does not exist and stores creds in local cache)
* Converts request json data to xml per filter
* Creates new volume container with xml
* Starts origin container with responder service
* Starts identity container with responder service
* Starts valkyrie container with responder service
* Creates new Repose container with specified version and specified filters (uses label with generated repose instance id guid)
* Links all containers together, starts the container and returns container id
* Adds container id to local cache
* Sets instance version in the local cache to 1

````
GET /app/version
````

Lists all available versions

* Retrieves all Repose versions from github

````
GET /app/version/:id
````

Lists all available filters for this version

* Retrieves all Repose filters for the specified version from maven

````
GET /app/version/:id/:component_id
````

Lists json metadata for the specified filter

* Retrieves xsd from github.
* Parses to json, adding required attributes to required elements

#### Detail page

Instance detail page.  Shows repose instance status, version, filter list, configurations used, ability to make a sample request, and ability to start a small load test

````
GET /app/repose/:id     controllers.Repose.index()
````

Retrieves high level details for current Repose instance

* Retrieves repose instance id from local cache
* If local cache does not have container id, reaches out to Carina to get the status of the container (retrieves by label)
* Returns:
  * Repose version
  * Filter list
  * Running configurations
  * Status
  * Version list

````
GET /app/repose/:id/filters controllers.ReposeFilters.list()
````

Retrieves filters from created Repose instance and its appropriate configurations (in xml format) - for the latest version

* Retrieves repose instance id from local cache
* Retrieves list of filters from system model on repose container
* Retrieves appropriate xml configurations for each filter

````
GET /app/repose/:id/filters/previous/:pid controllers.ReposeFilters.list()
````

Retrieves filters from created Repose instance and its appropriate configurations (in xml format) - for the specified version

* Retrieves repose instance id from local cache
* Retrieves list of filters from system model on repose container
* Retrieves appropriate xml configurations for each filter

````
GET /app/repose/:id/filters/:fid    controllers.ReposeFilters.index()
````

Retrieves specific filter configuration and converts it to json data

````
PUT /app/repose/:id/filters/:fid    controllers.ReposeFilters.edit()
````

Updates json data for filter and updates create Repose instance with new data

* Retrieves Repose swarm cluster from Carina
* Retrieves Repose container
* Converts request json data to xml for that filter
* Updates xml file on the volume container
* Restarts the container
* Increments instance version in the local cache

````
DELETE /app/repose/:id/filters/:fid    controllers.ReposeFilters.delete()
````

Removes filter from create Repose instance

* Retrieves Repose swarm cluster from Carina
* Retrieves Repose container
* Removes specified filter from system model
* Restarts the container
* Increments instance version in the local cache

````
POST /app/repose/:id/filters    controllers.ReposeFilters.create()
````

Replaces existing filters with filter set from the request

* Retrieves Repose swarm cluster from Carina
* Converts request json data to xml per filter
* Replaces all xml in volume container with new xml
* Restarts the container
* Increments instance version in the local cache

````
PATCH /app/repose/:id/filters    controllers.ReposeFilters.update()
````

Appends existing filters with filter set from the request

* Retrieves Repose swarm cluster from Carina
* Converts request json data to xml per filter
* Appends existing xml in volume container with new xml
* Restarts the container
* Increments instance version in the local cache

#### Requests page

Shows request history for a repose instance.  Specifies request, intra-filter chain, response, execution time and version

````
GET /app/repose/:id/requests    controllers.ReposeRequests.list()
````

Shows all requests regardless of version

* Retrieves requests from local cache based on instance id

````
POST /app/repose/:id/requests   controllers.ReposeRequests.create()
````

Creates a new request for the specified version

* Sends a specified request to running repose container
* Stores request in local cache
* Retrieves intra-filter requests and stores in local cache
* Stores requests to third party (identity, origin, monitoring) in local cache
* Stores responses from third party (identity, origin, monitoring) in local cache
* Stores origin response in local cache
* Stores intra-filter responses in local cache
* Stores response in local cache
* Stores total response time in local cache and per filter (x-trace-request)
* Stores repose instance version in local cache

