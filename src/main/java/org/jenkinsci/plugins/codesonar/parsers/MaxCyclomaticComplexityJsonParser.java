package org.jenkinsci.plugins.codesonar.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.models.procedures.ProcedureMetric;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

//TODO This class need to be cleaned up as well as a little bit refactored to avoid repetitions
public class MaxCyclomaticComplexityJsonParser extends AbstractJsonParser<ProcedureMetric> {
     private static final Logger LOGGER = Logger.getLogger(MaxCyclomaticComplexityJsonParser.class.getName());

    private static final String USER_OBJECT = "user";
    private static final Object BUILD_LAUNCHD_OBJECT = "buildLaunchd";
    private static final Object LAUNCHD_OBJECT = "launchd";
    private static final Object PROJECT_OBJECT = "project";
    private static final Object ROWS_ARRAY = "rows";
    private static final Object METRIC_CYCLOMATIC_COMPLEXITY = "metricCyclomaticComplexity";
    private static final String PROCEDURE = "procedure";
    private Reader inputStreamReader;
    
    public MaxCyclomaticComplexityJsonParser(InputStream inputStream) {
        inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    }

    @Override
    public ProcedureMetric parseObject() throws IOException {
        ProcedureMetric maxCyclomaticComplexityProcedure = null;
        
//      String jsonFile = "./maxCyclomaticComplexity.json";
//      JsonReader jsonReader = new JsonReader(new FileReader(jsonFile , StandardCharsets.UTF_8));
        JsonReader jsonReader = new JsonReader(inputStreamReader);
        jsonReader.setLenient(true);
         
        try {
            while (jsonReader.hasNext()) {
                JsonToken nextToken = jsonReader.peek();

                if (JsonToken.BEGIN_OBJECT.equals(nextToken)) {

                    jsonReader.beginObject();

                } else if (JsonToken.NAME.equals(nextToken)) {

                    String name = jsonReader.nextName();
                    System.out.println("Token KEY >>>> " + name);
                    
                    if(BUILD_LAUNCHD_OBJECT.equals(name)) {
                        parseBuildLaunchd(jsonReader);
                    } else if(LAUNCHD_OBJECT.equals(name)) {
                        parseLaunchd(jsonReader);
                    } else if(PROJECT_OBJECT.equals(name)) {
                        parseProject(jsonReader);
                    } else if(ROWS_ARRAY.equals(name)) {
                        maxCyclomaticComplexityProcedure = parseRowsArray(jsonReader);
                    }

                } else if (JsonToken.STRING.equals(nextToken)) {

                    String value = jsonReader.nextString();
                    System.out.println("Token Value >>>> " + value);

                } else if (JsonToken.NUMBER.equals(nextToken)) {

                    long value = jsonReader.nextLong();
                    System.out.println("Token Value >>>> " + value);

                } else if (JsonToken.BOOLEAN.equals(nextToken)) {

                    boolean value = jsonReader.nextBoolean();
                    System.out.println("Token Value >>>> " + value);

                } else if (JsonToken.NULL.equals(nextToken)) {

                    jsonReader.nextNull();
                    System.out.println("Token Value >>>> null");

                } else if (JsonToken.END_OBJECT.equals(nextToken)) {

                    jsonReader.endObject();

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
    
    private ProcedureMetric parseRowsArray(JsonReader jsonReader) throws IOException {
        ArrayList<ProcedureMetric> rowsArray = new ArrayList<>();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            rowsArray.add(parseRowObject(jsonReader));
        }
        jsonReader.endArray();
        
        LOGGER.info("rowsArray (before sorting)=" + rowsArray);
        // Sort rows array by "cyclomatic complexity" firstly and by "procedure" secondly
        Collections.sort(rowsArray);
        LOGGER.info("rowsArray (after sorting)=" + rowsArray);
        
        /*
         *  Get the first element of the list, which according to sorting settings,
         *  corresponds to the one with the maximum cyclomatic complexity. 
         */
        if(rowsArray.size() > 0) {
            return rowsArray.get(0);
        }
        
        return null;
    }

    private ProcedureMetric parseRowObject(JsonReader jsonReader) throws IOException {
        ProcedureMetric row = new ProcedureMetric();
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            
            if (name.equals(METRIC_CYCLOMATIC_COMPLEXITY)) {
                row.setMetricCyclomaticComplexity(jsonReader.nextInt());
            } else if (name.equals(PROCEDURE)) {
                row.setProcedure(jsonReader.nextString());
            } else {
                jsonReader.skipValue();
            }
        }
        
        jsonReader.endObject();
        
        return row;
    }

    private void parseProject(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            JsonToken nextToken = jsonReader.peek();
            
            if (JsonToken.NAME.equals(nextToken)) {

                String name = jsonReader.nextName();
                System.out.println("Token KEY >>>> " + name);
                
            } else if (JsonToken.STRING.equals(nextToken)) {

                String value = jsonReader.nextString();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.NUMBER.equals(nextToken)) {

                long value = jsonReader.nextLong();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.BOOLEAN.equals(nextToken)) {

                boolean value = jsonReader.nextBoolean();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.NULL.equals(nextToken)) {

                jsonReader.nextNull();
                System.out.println("Token Value >>>> null");

            }
        }
        
        jsonReader.endObject();
    }

    private void parseLaunchd(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            JsonToken nextToken = jsonReader.peek();
            
            if (JsonToken.NAME.equals(nextToken)) {

                String name = jsonReader.nextName();
                System.out.println("Token KEY >>>> " + name);
                
                if(USER_OBJECT.equals(name)) {
                    parseUserObject(jsonReader);
                }
                
            } else if (JsonToken.STRING.equals(nextToken)) {

                String value = jsonReader.nextString();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.NUMBER.equals(nextToken)) {

                long value = jsonReader.nextLong();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.BOOLEAN.equals(nextToken)) {

                boolean value = jsonReader.nextBoolean();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.NULL.equals(nextToken)) {

                jsonReader.nextNull();
                System.out.println("Token Value >>>> null");

            }
        }
        
        jsonReader.endObject();
    }

