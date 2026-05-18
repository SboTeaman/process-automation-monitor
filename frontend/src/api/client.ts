import axios from 'axios'
import { useAuthStore } from '../store/authStore'

// Use relative URLs - Vite proxy handles routing to http://localhost:8080
// This allows frontend on 3004 to communicate with backend on 8080 without CORS issues
const API_BASE_URL = ''

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = useAuthStore.getState().refreshToken
      if (refreshToken) {
        try {
          const response = await axios.post('/auth/refresh', {
            refreshToken,
          })
          const { accessToken } = response.data
          useAuthStore.getState().setAuth(
            useAuthStore.getState().user!,
            accessToken,
            refreshToken
          )
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return client(originalRequest)
        } catch {
          useAuthStore.getState().logout()
        }
      }
    }
    return Promise.reject(error)
  }
)

export default client
