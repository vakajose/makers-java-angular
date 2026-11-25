# Sistema de Gestión de Préstamos Bancarios

Aplicación Full-Stack (**Spring Boot 3.5.7 + Java 21** y **Angular 20 + Tailwind CSS v4**) para la gestión de solicitudes de préstamos bancarios y administración de usuarios.

- Especificación técnica: [`docs/SDD.md`](docs/SDD.md)
- Respuestas a las preguntas teóricas (30%): [`docs/PARTE_TEORICA.md`](docs/PARTE_TEORICA.md)

---

## Stack y Estructura del Proyecto

```text
makers_prueba_tecnica/
├── backend/                  # API REST Spring Boot 3.5.7 (Java 21, Maven Wrapper)
│   └── src/main/java/com/makers/loans/
│       ├── domain/           # Entidades ricas y excepciones en Java puro
│       ├── application/      # Puertos (in/out) y servicios de aplicación
│       └── infrastructure/   # Controladores REST, JPA, Caffeine, JWT Security, Config
├── frontend/                 # SPA Angular 20 + Tailwind CSS v4
│   └── src/app/
│       ├── core/             # AuthService, LoanService, UserService, Guards, Interceptor
│       ├── features/         # login, user-loans, admin-loans, admin-users
│       └── shared/           # NavbarComponent, StatusBadgeComponent
├── docs/
│   ├── SDD.md                # Especificación técnica para los agentes de IA
│   └── PARTE_TEORICA.md      # Plantilla para la parte teórica
└── docker-compose.yml        # PostgreSQL opcional para el perfil 'postgres'
```

---

## Credenciales Iniciales

Al iniciar el backend, `DataInitializer` crea únicamente los dos usuarios de prueba (sin préstamos precargados):

| Rol | Correo | Contraseña |
| :--- | :--- | :--- |
| `USER` | `usuario@test.com` | `123` |
| `ADMIN` | `admin@test.com` | `123` |

---

## Ejecución

### 1. Backend (H2 en memoria por defecto — perfil `dev`)

```bash
cd backend
./mvnw spring-boot:run
```

- API disponible en `http://localhost:8080`
- Consola H2 en `http://localhost:8080/h2-console` (`jdbc:h2:mem:loansdb`, usuario `sa`, sin contraseña)

### 2. Backend con PostgreSQL (Opcional — perfil `postgres`)

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

### 3. Frontend Angular

```bash
cd frontend
npm install
npm start
```

- Aplicación disponible en `http://localhost:4200`

---

## Contrato de API REST

| Método | Ruta | Rol | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Público | Autentica credenciales y devuelve JWT + perfil |
| `GET` | `/api/users/me` | `USER`, `ADMIN` | Retorna el perfil del usuario autenticado |
| `GET` | `/api/users` | `ADMIN` | Lista todos los usuarios |
| `POST` | `/api/users` | `ADMIN` | Crea un usuario (`USER` o `ADMIN`) |
| `PUT` | `/api/users/{id}` | `ADMIN` | Actualiza datos y rol de un usuario |
| `DELETE` | `/api/users/{id}` | `ADMIN` | Elimina un usuario sin préstamos asociados |
| `POST` | `/api/loans` | `USER`, `ADMIN` | Solicita préstamo (`amount`, `termMonths`) |
| `GET` | `/api/loans/my` | `USER`, `ADMIN` | Lista préstamos del usuario autenticado |
| `GET` | `/api/loans` | `ADMIN` | Lista todas las solicitudes |
| `GET` | `/api/loans/{id}` | `USER`, `ADMIN` | Consulta detalle de un préstamo (validando propiedad) |
| `PATCH` | `/api/loans/{id}/approve` | `ADMIN` | Aprueba préstamo en estado `PENDING` |
| `PATCH` | `/api/loans/{id}/reject` | `ADMIN` | Rechaza préstamo en estado `PENDING` |
| `DELETE` | `/api/loans/{id}` | `USER` (dueño), `ADMIN` | Cancela préstamo `PENDING` (`PENDING -> CANCELLED`) |

---

## Ejecución de Pruebas

### Backend (4 niveles: Dominio, Aplicación, `@WebMvcTest`, `@SpringBootTest`)

