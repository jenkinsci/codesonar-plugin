package org.jenkinsci.plugins.codesonar.models;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.collections.ListUtils;

/**
 *
 * @author andrius
 */
@XmlRootElement(name = "analysis")
@XmlAccessorType(XmlAccessType.FIELD)
public class Analysis implements Serializable {

    @XmlAttribute
    private String username;
    @XmlAttribute(name = "analysis_name")
    private String analysisName;
    @XmlAttribute(name = "user_id")
    private String userId;
    @XmlAttribute(name = "analysis_id")
    private String analysisId;
    @XmlAttribute
    private String modified;
    @XmlAttribute
    private String created;
    @XmlAttribute
    private String project;
    @XmlAttribute(name = "parent_id")
    private String parentId;
    @XmlAttribute
    private String finished;
    @XmlAttribute
    private String machine;
    @XmlAttribute
    private String address;
    @XmlElement(name = "warning")
    private List<Warning> warnings;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getFinished() {
        return finished;
    }

    public void setFinished(String finished) {
        this.finished = finished;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Warning> getWarnings() {
        if (warnings == null) {
            return ListUtils.EMPTY_LIST;
        }

        return warnings;
    }

    public void setWarnings(List<Warning> warnings) {
        this.warnings = warnings;
    }
}
