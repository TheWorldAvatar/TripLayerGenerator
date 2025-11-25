package cares.cam.ac.uk;

public class EnvConfig {
    public static final String VIS_DATA_JSON = System.getenv("VIS_DATA_JSON");
    public static final String GEOSERVER_WORKSPACE = System.getenv("GEOSERVER_WORKSPACE");
    public static final String DATABASE = System.getenv("DATABASE");
    public static final String SCHEMA = System.getenv("SCHEMA");
}
