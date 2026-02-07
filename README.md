# SMAGESCI - Multi-Agent Supply Chain System

**S**istema **M**ultiagente de **A**dministración y **GES**tión de **C**adena de Suministro **I**nteligente

A robust, production-grade multi-agent system for intelligent supply chain management using the JADE framework.

## 🌟 Overview

SMAGESCI is a comprehensive multi-agent system designed to optimize and manage complex supply chain operations. The system consists of 20+ autonomous agents distributed across 12 specialized categories, working collaboratively to ensure efficient supply chain operations from procurement to delivery.

## 🏗️ Architecture

### Agent Categories & Components

#### Strategic Management (4 agents)
- **OrquestadorPrincipal**: Main orchestrator coordinating all system operations
- **AnalizadorDeDatos**: Data analysis and insights generation
- **GestorDeRiesgos**: Strategic risk management and planning
- **EvaluadorDeRendimiento**: Performance evaluation and metrics tracking

#### Supply Chain (5 agents)
- **Proveedor_A/B/C**: Raw material suppliers
- **CompradorMP**: Raw materials procurement agent
- **GestorInventarioMP**: Raw materials inventory management
- **GestorDeCompras**: Purchase management and supplier evaluation

#### Production (4 agents)
- **PlanificadorProduccion**: Production planning and scheduling
- **LineaProduccion_A/B**: Production line management
- **ControlDeCalidad**: Quality control and assurance

#### Logistics (5 agents)
- **GestorInventarioPT**: Finished goods inventory management
- **DespachadorPedidos**: Order dispatch and fulfillment
- **Transportista_A/B/C**: Transportation and shipping agents
- **GestorDeTransporte**: Transport management and optimization

#### Demand Management (3 agents)
- **GeneradorDemanda**: Demand forecasting and generation
- **ProcesadorPedidosCliente**: Customer order processing
- **Cliente_1/2**: Customer simulation agents

#### Support Services (4 agents)
- **GestorMantenimiento**: Maintenance management
- **ServicioAlCliente**: Customer service
- **GestorDeEnergia**: Energy management and optimization
- **OptimizadorDeRutas**: Route optimization

#### Specialized Services
- **GestorFinanciero**: Financial management and cost tracking
- **AnalisisDatos**: Advanced analytics and reporting
- **GestorSeguridad**: System security and authentication
- **GestorComunicaciones**: Inter-agent communication management
- **PlanificadorEstrategico**: Strategic planning and forecasting
- **GestorRiesgos**: Operational risk management
- **GestorMonitoreo**: System monitoring and alerting
- **GestorOptimizacion**: Process optimization

## 🛠️ Technology Stack

- **Language**: Java 17
- **Multi-Agent Framework**: JADE 4.6.0
- **Database**: PostgreSQL 15
- **Connection Pooling**: HikariCP 5.1.0
- **JSON Processing**: Jackson 2.18.2
- **Logging**: SLF4J 2.0.16 + Logback 1.5.12
- **Build Tool**: Gradle 8.x
- **Containerization**: Docker & Docker Compose

## 📋 Prerequisites

