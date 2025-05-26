package com.smagesci;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/**
 * AgentLauncher - Lanzador principal del Sistema Multiagente SMAGESCI
 * 
 * Este launcher inicializa y despliega los 20 agentes del sistema distribuidos en 12 categorías:
 * 
 * ESTRATÉGICOS (4): AnalizadorDeDatos, GestorDeRiesgos, EvaluadorDeRendimiento + OrquestadorPrincipal
 * SUMINISTRO (5): Proveedor_A/B/C, CompradorMP, GestorInventarioMP  
 * PRODUCCIÓN (4): PlanificadorProduccion, LineaProduccion_A/B, ControlDeCalidad
 * LOGÍSTICA (5): GestorInventarioPT, DespachadorPedidos, Transportista_A/B/C
 * DEMANDA (3): GeneradorDemanda, ProcesadorPedidosCliente, Cliente_1/2
 * SOPORTE (4): GestorMantenimiento, ServicioAlCliente, GestorDeEnergia, OptimizadorDeRutas
 * FINANCIERO (1): GestorFinanciero
 * ANALYTICS (1): AnalisisDatos  
 * SEGURIDAD (1): GestorSeguridad
 * COMUNICACIONES (1): GestorComunicaciones
 * PLANIFICACIÓN (1): PlanificadorEstrategico
 * GESTIÓN RIESGOS (1): GestorRiesgos
 * MONITOREO (1): GestorMonitoreo - NUEVO
 * OPTIMIZACIÓN (1): GestorOptimizacion - NUEVO
 * 
 * Total: 20+ agentes en un ecosistema robusto de cadena de suministro inteligente
 * Versión: SMAGESCI v2.0 con capacidades de monitoreo y optimización
 */

