WITH trips AS (
    SELECT time_as_number, int AS trip 
    FROM [SCHEMA_PLACEHOLDER].time_series_data a
    LEFT JOIN [SCHEMA_PLACEHOLDER].time_series_data_iri b
    ON a.data_iri_index = b.data_iri_index
    WHERE b.data_iri = '%trip_iri%'
),
points AS (
    SELECT time_as_number, "geometry(Point,4326)" AS geom 
    FROM [SCHEMA_PLACEHOLDER].time_series_data a
    LEFT JOIN [SCHEMA_PLACEHOLDER].time_series_data_iri b
    ON a.data_iri_index = b.data_iri_index
    WHERE b.data_iri = '%point_iri%'
),
line AS (
    SELECT 
        points.time_as_number, 
        LAG(geom) OVER (ORDER BY points.time_as_number) AS prev_geom,
        ST_MakeLine(LAG(geom) OVER (ORDER BY points.time_as_number), geom) as geom,
        trip
    FROM points
    LEFT JOIN trips
    ON points.time_as_number = trips.time_as_number
)

SELECT time_as_number, trip, geom, '%point_iri%' AS iri
FROM line
WHERE geom is not null