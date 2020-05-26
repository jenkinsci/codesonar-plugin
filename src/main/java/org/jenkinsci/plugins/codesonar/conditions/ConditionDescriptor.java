package org.jenkinsci.plugins.codesonar.conditions;

import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

/**
 *
 * @author andrius
 * @param <T>
 */
public abstract class ConditionDescriptor<T extends Condition> extends Descriptor<Condition> {

    public ListBoxModel doFillWarrantedResultItems() {
        ListBoxModel output = new ListBoxModel();
        output.add(new ListBoxModel.Option("Unstable", Result.UNSTABLE.toString()));
        output.add(new ListBoxModel.Option("Failed", Result.FAILURE.toString()));
        return output;
    }

    @Override
    public Condition newInstance(StaplerRequest req, @Nonnull JSONObject formData) throws FormException {
        return super.newInstance(req, formData);
    }

}
