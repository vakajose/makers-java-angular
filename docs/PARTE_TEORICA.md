# Parte Teórica (30%)

## 1. Optimización en sistemas financieros

**Pregunta:**
> Imagina que trabajas en una aplicación bancaria que debe procesar transacciones en tiempo real. ¿Qué estrategias aplicarías para mejorar el rendimiento y la escalabilidad? Considera aspectos como concurrencia, caché y patrones de diseño.

**Respuesta:**

*Iniciando por la concurrencia*
    
    Analizar que procesos transaccionales requiere aplicacion de estrategia de Bloqueo Optimista y cuales requiere Bloqueo pesimista. 
    Aplicaria bloqueo optimista con @Version a los procesos de baja a media contencion, logrando que hilos paralelos modifiquen el mismo recurso recibiendo el error respectivo.
    Aplicaria Bloqueo pesimista, a los procesos individuales que sufran de altisima contencion, por ejemplo, cuentas de alta transaccionabilidad dentro de la entidad financiera.

*Revisar la estrategia de ejecicucion de los hilos.*

    En funcion del stack implementado podemos trabajar de distinta manera los hilos hacia la base de datos, ya sea en MVC que es bloqueante por naturaleza o usando Spring WebFLux que tambien tiene sus particularidades, vigilaria que las configuraciones esten de acuerdo a los recursos de la bse de datos, evitando sobrecargar generando Context Switching.

*Minimizar la retencion de conexion en transacciones*

    Los @Transactional deben ser lo mas cortas posibles evitando secuentros de la conexion, aplicanto atomicidad en lo posible siendo pragmatico. Evitando envolver llamadas http por ejemplo.

*Patrones de diseño*

    CQRS es uno de los mejores aliados en este contexto, en funcion de la dimension del proyecto, en cualquiera de sus tres niveles, cuidando lo principal en cualquiera de ellos que es la optimizacion de la lectura de base de datos con denormalizacion, y la escritura mas controlada cuidando integridad y normalizacion, trabajando optimamente cuando el caso de uso lo requiere.

    Cache Aside, usando Redis en entornos distribuidos o algun motor de cache local en entornos monoliticos como Caffeine que es recomendado de spring 3 como el que use en la prueba tecnica. Cuidando siempre la actualizacion de los datos cuando estos son modificados usando @CacheEvict.

---

## 2. Seguridad en APIs financieras

**Pregunta:**
> Explica cómo protegerías una API que maneja información sensible de cuentas bancarias contra ataques como inyección SQL, CSRF, XSS y otros ataques comunes.

**Respuesta:**

*Seguridad en JWT para evitar CSRF*

    Almacenamiento seguro del token de sesion, no uso Localstorage ni session storages al ser accesible con javascript, guardo en memoria ram el access token evitando cualquier acceso por scripting.
    Aunque en API REST el token viaja en la cabecera BEARER y aqui no aplicaria directamente, si en entornos como thymeleaf hay que mantener la validacion de tokens.

*Uso correcto de los interceptors*

    Centralizar los errores con interceptores bien diseñados.

*Uso de consultas parametrizadas para evitar SQLi*

    Spring Data JPA nos permite usar consultas parametrizadas para evitar inyeccion SQL por defecto, tambien debemos prohibir concatenacion manual en Native Querys, asi el motor trata como valor y no como un codigo/script ejecutable.

*Hibernate Validator y Bean Validation para evitar XSS

    Validacioens estrictas en los DTOS, @NotBlank, @Size, etc.
    Tipado stricto y definido con application/json para segurar la interpretacion correcta del contenido.
    Aplicacion de politicas de seguridad (CSP).

---

## 3. Transacciones en sistemas distribuidos

**Pregunta:**
> En un sistema bancario distribuido, ¿cómo implementarías la consistencia y el manejo de errores en una API que procesa transferencias entre cuentas en diferentes servicios?

**Respuesta:**

*Aqui podriamos aplicar SAGA, en cualquiera de sus alternativas*

    Al tratarse de sistemas distribuidos kafka nos ayuda a solucionar este problema aplicando SAGA evitando el famoso Two Phase Commit. Podemos lanzar un evento que los otros servicios lean cuando un proceso termina de lado de un servicio disparando asi el proceso siguiente que depende de esa confirmacion, pudiendo asi implementar en cadena rollbacks para los servicios previos en caso de que alguno falle.
    Otra alternativa es que exista on orquestador que se encargue el de escuchar los eventos de los servicios involucrados y asi orquesta el flujo y gestiona los rollbacks con transacciones reversas en caso de necesitarse.


---

