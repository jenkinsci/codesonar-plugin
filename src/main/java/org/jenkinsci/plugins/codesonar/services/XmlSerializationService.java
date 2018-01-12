package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.BufferedReader;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Serializable;


/**
 *
 * @author andrius
 */
public class XmlSerializationService implements Serializable {

    public <T extends Serializable> T deserialize(InputStream content, Class<T> t) throws AbortException {
        try {
            JAXBContext context = JAXBContext.newInstance(t);
            Unmarshaller un = context.createUnmarshaller();

            return (T) un.unmarshal(content);
        } catch (JAXBException ex) {
            throw new AbortException(ex.getMessage());
        }
    }
}
