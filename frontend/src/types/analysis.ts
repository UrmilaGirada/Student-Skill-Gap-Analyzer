export interface SkillGapAnalysisResponse {
  studentId: number;
  jobDescriptionId: number;
  jobTitle: string;
  companyName: string;
  totalRequiredSkills: number;
  matchedSkills: string[];
  partiallyMatchedSkills: string[];
  missingSkills: string[];
  matchedCount: number;
  partiallyMatchedCount: number;
  missingCount: number;
  matchPercentage: number;
}
