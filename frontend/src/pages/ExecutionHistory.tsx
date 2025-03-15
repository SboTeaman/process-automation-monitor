import { useEffect, useState, useCallback } from 'react'
import client from '../api/client'
import { formatDistanceToNow, parseISO, isValid } from 'date-fns'

interface Execution {
  id: string
  jobId: string
  jobName?: string
  status: 'SUCCESS' | 'FAILED' | 'RUNNING'
  durationMs?: number
  output?: string
  error?: string
  errorMessage?: string
  executedAt?: string
  startedAt?: string
}

export default function ExecutionHistory() {
  const [executions, setExecutions] = useState<Execution[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'SUCCESS' | 'FAILED' | 'RUNNING'>('ALL')

  const fetchExecutions = useCallback(async () => {
    try {
      const response = await client.get('/executions', {
        params: {
          page,
          size: 20,
          status: statusFilter !== 'ALL' ? statusFilter : undefined,
        },
      })
      setExecutions(response.data.content ?? [])
      setTotalPages(response.data.totalPages ?? 0)
    } catch (error) {
      console.error('Failed to fetch executions:', error)
    } finally {
      setLoading(false)
    }
  }, [page, statusFilter])

  useEffect(() => {
    fetchExecutions()
  }, [fetchExecutions])

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return 'bg-green-100 text-green-800'
      case 'FAILED':
        return 'bg-red-100 text-red-800'
      case 'RUNNING':
        return 'bg-yellow-100 text-yellow-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Execution History</h1>
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as any)
            setPage(0)
          }}
          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
        >
          <option value="ALL">All Statuses</option>
          <option value="SUCCESS">Success</option>
          <option value="FAILED">Failed</option>
          <option value="RUNNING">Running</option>
        </select>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Job Name
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Status
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Duration (ms)
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Executed
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {executions.map((execution) => (
              <tr key={execution.id} className="hover:bg-gray-50">
                <td className="px-6 py-4">
                  <p className="font-medium text-gray-900">
                    {execution.jobName ?? execution.jobId}
                  </p>
                  {(execution.error ?? execution.errorMessage) && (
                    <details className="mt-2">
                      <summary className="cursor-pointer text-sm text-red-600 hover:text-red-700">
                        View error
                      </summary>
                      <pre className="mt-2 bg-red-50 p-3 rounded text-xs overflow-auto text-red-800">
                        {execution.error ?? execution.errorMessage}
                      </pre>
                    </details>
                  )}
                  {execution.output && (
                    <details className="mt-2">
                      <summary className="cursor-pointer text-sm text-blue-600 hover:text-blue-700">
                        View output
                      </summary>
                      <pre className="mt-2 bg-blue-50 p-3 rounded text-xs overflow-auto text-blue-800">
                        {execution.output}
                      </pre>
                    </details>
                  )}
                </td>
                <td className="px-6 py-4">
                  <span
                    className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(execution.status)}`}
                  >
                    {execution.status}
                  </span>
                </td>
                <td className="px-6 py-4 text-gray-600">
                  {execution.durationMs != null ? `${execution.durationMs}ms` : '—'}
                </td>
                <td className="px-6 py-4 text-sm text-gray-600">
                  {(() => {
                    const raw = execution.executedAt ?? execution.startedAt
                    if (!raw) return '—'
                    const d = parseISO(raw)
                    return isValid(d) ? formatDistanceToNow(d, { addSuffix: true }) : '—'
                  })()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {executions.length === 0 && (
          <div className="p-8 text-center text-gray-600">
            <p>No execution history found</p>
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-4 py-2 border rounded-lg disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-4 py-2">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page === totalPages - 1}
            className="px-4 py-2 border rounded-lg disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
