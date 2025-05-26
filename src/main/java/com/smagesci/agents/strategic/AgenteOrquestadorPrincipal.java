package com.smagesci.agents.strategic;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteOrquestadorPrincipal extends BaseAgent {

    @Override
    protected void setup() {
        super.setup();
        log.info("AgenteOrquestadorPrincipal {} está listo.", getAID().getName());
        registerService("orchestration", "smagesci-orchestrator");

        addBehaviour(new OrchestrationBehaviour());
    }

    private class OrchestrationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Ejemplo: Escuchar mensajes de otros agentes para coordinar
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST); // Ejemplo
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                log.info("Orquestador recibió un mensaje de {}: {}", msg.getSender().getName(), msg.getContent());
                // Lógica de procesamiento y coordinación
                // ACLMessage reply = msg.createReply();
                // reply.setPerformative(ACLMessage.INFORM);
                // reply.setContent("Acción coordinada iniciada.");
                // myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        log.info("AgenteOrquestadorPrincipal {} terminando.", getAID().getName());
        super.takeDown();
    }
}
