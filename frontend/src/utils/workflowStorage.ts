/**
 * Persists the non-sensitive workflow context (Phase 8C) so returning users
 * land back on their previously selected student / job description.
 *
 * Only numeric IDs are stored - never credentials, tokens or profile data.
 */
const STUDENT_ID_KEY = 'ssga.selectedStudentId';
const JOB_DESCRIPTION_ID_KEY = 'ssga.selectedJobDescriptionId';

function saveId(key: string, id: number): void {
  try {
    localStorage.setItem(key, String(id));
  } catch {
    /* storage unavailable (private mode) - workflow still works via URL params */
  }
}

function loadId(key: string): number | null {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    const parsed = parseInt(raw, 10);
    return Number.isNaN(parsed) ? null : parsed;
  } catch {
    return null;
  }
}

export const workflowStorage = {
  saveStudentId(id: number): void {
    saveId(STUDENT_ID_KEY, id);
  },
  saveJobDescriptionId(id: number): void {
    saveId(JOB_DESCRIPTION_ID_KEY, id);
  },
  loadStudentId(): number | null {
    return loadId(STUDENT_ID_KEY);
  },
  loadJobDescriptionId(): number | null {
    return loadId(JOB_DESCRIPTION_ID_KEY);
  },
};
