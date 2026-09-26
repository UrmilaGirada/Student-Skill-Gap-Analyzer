import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { studentApi } from '../api/studentApi';
import { jobDescriptionApi } from '../api/jobDescriptionApi';
import { analysisApi } from '../api/analysisApi';
import { parseApiError } from '../api/client';
import { StudentProfile } from '../types/student';
import { JobDescriptionResponse } from '../types/jobDescription';
import { SkillGapRoadmapResponse } from '../types/roadmap';
import { RoadmapView } from '../components/roadmap/RoadmapView';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { EmptyState } from '../components/common/EmptyState';
import { workflowStorage } from '../utils/workflowStorage';

export const RoadmapPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const paramStudentId = searchParams.get('studentId');
  const paramJobId = searchParams.get('jobDescriptionId') || searchParams.get('jobId');

  const [students, setStudents] = useState<StudentProfile[]>([]);
  const [studentsLoaded, setStudentsLoaded] = useState(false);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(
    paramStudentId ? parseInt(paramStudentId, 10) : workflowStorage.loadStudentId()
  );

  const [jobs, setJobs] = useState<JobDescriptionResponse[]>([]);
  const [jobsLoaded, setJobsLoaded] = useState(false);
  const [selectedJobId, setSelectedJobId] = useState<number | null>(
    paramJobId ? parseInt(paramJobId, 10) : workflowStorage.loadJobDescriptionId()
  );

  const [roadmapData, setRoadmapData] = useState<SkillGapRoadmapResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<{ status: number; message: string } | null>(null);
  useEffect(() => {
    studentApi.getAllStudents()
      .then((data) => {
        setStudents(data);
        if (!selectedStudentId && data.length > 0) {
          setSelectedStudentId(data[0].id!);
          workflowStorage.saveStudentId(data[0].id!);
        }
      })
      .catch((err) => setError(parseApiError(err)))
      .finally(() => setStudentsLoaded(true));
  }, []);

  useEffect(() => {
    if (!selectedStudentId) {
      setJobs([]);
      setJobsLoaded(false);
      return;
    }
    setJobsLoaded(false);
    jobDescriptionApi.findJobDescriptionsOfStudent(selectedStudentId)
      .then((data) => {
        setJobs(data);
        setJobsLoaded(true);
        const stillValid = data.find((j) => j.id === selectedJobId);
        if (!stillValid) {
          if (data.length > 0) {
            setSelectedJobId(data[0].id);
            workflowStorage.saveJobDescriptionId(data[0].id);
          } else {
            setSelectedJobId(null);
            setRoadmapData(null);
          }
        }
      })
      .catch((err) => setError(parseApiError(err)));
  }, [selectedStudentId]);

  const fetchRoadmap = async () => {
    if (!selectedStudentId || !selectedJobId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await analysisApi.findRoadmap(selectedStudentId, selectedJobId);
      setRoadmapData(data);
      setSearchParams({
        studentId: String(selectedStudentId),
        jobDescriptionId: String(selectedJobId),
      });
      workflowStorage.saveStudentId(selectedStudentId);
      workflowStorage.saveJobDescriptionId(selectedJobId);
    } catch (err) {
      setError(parseApiError(err));
      setRoadmapData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedStudentId && selectedJobId) {
      fetchRoadmap();
    }
  }, [selectedStudentId, selectedJobId]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Personalized Learning Roadmap</h1>
          <p className="text-sm text-slate-500">Deterministic step-by-step topic recommendations</p>
        </div>
        {selectedStudentId && selectedJobId && (
          <button
            onClick={() =>
              navigate(`/analysis?studentId=${selectedStudentId}&jobDescriptionId=${selectedJobId}`)
            }
            className="px-4 py-2 border border-slate-300 rounded-md text-sm font-medium text-slate-700 bg-white hover:bg-slate-50"
          >
            &larr; View Gap Breakdown
          </button>
        )}
      </div>

      <div className="bg-white p-4 rounded-lg border border-slate-200 grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label htmlFor="roadmap-student" className="block text-xs font-semibold text-slate-600 mb-1">Select Student:</label>
          <select
            id="roadmap-student"
            value={selectedStudentId || ''}
            onChange={(e) => setSelectedStudentId(Number(e.target.value))}
            className="w-full rounded border border-slate-300 px-3 py-1.5 text-sm bg-white"
          >
            {students.map((s) => (
              <option key={s.id} value={s.id}>{s.fullName} (#{s.id})</option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="roadmap-job" className="block text-xs font-semibold text-slate-600 mb-1">Select Target Job Description:</label>
          {jobs.length === 0 ? (
            <div className="text-xs text-slate-500 pt-2">No jobs available for this student.</div>
          ) : (
            <select
              id="roadmap-job"
              value={selectedJobId || ''}
              onChange={(e) => setSelectedJobId(Number(e.target.value))}
              className="w-full rounded border border-slate-300 px-3 py-1.5 text-sm bg-white"
            >
              {jobs.map((j) => (
                <option key={j.id} value={j.id}>{j.title} ({j.companyName})</option>
              ))}
            </select>
          )}
        </div>
      </div>

      {error && <ErrorAlert status={error.status} message={error.message} onRetry={fetchRoadmap} />}

      {loading ? (
        <LoadingSpinner message="Generating learning roadmap..." />
      ) : studentsLoaded && students.length === 0 ? (
        <EmptyState
          title="No Student Profile Found"
          message="No student profile found. Create your profile to continue."
          actionLabel="Create Profile"
          onAction={() => navigate('/students')}
        />
      ) : jobsLoaded && jobs.length === 0 ? (
        <EmptyState
          title="No Target Job Found"
          message="No target job found. Add a job description to analyze your skill gap."
          actionLabel="Add Target Job"
          onAction={() => navigate(`/jobs?studentId=${selectedStudentId ?? ''}`)}
        />
      ) : !selectedStudentId || !selectedJobId ? (
        <EmptyState
          title="Selection Required"
          message="Please choose both a student and a target job to generate the roadmap."
        />
      ) : roadmapData ? (
        <RoadmapView roadmapData={roadmapData} />
      ) : null}
    </div>
  );
};


