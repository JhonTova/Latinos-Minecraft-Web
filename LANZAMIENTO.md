# Lista de lanzamiento

Lo que hay que hacer **antes** de mandar el primer visitante a `latinosmc.com`.
Ordenada por lo que pasa si te la saltas, no por lo que cuesta hacerla.

---

## 🔴 Bloqueante — sin esto no se abre


### 1. Empezar de cero

Durante el desarrollo se creó de todo: cuentas, servidores, votos y un pedido
real de PayPal en modo sandbox con su asiento en el ledger. **Ese asiento es
dinero falso dentro de una contabilidad inmutable**: si se queda, tus ingresos
reales nunca cuadrarán con lo que diga el panel.

Copia de seguridad primero, por si acaso:

```bash
cd ~/latinosmc-website && mkdir -p data/copias
docker compose exec -T db pg_dump -U latinosmc latinosmc | gzip > data/copias/antes-de-lanzar.sql.gz
```

Y ahora el borrado completo:

```bash
docker compose down
sudo rm -rf data/postgres data/archivos
docker compose up -d
```

Elimina la base de datos entera y las imágenes subidas. Flyway vuelve a crear el
esquema al arrancar y deja los precios de los espacios en sus valores por
defecto.

> Se tira el volumen en lugar de vaciar tablas a propósito. Un `DELETE` deja
> secuencias avanzadas, historial de Flyway y filas huérfanas que nadie mira; el
> volumen fuera garantiza que lo que arranca es exactamente lo que arrancaría en
> una instalación nueva. Y de paso comprueba que el despliegue desde cero
> funciona — que es justo lo que interesa saber antes de abrir.

Después de esto **no existe ninguna cuenta**.

### 1b. Crear tu cuenta y hacerte administrador

No hay usuario de administrador por defecto, ni contraseña maestra. Es
deliberado: una credencial que viene puesta de fábrica es una credencial que
alguien olvida cambiar.

1. Entra en `https://latinosmc.com/registro` y regístrate con tu correo
2. Confirma el código que llega por email
3. Concédete el permiso:

```bash
docker compose exec db psql -U latinosmc -d latinosmc -c \
  "INSERT INTO platform_admins (user_id, notes)
   SELECT id, 'primer administrador' FROM users WHERE email = 'TU@CORREO.COM';"
```

4. Recarga `/panel` y aparecerá el acceso a **Administración**, en `/panel/admin`

Esa ruta devuelve **404** a quien no sea administrador, no un «no tienes
permiso»: quien no lo es no tiene por qué saber que existe.

### 2. Pasar el perfil a `prod`

En el `.env`:

```bash
PERFIL=prod
```

Cambia dos comportamientos, los dos importantes:

- La aplicación **se niega a arrancar** si falta el proveedor de correo. En
  `staging` los códigos de verificación salen por el log del contenedor, y nadie
  se entera de que ningún usuario los recibe hasta las quejas.
- Se niega a arrancar si `LATINOSMC_IDENTITY_PEPPER` tiene el valor de
  desarrollo. Sin esa clave, los hashes de identidad serían reversibles por
  cualquiera que lea el repositorio.

### 3. PayPal a modo real

Las credenciales que tienes ahora son de **Sandbox**: cobran dinero falso.

1. `developer.paypal.com` → **Apps & Credentials** → pestaña **Live**
2. Crea la app y copia Client ID y Secret (son distintos de los de sandbox)
3. Webhook nuevo apuntando a `https://api.latinosmc.com/api/v1/pagos/webhook`
   con `PAYMENT.CAPTURE.COMPLETED`, `PAYMENT.CAPTURE.REFUNDED` y
   `PAYMENT.CAPTURE.REVERSED`
4. En el `.env`: las tres claves nuevas y `PAYPAL_MODO=live`

Y haz **una compra de verdad** con tu propia tarjeta antes de anunciar nada. Es
la única forma de saber que el circuito completo funciona con dinero real; te
la reembolsas después.

### 4. Rotar lo que se pegó por chat

Durante el desarrollo pasaron por el chat la contraseña de PostgreSQL y las
claves de Stripe de prueba. Ninguna llegó nunca a git —está comprobado— pero
cámbialas igual: una credencial que ha viajado por un sitio que no controlas ya
no es secreta.

```bash
docker compose exec db psql -U latinosmc -d postgres \
  -c "ALTER USER latinosmc WITH PASSWORD 'la-nueva';"
```

Luego actualiza `DB_PASSWORD` en el `.env` y `docker compose up -d`.

### 5. Publicar el plugin y enlazarlo

```bash
cd plugin && mvn clean package
# -> plugin/target/LatinosMC-0.1.0.jar
```

