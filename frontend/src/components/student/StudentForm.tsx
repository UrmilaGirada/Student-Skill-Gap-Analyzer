import React, { useState } from 'react';
import { StudentProfile } from '../../types/student';

interface StudentFormProps {
  onSubmit: (student: Omit<StudentProfile, 'id'>) => Promise<void>;
  isLoading: boolean;
  submitLabel?: string;
}

export const StudentForm: React.FC<StudentFormProps> = ({
  onSubmit,
  isLoading,
  submitLabel = 'Create Profile',
}) => {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [college, setCollege] = useState('');
  const [branch, setBranch] = useState('');
  const [graduationYear, setGraduationYear] = useState('');
  const [cgpa, setCgpa] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim() || !email.trim()) {
      setError('Full Name and Email are required.');
      return;
    }
    if (graduationYear) {
      const year = parseInt(graduationYear, 10);
      if (Number.isNaN(year) || year < 2000 || year > 2100) {
        setError('Graduation year must be a number between 2000 and 2100.');
        return;
      }
    }
    if (cgpa) {
      const value = parseFloat(cgpa);
      if (Number.isNaN(value) || value < 0 || value > 10) {
        setError('CGPA must be a number between 0 and 10.');
        return;
      }
    }
    setError(null);
    await onSubmit({
      fullName: fullName.trim(),
      email: email.trim(),
      college: college.trim() || null,
      branch: branch.trim() || null,
      graduationYear: graduationYear ? parseInt(graduationYear, 10) : null,
      cgpa: cgpa ? parseFloat(cgpa) : null,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 bg-white p-6 rounded-lg border border-slate-200">
      {error && <div className="p-3 bg-red-50 text-red-700 text-sm rounded">{error}</div>}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label htmlFor="fn" className="block text-sm font-medium text-slate-700">Full Name *</label>
          <input
            id="fn"
            type="text"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="John Doe"
          />
        </div>
        <div>
          <label htmlFor="em" className="block text-sm font-medium text-slate-700">Email *</label>
          <input
            id="em"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="john@example.com"
          />
        </div>
        <div>
          <label htmlFor="clg" className="block text-sm font-medium text-slate-700">College</label>
          <input
            id="clg"
            type="text"
            value={college}
            onChange={(e) => setCollege(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="Engineering College"
          />
        </div>
        <div>
          <label htmlFor="br" className="block text-sm font-medium text-slate-700">Branch</label>
          <input
            id="br"
            type="text"
            value={branch}
            onChange={(e) => setBranch(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="Computer Science"
          />
        </div>
        <div>
          <label htmlFor="gy" className="block text-sm font-medium text-slate-700">Graduation Year</label>
          <input
            id="gy"
            type="number"
            min={2000}
            max={2100}
            value={graduationYear}
            onChange={(e) => setGraduationYear(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="2026"
          />
        </div>
        <div>
          <label htmlFor="cg" className="block text-sm font-medium text-slate-700">CGPA</label>
          <input
            id="cg"
            type="number"
            step="0.01"
            min={0}
            max={10}
            value={cgpa}
            onChange={(e) => setCgpa(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="8.5"
          />
        </div>
      </div>
      <div className="flex justify-end pt-2">
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-primary-600 text-white rounded text-sm font-medium hover:bg-primary-700 disabled:opacity-50"
        >
          {isLoading ? 'Saving...' : submitLabel}
        </button>
      </div>
    </form>
  );
};
