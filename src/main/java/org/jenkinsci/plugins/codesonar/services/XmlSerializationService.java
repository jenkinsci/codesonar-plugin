package org.jenkinsci.plugins.codesonar.services;

import hudson.AbortException;
import java.io.Serializable;
import java.io.StringReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author andrius
 */
public class XmlSerializationService implements Serializable {

    public <T extends Serializable> T deserialize(String content, Class<T> t) throws AbortException {
        try {
            JAXBContext context = JAXBContext.newInstance(t);
            Unmarshaller un = context.createUnmarshaller();

            String cleanContent = content.replace("&", "&amp;");
            StringReader reader = new StringReader(cleanContent);

            return (T) un.unmarshal(reader);
        } catch (JAXBException ex) {
            throw new AbortException(ex.getMessage());
        }
    }
}