## 4. Pruebas unitarias y de integración

**Pregunta:**
> Describe cómo diseñarías una suite de pruebas que asegure la correcta operación de una API bancaria, considerando tanto pruebas unitarias como de integración. ¿Qué herramientas utilizarías?

**Respuesta:**

*No aplico un solo tipo de test ni herramientas si no uso el princpio de piramide de 4 niveles*

    1. Pruebas Unitarios de dominio, mencionando aqui la arquitectura hexagonal que implemente en la prueba, en el dominio, se aplican pruebas unitarias agnosticas al framework con Junit en este caso. Validando reglas de negocio, reglas de calculos de montos, etc.

    2. Pruebas unitarias de aplicacion, en donde pruebo los casos de uso, y la orquestacion de los mismos, aqui podriamos usar mockito para evitar levantar los puertos de salida a la persistencia.

    3. Pruebas de slice o slice test que evaluan el entorno web y parte de la seguridad. Aqui probamos JWT, podemos probar serializacion/deserializacion, podemos probar las reglas RBAC implementadas en conjunto con el ExceptionHandler, en este entorno tampoco levantamos instancia de persistencia.

    4. e2e de integracion, las mas robustas que buscan probar el conjunto de componentes de la infraestructura, en donde nos conectamos a una base datos, aqui me gusta usar playright, @SpringBooTest para  el backend, de lo mas importante aqui probar la gestion transaccional de los @Transactional que hemos implementado.

    Entre otras herarmientas suelo usar testcontainers para levantar instancias de docker efimeras usualmente en bases de datos para realizar pruebas con motores de bd reales pero con data fake.

---

## 5. Front-end

**Pregunta:**
> En una aplicación bancaria que muestra el saldo de las cuentas, ¿cómo gestionarías el estado y la autenticación en el front-end, garantizando la seguridad y la coherencia de los datos?

**Respuesta:**

*Cuidando de tener una sola fuente de verdad*

    Es importante que nuestras modificaciones actualicen correctamente los estados, en especial si usamos cache, siempre como regla sea cual sea le tecnologia que usemos actualizar la cache cuando la bd sufra alguna modificacion.

    Aqui por supuesto entra tambien el princpio del cuidado de la concurrencia mencionado en los anteriores puntos.

    En puntos criticos como los que mencionan nunca usar UI Optimista, y esperar siempre que el proceso termine en el backend antes de mostrar resultados.

    En angular usar RouteGuards.

    Usar Signals y NgRX correctamente segun corresponda. Usualmente una app bancanria tiende a tener bastante concurrencia en este caso NgRx se adapta mejor al manejar estados inmutables centralizados.

---

## 6. Spring Boot

**Pregunta:**
> ¿Qué pasa internamente en una aplicación cuando usas la anotación `@SpringBootApplication` y cómo afecta el arranque de la aplicación?

**Respuesta:**

*Arranque y @SpringBootApplication*
    
    Basicamente tenemos el SpringBootConfiguration que es la fuente inicial de las configuraciones de los beans dentro del contexto de la aplicacion.
    Luego el Component scan, que lee recursivamente los beans y sus dependencias y busca los distintos estereotipos.
    Finalmente Enable auto configuration, que seria probablemente el mas pesado al leer todo el descriptor de la configuracion. EN este punto podemos evitar que impacte negativamente el boot usando @Lazy en ciertos beans que pueden levatarse luego.

**Pregunta:**
> ¿Cómo funciona el ciclo de vida de un bean en Spring y cómo podrías intervenir en él?

**Respuesta:**

*Ciclo de vida de un bean en Spring*

    Son varios pasos, a grosso modo: 
        Se instancia mediante su constructor, 
        Luego hace iyeccion de dependencias, 
        Inicializa tomando en cuenta los modificadores del ciclo de vida, 
        Aplica el postprocesor que es donde aplica proxy dinamico si contienes aspectos tipo AOP como @transactional, @cacheable, etc.
        Registro en el contenedor
        Se destruye.

    Podemos usar @PostConstruct, @Predestroy,@Lazy como mencione anteriormente para manipular el ciclo de vida de los beans o aplicar transversalmente modificaciones a todos los beans.

**Pregunta:**
> ¿Cómo personalizarías el comportamiento de la autoconfiguración en Spring Boot sin romper la filosofía de «convención sobre configuración»?

**Respuesta:**

*Hay varias formas*
    La mas conocida es por medio de las configuraciones del archivo application.yml y/o properties, en donde aplicamos configuracion declarativa
    Uso de customizers especificos
    Exclusiones selectivas con exclude.
