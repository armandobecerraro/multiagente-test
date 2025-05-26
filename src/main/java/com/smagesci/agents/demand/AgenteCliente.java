package com.smagesci.agents.demand;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.OneShotBehaviour;

public class AgenteCliente extends BaseAgent {

    private String customerId;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        if (args != null && args.length == 1) {
            customerId = (String) args[0];
            log.info("AgenteCliente {} (ID: {}) está listo.", getAID().getName(), customerId);
            registerService("customer-actions", "customer-" + customerId);
        } else {
            log.error("AgenteCliente {} requiere argumento: customerId (String).", getAID().getName());
            doDelete();
            return;
        }

        // Comportamiento inicial: quizás enviar un pedido de prueba o registrarse
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                log.info("Cliente {}: Realizando acción inicial (ej. colocar un pedido).", customerId);
                // Lógica para crear y enviar un ACLMessage a AgenteProcesadorDePedidosCliente
                // ACLMessage orderMsg = new ACLMessage(ACLMessage.REQUEST);
                // orderMsg.addReceiver(new AID("ProcesadorPedidosCliente", AID.ISLOCALNAME));
                // orderMsg.setLanguage("JSON");
                // orderMsg.setOntology("smagesci-order-ontology");
                // JSONObject orderJson = new JSONObject();
                // orderJson.put("action", "PLACE_ORDER");
                // orderJson.put("customerId", customerId);
                // orderJson.put("productId", "PRODUCT_X");
                // orderJson.put("quantity", 10);
                // orderMsg.setContent(orderJson.toString());
                // myAgent.send(orderMsg);
                // log.info("Cliente {}: Pedido enviado.", customerId);
            }
        });
    }

    @Override
    protected void takeDown() {
        log.info("AgenteCliente {} (ID: {}) terminando.", getAID().getName(), customerId);
        super.takeDown();
    }
}
