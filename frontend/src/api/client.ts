import axios, { AxiosError } from 'axios';
import { ApiError } from '../types/common';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

export function parseApiError(error: unknown): ApiError {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<{ message?: string; status?: number }>;
    if (axiosError.response) {
      const data = axiosError.response.data;
      const status = axiosError.response.status;
      const message = data?.message || getDefaultErrorMessage(status);
      return { status, message };
    } else if (axiosError.request) {
      return {
        status: 0,
        message: 'Unable to connect to the backend. Please make sure the Spring Boot server is running.',
      };
    }
  }

  if (error instanceof Error) {
    return { status: 0, message: error.message };
  }

  return { status: 0, message: 'An unexpected error occurred.' };
}

function getDefaultErrorMessage(status: number): string {
  switch (status) {
    case 400:
      return 'Bad request. Please verify the submitted data.';
    case 404:
      return 'Student or job description could not be found.';
    case 409:
      return 'A conflict occurred. A record with these details might already exist.';
    case 500:
      return 'Backend internal server error. Please try again later.';
    case 502:
      return 'Bad gateway from the backend service.';
    case 503:
      return 'Service temporarily unavailable. Please try again later.';
    default:
      return `Request failed with HTTP status ${status}.`;
  }
}
