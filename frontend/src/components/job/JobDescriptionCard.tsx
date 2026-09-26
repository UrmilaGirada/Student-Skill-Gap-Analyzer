import React from 'react';
import { JobDescriptionResponse } from '../../types/jobDescription';

interface JobDescriptionCardProps {
  job: JobDescriptionResponse;
  isSelected?: boolean;
  onSelect?: (job: JobDescriptionResponse) => void;
  onDelete?: (id: number) => void;
  onViewAnalysis?: (jobId: number) => void;
  onViewRoadmap?: (jobId: number) => void;
}

export const JobDescriptionCard: React.FC<JobDescriptionCardProps> = ({
  job,
  isSelected = false,
  onSelect,
  onDelete,
  onViewAnalysis,
  onViewRoadmap,
}) => {
  return (
    <div
      className={`rounded-lg border bg-white p-5 transition shadow-sm ${
        isSelected
          ? 'border-primary-500 ring-2 ring-primary-100'
          : 'border-slate-200 hover:border-slate-300'
      }`}
    >
      <div className="flex items-start justify-between">
        <div>
          <h4 className="text-base font-semibold text-slate-900">{job.title}</h4>
          <p className="text-sm font-medium text-primary-600">{job.companyName}</p>
        </div>
        <span className="inline-flex items-center rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
          Job #{job.id}
        </span>
      </div>

      <div className="mt-3">
        <span className="text-xs font-medium text-slate-500 uppercase tracking-wider block mb-1.5">
          Extracted Skills ({job.requiredSkills ? job.requiredSkills.length : 0})
        </span>
        <div className="flex flex-wrap gap-1.5">
          {job.requiredSkills && job.requiredSkills.length > 0 ? (
            job.requiredSkills.map((skill) => (
              <span
                key={skill}
                className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700"
              >
                {skill}
              </span>
            ))
          ) : (
            <span className="text-xs text-slate-400 italic">No skills extracted yet</span>
          )}
        </div>
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-slate-100 text-xs">
        <span className="text-slate-400">
          Created: {new Date(job.createdAt).toLocaleDateString()}
        </span>
        <div className="flex items-center space-x-2">
          {onSelect && (
            <button
              type="button"
              onClick={() => onSelect(job)}
              className="px-2.5 py-1 text-xs rounded border border-slate-300 text-slate-700 hover:bg-slate-50"
            >
              Select
            </button>
          )}
          {onViewAnalysis && (
            <button
              type="button"
              onClick={() => onViewAnalysis(job.id)}
              className="px-2.5 py-1 text-xs rounded bg-primary-50 text-primary-700 hover:bg-primary-100 font-medium"
            >
              Skill Gap
            </button>
          )}
          {onViewRoadmap && (
            <button
              type="button"
              onClick={() => onViewRoadmap(job.id)}
              className="px-2.5 py-1 text-xs rounded bg-indigo-50 text-indigo-700 hover:bg-indigo-100 font-medium"
            >
              Roadmap
            </button>
          )}
          {onDelete && (
            <button
              type="button"
              onClick={() => onDelete(job.id)}
              className="px-2 py-1 text-xs rounded text-rose-600 hover:bg-rose-50"
            >
              Delete
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
