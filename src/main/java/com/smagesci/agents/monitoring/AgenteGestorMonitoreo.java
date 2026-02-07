package com.smagesci.agents.monitoring;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agente Gestor de Monitoreo - Responsable del monitoreo integral del sistema
 * Supervisa rendimiento, disponibilidad, recursos, alertas y salud general del sistema
 */
public class AgenteGestorMonitoreo extends BaseAgent {
    
    // Constantes para tipos de acción
    public static final String MONITOR_SYSTEM_HEALTH = "MONITOR_SYSTEM_HEALTH";
    public static final String MONITOR_PERFORMANCE = "MONITOR_PERFORMANCE";
    public static final String MONITOR_RESOURCES = "MONITOR_RESOURCES";
    public static final String GENERATE_ALERT = "GENERATE_ALERT";
    public static final String COLLECT_METRICS = "COLLECT_METRICS";
    public static final String GENERATE_MONITORING_REPORT = "GENERATE_MONITORING_REPORT";
    public static final String UPDATE_THRESHOLDS = "UPDATE_THRESHOLDS";
    public static final String PREDICT_FAILURES = "PREDICT_FAILURES";
    public static final String TRACK_SLA = "TRACK_SLA";
    public static final String DASHBOARD_UPDATE = "DASHBOARD_UPDATE";
    
