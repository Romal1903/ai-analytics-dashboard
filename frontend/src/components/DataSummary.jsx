import React from "react";

function DataSummary({ summary }) {
  if (!summary) return null;

  return (
    <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-xl border border-gray-200 p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-5 flex items-center">
        <div className="bg-gradient-to-r from-green-500 to-emerald-500 p-2 rounded-lg mr-3">
          <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        Data Overview
      </h2>

      <div className="grid grid-cols-2 gap-4 mb-5">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl p-4 text-white shadow-lg transform transition-all hover:scale-105">
          <p className="text-xs font-medium opacity-90 mb-1">Total Rows</p>
          <p className="text-3xl font-bold">{summary.total_rows?.toLocaleString()}</p>
          <div className="flex items-center mt-2 text-xs opacity-75">
            <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M3 3a1 1 0 000 2v8a2 2 0 002 2h2.586l-1.293 1.293a1 1 0 101.414 1.414L10 15.414l2.293 2.293a1 1 0 001.414-1.414L12.414 15H15a2 2 0 002-2V5a1 1 0 100-2H3zm11.707 4.707a1 1 0 00-1.414-1.414L10 9.586 8.707 8.293a1 1 0 00-1.414 0l-2 2a1 1 0 101.414 1.414L8 10.414l1.293 1.293a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
            Records
          </div>
        </div>

        <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl p-4 text-white shadow-lg transform transition-all hover:scale-105">
          <p className="text-xs font-medium opacity-90 mb-1">Columns</p>
          <p className="text-3xl font-bold">{summary.total_columns}</p>
          <div className="flex items-center mt-2 text-xs opacity-75">
            <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
              <path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z" />
              <path fillRule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm3 4a1 1 0 000 2h.01a1 1 0 100-2H7zm3 0a1 1 0 000 2h3a1 1 0 100-2h-3zm-3 4a1 1 0 100 2h.01a1 1 0 100-2H7zm3 0a1 1 0 100 2h3a1 1 0 100-2h-3z" clipRule="evenodd" />
            </svg>
            Fields
          </div>
        </div>
      </div>

      <div className="mb-5">
        <p className="text-xs font-semibold text-gray-700 mb-3 flex items-center">
          <svg className="w-4 h-4 mr-1 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
          </svg>
          Column Names:
        </p>
        <div className="flex flex-wrap gap-2">
          {summary.columns?.map((col, idx) => (
            <span
              key={idx}
              className="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium bg-gradient-to-r from-gray-100 to-gray-200 text-gray-800 border border-gray-300 shadow-sm hover:shadow-md transition-all"
            >
              <svg className="w-3 h-3 mr-1 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              {col}
            </span>
          ))}
        </div>
      </div>

      {summary.ai_insights && summary.ai_insights.trim().length > 0 && (
        <div className="mt-5 p-5 bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 rounded-xl border border-indigo-200 shadow-md">
          <div className="flex items-center mb-3">
            <div className="bg-gradient-to-r from-indigo-500 to-purple-500 p-2 rounded-lg mr-2">
              <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
            </div>
            <p className="text-sm font-semibold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
              AI-Generated Insights
            </p>
          </div>
          <div className="text-xs text-gray-800 leading-relaxed whitespace-pre-line pl-8">
            {summary.ai_insights}
          </div>
        </div>
      )}
    </div>
  );
}

export default DataSummary;
