package org.jenkinsci.plugins.codesonar.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.models.ProcedureMetric;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Json parser implementation specialized on processing the response of the HUB JSON API which endpoint is /analysis/{analysis}-procedures.json.
 * In particular, its purpose is to extract the CodeSonar procedure having the maximum cyclomatic complexity.
 * Its implementation leverages the streamed parsing capability of the Google GSon library in order to avoid loading the entire response in-memory. 
 * @author aseno
 *
 */
public class MaxCyclomaticComplexityJsonParser extends AbstractJsonParser<ProcedureMetric> {
     private static final String NAME_TOKEN = "NAME TOKEN value=";
     private static final String STRING_TOKEN = "STRING TOKEN value=";
     private static final String NUMBER_TOKEN = "NUMBER TOKEN value=";
     private static final String BOOLEAN_TOKEN = "BOOLEAN TOKEN value=";
     private static final String NULL_TOKEN = "NULL TOKEN";

    private static final Logger LOGGER = Logger.getLogger(MaxCyclomaticComplexityJsonParser.class.getName());

    private static final String USER_OBJECT = "user";
    private static final Object BUILD_LAUNCHD_OBJECT = "buildLaunchd";
    private static final Object LAUNCHD_OBJECT = "launchd";
    private static final Object PROJECT_OBJECT = "project";
    private static final Object ROWS_ARRAY = "rows";
    private static final Object METRIC_CYCLOMATIC_COMPLEXITY = "metricCyclomaticComplexity";
    private static final String PROCEDURE = "procedure";
    private Reader inputStreamReader;
    private ProcedureMetric maxCyclomaticComplexityProcedure = null;
    
    /**
     * Interface that represents an action to take during parsing based off of the value of the token which type is NAME.
     * @author aseno
     *
     */
    private interface SimpleParseAction {
        void take(String tokenName) throws IOException;
    }
    
    /**
     * Interface that represents an action to take during parsing based off of the value of the token which type is NAME,
     * which lets you pass also an additional parameter.
     * @author aseno
     *
     */
    private interface ParametrizedParseAction<P> {
        void take(String tokenName, P parameter) throws IOException;
    }
    
    public MaxCyclomaticComplexityJsonParser(InputStream inputStream) {
        inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    }
    
    
    private void parseToken(JsonToken token, JsonReader jsonReader) throws IOException {
        parseToken(token, jsonReader, (SimpleParseAction) null);
    }

    /**
     * Method specific for those usages where it's needed an action without parameters.
     * It's purpose is to reducing code verbosity in simpler cases, as well as to
     * clarify the needs of the caller.  
     * Internally it wraps a SimpleParseAction into a ParametrizedParseAction with
     * a parameter of type Void.
     * @param token
     * @param jsonReader
     * @param action
     * @throws IOException
     */
    private void parseToken(JsonToken token, JsonReader jsonReader, SimpleParseAction action) throws IOException {
        if(action != null) {
            parseToken(token, jsonReader, new ParametrizedParseAction<Void>() {
                @Override
                public void take(String tokenName, Void parameter) throws IOException {
                    action.take(tokenName);
                }
            });
        } else {
            parseToken(token, jsonReader, null, null);
        }
    }
    
    private <P> void parseToken(JsonToken token, JsonReader jsonReader, ParametrizedParseAction<P> action) throws IOException {
        parseToken(token, jsonReader, action, null);
    }
    
    /**
     * Main method that parses a json token, which can optionally take a custom action
     * when processing the token of type NAME.
     * @param <P> The type of the parameter passed to method take() of the action
     * @param token The token that's required to be parsed
     * @param jsonReader The json reader where to read tokens from
     * @param action An optional action to be taken during processing of tokens of type NAME 
     * @param parameter An optional parameter to the action
     * @throws IOException
     */
    private <P> void parseToken(JsonToken token, JsonReader jsonReader, ParametrizedParseAction<P> action, P parameter) throws IOException {
        switch(token) {
        case NAME:
            String name = jsonReader.nextName();
            LOGGER.fine(NAME_TOKEN + name);
            /*
             * If it has specified a special action to take based off of
             * the name parameter, than call it.
             */
            if(action != null) {
                action.take(name, parameter);
            }
            break;
        case STRING:
            LOGGER.fine(STRING_TOKEN + jsonReader.nextString());
            break;
        case NUMBER:
            LOGGER.fine(NUMBER_TOKEN + jsonReader.nextLong());
            break;
        case BOOLEAN:
            LOGGER.fine(BOOLEAN_TOKEN + jsonReader.nextBoolean());
            break;
        case NULL:
            jsonReader.nextNull();
            LOGGER.fine(NULL_TOKEN);
            break;
        default:
            LOGGER.fine(MessageFormat.format("Unexpected token type: \"{0}\"", token.name()));
        }
        
    }

