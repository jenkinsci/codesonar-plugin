package org.jenkinsci.plugins.codesonar.services;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.jenkinsci.plugins.codesonar.CodeSonarPluginException;

/**
 *
 * @author andrius
 */
public class XmlSerializationService implements Serializable {
    
    private CodeSonarPluginException createError(String msg, Throwable cause) {
        return new CodeSonarPluginException(msg, cause);
    }

    public <T extends Serializable> T deserialize(InputStream content, Class<T> t) throws CodeSonarPluginException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            JAXBContext context = JAXBContext.newInstance(t);
            Unmarshaller un = context.createUnmarshaller();
            BufferedInputStream bis = new BufferedInputStream(content);

            return (T) un.unmarshal(bis);
        } catch (JAXBException ex) {
            throw createError("Error deserializing XML.", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
