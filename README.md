# SMAGESCI - Sistema Multiagente para Gestión de Cadena de Suministro

## 📋 Descripción General

**SMAGESCI** (Sistema Multi-Agente para la Gestión Inteligente de Cadena de Suministro) es un sistema distribuido basado en agentes inteligentes desarrollado con el framework **JADE** (Java Agent DEvelopment Framework). Este sistema simula y gestiona una cadena de suministro completa utilizando 20+ agentes autónomos organizados en 14 categorías funcionales.

## 🏗️ Arquitectura del Sistema

### Tecnologías Principales

- **Java 17**: Lenguaje de programación principal
- **JADE 4.6.0**: Framework para desarrollo de sistemas multiagente
- **PostgreSQL 15**: Base de datos relacional para persistencia
- **Gradle**: Sistema de construcción y gestión de dependencias
- **Docker/Kubernetes**: Orquestación y despliegue de contenedores

### Dependencias Clave

- **Jackson 2.18.2**: Procesamiento de JSON
- **SLF4J/Logback**: Sistema de logging
- **PostgreSQL & MySQL Connectors**: Conectividad con bases de datos
- **JUnit 5 & Mockito**: Framework de testing

## 🤖 Agentes del Sistema

El sistema está compuesto por **20+ agentes inteligentes** distribuidos en las siguientes categorías:

### 1. Agentes Estratégicos (4)
- **OrquestadorPrincipal**: Coordina y supervisa todo el sistema
- **AnalizadorDeDatos**: Análisis de datos y métricas del sistema
- **GestorDeRiesgos**: Gestión y evaluación de riesgos
- **EvaluadorDeRendimiento**: Evaluación del rendimiento del sistema

### 2. Agentes de Suministro (5)
- **Proveedor_A/B/C**: Tres proveedores de materias primas con diferentes capacidades
- **CompradorMP**: Gestión de compras de materias primas
- **GestorInventarioMP**: Control de inventario de materias primas

### 3. Agentes de Producción (4)
- **PlanificadorProduccion**: Planificación de la producción
- **LineaProduccion_A/B**: Dos líneas de producción paralelas
- **ControlDeCalidad**: Control de calidad de productos

### 4. Agentes de Logística (5)
- **GestorInventarioPT**: Gestión de inventario de productos terminados
- **DespachadorPedidos**: Coordinación de despachos
- **Transportista_A/B/C**: Tres transportistas con diferentes capacidades

### 5. Agentes de Demanda (4)
- **GeneradorDemanda**: Generación de demanda simulada
- **ProcesadorPedidosCliente**: Procesamiento de pedidos
- **Cliente_1/2**: Simulación de clientes

### 6. Agentes de Soporte (4)
- **GestorMantenimiento**: Gestión de mantenimiento preventivo/correctivo
- **ServicioAlCliente**: Atención al cliente
- **GestorDeEnergia**: Optimización energética
- **OptimizadorDeRutas**: Optimización de rutas de transporte

### 7. Agentes Financieros (1)
- **GestorFinanciero**: Gestión financiera y contabilidad

### 8. Agentes de Análisis (1)
- **AnalisisDatos**: Análisis avanzado de datos

### 9. Agentes de Seguridad (1)
- **GestorSeguridad**: Seguridad del sistema

### 10. Agentes de Comunicaciones (1)
- **GestorComunicaciones**: Coordinación de comunicaciones entre agentes

### 11. Agentes de Planificación (1)
- **PlanificadorEstrategico**: Planificación estratégica a largo plazo

### 12. Agentes de Gestión de Riesgos (1)
- **GestorRiesgos**: Análisis y mitigación de riesgos

### 13. Agentes de Monitoreo (1)
- **GestorMonitoreo**: Monitoreo en tiempo real del sistema

### 14. Agentes de Optimización (1)
- **GestorOptimizacion**: Optimización continua del sistema

## 📁 Estructura del Proyecto

