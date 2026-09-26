import React from 'react';
import { SkillGapAnalysisResponse } from '../../types/analysis';
import { MatchScoreBadge } from './MatchScoreBadge';
import { SkillPillList } from './SkillPillList';

interface SkillGapSummaryProps {
  analysis: SkillGapAnalysisResponse;
  /** Names of the skills currently stored for the student (GET /api/students/{id}/skills). */
  currentSkills?: string[];
  /** Optional CTA shown below the score explanation. */
  onViewRoadmap?: () => void;
}

export const SkillGapSummary: React.FC<SkillGapSummaryProps> = ({
  analysis,
  currentSkills = [],
  onViewRoadmap,
}) => {
  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border border-slate-200 p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Skill Gap Analysis
            </span>
            <h3 className="text-xl font-bold text-slate-900">{analysis.jobTitle}</h3>
            <p className="text-sm font-medium text-slate-600">{analysis.companyName}</p>
          </div>
          <div className="flex flex-col items-start sm:items-end">
            <MatchScoreBadge score={analysis.matchPercentage} size="lg" />
            <span className="text-xs text-slate-500 mt-1">
              Required: {analysis.totalRequiredSkills} skill(s)
            </span>
          </div>
        </div>

        <p className="mt-4 text-sm text-slate-500">
          Your match percentage is calculated from the skills required by the selected job
          description.
        </p>
        {onViewRoadmap && (
          <div className="mt-4">
            <button
              type="button"
              onClick={onViewRoadmap}
              className="inline-flex items-center px-4 py-2 bg-indigo-600 text-white rounded-md text-sm font-medium hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              View Personalized Roadmap &rarr;
            </button>
          </div>
        )}

        {/* Progress bar breakdown */}
        <div className="mt-6">
          <div className="h-3 w-full bg-slate-100 rounded-full overflow-hidden flex">
            {analysis.totalRequiredSkills > 0 && (
              <>
                <div
                  style={{
                    width: `${(analysis.matchedCount / analysis.totalRequiredSkills) * 100}%`,
                  }}
                  className="bg-emerald-500 transition-all duration-500"
                  title={`Matched: ${analysis.matchedCount}`}
                />
                <div
                  style={{
                    width: `${
                      (analysis.partiallyMatchedCount / analysis.totalRequiredSkills) * 100
                    }%`,
                  }}
                  className="bg-amber-400 transition-all duration-500"
                  title={`Partially Matched: ${analysis.partiallyMatchedCount}`}
                />
                <div
                  style={{
                    width: `${(analysis.missingCount / analysis.totalRequiredSkills) * 100}%`,
                  }}
                  className="bg-rose-400 transition-all duration-500"
                  title={`Missing: ${analysis.missingCount}`}
                />
              </>
            )}
          </div>
          <div className="flex justify-between items-center text-xs text-slate-500 mt-2">
            <span className="flex items-center">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 inline-block mr-1" />
              Matched: {analysis.matchedCount}
            </span>
            <span className="flex items-center">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-400 inline-block mr-1" />
              Partially Matched: {analysis.partiallyMatchedCount}
            </span>
            <span className="flex items-center">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-400 inline-block mr-1" />
              Missing: {analysis.missingCount}
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <SkillPillList
          title="Current Skills"
          skills={currentSkills}
          variant="current"
          emptyMessage="No skills recorded for this student yet."
        />
        <SkillPillList
          title="Matched Skills"
          skills={analysis.matchedSkills}
          variant="matched"
          emptyMessage="No fully matched skills."
        />
        <SkillPillList
          title="Partially Matched Skills"
          skills={analysis.partiallyMatchedSkills}
          variant="partial"
          emptyMessage="No partially matched skills."
        />
        <SkillPillList
          title="Missing Skills"
          skills={analysis.missingSkills}
          variant="missing"
          emptyMessage="No missing skills! Fully qualified."
        />
      </div>
    </div>
  );
};
