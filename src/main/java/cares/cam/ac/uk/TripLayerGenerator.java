package cares.cam.ac.uk;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.core.io.ClassPathResource;

import com.cmclinnovations.stack.clients.geoserver.GeoServerClient;
import com.cmclinnovations.stack.clients.geoserver.GeoServerVectorSettings;
import com.cmclinnovations.stack.clients.geoserver.UpdatedGSVirtualTableEncoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@WebServlet(urlPatterns = { "/" })
public class TripLayerGenerator extends HttpServlet {
    private static final Logger LOGGER = LogManager.getLogger(TripLayerGenerator.class);
    QueryClient queryClient;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String layerGroupName = req.getParameter("layerGroupName");
        String pointIri = req.getParameter("iri");
        String host = req.getParameter("host");

        if (host == null) {
            host = "http://localhost:3838";
        }

        LOGGER.info("Received request for iri = <{}>", pointIri);
        String tripIri = queryClient.getTrip(pointIri);
        List<Integer> tripIndicies = queryClient.getTripIndices(tripIri);

        String schema = queryClient.getSchema(pointIri);

        createLayer(schema);
        setDataJson(tripIndicies, pointIri, tripIri, layerGroupName);
    }

    private void createLayer(String schema) {
        String layerSql = null;
        try (InputStream is = new ClassPathResource("layer.sql").getInputStream()) {
            layerSql = IOUtils.toString(is, StandardCharsets.UTF_8).replace("[SCHEMA_PLACEHOLDER]", schema);
        } catch (IOException e) {
            String errmsg = "Failed to read layer.sql";
            LOGGER.error(errmsg);
            LOGGER.error(e.getMessage());
            throw new RuntimeException(errmsg, e);
        }

        GeoServerClient geoServerClient = GeoServerClient.getInstance();
        geoServerClient.createWorkspace(EnvConfig.GEOSERVER_WORKSPACE);
        UpdatedGSVirtualTableEncoder virtualTable = new UpdatedGSVirtualTableEncoder();
        GeoServerVectorSettings geoServerVectorSettings = new GeoServerVectorSettings();
        virtualTable.setSql(layerSql);
        virtualTable.setEscapeSql(true);
        virtualTable.setName(EnvConfig.LAYERNAME);
        virtualTable.addVirtualTableParameter("trip_iri", "", ".*");
        virtualTable.addVirtualTableParameter("point_iri", "", ".*");
        virtualTable.addVirtualTableGeometry("geom", "Geometry", "4326"); // geom needs to match the sql query
        geoServerVectorSettings.setVirtualTable(virtualTable);
        geoServerClient.createPostGISDataStore(EnvConfig.GEOSERVER_WORKSPACE, "trajectory", EnvConfig.DATABASE,
                EnvConfig.SCHEMA);
        geoServerClient.createPostGISLayer(EnvConfig.GEOSERVER_WORKSPACE, EnvConfig.DATABASE, EnvConfig.SCHEMA,
                EnvConfig.LAYERNAME, geoServerVectorSettings);
    }

    private void setDataJson(List<Integer> trips, String pointIri, String tripIri, String layerGroupName) {
        String viewparams = String.format("trip_iri:%s;point_iri:%s", tripIri, pointIri);

        String wmsPath = "/geoserver/twa/wms?service=WMS&version=1.1.0&request=GetMap&bbox=%7Bbbox-epsg-3857%7D&width=256&height=256&srs=EPSG:3857&format=application/vnd.mapbox-vector-tile";
        URIBuilder builder;
        URI wmsEndpoint;
        try {
            builder = new URIBuilder(EnvConfig.GEOSERVER_HOST + wmsPath);
            builder.setParameter("layers", EnvConfig.GEOSERVER_WORKSPACE + ":" + EnvConfig.LAYERNAME);
            builder.setParameter("viewparams", viewparams);
            wmsEndpoint = builder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        URIBuilder builder2;
        URI stackEndpoint;
        try {
            builder2 = new URIBuilder(EnvConfig.GEOSERVER_HOST);
            builder2.setPath("exposure-feature-info-agent/trajectory"); // harcoded
            stackEndpoint = builder2.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        JSONObject group = new JSONObject();
        group.put("name", layerGroupName);
        group.put("expanded", true);
        group.put("stack", stackEndpoint);

        JSONArray sources = new JSONArray();
        JSONObject source = new JSONObject();
        source.put("type", "vector");
        source.put("tiles", new JSONArray(List.of(wmsEndpoint)));
        source.put("id", "trajectory-source");
        sources.put(source);
        group.put("sources", sources);

        JSONArray layers = new JSONArray();
        JSONObject layout = new JSONObject();
        layout.put("visibility", "none");
        group.put("layers", layers);

        for (int tripIndex : trips) {
            JSONObject layer = new JSONObject();
            layer.put("id", "trip" + String.valueOf(tripIndex));
            if (tripIndex == 0) {
                layer.put("name", "Stays");
            } else {
                layer.put("name", "Trip " + String.valueOf(tripIndex));
            }

            layer.put("source", "trajectory-source");
            layer.put("source-layer", EnvConfig.LAYERNAME);
            layer.put("type", "line");
            layer.put("layout", layout);

            JSONArray filter = new JSONArray();
            filter.put("==").put(new JSONArray().put("get").put("trip")).put(tripIndex); // needs to be in sync with
                                                                                         // layer.sql
            layer.put("filter", filter);

            JSONObject paint = new JSONObject();
            if (tripIndex == 0) {
                paint.put("line-color", "black");
            } else {
                paint.put("line-color", "blue");
            }
            layer.put("paint", paint);
            layers.put(layer);
        }

        String dataJson;
        try {
            dataJson = Files.readString(Paths.get(EnvConfig.VIS_DATA_JSON));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JSONObject data = new JSONObject(dataJson);
        JSONArray existingGroups = data.getJSONArray("groups");

        for (int i = 0; i < existingGroups.length(); i++) {
            if (existingGroups.getJSONObject(i).getString("name").contentEquals(layerGroupName)) {
                existingGroups.remove(i);
            }
        }
        existingGroups.put(group);

        try {
            Files.write(Paths.get(EnvConfig.VIS_DATA_JSON), data.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() throws ServletException {
        queryClient = new QueryClient();
    }
}
