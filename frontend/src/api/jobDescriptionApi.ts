import { apiClient } from './client';
import { JobDescriptionRequest, JobDescriptionResponse } from '../types/jobDescription';

export const jobDescriptionApi = {
  createJobDescription: async (
    studentId: number,
    request: JobDescriptionRequest
  ): Promise<JobDescriptionResponse> => {
    const response = await apiClient.post<JobDescriptionResponse>(
      `/api/students/${studentId}/job-descriptions`,
      request
    );
    return response.data;
  },

  findJobDescriptionsOfStudent: async (studentId: number): Promise<JobDescriptionResponse[]> => {
    const response = await apiClient.get<JobDescriptionResponse[]>(
      `/api/students/${studentId}/job-descriptions`
    );
    return response.data;
  },

  findJobDescription: async (
    studentId: number,
    jobDescriptionId: number
  ): Promise<JobDescriptionResponse> => {
    const response = await apiClient.get<JobDescriptionResponse>(
      `/api/students/${studentId}/job-descriptions/${jobDescriptionId}`
    );
    return response.data;
  },

  deleteJobDescription: async (studentId: number, jobDescriptionId: number): Promise<void> => {
    await apiClient.delete(`/api/students/${studentId}/job-descriptions/${jobDescriptionId}`);
  },
};
