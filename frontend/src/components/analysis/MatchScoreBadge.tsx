import React from 'react';

interface MatchScoreBadgeProps {
  score: number;
  size?: 'sm' | 'md' | 'lg';
}

export const MatchScoreBadge: React.FC<MatchScoreBadgeProps> = ({ score, size = 'md' }) => {
  const percentage = Math.round(score * 10) / 10;

  const getColorClasses = (val: number) => {
    if (val >= 75) {
      return 'bg-emerald-50 text-emerald-700 border-emerald-300';
    }
    if (val >= 40) {
      return 'bg-amber-50 text-amber-700 border-amber-300';
    }
    return 'bg-rose-50 text-rose-700 border-rose-300';
  };

  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-sm px-3 py-1',
    lg: 'text-2xl px-4 py-2 font-bold',
  };

  return (
    <div
      className={`inline-flex items-center justify-center font-semibold rounded-full border ${getColorClasses(
        percentage
      )} ${sizeClasses[size]}`}
    >
      {percentage}% Match
    </div>
  );
};
