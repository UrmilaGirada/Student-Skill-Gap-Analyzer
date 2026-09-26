import React from 'react';
import { SkillGapRoadmapResponse } from '../../types/roadmap';
import { RoadmapStepItem } from './RoadmapStepItem';
import { PriorityBadge } from './PriorityBadge';
import { MatchScoreBadge } from '../analysis/MatchScoreBadge';
import { SkillPillList } from '../analysis/SkillPillList';

interface RoadmapViewProps {
  roadmapData: SkillGapRoadmapResponse;
}

export const RoadmapView: React.FC<RoadmapViewProps> = ({ roadmapData }) => {
  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border border-slate-200 p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Personalized Learning Roadmap
            </span>
            <h3 className="text-xl font-bold text-slate-900">{roadmapData.jobTitle}</h3>
            <p className="text-sm font-medium text-slate-600">{roadmapData.companyName}</p>
          </div>
          <MatchScoreBadge score={roadmapData.matchPercentage} size="lg" />
        </div>

        <div className="mt-6 pt-4 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
          <SkillPillList
            title="Current Skills"
            skills={roadmapData.currentSkills}
            variant="current"
            emptyMessage="No skills recorded for student."
          />
          <SkillPillList
            title="Matched Skills"
            skills={roadmapData.matchedSkills}
            variant="matched"
            emptyMessage="No fully matched skills."
          />
          <SkillPillList
            title="Partially Matched Skills"
            skills={roadmapData.partiallyMatchedSkills}
            variant="partial"
            emptyMessage="No partially matched skills."
          />
          <SkillPillList
            title="Missing Skills"
            skills={roadmapData.missingSkills}
            variant="missing"
            emptyMessage="No missing skills! Fully qualified."
          />
        </div>
      </div>

      {/* Compact recommendations summary - full detail lives in the timeline below */}
      <div className="bg-white rounded-lg border border-slate-200 p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-base font-bold text-slate-900">
            Recommendations ({roadmapData.recommendations.length})
          </h3>
          <span className="text-xs text-slate-500">
            Skills to work on, ordered by priority
          </span>
        </div>
        {roadmapData.recommendations.length === 0 ? (
          <p className="text-sm text-slate-500">
            No recommendations - all required skills are already covered.
          </p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {roadmapData.recommendations.map((rec) => (
              <span
                key={rec.skill}
                className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-slate-200 bg-slate-50 text-sm font-medium text-slate-800"
              >
                {rec.skill}
                <PriorityBadge priority={rec.priority} />
              </span>
            ))}
          </div>
        )}
      </div>

      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-bold text-slate-900">
            Step-by-Step Learning Plan ({roadmapData.roadmap.length} steps)
          </h3>
          <span className="text-xs text-slate-500">
            Prioritized by impact (HIGH first, then MEDIUM)
          </span>
        </div>

        {roadmapData.roadmap.length === 0 ? (
          <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-6 text-center text-emerald-800">
            <h4 className="font-semibold">Congratulations! No Gap Detected</h4>
            <p className="text-sm mt-1">
              You already hold all the required skills for this position. No roadmap steps needed.
            </p>
          </div>
        ) : (
          <div className="space-y-2 mt-4">
            {roadmapData.roadmap.map((step) => (
              <RoadmapStepItem key={`${step.order}-${step.skill}`} step={step} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
