import React from 'react';
import { RoadmapStep } from '../../types/roadmap';
import { PriorityBadge } from './PriorityBadge';

interface RoadmapStepItemProps {
  step: RoadmapStep;
}

export const RoadmapStepItem: React.FC<RoadmapStepItemProps> = ({ step }) => {
  return (
    <div className="relative pl-10 pb-8 last:pb-0">
      {/* Step line indicator */}
      <div className="absolute left-3.5 top-8 bottom-0 w-0.5 bg-slate-200 last:hidden" />

      {/* Step order badge */}
      <div className="absolute left-0 top-0 flex items-center justify-center w-7 h-7 rounded-full bg-primary-600 text-white font-bold text-xs ring-4 ring-white shadow">
        {step.order}
      </div>

      <div className="bg-white rounded-lg border border-slate-200 p-5 shadow-sm hover:border-slate-300 transition">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <div className="flex items-center space-x-2">
            <h4 className="text-base font-semibold text-slate-900">{step.skill}</h4>
            <PriorityBadge priority={step.priority} />
          </div>
          <span className="text-xs text-slate-400">Step {step.order}</span>
        </div>

        <p className="mt-2 text-sm text-slate-600">{step.reason}</p>

        {step.suggestedTopics && step.suggestedTopics.length > 0 && (
          <div className="mt-4 pt-3 border-t border-slate-100">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block mb-2">
              Suggested Topics to Study:
            </span>
            <ul className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
              {step.suggestedTopics.map((topic, idx) => (
                <li key={idx} className="flex items-start text-xs text-slate-700">
                  <span className="text-primary-500 mr-2 font-bold">•</span>
                  <span>{topic}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};
