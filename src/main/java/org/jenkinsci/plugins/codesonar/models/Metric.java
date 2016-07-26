package org.jenkinsci.plugins.codesonar.models;

import java.io.Serializable;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Metric implements Serializable {

    @XmlAttribute
    private String name;
    @XmlValue
    private String value;

    public Metric() {
    }

    public Metric(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Metric)) {
            return false;
        }
        
        Metric other = (Metric) obj;

        if (!this.name.equals(other.name)) {
            return false;
        }
        if (!this.value.equals(other.value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + Objects.hashCode(this.value);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("name: ").append(name);
        sb.append("\n");
        sb.append("value: ").append(value);
        sb.append("\n");

        return sb.toString();
    }
}
