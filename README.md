# Trip layer generator

The purpose of this agent is to create the GeoServer layer for trajectories instantiated using TimeSeriesRDBClientOntop 

## Environment variables

- VIS_DATA_JSON: Absolute path to the data.json file for visualisation
- GEOSERVER_WORKSPACE: Name of workspace
- DATABASE: Database for GeoServer layer
- SCHEMA: Schema for GeoServer layer creation, used in the SQL view
- LAYERNAME: Layer name in GeoServer

## Usage

POST to "/" with parameters

- iri: IRI of the instance containing point time series
- layerGroupName: Name of the layer group for visualisation
- host: External facing host of the stack, used to construct WMS endpoint and feature info agent query

```bash
curl -X POST http://localhost:3838/trip-layer-generator/?iri=http://trajectory&layerGroupName=Trajectory&host=http://localhost:3838
```
