import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { studentApi } from '../api/studentApi';
import { jobDescriptionApi } from '../api/jobDescriptionApi';
import { parseApiError } from '../api/client';
import { StudentProfile } from '../types/student';
import { JobDescriptionRequest, JobDescriptionResponse } from '../types/jobDescription';
import { JobDescriptionForm } from '../components/job/JobDescriptionForm';
import { JobDescriptionCard } from '../components/job/JobDescriptionCard';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { EmptyState } from '../components/common/EmptyState';
import { workflowStorage } from '../utils/workflowStorage';

export const JobDescriptionPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const [students, setStudents] = useState<StudentProfile[]>([]);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(
    searchParams.get('studentId')
      ? parseInt(searchParams.get('studentId')!, 10)
      : workflowStorage.loadStudentId()
  );

  const [jobs, setJobs] = useState<JobDescriptionResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<{ status: number; message: string } | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [createdJob, setCreatedJob] = useState<JobDescriptionResponse | null>(null);
  useEffect(() => {
    studentApi.getAllStudents()
      .then((data) => {
        setStudents(data);
        if (!selectedStudentId && data.length > 0) {
          setSelectedStudentId(data[0].id!);
          setSearchParams({ studentId: String(data[0].id) });
        }
      })
      .catch((err) => setError(parseApiError(err)));
  }, []);

  const fetchJobs = async () => {
    if (!selectedStudentId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await jobDescriptionApi.findJobDescriptionsOfStudent(selectedStudentId);
      setJobs(data);
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, [selectedStudentId]);

  const handleStudentChange = (id: number) => {
    setSelectedStudentId(id);
    setSearchParams({ studentId: String(id) });
    workflowStorage.saveStudentId(id);
  };

  const handleCreateJob = async (request: JobDescriptionRequest) => {
    if (!selectedStudentId) return;
    setSubmitting(true);
    setError(null);
    try {
      const created = await jobDescriptionApi.createJobDescription(selectedStudentId, request);
      setJobs((prev) => [created, ...prev]);
      setShowForm(false);
      setCreatedJob(created);
      workflowStorage.saveJobDescriptionId(created.id);
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteJob = async (jobId: number) => {
    if (!selectedStudentId) return;
    if (!window.confirm('Delete job description #' + jobId + '?')) return;
    setError(null);
    try {
      await jobDescriptionApi.deleteJobDescription(selectedStudentId, jobId);
      setJobs((prev) => prev.filter((j) => j.id !== jobId));
    } catch (err) {
      setError(parseApiError(err));
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Job Descriptions</h1>
          <p className="text-sm text-slate-500">Target openings & AI skill extraction</p>
        </div>
        {selectedStudentId && (
          <button onClick={() => setShowForm((prev) => !prev)} className="px-4 py-2 bg-primary-600 text-white rounded-md text-sm font-medium hover:bg-primary-700">
            {showForm ? 'Close Form' : '+ Add Job Description'}
          </button>
        )}
      </div>

      <div className="bg-white p-4 rounded-lg border border-slate-200 flex flex-wrap items-center gap-3">
        <label htmlFor="student-select" className="text-sm font-semibold text-slate-700">Target Student:</label>
        {students.length === 0 ? (
          <span className="text-sm text-slate-500">No students found. <button onClick={() => navigate('/students')} className="text-primary-600 underline font-medium">Create one first</button></span>
        ) : (
          <select id="student-select" value={selectedStudentId || ''} onChange={(e) => handleStudentChange(Number(e.target.value))} className="rounded border border-slate-300 px-3 py-1.5 text-sm bg-white">
            {students.map((s) => (<option key={s.id} value={s.id}>{s.fullName} (#{s.id})</option>))}
          </select>
        )}
      </div>

      {error && <ErrorAlert status={error.status} message={error.message} onRetry={fetchJobs} />}

      {createdJob && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4" role="status">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-emerald-900">
                Job description created: {createdJob.title} (Job #{createdJob.id})
              </h2>
              <p className="mt-0.5 text-sm text-emerald-700">
                {createdJob.requiredSkills.length > 0
                  ? `Extracted ${createdJob.requiredSkills.length} required skill(s): ${createdJob.requiredSkills.join(', ')}.`
                  : 'Required skills are being extracted by the backend.'}
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() =>
                  navigate(`/analysis?studentId=${createdJob.studentId}&jobDescriptionId=${createdJob.id}`)
                }
                className="px-4 py-2 bg-primary-600 text-white rounded-md text-sm font-medium hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                Analyze Skill Gap &rarr;
              </button>
              <button
                type="button"
                onClick={() => setCreatedJob(null)}
                className="px-3 py-2 text-sm font-medium text-emerald-800 hover:bg-emerald-100 rounded-md"
              >
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}

      {showForm && selectedStudentId && (
        <div className="border-t pt-4">
          <h2 className="text-base font-semibold text-slate-800 mb-3">Add Job Description</h2>
          <JobDescriptionForm studentId={selectedStudentId} onSubmit={handleCreateJob} isLoading={submitting} />
        </div>
      )}

      {loading ? (
        <LoadingSpinner message="Fetching job descriptions..." />
      ) : !selectedStudentId ? (
        <EmptyState title="No Student Selected" message="Please select or create a student profile first." />
      ) : jobs.length === 0 ? (
        <EmptyState
          title="No Target Job Found"
          message="No target job found. Add a job description to analyze your skill gap."
          actionLabel="Add First Job Posting"
          onAction={() => setShowForm(true)}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {jobs.map((job) => (
            <JobDescriptionCard
              key={job.id}
              job={job}
              onViewAnalysis={(jobId) =>
                navigate(`/analysis?studentId=${selectedStudentId}&jobDescriptionId=${jobId}`)
              }
              onViewRoadmap={(jobId) =>
                navigate(`/roadmap?studentId=${selectedStudentId}&jobDescriptionId=${jobId}`)
              }
              onDelete={handleDeleteJob}
            />
          ))}
        </div>
      )}
    </div>
  );
};


