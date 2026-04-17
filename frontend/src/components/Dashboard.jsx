import React, { useState } from "react";
import Upload from "./Upload";
import Chat from "./Chat";
import DataSummary from "./DataSummary";

function Dashboard() {
  const [dataUploaded, setDataUploaded] = useState(false);
  const [summary, setSummary] = useState(null);

  const handleUploadSuccess = (summaryData) => {
    setDataUploaded(true);
    setSummary(summaryData);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">
      <header className="bg-white/80 backdrop-blur-sm shadow-lg border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-3 rounded-xl shadow-lg">
                <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <div>
                <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
                  AI Analytics Dashboard
                </h1>
                <p className="text-sm text-gray-600">Powered by Groq Llama 3.3 70B Versatile</p>
              </div>
            </div>
            {dataUploaded && (
              <div className="flex items-center space-x-2 bg-green-50 px-4 py-2 rounded-full border border-green-200">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-green-500"></span>
                </span>
                <span className="text-sm font-medium text-green-700">Data Ready</span>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1 space-y-6 fade-in">
            <Upload onUploadSuccess={handleUploadSuccess} />
            {summary && <DataSummary summary={summary} />}
          </div>

          <div className="lg:col-span-2 fade-in">
            {dataUploaded ? (
              <Chat />
            ) : (
              <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-xl border border-gray-200 p-12 text-center">
                <div className="max-w-md mx-auto">
                  <div className="bg-gradient-to-r from-blue-100 to-indigo-100 rounded-full p-6 w-24 h-24 mx-auto mb-6 flex items-center justify-center shadow-lg">
                    <svg className="w-12 h-12 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900 mb-3">Welcome!</h3>
                  <p className="text-gray-600 mb-4">
                    Upload a CSV file to start analyzing your data with AI-powered insights
                  </p>
                  <div className="flex items-center justify-center space-x-2 text-sm text-gray-500">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                    <span>Ask questions in natural language</span>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>

      <footer className="mt-12 pb-6 text-center text-sm text-gray-600">
        <p>Built with Spring Boot, React, Tailwind CSS & Groq</p>
      </footer>
    </div>
  );
}

export default Dashboard;
