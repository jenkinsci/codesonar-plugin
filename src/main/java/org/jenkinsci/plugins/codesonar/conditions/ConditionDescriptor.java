/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codesonar.conditions;

import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author andrius
 * @param <T>
 */
public abstract class ConditionDescriptor<T extends Condition> extends Descriptor<Condition> {

    @Override
    public Condition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        return super.newInstance(req, formData); //To change body of generated methods, choose Tools | Templates.
    }
    
}
