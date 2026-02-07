package com.smagesci.agents.planning;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agente Planificador Estratégico - Responsable de la planificación integral del sistema
 * Maneja planificación estratégica, operativa, de recursos, proyectos y presupuestos
 */
public class AgentePlanificadorEstrategico extends BaseAgent {
    
    // Constantes para tipos de acción
    public static final String CREATE_STRATEGIC_PLAN = "CREATE_STRATEGIC_PLAN";
    public static final String CREATE_OPERATIONAL_PLAN = "CREATE_OPERATIONAL_PLAN";
    public static final String PLAN_RESOURCES = "PLAN_RESOURCES";
    public static final String CREATE_PROJECT_PLAN = "CREATE_PROJECT_PLAN";
    public static final String OPTIMIZE_SCHEDULE = "OPTIMIZE_SCHEDULE";
    public static final String FORECAST_DEMAND = "FORECAST_DEMAND";
    public static final String PLAN_CAPACITY = "PLAN_CAPACITY";
    public static final String GENERATE_PLANNING_REPORT = "GENERATE_PLANNING_REPORT";
    public static final String UPDATE_PLAN = "UPDATE_PLAN";
    public static final String EVALUATE_SCENARIOS = "EVALUATE_SCENARIOS";
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // Estructuras de datos para gestión de planificación
    private Map<String, PlanEstrategico> planesEstrategicos = new ConcurrentHashMap<>();
    private Map<String, PlanOperativo> planesOperativos = new ConcurrentHashMap<>();
    private Map<String, PlanRecursos> planesRecursos = new ConcurrentHashMap<>();
    private Map<String, ProyectoPlanificado> proyectos = new ConcurrentHashMap<>();
    private Map<String, PrevisionDemanda> previsionesDemanda = new ConcurrentHashMap<>();
    private Map<String, PlanCapacidad> planesCapacidad = new ConcurrentHashMap<>();
    private Map<String, Escenario> escenarios = new ConcurrentHashMap<>();
    private List<Hito> hitos = Collections.synchronizedList(new ArrayList<>());
    private Map<String, MetricaPlanificacion> metricas = new ConcurrentHashMap<>();
    
    // Configuración del planificador
    private ConfiguracionPlanificacion configuracion;
    
