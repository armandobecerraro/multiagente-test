package com.smagesci.agents.support;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.TickerBehaviour;

public class AgenteGestorDeMantenimiento extends BaseAgent {

    private static final long CHECK_INTERVAL = 60000; // 1 minuto para el ejemplo

    @Override
    protected void setup() {
        super.setup();
        log.info("AgenteGestorDeMantenimiento {} está listo.", getAID().getName());
        registerService("maintenance-management", "smagesci-maintenance-manager");

        addBehaviour(new TickerBehaviour(this, CHECK_INTERVAL) {
            @Override
            protected void onTick() {
                log.info("GestorMantenimiento: Verificando estado de equipos y programando mantenimiento preventivo...");
                // Lógica para:
                // 1. Consultar estado de AgenteLineaDeProduccion (u otros agentes con equipos)
                // 2. Analizar datos de uso o fallos (podría interactuar con AgenteAnalizadorDeDatos)
                // 3. Programar tareas de mantenimiento y notificar a los agentes correspondientes
                // ACLMessage checkMsg = new ACLMessage(ACLMessage.QUERY_IF);
                // checkMsg.addReceiver(new AID("LineaProduccion_A", AID.ISLOCALNAME));
                // checkMsg.setContent("request_status");
                // send(checkMsg);
            }
        });
    }

    @Override
    protected void takeDown() {
        log.info("AgenteGestorDeMantenimiento {} terminando.", getAID().getName());
        super.takeDown();
    }
}