Súbelo a una release de GitHub y pega el enlace en `/panel/admin` →
**Enlace de descarga del plugin**. Sin él, el botón no aparece: es mejor no
tenerlo que tenerlo llevando a ninguna parte.

---

### 6. Subir el límite de tamaño en Nginx

Nginx rechaza cualquier cuerpo de más de **1 MB** por defecto, y lo hace antes
de que la petición llegue a la aplicación. El síntoma es engañoso: la subida
falla y el log del backend está **vacío**, porque la petición nunca llegó.

```bash
sudo nano /etc/nginx/sites-available/latinosmc.com.conf
```

Va en el bloque de **`latinosmc.com`**, no solo en el de `api.`:

```nginx
client_max_body_size 8m;
```

**El bloque que importa es el de la web.** Todo lo que hace el navegador pasa
por ahí: las subidas del panel llaman a `/api/backend/...` en el dominio
principal y es el proxy de Next quien las reenvía al backend. El bloque de
`api.` solo lo usan el plugin y los webhooks de PayPal, que no suben imágenes —
ponerlo únicamente ahí no arregla nada y parece que sí.

Ponlo en los dos y te olvidas. Ocho megas cubre el tope de la aplicación (6 MB
por archivo) más lo que añade el envelope multipart. Después:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

> `nginx -t` primero, siempre. Recargar una configuración con un error de
> sintaxis deja el sitio caído hasta que alguien lo note.

### 7. Que Nginx escriba la IP real del visitante

Sin esto, **el antifraude por IP no protege nada**. La aplicación no ve al
visitante: ve al proxy. La única IP que conoce es la que Nginx le escriba, y con
esa IP se decide si un voto cuenta.

La configuración que sale en casi todas las guías (`$proxy_add_x_forwarded_for`)
**añade** la IP real detrás de lo que mandara el cliente. La aplicación se queda
con la primera, que es la que eligió el cliente. Resultado: una IP distinta por
voto, el cupo por IP desaparece y el ranking se compra con un script de diez
líneas.

Son dos piezas. La primera hace que Nginx sepa quién es el visitante de verdad
cuando hay Cloudflare delante — y que solo se lo crea si viene *de* Cloudflare:

```nginx
# /etc/nginx/conf.d/cloudflare.conf
#
# Genera la lista de rangos con:
#   curl -s https://www.cloudflare.com/ips-v4 | sed 's/^/set_real_ip_from /;s/$/;/'
#   curl -s https://www.cloudflare.com/ips-v6 | sed 's/^/set_real_ip_from /;s/$/;/'
set_real_ip_from 173.245.48.0/20;
set_real_ip_from 103.21.244.0/22;
# ... el resto ...

real_ip_header CF-Connecting-IP;
real_ip_recursive on;
```

La segunda va en **cada bloque `location`** de los dos sitios (`latinosmc.com` y
`api.latinosmc.com`):

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Forwarded-Proto $scheme;

# $remote_addr, NO $proxy_add_x_forwarded_for. El segundo añade la IP real
# detrás de lo que mandara el cliente, y la aplicación se queda con la primera
# de la lista: la que eligió él.
proxy_set_header X-Forwarded-For   $remote_addr;
```

Con `set_real_ip_from` puesto, `$remote_addr` ya es la IP del visitante —Nginx la
sustituye—, así que esa línea vale con Cloudflare y sin él.

```bash
sudo nginx -t && sudo systemctl reload nginx
```

Compruébalo desde otra máquina antes de dar por bueno el despliegue:

```bash
curl -s -H "CF-Connecting-IP: 6.6.6.6" -H "X-Forwarded-For: 6.6.6.6" \
  https://latinosmc.com/api/backend/servers > /dev/null
