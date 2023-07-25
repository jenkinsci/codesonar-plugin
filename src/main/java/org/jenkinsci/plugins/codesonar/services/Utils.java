package org.jenkinsci.plugins.codesonar.services;

import org.apache.commons.lang.StringUtils;

public class Utils {
    
    /**
     * How to format a query-string parameter values according to both parameter type and hub requirements
     * @param parameter
     * @param strictQueryParameters Depends on hub requirements
     * @return The formatted parameter value
     */
    public static String formatParameter(String parameter, boolean strictQueryParameters) {
        //Remove any unwanted leading or trailing white space character
        parameter = StringUtils.strip(parameter);
        try {
            Long.parseLong(parameter);
        } catch(NumberFormatException e) {
            if(strictQueryParameters) {
                //Surround with double quotes only if parameter is not numeric and the hub supports strict query parameters
                parameter = String.format("\"%s\"", parameter);
            }
        }
        return parameter;
    }

}
