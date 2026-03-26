# Synchronizer – Sincronizador de Datos Master–Slave (PostgreSQL <-> MariaDB)

Proyecto 2 de la asignatura **Teoría de Base de Datos II (TBD2)**.  
Este repositorio contiene la implementación de un **sistema de sincronización de datos** entre una base de datos **MASTER en PostgreSQL** (esquema Pagila) y una base de datos **SLAVE en MariaDB**, desarrollado en **Java (NetBeans)** y utilizando **DBeaver** como herramienta principal de administración de bases de datos.

El sistema implementa:

- **Replicación selectiva** entre tablas del esquema Pagila.
- **Captura de cambios** mediante *shadow tables* y **triggers** en el SLAVE.
- Procesos de **Sync-IN** (MASTER → SLAVE) y **Sync-OUT** (SLAVE → MASTER).
- Un **dashboard de monitoreo** simple para controlar y visualizar el estado de la sincronización.

---

## Tabla de contenidos

1. [Descripción general](#descripción-general)  
2. [Arquitectura del sistema](#arquitectura-del-sistema)  
3. [Clasificación de tablas Pagila (IN / OUT)](#clasificación-de-tablas-pagila-in--out)  
4. [Tecnologías utilizadas](#tecnologías-utilizadas)  
5. [Estructura del proyecto](#estructura-del-proyecto)  
6. [Configuración de bases de datos](#configuración-de-bases-de-datos)  
7. [Captura de cambios en SLAVE (Shadow Tables y Triggers)](#captura-de-cambios-en-slave-shadow-tables-y-triggers)  
8. [Procesos de sincronización (Sync-IN / Sync-OUT)](#procesos-de-sincronización-sync-in--sync-out)  
9. [Dashboard de monitoreo](#dashboard-de-monitoreo)  
10. [Instalación y despliegue](#instalación-y-despliegue)  
11. [Ejecución y flujo de uso típico](#ejecución-y-flujo-de-uso-típico)  
12. [Pruebas](#pruebas) 

---

## Descripción general

El **objetivo principal** del proyecto es investigar y aplicar técnicas de **sincronización de datos entre motores de bases de datos heterogéneos**, reforzando especialmente:

- El uso de **triggers (disparadores)** para capturar cambios en tablas transaccionales.
- La implementación de lógica de integración y sincronización mediante:
  - Procedimientos lógicos en Java.
  - Scripts SQL de apoyo (creación de esquema SLAVE, triggers, shadow tables, etc.).

El sistema se basa en el esquema estándar **Pagila** en PostgreSQL como base de datos **MASTER** y una base de datos **SLAVE en MariaDB**, manteniendo compatibilidad estructural para las tablas que se sincronizan.

---

## Arquitectura del sistema

Se implementa un modelo de **replicación selectiva** con dos direcciones de flujo:

- **MASTER (PostgreSQL – Pagila)**  
  - Contiene el catálogo maestro y las tablas centrales.
  - Es la fuente de verdad para las **tablas de entrada (IN)**.

- **SLAVE (MariaDB)**  
  - Contiene tablas con estructura compatible con Pagila para las tablas seleccionadas.
  - Es el origen de las **tablas de salida (OUT)**, donde se registran operaciones transaccionales locales.
  - Implementa **triggers** y **shadow tables** (`*_log`) para capturar cambios.

### Flujo general

1. **Sync-IN (MASTER → SLAVE)**  
   - Se descargan datos desde PostgreSQL hacia MariaDB.
   - Se actualizan únicamente las tablas clasificadas como **IN**.
   - Se respeta la integridad referencial y se previenen duplicados.

2. **Sync-OUT (SLAVE → MASTER)**  
   - Se leen los cambios registrados en las tablas `*_log` de MariaDB.
   - Se aplican los cambios en las tablas correspondientes del MASTER (PostgreSQL).
   - Una vez confirmada la subida, se limpian las tablas de log.

3. **Dashboard**  
   - Permite lanzar manualmente Sync-IN y Sync-OUT.
   - Muestra el estado de la última sincronización, el progreso y los errores.

---

## Clasificación de tablas Pagila (IN / OUT)

El esquema **Pagila** se divide en dos grupos según el flujo de sincronización:

### A. Tablas de Entrada (IN) – MASTER → SLAVE

Tablas que representan catálogo maestro, configuraciones globales y estructura organizacional.  
El SLAVE **no puede modificarlas**; solo recibe actualizaciones desde el MASTER.

1. `actor` – Listado de actores.  
2. `category` – Categorías de películas.  
3. `language` – Idiomas registrados.  
4. `film` – Catálogo principal de películas.  
5. `film_actor` – Relación películas–actores.  
6. `film_category` – Relación películas–categorías.  
7. `country` – Países.  
8. `city` – Ciudades.  
9. `address` – Direcciones físicas.  
10. `store` – Sucursales.  
11. `staff` – Personal administrativo y operativo.  
12. `inventory` – Existencias de películas por sucursal.  

### B. Tablas de Salida (OUT) – SLAVE → MASTER

Tablas que representan actividad transaccional local:

1. `customer` – Nuevos clientes o actualización de perfiles.  
2. `rental` – Operaciones de alquiler realizadas en el nodo local.  
3. `payment` – Pagos procesados.

En estas tablas se habilita la captura de cambios en el SLAVE mediante shadow tables y triggers.

---

## Tecnologías utilizadas

- **Lenguaje de programación:** Java  
- **IDE:** NetBeans  
- **Motores de base de datos:**
  - **PostgreSQL** (MASTER – esquema Pagila)
  - **MariaDB** (SLAVE)
- **Herramienta de administración de DB:** DBeaver  
- **Framework / Librerías Java:**
  - JDBC para conexión a PostgreSQL y MariaDB
- **Frontend / Dashboard:**
  -  Java Swing


---
## Estructura del Proyecto

```text
Synchronizer/
├─ src/
│  └─ main/
│     └─ java/
│        └─ sync/
│           ├─ DashboardFrame.java
│           ├─ DatabaseConfig.java
│           ├─ SyncHistory.java
│           ├─ SyncIn.java
│           ├─ SyncOut.java
│           └─ SyncResult.java
├─ target/
│  └─ ... (archivos generados por Maven)
├─ README.md
├─ Slave_pagila_schema.sql
├─ sync_history.json
├─ triggers_slave_pagila.sql
├─ pom.xml
└─ .gitignore
```


---


## Configuración de bases de datos

### MASTER – PostgreSQL (Pagila)

1. Instalar PostgreSQL.
2. Crear la base de datos Pagila (o importar el esquema oficial).
3. Verificar que las tablas listadas en [Clasificación de tablas Pagila (IN / OUT)](#clasificación-de-tablas-pagila-in--out) existen y tienen las claves primarias y foráneas correctas.

### SLAVE – MariaDB

1. Crear una base de datos en MariaDB, por ejemplo:

   ```sql
   CREATE DATABASE pagila_slave CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Ejecutar el script de **esquema SLAVE** (adaptado a MariaDB), por ejemplo:

   ```bash
   # Desde DBeaver o cliente CLI de MariaDB
   SOURCE sql/slave/schema_slave_mariadb.sql;
   ```

3. Verificar que las tablas IN y OUT existen con una estructura compatible.

> Toda la administración y ejecución de scripts en las bases de datos se realizó utilizando **DBeaver**.

---

## Captura de cambios en SLAVE (Shadow Tables y Triggers)

Para cada tabla OUT (`customer`, `rental`, `payment`) se implementa una estrategia de **Shadow Tables**:

- Por cada tabla OUT se crea una tabla `_log`, por ejemplo:
  - `customer_log`
  - `rental_log`
  - `payment_log`
- Cada tabla `_log` almacena:
  - Tipo de operación (`INSERT`, `UPDATE`, `DELETE`)
  - Fecha/hora de la operación
  - Datos afectados (antes/después, según diseño)

### Ejemplo de tabla `_log` (MariaDB)

```sql
CREATE TABLE rental_log (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    operation_type ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    operation_timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rental_id INT,
    customer_id INT,
    inventory_id INT,
    rental_date DATETIME,
    return_date DATETIME,
    staff_id INT
);
```

### Ejemplo de trigger en tabla OUT (MariaDB)

```sql
DELIMITER $$

CREATE TRIGGER trg_rental_insert
AFTER INSERT ON rental
FOR EACH ROW
BEGIN
    INSERT INTO rental_log (
        operation_type, rental_id, customer_id, inventory_id,
        rental_date, return_date, staff_id
    ) VALUES (
        'INSERT',
        NEW.rental_id,
        NEW.customer_id,
        NEW.inventory_id,
        NEW.rental_date,
        NEW.return_date,
        NEW.staff_id
    );
END$$

DELIMITER ;
```

> En el proyecto real se definen triggers similares para `UPDATE` y `DELETE` en cada tabla OUT (`customer`, `rental`, `payment`), ajustando los campos de acuerdo con la estructura exacta de las tablas en MariaDB.

---

## Procesos de sincronización (Sync-IN / Sync-OUT)

### Sync-IN (MASTER → SLAVE)

Responsable de sincronizar las **tablas IN** desde PostgreSQL hacia MariaDB.

Flujo general:

1. Leer configuración de conexión a MASTER y SLAVE + `mapping.json`.
2. Para cada tabla IN:
   - Consultar cambios o realizar carga completa según la estrategia elegida.
   - Insertar/actualizar en SLAVE respetando:
     - Claves primarias.
     - Relaciones foráneas.
   - Manejar duplicados (por ejemplo, `ON DUPLICATE KEY UPDATE` en MariaDB).

La lógica se implementa en una clase Java del tipo:

```java
public class SyncInService {
    public void runSyncIn() {
        // 1. Cargar mapping.json
        // 2. Conectarse a PostgreSQL (MASTER) y MariaDB (SLAVE)
        // 3. Recorrer tablas IN y sincronizar datos
        // 4. Registrar en logs el resultado
    }
}
```

### Sync-OUT (SLAVE → MASTER)

Responsable de subir los cambios capturados en las tablas `*_log` del SLAVE (MariaDB) hacia el MASTER (PostgreSQL).

Flujo general:

1. Leer `mapping.json` y determinar qué tablas OUT manejar.
2. Para cada tabla OUT:
   - Leer registros pendientes en la tabla `_log`.
   - Interpretar tipo de operación (`INSERT`, `UPDATE`, `DELETE`).
   - Aplicar la operación correspondiente en la tabla del MASTER.
   - Registrar errores (por ejemplo, conflictos de llaves primarias, violaciones de FK).
3. Tras una sincronización exitosa, **limpiar** (vaciar) las tablas `_log`.

Ejemplo de pseudocódigo Java:

```java
public class SyncOutService {
    public void runSyncOut() {
        // 1. Cargar mapping.json
        // 2. Conectar a MariaDB (SLAVE) y PostgreSQL (MASTER)
        // 3. Leer tablas *_log según mapping
        // 4. Por cada registro log:
        //    - Generar y ejecutar el SQL apropiado en MASTER
        // 5. Si todo es correcto, truncar o borrar registros aplicados en *_log
    }
}
```

---

## Dashboard de monitoreo

El proyecto incluye (o está diseñado para incluir) un **dashboard web simple** que permite:

- Ver el estado de la última sincronización:
  - Fecha y hora.
  - Tipo de sincronización (IN/OUT).
  - Resultado (éxito, error).
- Lanzar manualmente:
  - **Botón Sync-IN**
  - **Botón Sync-OUT**
- Ver un listado o panel de:
  - Progreso de la ejecución.
  - Logs de errores (ej. conflictos de llaves, fallos de conexión).

---

## Instalación y despliegue

### Requisitos previos

- **Java**
- **NetBeans** para abrir y ejecutar el proyecto.
- **PostgreSQL** con esquema **Pagila**.
- **MariaDB** para la base de datos SLAVE.
- **DBeaver** (opcional pero recomendado) para gestionar y ejecutar los scripts SQL.

### Pasos generales

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/Scastelar/Synchronizer.git
   cd Synchronizer
   ```

2. **Configurar las bases de datos**
   - Crear y configurar la base de datos MASTER en PostgreSQL.
   - Crear y configurar la base de datos SLAVE en MariaDB utilizando los scripts en `sql/slave/`.

3. **Configurar conexiones en el proyecto Java**
   - Editar el archivo de configuración (por ejemplo `resources/application.properties` o la clase `DatabaseConfig.java`) con:
     - URL, usuario y contraseña de PostgreSQL.
     - URL, usuario y contraseña de MariaDB.

4. **Importar el proyecto en NetBeans**
   - Abrir NetBeans.
   - Importar el proyecto existente desde la carpeta del repositorio.
   - Asegurarse de que se reconocen correctamente las librerías externas (drivers JDBC, librerías JSON, etc.).

5. **Compilar el proyecto**
   - Compilar desde NetBeans o usando Maven/Gradle si el proyecto lo utiliza.

---

## Ejecución y flujo de uso típico

1. **Iniciar bases de datos**  
   Arrancar los servicios de PostgreSQL y MariaDB.

2. **Ejecutar la aplicación Java (Dashboard)**
   - Ejecutar la clase `Main` desde NetBeans.
   - Abrir el dashboard (ventana de la aplicación o página web, según implementación).

3. **Flujo de sincronización sugerido**
   - Hacer click en  **Sync-IN** para actualizar tablas IN en el SLAVE.
   - Realizar operaciones locales en el SLAVE (altas de clientes, rentals, payments) usando DBeaver u otra aplicación.
   - Verificar que los triggers generan entradas en las tablas `*_log`.
   - Hacer click en **Sync-OUT** para subir los cambios al MASTER.
   - Confirmar en la base MASTER que los datos se han replicado correctamente.

---

## Pruebas

Algunos escenarios de prueba recomendados:

1. **Creación de un nuevo cliente (customer) en SLAVE**
   - Insertar un `customer` en MariaDB.
   - Verificar que se genera entrada en `customer_log`.
   - Ejecutar Sync-OUT.
   - Verificar que el `customer` existe en PostgreSQL.

2. **Registrar un alquiler (rental) y pago (payment) en SLAVE**
   - Insertar registros en `rental` y `payment`.
   - Validar entradas en `rental_log` y `payment_log`.
   - Ejecutar Sync-OUT y verificar en MASTER.

3. **Actualización de un cliente en SLAVE**
   - Actualizar datos en `customer`.
   - Comprobar que el trigger registra operación `UPDATE`.
   - Sincronizar y verificar cambios en MASTER.

4. **Errores controlados**
   - Probar inserciones que generen conflictos de llave primaria o referencias inválidas, y comprobar que el sistema:
     - Registra el error en el dashboard/log.
     - Maneja correctamente la transacción (rollback/continuación).

---
