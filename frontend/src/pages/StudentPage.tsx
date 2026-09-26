import React, { useEffect, useState } from 'react';
import { studentApi } from '../api/studentApi';
import { parseApiError } from '../api/client';
import { StudentProfile } from '../types/student';
import { StudentForm } from '../components/student/StudentForm';
import { StudentCard } from '../components/student/StudentCard';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ErrorAlert } from '../components/common/ErrorAlert';
import { EmptyState } from '../components/common/EmptyState';
import { useNavigate } from 'react-router-dom';
import { workflowStorage } from '../utils/workflowStorage';

export const StudentPage: React.FC = () => {
  const navigate = useNavigate();
  const [students, setStudents] = useState<StudentProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<{ status: number; message: string } | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [createdStudent, setCreatedStudent] = useState<StudentProfile | null>(null);

  const fetchStudents = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await studentApi.getAllStudents();
      setStudents(data);
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStudents();
  }, []);

  const handleCreate = async (studentData: Omit<StudentProfile, 'id'>) => {
    setSubmitting(true);
    setError(null);
    try {
      const created = await studentApi.createStudent(studentData);
      setStudents((prev) => [...prev, created]);
      setShowForm(false);
      setCreatedStudent(created);
      if (created.id) {
        workflowStorage.saveStudentId(created.id);
      }
    } catch (err) {
      setError(parseApiError(err));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm(`Are you sure you want to delete student profile #${id}?`)) {
      return;
    }
    setError(null);
    try {
      await studentApi.deleteStudent(id);
      setStudents((prev) => prev.filter((s) => s.id !== id));
    } catch (err) {
      setError(parseApiError(err));
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Student Profiles</h1>
          <p className="text-sm text-slate-500">Manage registered students and their academic records</p>
        </div>
        <button
          onClick={() => setShowForm((prev) => !prev)}
          className="px-4 py-2 bg-primary-600 text-white rounded-md text-sm font-medium hover:bg-primary-700"
        >
          {showForm ? 'Close Form' : '+ New Student Profile'}
        </button>
      </div>

      {error && (
        <ErrorAlert
          status={error.status}
          message={error.message}
          onRetry={fetchStudents}
        />
      )}

      {createdStudent && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4" role="status">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-emerald-900">
                Profile created for {createdStudent.fullName} (ID {createdStudent.id})
              </h2>
              <p className="mt-0.5 text-sm text-emerald-700">
                Your profile is saved. Continue to add a target job description.
              </p>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => navigate(`/jobs?studentId=${createdStudent.id}`)}
                className="px-4 py-2 bg-primary-600 text-white rounded-md text-sm font-medium hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                Continue to Job Description &rarr;
              </button>
              <button
                type="button"
                onClick={() => setCreatedStudent(null)}
                className="px-3 py-2 text-sm font-medium text-emerald-800 hover:bg-emerald-100 rounded-md"
              >
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}

      {showForm && (
        <div className="border-t pt-4">
          <h2 className="text-base font-semibold text-slate-800 mb-3">Register New Student</h2>
          <StudentForm onSubmit={handleCreate} isLoading={submitting} />
        </div>
      )}

      {loading ? (
        <LoadingSpinner message="Fetching student profiles..." />
      ) : students.length === 0 ? (
        <EmptyState
          title="No Student Profiles Found"
          message="No student profile found. Create your profile to continue."
          actionLabel="Create First Profile"
          onAction={() => setShowForm(true)}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {students.map((student) => (
            <StudentCard
              key={student.id}
              student={student}
              onSelect={(s) => navigate(`/jobs?studentId=${s.id}`)}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
};
