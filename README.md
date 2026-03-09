# Trip layer generator

The purpose of this agent is to create the GeoServer layer for trajectories instantiated using TimeSeriesRDBClientOntop 

## Environment variables

- VIS_DATA_JSON: Absolute path to the data.json file for visualisation
- GEOSERVER_WORKSPACE: Name of workspace
- DATABASE: Database for GeoServer layer
- SCHEMA: Schema for GeoServer layer creation, used in the SQL view

## Requirements

A basic knowledge on using the TWA stack <https://github.com/TheWorldAvatar/stack> is assumed.

Trajectory data instantiated using <https://github.com/TheWorldAvatar/stack/blob/main/stack-clients/src/main/java/com/cmclinnovations/stack/clients/timeseries/TimeSeriesRDBClient.java>.

Data is queryable via the stack outgoing federation endpoint <https://github.com/TheWorldAvatar/stack/tree/main/stack-manager#outgoing-stack-endpoint>.

## Usage

POST to "/" with parameters

- `iri`: IRI of the instance containing point time series
- `layerGroupName`: Name of the layer group for visualisation (user facing)
- `host`: External facing host of the stack, used to construct WMS endpoint and feature info agent query
- `layerName`: Layer name in GeoServer (internal in GeoServer), recommend to have one layer for trajectories with trips and one layer for trajectories without trips.
- `colour`: Colour of the trajectory/trip line (optional, defaults to blue). Visits are hard-coded to black.
- `width`: Width of the trajectory/trip line (optional, defaults to 3).

```bash
curl -X POST http://localhost:3838/trip-layer-generator/?iri=http://trajectory&layerGroupName=Trajectory&host=http://localhost:3838&layerName=trajectory
```

This command assumes that `http://localhost:3838/trip-layer-generator` is the base URL of the container, which depends on the configuration used to spin this container up, an example can be found in <https://github.com/TheWorldAvatar/hd4-stack>.

Running this will create a layer in GeoServer if it does not exist, and add a new layer group in the data.json file for visualisation.

## Building and debugging

To build, populate ./docker/credentials/ with

- repo_password.txt
- repo_username.txt

containing your GitHub credentials, then run

```bash
docker compose build
```

To push the image to the repository:

```bash
docker compose push
```
