package org.jenkinsci.plugins.codesonar.models.procedures;

import java.io.Serializable;
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
@XmlRootElement(name = "procedures")
@XmlAccessorType(XmlAccessType.FIELD)
public class Procedures implements Serializable {
    @XmlElement(name = "procedure_row")
    private List<ProcedureRow> procedureRows;

    public List<ProcedureRow> getProcedureRows() {
        return procedureRows;
    }

    public void setProcedureRows(List<ProcedureRow> procedureRows) {
        this.procedureRows = procedureRows;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.procedureRows);
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
        final Procedures other = (Procedures) obj;
        if (!Objects.equals(this.procedureRows, other.procedureRows)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Procedures{" + "procedureRows=" + procedureRows + '}';
    }
}
