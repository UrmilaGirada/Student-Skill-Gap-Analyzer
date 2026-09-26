import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-slate-200 py-6 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-2">
        <p>Student Skill Gap Analyzer &copy; {new Date().getFullYear()} — Career &amp; Placement Preparation Platform</p>
        <p>Built with React, TypeScript, Tailwind CSS &amp; Spring Boot</p>
      </div>
    </footer>
  );
};
