export interface StudentProfile {
  id?: number;
  fullName: string;
  email: string;
  college?: string | null;
  branch?: string | null;
  graduationYear?: number | null;
  cgpa?: number | null;
}

/** Shape of GET /api/students/{studentId}/skills (backend StudentSkillResponse). */
export interface StudentSkill {
  id: number;
  studentId: number;
  skill: {
    id: number;
    name: string;
    category: string;
  };
  proficiency: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  yearsOfExperience: number | null;
}
