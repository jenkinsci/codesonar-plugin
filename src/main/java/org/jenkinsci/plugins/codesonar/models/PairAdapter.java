package org.jenkinsci.plugins.codesonar.models;

import java.lang.reflect.Type;

import org.javatuples.Pair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PairAdapter implements JsonSerializer<Pair<String, String>> {

 @Override
 public JsonElement serialize(Pair<String, String> src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject obj = new JsonObject();
        obj.addProperty(src.getValue0(), src.getValue1());

        return obj;
    }
}