```
multiagente-test/
├── src/
│   ├── main/
│   │   ├── java/com/smagesci/
│   │   │   ├── AgentLauncher.java          # Lanzador de todos los agentes
│   │   │   ├── MainContainerLauncher.java  # Contenedor principal JADE
│   │   │   ├── DatabaseConnectionTest.java # Test de conectividad BD
│   │   │   ├── agents/                     # Paquete de agentes
│   │   │   │   ├── BaseAgent.java         # Clase base para agentes
│   │   │   │   ├── analytics/             # Agentes de análisis
│   │   │   │   ├── communications/        # Agentes de comunicación
│   │   │   │   ├── demand/                # Agentes de demanda
│   │   │   │   ├── finance/               # Agentes financieros
│   │   │   │   ├── logistics/             # Agentes logísticos
│   │   │   │   ├── monitoring/            # Agentes de monitoreo
│   │   │   │   ├── optimization/          # Agentes de optimización
│   │   │   │   ├── planning/              # Agentes de planificación
│   │   │   │   ├── production/            # Agentes de producción
│   │   │   │   ├── riskmanagement/        # Gestión de riesgos
│   │   │   │   ├── security/              # Agentes de seguridad
│   │   │   │   ├── strategic/             # Agentes estratégicos
│   │   │   │   ├── supply/                # Agentes de suministro
│   │   │   │   └── support/               # Agentes de soporte
│   │   │   ├── dao/                       # Data Access Objects
│   │   │   │   └── InventoryDAO.java
│   │   │   ├── models/                    # Modelos de datos
│   │   │   │   ├── Customer.java
│   │   │   │   ├── InventoryItem.java
│   │   │   │   ├── Order.java
│   │   │   │   ├── OrderItem.java
│   │   │   │   ├── Product.java
│   │   │   │   └── RawMaterial.java
│   │   │   └── utils/                     # Utilidades
│   │   │       ├── DatabaseManager.java
│   │   │       └── JsonUtil.java
│   │   └── resources/                     # Recursos
│   └── test/                              # Tests unitarios
├── sql/
│   └── init/                              # Scripts de inicialización BD
│       ├── 01-create-tables.sql
│       ├── 01-schema.sql
│       └── 02-additional-tables.sql
├── k8s/
│   └── database/                          # Configuraciones Kubernetes
│       ├── postgres-configmap.yaml
│       ├── postgres-deployment.yaml
│       ├── postgres-service.yaml
│       └── postgres-storage.yaml
├── libs/
│   └── jade/                              # Librerías JADE locales
├── logs/                                  # Logs del sistema
├── build.gradle                           # Configuración Gradle
├── build.gradle.kts                       # Configuración Gradle (Kotlin DSL)
├── docker-compose.yml                     # Configuración Docker Compose
├── test-system.sh                         # Script de prueba del sistema
└── README.md                              # Este archivo
```

## 🚀 Instalación y Configuración

### Prerrequisitos

- **Java JDK 17** o superior
- **Gradle** (incluido en el proyecto via wrapper)
- **Docker & Docker Compose** (para base de datos)
- **PostgreSQL 15** (si no usa Docker)

### Pasos de Instalación

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/armandobecerraro/multiagente-test.git
   cd multiagente-test
   ```

2. **Iniciar la base de datos (Docker)**
   ```bash
   docker-compose up -d
   ```
   
   Esto iniciará:
   - PostgreSQL en el puerto 5432
   - pgAdmin en el puerto 8080 (admin@admin.com / admin)

3. **Compilar el proyecto**
   ```bash
   ./gradlew assemble
   ```

4. **Verificar la conectividad de la base de datos**
   ```bash
   ./gradlew testDatabase
   ```

## 🎮 Uso del Sistema

### Opción 1: Script de Prueba Automatizado

```bash
./test-system.sh
```

Este script:
1. Compila el proyecto
2. Lanza el contenedor principal JADE
3. Inicia el OrquestadorPrincipal
4. Muestra la GUI RMA de JADE

### Opción 2: Ejecución Manual

**Terminal 1 - Iniciar el contenedor principal JADE:**
```bash
./gradlew run
```

Esto iniciará:
- El contenedor principal JADE
- La GUI RMA (Remote Monitoring Agent)
- El OrquestadorPrincipal

**Terminal 2 - Lanzar todos los agentes:**
```bash
./gradlew runAgentLauncher
```

Esto desplegará los 20+ agentes del sistema.

### Acceso a Servicios

- **JADE RMA GUI**: Se abre automáticamente con `./gradlew run`
- **pgAdmin**: http://localhost:8080
  - Usuario: admin@admin.com
  - Contraseña: admin
- **PostgreSQL**: localhost:5432
  - Base de datos: supply_chain
  - Usuario: postgres
  - Contraseña: password

## 🧪 Testing

### Ejecutar todos los tests
```bash
./gradlew test
```

### Ejecutar test de base de datos
```bash
./gradlew testDatabase
```

### Ver resultados de tests
Los resultados se generan en: `build/reports/tests/test/index.html`

## 🗄️ Base de Datos

### Esquema Principal

El sistema utiliza PostgreSQL con las siguientes tablas principales:

- **customers**: Información de clientes
- **products**: Catálogo de productos
- **orders**: Órdenes de compra
- **order_items**: Detalles de órdenes
- **inventory_items**: Inventario actual
- **raw_materials**: Materias primas

### Inicialización

Los scripts SQL en `sql/init/` se ejecutan automáticamente cuando se inicia el contenedor Docker de PostgreSQL por primera vez.

## 🐳 Despliegue con Docker

### Docker Compose (Desarrollo)

```bash
# Iniciar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f

# Detener servicios
docker-compose down

# Limpiar volúmenes
docker-compose down -v
```

### Kubernetes (Producción)

Los manifiestos de Kubernetes están en el directorio `k8s/database/`:

```bash
# Aplicar configuraciones
kubectl apply -f k8s/database/

# Verificar estado
kubectl get pods
kubectl get services
```

## 🛠️ Desarrollo

### Estructura de un Agente

Todos los agentes extienden de `BaseAgent` o directamente de `jade.core.Agent`:

```java
package com.smagesci.agents.category;

import com.smagesci.agents.BaseAgent;
import jade.core.behaviours.*;

