package com.smagesci.agents.logistics;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.HashMap;
import java.util.Map;

public class AgenteGestorDeInventarioPT extends BaseAgent {

    private Map<String, Integer> finishedProductStock;
    // private InventoryDAO inventoryDAO; // Para persistencia

    @Override
    protected void setup() {
        super.setup();
        finishedProductStock = new HashMap<>();
        // inventoryDAO = new InventoryDAO(); // Inicializar DAO
        // loadInventoryFromDB(); // Cargar inventario inicial

        log.info("AgenteGestorDeInventarioPT {} está listo.", getAID().getName());
        registerService("inventory-finished-product", "smagesci-fp-inventory");

        addBehaviour(new ManageInventoryBehaviour());
    }

    private class ManageInventoryBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Escuchar mensajes para actualizar stock, consultar disponibilidad, etc.
            MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST), // Consultas de stock
                MessageTemplate.MatchPerformative(ACLMessage.INFORM)  // Actualizaciones de producción
            );
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                log.info("GestorInventarioPT {} recibió mensaje de {}: {}", getLocalName(), msg.getSender().getName(), msg.getContent());
                // String content = msg.getContent();
                // JSONObject json = new JSONObject(content);
                // String action = json.getString("action");
                // if ("UPDATE_STOCK".equals(action)) { ... }
                // else if ("QUERY_STOCK".equals(action)) { ... }
            } else {
                block();
            }
        }
    }

    // Métodos para actualizar, consultar stock, y persistir cambios
    // private void loadInventoryFromDB() { ... }
    // private void updateStock(String productId, int quantity) { ... }

    @Override
    protected void takeDown() {
        log.info("AgenteGestorDeInventarioPT {} terminando.", getAID().getName());
        super.takeDown();
    }
}
