package org.jenkinsci.plugins.pluginusage.analyzer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hudson.PluginWrapper;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.promoted_builds.PromotedProjectAction;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;

abstract class AbstractProjectAnalyzer {

    protected PluginWrapper getPluginFromClass(Class<?> clazz) {
        return Jenkins.get().getPluginManager().whichPlugin(clazz);
    }

    @Deprecated
    protected PluginWrapper getUsedPlugin(Class<?> clazz) {
        return getPluginFromClass(clazz);
    }

    protected Set<PluginWrapper> getPlugins(){
        Set<PluginWrapper> plugins = new HashSet<>();

        for (Descriptor<Builder> b : Builder.all()) {
            plugins.add(getPluginFromClass(b.clazz));
        }
        for (Descriptor<BuildWrapper> b : BuildWrapper.all()) {
            plugins.add(getPluginFromClass(b.clazz));
        }
        for(Descriptor<Publisher> b: Publisher.all())
        {
            plugins.add(getPluginFromClass(b.clazz));
        }
        for(SCMDescriptor<?> b: SCM.all())
        {
            plugins.add(getPluginFromClass(b.clazz));
        }
        for(TriggerDescriptor b: Trigger.all())
        {
            plugins.add(getPluginFromClass(b.clazz));
        }
        return plugins;
    }

    protected Set<PluginWrapper> getPluginsFromItem(Job<?,?> item){
        Set<PluginWrapper> plugins = new HashSet<>();
        plugins.addAll(getPluginsFromBuilders(item));
        plugins.addAll(getPluginsFromProperties(item));
        plugins.addAll(getPluginsFromScm(item));
        plugins.addAll(getPluginsFromTriggers(item));
        plugins.addAll(getPluginsFromPublishers(item));
        plugins.addAll(getPluginsFromParameters(item));
        plugins.addAll(getPluginsFromPromotedBuilds(item));
        return plugins;
    }

    protected boolean ignoreJob(Job<?,?> item){
        return false;
    }

    protected abstract Set<PluginWrapper> getPluginsFromBuilders(Job<?,?> item);

    protected Set<PluginWrapper> getPluginsFromPromotedBuilds(Job item) {
        Set<PluginWrapper> plugins = new HashSet<>();

        if (Jenkins.get().getPlugin("promoted-builds") == null){
            return plugins;
        }
        PromotedProjectAction action = item.getAction(PromotedProjectAction.class);
        if (action != null){
            List<PromotionProcess> processes = action.getProcesses();
            for (PromotionProcess process: processes) {
                List<BuildStep> buildSteps = process.getBuildSteps();
                for (BuildStep buildStep: buildSteps) {
                    if (buildStep instanceof Builder){
                        Builder innerBuilder = (Builder) buildStep;
                        plugins.add(getPluginFromClass(innerBuilder.getDescriptor().clazz));
                    }
                    if (buildStep instanceof Publisher){
                        Publisher innerPublisher = (Publisher) buildStep;
                        plugins.add(getPluginFromClass(innerPublisher.getDescriptor().clazz));
                    }
                }
            }
        }

        return plugins;
    }

    protected Set<PluginWrapper> getPluginsFromParameters(Job<?, ?> project) {
        Set<PluginWrapper> plugins = new HashSet<>();
        ParametersDefinitionProperty parameters = project.getAction(ParametersDefinitionProperty.class);
        if (parameters != null) {
            List<ParameterDefinition> parameterDefinitions = parameters.getParameterDefinitions();
            for (ParameterDefinition parameterDefinition : parameterDefinitions) {
                plugins.add(getPluginFromClass(parameterDefinition.getDescriptor().clazz));
            }
        }
        return plugins;
    }

    protected Set<PluginWrapper> getPluginsFromScm(Job<?,?> item){
        Set<PluginWrapper> plugins = new HashSet<>();
        if(item instanceof AbstractProject){
            plugins.add(getPluginFromClass(((AbstractProject<?, ?>)item).getScm().getDescriptor().clazz));
        }
        return plugins;
    }

    protected Set<PluginWrapper> getPluginsFromPublishers(Job<?,?> item){
        Set<PluginWrapper> plugins = new HashSet<>();
        if(item instanceof AbstractProject){
            DescribableList<Publisher, Descriptor<Publisher>> publisherList = ((AbstractProject)item).getPublishersList();
            Map<Descriptor<Publisher>, Publisher> map = publisherList.toMap();
            for (Map.Entry<Descriptor<Publisher>, Publisher> entry : map.entrySet())
            {
                plugins.add(getPluginFromClass(entry.getKey().clazz));
            }
        }
        return plugins;
    }

    protected Set<PluginWrapper> getPluginsFromTriggers(Job<?,?> item){
        Set<PluginWrapper> plugins = new HashSet<>();
        if(item instanceof AbstractProject){
            Map<TriggerDescriptor, Trigger<?>> triggers = ((AbstractProject)item).getTriggers();
            for (Map.Entry<TriggerDescriptor,Trigger<?>> entry : triggers.entrySet()) {
                plugins.add(getPluginFromClass(entry.getKey().clazz));
            }
        }
        return plugins;
    }

    protected Set<PluginWrapper> getPluginsFromProperties(Job item) {
        Set<PluginWrapper> plugins = new HashSet<>();

        Map<JobPropertyDescriptor,JobProperty<?>> properties = item.getProperties();
        for (Map.Entry<JobPropertyDescriptor,JobProperty<?>> entry : properties.entrySet())
        {
            plugins.add(getPluginFromClass(entry.getKey().clazz));
        }

        return plugins;
    }

    protected Set<PluginWrapper> getPluginsFromBuilder(Builder builder) {
        Set<PluginWrapper> plugins = new HashSet<>();

        plugins.add(getPluginFromClass(builder.getDescriptor().clazz));

        if (Jenkins.get().getPlugin("conditional-buildstep") == null){
            return plugins;
        }

        if(builder instanceof ConditionalBuilder){
            ConditionalBuilder conditionalBuilder = (ConditionalBuilder) builder;
            List<Builder> conditionalBuilders = conditionalBuilder.getConditionalbuilders();
            for (Builder innerBuilder: conditionalBuilders) {
                plugins.add(getPluginFromClass(innerBuilder.getDescriptor().clazz));
            }
        }
        if(builder instanceof SingleConditionalBuilder){
            SingleConditionalBuilder singleConditionalBuilder = (SingleConditionalBuilder) builder;
            BuildStep buildStep = singleConditionalBuilder.getBuildStep();
            if (buildStep instanceof Builder){
                Builder innerBuilder = (Builder) buildStep;
                plugins.add(getPluginFromClass(innerBuilder.getDescriptor().clazz));
            }
        }
        return plugins;
    }

}
