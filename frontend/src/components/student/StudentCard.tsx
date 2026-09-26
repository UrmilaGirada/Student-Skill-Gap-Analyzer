import React from 'react';
import { StudentProfile } from '../../types/student';

interface StudentCardProps {
  student: StudentProfile;
  isSelected?: boolean;
  onSelect?: (student: StudentProfile) => void;
  onDelete?: (id: number) => void;
}

export const StudentCard: React.FC<StudentCardProps> = ({
  student,
  isSelected = false,
  onSelect,
  onDelete,
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
          <h4 className="text-base font-semibold text-slate-900">{student.fullName}</h4>
          <p className="text-sm text-slate-500">{student.email}</p>
        </div>
        <span className="inline-flex items-center rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
          ID: {student.id}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2 text-xs text-slate-600 border-t border-slate-100 pt-3">
        <div>
          <span className="text-slate-400 block">College</span>
          <span className="font-medium text-slate-700 truncate block">
            {student.college || '—'}
          </span>
        </div>
        <div>
          <span className="text-slate-400 block">Branch</span>
          <span className="font-medium text-slate-700 truncate block">
            {student.branch || '—'}
          </span>
        </div>
        <div>
          <span className="text-slate-400 block">Graduation</span>
          <span className="font-medium text-slate-700">
            {student.graduationYear || '—'}
          </span>
        </div>
        <div>
          <span className="text-slate-400 block">CGPA</span>
          <span className="font-medium text-slate-700">
            {student.cgpa !== null && student.cgpa !== undefined ? student.cgpa : '—'}
          </span>
        </div>
      </div>

      <div className="mt-4 flex items-center justify-end space-x-2 pt-2 border-t border-slate-100">
        {onSelect && (
          <button
            type="button"
            onClick={() => onSelect(student)}
            className={`text-xs px-3 py-1.5 rounded font-medium transition ${
              isSelected
                ? 'bg-primary-600 text-white'
                : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
            }`}
          >
            {isSelected ? 'Selected' : 'Select'}
          </button>
        )}
        {onDelete && student.id && (
          <button
            type="button"
            onClick={() => onDelete(student.id!)}
            className="text-xs px-2 py-1.5 text-rose-600 hover:bg-rose-50 rounded font-medium transition"
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
};
