package org.jenkinsci.plugins.codesonar.models;

import com.google.gson.annotations.SerializedName;

public enum CodeSonarAlertLevels {
    @SerializedName("RED") RED,
    @SerializedName("YELLOW") YELLOW,
    @SerializedName("BLUE") BLUE,
    @SerializedName("GREEN") GREEN
}
