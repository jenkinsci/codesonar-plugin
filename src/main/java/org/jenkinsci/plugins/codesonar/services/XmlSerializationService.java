package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.BufferedInputStream;
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
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            JAXBContext context = JAXBContext.newInstance(t);
            Unmarshaller un = context.createUnmarshaller();
            BufferedInputStream bis = new BufferedInputStream(content);

            return (T) un.unmarshal(bis);
        } catch (JAXBException ex) {
            throw new AbortException(ex.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
