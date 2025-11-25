package cares.cam.ac.uk;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.json.JSONArray;

import com.cmclinnovations.stack.clients.rdf4j.Rdf4jClient;

import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import org.apache.logging.log4j.LogManager;

public class QueryClient {
    private static final Logger LOGGER = LogManager.getLogger(QueryClient.class);

    RemoteStoreClient remoteStoreClient;

    static final Prefix PREFIX_DERIVATION = SparqlBuilder
            .prefix("derivation", Rdf.iri("https://www.theworldavatar.com/kg/ontoderivation/"));
    static final Prefix PREFIX_EXPOSURE = SparqlBuilder
            .prefix("exposure", Rdf.iri("https://www.theworldavatar.com/kg/ontoexposure/"));
    static final Prefix PREFIX_TIMESERIES = SparqlBuilder
            .prefix("timeseries", Rdf.iri("https://www.theworldavatar.com/kg/ontotimeseries/"));

    static final Iri IS_DERIVED_FROM = PREFIX_DERIVATION.iri("isDerivedFrom");
    static final Iri HAS_CALCULATION_METHOD = PREFIX_EXPOSURE.iri("hasCalculationMethod");
    static final Iri BELONGS_TO = PREFIX_DERIVATION.iri("belongsTo");
    static final Iri HAS_VALUE = PREFIX_EXPOSURE.iri("hasValue");
    static final Iri HAS_DISTANCE = PREFIX_EXPOSURE.iri("hasDistance");
    static final Iri HAS_TIME_SERIES = PREFIX_TIMESERIES.iri("hasTimeSeries");
    static final Iri HAS_TIME_CLASS = PREFIX_TIMESERIES.iri("hasTimeClass");
    static final Iri HAS_UNIT = PREFIX_EXPOSURE.iri("hasUnit");
    static final Iri HAS_SCHEMA = PREFIX_TIMESERIES.iri("hasSchema");

    static final Iri TRIP = PREFIX_EXPOSURE.iri("Trip");

    public QueryClient() {
        remoteStoreClient = new RemoteStoreClient(
                Rdf4jClient.getInstance().readEndpointConfig().getOutgoingRepositoryUrl());
    }

    public String getTrip(String trajectoryIri) {
        SelectQuery query = Queries.SELECT();
        Variable timeseriesVar = query.var();
        Variable tripVar = query.var();

        GraphPatternNotTriples gp = GraphPatterns.and(tripVar.isA(TRIP).andHas(HAS_TIME_SERIES, timeseriesVar),
                Rdf.iri(trajectoryIri).has(HAS_TIME_SERIES, timeseriesVar));

        query.select(tripVar).where(gp).prefix(PREFIX_EXPOSURE, PREFIX_TIMESERIES);

        JSONArray queryResult = remoteStoreClient.executeQuery(query.getQueryString());

        if (queryResult.isEmpty()) {
            return null;
        } else if (queryResult.length() == 1) {
            return queryResult.getJSONObject(0).getString(tripVar.getVarName());
        } else {
            String errmsg = "More than one trip instances?";
            LOGGER.error(errmsg);
            throw new RuntimeException(errmsg);
        }
    }

    public List<Integer> getTripIndices(String tripIri) {
        List<Integer> trips = new ArrayList<>();

        String query = """
                    PREFIX time: <http://www.w3.org/2006/time#>
                    PREFIX timeseries: <https://www.theworldavatar.com/kg/ontotimeseries/>

                    SELECT DISTINCT ?val
                    WHERE {
                        ?obs timeseries:observationOf <%s>;
                            timeseries:hasResult/timeseries:hasValue ?val.
                    }
                """.formatted(tripIri);

        JSONArray queryResult = remoteStoreClient.executeQuery(query);

        for (int i = 0; i < queryResult.length(); i++) {
            trips.add(queryResult.getJSONObject(i).getInt("val"));
        }

        return trips;
    }

    String getSchema(String pointIri) {
        String query = """
                PREFIX timeseries: <https://www.theworldavatar.com/kg/ontotimeseries/>
                SELECT ?schema
                WHERE {
                    <%s> timeseries:hasTimeSeries/timeseries:hasSchema ?schema
                }
                """.formatted(pointIri);
        JSONArray queryResult = remoteStoreClient.executeQuery(query);

        if (queryResult.length() != 1) {
            throw new RuntimeException("Unexpected query result size in getSchema");
        }

        return queryResult.getJSONObject(0).getString("schema");
    }
}
