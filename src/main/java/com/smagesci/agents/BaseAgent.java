package com.smagesci.agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAgent extends Agent {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void setup() {
        log.info("Agent {} is initializing...", getAID().getName());
        // Common setup tasks can be added here
    }

    @Override
    protected void takeDown() {
        log.info("Agent {} is shutting down.", getAID().getName());
        // Unregister from DF before shutting down
        try {
            DFService.deregister(this);
            log.info("Agent {} deregistered from DF.", getAID().getName());
        } catch (FIPAException e) {
            log.error("Error deregistering agent {} from DF: {}", getAID().getName(), e.getMessage());
        }
        // Common takedown tasks
    }

    protected void registerService(String serviceType, String serviceName) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(serviceName); // Unique name for this instance of the service
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            log.info("Agent {} registered service type '{}' with name '{}'", getAID().getName(), serviceType, serviceName);
        } catch (FIPAException fe) {
            log.error("Error registering service for agent {}: {}", getAID().getName(), fe.getMessage());
        }
    }
}
