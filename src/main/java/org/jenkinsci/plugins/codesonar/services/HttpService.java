/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codesonar.services;

import java.io.IOException;
import org.apache.http.client.fluent.Request;

/**
 *
 * @author Andrius
 */
public class HttpService {
    public String GetContentFromURLAsString(String url) throws IOException {
        return Request.Get(url).execute().returnContent().asString();
    }
}
