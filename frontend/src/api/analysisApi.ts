import { apiClient } from './client';
import { SkillGapAnalysisResponse } from '../types/analysis';
import { SkillGapRoadmapResponse } from '../types/roadmap';

export const analysisApi = {
  findSkillGap: async (
    studentId: number,
    jobDescriptionId: number
  ): Promise<SkillGapAnalysisResponse> => {
    const response = await apiClient.get<SkillGapAnalysisResponse>(
      `/api/students/${studentId}/job-descriptions/${jobDescriptionId}/skill-gap`
    );
    return response.data;
  },

  findRoadmap: async (
    studentId: number,
    jobDescriptionId: number
  ): Promise<SkillGapRoadmapResponse> => {
    const response = await apiClient.get<SkillGapRoadmapResponse>(
      `/api/students/${studentId}/job-descriptions/${jobDescriptionId}/roadmap`
    );
    return response.data;
  },
};
