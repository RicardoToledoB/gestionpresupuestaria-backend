# Plataforma de Gestión Presupuestaria DSSM - MySQL local y Railway

Esta versión mantiene H2 para pruebas rápidas, pero agrega MySQL para uso persistente local y en Railway.

## Perfiles disponibles

- `local-h2`: H2 en memoria. Perfil por defecto.
- `local-mysql`: MySQL instalado en tu notebook.
- `production`: MySQL en Railway.

## 1. Ejecutar con H2 local

```bash
cd backend
mvn spring-boot:run
```

Swagger:

```text
http://localhost:8080/api/v1/docs
```

## 2. Crear base MySQL local

En tu MySQL local:

```sql
CREATE DATABASE IF NOT EXISTS presupuesto_dssm
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

## 3. Ejecutar backend con MySQL local

Si tu usuario es `root` sin password:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local-mysql mvn spring-boot:run
```

Si tienes password:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local-mysql \
LOCAL_MYSQL_USERNAME=root \
LOCAL_MYSQL_PASSWORD='TU_PASSWORD' \
mvn spring-boot:run
```

También puedes usar una URL completa:

```bash
SPRING_PROFILES_ACTIVE=local-mysql \
LOCAL_MYSQL_URL='jdbc:mysql://localhost:3306/presupuesto_dssm?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8' \
LOCAL_MYSQL_USERNAME=root \
LOCAL_MYSQL_PASSWORD='TU_PASSWORD' \
mvn spring-boot:run
```

## 4. Crear MySQL en Railway

En Railway:

```text
+ New > Database > MySQL
```

Luego en el servicio backend agrega:

```text
SPRING_PROFILES_ACTIVE=production
```

Y asegúrate de que el backend tenga acceso a las variables del servicio MySQL:

```text
MYSQLHOST
MYSQLPORT
MYSQLDATABASE
MYSQLUSER
MYSQLPASSWORD
```

Si Railway no las inyecta automáticamente al backend, agrégalas como referencias desde el servicio MySQL, por ejemplo:

```text
MYSQLHOST=${{MySQL.MYSQLHOST}}
MYSQLPORT=${{MySQL.MYSQLPORT}}
MYSQLDATABASE=${{MySQL.MYSQLDATABASE}}
MYSQLUSER=${{MySQL.MYSQLUSER}}
MYSQLPASSWORD=${{MySQL.MYSQLPASSWORD}}
```

También configura CORS/JWT:

```text
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:4200,http://localhost:4300,http://127.0.0.1:4200,https://gestionpresupuestaria-frontend-production.up.railway.app,https://*.up.railway.app,https://*.dssm.cl
APP_JWT_SECRET=CAMBIAR_POR_UN_SECRETO_LARGO_DE_256_BITS_O_MAS
```

## 5. Subir backend a GitHub

```bash
cd backend
git add .
git commit -m "feat: configurar mysql local y railway"
git push origin main --force
```

## 6. Validación posterior

Después del despliegue:

1. Abrir Swagger.
2. Iniciar sesión con `admin / admin123`.
3. Importar Excel.
4. Revisar dashboard.
5. Revisar cuadratura.
6. Reiniciar el backend en Railway.
7. Confirmar que los datos siguen existiendo.

Si los datos sobreviven al reinicio, MySQL quedó funcionando correctamente.
