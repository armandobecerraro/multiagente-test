package com.smagesci.agents.supply;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteProveedorDeMateriasPrimas extends BaseAgent {

    private String materialId;
    private int stock;
    private double pricePerUnit;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        if (args != null && args.length == 3) {
            materialId = (String) args[0];
            stock = (Integer) args[1];
            pricePerUnit = (Double) args[2];
            log.info("AgenteProveedorDeMateriasPrimas {} para material {} con stock {} a precio {} está listo.",
                    getAID().getName(), materialId, stock, pricePerUnit);
            registerService("supply-raw-material-" + materialId, "supplier-" + getLocalName());
        } else {
            log.error("AgenteProveedorDeMateriasPrimas {} requiere argumentos: materialId (String), stock (int), pricePerUnit (double).", getAID().getName());
            doDelete(); // Terminar el agente si no tiene los argumentos correctos
            return;
        }

        addBehaviour(new HandleMaterialRequestsBehaviour());
    }

    private class HandleMaterialRequestsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP); // Call For Proposal
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                log.info("Proveedor {} recibió CFP de {}: {}", getLocalName(), msg.getSender().getName(), msg.getContent());
                // Lógica para procesar la solicitud de cotización (CFP)
                // String requestedMaterial = ... parse msg.getContent() ...
                // int requestedQuantity = ... parse msg.getContent() ...

                // if (materialId.equals(requestedMaterial) && stock >= requestedQuantity) {
                //     ACLMessage proposal = msg.createReply();
                //     proposal.setPerformative(ACLMessage.PROPOSE);
                //     proposal.setContent("price:" + pricePerUnit + ",quantity:" + requestedQuantity);
                //     myAgent.send(proposal);
                // } else {
                //     ACLMessage refuse = msg.createReply();
                //     refuse.setPerformative(ACLMessage.REFUSE);
                //     refuse.setContent("Material no disponible o stock insuficiente.");
                //     myAgent.send(refuse);
                // }
            } else {
                block();
            }
        }
    }

    @Override
    protected void takeDown() {
        log.info("AgenteProveedorDeMateriasPrimas {} terminando.", getAID().getName());
        super.takeDown();
    }
}
