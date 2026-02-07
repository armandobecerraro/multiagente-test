package com.smagesci.agents.riskmanagement;

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
 * Agente Gestor de Riesgos - Responsable de la gestión integral de riesgos del sistema
 * Maneja identificación, evaluación, mitigación y monitoreo de riesgos operacionales, financieros y estratégicos
 */
public class AgenteGestorRiesgos extends BaseAgent {
    
    // Constantes para tipos de acción
    public static final String IDENTIFY_RISKS = "IDENTIFY_RISKS";
    public static final String ASSESS_RISK = "ASSESS_RISK";
    public static final String CREATE_MITIGATION_PLAN = "CREATE_MITIGATION_PLAN";
    public static final String MONITOR_RISKS = "MONITOR_RISKS";
    public static final String UPDATE_RISK_STATUS = "UPDATE_RISK_STATUS";
    public static final String GENERATE_RISK_REPORT = "GENERATE_RISK_REPORT";
    public static final String SIMULATE_SCENARIO = "SIMULATE_SCENARIO";
    public static final String CALCULATE_RISK_EXPOSURE = "CALCULATE_RISK_EXPOSURE";
    public static final String EXECUTE_CONTINGENCY = "EXECUTE_CONTINGENCY";
    public static final String COMPLIANCE_CHECK = "COMPLIANCE_CHECK";
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // Estructuras de datos para gestión de riesgos
    private Map<String, Riesgo> riesgos = new ConcurrentHashMap<>();
    private Map<String, PlanMitigacion> planesMitigacion = new ConcurrentHashMap<>();
    private Map<String, Indicador> indicadoresRiesgo = new ConcurrentHashMap<>();
    private Map<String, Escenario> escenarios = new ConcurrentHashMap<>();
    private List<Evento> eventosRiesgo = Collections.synchronizedList(new ArrayList<>());
    private Map<String, ControlRiesgo> controles = new ConcurrentHashMap<>();
    private Map<String, Alerta> alertasRiesgo = new ConcurrentHashMap<>();
    private MatrizRiesgo matrizRiesgo;
    
    // Configuración del gestor de riesgos
    private ConfiguracionRiesgo configuracion;
    