    // Estructuras de datos para monitoreo
    private Map<String, SistemaMonitoreado> sistemas = new ConcurrentHashMap<>();
    private Map<String, MetricaRendimiento> metricasRendimiento = new ConcurrentHashMap<>();
    private Map<String, UmbralMonitoreo> umbrales = new ConcurrentHashMap<>();
    private List<AlertaMonitoreo> alertasActivas = new ArrayList<>();
    private Map<String, HistorialMetricas> historialMetricas = new ConcurrentHashMap<>();
    private ConfiguracionMonitoreo configuracion;
    private Map<String, SLA> acuerdosNivel = new ConcurrentHashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void setup() {
        super.setup();
        
        inicializarConfiguracionMonitoreo();
        inicializarSistemasMonitoreados();
        inicializarUmbrales();
        inicializarSLA();
        
        // Comportamiento para monitoreo continuo de salud del sistema
        addBehaviour(new TickerBehaviour(this, 10000) { // Cada 10 segundos
            @Override
            protected void onTick() {
                ejecutarMonitoreoSaludSistema();
            }
        });
        
        // Comportamiento para recolección de métricas de rendimiento
        addBehaviour(new TickerBehaviour(this, 30000) { // Cada 30 segundos
            @Override
            protected void onTick() {
                recolectarMetricasRendimiento();
            }
        });
        
        // Comportamiento para monitoreo de recursos
        addBehaviour(new TickerBehaviour(this, 60000) { // Cada minuto
            @Override
            protected void onTick() {
                monitorearRecursos();
            }
        });
        
        // Comportamiento para predicción de fallos
        addBehaviour(new TickerBehaviour(this, 300000) { // Cada 5 minutos
            @Override
            protected void onTick() {
                ejecutarPrediccionFallos();
            }
        });
        
        // Comportamiento para seguimiento de SLA
        addBehaviour(new TickerBehaviour(this, 900000) { // Cada 15 minutos
            @Override
            protected void onTick() {
                evaluarCumplimientoSLA();
            }
        });
        
        // Comportamiento principal para procesamiento de solicitudes
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    procesarSolicitudMonitoreo(msg);
                } else {
                    block();
                }
            }
        });
        
        log.info("AgenteGestorMonitoreo iniciado con {} sistemas monitoreados", sistemas.size());
    }
    
    private void inicializarConfiguracionMonitoreo() {
        configuracion = new ConfiguracionMonitoreo();
        configuracion.frecuenciaMonitoreoSalud = 10; // segundos
        configuracion.frecuenciaMetricas = 30; // segundos
        configuracion.frecuenciaRecursos = 60; // segundos
        configuracion.retencionHistorial = 30; // días
        configuracion.nivelLogging = "INFO";
        configuracion.habilitarPrediccionFallos = true;
        configuracion.umbralCritico = 0.95;
        configuracion.umbralAdvertencia = 0.80;
        
        log.info("Configuración de monitoreo inicializada: {}", configuracion);
    }
    
    private void inicializarSistemasMonitoreados() {
        // Sistema de bases de datos
        SistemaMonitoreado db = new SistemaMonitoreado();
        db.id = "DATABASE_SYSTEM";
        db.nombre = "Sistema de Base de Datos";
        db.tipo = "DATABASE";
        db.estado = "ACTIVO";
        db.disponibilidad = 0.999;
        db.ultimaVerificacion = LocalDateTime.now();
        db.metricas = Map.of(
            "conexiones_activas", 25.0,
            "tiempo_respuesta_ms", 120.0,
            "uso_cpu", 0.45,
            "uso_memoria", 0.62
        );
        sistemas.put(db.id, db);
        
        // Sistema de comunicaciones
        SistemaMonitoreado comm = new SistemaMonitoreado();
        comm.id = "COMMUNICATION_SYSTEM";
        comm.nombre = "Sistema de Comunicaciones";
        comm.tipo = "COMMUNICATION";
        comm.estado = "ACTIVO";
        comm.disponibilidad = 0.995;
        comm.ultimaVerificacion = LocalDateTime.now();
        comm.metricas = Map.of(
            "mensajes_procesados", 150.0,
            "latencia_ms", 85.0,
            "throughput_msg_min", 200.0,
            "errores_comunicacion", 2.0
        );
        sistemas.put(comm.id, comm);
        
        // Sistema de agentes
        SistemaMonitoreado agents = new SistemaMonitoreado();
        agents.id = "AGENT_SYSTEM";
        agents.nombre = "Sistema de Agentes";
        agents.tipo = "MULTIAGENT";
        agents.estado = "ACTIVO";
        agents.disponibilidad = 0.998;
        agents.ultimaVerificacion = LocalDateTime.now();
        agents.metricas = Map.of(
            "agentes_activos", 18.0,
            "mensajes_intercambiados", 320.0,
            "tiempo_procesamiento_ms", 95.0,
            "memoria_utilizada_mb", 512.0
        );
        sistemas.put(agents.id, agents);
        
        log.info("Sistemas monitoreados inicializados: {}", sistemas.size());
    }
    
    private void inicializarUmbrales() {
        // Umbrales para base de datos
        UmbralMonitoreo dbThreshold = new UmbralMonitoreo();
        dbThreshold.sistemaId = "DATABASE_SYSTEM";
        dbThreshold.metrica = "tiempo_respuesta_ms";
        dbThreshold.valorAdvertencia = 200.0;
        dbThreshold.valorCritico = 500.0;
        dbThreshold.accion = "GENERAR_ALERTA";
        umbrales.put("DB_RESPONSE_TIME", dbThreshold);
        
        // Umbrales para comunicaciones
        UmbralMonitoreo commThreshold = new UmbralMonitoreo();
        commThreshold.sistemaId = "COMMUNICATION_SYSTEM";
        commThreshold.metrica = "latencia_ms";
        commThreshold.valorAdvertencia = 150.0;
        commThreshold.valorCritico = 300.0;
        commThreshold.accion = "GENERAR_ALERTA";
        umbrales.put("COMM_LATENCY", commThreshold);
        
        // Umbrales para agentes
        UmbralMonitoreo agentThreshold = new UmbralMonitoreo();
        agentThreshold.sistemaId = "AGENT_SYSTEM";
        agentThreshold.metrica = "tiempo_procesamiento_ms";
        agentThreshold.valorAdvertencia = 200.0;
        agentThreshold.valorCritico = 500.0;
        agentThreshold.accion = "GENERAR_ALERTA";
        umbrales.put("AGENT_PROCESSING_TIME", agentThreshold);
        
        log.info("Umbrales de monitoreo configurados: {}", umbrales.size());
    }
    
    private void inicializarSLA() {
        // SLA para disponibilidad del sistema
        SLA disponibilidadSLA = new SLA();
        disponibilidadSLA.id = "SYSTEM_AVAILABILITY";
        disponibilidadSLA.nombre = "Disponibilidad del Sistema";
        disponibilidadSLA.descripcion = "Garantía de disponibilidad del 99.5%";
        disponibilidadSLA.valorObjetivo = 0.995;
        disponibilidadSLA.valorActual = 0.998;
        disponibilidadSLA.periodo = "MENSUAL";
        disponibilidadSLA.estado = "CUMPLIENDO";
        disponibilidadSLA.fechaInicio = LocalDateTime.now().minusDays(30);
        disponibilidadSLA.fechaFin = LocalDateTime.now().plusDays(1);
        acuerdosNivel.put(disponibilidadSLA.id, disponibilidadSLA);
        
        // SLA para tiempo de respuesta
        SLA tiempoRespuestaSLA = new SLA();
        tiempoRespuestaSLA.id = "RESPONSE_TIME";
        tiempoRespuestaSLA.nombre = "Tiempo de Respuesta";
        tiempoRespuestaSLA.descripcion = "Tiempo de respuesta promedio menor a 200ms";
        tiempoRespuestaSLA.valorObjetivo = 200.0;
        tiempoRespuestaSLA.valorActual = 120.0;
        tiempoRespuestaSLA.periodo = "DIARIO";
        tiempoRespuestaSLA.estado = "CUMPLIENDO";
        tiempoRespuestaSLA.fechaInicio = LocalDateTime.now().minusDays(1);
        tiempoRespuestaSLA.fechaFin = LocalDateTime.now().plusDays(1);
        acuerdosNivel.put(tiempoRespuestaSLA.id, tiempoRespuestaSLA);
        
        log.info("SLAs inicializados: {}", acuerdosNivel.size());
    }
    
    private void procesarSolicitudMonitoreo(ACLMessage msg) {
        try {
            String action = msg.getOntology();
            String content = msg.getContent();
            
            switch (action) {
                case MONITOR_SYSTEM_HEALTH:
                    procesarMonitoreoSalud(msg, content);
                    break;
                case MONITOR_PERFORMANCE:
                    procesarMonitoreoRendimiento(msg, content);
                    break;
                case MONITOR_RESOURCES:
                    procesarMonitoreoRecursos(msg, content);
                    break;
                case GENERATE_ALERT:
                    procesarGeneracionAlerta(msg, content);
                    break;
                case COLLECT_METRICS:
                    procesarRecoleccionMetricas(msg, content);
                    break;
                case GENERATE_MONITORING_REPORT:
                    generarReporteMonitoreo(msg, content);
                    break;
                case UPDATE_THRESHOLDS:
                    actualizarUmbrales(msg, content);
                    break;
                case PREDICT_FAILURES:
                    procesarPrediccionFallos(msg, content);
                    break;
                case TRACK_SLA:
                    procesarSeguimientoSLA(msg, content);
                    break;
                case DASHBOARD_UPDATE:
                    actualizarDashboard(msg, content);
                    break;
                default:
                    log.warn("Acción de monitoreo no reconocida: {}", action);
            }
        } catch (Exception e) {
            log.error("Error procesando solicitud de monitoreo: {}", e.getMessage());
        }
    }
    
    private void ejecutarMonitoreoSaludSistema() {
        for (SistemaMonitoreado sistema : sistemas.values()) {
            // Simular verificación de salud
            double saludActual = Math.random() * 0.2 + 0.8; // Entre 0.8 y 1.0
            sistema.disponibilidad = saludActual;
            sistema.ultimaVerificacion = LocalDateTime.now();
            
            // Actualizar estado basado en disponibilidad
            if (saludActual >= configuracion.umbralCritico) {
                sistema.estado = "OPTIMO";
            } else if (saludActual >= configuracion.umbralAdvertencia) {
                sistema.estado = "ADVERTENCIA";
                generarAlertaMonitoreo(sistema.id, "ADVERTENCIA", 
                    "Disponibilidad del sistema " + sistema.nombre + " en estado de advertencia: " + String.format("%.2f%%", saludActual * 100));
            } else {
                sistema.estado = "CRITICO";
                generarAlertaMonitoreo(sistema.id, "CRITICO", 
                    "Disponibilidad del sistema " + sistema.nombre + " en estado crítico: " + String.format("%.2f%%", saludActual * 100));
            }
        }
        
        log.debug("Monitoreo de salud del sistema ejecutado para {} sistemas", sistemas.size());
    }
    
    private void recolectarMetricasRendimiento() {
        for (SistemaMonitoreado sistema : sistemas.values()) {
            // Simular recolección de métricas
            Map<String, Double> nuevasMetricas = new HashMap<>();
            
            switch (sistema.tipo) {
                case "DATABASE":
                    nuevasMetricas.put("conexiones_activas", Math.random() * 20 + 20);
                    nuevasMetricas.put("tiempo_respuesta_ms", Math.random() * 100 + 80);
                    nuevasMetricas.put("uso_cpu", Math.random() * 0.3 + 0.3);
                    nuevasMetricas.put("uso_memoria", Math.random() * 0.2 + 0.5);
                    break;
                case "COMMUNICATION":
                    nuevasMetricas.put("mensajes_procesados", Math.random() * 100 + 100);
                    nuevasMetricas.put("latencia_ms", Math.random() * 50 + 60);
                    nuevasMetricas.put("throughput_msg_min", Math.random() * 100 + 150);
                    nuevasMetricas.put("errores_comunicacion", Math.random() * 5);
                    break;
                case "MULTIAGENT":
                    nuevasMetricas.put("agentes_activos", 18.0);
                    nuevasMetricas.put("mensajes_intercambiados", Math.random() * 200 + 250);
                    nuevasMetricas.put("tiempo_procesamiento_ms", Math.random() * 50 + 70);
                    nuevasMetricas.put("memoria_utilizada_mb", Math.random() * 100 + 450);
                    break;
            }
            
            sistema.metricas = nuevasMetricas;
            
            // Almacenar en historial
            HistorialMetricas historial = historialMetricas.computeIfAbsent(sistema.id, k -> new HistorialMetricas());
            historial.sistemaId = sistema.id;
            historial.registros.add(new RegistroMetrica(LocalDateTime.now(), nuevasMetricas));
            
            // Verificar umbrales
            verificarUmbrales(sistema.id, nuevasMetricas);
        }
        
        log.debug("Métricas de rendimiento recolectadas para {} sistemas", sistemas.size());
    }
    
    private void monitorearRecursos() {
        // Simular monitoreo de recursos del sistema
        Map<String, Double> recursosGlobales = new HashMap<>();
        recursosGlobales.put("cpu_total", Math.random() * 0.3 + 0.4);
        recursosGlobales.put("memoria_total", Math.random() * 0.2 + 0.6);
        recursosGlobales.put("disco_total", Math.random() * 0.1 + 0.3);
        recursosGlobales.put("red_utilizacion", Math.random() * 0.2 + 0.2);
        
        // Crear métrica de recursos
        MetricaRendimiento recursosMetrica = new MetricaRendimiento();
        recursosMetrica.sistemaId = "GLOBAL_RESOURCES";
        recursosMetrica.timestamp = LocalDateTime.now();
        recursosMetrica.valores = recursosGlobales;
        recursosMetrica.promedio = recursosGlobales.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        metricasRendimiento.put("RESOURCES_" + LocalDateTime.now().toString(), recursosMetrica);
        
        // Verificar si los recursos están bajo estrés
        if (recursosMetrica.promedio > 0.85) {
            generarAlertaMonitoreo("GLOBAL_RESOURCES", "ADVERTENCIA", 
                "Alto uso de recursos del sistema: " + String.format("%.2f%%", recursosMetrica.promedio * 100));
        }
        
        log.debug("Monitoreo de recursos ejecutado - Uso promedio: {:.2f}%", recursosMetrica.promedio * 100);
    }
    
    private void verificarUmbrales(String sistemaId, Map<String, Double> metricas) {
        for (UmbralMonitoreo umbral : umbrales.values()) {
            if (umbral.sistemaId.equals(sistemaId) && metricas.containsKey(umbral.metrica)) {
                double valor = metricas.get(umbral.metrica);
                
                if (valor >= umbral.valorCritico) {
                    generarAlertaMonitoreo(sistemaId, "CRITICO", 
                        "Métrica " + umbral.metrica + " en nivel crítico: " + valor);
                } else if (valor >= umbral.valorAdvertencia) {
                    generarAlertaMonitoreo(sistemaId, "ADVERTENCIA", 
                        "Métrica " + umbral.metrica + " en nivel de advertencia: " + valor);
                }
            }
        }
    }
    
    private void generarAlertaMonitoreo(String sistemaId, String severidad, String mensaje) {
        AlertaMonitoreo alerta = new AlertaMonitoreo();
        alerta.id = UUID.randomUUID().toString();
        alerta.sistemaId = sistemaId;
        alerta.severidad = severidad;
        alerta.mensaje = mensaje;
        alerta.timestamp = LocalDateTime.now();
        alerta.estado = "ACTIVA";
        alerta.notificada = false;
        
        alertasActivas.add(alerta);
        
        log.warn("Alerta generada [{}] para sistema {}: {}", severidad, sistemaId, mensaje);
        
        // Notificar a otros agentes si es necesario
        if ("CRITICO".equals(severidad)) {
            notificarAlertaCritica(alerta);
        }
    }
    
    private void notificarAlertaCritica(AlertaMonitoreo alerta) {
        // Enviar notificación al agente de comunicaciones
        try {
            ACLMessage notificacion = new ACLMessage(ACLMessage.INFORM);
            notificacion.addReceiver(new jade.core.AID("AgenteGestorComunicaciones", jade.core.AID.ISLOCALNAME));
            notificacion.setOntology("SEND_ALERT");
            
            Map<String, Object> alertData = Map.of(
                "tipo", "MONITOREO_CRITICO",
                "sistema", alerta.sistemaId,
                "mensaje", alerta.mensaje,
                "timestamp", alerta.timestamp.toString(),
                "severidad", alerta.severidad
            );
            
            notificacion.setContent(objectMapper.writeValueAsString(alertData));
            send(notificacion);
            
            alerta.notificada = true;
            log.info("Notificación de alerta crítica enviada para sistema: {}", alerta.sistemaId);
            
        } catch (Exception e) {
            log.error("Error enviando notificación de alerta crítica: {}", e.getMessage());
        }
    }
    
    private void ejecutarPrediccionFallos() {
        if (!configuracion.habilitarPrediccionFallos) return;
        
        for (SistemaMonitoreado sistema : sistemas.values()) {
            HistorialMetricas historial = historialMetricas.get(sistema.id);
            if (historial != null && historial.registros.size() >= 5) {
                // Análisis simple de tendencias
                List<RegistroMetrica> ultimos = historial.registros.stream()
                    .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                    .limit(5)
                    .collect(Collectors.toList());
                
                // Verificar tendencia de degradación en métricas clave
                boolean tendenciaNegativa = analizarTendencia(ultimos, sistema.tipo);
                
                if (tendenciaNegativa) {
                    generarAlertaMonitoreo(sistema.id, "ADVERTENCIA", 
                        "Predicción: Posible degradación del rendimiento detectada en " + sistema.nombre);
                    
                    log.warn("Predicción de fallo potencial para sistema: {}", sistema.nombre);
                }
            }
        }
    }
    
    private boolean analizarTendencia(List<RegistroMetrica> registros, String tipoSistema) {
        // Análisis simplificado de tendencias
        if (registros.size() < 3) return false;
        
        String metricaClave = switch (tipoSistema) {
            case "DATABASE" -> "tiempo_respuesta_ms";
            case "COMMUNICATION" -> "latencia_ms";
            case "MULTIAGENT" -> "tiempo_procesamiento_ms";
            default -> "uso_cpu";
        };
        
        List<Double> valores = registros.stream()
            .map(r -> r.metricas.getOrDefault(metricaClave, 0.0))
            .collect(Collectors.toList());
        
        // Verificar si los últimos 3 valores muestran incremento sostenido
        return valores.size() >= 3 && 
               valores.get(0) > valores.get(1) && 
               valores.get(1) > valores.get(2);
    }
    
    private void evaluarCumplimientoSLA() {
        for (SLA sla : acuerdosNivel.values()) {
            // Calcular valor actual basado en métricas recientes
            double valorActual = calcularValorActualSLA(sla);
            sla.valorActual = valorActual;
            
            // Evaluar cumplimiento
            boolean cumpliendo = evaluarCumplimiento(sla, valorActual);
            String estadoAnterior = sla.estado;
            sla.estado = cumpliendo ? "CUMPLIENDO" : "INCUMPLIENDO";
            
            // Generar alerta si cambia el estado
            if (!sla.estado.equals(estadoAnterior)) {
                generarAlertaMonitoreo("SLA_" + sla.id, 
                    cumpliendo ? "INFO" : "ADVERTENCIA",
                    "SLA " + sla.nombre + " ahora está " + sla.estado.toLowerCase());
            }
            
            log.debug("SLA {} evaluado: {} (valor actual: {}, objetivo: {})", 
                sla.nombre, sla.estado, valorActual, sla.valorObjetivo);
        }
    }
    
    private double calcularValorActualSLA(SLA sla) {
        // Simplificación: usar promedios de disponibilidad/rendimiento recientes
        return switch (sla.id) {
            case "SYSTEM_AVAILABILITY" -> sistemas.values().stream()
                .mapToDouble(s -> s.disponibilidad)
                .average()
                .orElse(0.0);
            case "RESPONSE_TIME" -> sistemas.values().stream()
                .flatMap(s -> s.metricas.values().stream())
                .filter(v -> v < 1000) // filtrar métricas de tiempo
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            default -> sla.valorActual;
        };
    }
    
    private boolean evaluarCumplimiento(SLA sla, double valorActual) {
        return switch (sla.id) {
            case "SYSTEM_AVAILABILITY" -> valorActual >= sla.valorObjetivo;
            case "RESPONSE_TIME" -> valorActual <= sla.valorObjetivo;
            default -> true;
        };
    }
    
    private void procesarMonitoreoSalud(ACLMessage msg, String content) {
        ejecutarMonitoreoSaludSistema();
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent("Monitoreo de salud del sistema ejecutado exitosamente");
        send(reply);
        
        log.info("Monitoreo de salud procesado manualmente");
    }
    
    private void procesarMonitoreoRendimiento(ACLMessage msg, String content) {
        recolectarMetricasRendimiento();
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent("Monitoreo de rendimiento ejecutado exitosamente");
        send(reply);
        
        log.info("Monitoreo de rendimiento procesado manualmente");
    }
    
    private void procesarMonitoreoRecursos(ACLMessage msg, String content) {
        monitorearRecursos();
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent("Monitoreo de recursos ejecutado exitosamente");
        send(reply);
        
        log.info("Monitoreo de recursos procesado manualmente");
    }
    
    private void procesarGeneracionAlerta(ACLMessage msg, String content) {
        try {
            Map<String, Object> request = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            String sistemaId = (String) request.get("sistemaId");
            String severidad = (String) request.get("severidad");
            String mensaje = (String) request.get("mensaje");
            
            generarAlertaMonitoreo(sistemaId, severidad, mensaje);
            
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("Alerta generada exitosamente");
            send(reply);
            
        } catch (Exception e) {
            log.error("Error procesando generación de alerta: {}", e.getMessage());
        }
    }
    
    private void procesarRecoleccionMetricas(ACLMessage msg, String content) {
        recolectarMetricasRendimiento();
        
        try {
            Map<String, Object> metricas = sistemas.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().metricas
                ));
            
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(objectMapper.writeValueAsString(metricas));
            send(reply);
            
        } catch (Exception e) {
            log.error("Error procesando recolección de métricas: {}", e.getMessage());
        }
    }
    
    private void generarReporteMonitoreo(ACLMessage msg, String content) {
        try {
            ReporteMonitoreo reporte = new ReporteMonitoreo();
            reporte.id = "REPORTE_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            reporte.fechaGeneracion = LocalDateTime.now();
            reporte.periodo = "TIEMPO_REAL";
            
            // Resumen de sistemas
            reporte.resumenSistemas = sistemas.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> Map.of(
                        "estado", e.getValue().estado,
                        "disponibilidad", e.getValue().disponibilidad,
                        "ultimaVerificacion", e.getValue().ultimaVerificacion.toString()
                    )
                ));
            
            // Alertas activas
            reporte.alertasActivas = alertasActivas.stream()
                .filter(a -> "ACTIVA".equals(a.estado))
                .collect(Collectors.toList());
            
            // Estado de SLAs
            reporte.estadoSLAs = acuerdosNivel.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> Map.of(
                        "estado", e.getValue().estado,
                        "valorActual", e.getValue().valorActual,
                        "valorObjetivo", e.getValue().valorObjetivo
                    )
                ));
            
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(objectMapper.writeValueAsString(reporte));
            send(reply);
            
            log.info("Reporte de monitoreo generado: {}", reporte.id);
            
        } catch (Exception e) {
            log.error("Error generando reporte de monitoreo: {}", e.getMessage());
        }
    }
    
    private void actualizarUmbrales(ACLMessage msg, String content) {
        // Implementación de actualización de umbrales
        log.info("Actualizando umbrales de monitoreo");
    }
    
    private void procesarPrediccionFallos(ACLMessage msg, String content) {
        ejecutarPrediccionFallos();
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent("Predicción de fallos ejecutada exitosamente");
        send(reply);
        
        log.info("Predicción de fallos procesada manualmente");
    }
    
    private void procesarSeguimientoSLA(ACLMessage msg, String content) {
        evaluarCumplimientoSLA();
        
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent("Seguimiento de SLA ejecutado exitosamente");
        send(reply);
        
        log.info("Seguimiento de SLA procesado manualmente");
    }
    
    private void actualizarDashboard(ACLMessage msg, String content) {
        // Implementación de actualización de dashboard
        log.info("Actualizando dashboard de monitoreo");
    }
    
    // Clases auxiliares
    public static class ConfiguracionMonitoreo {
        public int frecuenciaMonitoreoSalud;
        public int frecuenciaMetricas;
        public int frecuenciaRecursos;
        public int retencionHistorial;
        public String nivelLogging;
        public boolean habilitarPrediccionFallos;
        public double umbralCritico;
        public double umbralAdvertencia;
        
        @Override
        public String toString() {
            return String.format("ConfiguracionMonitoreo{salud=%ds, metricas=%ds, prediccion=%b}", 
                               frecuenciaMonitoreoSalud, frecuenciaMetricas, habilitarPrediccionFallos);
        }
    }
    
    public static class SistemaMonitoreado {
        public String id;
        public String nombre;
        public String tipo;
        public String estado;
        public double disponibilidad;
        public LocalDateTime ultimaVerificacion;
        public Map<String, Double> metricas;
    }
    
    public static class MetricaRendimiento {
        public String sistemaId;
        public LocalDateTime timestamp;
        public Map<String, Double> valores;
        public double promedio;
    }
    
    public static class UmbralMonitoreo {
        public String sistemaId;
        public String metrica;
        public double valorAdvertencia;
        public double valorCritico;
        public String accion;
    }
    
    public static class AlertaMonitoreo {
        public String id;
        public String sistemaId;
        public String severidad;
        public String mensaje;
        public LocalDateTime timestamp;
        public String estado;
        public boolean notificada;
    }
    
    public static class HistorialMetricas {
        public String sistemaId;
        public List<RegistroMetrica> registros = new ArrayList<>();
    }
    
    public static class RegistroMetrica {
        public LocalDateTime timestamp;
        public Map<String, Double> metricas;
        
        public RegistroMetrica(LocalDateTime timestamp, Map<String, Double> metricas) {
            this.timestamp = timestamp;
            this.metricas = metricas;
        }
    }
    
    public static class SLA {
        public String id;
        public String nombre;
        public String descripcion;
        public double valorObjetivo;
        public double valorActual;
        public String periodo;
        public String estado;
        public LocalDateTime fechaInicio;
        public LocalDateTime fechaFin;
    }
    
    public static class ReporteMonitoreo {
        public String id;
        public LocalDateTime fechaGeneracion;
        public String periodo;
        public Map<String, Object> resumenSistemas;
        public List<AlertaMonitoreo> alertasActivas;
        public Map<String, Object> estadoSLAs;
    }
}
