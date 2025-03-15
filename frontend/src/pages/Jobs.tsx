import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'
import { Plus, Edit2, Trash2, Play, ToggleRight, ToggleLeft } from 'lucide-react'

interface Job {
  id: string
  name: string
  description: string
  type: string
  cronExpression: string
  enabled: boolean
  createdAt: string
  status: 'ACTIVE' | 'INACTIVE'
}

export default function Jobs() {
  const [jobs, setJobs] = useState<Job[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const fetchJobs = useCallback(async () => {
    try {
      const response = await client.get('/jobs', {
        params: { page, size: 10 },
      })
      setJobs(response.data.content)
      setTotalPages(response.data.totalPages)
    } catch (error) {
      console.error('Failed to fetch jobs:', error)
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    fetchJobs()
  }, [fetchJobs])

  const handleToggle = async (jobId: string, enabled: boolean) => {
    try {
      await client.patch(`/jobs/${jobId}/toggle`)
      setJobs(
        jobs.map((job) =>
          job.id === jobId ? { ...job, enabled: !enabled } : job
        )
      )
    } catch (error) {
      console.error('Failed to toggle job:', error)
    }
  }

  const handleDelete = async (jobId: string) => {
    if (confirm('Are you sure you want to delete this job?')) {
      try {
        await client.delete(`/jobs/${jobId}`)
        setJobs(jobs.filter((job) => job.id !== jobId))
      } catch (error) {
        console.error('Failed to delete job:', error)
      }
    }
  }

  const handleTrigger = async (jobId: string) => {
    try {
      await client.post(`/jobs/${jobId}/trigger`)
      alert('Job queued — check Execution History in a moment')
    } catch (error: any) {
      const msg = error?.response?.data?.message ?? error?.message ?? 'Unknown error'
      alert(`Failed to trigger job: ${msg}`)
      console.error('Failed to trigger job:', error)
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
        <h1 className="text-3xl font-bold text-gray-900">Jobs</h1>
        <Link
          to="/jobs/create"
          className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2 rounded-lg transition-colors"
        >
          <Plus size={20} />
          Create Job
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Name
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Type
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Cron
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold text-gray-900">
                Status
              </th>
              <th className="px-6 py-3 text-right text-sm font-semibold text-gray-900">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {jobs.map((job) => (
              <tr key={job.id} className="hover:bg-gray-50">
                <td className="px-6 py-4">
                  <div>
                    <p className="font-medium text-gray-900">{job.name}</p>
                    <p className="text-sm text-gray-600">{job.description}</p>
                  </div>
                </td>
                <td className="px-6 py-4">
                  <span className="inline-block px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
                    {job.type}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-600">
                  {job.cronExpression}
                </td>
                <td className="px-6 py-4">
                  <span
                    className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${
                      job.enabled
                        ? 'bg-green-100 text-green-800'
                        : 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {job.enabled ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-6 py-4 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => handleToggle(job.id, job.enabled)}
                      className="p-2 hover:bg-gray-200 rounded transition-colors"
                      title={job.enabled ? 'Disable' : 'Enable'}
                    >
                      {job.enabled ? (
                        <ToggleRight size={18} className="text-green-600" />
                      ) : (
                        <ToggleLeft size={18} className="text-gray-400" />
                      )}
                    </button>
                    <button
                      onClick={() => handleTrigger(job.id)}
                      className="p-2 hover:bg-gray-200 rounded transition-colors"
                      title="Trigger now"
                    >
                      <Play size={18} className="text-blue-600" />
                    </button>
                    <Link
                      to={`/jobs/${job.id}/edit`}
                      className="p-2 hover:bg-gray-200 rounded transition-colors"
                      title="Edit"
                    >
                      <Edit2 size={18} className="text-gray-600" />
                    </Link>
                    <button
                      onClick={() => handleDelete(job.id)}
                      className="p-2 hover:bg-gray-200 rounded transition-colors"
                      title="Delete"
                    >
                      <Trash2 size={18} className="text-red-600" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {jobs.length === 0 && (
          <div className="p-8 text-center text-gray-600">
            <p>No jobs found. Create your first job!</p>
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
