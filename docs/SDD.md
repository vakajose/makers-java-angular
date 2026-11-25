# Especificación de Diseño de Software (SDD) — Sistema de Gestión de Préstamos Bancarios

## 1. Visión General y Stack Tecnológico

Sistema Full-Stack para la gestión de solicitudes de préstamos bancarios y administración de usuarios, compuesto por una API REST en Spring Boot y una SPA en Angular.

| Componente | Tecnología / Elección |
| :--- | :--- |
| **Lenguaje Backend** | Java 21 |
| **Framework Web** | Spring Boot 3.5.7 (`spring-boot-starter-web` / Spring MVC sobre Tomcat) |
| **Arquitectura Backend** | Arquitectura Hexagonal (Ports & Adapters): `domain`, `application`, `infrastructure` |
| **Persistencia** | Spring Data JPA + Hibernate Validator (`users`, `loans`, `BigDecimal(19, 2)`) |
| **Base de Datos** | H2 en memoria (perfil `dev` por defecto) y PostgreSQL (perfil `postgres` + Docker Compose) |
| **Caché** | Caffeine sobre Spring Cache (`@EnableCaching`) en adaptador de persistencia |
| **Seguridad** | Spring Security 6 + JWT Stateless (`jjwt 0.12.6`) + BCrypt + `@PreAuthorize` |
| **Transacciones y Concurrencia** | `@Transactional` + Optimistic Locking (`@Version`) + `ProblemDetail` (RFC 7807) |
| **Frontend** | Angular 20 (SPA) + Tailwind CSS v4 + Reactive Forms + Signals + `sessionStorage` |
| **Pruebas** | Backend: JUnit 5, Mockito, `@WebMvcTest`, `@SpringBootTest` · Frontend: Jasmine / Karma |

---

## 2. Arquitectura del Backend

El backend sigue **Arquitectura Hexagonal** estricta bajo el paquete `com.makers.loans`, aplicando el Principio de Inversión de Dependencias (DIP):

- **`domain/` (Java 100% puro):** Contiene las entidades de dominio (`Loan`, `User`), enumeraciones (`LoanStatus`, `Role`) y excepciones de negocio (`DomainException` y subclases). No tiene dependencias ni anotaciones de Spring, JPA ni Lombok.
- **`application/`:** Contiene los puertos de entrada (`ports/in/`), puertos de salida (`ports/out/`) y servicios de aplicación (`AuthService`, `LoanService`, `UserService`). No utiliza `@Service`; los casos de uso se instancian y cablean explícitamente desde `infrastructure/config/BeanConfiguration.java`.
- **`infrastructure/`:** Contiene adaptadores de entrada REST (`adapters/in/web/`), adaptadores de salida JPA y caché (`adapters/out/persistence/`), seguridad (`security/`) y configuración (`config/`). Los controladores dependen exclusivamente de interfaces en `application/ports/in/`.

---

## 3. Modelo de Dominio y Reglas de Negocio

### 3.1 Usuarios (`User`)
- **Roles:** `USER` (cliente) y `ADMIN` (administrador).
- **Alta exclusiva por administrador:** No existe auto-registro público. Solo un usuario con rol `ADMIN` puede crear usuarios (`USER` o `ADMIN`), editarlos y eliminarlos.
- **Validaciones de dominio:**
  - `name`: obligatorio y no vacío.
  - `email`: obligatorio, con formato válido y normalizado en minúsculas sin espacios laterales; único en el sistema (`DuplicateEmailException` -> `409 Conflict`).
  - `password`: obligatorio al crear (mínimo 3 caracteres), almacenado cifrado con `BCryptPasswordEncoder`.
  - `role`: obligatorio (`USER` o `ADMIN`).
- **Reglas de integridad operativa:**
  - Un administrador autenticado no puede degradar su propio rol (`InvalidUserOperationException` -> `409 Conflict`).
  - Un administrador autenticado no puede eliminarse a sí mismo (`InvalidUserOperationException` -> `409 Conflict`).
  - No se permite eliminar un usuario que tenga préstamos registrados para preservar la trazabilidad histórica (`InvalidUserOperationException` -> `409 Conflict`).

### 3.2 Préstamos (`Loan`)
- **Validaciones de solicitud:**
  - `amount`: obligatorio, numérico decimal en rango `[1.00, 1000000000.00]` con escala de 2 decimales (`RoundingMode.HALF_UP`).
  - `termMonths`: obligatorio, entero en rango `[1, 360]`.
  - El préstamo nace siempre en estado `PENDING`.
- **Máquina de estados:**
  - `PENDING -> APPROVED` (`ADMIN` únicamente).
  - `PENDING -> REJECTED` (`ADMIN` únicamente).
  - `PENDING -> CANCELLED` (borrado lógico conservando historial; ejecutable por el `USER` propietario o por `ADMIN`).
  - Cualquier intento de transición desde un estado distinto de `PENDING` lanza `InvalidLoanStateException` (`409 Conflict`).

