WITH points AS (
    SELECT time_as_number, "geometry(Point,4326)" AS geom 
    FROM [SCHEMA_PLACEHOLDER].time_series_data a
    LEFT JOIN [SCHEMA_PLACEHOLDER].time_series_data_iri b
    ON a.data_iri_index = b.data_iri_index
    WHERE b.data_iri = '%point_iri%'
),
line AS (
    SELECT 
        time_as_number, 
        LAG(geom) OVER (ORDER BY time_as_number) AS prev_geom,
        ST_MakeLine(LAG(geom) OVER (ORDER BY time_as_number), geom) as geom
    FROM points
)

SELECT time_as_number, geom, '%point_iri%' AS iri
FROM line
WHERE geom is not null