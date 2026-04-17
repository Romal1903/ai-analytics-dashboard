import React, { useState, useRef, useEffect } from "react";
import api from "../utils/api";
import ChartDisplay from "./ChartDisplay";

function Chat() {
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!question.trim()) return;

    const userMessage = {
      type: "user",
      content: question,
      timestamp: new Date().toLocaleTimeString()
    };

    setMessages(prev => [...prev, userMessage]);
    setQuestion("");
    setLoading(true);

    try {
      const response = await api.post("/query", { question });

      const aiMessage = {
        type: "ai",
        content: response.data,
        timestamp: new Date().toLocaleTimeString()
      };

      setMessages(prev => [...prev, aiMessage]);

    } catch (error) {
      console.error("Query error:", error);
      
      const errorMessage = {
        type: "error",
        content: error.response?.data?.error || "Failed to process query",
        timestamp: new Date().toLocaleTimeString()
      };

      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async (messageContent, format) => {
    try {
      if (format === 'excel') {
        const response = await api.post("/export/excel", {
          chartData: messageContent.chart_data,
          question: messageContent.question
        }, {
          responseType: 'blob'
        });

        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'results.xlsx');
        document.body.appendChild(link);
        link.click();
        link.remove();
      } else if (format === 'csv') {
        const response = await api.post("/export/csv", {
          chartData: messageContent.chart_data
        });

        const blob = new Blob([response.data], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'results.csv');
        document.body.appendChild(link);
        link.click();
        link.remove();
      }
    } catch (error) {
      console.error("Export error:", error);

      let errorMessage = "Failed to export data";
      try {
        const blob = error?.response?.data;
        if (blob instanceof Blob) {
          const text = await blob.text();
          errorMessage = text || errorMessage;
        } else if (typeof error?.response?.data === 'string') {
          errorMessage = error.response.data;
        } else if (error?.response?.data?.error) {
          errorMessage = error.response.data.error;
        }
      } catch (e) {
      }

      alert(errorMessage);
    }
  };

  const suggestedQuestions = [
    "What is the total of all values?",
    "Show me the distribution",
    "What are the top 5 items?",
    "Calculate the average",
  ];

  return (
    <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-xl border border-gray-200 flex flex-col h-[calc(100vh-8rem)]">
      <div className="border-b border-gray-200 p-5 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-t-2xl">
        <h2 className="text-lg font-semibold text-gray-900 flex items-center">
          <div className="bg-gradient-to-r from-blue-600 to-indigo-600 p-2 rounded-lg mr-3">
            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
            </svg>
          </div>
          Ask Questions About Your Data
        </h2>
        <p className="text-sm text-gray-600 mt-1 ml-12">
          Type in natural language - AI will understand
        </p>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-4 custom-scrollbar">
        {messages.length === 0 ? (
          <div className="text-center py-12">
            <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-full p-6 w-20 h-20 mx-auto mb-6 flex items-center justify-center shadow-lg">
              <svg className="w-10 h-10 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-gray-900 mb-2">Start a Conversation</h3>
            <p className="text-gray-600 mb-6">Ask anything about your data</p>
            
            <div className="max-w-md mx-auto">
              <p className="text-xs font-semibold text-gray-500 mb-3">💡 Try these examples:</p>
              <div className="space-y-2">
                {suggestedQuestions.map((q, idx) => (
                  <button
                    key={idx}
                    onClick={() => setQuestion(q)}
                    className="w-full text-left px-4 py-3 text-sm bg-white hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 rounded-xl border border-gray-200 hover:border-blue-300 transition-all shadow-sm hover:shadow-md transform hover:-translate-y-0.5"
                  >
                    <span className="text-gray-700">{q}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        ) : (
          messages.map((msg, idx) => (
            <div key={idx} className={`flex ${msg.type === "user" ? "justify-end" : "justify-start"} fade-in`}>
              <div className={`max-w-3xl ${msg.type === "user" ? "ml-12" : "mr-12"}`}>
                <div className="flex items-center mb-2">
                  {msg.type === "ai" && (
                    <div className="w-7 h-7 rounded-full bg-gradient-to-r from-blue-600 to-indigo-600 flex items-center justify-center mr-2 shadow-md">
                      <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                      </svg>
                    </div>
                  )}
                  <span className="text-xs text-gray-500 font-medium">{msg.timestamp}</span>
                </div>

                {msg.type === "user" ? (
                  <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-5 py-3 rounded-2xl rounded-tr-sm shadow-lg">
                    <p className="text-sm leading-relaxed">{msg.content}</p>
                  </div>
                ) : msg.type === "error" ? (
                  <div className="bg-red-50 border border-red-200 text-red-800 px-5 py-3 rounded-2xl rounded-tl-sm shadow-md">
                    <p className="text-sm leading-relaxed">{msg.content}</p>
                  </div>
                ) : (
                  <div className="bg-gradient-to-r from-gray-50 to-gray-100 border border-gray-200 px-5 py-4 rounded-2xl rounded-tl-sm shadow-lg">
                    {msg.content.error ? (
                      <p className="text-sm text-red-600">{msg.content.message}</p>
                    ) : (
                      <div>
                        {msg.content.summary && (
                          <p className="text-sm text-gray-900 mb-4 font-medium leading-relaxed">{msg.content.summary}</p>
                        )}

                        {msg.content.chart_data && (
                          <div>
                            <div className="flex gap-2 mb-4">
                              <button
                                onClick={() => handleExport(msg.content, 'excel')}
                                className="inline-flex items-center px-4 py-2 text-xs font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-lg transition-all shadow-sm hover:shadow-md transform hover:-translate-y-0.5"
                              >
                                <svg className="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                                Export Excel
                              </button>
                              <button
                                onClick={() => handleExport(msg.content, 'csv')}
                                className="inline-flex items-center px-4 py-2 text-xs font-medium text-green-700 bg-green-50 hover:bg-green-100 rounded-lg transition-all shadow-sm hover:shadow-md transform hover:-translate-y-0.5"
                              >
                                <svg className="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                                Export CSV
                              </button>
                            </div>

                            <ChartDisplay 
                              data={msg.content.chart_data}
                              type={msg.content.chart_type}
                            />
                          </div>
                        )}

                        {msg.content.result !== undefined && !msg.content.chart_data && (
                          <div className="bg-white rounded-lg p-4 border border-gray-200 mt-3 shadow-sm">
                            <p className="text-sm font-mono text-gray-900">
                              {typeof msg.content.result === 'object' 
                                ? JSON.stringify(msg.content.result, null, 2)
                                : msg.content.result
                              }
                            </p>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))
        )}

        {loading && (
          <div className="flex justify-start fade-in">
            <div className="bg-gradient-to-r from-gray-50 to-gray-100 border border-gray-200 px-5 py-4 rounded-2xl rounded-tl-sm shadow-lg">
              <div className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-blue-400 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-indigo-400 rounded-full animate-bounce" style={{ animationDelay: "0.2s" }}></div>
                <div className="w-2 h-2 bg-purple-400 rounded-full animate-bounce" style={{ animationDelay: "0.4s" }}></div>
                <span className="text-sm text-gray-600 ml-2">AI is thinking...</span>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      <div className="border-t border-gray-200 p-4 bg-white rounded-b-2xl">
        <form onSubmit={handleSubmit} className="flex space-x-3">
          <input
            type="text"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask a question about your data..."
            disabled={loading}
            className="flex-1 px-5 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 transition-all shadow-sm"
          />
          <button
            type="submit"
            disabled={loading || !question.trim()}
            className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-8 py-3 rounded-xl font-medium hover:from-blue-700 hover:to-indigo-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </form>
      </div>
    </div>
  );
}

export default Chat;
