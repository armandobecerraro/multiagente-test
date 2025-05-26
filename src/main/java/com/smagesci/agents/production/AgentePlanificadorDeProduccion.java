package com.smagesci.agents.production;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.OneShotBehaviour;

public class AgentePlanificadorDeProduccion extends BaseAgent {

    @Override
    protected void setup() {
        super.setup();
        log.info("AgentePlanificadorDeProduccion {} está listo.", getAID().getName());
        registerService("production-planning", "smagesci-production-planner");

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                log.info("Planificador: Realizando la planificación inicial de producción...");
                // Lógica para consultar demanda, inventarios y crear planes de producción
                // Esto podría implicar enviar mensajes a otros agentes (InventarioPT, GeneradorDemanda)
            }
        });
    }

    // Aquí irían más Behaviours para manejar solicitudes de replanificación, etc.

    @Override
    protected void takeDown() {
        log.info("AgentePlanificadorDeProduccion {} terminando.", getAID().getName());
        super.takeDown();
    }
}
