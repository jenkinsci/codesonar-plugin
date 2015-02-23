/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codesonar.services;

import java.io.Serializable;
import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author andrius
 */
public class XmlSerializationService {

    public <T> T deserialize(String content, Class<T> t) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(t);
        Unmarshaller un = context.createUnmarshaller();

        String cleanContent  = content.replaceAll("&", "&amp;");
        StringReader reader = new StringReader(cleanContent);

        return (T) un.unmarshal(reader);
    }
}
