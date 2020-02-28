package org.jenkinsci.plugins.codesonar.models.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    @XmlElement(name = "alert")
    private List<Alert> alerts;

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

    public List<Alert> getAlerts() {
        if (alerts == null) {
            return ListUtils.EMPTY_LIST;
        }

        return alerts;
    }

    public List<Alert> getRedAlerts() {
        List<Alert> redAlerts = new ArrayList<>();
        
        if (alerts == null)
            return redAlerts;
        
        for (Alert alert : alerts) {
            boolean isRed = false;

            String message = alert.getMessage();

            if (message.contains("Bad File System")) {
                isRed = true;
            } else if (message.contains("Bad Configuration File Setting")) {
                isRed = true;
            } else if (message.contains("Bad Configuration File Settings")) {
                isRed = true;
            } else if (message.contains("Bad Extension Construct")) {
                isRed = true;
            } else if (message.contains("Bad Extension Constructs")) {
                isRed = true;
            } else if (message.contains("Visualization Feature Failure")) {
                isRed = true;
            } else if (message.contains("Analysis Stalled")) {
                isRed = true;
            } else if (message.contains("Miscellaneous Error")) {
                isRed = true;
            } else if (message.contains("Miscellaneous Errors")) {
                isRed = true;
            } else if (message.contains("Missing Debug Information")) {
                isRed = true;
            } else if (message.contains("Missing Source File")) {
                isRed = true;
            } else if (message.contains("Missing Source Files")) {
                isRed = true;
            } else if (message.contains("Native Build Failed")) {
                isRed = true;
            } else if (message.contains("Binary Analysis Configuration Errors")) {
                isRed = true;
            }
            
            if (isRed) {
                redAlerts.add(alert);
            }
        }

        return redAlerts;
    }

    public List<Alert> getYellowAlerts() {
        List<Alert> yellowAlerts = new ArrayList<>();

        if (alerts == null)
        	return yellowAlerts;
        
        for (Alert alert : alerts) {
            boolean isYellow = false;

            String message = alert.getMessage();

            if (message.contains("Parse Error")) {
                isYellow = true;
            } else if (message.contains("Parse Errors")) {
                isYellow = true;
            } else if (message.contains("Undefined Function")) {
                isYellow = true;
            } else if (message.contains("Undefined Functions")) {
                isYellow = true;
            } else if (message.contains("Incremental Parent Analysis Absent")) {
                isYellow = true;
            }
            
            if (isYellow) {
                yellowAlerts.add(alert);
            }
        }

        return yellowAlerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.username);
        hash = 29 * hash + Objects.hashCode(this.analysisName);
        hash = 29 * hash + Objects.hashCode(this.userId);
        hash = 29 * hash + Objects.hashCode(this.analysisId);
        hash = 29 * hash + Objects.hashCode(this.modified);
        hash = 29 * hash + Objects.hashCode(this.created);
        hash = 29 * hash + Objects.hashCode(this.project);
        hash = 29 * hash + Objects.hashCode(this.parentId);
        hash = 29 * hash + Objects.hashCode(this.finished);
        hash = 29 * hash + Objects.hashCode(this.machine);
        hash = 29 * hash + Objects.hashCode(this.address);
        hash = 29 * hash + Objects.hashCode(this.warnings);
        hash = 29 * hash + Objects.hashCode(this.alerts);
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
        final Analysis other = (Analysis) obj;
        if (!Objects.equals(this.username, other.username)) {
            return false;
        }
        if (!Objects.equals(this.analysisName, other.analysisName)) {
            return false;
        }
        if (!Objects.equals(this.userId, other.userId)) {
            return false;
        }
        if (!Objects.equals(this.analysisId, other.analysisId)) {
            return false;
        }
        if (!Objects.equals(this.modified, other.modified)) {
            return false;
        }
        if (!Objects.equals(this.created, other.created)) {
            return false;
        }
        if (!Objects.equals(this.project, other.project)) {
            return false;
        }
        if (!Objects.equals(this.parentId, other.parentId)) {
            return false;
        }
        if (!Objects.equals(this.finished, other.finished)) {
            return false;
        }
        if (!Objects.equals(this.machine, other.machine)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        if (!Objects.equals(this.warnings, other.warnings)) {
            return false;
        }
        if (!Objects.equals(this.alerts, other.alerts)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Analysis{" + "username=" + username + ", analysisName=" + analysisName + ", userId=" + userId + ", analysisId=" + analysisId + ", modified=" + modified + ", created=" + created + ", project=" + project + ", parentId=" + parentId + ", finished=" + finished + ", machine=" + machine + ", address=" + address + ", warnings=" + warnings + ", alerts=" + alerts + '}';
    }
}