    @Override
    protected void setup() {
        super.setup();
        
        inicializarConfiguracionRiesgo();
        inicializarMatrizRiesgo();
        inicializarRiesgosPredeterminados();
        inicializarIndicadores();
        inicializarControles();
        
        // Comportamiento para monitoreo continuo de riesgos
        addBehaviour(new TickerBehaviour(this, 600000) { // Cada 10 minutos
            @Override
            protected void onTick() {
                monitorearRiesgos();
            }
        });
        
        // Comportamiento para evaluación periódica de riesgos
        addBehaviour(new TickerBehaviour(this, 3600000) { // Cada hora
            @Override
            protected void onTick() {
                evaluarRiesgosPeriodicamente();
            }
        });
        
        // Comportamiento para generación de alertas
        addBehaviour(new TickerBehaviour(this, 300000) { // Cada 5 minutos
            @Override
            protected void onTick() {
                procesarAlertas();
            }
        });
        
        // Comportamiento principal para procesamiento de solicitudes
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    procesarSolicitudRiesgo(msg);
                } else {
                    block();
                }
            }
        });
        
        log.info("AgenteGestorRiesgos iniciado con {} riesgos identificados", riesgos.size());
    }
    
    private void inicializarConfiguracionRiesgo() {
        configuracion = new ConfiguracionRiesgo();
        configuracion.toleranciaRiesgoAlto = 80.0;
        configuracion.toleranciaRiesgoMedio = 50.0;
        configuracion.toleranciaRiesgoBajo = 20.0;
        configuracion.frecuenciaEvaluacion = 24; // horas
        configuracion.umbralAlertas = 70.0;
        configuracion.metodologiaEvaluacion = "MATRIZ_PROBABILIDAD_IMPACTO";
        configuracion.escalaTiempo = Arrays.asList("INMEDIATO", "CORTO_PLAZO", "MEDIO_PLAZO", "LARGO_PLAZO");
        configuracion.categoriasRiesgo = Arrays.asList(
            "OPERACIONAL", "FINANCIERO", "ESTRATEGICO", "TECNOLOGICO", 
            "LEGAL", "REPUTACIONAL", "AMBIENTAL", "SEGURIDAD"
        );
        
        log.info("Configuración de riesgos inicializada - Tolerancia alto: {}", 
                   configuracion.toleranciaRiesgoAlto);
    }
    
    private void inicializarMatrizRiesgo() {
        matrizRiesgo = new MatrizRiesgo();
        matrizRiesgo.escalaProbabilidad = Arrays.asList(
            "MUY_BAJA", "BAJA", "MEDIA", "ALTA", "MUY_ALTA"
        );
        matrizRiesgo.escalaImpacto = Arrays.asList(
            "INSIGNIFICANTE", "MENOR", "MODERADO", "MAYOR", "CATASTROFICO"
        );
        
        // Inicializar matriz de valores
        matrizRiesgo.valores = new HashMap<>();
        matrizRiesgo.valores.put("MUY_BAJA_INSIGNIFICANTE", 1);
        matrizRiesgo.valores.put("MUY_BAJA_MENOR", 2);
        matrizRiesgo.valores.put("MUY_BAJA_MODERADO", 3);
        matrizRiesgo.valores.put("MUY_BAJA_MAYOR", 4);
        matrizRiesgo.valores.put("MUY_BAJA_CATASTROFICO", 5);
        
        matrizRiesgo.valores.put("BAJA_INSIGNIFICANTE", 2);
        matrizRiesgo.valores.put("BAJA_MENOR", 4);
        matrizRiesgo.valores.put("BAJA_MODERADO", 6);
        matrizRiesgo.valores.put("BAJA_MAYOR", 8);
        matrizRiesgo.valores.put("BAJA_CATASTROFICO", 10);
        
        matrizRiesgo.valores.put("MEDIA_INSIGNIFICANTE", 3);
        matrizRiesgo.valores.put("MEDIA_MENOR", 6);
        matrizRiesgo.valores.put("MEDIA_MODERADO", 9);
        matrizRiesgo.valores.put("MEDIA_MAYOR", 12);
        matrizRiesgo.valores.put("MEDIA_CATASTROFICO", 15);
        
        matrizRiesgo.valores.put("ALTA_INSIGNIFICANTE", 4);
        matrizRiesgo.valores.put("ALTA_MENOR", 8);
        matrizRiesgo.valores.put("ALTA_MODERADO", 12);
        matrizRiesgo.valores.put("ALTA_MAYOR", 16);
        matrizRiesgo.valores.put("ALTA_CATASTROFICO", 20);
        
        matrizRiesgo.valores.put("MUY_ALTA_INSIGNIFICANTE", 5);
        matrizRiesgo.valores.put("MUY_ALTA_MENOR", 10);
        matrizRiesgo.valores.put("MUY_ALTA_MODERADO", 15);
        matrizRiesgo.valores.put("MUY_ALTA_MAYOR", 20);
        matrizRiesgo.valores.put("MUY_ALTA_CATASTROFICO", 25);
        
        log.info("Matriz de riesgo inicializada con escala 1-25");
    }
    
    private void inicializarRiesgosPredeterminados() {
        // Riesgo operacional: Falla de equipos
        Riesgo riesgoEquipos = new Riesgo();
        riesgoEquipos.id = "RIESGO_001";
        riesgoEquipos.nombre = "Falla de Equipos de Producción";
        riesgoEquipos.descripcion = "Riesgo de falla en equipos críticos de producción";
        riesgoEquipos.categoria = "OPERACIONAL";
        riesgoEquipos.probabilidad = "MEDIA";
        riesgoEquipos.impacto = "MAYOR";
        riesgoEquipos.valorRiesgo = calcularValorRiesgo(riesgoEquipos.probabilidad, riesgoEquipos.impacto);
        riesgoEquipos.estado = "ACTIVO";
        riesgoEquipos.fechaIdentificacion = LocalDateTime.now();
        riesgoEquipos.propietario = "AgenteGestorDeMantenimiento";
        riesgoEquipos.causas = Arrays.asList(
            "Desgaste natural de componentes",
            "Mantenimiento inadecuado",
            "Sobrecarga de operación",
            "Condiciones ambientales adversas"
        );
        riesgoEquipos.consecuencias = Arrays.asList(
            "Parada de producción",
            "Pérdidas económicas",
            "Retrasos en entregas",
            "Costos de reparación"
        );
        riesgos.put(riesgoEquipos.id, riesgoEquipos);
        
        // Riesgo financiero: Fluctuación de precios
        Riesgo riesgoFinanciero = new Riesgo();
        riesgoFinanciero.id = "RIESGO_002";
        riesgoFinanciero.nombre = "Fluctuación de Precios de Materias Primas";
        riesgoFinanciero.descripcion = "Variabilidad en precios de materias primas críticas";
        riesgoFinanciero.categoria = "FINANCIERO";
        riesgoFinanciero.probabilidad = "ALTA";
        riesgoFinanciero.impacto = "MODERADO";
        riesgoFinanciero.valorRiesgo = calcularValorRiesgo(riesgoFinanciero.probabilidad, riesgoFinanciero.impacto);
        riesgoFinanciero.estado = "ACTIVO";
        riesgoFinanciero.fechaIdentificacion = LocalDateTime.now();
        riesgoFinanciero.propietario = "AgenteGestorFinanciero";
        riesgoFinanciero.causas = Arrays.asList(
            "Volatilidad del mercado",
            "Factores geopolíticos",
            "Cambios en la oferta y demanda",
            "Fluctuaciones monetarias"
        );
        riesgoFinanciero.consecuencias = Arrays.asList(
            "Aumento de costos de producción",
            "Reducción de márgenes",
            "Necesidad de ajustar precios",
            "Impacto en flujo de caja"
        );
        riesgos.put(riesgoFinanciero.id, riesgoFinanciero);
        
        // Riesgo estratégico: Competencia
        Riesgo riesgoCompetencia = new Riesgo();
        riesgoCompetencia.id = "RIESGO_003";
        riesgoCompetencia.nombre = "Entrada de Nuevos Competidores";
        riesgoCompetencia.descripcion = "Aparición de competidores con tecnología disruptiva";
        riesgoCompetencia.categoria = "ESTRATEGICO";
        riesgoCompetencia.probabilidad = "MEDIA";
        riesgoCompetencia.impacto = "MAYOR";
        riesgoCompetencia.valorRiesgo = calcularValorRiesgo(riesgoCompetencia.probabilidad, riesgoCompetencia.impacto);
        riesgoCompetencia.estado = "ACTIVO";
        riesgoCompetencia.fechaIdentificacion = LocalDateTime.now();
        riesgoCompetencia.propietario = "AgenteOrquestadorPrincipal";
        riesgoCompetencia.causas = Arrays.asList(
            "Avances tecnológicos",
            "Barreras de entrada reducidas",
            "Nuevos modelos de negocio",
            "Cambios regulatorios"
        );
        riesgoCompetencia.consecuencias = Arrays.asList(
            "Pérdida de cuota de mercado",
            "Presión sobre precios",
            "Necesidad de innovación acelerada",
            "Reducción de rentabilidad"
        );
        riesgos.put(riesgoCompetencia.id, riesgoCompetencia);
        
        // Riesgo tecnológico: Ciberseguridad
        Riesgo riesgoCiber = new Riesgo();
        riesgoCiber.id = "RIESGO_004";
        riesgoCiber.nombre = "Ataque Cibernético";
        riesgoCiber.descripcion = "Riesgo de ataques a la infraestructura tecnológica";
        riesgoCiber.categoria = "TECNOLOGICO";
        riesgoCiber.probabilidad = "ALTA";
        riesgoCiber.impacto = "CATASTROFICO";
        riesgoCiber.valorRiesgo = calcularValorRiesgo(riesgoCiber.probabilidad, riesgoCiber.impacto);
        riesgoCiber.estado = "ACTIVO";
        riesgoCiber.fechaIdentificacion = LocalDateTime.now();
        riesgoCiber.propietario = "AgenteGestorSeguridad";
        riesgoCiber.causas = Arrays.asList(
            "Vulnerabilidades de software",
            "Acceso no autorizado",
            "Ingeniería social",
            "Malware y ransomware"
        );
        riesgoCiber.consecuencias = Arrays.asList(
            "Pérdida de datos críticos",
            "Interrupción de operaciones",
            "Daño reputacional",
            "Costos de recuperación",
            "Multas regulatorias"
        );
        riesgos.put(riesgoCiber.id, riesgoCiber);
        
        log.info("Riesgos predeterminados inicializados: {}", riesgos.size());
    }
    
    private int calcularValorRiesgo(String probabilidad, String impacto) {
        String clave = probabilidad + "_" + impacto;
        return matrizRiesgo.valores.getOrDefault(clave, 5);
    }
    
    private void inicializarIndicadores() {
        // Indicador de disponibilidad de equipos
        Indicador indDisponibilidad = new Indicador();
        indDisponibilidad.id = "IND_DISPONIBILIDAD";
        indDisponibilidad.nombre = "Disponibilidad de Equipos";
        indDisponibilidad.descripcion = "Porcentaje de tiempo que los equipos están operativos";
        indDisponibilidad.unidad = "PORCENTAJE";
        indDisponibilidad.valorActual = 95.5;
        indDisponibilidad.umbralVerde = 98.0;
        indDisponibilidad.umbralAmarillo = 95.0;
        indDisponibilidad.umbralRojo = 90.0;
        indDisponibilidad.frecuenciaMonitoreo = "TIEMPO_REAL";
        indDisponibilidad.riesgoAsociado = "RIESGO_001";
        indicadoresRiesgo.put(indDisponibilidad.id, indDisponibilidad);
        
        // Indicador de volatilidad de precios
        Indicador indVolatilidad = new Indicador();
        indVolatilidad.id = "IND_VOLATILIDAD_PRECIOS";
        indVolatilidad.nombre = "Volatilidad de Precios";
        indVolatilidad.descripcion = "Variación porcentual en precios de materias primas";
        indVolatilidad.unidad = "PORCENTAJE";
        indVolatilidad.valorActual = 12.3;
        indVolatilidad.umbralVerde = 5.0;
        indVolatilidad.umbralAmarillo = 10.0;
        indVolatilidad.umbralRojo = 15.0;
        indVolatilidad.frecuenciaMonitoreo = "DIARIO";
        indVolatilidad.riesgoAsociado = "RIESGO_002";
        indicadoresRiesgo.put(indVolatilidad.id, indVolatilidad);
        
        // Indicador de cuota de mercado
        Indicador indCuota = new Indicador();
        indCuota.id = "IND_CUOTA_MERCADO";
        indCuota.nombre = "Cuota de Mercado";
        indCuota.descripcion = "Porcentaje de participación en el mercado";
        indCuota.unidad = "PORCENTAJE";
        indCuota.valorActual = 15.8;
        indCuota.umbralVerde = 18.0;
        indCuota.umbralAmarillo = 15.0;
        indCuota.umbralRojo = 12.0;
        indCuota.frecuenciaMonitoreo = "MENSUAL";
        indCuota.riesgoAsociado = "RIESGO_003";
        indicadoresRiesgo.put(indCuota.id, indCuota);
        
        // Indicador de incidentes de seguridad
        Indicador indSeguridad = new Indicador();
        indSeguridad.id = "IND_INCIDENTES_SEGURIDAD";
        indSeguridad.nombre = "Incidentes de Seguridad";
        indSeguridad.descripcion = "Número de incidentes de seguridad por mes";
        indSeguridad.unidad = "NUMERO";
        indSeguridad.valorActual = 2.0;
        indSeguridad.umbralVerde = 0.0;
        indSeguridad.umbralAmarillo = 2.0;
        indSeguridad.umbralRojo = 5.0;
        indSeguridad.frecuenciaMonitoreo = "TIEMPO_REAL";
        indSeguridad.riesgoAsociado = "RIESGO_004";
        indicadoresRiesgo.put(indSeguridad.id, indSeguridad);
        
        log.info("Indicadores de riesgo inicializados: {}", indicadoresRiesgo.size());
    }
    
    private void inicializarControles() {
        // Control para falla de equipos
        ControlRiesgo controlMantenimiento = new ControlRiesgo();
        controlMantenimiento.id = "CTRL_001";
        controlMantenimiento.nombre = "Mantenimiento Preventivo";
        controlMantenimiento.descripcion = "Programa de mantenimiento preventivo de equipos";
        controlMantenimiento.tipo = "PREVENTIVO";
        controlMantenimiento.riesgoId = "RIESGO_001";
        controlMantenimiento.efectividad = 85.0;
        controlMantenimiento.estado = "ACTIVO";
        controlMantenimiento.frecuencia = "SEMANAL";
        controlMantenimiento.responsable = "AgenteGestorDeMantenimiento";
        controlMantenimiento.ultimaEjecucion = LocalDateTime.now().minusDays(3);
        controles.put(controlMantenimiento.id, controlMantenimiento);
        
        // Control para riesgo financiero
        ControlRiesgo controlCobertura = new ControlRiesgo();
        controlCobertura.id = "CTRL_002";
        controlCobertura.nombre = "Cobertura de Precios";
        controlCobertura.descripcion = "Contratos de cobertura para materias primas";
        controlCobertura.tipo = "MITIGACION";
        controlCobertura.riesgoId = "RIESGO_002";
        controlCobertura.efectividad = 70.0;
        controlCobertura.estado = "ACTIVO";
        controlCobertura.frecuencia = "TRIMESTRAL";
        controlCobertura.responsable = "AgenteGestorFinanciero";
        controlCobertura.ultimaEjecucion = LocalDateTime.now().minusDays(45);
        controles.put(controlCobertura.id, controlCobertura);
        
        // Control para riesgo competitivo
        ControlRiesgo controlMonitoreo = new ControlRiesgo();
        controlMonitoreo.id = "CTRL_003";
        controlMonitoreo.nombre = "Monitoreo de Competencia";
        controlMonitoreo.descripcion = "Seguimiento continuo del entorno competitivo";
        controlMonitoreo.tipo = "DETECCION";
        controlMonitoreo.riesgoId = "RIESGO_003";
        controlMonitoreo.efectividad = 60.0;
        controlMonitoreo.estado = "ACTIVO";
        controlMonitoreo.frecuencia = "MENSUAL";
        controlMonitoreo.responsable = "AgenteAnalisisDatos";
        controlMonitoreo.ultimaEjecucion = LocalDateTime.now().minusDays(15);
        controles.put(controlMonitoreo.id, controlMonitoreo);
        
        // Control para riesgo cibernético
        ControlRiesgo controlSeguridad = new ControlRiesgo();
        controlSeguridad.id = "CTRL_004";
        controlSeguridad.nombre = "Monitoreo de Seguridad";
        controlSeguridad.descripcion = "Monitoreo continuo de amenazas cibernéticas";
        controlSeguridad.tipo = "PREVENTIVO";
        controlSeguridad.riesgoId = "RIESGO_004";
        controlSeguridad.efectividad = 90.0;
        controlSeguridad.estado = "ACTIVO";
        controlSeguridad.frecuencia = "CONTINUO";
        controlSeguridad.responsable = "AgenteGestorSeguridad";
        controlSeguridad.ultimaEjecucion = LocalDateTime.now().minusMinutes(10);
        controles.put(controlSeguridad.id, controlSeguridad);
        
        log.info("Controles de riesgo inicializados: {}", controles.size());
    }
    
    private void procesarSolicitudRiesgo(ACLMessage msg) {
        try {
            String action = msg.getOntology();
            String content = msg.getContent();
            
            switch (action) {
                case IDENTIFY_RISKS:
                    identificarRiesgos(msg, content);
                    break;
                case ASSESS_RISK:
                    evaluarRiesgo(msg, content);
                    break;
                case CREATE_MITIGATION_PLAN:
                    crearPlanMitigacion(msg, content);
                    break;
                case MONITOR_RISKS:
                    monitorearRiesgosEspecificos(msg, content);
                    break;
                case UPDATE_RISK_STATUS:
                    actualizarEstadoRiesgo(msg, content);
                    break;
                case GENERATE_RISK_REPORT:
                    generarReporteRiesgo(msg, content);
                    break;
                case SIMULATE_SCENARIO:
                    simularEscenario(msg, content);
                    break;
                case CALCULATE_RISK_EXPOSURE:
                    calcularExposicionRiesgo(msg, content);
                    break;
                case EXECUTE_CONTINGENCY:
                    ejecutarPlanContingencia(msg, content);
                    break;
                case COMPLIANCE_CHECK:
                    verificarCumplimiento(msg, content);
                    break;
                default:
                    log.warn("Acción de riesgo no reconocida: {}", action);
            }
        } catch (Exception e) {
            log.error("Error procesando solicitud de riesgo: {}", e.getMessage());
        }
    }
    
    private void identificarRiesgos(ACLMessage msg, String content) {
        try {
            Map<String, Object> request = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            String contexto = (String) request.get("contexto");
            String categoria = (String) request.getOrDefault("categoria", "OPERACIONAL");
            
            // Simular identificación de nuevos riesgos basada en contexto
            List<RiesgoIdentificado> riesgosIdentificados = analizarContextoParaRiesgos(contexto, categoria);
            
            // Crear riesgos identificados
            for (RiesgoIdentificado riesgoId : riesgosIdentificados) {
                Riesgo nuevoRiesgo = new Riesgo();
                nuevoRiesgo.id = UUID.randomUUID().toString();
                nuevoRiesgo.nombre = riesgoId.nombre;
                nuevoRiesgo.descripcion = riesgoId.descripcion;
                nuevoRiesgo.categoria = categoria;
                nuevoRiesgo.probabilidad = riesgoId.probabilidadEstimada;
                nuevoRiesgo.impacto = riesgoId.impactoEstimado;
                nuevoRiesgo.valorRiesgo = calcularValorRiesgo(nuevoRiesgo.probabilidad, nuevoRiesgo.impacto);
                nuevoRiesgo.estado = "IDENTIFICADO";
                nuevoRiesgo.fechaIdentificacion = LocalDateTime.now();
                nuevoRiesgo.propietario = msg.getSender().getLocalName();
                nuevoRiesgo.causas = riesgoId.causasPotenciales;
                nuevoRiesgo.consecuencias = riesgoId.consecuenciasPotenciales;
                
                riesgos.put(nuevoRiesgo.id, nuevoRiesgo);
            }
            
            // Responder con riesgos identificados
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(objectMapper.writeValueAsString(Map.of(
                "riesgosIdentificados", riesgosIdentificados.size(),
                "categoria", categoria,
                "contexto", contexto,
                "detalles", riesgosIdentificados
            )));
            send(reply);
            
            log.info("Identificados {} nuevos riesgos en categoría: {}", 
                       riesgosIdentificados.size(), categoria);
            
        } catch (Exception e) {
            log.error("Error identificando riesgos: {}", e.getMessage());
        }
    }
    
    private List<RiesgoIdentificado> analizarContextoParaRiesgos(String contexto, String categoria) {
        List<RiesgoIdentificado> riesgosIdentificados = new ArrayList<>();
        
        // Análisis basado en palabras clave del contexto
        String contextoLower = contexto.toLowerCase();
        
        if (contextoLower.contains("produccion") || contextoLower.contains("manufactura")) {
            RiesgoIdentificado riesgo = new RiesgoIdentificado();
            riesgo.nombre = "Interrupción de Línea de Producción";
            riesgo.descripcion = "Riesgo de parada no planificada en línea de producción";
            riesgo.probabilidadEstimada = "MEDIA";
            riesgo.impactoEstimado = "MAYOR";
            riesgo.causasPotenciales = Arrays.asList("Falla de equipos", "Falta de materiales", "Problemas de calidad");
            riesgo.consecuenciasPotenciales = Arrays.asList("Pérdida de producción", "Incumplimiento de pedidos");
            riesgosIdentificados.add(riesgo);
        }
        
        if (contextoLower.contains("proveedor") || contextoLower.contains("suministro")) {
            RiesgoIdentificado riesgo = new RiesgoIdentificado();
            riesgo.nombre = "Interrupción de Suministro";
            riesgo.descripcion = "Riesgo de falta de materias primas críticas";
            riesgo.probabilidadEstimada = "MEDIA";
            riesgo.impactoEstimado = "MAYOR";
            riesgo.causasPotenciales = Arrays.asList("Falla de proveedor", "Problemas logísticos", "Eventos climáticos");
            riesgo.consecuenciasPotenciales = Arrays.asList("Parada de producción", "Aumento de costos");
            riesgosIdentificados.add(riesgo);
        }
        
        if (contextoLower.contains("mercado") || contextoLower.contains("demanda")) {
            RiesgoIdentificado riesgo = new RiesgoIdentificado();
            riesgo.nombre = "Cambio en Demanda del Mercado";
            riesgo.descripcion = "Riesgo de variación significativa en la demanda";
            riesgo.probabilidadEstimada = "ALTA";
            riesgo.impactoEstimado = "MODERADO";
            riesgo.causasPotenciales = Arrays.asList("Cambios económicos", "Nuevas tendencias", "Competencia");
            riesgo.consecuenciasPotenciales = Arrays.asList("Exceso de inventario", "Pérdida de ventas");
            riesgosIdentificados.add(riesgo);
        }
        
        if (contextoLower.contains("tecnologia") || contextoLower.contains("sistema")) {
            RiesgoIdentificado riesgo = new RiesgoIdentificado();
            riesgo.nombre = "Falla Tecnológica";
            riesgo.descripcion = "Riesgo de falla en sistemas críticos de tecnología";
            riesgo.probabilidadEstimada = "BAJA";
            riesgo.impactoEstimado = "CATASTROFICO";
            riesgo.causasPotenciales = Arrays.asList("Falla de hardware", "Error de software", "Ataque cibernético");
            riesgo.consecuenciasPotenciales = Arrays.asList("Pérdida de datos", "Interrupción operativa");
            riesgosIdentificados.add(riesgo);
        }
        
        return riesgosIdentificados;
    }
    
    private void evaluarRiesgo(ACLMessage msg, String content) {
        try {
            Map<String, Object> request = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            String riesgoId = (String) request.get("riesgoId");
            
            Riesgo riesgo = riesgos.get(riesgoId);
            if (riesgo == null) {
                log.warn("Riesgo no encontrado: {}", riesgoId);
                return;
            }
            
            // Realizar evaluación detallada
            EvaluacionRiesgo evaluacion = realizarEvaluacionDetallada(riesgo);
            riesgo.ultimaEvaluacion = evaluacion;
            
            // Actualizar estado basado en evaluación
            actualizarEstadoRiesgoBasadoEnEvaluacion(riesgo, evaluacion);
            
            // Responder con resultado de evaluación
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(objectMapper.writeValueAsString(Map.of(
                "riesgoId", riesgoId,
                "evaluacion", evaluacion,
                "nuevoEstado", riesgo.estado,
                "recomendaciones", evaluacion.recomendaciones
            )));
            send(reply);
            
            log.info("Riesgo evaluado: {} - Puntuación: {:.2f}", 
                       riesgo.nombre, evaluacion.puntuacionTotal);
            
        } catch (Exception e) {
            log.error("Error evaluando riesgo: {}", e.getMessage());
        }
    }
    
    private EvaluacionRiesgo realizarEvaluacionDetallada(Riesgo riesgo) {
        EvaluacionRiesgo evaluacion = new EvaluacionRiesgo();
        evaluacion.fechaEvaluacion = LocalDateTime.now();
        evaluacion.metodologia = configuracion.metodologiaEvaluacion;
        evaluacion.recomendaciones = new ArrayList<>();
        
        // Calcular puntuación base de la matriz
        int valorMatriz = riesgo.valorRiesgo;
        evaluacion.puntuacionBase = valorMatriz * 4.0; // Escala a 100
        
        // Aplicar factores de ajuste
        double factorTempo = calcularFactorTemporal(riesgo);
        double factorControl = calcularFactorControl(riesgo);
        double factorTendencia = calcularFactorTendencia(riesgo);
        
        evaluacion.factorTemporal = factorTempo;
        evaluacion.factorControl = factorControl;
        evaluacion.factorTendencia = factorTendencia;
        
        // Calcular puntuación total
        evaluacion.puntuacionTotal = evaluacion.puntuacionBase * factorTempo * factorControl * factorTendencia;
        
        // Clasificar riesgo
        if (evaluacion.puntuacionTotal >= configuracion.toleranciaRiesgoAlto) {
            evaluacion.clasificacion = "ALTO";
            evaluacion.recomendaciones.add("Requiere acción inmediata");
            evaluacion.recomendaciones.add("Asignar recursos prioritarios");
        } else if (evaluacion.puntuacionTotal >= configuracion.toleranciaRiesgoMedio) {
            evaluacion.clasificacion = "MEDIO";
            evaluacion.recomendaciones.add("Monitorear de cerca");
            evaluacion.recomendaciones.add("Desarrollar plan de mitigación");
        } else {
            evaluacion.clasificacion = "BAJO";
            evaluacion.recomendaciones.add("Monitoreo rutinario");
        }
        
        // Calcular probabilidad residual (después de controles)
        evaluacion.probabilidadResidual = calcularProbabilidadResidual(riesgo, factorControl);
        evaluacion.impactoResidual = calcularImpactoResidual(riesgo, factorControl);
        
        return evaluacion;
    }
    
    private double calcularFactorTemporal(Riesgo riesgo) {
        // Factor basado en cuándo podría materializarse el riesgo
        LocalDateTime ahora = LocalDateTime.now();
        long diasDesdeIdentificacion = java.time.temporal.ChronoUnit.DAYS.between(
            riesgo.fechaIdentificacion, ahora);
        
        // Riesgos más antiguos sin materializar tienen factor menor
        if (diasDesdeIdentificacion > 365) return 0.8;
        if (diasDesdeIdentificacion > 180) return 0.9;
        if (diasDesdeIdentificacion > 90) return 0.95;
        return 1.0;
    }
    
    private double calcularFactorControl(Riesgo riesgo) {
        // Factor basado en efectividad de controles existentes
        List<ControlRiesgo> controlesRiesgo = controles.values().stream()
            .filter(c -> c.riesgoId.equals(riesgo.id) && "ACTIVO".equals(c.estado))
            .collect(Collectors.toList());
        
        if (controlesRiesgo.isEmpty()) return 1.0;
        
        double efectividadPromedio = controlesRiesgo.stream()
            .mapToDouble(c -> c.efectividad)
            .average()
            .orElse(0.0);
        
        // Convertir efectividad a factor de reducción
        return 1.0 - (efectividadPromedio / 100.0 * 0.5); // Máximo 50% de reducción
    }
    
    private double calcularFactorTendencia(Riesgo riesgo) {
        // Factor basado en tendencia de indicadores asociados
        Indicador indicador = indicadoresRiesgo.values().stream()
            .filter(i -> i.riesgoAsociado.equals(riesgo.id))
            .findFirst()
            .orElse(null);
        
        if (indicador == null) return 1.0;
        
        // Simular tendencia basada en valor actual vs umbrales
        if (indicador.valorActual <= indicador.umbralVerde) return 0.8;
        if (indicador.valorActual <= indicador.umbralAmarillo) return 1.0;
        return 1.2;
    }
    
    private String calcularProbabilidadResidual(Riesgo riesgo, double factorControl) {
        // Calcular probabilidad después de aplicar controles
        List<String> escalaProbabilidad = matrizRiesgo.escalaProbabilidad;
        int indiceActual = escalaProbabilidad.indexOf(riesgo.probabilidad);
        
        // Reducir probabilidad según efectividad de controles
        int nuevoIndice = Math.max(0, indiceActual - (int) ((1.0 - factorControl) * 2));
        return escalaProbabilidad.get(nuevoIndice);
    }
    
    private String calcularImpactoResidual(Riesgo riesgo, double factorControl) {
        // El impacto se reduce menos que la probabilidad
        List<String> escalaImpacto = matrizRiesgo.escalaImpacto;
        int indiceActual = escalaImpacto.indexOf(riesgo.impacto);
        
        int nuevoIndice = Math.max(0, indiceActual - (int) ((1.0 - factorControl) * 1));
        return escalaImpacto.get(nuevoIndice);
    }
    
    private void actualizarEstadoRiesgoBasadoEnEvaluacion(Riesgo riesgo, EvaluacionRiesgo evaluacion) {
        switch (evaluacion.clasificacion) {
            case "ALTO":
                riesgo.estado = "CRITICO";
                break;
            case "MEDIO":
                riesgo.estado = "ATENCION";
                break;
            case "BAJO":
                riesgo.estado = "MONITOREADO";
                break;
        }
        
        riesgo.fechaUltimaEvaluacion = LocalDateTime.now();
    }
    
    private void monitorearRiesgos() {
        try {
            // Actualizar indicadores de riesgo
            actualizarIndicadores();
            
            // Evaluar alertas
            generarAlertas();
            
            // Revisar efectividad de controles
            revisarControles();
            
            // Actualizar exposición general
            calcularExposicionGeneral();
            
            log.debug("Monitoreo de riesgos completado");
            
        } catch (Exception e) {
            log.error("Error en monitoreo de riesgos: {}", e.getMessage());
        }
    }
    
    private void actualizarIndicadores() {
        for (Indicador indicador : indicadoresRiesgo.values()) {
            // Simular actualización de valores
            double variacion = (Math.random() - 0.5) * 0.1; // ±5% de variación
            indicador.valorActual = Math.max(0, indicador.valorActual * (1 + variacion));
            indicador.ultimaActualizacion = LocalDateTime.now();
            
            // Evaluar umbrales
            evaluarUmbralesIndicador(indicador);
        }
    }
    
    private void evaluarUmbralesIndicador(Indicador indicador) {
        String estadoAnterior = indicador.estado;
        
        if (indicador.valorActual <= indicador.umbralVerde) {
            indicador.estado = "VERDE";
        } else if (indicador.valorActual <= indicador.umbralAmarillo) {
            indicador.estado = "AMARILLO";
        } else {
            indicador.estado = "ROJO";
        }
        
        // Generar alerta si cambió a estado peor
        if (!estadoAnterior.equals(indicador.estado) && 
            ("ROJO".equals(indicador.estado) || "AMARILLO".equals(indicador.estado))) {
            generarAlertaIndicador(indicador, estadoAnterior);
        }
    }
    
    private void generarAlertaIndicador(Indicador indicador, String estadoAnterior) {
        Alerta alerta = new Alerta();
        alerta.id = UUID.randomUUID().toString();
        alerta.tipo = "UMBRAL_SUPERADO";
        alerta.severidad = "ROJO".equals(indicador.estado) ? "ALTA" : "MEDIA";
        alerta.titulo = "Umbral superado en " + indicador.nombre;
        alerta.descripcion = String.format("El indicador %s cambió de %s a %s (valor: %.2f)", 
                                         indicador.nombre, estadoAnterior, indicador.estado, indicador.valorActual);
        alerta.fechaGeneracion = LocalDateTime.now();
        alerta.estado = "ACTIVA";
        alerta.riesgoAsociado = indicador.riesgoAsociado;
        
        alertasRiesgo.put(alerta.id, alerta);
        
        log.warn("ALERTA GENERADA: {} - {}", alerta.titulo, alerta.descripcion);
    }
    
    private void generarAlertas() {
        // Evaluar riesgos que requieren atención inmediata
        for (Riesgo riesgo : riesgos.values()) {
            if ("CRITICO".equals(riesgo.estado) && riesgo.ultimaEvaluacion != null) {
                if (riesgo.ultimaEvaluacion.puntuacionTotal >= configuracion.umbralAlertas) {
                    generarAlertaRiesgoCritico(riesgo);
                }
            }
        }
    }
    
    private void generarAlertaRiesgoCritico(Riesgo riesgo) {
        // Verificar si ya existe una alerta activa para este riesgo
        boolean alertaExistente = alertasRiesgo.values().stream()
            .anyMatch(a -> a.riesgoAsociado.equals(riesgo.id) && "ACTIVA".equals(a.estado));
        
        if (!alertaExistente) {
            Alerta alerta = new Alerta();
            alerta.id = UUID.randomUUID().toString();
            alerta.tipo = "RIESGO_CRITICO";
            alerta.severidad = "CRITICA";
            alerta.titulo = "Riesgo Crítico Detectado";
            alerta.descripcion = String.format("El riesgo %s requiere atención inmediata (puntuación: %.2f)", 
                                             riesgo.nombre, riesgo.ultimaEvaluacion.puntuacionTotal);
            alerta.fechaGeneracion = LocalDateTime.now();
            alerta.estado = "ACTIVA";
            alerta.riesgoAsociado = riesgo.id;
            alerta.accionesRecomendadas = riesgo.ultimaEvaluacion.recomendaciones;
            
            alertasRiesgo.put(alerta.id, alerta);
            
            log.error("ALERTA CRÍTICA: {} - {}", alerta.titulo, alerta.descripcion);
        }
    }
    
    private void revisarControles() {
        LocalDateTime ahora = LocalDateTime.now();
        
        for (ControlRiesgo control : controles.values()) {
            // Verificar si el control necesita ejecución
            if (necesitaEjecucion(control, ahora)) {
                log.info("Control requiere ejecución: {} - Último: {}", 
                           control.nombre, control.ultimaEjecucion);
                
                // En implementación real, aquí se ejecutaría el control
                // Por ahora, simulamos la ejecución
                simularEjecucionControl(control);
            }
        }
    }
    
    private boolean necesitaEjecucion(ControlRiesgo control, LocalDateTime ahora) {
        if (control.ultimaEjecucion == null) return true;
        
        return switch (control.frecuencia) {
            case "CONTINUO" -> ahora.isAfter(control.ultimaEjecucion.plusMinutes(30));
            case "DIARIO" -> ahora.isAfter(control.ultimaEjecucion.plusDays(1));
            case "SEMANAL" -> ahora.isAfter(control.ultimaEjecucion.plusWeeks(1));
            case "MENSUAL" -> ahora.isAfter(control.ultimaEjecucion.plusMonths(1));
            case "TRIMESTRAL" -> ahora.isAfter(control.ultimaEjecucion.plusMonths(3));
            default -> false;
        };
    }
    
    private void simularEjecucionControl(ControlRiesgo control) {
        // Simular ejecución del control
        boolean exitoso = Math.random() > 0.1; // 90% de éxito
        
        control.ultimaEjecucion = LocalDateTime.now();
        control.ultimoResultado = exitoso ? "EXITOSO" : "FALLIDO";
        
        if (!exitoso) {
            log.warn("Control falló en ejecución: {}", control.nombre);
            // Generar alerta de falla de control
            generarAlertaFallaControl(control);
        }
    }
    
    private void generarAlertaFallaControl(ControlRiesgo control) {
        Alerta alerta = new Alerta();
        alerta.id = UUID.randomUUID().toString();
        alerta.tipo = "FALLA_CONTROL";
        alerta.severidad = "ALTA";
        alerta.titulo = "Falla en Control de Riesgo";
        alerta.descripcion = String.format("El control %s ha fallado en su ejecución", control.nombre);
        alerta.fechaGeneracion = LocalDateTime.now();
        alerta.estado = "ACTIVA";
        alerta.riesgoAsociado = control.riesgoId;
        
        alertasRiesgo.put(alerta.id, alerta);
    }
    
    private void calcularExposicionGeneral() {
        // Calcular exposición total de la organización
        double exposicionTotal = 0.0;
        int riesgosActivos = 0;
        
        for (Riesgo riesgo : riesgos.values()) {
            if ("ACTIVO".equals(riesgo.estado) || "CRITICO".equals(riesgo.estado) || "ATENCION".equals(riesgo.estado)) {
                if (riesgo.ultimaEvaluacion != null) {
                    exposicionTotal += riesgo.ultimaEvaluacion.puntuacionTotal;
                    riesgosActivos++;
                }
            }
        }
        
        if (riesgosActivos > 0) {
            double exposicionPromedio = exposicionTotal / riesgosActivos;
            log.info("Exposición promedio de riesgo: {:.2f} (basada en {} riesgos activos)", 
                       exposicionPromedio, riesgosActivos);
        }
    }
    
    private void evaluarRiesgosPeriodicamente() {
        try {
            // Evaluar riesgos que no han sido evaluados recientemente
            LocalDateTime umbralEvaluacion = LocalDateTime.now().minusHours(configuracion.frecuenciaEvaluacion);
            
            for (Riesgo riesgo : riesgos.values()) {
                if (riesgo.fechaUltimaEvaluacion == null || 
                    riesgo.fechaUltimaEvaluacion.isBefore(umbralEvaluacion)) {
                    
                    EvaluacionRiesgo evaluacion = realizarEvaluacionDetallada(riesgo);
                    riesgo.ultimaEvaluacion = evaluacion;
                    actualizarEstadoRiesgoBasadoEnEvaluacion(riesgo, evaluacion);
                    
                    log.debug("Evaluación periódica completada para riesgo: {}", riesgo.nombre);
                }
            }
            
        } catch (Exception e) {
            log.error("Error en evaluación periódica de riesgos: {}", e.getMessage());
        }
    }
    
    private void procesarAlertas() {
        try {
            // Procesar alertas pendientes
            for (Alerta alerta : alertasRiesgo.values()) {
                if ("ACTIVA".equals(alerta.estado)) {
                    procesarAlertaActiva(alerta);
                }
            }
            
            // Cerrar alertas antiguas
            cerrarAlertasAntiguas();
            
        } catch (Exception e) {
            log.error("Error procesando alertas: {}", e.getMessage());
        }
    }
    
    private void procesarAlertaActiva(Alerta alerta) {
        // Determinar acciones basadas en severidad
        switch (alerta.severidad) {
            case "CRITICA":
                // Notificar inmediatamente a todos los responsables
                notificarAlertaCritica(alerta);
                break;
            case "ALTA":
                // Notificar a responsables específicos
                notificarAlertaAlta(alerta);
                break;
            case "MEDIA":
                // Registrar para revisión
                registrarAlertaMedia(alerta);
                break;
        }
    }
    
    private void notificarAlertaCritica(Alerta alerta) {
        log.error("NOTIFICACIÓN CRÍTICA: {} - {}", alerta.titulo, alerta.descripcion);
        // En implementación real, enviar notificaciones push, emails, SMS, etc.
    }
    
    private void notificarAlertaAlta(Alerta alerta) {
        log.warn("NOTIFICACIÓN ALTA: {} - {}", alerta.titulo, alerta.descripcion);
        // En implementación real, enviar notificaciones a responsables
    }
    
    private void registrarAlertaMedia(Alerta alerta) {
        log.info("ALERTA MEDIA REGISTRADA: {} - {}", alerta.titulo, alerta.descripcion);
        // En implementación real, agregar a cola de revisión
    }
    
    private void cerrarAlertasAntiguas() {
        LocalDateTime umbralCierre = LocalDateTime.now().minusDays(7);
        
        for (Alerta alerta : alertasRiesgo.values()) {
            if ("ACTIVA".equals(alerta.estado) && alerta.fechaGeneracion.isBefore(umbralCierre)) {
                alerta.estado = "CERRADA";
                alerta.fechaCierre = LocalDateTime.now();
                alerta.motivoCierre = "Cierre automático por antigüedad";
            }
        }
    }
    
    // Clases internas para modelado de datos
    
    public static class ConfiguracionRiesgo {
        public double toleranciaRiesgoAlto;
        public double toleranciaRiesgoMedio;
        public double toleranciaRiesgoBajo;
        public int frecuenciaEvaluacion;
        public double umbralAlertas;
        public String metodologiaEvaluacion;
        public List<String> escalaTiempo;
        public List<String> categoriasRiesgo;
    }
    
    public static class Riesgo {
        public String id;
        public String nombre;
        public String descripcion;
        public String categoria;
        public String probabilidad;
        public String impacto;
        public int valorRiesgo;
        public String estado;
        public LocalDateTime fechaIdentificacion;
        public LocalDateTime fechaUltimaEvaluacion;
        public String propietario;
        public List<String> causas;
        public List<String> consecuencias;
        public EvaluacionRiesgo ultimaEvaluacion;
    }
    
    public static class EvaluacionRiesgo {
        public LocalDateTime fechaEvaluacion;
        public String metodologia;
        public double puntuacionBase;
        public double factorTemporal;
        public double factorControl;
        public double factorTendencia;
        public double puntuacionTotal;
        public String clasificacion;
        public String probabilidadResidual;
        public String impactoResidual;
        public List<String> recomendaciones;
    }
    
    public static class PlanMitigacion {
        public String id;
        public String riesgoId;
        public String nombre;
        public String descripcion;
        public List<String> acciones;
        public String responsable;
        public LocalDateTime fechaCreacion;
        public LocalDateTime fechaImplementacion;
        public String estado;
        public double efectividadEsperada;
        public double costoEstimado;
    }
    
    public static class Indicador {
        public String id;
        public String nombre;
        public String descripcion;
        public String unidad;
        public double valorActual;
        public double umbralVerde;
        public double umbralAmarillo;
        public double umbralRojo;
        public String estado;
        public String frecuenciaMonitoreo;
        public String riesgoAsociado;
        public LocalDateTime ultimaActualizacion;
    }
    
    public static class Escenario {
        public String id;
        public String nombre;
        public String descripcion;
        public Map<String, Object> parametros;
        public List<String> riesgosAfectados;
        public double probabilidadOcurrencia;
        public LocalDateTime fechaCreacion;
    }
    
    public static class Evento {
        public String id;
        public String tipo;
        public String descripcion;
        public LocalDateTime fechaOcurrencia;
        public String severidad;
        public String riesgoAsociado;
        public Map<String, Object> detalles;
    }
    
    public static class ControlRiesgo {
        public String id;
        public String nombre;
        public String descripcion;
        public String tipo;
        public String riesgoId;
        public double efectividad;
        public String estado;
        public String frecuencia;
        public String responsable;
        public LocalDateTime ultimaEjecucion;
        public String ultimoResultado;
    }
    
    public static class Alerta {
        public String id;
        public String tipo;
        public String severidad;
        public String titulo;
        public String descripcion;
        public LocalDateTime fechaGeneracion;
        public LocalDateTime fechaCierre;
        public String estado;
        public String riesgoAsociado;
        public List<String> accionesRecomendadas;
        public String motivoCierre;
    }
    
    public static class MatrizRiesgo {
        public List<String> escalaProbabilidad;
        public List<String> escalaImpacto;
        public Map<String, Integer> valores;
    }
    
    public static class RiesgoIdentificado {
        public String nombre;
        public String descripcion;
        public String probabilidadEstimada;
        public String impactoEstimado;
        public List<String> causasPotenciales;
        public List<String> consecuenciasPotenciales;
    }
    
    // Métodos auxiliares pendientes de implementación
    
    private void crearPlanMitigacion(ACLMessage msg, String content) {
        log.info("Creando plan de mitigación");
    }
    
    private void monitorearRiesgosEspecificos(ACLMessage msg, String content) {
        log.info("Monitoreando riesgos específicos");
    }
    
    private void actualizarEstadoRiesgo(ACLMessage msg, String content) {
        log.info("Actualizando estado de riesgo");
    }
    
    private void generarReporteRiesgo(ACLMessage msg, String content) {
        log.info("Generando reporte de riesgo");
    }
    
    private void simularEscenario(ACLMessage msg, String content) {
        log.info("Simulando escenario");
    }
    
    private void calcularExposicionRiesgo(ACLMessage msg, String content) {
        log.info("Calculando exposición de riesgo");
    }
    
    private void ejecutarPlanContingencia(ACLMessage msg, String content) {
        log.info("Ejecutando plan de contingencia");
    }
    
    private void verificarCumplimiento(ACLMessage msg, String content) {
        log.info("Verificando cumplimiento");
    }
}
