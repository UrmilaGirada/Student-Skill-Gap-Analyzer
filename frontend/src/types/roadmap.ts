export type RoadmapPriority = 'HIGH' | 'MEDIUM';

export interface SkillRecommendation {
  skill: string;
  priority: RoadmapPriority;
  reason: string;
  suggestedTopics: string[];
}

export interface RoadmapStep {
  order: number;
  skill: string;
  priority: RoadmapPriority;
  reason: string;
  suggestedTopics: string[];
}

export interface SkillGapRoadmapResponse {
  studentId: number;
  jobDescriptionId: number;
  jobTitle: string;
  companyName: string;
  matchPercentage: number;
  currentSkills: string[];
  matchedSkills: string[];
  partiallyMatchedSkills: string[];
  missingSkills: string[];
  recommendations: SkillRecommendation[];
  roadmap: RoadmapStep[];
}
