import React from "react";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from "recharts";

const COLORS = [
  "#3B82F6", "#8B5CF6", "#10B981", "#F59E0B", "#EF4444",
  "#06B6D4", "#EC4899", "#6366F1", "#14B8A6", "#F97316"
];

function ChartDisplay({ data, type }) {
  if (!data || data.length === 0) {
    return (
      <div className="text-center py-12 bg-gray-50 rounded-xl border border-gray-200">
        <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
        <p className="text-gray-500 font-medium">No data to display</p>
      </div>
    );
  }

  const chartData = data.map(item => {
    const base = {
      name: item.label || item.name || 'N/A',
      value: typeof item.value === 'string' ? parseFloat(item.value) : item.value
    };
    Object.keys(item).forEach(key => {
      if (key !== 'label' && key !== 'value' && key !== 'name') {
        base[key] = item[key];
      }
    });
    return base;
  });

  const renderChart = () => {
    switch (type?.toLowerCase()) {
      case "bar":
        return (
          <ResponsiveContainer width="100%" height={350}>
            <BarChart data={chartData}>
              <defs>
                <linearGradient id="colorBar" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.8}/>
                  <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0.8}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis 
                dataKey="name" 
                tick={{ fontSize: 12, fill: '#6B7280' }}
                angle={-45}
                textAnchor="end"
                height={100}
              />
              <YAxis tick={{ fontSize: 12, fill: '#6B7280' }} />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#fff', 
                  border: '1px solid #E5E7EB',
                  borderRadius: '12px',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                }}
              />
              <Legend wrapperStyle={{ paddingTop: '20px' }} />
              <Bar dataKey="value" fill="url(#colorBar)" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        );

      case "line":
        return (
          <ResponsiveContainer width="100%" height={350}>
            <LineChart data={chartData}>
              <defs>
                <linearGradient id="colorLine" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="5%" stopColor="#3B82F6" stopOpacity={1}/>
                  <stop offset="95%" stopColor="#8B5CF6" stopOpacity={1}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis 
                dataKey="name" 
                tick={{ fontSize: 12, fill: '#6B7280' }}
                angle={-45}
                textAnchor="end"
                height={100}
              />
              <YAxis tick={{ fontSize: 12, fill: '#6B7280' }} />
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#fff', 
                  border: '1px solid #E5E7EB',
                  borderRadius: '12px',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                }}
              />
              <Legend wrapperStyle={{ paddingTop: '20px' }} />
              <Line 
                type="monotone" 
                dataKey="value" 
                stroke="url(#colorLine)"
                strokeWidth={3}
                dot={{ fill: '#3B82F6', r: 5, strokeWidth: 2, stroke: '#fff' }}
                activeDot={{ r: 7 }}
              />
            </LineChart>
          </ResponsiveContainer>
        );

      case "pie":
        return (
          <ResponsiveContainer width="100%" height={400}>
            <PieChart margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                outerRadius={49}
                innerRadius={0}
                fill="#8884d8"
                dataKey="value"
                paddingAngle={2}
              >
                {chartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip 
                contentStyle={{ 
                  backgroundColor: '#fff', 
                  border: '1px solid #E5E7EB',
                  borderRadius: '12px',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
                }}
              />
              <Legend 
                layout="horizontal" 
                verticalAlign="bottom" 
                align="center"
                wrapperStyle={{ paddingTop: '10px' }}
              />
            </PieChart>
          </ResponsiveContainer>
        );

      case "table":
      default:
        const firstRow = data && data.length > 0 ? data[0] : chartData && chartData.length > 0 ? chartData[0] : {};
        
        const allKeys = Object.keys(firstRow);
        
        const displayColumns = allKeys.filter(key => {
          if (key === 'value' && allKeys.length > 2) return false;
          if (key === 'label' && (allKeys.includes('name') || allKeys.some(k => k !== 'label' && k !== 'value'))) return false;
          return true;
        });
        
        const finalColumns = displayColumns.length > 0 ? displayColumns : ['name', 'value'];

        return (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gradient-to-r from-blue-50 to-indigo-50">
                <tr>
                  {finalColumns.map(col => (
                    <th
                      key={col}
                      className="px-6 py-4 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider"
                    >
                      {col.replace(/_/g, ' ')}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {(data || chartData).map((row, idx) => (
                  <tr key={idx} className="hover:bg-gray-50 transition-colors">
                    {finalColumns.map(col => (
                      <td
                        key={`${idx}-${col}`}
                        className="px-6 py-4 whitespace-nowrap text-sm text-gray-900"
                      >
                        {col === 'value' ? (
                          <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                            {typeof row[col] === 'number'
                              ? row[col].toLocaleString(undefined, { maximumFractionDigits: 2 })
                              : row[col]}
                          </span>
                        ) : (
                          row[col] !== undefined ? row[col] : row.name || 'N/A'
                        )}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
    }
  };

  return (
    <div className="bg-white rounded-xl p-6 border border-gray-200 my-4 shadow-lg">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-base font-semibold text-gray-800 flex items-center">
          <div className="bg-gradient-to-r from-blue-100 to-indigo-100 p-2 rounded-lg mr-2">
            <svg className="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          {type === "bar" && "Bar Chart"}
          {type === "line" && "Line Chart"}
          {type === "pie" && "Pie Chart"}
          {type === "table" && "Data Table"}
          {!type && "Visualization"}
        </h3>
        <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-700">
          {chartData.length} items
        </span>
      </div>
      {renderChart()}
    </div>
  );
}

export default ChartDisplay;