    @Override
    protected void setup() {
        super.setup();
        
        inicializarConfiguracionPlanificacion();
        inicializarEscenariosBase();
        inicializarMetricas();
        crearPlanesIniciales();
        
        // Comportamiento para revisión periódica de planes
        addBehaviour(new TickerBehaviour(this, 3600000) { // Cada hora
            @Override
            protected void onTick() {
                revisarProgresoPlanificacion();
            }
        });
        
        // Comportamiento para actualización de previsiones
        addBehaviour(new TickerBehaviour(this, 86400000) { // Cada día
            @Override
            protected void onTick() {
                actualizarPrevisionesDemanda();
            }
        });
        
        // Comportamiento para optimización de recursos
        addBehaviour(new TickerBehaviour(this, 21600000) { // Cada 6 horas
            @Override
            protected void onTick() {
                optimizarAsignacionRecursos();
            }
        });
        
        // Comportamiento principal para procesamiento de solicitudes
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    procesarSolicitudPlanificacion(msg);
                } else {
                    block();
                }
            }
        });
        
        log.info("AgentePlanificadorEstrategico iniciado con {} planes estratégicos", 
                   planesEstrategicos.size());
    }
    
    private void inicializarConfiguracionPlanificacion() {
        configuracion = new ConfiguracionPlanificacion();
        configuracion.horizontePlanificacionEstrategico = 36; // 3 años en meses
        configuracion.horizontePlanificacionOperativo = 12; // 1 año en meses
        configuracion.frecuenciaRevision = 30; // días
        configuracion.toleranciaDesviacion = 0.15; // 15%
        configuracion.algoritmoOptimizacion = "ALGORITMO_GENETICO";
        configuracion.factorConfiabilidad = 0.95;
        
        log.info("Configuración de planificación inicializada: horizonte estratégico {} meses", 
                   configuracion.horizontePlanificacionEstrategico);
    }
    
    private void inicializarEscenariosBase() {
        // Escenario optimista
        Escenario optimista = new Escenario();
        optimista.id = "OPTIMISTA_2024";
        optimista.nombre = "Escenario Optimista 2024";
        optimista.descripcion = "Crecimiento sostenido del mercado";
        optimista.probabilidad = 0.3;
        optimista.parametros = Map.of(
            "crecimientoDemanda", 0.15,
            "eficienciaProduccion", 0.20,
            "reduccionCostos", 0.10,
            "nuevosClientes", 50
        );
        optimista.fechaCreacion = LocalDateTime.now();
        escenarios.put(optimista.id, optimista);
        
        // Escenario base
        Escenario base = new Escenario();
        base.id = "BASE_2024";
        base.nombre = "Escenario Base 2024";
        base.descripcion = "Crecimiento moderado del mercado";
        base.probabilidad = 0.5;
        base.parametros = Map.of(
            "crecimientoDemanda", 0.08,
            "eficienciaProduccion", 0.10,
            "reduccionCostos", 0.05,
            "nuevosClientes", 25
        );
        base.fechaCreacion = LocalDateTime.now();
        escenarios.put(base.id, base);
        
        // Escenario pesimista
        Escenario pesimista = new Escenario();
        pesimista.id = "PESIMISTA_2024";
        pesimista.nombre = "Escenario Pesimista 2024";
        pesimista.descripcion = "Contracción del mercado";
        pesimista.probabilidad = 0.2;
        pesimista.parametros = Map.of(
            "crecimientoDemanda", -0.05,
            "eficienciaProduccion", 0.02,
            "reduccionCostos", 0.15,
            "nuevosClientes", 5
        );
        pesimista.fechaCreacion = LocalDateTime.now();
        escenarios.put(pesimista.id, pesimista);
        
        log.info("Escenarios base inicializados: {}", escenarios.size());
    }
    
    private void inicializarMetricas() {
        // Métricas de planificación estratégica
        MetricaPlanificacion metricaEstrategica = new MetricaPlanificacion();
        metricaEstrategica.id = "CUMPLIMIENTO_ESTRATEGICO";
        metricaEstrategica.nombre = "Cumplimiento de Objetivos Estratégicos";
        metricaEstrategica.unidad = "PORCENTAJE";
        metricaEstrategica.valorActual = 85.0;
        metricaEstrategica.valorObjetivo = 90.0;
        metricaEstrategica.valorMinimo = 70.0;
        metricaEstrategica.frecuenciaCalculo = "MENSUAL";
        metricas.put(metricaEstrategica.id, metricaEstrategica);
        
        // Métricas de eficiencia operativa
        MetricaPlanificacion metricaOperativa = new MetricaPlanificacion();
        metricaOperativa.id = "EFICIENCIA_OPERATIVA";
        metricaOperativa.nombre = "Eficiencia Operativa General";
        metricaOperativa.unidad = "PORCENTAJE";
        metricaOperativa.valorActual = 78.5;
        metricaOperativa.valorObjetivo = 85.0;
        metricaOperativa.valorMinimo = 65.0;
        metricaOperativa.frecuenciaCalculo = "SEMANAL";
        metricas.put(metricaOperativa.id, metricaOperativa);
        
        // Métricas de utilización de recursos
        MetricaPlanificacion metricaRecursos = new MetricaPlanificacion();
        metricaRecursos.id = "UTILIZACION_RECURSOS";
        metricaRecursos.nombre = "Utilización de Recursos";
        metricaRecursos.unidad = "PORCENTAJE";
        metricaRecursos.valorActual = 72.0;
        metricaRecursos.valorObjetivo = 80.0;
        metricaRecursos.valorMinimo = 60.0;
        metricaRecursos.frecuenciaCalculo = "DIARIA";
        metricas.put(metricaRecursos.id, metricaRecursos);
        
        log.info("Métricas de planificación inicializadas: {}", metricas.size());
    }
    
    private void crearPlanesIniciales() {
        // Plan estratégico principal
        PlanEstrategico planPrincipal = new PlanEstrategico();
        planPrincipal.id = "PLAN_ESTRATEGICO_2024_2027";
        planPrincipal.nombre = "Plan Estratégico 2024-2027";
        planPrincipal.descripcion = "Plan estratégico integral para el período 2024-2027";
        planPrincipal.fechaInicio = LocalDate.now();
        planPrincipal.fechaFin = LocalDate.now().plusYears(3);
        planPrincipal.estado = "ACTIVO";
        planPrincipal.objetivos = Arrays.asList(
            "Incrementar participación de mercado en 25%",
            "Mejorar eficiencia operativa en 20%",
            "Desarrollar 3 nuevos productos",
            "Expandir a 2 nuevos mercados geográficos",
            "Alcanzar certificación ISO 14001"
        );
        planPrincipal.indicadores = Arrays.asList("ROI", "Market_Share", "Eficiencia_Operativa", "Satisfaccion_Cliente");
        planPrincipal.presupuesto = 2500000.0;
        planPrincipal.fechaCreacion = LocalDateTime.now();
        planesEstrategicos.put(planPrincipal.id, planPrincipal);
        
        // Plan operativo Q1 2024
        PlanOperativo planQ1 = new PlanOperativo();
        planQ1.id = "PLAN_OPERATIVO_Q1_2024";
        planQ1.nombre = "Plan Operativo Q1 2024";
        planQ1.descripcion = "Plan operativo para el primer trimestre de 2024";
        planQ1.planEstrategicoId = planPrincipal.id;
        planQ1.fechaInicio = LocalDate.now();
        planQ1.fechaFin = LocalDate.now().plusMonths(3);
        planQ1.estado = "EN_EJECUCION";
        planQ1.actividades = Arrays.asList(
            "Optimización de procesos de producción",
            "Implementación de sistema CRM",
            "Campaña de marketing digital",
            "Capacitación del personal",
            "Auditoría de calidad"
        );
        planQ1.recursos = Map.of(
            "personal", 25,
            "presupuesto", 150000.0,
            "equipos", 5
        );
        planesOperativos.put(planQ1.id, planQ1);
        
        log.info("Planes iniciales creados - Estratégicos: {}, Operativos: {}", 
                   planesEstrategicos.size(), planesOperativos.size());
    }
    
    private void procesarSolicitudPlanificacion(ACLMessage msg) {
        try {
            String action = msg.getOntology();
            String content = msg.getContent();
            
            switch (action) {
                case CREATE_STRATEGIC_PLAN:
                    crearPlanEstrategico(msg, content);
                    break;
                case CREATE_OPERATIONAL_PLAN:
                    crearPlanOperativo(msg, content);
                    break;
                case PLAN_RESOURCES:
                    planificarRecursos(msg, content);
                    break;
                case CREATE_PROJECT_PLAN:
                    crearPlanProyecto(msg, content);
                    break;
                case OPTIMIZE_SCHEDULE:
                    optimizarProgramacion(msg, content);
                    break;
                case FORECAST_DEMAND:
                    realizarPrevisionDemanda(msg, content);
                    break;
                case PLAN_CAPACITY:
                    planificarCapacidad(msg, content);
                    break;
                case GENERATE_PLANNING_REPORT:
                    generarReportePlanificacion(msg, content);
                    break;
                case UPDATE_PLAN:
                    actualizarPlan(msg, content);
                    break;
                case EVALUATE_SCENARIOS:
                    evaluarEscenarios(msg, content);
                    break;
                default:
                    log.warn("Acción de planificación no reconocida: {}", action);
            }
        } catch (Exception e) {
            log.error("Error procesando solicitud de planificación: {}", e.getMessage());
        }
    }
    
    private void crearPlanEstrategico(ACLMessage msg, String content) {
        try {
            Map<String, Object> request = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            
            PlanEstrategico plan = new PlanEstrategico();
            plan.id = UUID.randomUUID().toString();
            plan.nombre = (String) request.get("nombre");
            plan.descripcion = (String) request.get("descripcion");
            plan.fechaInicio = LocalDate.parse((String) request.get("fechaInicio"));
            plan.fechaFin = LocalDate.parse((String) request.get("fechaFin"));
            plan.objetivos = (List<String>) request.get("objetivos");
            plan.indicadores = (List<String>) request.get("indicadores");
            plan.presupuesto = ((Number) request.get("presupuesto")).doubleValue();
            plan.estado = "BORRADOR";
            plan.fechaCreacion = LocalDateTime.now();
            plan.escenarioBase = (String) request.getOrDefault("escenarioBase", "BASE_2024");
            
            // Validar y crear análisis de viabilidad
            AnalisisViabilidad analisis = realizarAnalisisViabilidad(plan);
            plan.analisisViabilidad = analisis;
            
            if (analisis.viabilidad > 0.7) {
                plan.estado = "APROBADO";
            } else if (analisis.viabilidad > 0.5) {
                plan.estado = "REVISION";
            }
            
            planesEstrategicos.put(plan.id, plan);
            
            // Generar hitos principales
            generarHitosPlan(plan);
            
            // Responder con resultado
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(objectMapper.writeValueAsString(Map.of(
                "planId", plan.id,
                "estado", plan.estado,
                "viabilidad", analisis.viabilidad,
                "recomendaciones", analisis.recomendaciones
            )));
            send(reply);
            
            log.info("Plan estratégico creado: {} - Viabilidad: {:.2f}", 
                       plan.nombre, analisis.viabilidad);
            
        } catch (Exception e) {
            log.error("Error creando plan estratégico: {}", e.getMessage());
        }
    }
    
    private AnalisisViabilidad realizarAnalisisViabilidad(PlanEstrategico plan) {
        AnalisisViabilidad analisis = new AnalisisViabilidad();
        analisis.fechaAnalisis = LocalDateTime.now();
        analisis.recomendaciones = new ArrayList<>();
        
        // Análisis de viabilidad temporal
        long duracionMeses = java.time.temporal.ChronoUnit.MONTHS.between(plan.fechaInicio, plan.fechaFin);
        double factorTiempo = duracionMeses <= 12 ? 1.0 : 
                             duracionMeses <= 36 ? 0.9 : 
                             duracionMeses <= 60 ? 0.7 : 0.5;
        
        // Análisis de viabilidad presupuestaria
        double presupuestoAnual = plan.presupuesto / (duracionMeses / 12.0);
        double factorPresupuesto = presupuestoAnual <= 1000000 ? 1.0 :
                                  presupuestoAnual <= 5000000 ? 0.8 :
                                  presupuestoAnual <= 10000000 ? 0.6 : 0.4;
        
        // Análisis de complejidad (número de objetivos)
        double factorComplejidad = plan.objetivos.size() <= 3 ? 1.0 :
                                  plan.objetivos.size() <= 5 ? 0.9 :
                                  plan.objetivos.size() <= 8 ? 0.7 : 0.5;
        
        // Cálculo de viabilidad general
        analisis.viabilidad = (factorTiempo + factorPresupuesto + factorComplejidad) / 3.0;
        
        // Generar recomendaciones
        if (factorTiempo < 0.8) {
            analisis.recomendaciones.add("Considerar dividir el plan en fases más cortas");
        }
        if (factorPresupuesto < 0.8) {
            analisis.recomendaciones.add("Revisar y optimizar el presupuesto asignado");
        }
        if (factorComplejidad < 0.8) {
            analisis.recomendaciones.add("Reducir el número de objetivos o priorizarlos");
        }
        
        if (analisis.recomendaciones.isEmpty()) {
            analisis.recomendaciones.add("Plan bien estructurado, proceder con la implementación");
        }
        
        return analisis;
    }
    
    private void generarHitosPlan(PlanEstrategico plan) {
        // Generar hitos trimestrales
        LocalDate fechaActual = plan.fechaInicio;
        LocalDate fechaFin = plan.fechaFin;
        int numeroHito = 1;
        
        while (fechaActual.isBefore(fechaFin)) {
            Hito hito = new Hito();
            hito.id = UUID.randomUUID().toString();
            hito.planId = plan.id;
            hito.nombre = "Hito Q" + numeroHito + " - " + plan.nombre;
            hito.descripcion = "Revisión trimestral de progreso";
            hito.fechaPrevista = fechaActual.plusMonths(3);
            hito.tipo = "REVISION_TRIMESTRAL";
            hito.estado = "PENDIENTE";
            hito.importancia = "ALTA";
            
            hitos.add(hito);
            
            fechaActual = fechaActual.plusMonths(3);
            numeroHito++;
        }
        
        log.info("Generados {} hitos para el plan {}", numeroHito - 1, plan.nombre);
    }
    
    private void realizarPrevisionDemanda(ACLMessage msg, String content) {
        try {
            Map<String, Object> request = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            String producto = (String) request.get("producto");
            int mesesPrevision = ((Number) request.get("mesesPrevision")).intValue();
            String metodo = (String) request.getOrDefault("metodo", "TENDENCIA_LINEAL");
            
            PrevisionDemanda prevision = new PrevisionDemanda();
            prevision.id = UUID.randomUUID().toString();
            prevision.producto = producto;
            prevision.metodo = metodo;
            prevision.mesesPrevision = mesesPrevision;
            prevision.fechaGeneracion = LocalDateTime.now();
            
            // Generar datos históricos simulados
            List<DatoHistorico> datosHistoricos = generarDatosHistoricosSimulados(producto, 12);
            prevision.datosHistoricos = datosHistoricos;
            
            // Calcular previsión según el método
            List<PrediccionMensual> predicciones = calcularPrevisiones(datosHistoricos, mesesPrevision, metodo);
            prevision.predicciones = predicciones;
            
            // Calcular métricas de confiabilidad
            prevision.nivelConfianza = calcularNivelConfianza(metodo, datosHistoricos);
            prevision.margenError = calcularMargenError(datosHistoricos);
            
            previsionesDemanda.put(prevision.id, prevision);
            
            // Responder con resultado
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(objectMapper.writeValueAsString(Map.of(
                "previsionId", prevision.id,
                "producto", prevision.producto,
                "nivelConfianza", prevision.nivelConfianza,
                "predicciones", predicciones.stream()
                    .map(p -> Map.of("mes", p.mes, "demandaPrevista", p.demandaPrevista))
                    .collect(Collectors.toList())
            )));
            send(reply);
            
            log.info("Previsión de demanda generada: {} - Producto: {} - Confianza: {:.2f}", 
                       prevision.id, producto, prevision.nivelConfianza);
            
        } catch (Exception e) {
            log.error("Error generando previsión de demanda: {}", e.getMessage());
        }
    }
    
    private List<DatoHistorico> generarDatosHistoricosSimulados(String producto, int meses) {
        List<DatoHistorico> datos = new ArrayList<>();
        Random random = new Random();
        double basedemanda = 1000 + random.nextDouble() * 2000; // Demanda base entre 1000-3000
        double tendencia = (random.nextDouble() - 0.5) * 0.1; // Tendencia entre -5% y +5%
        
        for (int i = 0; i < meses; i++) {
            DatoHistorico dato = new DatoHistorico();
            dato.mes = LocalDate.now().minusMonths(meses - i);
            
            // Simular estacionalidad
            double factorEstacional = 1.0 + 0.2 * Math.sin(2 * Math.PI * i / 12);
            
            // Agregar ruido aleatorio
            double ruido = 1.0 + (random.nextDouble() - 0.5) * 0.2;
            
            dato.demandaReal = basedemanda * (1 + tendencia * i / 12) * factorEstacional * ruido;
            dato.factoresExternos = Map.of(
                "estacionalidad", factorEstacional,
                "tendencia", tendencia,
                "ruido", ruido
            );
            
            datos.add(dato);
        }
        
        return datos;
    }
    
    private List<PrediccionMensual> calcularPrevisiones(List<DatoHistorico> historicos, int meses, String metodo) {
        List<PrediccionMensual> predicciones = new ArrayList<>();
        
        switch (metodo) {
            case "TENDENCIA_LINEAL":
                predicciones = calcularTendenciaLineal(historicos, meses);
                break;
            case "MEDIA_MOVIL":
                predicciones = calcularMediaMovil(historicos, meses);
                break;
            case "SUAVIZADO_EXPONENCIAL":
                predicciones = calcularSuavizadoExponencial(historicos, meses);
                break;
            default:
                predicciones = calcularTendenciaLineal(historicos, meses);
        }
        
        return predicciones;
    }
    
    private List<PrediccionMensual> calcularTendenciaLineal(List<DatoHistorico> historicos, int mesesFuturos) {
        List<PrediccionMensual> predicciones = new ArrayList<>();
        
        // Calcular tendencia lineal simple
        double sumaX = 0, sumaY = 0, sumaXY = 0, sumaX2 = 0;
        int n = historicos.size();
        
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = historicos.get(i).demandaReal;
            sumaX += x;
            sumaY += y;
            sumaXY += x * y;
            sumaX2 += x * x;
        }
        
        double pendiente = (n * sumaXY - sumaX * sumaY) / (n * sumaX2 - sumaX * sumaX);
        double intercepto = (sumaY - pendiente * sumaX) / n;
        
        // Generar predicciones
        for (int i = 1; i <= mesesFuturos; i++) {
            PrediccionMensual prediccion = new PrediccionMensual();
            prediccion.mes = LocalDate.now().plusMonths(i);
            prediccion.demandaPrevista = intercepto + pendiente * (n + i);
            prediccion.limiteInferior = prediccion.demandaPrevista * 0.85;
            prediccion.limiteSuperior = prediccion.demandaPrevista * 1.15;
            prediccion.confianza = 0.85;
            
            predicciones.add(prediccion);
        }
        
        return predicciones;
    }
    
    private List<PrediccionMensual> calcularMediaMovil(List<DatoHistorico> historicos, int mesesFuturos) {
        List<PrediccionMensual> predicciones = new ArrayList<>();
        int ventana = Math.min(6, historicos.size()); // Ventana de 6 meses o menos
        
        // Calcular media de los últimos períodos
        double suma = historicos.stream()
            .skip(historicos.size() - ventana)
            .mapToDouble(h -> h.demandaReal)
            .sum();
        double media = suma / ventana;
        
        // Generar predicciones
        for (int i = 1; i <= mesesFuturos; i++) {
            PrediccionMensual prediccion = new PrediccionMensual();
            prediccion.mes = LocalDate.now().plusMonths(i);
            prediccion.demandaPrevista = media;
            prediccion.limiteInferior = media * 0.80;
            prediccion.limiteSuperior = media * 1.20;
            prediccion.confianza = 0.75;
            
            predicciones.add(prediccion);
        }
        
        return predicciones;
    }
    
    private List<PrediccionMensual> calcularSuavizadoExponencial(List<DatoHistorico> historicos, int mesesFuturos) {
        List<PrediccionMensual> predicciones = new ArrayList<>();
        double alpha = 0.3; // Factor de suavizado
        
        // Calcular valor inicial
        double valorSuavizado = historicos.get(0).demandaReal;
        
        // Aplicar suavizado exponencial
        for (int i = 1; i < historicos.size(); i++) {
            valorSuavizado = alpha * historicos.get(i).demandaReal + (1 - alpha) * valorSuavizado;
        }
        
        // Generar predicciones
        for (int i = 1; i <= mesesFuturos; i++) {
            PrediccionMensual prediccion = new PrediccionMensual();
            prediccion.mes = LocalDate.now().plusMonths(i);
            prediccion.demandaPrevista = valorSuavizado;
            prediccion.limiteInferior = valorSuavizado * 0.82;
            prediccion.limiteSuperior = valorSuavizado * 1.18;
            prediccion.confianza = 0.80;
            
            predicciones.add(prediccion);
        }
        
        return predicciones;
    }
    
    private double calcularNivelConfianza(String metodo, List<DatoHistorico> datos) {
        return switch (metodo) {
            case "TENDENCIA_LINEAL" -> 0.85;
            case "SUAVIZADO_EXPONENCIAL" -> 0.80;
            case "MEDIA_MOVIL" -> 0.75;
            default -> 0.70;
        };
    }
    
    private double calcularMargenError(List<DatoHistorico> datos) {
        if (datos.size() < 2) return 0.20;
        
        double suma = 0;
        double media = datos.stream().mapToDouble(d -> d.demandaReal).average().orElse(0);
        
        for (DatoHistorico dato : datos) {
            suma += Math.pow(dato.demandaReal - media, 2);
        }
        
        double desviacion = Math.sqrt(suma / datos.size());
        return desviacion / media;
    }
    
    private void revisarProgresoPlanificacion() {
        try {
            // Revisar planes estratégicos
            for (PlanEstrategico plan : planesEstrategicos.values()) {
                if ("ACTIVO".equals(plan.estado) || "EN_EJECUCION".equals(plan.estado)) {
                    evaluarProgresoPlan(plan);
                }
            }
            
            // Revisar planes operativos
            for (PlanOperativo plan : planesOperativos.values()) {
                if ("EN_EJECUCION".equals(plan.estado)) {
                    evaluarProgresoOperativo(plan);
                }
            }
            
            // Revisar hitos
            revisarHitosPendientes();
            
            log.debug("Revisión de progreso de planificación completada");
            
        } catch (Exception e) {
            log.error("Error en revisión de progreso: {}", e.getMessage());
        }
    }
    
    private void evaluarProgresoPlan(PlanEstrategico plan) {
        // Calcular progreso temporal
        LocalDate hoy = LocalDate.now();
        long diasTranscurridos = java.time.temporal.ChronoUnit.DAYS.between(plan.fechaInicio, hoy);
        long diasTotales = java.time.temporal.ChronoUnit.DAYS.between(plan.fechaInicio, plan.fechaFin);
        double progresoTemporal = Math.min(1.0, (double) diasTranscurridos / diasTotales);
        
        // Simular progreso real (en implementación real se obtendría de métricas reales)
        double progresoReal = Math.random() * 1.1 * progresoTemporal;
        
        plan.progreso = progresoReal;
        
        // Evaluar desviaciones
        double desviacion = Math.abs(progresoReal - progresoTemporal);
        if (desviacion > configuracion.toleranciaDesviacion) {
            log.warn("Desviación detectada en plan {}: esperado {:.2f}, real {:.2f}", 
                       plan.nombre, progresoTemporal, progresoReal);
            
            // Generar alerta si hay desviaciones significativas
            if (progresoReal < progresoTemporal * 0.8) {
                plan.estado = "RETRASADO";
            } else if (progresoReal > progresoTemporal * 1.2) {
                plan.estado = "ADELANTADO";
            }
        }
    }
    
    private void evaluarProgresoOperativo(PlanOperativo plan) {
        // Evaluar progreso de actividades
        int actividadesCompletadas = (int) (Math.random() * plan.actividades.size());
        plan.actividadesCompletadas = actividadesCompletadas;
        plan.progreso = (double) actividadesCompletadas / plan.actividades.size();
        
        if (plan.progreso >= 1.0) {
            plan.estado = "COMPLETADO";
            plan.fechaFinalizacion = LocalDate.now();
        }
    }
    
    private void revisarHitosPendientes() {
        LocalDate hoy = LocalDate.now();
        
        for (Hito hito : hitos) {
            if ("PENDIENTE".equals(hito.estado) && !hito.fechaPrevista.isAfter(hoy)) {
                // Simular cumplimiento de hito
                boolean cumplido = Math.random() > 0.2; // 80% probabilidad de cumplimiento
                
                if (cumplido) {
                    hito.estado = "COMPLETADO";
                    hito.fechaCompletado = hoy;
                } else {
                    hito.estado = "RETRASADO";
                    hito.fechaPrevista = hoy.plusDays(7); // Reprogramar una semana
                }
                
                log.info("Hito {} - Estado: {}", hito.nombre, hito.estado);
            }
        }
    }
    
    private void actualizarPrevisionesDemanda() {
        try {
            // Actualizar previsiones existentes con nuevos datos
            for (PrevisionDemanda prevision : previsionesDemanda.values()) {
                if (prevision.fechaGeneracion.isBefore(LocalDateTime.now().minusDays(30))) {
                    // Regenerar previsión si tiene más de 30 días
                    List<DatoHistorico> nuevosHistoricos = generarDatosHistoricosSimulados(
                        prevision.producto, 12);
                    List<PrediccionMensual> nuevasPredicciones = calcularPrevisiones(
                        nuevosHistoricos, prevision.mesesPrevision, prevision.metodo);
                    
                    prevision.datosHistoricos = nuevosHistoricos;
                    prevision.predicciones = nuevasPredicciones;
                    prevision.fechaGeneracion = LocalDateTime.now();
                    
                    log.info("Previsión actualizada: {} - Producto: {}", 
                               prevision.id, prevision.producto);
                }
            }
            
        } catch (Exception e) {
            log.error("Error actualizando previsiones de demanda: {}", e.getMessage());
        }
    }
    
    private void optimizarAsignacionRecursos() {
        try {
            // Optimizar asignación de recursos entre planes activos
            List<PlanOperativo> planesActivos = planesOperativos.values().stream()
                .filter(p -> "EN_EJECUCION".equals(p.estado))
                .collect(Collectors.toList());
            
            for (PlanOperativo plan : planesActivos) {
                optimizarRecursosPlan(plan);
            }
            
            log.debug("Optimización de recursos completada para {} planes", planesActivos.size());
            
        } catch (Exception e) {
            log.error("Error en optimización de recursos: {}", e.getMessage());
        }
    }
    
    private void optimizarRecursosPlan(PlanOperativo plan) {
        // Simular optimización de recursos
        Map<String, Object> recursosActuales = plan.recursos;
        Map<String, Object> recursosOptimizados = new HashMap<>(recursosActuales);
        
        // Optimizar personal
        if (recursosActuales.containsKey("personal")) {
            int personalActual = (Integer) recursosActuales.get("personal");
            int personalOptimo = (int) (personalActual * (0.9 + Math.random() * 0.2));
            recursosOptimizados.put("personal", personalOptimo);
        }
        
        // Optimizar presupuesto
        if (recursosActuales.containsKey("presupuesto")) {
            double presupuestoActual = (Double) recursosActuales.get("presupuesto");
            double presupuestoOptimo = presupuestoActual * (0.95 + Math.random() * 0.1);
            recursosOptimizados.put("presupuesto", presupuestoOptimo);
        }
        
        plan.recursos = recursosOptimizados;
    }
    
    // Clases internas para modelado de datos
    
    public static class ConfiguracionPlanificacion {
        public int horizontePlanificacionEstrategico;
        public int horizontePlanificacionOperativo;
        public int frecuenciaRevision;
        public double toleranciaDesviacion;
        public String algoritmoOptimizacion;
        public double factorConfiabilidad;
    }
    
    public static class PlanEstrategico {
        public String id;
        public String nombre;
        public String descripcion;
        public LocalDate fechaInicio;
        public LocalDate fechaFin;
        public List<String> objetivos;
        public List<String> indicadores;
        public double presupuesto;
        public String estado;
        public LocalDateTime fechaCreacion;
        public String escenarioBase;
        public double progreso;
        public AnalisisViabilidad analisisViabilidad;
    }
    
    public static class PlanOperativo {
        public String id;
        public String nombre;
        public String descripcion;
        public String planEstrategicoId;
        public LocalDate fechaInicio;
        public LocalDate fechaFin;
        public LocalDate fechaFinalizacion;
        public List<String> actividades;
        public Map<String, Object> recursos;
        public String estado;
        public double progreso;
        public int actividadesCompletadas;
    }
    
    public static class PlanRecursos {
        public String id;
        public String nombre;
        public String tipo;
        public Map<String, Integer> recursosHumanos;
        public Map<String, Double> recursosMateriales;
        public Map<String, Double> recursosFinancieros;
        public LocalDate fechaInicio;
        public LocalDate fechaFin;
        public String estado;
    }
    
    public static class ProyectoPlanificado {
        public String id;
        public String nombre;
        public String descripcion;
        public String estado;
        public LocalDate fechaInicio;
        public LocalDate fechaFin;
        public List<String> entregables;
        public Map<String, Object> recursos;
        public double presupuesto;
        public double progreso;
    }
    
    public static class PrevisionDemanda {
        public String id;
        public String producto;
        public String metodo;
        public int mesesPrevision;
        public LocalDateTime fechaGeneracion;
        public List<DatoHistorico> datosHistoricos;
        public List<PrediccionMensual> predicciones;
        public double nivelConfianza;
        public double margenError;
    }
    
    public static class DatoHistorico {
        public LocalDate mes;
        public double demandaReal;
        public Map<String, Object> factoresExternos;
    }
    
    public static class PrediccionMensual {
        public LocalDate mes;
        public double demandaPrevista;
        public double limiteInferior;
        public double limiteSuperior;
        public double confianza;
    }
    
    public static class PlanCapacidad {
        public String id;
        public String recurso;
        public double capacidadActual;
        public double capacidadRequerida;
        public LocalDate fechaAnalisis;
        public String recomendacion;
    }
    
    public static class Escenario {
        public String id;
        public String nombre;
        public String descripcion;
        public double probabilidad;
        public Map<String, Object> parametros;
        public LocalDateTime fechaCreacion;
    }
    
    public static class Hito {
        public String id;
        public String planId;
        public String nombre;
        public String descripcion;
        public LocalDate fechaPrevista;
        public LocalDate fechaCompletado;
        public String tipo;
        public String estado;
        public String importancia;
    }
    
    public static class MetricaPlanificacion {
        public String id;
        public String nombre;
        public String unidad;
        public double valorActual;
        public double valorObjetivo;
        public double valorMinimo;
        public String frecuenciaCalculo;
        public LocalDateTime ultimaActualizacion;
    }
    
    public static class AnalisisViabilidad {
        public LocalDateTime fechaAnalisis;
        public double viabilidad;
        public List<String> recomendaciones;
        public Map<String, Double> factores;
    }
    
    // Métodos auxiliares pendientes de implementación
    
    private void crearPlanOperativo(ACLMessage msg, String content) {
        log.info("Creando plan operativo");
    }
    
    private void planificarRecursos(ACLMessage msg, String content) {
        log.info("Planificando recursos");
    }
    
    private void crearPlanProyecto(ACLMessage msg, String content) {
        log.info("Creando plan de proyecto");
    }
    
    private void optimizarProgramacion(ACLMessage msg, String content) {
        log.info("Optimizando programación");
    }
    
    private void planificarCapacidad(ACLMessage msg, String content) {
        log.info("Planificando capacidad");
    }
    
    private void generarReportePlanificacion(ACLMessage msg, String content) {
        log.info("Generando reporte de planificación");
    }
    
    private void actualizarPlan(ACLMessage msg, String content) {
        log.info("Actualizando plan");
    }
    
    private void evaluarEscenarios(ACLMessage msg, String content) {
        log.info("Evaluando escenarios");
    }
}