    @Override
    public ProcedureMetric parseObject() throws IOException {
        JsonReader jsonReader = new JsonReader(inputStreamReader);
        jsonReader.setLenient(true);

        try {
            while (jsonReader.hasNext()) {
                JsonToken nextToken = jsonReader.peek();
                
                if (JsonToken.BEGIN_OBJECT.equals(nextToken)) {

                    jsonReader.beginObject();

                } else if (JsonToken.END_OBJECT.equals(nextToken)) {

                    jsonReader.endObject();

                } else {
                    parseToken(jsonReader.peek(), jsonReader, new SimpleParseAction() {
                        @Override
                        public void take(String name) throws IOException {
                            if(BUILD_LAUNCHD_OBJECT.equals(name)) {
                                parseBuildLaunchd(jsonReader);
                            } else if(LAUNCHD_OBJECT.equals(name)) {
                                parseLaunchd(jsonReader);
                            } else if(PROJECT_OBJECT.equals(name)) {
                                parseProject(jsonReader);
                            } else if(ROWS_ARRAY.equals(name)) {
                                List<ProcedureMetric> rowsArray = parseRowsArray(jsonReader);
                                // Sort rows array by "cyclomatic complexity" firstly and by "procedure" secondly
                                LOGGER.fine("rowsArray (before sorting)=" + rowsArray);
                                Collections.sort(rowsArray);
                                LOGGER.fine("rowsArray (after sorting)=" + rowsArray);
                                /*
                                 *  Get the first element of the list, which according to sorting settings,
                                 *  corresponds to the one with the maximum cyclomatic complexity. 
                                 */
                                if(rowsArray.size() > 0) {
                                    maxCyclomaticComplexityProcedure = rowsArray.get(0);
                                }
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                jsonReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return maxCyclomaticComplexityProcedure;
    }
    
    private List<ProcedureMetric> parseRowsArray(JsonReader jsonReader) throws IOException {
        ArrayList<ProcedureMetric> rowsArray = new ArrayList<>();
        
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            rowsArray.add(parseRowObject(jsonReader));
        }
        jsonReader.endArray();
        
        return rowsArray;
    }

    private ProcedureMetric parseRowObject(JsonReader jsonReader) throws IOException {
        ProcedureMetric row = new ProcedureMetric();
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            parseToken(jsonReader.peek(), jsonReader, new ParametrizedParseAction<ProcedureMetric>() {
                @Override
                public void take(String name, ProcedureMetric row) throws IOException {
                    if (name.equals(METRIC_CYCLOMATIC_COMPLEXITY)) {
                        row.setMetricCyclomaticComplexity(jsonReader.nextInt());
                    } else if (name.equals(PROCEDURE)) {
                        row.setProcedure(jsonReader.nextString());
                    } else {
                        jsonReader.skipValue();
                    }
                }
            },
            row);
        }
        
        jsonReader.endObject();
        
        return row;
    }

    private void parseProject(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            parseToken(jsonReader.peek(), jsonReader, new SimpleParseAction() {
                @Override
                public void take(String name) throws IOException {
                    if(USER_OBJECT.equals(name)) {
                        parseUserObject(jsonReader);
                    }
                }
            });
        }
        
        jsonReader.endObject();
    }
    
    private void parseLaunchd(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            parseToken(jsonReader.peek(), jsonReader, new SimpleParseAction() {
                @Override
                public void take(String name) throws IOException {
                    if(USER_OBJECT.equals(name)) {
                        parseUserObject(jsonReader);
                    }
                }
            });
        }
        
        jsonReader.endObject();
    }

    private void parseBuildLaunchd(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            parseToken(jsonReader.peek(), jsonReader, new SimpleParseAction() {
                @Override
                public void take(String name) throws IOException {
                    if(USER_OBJECT.equals(name)) {
                        parseUserObject(jsonReader);
                    }
                }
            });
        }
        
        jsonReader.endObject();
    }

    private void parseUserObject(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            parseToken(jsonReader.peek(), jsonReader);
        }
        
        jsonReader.endObject();
    }
    
    static void a(ProcedureMetric  m) {
        m = new ProcedureMetric();
        LOGGER.fine("a=" + m);
    }
    
}
