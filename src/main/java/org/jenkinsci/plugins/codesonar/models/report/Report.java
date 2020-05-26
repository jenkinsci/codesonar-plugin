package org.jenkinsci.plugins.codesonar.models.report;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Andrius
 */
@XmlRootElement(name = "report")
@XmlAccessorType(XmlAccessType.FIELD)
public class Report {

    @XmlElement(name = "table")
    private List<Table> tables;

    public List<Table> getTables() {
        if (tables == null) {
            return Collections.EMPTY_LIST;
        }

        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.tables);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Report other = (Report) obj;
        if (!Objects.equals(this.tables, other.tables)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Report{" + "tables=" + tables + '}';
    }
}
