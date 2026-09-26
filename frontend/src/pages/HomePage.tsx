import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { studentApi } from '../api/studentApi';
import { StudentProfile } from '../types/student';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const [students, setStudents] = useState<StudentProfile[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    studentApi
      .getAllStudents()
      .then((data) => setStudents(data))
      .catch(() => setStudents([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-10">
      <section className="bg-gradient-to-r from-primary-800 to-slate-900 rounded-2xl p-8 sm:p-10 text-white shadow-lg">
        <div className="max-w-2xl">
          <span className="inline-block px-3 py-1 bg-primary-700/60 rounded-full text-xs font-semibold uppercase tracking-wider text-primary-200 mb-3">
            Placement Readiness
          </span>
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight">
            Student Skill Gap Analyzer
          </h1>
          <p className="mt-3 text-sm sm:text-base text-slate-300">
            Compare your skills with a target job and get a personalized learning roadmap.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              onClick={() => navigate('/students')}
              className="px-5 py-2.5 bg-white text-primary-800 rounded-md font-semibold text-sm hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-white"
            >
              Start Skill Analysis
            </button>
            <button
              onClick={() =>
                document
                  .getElementById('dashboard')
                  ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
              }
              className="px-5 py-2.5 bg-primary-600 rounded-md font-semibold text-sm hover:bg-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-400"
            >
              View Dashboard
            </button>
          </div>
        </div>
      </section>

      <section>
        <h2 className="text-xl font-bold text-slate-900 mb-1">Simple Placement Workflow</h2>
        <p className="text-xs text-slate-500 mb-4">
          Profile &rarr; Target Job &rarr; Skill Gap &rarr; Roadmap
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          <Link
            to="/students"
            className="bg-white p-4 rounded-lg border border-slate-200 hover:border-primary-300 hover:shadow-sm transition block"
          >
            <div className="font-bold text-primary-600 text-lg mb-1">1. Create Profile</div>
            <p className="text-xs text-slate-600">
              Register student details and curriculum records.
            </p>
          </Link>
          <Link
            to="/jobs"
            className="bg-white p-4 rounded-lg border border-slate-200 hover:border-primary-300 hover:shadow-sm transition block"
          >
            <div className="font-bold text-primary-600 text-lg mb-1">2. Add Target Job</div>
            <p className="text-xs text-slate-600">
              Save a target job description and let the backend extract required skills.
            </p>
          </Link>
          <Link
            to="/analysis"
            className="bg-white p-4 rounded-lg border border-slate-200 hover:border-primary-300 hover:shadow-sm transition block"
          >
            <div className="font-bold text-primary-600 text-lg mb-1">3. Analyze Skill Gap</div>
            <p className="text-xs text-slate-600">
              Compute match percentage and identify missing skill tags.
            </p>
          </Link>
          <Link
            to="/roadmap"
            className="bg-white p-4 rounded-lg border border-slate-200 hover:border-primary-300 hover:shadow-sm transition block"
          >
            <div className="font-bold text-primary-600 text-lg mb-1">4. Follow Roadmap</div>
            <p className="text-xs text-slate-600">
              Receive prioritized learning steps (HIGH &amp; MEDIUM).
            </p>
          </Link>
        </div>
      </section>

      <section id="dashboard" className="bg-white p-6 rounded-lg border border-slate-200">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-bold text-slate-900">Registered Students</h2>
          <Link to="/students" className="text-xs font-semibold text-primary-600 hover:underline">
            View All &rarr;
          </Link>
        </div>
        {loading ? (
          <LoadingSpinner message="Checking profiles..." size="sm" />
        ) : students.length === 0 ? (
          <p className="text-xs text-slate-500">No students registered yet. Click &apos;Create Student Profile&apos; above.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {students.slice(0, 6).map((s) => (
              <div key={s.id} className="p-3 rounded border border-slate-100 bg-slate-50 text-xs">
                <div className="font-semibold text-slate-800">{s.fullName} (#{s.id})</div>
                <div className="text-slate-500 truncate">{s.email}</div>
                <Link to={`/jobs?studentId=${s.id}`} className="mt-2 inline-block font-medium text-primary-600">
                  Target Jobs &rarr;
                </Link>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