```bash
cd backend
./mvnw test
```

### Frontend (Jasmine / Karma)

```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadlessNoSandbox
npm run build
```

---

## Decisiones y Justificaciones Técnicas

1. **Modelo de ejecución web (Spring MVC vs. WebFlux):**
   - En la prueba sugiere usar WebFLux si es necesario, en este caso no lo es, dado que MVC es suficiente ademas que se podria implementar solamente encapsulandolo, dado que MVC es naturalmente bloqueante hacia la base de datos, y no trabajan con la misma filosofia que WebFlux.

2. **Arquitectura Hexagonal (`domain`, `application`, `infrastructure`):**
   - En este caso aplica hexagonal, aislando la logica de negocio y manteniendo aislado y agnostico del framework segun sugiere el estandar, ganando asi facilidad y rapidez en ejecucion de las pruebas y atomicidad en las mismas, usando dominios enriquecidos (no anemicos) que validan su propio comportamiento y son agnosticos al modelo de bd.
   Estos son orquestados desde la capa `application` que en este caso mantuve una logica de aplicacion pragmatica dado que si usamos algunas anotaciones de spring, pero dada la dimension de la prueba es suficiente para lo que necesitamos resolver sin entrar en demasiado boilerplate, en este nivel orquestamos, mapeamos (transformamos) y ejecutamos los casos de uso de solicitud y aprobacion de prestamos.
   Por ultimo la capa de infraestructura en laque tenemos implementada la mayoria de los componentes relacionados al framework.

3. **Persistencia dual (H2 + PostgreSQL), precisión monetaria**
   - Para evitar depender de un base de datos use H2 para avanzar rapidamente pero si se desea usar postgres, se peude levantar el contenedor incluido segun el readme.
   Uso Bigdecimal ya que es conocido en la industria no debemos usar float ni double en tipos de datos de representacion monetaria.

4. **Estrategia de caché (Caffeine):**
   - Decidi usar Caffeine porque es mas eficiente y facil de configurar que el sugerido, Ehcache, ademas que es el recomendado oficial por el stack de spring. Como mencione en las respuestas teoricas, usamos Cacheable al consultar y CacheEvict al modificar siempre en nuestros puertos de salida persistentes, teniendo cuidado de mantener la integridad en lo visualizado y lo existente en la bd.

5. **Seguridad (Spring Security 6 + JWT Stateless + Control de Acceso):**
   - Usamos JWT statles, y evitamos el problema IDOR/BOLA tomando siempre el dato del userid del token y no asi de otra fuente como url o dato en body, asi seguramos que el usuario solo puede consultar su informacion y no de otro usuario.
   Implementamos RBAC basico, usando controld de roles con @PreAuthorize("hasRole('ADMIN')") y @PreAuthorize("hasAnyRole('USER','ADMIN')") evitando asi tambien que usuarios puedan acceder a recursos no permitidos.

6. **Transacciones, concurrencia y manejo de errores:**
   - Uso de @Transactional, Bloqueo Optimista (`@Version`) frente a bloqueos pesimistas porque en este caso no lo necesitamos, borrado lógico (`PENDING -> CANCELLED`) para trazabilidad financiera usando maquina de estados y estandarización de errores con ProblemDetail.

7. **Frontend Angular (Signals, Reactive Forms, Guards e Interceptor):**
   - De igual forma como mencione en las respuestas teoricas uso signals dada la dimension del proyecto. Reactive forms como lo solicita el requerimiento, guards para ocultar los elementos visuales de un usuario que no pertenece a cierto rol, y interceptores para el JWT y el control de errores.

8. **Estrategia de pruebas:**
    1. Pruebas Unitarias de dominio, se aplican pruebas unitarias agnosticas al framework con Junit en este caso en LoanTest y UserTest. Validando reglas de negocio, reglas de calculos de montos con bigdecimal.

    2. Pruebas unitarias de aplicacion, en donde pruebo los casos de uso, y la orquestacion de los mismos en las clases AuthServiceTest, LoanServiceTest, UserServiceTest

    3. Backend Integracion e2e — Web/Seguridad (@WebMvcTest + MockMvc): LoanControllerWebMvcTest, UserControllerWebMvcTest.