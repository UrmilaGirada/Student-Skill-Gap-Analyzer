import React from 'react';

interface SkillPillListProps {
  title: string;
  skills: string[];
  variant: 'matched' | 'partial' | 'missing' | 'current';
  emptyMessage?: string;
}

export const SkillPillList: React.FC<SkillPillListProps> = ({
  title,
  skills,
  variant,
  emptyMessage = 'None',
}) => {
  const variantStyles = {
    matched: {
      badge: 'bg-emerald-100 text-emerald-800 border-emerald-300',
      heading: 'text-emerald-900',
      countBg: 'bg-emerald-200 text-emerald-800',
    },
    partial: {
      badge: 'bg-amber-100 text-amber-800 border-amber-300',
      heading: 'text-amber-900',
      countBg: 'bg-amber-200 text-amber-800',
    },
    missing: {
      badge: 'bg-rose-100 text-rose-800 border-rose-300',
      heading: 'text-rose-900',
      countBg: 'bg-rose-200 text-rose-800',
    },
    current: {
      badge: 'bg-slate-100 text-slate-800 border-slate-300',
      heading: 'text-slate-900',
      countBg: 'bg-slate-200 text-slate-800',
    },
  };

  const styles = variantStyles[variant];

  return (
    <div className="bg-white rounded-lg border border-slate-200 p-4">
      <div className="flex items-center justify-between mb-3">
        <h4 className={`text-sm font-semibold ${styles.heading}`}>{title}</h4>
        <span className={`text-xs px-2 py-0.5 rounded-full font-bold ${styles.countBg}`}>
          {skills.length}
        </span>
      </div>
      {skills.length === 0 ? (
        <p className="text-xs text-slate-400 italic">{emptyMessage}</p>
      ) : (
        <div className="flex flex-wrap gap-1.5">
          {skills.map((skill) => (
            <span
              key={skill}
              className={`inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium border ${styles.badge}`}
            >
              {skill}
            </span>
          ))}
        </div>
      )}
    </div>
  );
};
