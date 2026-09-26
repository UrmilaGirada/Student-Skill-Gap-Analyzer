export interface JobDescriptionRequest {
  title: string;
  companyName: string;
  descriptionText: string;
}

export interface JobDescriptionResponse {
  id: number;
  studentId: number;
  title: string;
  companyName: string;
  descriptionText: string;
  requiredSkills: string[];
  createdAt: string;
  updatedAt: string;
}
