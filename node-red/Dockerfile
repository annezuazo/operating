# Usamos la versión Debian (más estable y con herramientas estándar)
FROM nodered/node-red:latest-debian

# Cambiamos a root para poder instalar programas
USER root

# Instalamos Java (JRE) usando apt-get
RUN apt-get update && apt-get install -y default-jre

# IMPORTANTE: Volvemos al usuario 'node' para que Node-RED funcione bien
USER 1000