    private void parseBuildLaunchd(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            JsonToken nextToken = jsonReader.peek();
            
            if (JsonToken.NAME.equals(nextToken)) {

                String name = jsonReader.nextName();
                System.out.println("Token KEY >>>> " + name);
                
                if(USER_OBJECT.equals(name)) {
                    parseUserObject(jsonReader);
                }
                
            } else if (JsonToken.STRING.equals(nextToken)) {

                String value = jsonReader.nextString();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.NUMBER.equals(nextToken)) {

                long value = jsonReader.nextLong();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.BOOLEAN.equals(nextToken)) {

                boolean value = jsonReader.nextBoolean();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.NULL.equals(nextToken)) {

                jsonReader.nextNull();
                System.out.println("Token Value >>>> null");

            }
        }
        
        jsonReader.endObject();
    }

    private void parseUserObject(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        
        while(jsonReader.hasNext()) {
            JsonToken nextToken = jsonReader.peek();
            
            if (JsonToken.NAME.equals(nextToken)) {

                String name = jsonReader.nextName();
                System.out.println("Token KEY >>>> " + name);
                
            } else if (JsonToken.STRING.equals(nextToken)) {

                String value = jsonReader.nextString();
                System.out.println("Token Value >>>> " + value);

            } else if (JsonToken.NUMBER.equals(nextToken)) {

                long value = jsonReader.nextLong();
                System.out.println("Token Value >>>> " + value);

            }
        }
        
        jsonReader.endObject();
    }
//
//  public static void main(String[] args) {
//      MaxCyclomaticComplexityJsonParser parser = new MaxCyclomaticComplexityJsonParser();
//      try {
//          ProcedureMetric maxCCProc = parser.parseObject();
//          if(maxCCProc == null) {
//              System.out.println("Max Cyclomatic Complexity not available");
//          } else {
//              System.out.println("Max Cyclomatic Complexity is: " + maxCCProc);
//          }
//      } catch (IOException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }
//  }
}
