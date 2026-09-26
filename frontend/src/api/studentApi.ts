import { apiClient } from './client';
import { HealthResponse } from '../types/common';
import { StudentProfile } from '../types/student';

export const healthApi = {
  checkHealth: async (): Promise<HealthResponse> => {
    const response = await apiClient.get<HealthResponse>('/api/health');
    return response.data;
  },
};

export const studentApi = {
  createStudent: async (student: Omit<StudentProfile, 'id'>): Promise<StudentProfile> => {
    const response = await apiClient.post<StudentProfile>('/api/students', student);
    return response.data;
  },

  getAllStudents: async (): Promise<StudentProfile[]> => {
    const response = await apiClient.get<StudentProfile[]>('/api/students');
    return response.data;
  },

  getStudentById: async (id: number): Promise<StudentProfile> => {
    const response = await apiClient.get<StudentProfile>(`/api/students/${id}`);
    return response.data;
  },

  updateStudent: async (id: number, student: StudentProfile): Promise<StudentProfile> => {
    const response = await apiClient.put<StudentProfile>(`/api/students/${id}`, student);
    return response.data;
  },

  deleteStudent: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/students/${id}`);
  },
};
