# FIPE Async Integration

Projeto em Java 21 com Spring Boot WebFlux para integração com a FIPE usando:

- API-1 para expor endpoints REST
- API-2 para consumir a fila e enriquecer dados
- RabbitMQ para processamento assíncrono
- PostgreSQL para persistência SQL
- Redis para cache
- Spring Security com HTTP Basic
- OpenAPI/Swagger para documentação

## Arquitetura

### API-1

Responsável por:

- iniciar a carga inicial de marcas
- consultar marcas persistidas
- consultar modelos por marca
- consultar detalhes internos de um modelo
- atualizar nome e observações de um modelo

Principais classes:

- `src/main/java/org/fipe/entrypoint/api1/FipeController.java`
- `src/main/java/org/fipe/application/InitialLoadService.java`
- `src/main/java/org/fipe/application/VehicleQueryService.java`
- `src/main/java/org/fipe/entrypoint/api1/ApiExceptionHandler.java`

### API-2

Responsável por:

- consumir mensagens da fila com `vehicleType`, `code` e `name`
- buscar modelos na FIPE por marca
- persistir modelos no PostgreSQL
- invalidar cache afetado

Principais classes:

- `src/main/java/org/fipe/messaging/BrandQueueConsumer.java`
- `src/main/java/org/fipe/application/BrandLoadProcessor.java`
- `src/main/java/org/fipe/integration/fipe/FipeHttpGateway.java`
- `src/main/java/org/fipe/persistence/VehicleUpsertRepository.java`

### Fluxo assíncrono

1. O cliente chama `POST /api/v1/{vehicleType}/brands/initial-load`.
2. A API-1 consulta `/{vehicleType}/brands` na FIPE v2.
3. Cada item retornado pela FIPE, com `code` e `name`, é enviado para a fila.
4. A API-2 consome a mensagem da fila.
5. Para cada marca, a API-2 consulta `/{vehicleType}/brands/{brandId}/models`.
6. Os modelos são persistidos no PostgreSQL.
7. O cache Redis da marca e da lista de marcas é invalidado.

## Endpoints

### Carga inicial

```http
POST /api/v1/{vehicleType}/brands/initial-load
```

Exemplo:

```bash
curl -X POST "http://localhost:8080/api/v1/cars/brands/initial-load" \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM="
```

Resposta:

```json
{
  "message": "Carga inicial enviada para a fila com sucesso.",
  "vehicleType": "cars",
  "totalBrandsEnqueued": 107
}
```

### Referências FIPE

```http
GET /api/v1/references
```

### Marcas persistidas

```http
GET /api/v1/{vehicleType}/brands
```

Resposta compatível com FIPE v2:

```json
[
  {
    "code": "58",
    "name": "Volvo"
  }
]
```

### Modelos persistidos por marca

```http
GET /api/v1/{vehicleType}/brands/{brandId}/models
```

Resposta compatível com FIPE v2:

```json
[
  {
    "code": "2296",
    "name": "440 Turbo 1.8"
  }
]
```

### Detalhes internos de um modelo

```http
GET /api/v1/{vehicleType}/brands/{brandId}/models/{modelId}
```

Resposta:

```json
{
  "vehicleType": "cars",
  "brandCode": "58",
  "brandName": "Volvo",
  "code": "2296",
  "name": "440 Turbo 1.8",
  "observations": null
}
```

### Atualização de modelo

```http
PUT /api/v1/{vehicleType}/brands/{brandId}/models/{modelId}
```

Body:

```json
{
  "modelName": "440 Turbo 1.8 Ajustado",
  "observations": "Atualizado manualmente"
}
```

## Contrato atual

### `vehicleType`

Os endpoints documentam estes valores:

- `cars`
- `motorcycles`
- `trucks`

### Mensagem da fila

Formato publicado para a API-2:

```json
{
  "vehicleType": "cars",
  "code": "58",
  "name": "Volvo"
}
```

## Banco de dados

Tabela principal:

```sql
vehicles (
  id uuid primary key,
  vehicle_type varchar(30),
  reference_code integer,
  brand_code varchar(50),
  brand_name varchar(255),
  vehicle_code varchar(50),
  model_name varchar(255),
  observations varchar(1000),
  created_at timestamp,
  updated_at timestamp
)
```

Script:

- `src/main/resources/schema.sql`

## Cache

O Redis é usado para:

- lista de marcas por `vehicleType`
- lista de modelos por `vehicleType + brandCode`

Implementação:

- `src/main/java/org/fipe/support/VehicleCacheService.java`

## Segurança

Todos os endpoints de negócio exigem HTTP Basic.

Credenciais padrão:

- usuário: `admin`
- senha: `admin123`

Swagger e OpenAPI ficam liberados.

## Configuração

Arquivo:

- `src/main/resources/application.yaml`

A aplicação usa variáveis de ambiente para:

- Postgres
- RabbitMQ
- Redis
- credenciais da aplicação
- token FIPE

Defaults locais apontam para `localhost`.

## Execução local

### Infraestrutura

```bash
docker compose up -d postgres rabbitmq redis
```

### Aplicação

```bash
./mvnw spring-boot:run
```

## Execução completa via Docker

```bash
docker compose up --build
```

Serviços:

- app: `http://localhost:8080`
- swagger: `http://localhost:8080/swagger-ui.html`
- rabbitmq management: `http://localhost:15672`

## OpenAPI

Documentação dinâmica:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

Contrato estático:

- `src/main/resources/openapi/fipe-api.yaml`

## Testes

Executar:

```bash
./mvnw test
```

Cobertura atual:

- carga inicial
- publicação na fila
- consumo e persistência de modelos
- consultas com cache
- atualização de modelo

## Limites conhecidos

- O projeto usa um único processo com dois contextos lógicos, API-1 e API-2.
- A referência FIPE não é mais exposta na API pública, mas continua sendo usada internamente com o valor padrão configurado.
