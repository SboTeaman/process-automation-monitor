import { useEffect, useState, useCallback } from 'react'
import client from '../api/client'
import { AlertCircle, CheckCircle } from 'lucide-react'
import { formatDistanceToNow, parseISO, isValid } from 'date-fns'

interface Alert {
  id: string
  jobId: string
  jobName?: string
  severity: string
  reason: string
  triggeredAt: string
  acknowledged: boolean
}

export default function Alerts() {
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [loading, setLoading] = useState(true)
  const [acknowledging, setAcknowledging] = useState<string | null>(null)

  const fetchAlerts = useCallback(async () => {
    try {
      const response = await client.get('/alerts')
      setAlerts(response.data.content ?? [])
    } catch (error) {
      console.error('Failed to fetch alerts:', error)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchAlerts()
  }, [fetchAlerts])

  const handleAcknowledge = async (alertId: string) => {
    setAcknowledging(alertId)
    try {
      await client.post(`/alerts/${alertId}/acknowledge`)
      setAlerts(
        alerts.map((alert) =>
          alert.id === alertId ? { ...alert, acknowledged: true } : alert
        )
      )
    } catch (error) {
      console.error('Failed to acknowledge alert:', error)
    } finally {
      setAcknowledging(null)
    }
  }

  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  const unacknowledgedAlerts = alerts.filter((a) => !a.acknowledged)

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Alerts</h1>

      <div className="space-y-4">
        {unacknowledgedAlerts.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <CheckCircle
              size={48}
              className="mx-auto text-green-600 mb-4"
            />
            <p className="text-gray-600">All alerts have been acknowledged</p>
          </div>
        ) : (
          unacknowledgedAlerts.map((alert) => (
            <div
              key={alert.id}
              className="bg-white rounded-lg shadow p-6 border-l-4 border-red-500"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <AlertCircle size={20} className="text-red-600" />
                    <h3 className="text-lg font-semibold text-gray-900">
                      {alert.jobName ?? alert.jobId}
                    </h3>
                    <span className="inline-block px-2 py-1 bg-red-100 text-red-800 rounded text-xs font-medium">
                      {alert.severity}
                    </span>
                  </div>
                  <p className="text-gray-600 mb-2">{alert.reason}</p>
                  <p className="text-sm text-gray-500">
                    {(() => {
                      const d = parseISO(alert.triggeredAt)
                      return isValid(d) ? formatDistanceToNow(d, { addSuffix: true }) : '—'
                    })()}
                  </p>
                </div>
                <button
                  onClick={() => handleAcknowledge(alert.id)}
                  disabled={acknowledging === alert.id}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white font-medium rounded-lg transition-colors flex-shrink-0"
                >
                  {acknowledging === alert.id ? 'Acknowledging...' : 'Acknowledge'}
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {alerts.length > unacknowledgedAlerts.length && (
        <div className="mt-8">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Acknowledged Alerts</h2>
          <div className="space-y-2">
            {alerts
              .filter((a) => a.acknowledged)
              .map((alert) => (
                <div
                  key={alert.id}
                  className="bg-gray-50 rounded-lg p-4 border-l-4 border-gray-300"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="font-medium text-gray-700">{alert.jobName ?? alert.jobId}</p>
                      <p className="text-sm text-gray-600">{alert.reason}</p>
                    </div>
                    <CheckCircle size={20} className="text-green-600 flex-shrink-0" />
                  </div>
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  )
}