```

En el log del backend tiene que aparecer **tu IP real**, nunca `6.6.6.6`.

> Esto era un fallo real hasta la versión que estás desplegando: la aplicación
> se creía `CF-Connecting-IP` a ciegas, y esa cabecera la puede escribir
> cualquiera que le pegue directo al VPS. Ahora solo se lee `X-Forwarded-For`,
> pero eso solo sirve si Nginx la reescribe.

---

## 🟡 Muy recomendable — se puede abrir sin esto, pero duele

### 8. Cortafuegos solo para Cloudflare

Ahora mismo cualquiera que averigüe la IP del VPS puede saltarse Cloudflare y
pegarle directo al Nginx. Eso anula la protección anti-DDoS por la que pasa todo
lo demás, y es la segunda mitad de lo del punto 7.

```bash
# Permitir solo los rangos de Cloudflare en 80 y 443
for ip in $(curl -s https://www.cloudflare.com/ips-v4); do
  ufw allow from $ip to any port 443 proto tcp
  ufw allow from $ip to any port 80 proto tcp
done
ufw --force enable
```

> Comprueba que sigues teniendo acceso SSH **antes** de cerrar la sesión.

### 9. Copias de seguridad automáticas

Todo vive en `./data/`. Sin copias, un `rm -rf` desafortunado se lleva la
plataforma entera.

```bash
# En crontab -e, todos los días a las 4:00
0 4 * * * cd ~/latinosmc-website && docker compose exec -T db \
  pg_dump -U latinosmc latinosmc | gzip > data/copias/$(date +\%F).sql.gz
0 5 * * * find ~/latinosmc-website/data/copias -name "*.sql.gz" -mtime +30 -delete
```

Y **prueba a restaurar una**. Una copia que nunca se ha restaurado no es una
copia, es una carpeta con archivos dentro.

### 10. Google Search Console

`search.google.com/search-console` → verifica el dominio con el registro TXT
que te dé → envía `https://latinosmc.com/sitemap.xml`.

Sin esto Google tarda semanas en encontrarte. Con esto, días.

---

## 🟢 Cuando quieras

### 11. Acceso con Google y Discord

Sin credenciales, los botones **no aparecen** y el registro por correo funciona
igual. Cuando los quieras:

- Google: `console.cloud.google.com` → APIs y servicios → Credenciales
- Discord: `discord.com/developers/applications` → OAuth2

URL de retorno, exacta o rechazan el acceso. Es el dominio del **sitio**, no el
de la API: si no, el usuario termina el proceso sin haber entrado.

```
https://latinosmc.com/api/backend/auth/oauth/google/callback
https://latinosmc.com/api/backend/auth/oauth/discord/callback
```

### 12. Marca

En `/panel/admin`:

- **Logotipo** — se ve en la cabecera y, en grande, en la portada. Sin él se
  pintan las iniciales sobre un degradado, que funciona pero no es tuyo.
- **Imagen al compartir** (1200 × 630) — lo que sale al pegar el enlace en
  Discord o WhatsApp.
- **Título y descripción** — lo que Google enseña. Ya vienen escritos apuntando
  a lo que la gente busca; retócalos cuando veas qué funciona.

---

## Actualizar el despliegue

```bash
./scripts/actualizar.sh
```

Hace el `git pull`, reconstruye, levanta y comprueba. Y antes de levantar, crea
`data/` y le pone el dueño correcto.

Ese último paso es el que importa: **todo lo que sobrevive a una actualización
vive en `./data`** (base de datos, copias e imágenes subidas), y esa carpeta
está en `.gitignore`, así que el pull nunca la toca. Pero si falta, Docker la
crea **como root** al montar el volumen, mientras la aplicación corre como
usuario sin privilegios. Todo arranca aparentemente bien y el fallo aparece
después, al subir un icono:

> El servidor no puede escribir en su carpeta de archivos.

Si ya te ha pasado, se arregla en una línea y no se pierde nada:

```bash
sudo chown -R 10001:10001 data/archivos
docker compose restart api
```

---

## Comprobación automática

Antes de nada, esto revisa la cadena entera y dice qué falla y por qué:

```bash
./scripts/comprobar.sh
```

Recorre contenedores, migraciones, permisos de disco, las cinco rutas de la
API, las ocho de la web, el límite de subida de Nginx, los metadatos, el modo
de PayPal y el del correo.

Existe porque los últimos fallos que aparecieron en producción eran todos
invisibles desde fuera —un límite de Nginx, unos permisos de carpeta, una caché
de cinco minutos— y ninguno daba un error legible. Cada uno costó una ronda
entera de diagnóstico.

Termina con código de salida distinto de cero si hay fallos, así que sirve
también para encadenarlo en un despliegue.

## Comprobación final a mano

Lo que el script no puede hacer por ti, porque necesita un servidor de
Minecraft de verdad y una tarjeta:

| Qué | Dónde | Qué tiene que pasar |
|---|---|---|
| Registro | `/registro` | Llega el correo con el código |
| Alta de servidor | `/panel` | Da la clave del plugin, **una sola vez** |
| Plugin | Tu servidor | `/votar` devuelve enlace y código |
| Voto | El enlace | Acepta, y la recompensa llega sola en un minuto |
| Segundo voto | `/votar` otra vez | Dice «ya has votado hoy» sin generar nada |
| Compra | `/panel/destacar` | Cobra de verdad y aparece en la portada |
| Compartir | Pega `latinosmc.com` en Discord | Sale la tarjeta con tu imagen |

Si los siete pasan, se puede abrir.