- Java Development Kit (JDK) 17 or higher
- Docker and Docker Compose (for database)
- Gradle (wrapper included)

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/armandobecerraro/multiagente-test.git
cd multiagente-test
```

### 2. Configure Environment Variables

Create a `.env` file in the project root based on `.env.example`:

```bash
cp .env.example .env
```

Edit `.env` and set secure passwords:

```env
POSTGRES_PASSWORD=your_secure_database_password
DB_PASSWORD=your_secure_database_password
PGADMIN_DEFAULT_PASSWORD=your_secure_pgadmin_password
```

### 3. Start Database Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database on port 5432
- PgAdmin web interface on port 8080

### 4. Build the Project

```bash
./gradlew build
```

## 🎮 Running the System

### Option 1: Run Main Container with GUI

Start the JADE main container with RMA (Remote Monitoring Agent) GUI:

```bash
./gradlew run
```

This launches the main container and the OrquestadorPrincipal agent.

### Option 2: Run All Agents

To launch all 20+ agents in the system:

```bash
./gradlew runAgentLauncher
```

### Option 3: Test Database Connection

Verify database connectivity:

```bash
./gradlew testDatabase
```

## 📁 Project Structure

```
multiagente-test/
├── src/
│   ├── main/
│   │   ├── java/com/smagesci/
│   │   │   ├── agents/          # Agent implementations
│   │   │   │   ├── analytics/
│   │   │   │   ├── communications/
│   │   │   │   ├── demand/
│   │   │   │   ├── finance/
│   │   │   │   ├── logistics/
│   │   │   │   ├── monitoring/
│   │   │   │   ├── optimization/
│   │   │   │   ├── planning/
│   │   │   │   ├── production/
│   │   │   │   ├── riskmanagement/
│   │   │   │   ├── security/
│   │   │   │   ├── strategic/
│   │   │   │   ├── supply/
│   │   │   │   └── support/
│   │   │   ├── dao/             # Data Access Objects
│   │   │   ├── models/          # Data models
│   │   │   └── utils/           # Utility classes
│   │   └── resources/
│   │       └── application.properties
│   └── test/                    # Unit tests
├── sql/                         # Database initialization scripts
├── libs/                        # JADE framework libraries
├── docker-compose.yml           # Docker services configuration
├── build.gradle                 # Gradle build configuration
└── .env.example                 # Environment variables template
```

## 🔒 Security Features

- Environment-based configuration for sensitive data
- No hardcoded credentials in source code
- HikariCP connection pooling with connection validation
- Secure inter-agent communication via JADE ACL messages
- Role-based access control in security agent
- Audit logging for all critical operations

## 🧪 Testing

Run the test suite:

```bash
./gradlew test
```

Run tests with detailed output:

```bash
./gradlew test --info
```

## 📊 Monitoring

The system includes a dedicated monitoring agent (**GestorMonitoreo**) that tracks:
- System performance metrics
- Agent health and availability
- Resource utilization
- SLA compliance
- Alert generation for anomalies

Access monitoring data through the JADE RMA GUI or via direct agent communication.

## 🔧 Configuration

### Database Configuration

Edit `.env` file to configure database connection:

```env
DB_URL=jdbc:postgresql://localhost:5432/supply_chain
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

### Agent Configuration

Agent-specific configurations are managed within each agent's setup method. Advanced configurations can be externalized to `application.properties`.

## 🤝 Agent Communication Pattern

Agents communicate using JADE's ACL (Agent Communication Language) messages:

1. **Request-Response**: Synchronous operations
2. **Inform**: Status updates and notifications
3. **Subscribe**: Event-based notifications
4. **Query**: Information retrieval

Example communication flow:
```
Cliente → ProcesadorPedidosCliente → GestorInventarioPT → DespachadorPedidos → Transportista
```

## 📈 Performance Optimization

- **Connection Pooling**: HikariCP manages database connections efficiently
- **Asynchronous Behaviors**: CyclicBehaviour for non-blocking message processing
- **Ticker Behaviors**: Scheduled periodic tasks with configurable intervals
- **Resource Management**: Automatic cleanup and connection management

## 🐛 Troubleshooting

### Database Connection Issues

1. Verify Docker containers are running: `docker-compose ps`
2. Check database logs: `docker-compose logs postgres`
3. Validate environment variables in `.env` file

### Agent Launch Failures

1. Ensure main container is running before launching agents
2. Check JADE platform port (1099) is available
3. Review agent logs in console output

### Build Errors

1. Verify Java 17+ is installed: `java -version`
2. Clean and rebuild: `./gradlew clean build`
3. Check dependencies: `./gradlew dependencies`

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Contributors

- Armando Becerra - Project Lead
- SMAGESCI Development Team

## 🔗 Resources

- [JADE Framework Documentation](https://jade.tilab.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [HikariCP Documentation](https://github.com/brettwooldridge/HikariCP)

## 📧 Support

For issues and questions, please open an issue in the GitHub repository.

---

**Version**: 2.0  
**Last Updated**: February 2026
