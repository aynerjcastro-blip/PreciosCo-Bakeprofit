// config.js
// Config del frontend — un solo lugar para ajustar según el entorno.
//
// Se arma dinámicamente a partir del protocolo y el host desde el que se
// está sirviendo esta misma página, así cada integrante del equipo no
// tiene que editar esto en su máquina si abre el frontend desde
// localhost, 127.0.0.1, su IP de red local, o un despliegue con HTTPS.
//
// Si el backend corre en un puerto distinto a 8080, cambia SOLO esa línea.
const API_BASE = `${window.location.protocol}//${window.location.hostname}:8080/api`;