public class MiAgente extends BaseAgent {
    @Override
    protected void setup() {
        super.setup();
        // Inicialización del agente
        
        // Agregar comportamientos
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Lógica del comportamiento
            }
        });
    }
}
```

### Agregar un Nuevo Agente

1. Crear la clase del agente en el paquete apropiado en `src/main/java/com/smagesci/agents/`
2. Implementar la lógica del agente
3. Registrar el agente en `AgentLauncher.java`
4. Compilar y probar

### Compilación y Build

```bash
# Compilar sin ejecutar tests
./gradlew assemble

# Compilar con tests
./gradlew build

# Limpiar build
./gradlew clean

# Ver todas las tareas disponibles
./gradlew tasks
```

## 📊 Características Principales

### Comunicación entre Agentes
- Uso de **ACL Messages** (Agent Communication Language) de JADE
- Protocolos de interacción: request, inform, query, propose
- Sistema de suscripción y notificación

### Persistencia de Datos
- Integración con PostgreSQL y MySQL
- DAO pattern para acceso a datos
- Gestión de transacciones

### Logging
- Sistema de logging con SLF4J y Logback
- Logs estructurados por niveles (DEBUG, INFO, WARN, ERROR)
- Archivos de log en directorio `logs/`

### Procesamiento JSON
- Serialización/deserialización con Jackson
- Soporte para tipos complejos y fechas
- Utilidades en `JsonUtil.java`

## 🔧 Configuración

### Propiedades del Sistema

Las propiedades se configuran en:
- `gradle.properties`: Configuración de Gradle
- `MainContainerLauncher.java`: Configuración de JADE
  - Puerto: 1099
  - Host: localhost
  - Platform ID: SMAGESCI-Platform

### Variables de Entorno

Para Docker Compose, las variables se definen en `docker-compose.yml`:
- `POSTGRES_DB`: Nombre de la base de datos
- `POSTGRES_USER`: Usuario de PostgreSQL
- `POSTGRES_PASSWORD`: Contraseña

## 📝 Logs y Monitoreo

### Ubicación de Logs
- Directorio principal: `logs/`
- Logs de JADE: Configurados en el contenedor principal
- Logs de aplicación: Via SLF4J/Logback

### Monitoreo del Sistema
- **JADE RMA**: Monitoreo visual de agentes en tiempo real
- **AgenteGestorMonitoreo**: Monitoreo programático del sistema
- **pgAdmin**: Monitoreo de base de datos

## 🔒 Seguridad

- **AgenteGestorSeguridad**: Gestión centralizada de seguridad
- Validación de mensajes entre agentes
- Control de acceso a recursos
- Auditoría de acciones

## 📈 Optimización

- **AgenteGestorOptimizacion**: Optimización continua del sistema
- Optimización de rutas de transporte
- Optimización de niveles de inventario
- Balanceo de carga entre agentes

## 🤝 Contribución

Para contribuir al proyecto:

1. Fork el repositorio
2. Crear una rama para tu feature (`git checkout -b feature/NuevaCaracteristica`)
3. Commit tus cambios (`git commit -m 'Agregar nueva característica'`)
4. Push a la rama (`git push origin feature/NuevaCaracteristica`)
5. Crear un Pull Request

## 📄 Licencia

Este proyecto está desarrollado como parte de un sistema de investigación y desarrollo.

## 👥 Autores

- Equipo SMAGESCI
- Organización: smagesci

## 📧 Contacto

Para preguntas o soporte, por favor crear un issue en el repositorio.

## 🎯 Roadmap

### Versión Actual (v2.0)
- ✅ 20+ agentes funcionales
- ✅ Integración con base de datos
- ✅ Sistema de monitoreo
- ✅ Sistema de optimización
- ✅ Despliegue con Docker
- ✅ Soporte para Kubernetes

### Versión Futura (v3.0)
- 🔄 Interfaz web para administración
- 🔄 API REST para integración externa
- 🔄 Machine Learning para predicción de demanda
- 🔄 Dashboard de métricas en tiempo real
- 🔄 Soporte multi-región
- 🔄 Clustering de agentes

## 🐛 Problemas Conocidos

- Los archivos `APDescription.txt` y `MTPs-Main-Container.txt` se generan automáticamente por JADE
- Se recomienda agregar estos archivos al `.gitignore` si no se desean versionar

## 💡 Tips y Buenas Prácticas

1. **Siempre iniciar el contenedor principal antes de lanzar los agentes**
2. **Utilizar el script `test-system.sh` para verificación rápida**
3. **Monitorear los logs para debugging**
4. **Usar la GUI RMA para visualizar el estado de los agentes**
5. **Mantener la base de datos respaldada antes de cambios mayores**

## 📚 Recursos Adicionales

- [Documentación JADE](https://jade.tilab.com/)
- [Documentación Gradle](https://docs.gradle.org/)
- [Documentación PostgreSQL](https://www.postgresql.org/docs/)
- [Documentación Docker](https://docs.docker.com/)

---

**SMAGESCI v2.0** - Sistema Multiagente para Gestión Inteligente de Cadena de Suministro
