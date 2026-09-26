import React, { useState } from 'react';
import { JobDescriptionRequest } from '../../types/jobDescription';

interface JobDescriptionFormProps {
  studentId: number;
  onSubmit: (request: JobDescriptionRequest) => Promise<void>;
  isLoading: boolean;
}

export const JobDescriptionForm: React.FC<JobDescriptionFormProps> = ({
  onSubmit,
  isLoading,
}) => {
  const [title, setTitle] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [descriptionText, setDescriptionText] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !companyName.trim() || !descriptionText.trim()) {
      setError('Title, Company Name, and Job Description text are all required.');
      return;
    }
    setError(null);
    await onSubmit({
      title: title.trim(),
      companyName: companyName.trim(),
      descriptionText: descriptionText.trim(),
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 bg-white p-6 rounded-lg border border-slate-200">
      {error && <div className="p-3 bg-red-50 text-red-700 text-sm rounded">{error}</div>}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label htmlFor="jd-title" className="block text-sm font-medium text-slate-700">Job Title *</label>
          <input
            id="jd-title"
            type="text"
            required
            maxLength={200}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="e.g. Backend Software Engineer"
          />
        </div>
        <div>
          <label htmlFor="jd-company" className="block text-sm font-medium text-slate-700">Company Name *</label>
          <input
            id="jd-company"
            type="text"
            required
            maxLength={200}
            value={companyName}
            onChange={(e) => setCompanyName(e.target.value)}
            className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
            placeholder="e.g. Acme Tech Corp"
          />
        </div>
      </div>
      <div>
        <label htmlFor="jd-text" className="block text-sm font-medium text-slate-700">
          Job Description Text * (Skills will be extracted via AI engine)
        </label>
        <textarea
          id="jd-text"
          required
          rows={7}
          maxLength={20000}
          value={descriptionText}
          onChange={(e) => setDescriptionText(e.target.value)}
          className="mt-1 block w-full rounded border border-slate-300 p-2 text-sm"
          placeholder="Looking for a Java developer with Spring Boot, REST APIs, MySQL, Git and AWS."
        />
        <p className="mt-1 text-xs text-slate-500 text-right">{descriptionText.length} / 20000 chars</p>
      </div>
      <div className="flex justify-end pt-2">
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-primary-600 text-white rounded text-sm font-medium hover:bg-primary-700 disabled:opacity-50"
        >
          {isLoading ? 'Extracting Skills & Saving...' : 'Save & Extract Skills'}
        </button>
      </div>
    </form>
  );
};
