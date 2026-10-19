//URL base configurada desde las variables de entorno de Vite
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Obtiene las cabeceras por defecto, incluyendo el Token de sesión si existe.
 */
function getHeaders(customHeaders = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...customHeaders,
  };

  // Obtener el token guardado en localStorage (puedes ajustar la clave si usas 'token' o 'jwt')
  const token = localStorage.getItem('token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
}

/**
 * Procesa la respuesta de la API y maneja errores centralizados (401, 403, 400, 409, 500, etc.).
 */
async function handleResponse(response) {
  // Manejo de respuesta 204 No Content (ej: reportar o eliminar exitosamente)
  if (response.status === 204) {
    return null;
  }

  let data = null;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    data = await response.json();
  }

  if (!response.ok) {
    // Si el backend devuelve 401 (No autorizado/Token vencido), limpiamos la sesión
    if (response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // Opcional: redirigir a login si no estamos ya allí
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }

    // Normalizar el mensaje de error basándonos en el ErrorResponse del backend
    const errorMessage = data?.message || data?.error || `Error ${response.status}: ${response.statusText}`;
    
    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

/**
 * Wrapper HTTP reutilizable
 */
export async function apiClient(endpoint, { body, headers: customHeaders, customBaseUrl, ...customConfig } = {}) {
  const url = `${customBaseUrl || BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  const config = {
    method: body ? 'POST' : 'GET',
    ...customConfig,
    headers: getHeaders(customHeaders),
  };

  if (body) {
    config.body = typeof body === 'string' ? body : JSON.stringify(body);
  }

  try {
    const response = await fetch(url, config);
    return await handleResponse(response);
  } catch (error) {
    console.error(`[API Client Error] en ${config.method} ${url}:`, error.message);
    throw error;
  }
}

// Métodos auxiliares para simplificar llamadas
apiClient.get = (endpoint, customConfig = {}) => 
  apiClient(endpoint, { ...customConfig, method: 'GET' });

apiClient.post = (endpoint, body, customConfig = {}) => 
  apiClient(endpoint, { ...customConfig, method: 'POST', body });

apiClient.put = (endpoint, body, customConfig = {}) => 
  apiClient(endpoint, { ...customConfig, method: 'PUT', body });

apiClient.patch = (endpoint, body, customConfig = {}) => 
  apiClient(endpoint, { ...customConfig, method: 'PATCH', body });

apiClient.delete = (endpoint, customConfig = {}) => 
  apiClient(endpoint, { ...customConfig, method: 'DELETE' });

export default apiClient;