public class AgentLauncher {

    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        // Conectarse a un Main Container existente
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.MAIN_PORT, "1099");
        // p.setParameter(Profile.CONTAINER_NAME, "AgentsContainer"); // Opcional

        ContainerController agentContainer = rt.createAgentContainer(p);
        System.out.println("Contenedor de agentes creado y conectado al principal.");

        try {
            // Lanzar Agentes de Nivel Estratégico y de Gestión
            // (El Orquestador ya se lanzó en MainContainerLauncher o puedes lanzarlo aquí)
            // mainContainer.createNewAgent("OrquestadorPrincipal", "com.smagesci.agents.strategic.AgenteOrquestadorPrincipal", null).start();
            agentContainer.createNewAgent("AnalizadorDeDatos", "com.smagesci.agents.strategic.AgenteAnalizadorDeDatos", null).start();
            agentContainer.createNewAgent("GestorDeRiesgos", "com.smagesci.agents.strategic.AgenteGestorDeRiesgos", null).start();
            agentContainer.createNewAgent("EvaluadorDeRendimiento", "com.smagesci.agents.strategic.AgenteEvaluadorDeRendimiento", null).start();

            // Agentes de Suministro y Adquisición
            agentContainer.createNewAgent("Proveedor_A", "com.smagesci.agents.supply.AgenteProveedorDeMateriasPrimas", new Object[]{"A", 1000, 5.0}).start(); // ID, Stock Inicial, Precio
            agentContainer.createNewAgent("Proveedor_B", "com.smagesci.agents.supply.AgenteProveedorDeMateriasPrimas", new Object[]{"B", 800, 5.5}).start();
            agentContainer.createNewAgent("Proveedor_C", "com.smagesci.agents.supply.AgenteProveedorDeMateriasPrimas", new Object[]{"C", 1200, 4.8}).start();
            agentContainer.createNewAgent("CompradorMP", "com.smagesci.agents.supply.AgenteCompradorDeMateriasPrimas", null).start();
            agentContainer.createNewAgent("GestorInventarioMP", "com.smagesci.agents.supply.AgenteGestorDeInventarioMP", null).start();

            // Agentes de Producción
            agentContainer.createNewAgent("PlanificadorProduccion", "com.smagesci.agents.production.AgentePlanificadorDeProduccion", null).start();
            agentContainer.createNewAgent("LineaProduccion_A", "com.smagesci.agents.production.AgenteLineaDeProduccion", new Object[]{"A", 50}).start(); // ID, Capacidad/hora
            agentContainer.createNewAgent("LineaProduccion_B", "com.smagesci.agents.production.AgenteLineaDeProduccion", new Object[]{"B", 70}).start();
            agentContainer.createNewAgent("ControlDeCalidad", "com.smagesci.agents.production.AgenteControlDeCalidad", null).start();

            // Agentes de Almacenamiento y Logística
            agentContainer.createNewAgent("GestorInventarioPT", "com.smagesci.agents.logistics.AgenteGestorDeInventarioPT", null).start();
            agentContainer.createNewAgent("DespachadorPedidos", "com.smagesci.agents.logistics.AgenteDespachadorDePedidos", null).start();
            agentContainer.createNewAgent("Transportista_A", "com.smagesci.agents.logistics.AgenteTransportista", new Object[]{"A"}).start(); // ID
            agentContainer.createNewAgent("Transportista_B", "com.smagesci.agents.logistics.AgenteTransportista", new Object[]{"B"}).start();
            agentContainer.createNewAgent("Transportista_C", "com.smagesci.agents.logistics.AgenteTransportista", new Object[]{"C"}).start();

            // Agentes de Demanda y Cliente
            agentContainer.createNewAgent("GeneradorDemanda", "com.smagesci.agents.demand.AgenteGeneradorDeDemanda", null).start();
            agentContainer.createNewAgent("ProcesadorPedidosCliente", "com.smagesci.agents.demand.AgenteProcesadorDePedidosCliente", null).start();
            agentContainer.createNewAgent("Cliente_1", "com.smagesci.agents.demand.AgenteCliente", new Object[]{"1"}).start(); // ID Cliente
            agentContainer.createNewAgent("Cliente_2", "com.smagesci.agents.demand.AgenteCliente", new Object[]{"2"}).start();

            // Agentes de Servicio y Soporte
            agentContainer.createNewAgent("GestorMantenimiento", "com.smagesci.agents.support.AgenteGestorDeMantenimiento", null).start();
            agentContainer.createNewAgent("ServicioAlCliente", "com.smagesci.agents.support.AgenteServicioAlCliente", null).start();
            agentContainer.createNewAgent("GestorDeEnergia", "com.smagesci.agents.support.AgenteGestorDeEnergia", null).start();
            agentContainer.createNewAgent("OptimizadorDeRutas", "com.smagesci.agents.support.AgenteOptimizadorDeRutas", null).start();

            // Agentes Financieros
            agentContainer.createNewAgent("GestorFinanciero", "com.smagesci.agents.finance.AgenteGestorFinanciero", null).start();

            // Agentes de Análisis de Datos
            agentContainer.createNewAgent("AnalisisDatos", "com.smagesci.agents.analytics.AgenteAnalisisDatos", null).start();

            // Agentes de Seguridad
            agentContainer.createNewAgent("GestorSeguridad", "com.smagesci.agents.security.AgenteGestorSeguridad", null).start();

            // Agentes de Comunicaciones
            agentContainer.createNewAgent("GestorComunicaciones", "com.smagesci.agents.communications.AgenteGestorComunicaciones", null).start();

            // Agentes de Planificación Estratégica
            agentContainer.createNewAgent("PlanificadorEstrategico", "com.smagesci.agents.planning.AgentePlanificadorEstrategico", null).start();

            // Agentes de Gestión de Riesgos
            agentContainer.createNewAgent("GestorRiesgos", "com.smagesci.agents.riskmanagement.AgenteGestorRiesgos", null).start();

            // Agentes de Monitoreo (NUEVO)
            agentContainer.createNewAgent("GestorMonitoreo", "com.smagesci.agents.monitoring.AgenteGestorMonitoreo", null).start();

            // Agentes de Optimización (NUEVO)
            agentContainer.createNewAgent("GestorOptimizacion", "com.smagesci.agents.optimization.AgenteGestorOptimizacion", null).start();

            System.out.println("Todos los agentes solicitados han sido lanzados (20 agentes en 12 categorías).");

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
