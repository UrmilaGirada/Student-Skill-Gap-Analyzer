import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { healthApi } from '../../api/studentApi';

export const Navbar: React.FC = () => {
  const location = useLocation();
  const [backendStatus, setBackendStatus] = useState<'UP' | 'DOWN' | 'CHECKING'>('CHECKING');

  useEffect(() => {
    let mounted = true;
    healthApi
      .checkHealth()
      .then((res) => {
        if (mounted) {
          setBackendStatus(res.status === 'UP' ? 'UP' : 'DOWN');
        }
      })
      .catch(() => {
        if (mounted) {
          setBackendStatus('DOWN');
        }
      });

    return () => {
      mounted = false;
    };
  }, [location.pathname]);

  const navLinks = [
    { label: 'Dashboard', path: '/' },
    { label: 'Students', path: '/students' },
    { label: 'Job Descriptions', path: '/jobs' },
    { label: 'Analyze Skills', path: '/analysis' },
    { label: 'Roadmap', path: '/roadmap' },
  ];

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex">
            <Link to="/" className="flex items-center space-x-3">
              <div className="w-9 h-9 rounded-lg bg-primary-600 text-white flex items-center justify-center font-bold text-lg shadow-sm">
                S
              </div>
              <span className="font-bold text-lg sm:text-xl text-slate-900 tracking-tight">
                Student Skill Gap Analyzer
              </span>
            </Link>

            <nav className="hidden md:ml-8 md:flex md:space-x-4 items-center">
              {navLinks.map((link) => {
                const isActive = location.pathname === link.path;
                return (
                  <Link
                    key={link.path}
                    to={link.path}
                    className={`px-3 py-2 rounded-md text-sm font-medium transition ${
                      isActive
                        ? 'bg-primary-50 text-primary-700'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                    }`}
                  >
                    {link.label}
                  </Link>
                );
              })}
            </nav>
          </div>

          <div className="flex items-center space-x-3">
            <div
              className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${
                backendStatus === 'UP'
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : backendStatus === 'DOWN'
                  ? 'bg-rose-50 text-rose-700 border border-rose-200'
                  : 'bg-amber-50 text-amber-700 border border-amber-200'
              }`}
              title={
                backendStatus === 'UP'
                  ? 'Backend is connected and healthy'
                  : backendStatus === 'DOWN'
                  ? 'Backend unavailable at configured base URL'
                  : 'Checking backend status...'
              }
            >
              <span
                className={`w-2 h-2 mr-1.5 rounded-full ${
                  backendStatus === 'UP'
                    ? 'bg-emerald-500'
                    : backendStatus === 'DOWN'
                    ? 'bg-rose-500'
                    : 'bg-amber-500 animate-pulse'
                }`}
              />
              Backend: {backendStatus}
            </div>
          </div>
        </div>
      </div>

      {/* Mobile nav bar */}
      <div className="md:hidden border-t border-slate-100 px-4 py-2 flex space-x-1 overflow-x-auto">
        {navLinks.map((link) => {
          const isActive = location.pathname === link.path;
          return (
            <Link
              key={link.path}
              to={link.path}
              className={`px-2.5 py-1 rounded text-xs font-medium whitespace-nowrap ${
                isActive
                  ? 'bg-primary-50 text-primary-700 font-semibold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {link.label}
            </Link>
          );
        })}
      </div>
    </header>
  );
};
