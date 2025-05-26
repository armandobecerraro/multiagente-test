package com.smagesci;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class MainContainerLauncher {

    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true"); // Iniciar el GUI de JADE (RMA)
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.MAIN_PORT, "1099"); // Puerto por defecto de JADE
        p.setParameter(Profile.PLATFORM_ID, "SMAGESCI-Platform");

        ContainerController mainContainer = rt.createMainContainer(p);
        System.out.println("Contenedor principal creado. RMA debería estar visible.");

        // Opcional: Iniciar algunos agentes clave aquí o usar AgentLauncher
        try {
            // Ejemplo: Lanzar el Orquestador Principal
            AgentController orquestador = mainContainer.createNewAgent(
                "OrquestadorPrincipal",
                "com.smagesci.agents.strategic.AgenteOrquestadorPrincipal",
                null // Sin argumentos iniciales
            );
            orquestador.start();

            // Para lanzar el resto de agentes, considera un script o el AgentLauncher
            // Se recomienda lanzar los agentes en sus propios contenedores o con un delay
            // para no sobrecargar el inicio.

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
