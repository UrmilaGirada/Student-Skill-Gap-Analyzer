import { apiClient } from './client';
import { StudentSkill } from '../types/student';

export const studentSkillApi = {
  /** Existing Phase 3 endpoint: the skills currently stored for a student. */
  findSkillsOfStudent: async (studentId: number): Promise<StudentSkill[]> => {
    const response = await apiClient.get<StudentSkill[]>(`/api/students/${studentId}/skills`);
    return response.data;
  },
};
