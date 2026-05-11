# Plataforma de Gestión Presupuestaria DSSM

Proyecto base profesional para evolucionar la planilla de control presupuestario a una aplicación web con Spring Boot y Angular Material.

## Estado actual

Esta versión queda configurada para trabajar primero con **H2 en memoria**, sin instalar PostgreSQL ni MariaDB.

Esto permite validar rápidamente:

- Entidades JPA.
- Endpoints REST.
- Dashboard ejecutivo.
- Mantenedores base.
- Carga masiva desde Excel.
- Cálculo de saldos, ejecución y alertas.

## Ejecutar backend

```bash
cd backend
mvn spring-boot:run
```

El backend queda disponible en:

```text
http://localhost:8080/api/v1
```

## Swagger

```text
http://localhost:8080/api/v1/docs
```

## Consola H2

```text
http://localhost:8080/api/v1/h2-console
```

Datos de conexión:

```text
JDBC URL: jdbc:h2:mem:presupuesto_dssm
User: sa
Password: dejar vacío
```

## Endpoints principales

- `GET /api/v1/dashboard/summary`
- `GET /api/v1/dashboard/programs`
- `GET /api/v1/programs`
- `GET /api/v1/providers`
- `GET /api/v1/cdps`
- `GET /api/v1/cdps/alerts`
- `GET /api/v1/purchase-orders`
- `POST /api/v1/imports/excel`

## Importar Excel

Desde Swagger o desde el frontend, usar:

```text
POST /api/v1/imports/excel
```

Campo multipart:

```text
file
```

Subir el archivo Excel original, por ejemplo:

```text
AT Direccion.xlsx
```

## Ejecutar frontend

```bash
cd frontend
npm install
ng serve -o
```

El frontend consume por defecto:

```text
http://localhost:8080/api/v1
```

## Nota senior

La seguridad está abierta en esta etapa para acelerar pruebas locales. Cuando validemos el modelo, la carga del Excel y las pantallas principales, corresponde activar:

- Usuarios.
- Roles.
- Login.
- JWT.
- Auditoría.
- Restricciones por perfil.

## Próxima fase recomendada

1. Levantar backend con H2.
2. Abrir Swagger.
3. Importar el Excel real.
4. Revisar tablas H2.
5. Validar dashboard y CDP.
6. Ajustar mapeo de columnas exactas.
7. Luego avanzar a CRUD completos y seguridad JWT.

## Corrección de serialización JPA

Los endpoints de CDP y OC no exponen entidades JPA directamente. Devuelven DTOs planos (`CdpResponseDto` y `PurchaseOrderResponseDto`) para evitar errores de serialización de proxies Lazy de Hibernate, como `ByteBuddyInterceptor` / `hibernateLazyInitializer`.
