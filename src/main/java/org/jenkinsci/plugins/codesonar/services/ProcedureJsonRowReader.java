package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.codesonar.models.json.ProcedureJsonRow;

import com.google.gson.stream.JsonReader;


/** Read procedure rows from CodeSonar hub /analysis/{analysis_id}-procedures.json endpoint.
 */
class ProcedureJsonRowReader {

    private static final Logger LOGGER = Logger.getLogger(ProcedureJsonRowReader.class.getName());

    private JsonReader inputJsonReader;
    private JsonReader rowsJsonReader = null;

    /** Initialize a row reader with a stream of CodeSonar hub procedure search JSON result text. */
    public ProcedureJsonRowReader(InputStream inputStream) {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        inputJsonReader = new JsonReader(inputStreamReader);
    }

    /** Close the reader and the underlying stream. */
    public void close()
    {
        if (inputJsonReader != null)
        {
            try {
                inputJsonReader.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error closing JsonReader", e);
            }
        }
        inputJsonReader = null;
        rowsJsonReader = null;
    }

    /** Read the next procedure from the results.
     *  @returns null if there are no more procedures in the result stream.
     */
    public ProcedureJsonRow readNextRow() throws IOException {
        JsonReader jsonReader = inputJsonReader;
        ProcedureJsonRow row = null;

        // JSON results look like: 
        //  { ...,
        //     rows: [ 
        //        { "procedure": STRING, "metricCyclomaticComplexity": NUMBER, ... },
        //  ... ] }

        if (jsonReader != null && rowsJsonReader == null)
        {
            // Assume we are at the beginning of the stream.
            jsonReader.beginObject();
            while (jsonReader.hasNext() && rowsJsonReader == null)
            {
                String propertyName = jsonReader.nextName();
                if (propertyName.equals("rows")) {
                    jsonReader.beginArray();
                    rowsJsonReader = jsonReader;
                } else {
                    jsonReader.skipValue();
                }
            }
            if (rowsJsonReader == null) {
                // We couldn't find the "rows" property.
                jsonReader.endObject();
                throw new IOException("A 'rows' property was expected, but not found in the Procedures JSON results");
            }
        }

        if (rowsJsonReader != null)
        {
            if (rowsJsonReader.hasNext()) {
                row = readProcedureJsonRowObject(jsonReader);
            } else {
                // No more rows to read.
                rowsJsonReader = null;
                close();
            }
        }

        return row;
    }

    /** Read a JSON procedure row object inside the procedure rows array.
     */
    private ProcedureJsonRow readProcedureJsonRowObject(JsonReader jsonReader) throws IOException
    {
        ProcedureJsonRow row = new ProcedureJsonRow();

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String propertyName = jsonReader.nextName();
            if (propertyName.equals("procedure")) {
                row.setProcedure(jsonReader.nextString());
            } else if (propertyName.equals("metricCyclomaticComplexity")) {
                row.setMetricCyclomaticComplexity(jsonReader.nextInt());
            } else {
                jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        return row;
    }

}