---

## 4. Persistencia, Perfiles y Caché

### 4.1 Persistencia Dual con Spring Profiles
- **Perfil por defecto (`dev`):** Base de datos H2 en memoria (`jdbc:h2:mem:loansdb`), consola H2 habilitada en `/h2-console` y `spring.jpa.open-in-view: false`.
- **Perfil `postgres`:** Configuración en `application-postgres.yml` para conectar a PostgreSQL (levantable mediante `docker-compose.yml` en el puerto `5432`).
- **Mapeo JPA:**
  - `UserEntity` mapeada a tabla `users` (`GenerationType.IDENTITY`).
  - `LoanEntity` mapeada a tabla `loans` (`GenerationType.IDENTITY`, `@ManyToOne(fetch = FetchType.LAZY)` a `UserEntity`, monto `BigDecimal(precision = 19, scale = 2)`, `@Version Long version`).
- **Datos iniciales (`DataInitializer`):** `ApplicationRunner` idempotente que precarga únicamente los dos usuarios definidos por el enunciado:
  - `usuario@test.com` / `123` (rol `USER`)
  - `admin@test.com` / `123` (rol `ADMIN`)
  - No se precargan préstamos iniciales al arrancar la aplicación.

### 4.2 Caché con Caffeine
- Habilitada con `@EnableCaching` en `CacheConfig` y configurada en `application.yml` (`maximumSize=500,expireAfterWrite=10m`, caché `loans`).
- Aplicada exclusivamente en `LoanPersistenceAdapter`:
  - `@Cacheable` en `findByUserId`, `findAll` y `findById`.
  - `@CacheEvict(value = "loans", allEntries = true)` en `save` para invalidar consultas tras crear, aprobar, rechazar o cancelar préstamos.

---

## 5. Seguridad, Transacciones, Concurrencia y Manejo de Errores

- **Autenticación y Autorización:**
  - `SecurityFilterChain` con `SessionCreationPolicy.STATELESS`, `JwtAuthenticationFilter` (`OncePerRequestFilter`) y `@EnableMethodSecurity` (`@PreAuthorize`).
  - Rutas públicas: `POST /api/auth/login`, `/h2-console/**` (perfil `dev`) y `/error`.
  - CSRF deshabilitado (autenticación vía cabecera `Authorization: Bearer <token>`).
  - CORS restringido al origen configurado (`http://localhost:4200`).
  - Prevención de IDOR: al crear, listar o cancelar préstamos como cliente, la identidad del solicitante se extrae siempre del `@AuthenticationPrincipal` del JWT y se valida propiedad (`LoanAccessDeniedException` -> `403 Forbidden`).
- **Transacciones y Concurrencia:**
  - `@Transactional` en operaciones de escritura y `@Transactional(readOnly = true)` en lecturas de los servicios de aplicación y en `LoanPersistenceAdapter.save`.
  - Optimistic Locking mediante `@Version` en `LoanEntity` (y `saveAndFlush`) para evitar condiciones de carrera si dos administradores operan sobre el mismo préstamo simultáneamente (`409 Conflict`).
- **Manejo de Errores:**
  - `GlobalExceptionHandler` (`@RestControllerAdvice`) y handlers de seguridad devolviendo `ProblemDetail` (RFC 7807) con códigos `400`, `401`, `403`, `404`, `409` y `500`.

---

## 6. Contrato de API REST

| Método | Ruta | Rol requerido | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Público | Autentica usuario (`email`, `password`) y retorna JWT + perfil. |
| `GET` | `/api/users/me` | `USER`, `ADMIN` | Retorna el perfil del usuario autenticado. |
| `GET` | `/api/users` | `ADMIN` | Lista todos los usuarios registrados. |
| `POST` | `/api/users` | `ADMIN` | Crea un usuario con rol `USER` o `ADMIN`. |
| `PUT` | `/api/users/{id}` | `ADMIN` | Actualiza datos y rol de un usuario (contraseña opcional). |
| `DELETE` | `/api/users/{id}` | `ADMIN` | Elimina un usuario sin préstamos asociados (no permite auto-borrado). |
| `POST` | `/api/loans` | `USER`, `ADMIN` | Solicita préstamo (`amount`, `termMonths`); `userId` tomado del JWT. |
| `GET` | `/api/loans/my` | `USER`, `ADMIN` | Lista préstamos del usuario autenticado (cacheado). |
| `GET` | `/api/loans` | `ADMIN` | Lista todos los préstamos (cacheado). |
| `GET` | `/api/loans/{id}` | `USER`, `ADMIN` | Consulta detalle de préstamo validando propiedad (cacheado). |
| `PATCH` | `/api/loans/{id}/approve` | `ADMIN` | Aprueba préstamo `PENDING` (`PENDING -> APPROVED`). |
| `PATCH` | `/api/loans/{id}/reject` | `ADMIN` | Rechaza préstamo `PENDING` (`PENDING -> REJECTED`). |
| `DELETE` | `/api/loans/{id}` | `USER` (dueño), `ADMIN` | Cancela préstamo `PENDING` mediante borrado lógico (`PENDING -> CANCELLED`). |

