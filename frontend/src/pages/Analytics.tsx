import { useEffect, useState } from 'react'
import client from '../api/client'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts'

interface JobPerformance {
  jobId: string
  jobName: string
  successRate: number | null
  avgDuration: number | null
}

export default function Analytics() {
  const [dailyData, setDailyData] = useState([])
  const [jobPerformance, setJobPerformance] = useState<JobPerformance[]>([])
  const [topFailing, setTopFailing] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        const [dailyRes, performanceRes, failingRes] = await Promise.all([
          client.get('/stats/daily'),
          client.get('/stats/jobs/performance'),
          client.get('/stats/top-failing'),
        ])
        setDailyData(dailyRes.data)
        setJobPerformance(performanceRes.data)
        setTopFailing(failingRes.data)
      } catch (error) {
        console.error('Failed to fetch analytics:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchAnalytics()
  }, [])

  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  const COLORS = ['#10b981', '#ef4444', '#f59e0b']

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Analytics</h1>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Daily Execution Count</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={dailyData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#3b82f6" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Top Failing Jobs</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={topFailing}
                dataKey="failureCount"
                nameKey="jobName"
                cx="50%"
                cy="50%"
                outerRadius={80}
                label
              >
                {topFailing.map((_, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Job Performance</h2>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                  Job Name
                </th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                  Success Rate
                </th>
                <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                  Avg Duration (ms)
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {jobPerformance.map((job) => (
                <tr key={job.jobId} className="hover:bg-gray-50">
                  <td className="px-6 py-4 font-medium text-gray-900">
                    {job.jobName}
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <div className="w-24 bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-green-500 h-2 rounded-full"
                          style={{ width: `${job.successRate ?? 0}%` }}
                        ></div>
                      </div>
                      <span className="text-sm font-medium">
                        {(job.successRate ?? 0).toFixed(1)}%
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-600">
                    {(job.avgDuration ?? 0).toFixed(0)}ms
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
