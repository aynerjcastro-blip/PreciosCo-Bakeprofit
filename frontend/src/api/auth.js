import apiClient from './client';

export const login = async (credentials) => {
  const data = await apiClient.post('/api/auth/login', credentials);
  if (data?.token) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
  }
  return data;
};

export const register = async (userData) => {
  return await apiClient.post('/api/auth/register', userData);
};