---

## 7. Arquitectura Frontend

- **Framework y Estilos:** Angular 20 (componentes standalone) con Tailwind CSS v4.
- **Gestión de Estado y Seguridad:**
  - `AuthService`, `LoanService` y `UserService` basados en **Angular Signals** (`signal`, `computed`) y `HttpClient`.
  - Token JWT y perfil persistidos en `sessionStorage`.
  - `authInterceptor` (`HttpInterceptorFn`) adjunta `Authorization: Bearer <token>` y cierra sesión automáticamente ante respuestas `401`.
  - `authGuard` y `adminGuard` (`CanActivateFn`) protegen rutas según autenticación y rol `ADMIN`.
  - Sin actualizaciones optimistas: toda mutación espera confirmación del backend y actualiza o recarga el estado.
- **Vistas y Formularios Reactivos:**
  - **`/login` (`LoginComponent`):** Formulario reactivo de inicio de sesión (sin auto-registro). Redirige a `/loans` o `/admin/loans` según rol.
  - **`/loans` (`UserLoansComponent`):** Formulario reactivo de solicitud de préstamo (`amount`, `termMonths`) con cálculo estimado de cuota, resumen de métricas, historial de préstamos con badges de estado y acción **Cancelar** para solicitudes `PENDING`.
  - **`/admin/loans` (`AdminLoansComponent`):** Resumen de solicitudes, filtros por estado (`ALL`, `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`) y acciones **Aprobar** / **Rechazar** sobre préstamos `PENDING`.
  - **`/admin/users` (`AdminUsersComponent`):** CRUD completo de usuarios (`USER` y `ADMIN`) mediante formulario reactivo (contraseña requerida al crear, opcional al editar) y listado con acciones de edición y eliminación.

---

## 8. Estrategia de Pruebas

- **Backend (Pirámide en 4 niveles):**
  1. **Dominio puro (JUnit 5):** `LoanTest`, `UserTest` sin mocks ni contexto Spring.
  2. **Aplicación (JUnit 5 + Mockito):** `AuthServiceTest`, `LoanServiceTest`, `UserServiceTest` mockeando puertos de salida.
  3. **Capa Web / Seguridad (`@WebMvcTest` + `MockMvc`):** `LoanControllerWebMvcTest`, `UserControllerWebMvcTest` verificando `@Valid` (`400`), autenticación (`401`), autorización por rol (`403`) y `ProblemDetail` (`409`).
  4. **Integración E2E (`@SpringBootTest` + `MockMvc` sobre H2):** `LoanAndUserFlowIntegrationTest` verificando arranque limpio (2 usuarios, 0 préstamos), CRUD usuarios, flujo completo de préstamos y cancelación, caché Caffeine, anti-IDOR y `@Version`.
- **Frontend (Jasmine / Karma):**
  - Guards e Interceptor: `auth.guard.spec.ts`, `admin.guard.spec.ts`, `auth.interceptor.spec.ts`.
  - Componentes: `login.component.spec.ts`, `user-loans.component.spec.ts`, `admin-loans.component.spec.ts`, `admin-users.component.spec.ts`.

---

## 9. Estructura del Repositorio

```text
makers_prueba_tecnica/
├── backend/                  # Proyecto Maven Spring Boot 3.5.7 (Java 21)
│   └── src/main/java/com/makers/loans/
│       ├── domain/           # Java puro: model/, exception/
│       ├── application/      # ports/in/, ports/out/, services/
│       └── infrastructure/   # adapters/in/web/, adapters/out/persistence/, security/, config/
├── frontend/                 # Proyecto Angular 20 + Tailwind CSS v4
│   └── src/app/
│       ├── core/             # auth/, guards/, interceptors/, models/, services/
│       ├── features/         # login/, user-loans/, admin-loans/, admin-users/
│       └── shared/           # components/ (navbar, status-badge)
├── docs/
│   ├── SDD.md                # Especificación técnica definitiva
│   └── PARTE_TEORICA.md      # Plantilla para las 6 preguntas teóricas
├── docker-compose.yml        # PostgreSQL opcional para perfil 'postgres'
└── README.md                 # Guía operativa y justificaciones técnicas
```
