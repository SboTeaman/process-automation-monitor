import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import { AlertCircle } from 'lucide-react'

const JOB_TYPES = ['HTTP_CALL', 'CSV_PROCESS', 'DATA_VALIDATE', 'REPORT_GENERATE']

// ============================================================================
// APPROACH 3: VISUAL FORM BUILDER - MOST USER FRIENDLY
// ============================================================================

export default function JobCreateAdvanced() {
  const navigate = useNavigate()

  const [basicInfo, setBasicInfo] = useState({
    name: '',
    description: '',
    type: 'CSV_PROCESS',
    cronExpression: '0 0 * * *',
    timeout: 30,
    maxRetries: 3,
    enabled: true,
  })

  const [csvConfig, setCsvConfig] = useState({
    sourceType: 'file',
    sourcePath: '/data/sales-import.csv',
    filterEnabled: true,
    filterField: 'status',
    filterValue: 'completed',
    selectedColumns: ['date', 'amount', 'customer'],
    sortEnabled: true,
    sortField: 'amount',
    sortDirection: 'desc',
    outputFormat: 'csv',
    outputPath: '/reports/sales-processed.csv',
  })

  const [step, setStep] = useState(1)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [editingJson, setEditingJson] = useState(false)
  const [jsonError, setJsonError] = useState('')
  const [customConfig, setCustomConfig] = useState<string | null>(null)

  // Convert visual form to JSON config
  const buildJsonConfig = () => {
    return {
      source: {
        type: csvConfig.sourceType,
        path: csvConfig.sourcePath,
      },
      processing: {
        ...(csvConfig.filterEnabled && {
          filter: {
            field: csvConfig.filterField,
            value: csvConfig.filterValue,
          },
        }),
        columns: csvConfig.selectedColumns,
        ...(csvConfig.sortEnabled && {
          sort: {
            field: csvConfig.sortField,
            direction: csvConfig.sortDirection,
          },
        }),
      },
      output: {
        format: csvConfig.outputFormat,
        path: csvConfig.outputPath,
      },
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const config = customConfig ? JSON.parse(customConfig) : buildJsonConfig()
      const payload = {
        ...basicInfo,
        config,
      }

      console.log('Sending payload:', payload)
      await client.post('/jobs', payload)
      navigate('/jobs')
    } catch (err) {
      console.error('Error creating job:', err)
      if (err instanceof SyntaxError) {
        setError('Invalid JSON format')
      } else {
        const error = err as any
        if (error.response?.data) {
          const errorData = error.response.data
          const message = errorData.message || errorData.error || JSON.stringify(errorData)
          setError(`API Error: ${message}`)
        } else if (error.message) {
          setError(`Network Error: ${error.message}`)
        } else {
          setError('Failed to create job - check console for details')
        }
      }
    } finally {
      setLoading(false)
    }
  }

  const availableColumns = [
    'id', 'date', 'amount', 'customer', 'customer_id', 'status',
    'product', 'category', 'region', 'revenue', 'profit'
  ]

  const toggleColumn = (col: string) => {
    setCsvConfig(prev => ({
      ...prev,
      selectedColumns: prev.selectedColumns.includes(col)
        ? prev.selectedColumns.filter(c => c !== col)
        : [...prev.selectedColumns, col]
    }))
  }

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold text-gray-900 mb-2">Create New Job</h1>
      <p className="text-gray-600 mb-8">Visual Form Builder - No JSON Required!</p>

      {/* Progress Indicator */}
      <div className="mb-8 flex items-center gap-4">
        {[1, 2, 3].map((s) => (
          <div key={s} className="flex items-center gap-2">
            <div
              className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm ${
                s <= step
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-600'
              }`}
            >
              {s}
            </div>
            {s < 3 && (
              <div
                className={`w-8 h-1 ${s < step ? 'bg-blue-600' : 'bg-gray-200'}`}
              ></div>
            )}
          </div>
        ))}
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
          <AlertCircle className="text-red-600 flex-shrink-0 mt-0.5" size={20} />
          <p className="text-red-800">{error}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-8 space-y-8">

        {/* STEP 1: Basic Information */}
        {step === 1 && (
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">
              Step 1: Basic Information
            </h2>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Job Name *
              </label>
              <input
                type="text"
                value={basicInfo.name}
                onChange={(e) =>
                  setBasicInfo({ ...basicInfo, name: e.target.value })
                }
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="e.g., Daily Sales Report Processing"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={basicInfo.description}
                onChange={(e) =>
                  setBasicInfo({ ...basicInfo, description: e.target.value })
                }
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                placeholder="What does this job do?"
                rows={3}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Job Type *
                </label>
                <select
                  value={basicInfo.type}
                  onChange={(e) =>
                    setBasicInfo({ ...basicInfo, type: e.target.value })
                  }
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                >
                  {JOB_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Schedule (Cron) *
                </label>
                <input
                  type="text"
                  value={basicInfo.cronExpression}
                  onChange={(e) =>
                    setBasicInfo({ ...basicInfo, cronExpression: e.target.value })
                  }
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  placeholder="0 2 * * *"
                />
                <p className="text-xs text-gray-500 mt-1">
                  💡 Use <a href="https://crontab.guru" target="_blank" className="text-blue-600 underline">crontab.guru</a>
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Timeout (seconds)
                </label>
                <input
                  type="number"
                  value={basicInfo.timeout}
                  onChange={(e) =>
                    setBasicInfo({
                      ...basicInfo,
                      timeout: parseInt(e.target.value),
                    })
                  }
                  min="1"
                  max="300"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Max Retries
                </label>
                <input
                  type="number"
                  value={basicInfo.maxRetries}
                  onChange={(e) =>
                    setBasicInfo({
                      ...basicInfo,
                      maxRetries: parseInt(e.target.value),
                    })
                  }
                  min="0"
                  max="10"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                />
              </div>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="enabled"
                checked={basicInfo.enabled}
                onChange={(e) =>
                  setBasicInfo({ ...basicInfo, enabled: e.target.checked })
                }
                className="rounded"
              />
              <label htmlFor="enabled" className="text-sm font-medium text-gray-700">
                Enable job immediately
              </label>
            </div>
          </div>
        )}

        {/* STEP 2: CSV Configuration - VISUAL BUILDER */}
        {step === 2 && (
          <div className="space-y-8">
            <h2 className="text-2xl font-bold text-gray-900">
              Step 2: Configure CSV Processing
            </h2>

            {/* SOURCE CONFIGURATION */}
            <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
              <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                📂 Source File
              </h3>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Where is the file?
                </label>
                <div className="space-y-2">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="sourceType"
                      value="file"
                      checked={csvConfig.sourceType === 'file'}
                      onChange={(e) =>
                        setCsvConfig({
                          ...csvConfig,
                          sourceType: e.target.value,
                        })
                      }
                    />
                    <span className="text-sm">Local file path</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="sourceType"
                      value="s3"
                      checked={csvConfig.sourceType === 's3'}
                      onChange={(e) =>
                        setCsvConfig({
                          ...csvConfig,
                          sourceType: e.target.value,
                        })
                      }
                    />
                    <span className="text-sm">S3 bucket</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="sourceType"
                      value="database"
                      checked={csvConfig.sourceType === 'database'}
                      onChange={(e) =>
                        setCsvConfig({
                          ...csvConfig,
                          sourceType: e.target.value,
                        })
                      }
                    />
                    <span className="text-sm">Database table</span>
                  </label>
                </div>

                {csvConfig.sourceType === 'file' && (
                  <div className="mt-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      File path
                    </label>
                    <input
                      type="text"
                      value={csvConfig.sourcePath}
                      onChange={(e) =>
                        setCsvConfig({
                          ...csvConfig,
                          sourcePath: e.target.value,
                        })
                      }
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none font-mono text-sm"
                      placeholder="/data/sales-import.csv"
                    />
                  </div>
                )}
              </div>
            </div>

            {/* FILTER CONFIGURATION */}
            <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  🔍 Filter
                </h3>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={csvConfig.filterEnabled}
                    onChange={(e) =>
                      setCsvConfig({
                        ...csvConfig,
                        filterEnabled: e.target.checked,
                      })
                    }
                  />
                  <span className="text-sm font-medium">Enable filter</span>
                </label>
              </div>

              {csvConfig.filterEnabled && (
                <div className="space-y-4">
                  <p className="text-sm text-gray-600">
                    Only include rows where:
                  </p>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Column
                      </label>
                      <input
                        type="text"
                        value={csvConfig.filterField}
                        onChange={(e) =>
                          setCsvConfig({
                            ...csvConfig,
                            filterField: e.target.value,
                          })
                        }
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                        placeholder="e.g., status"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Equals
                      </label>
                      <input
                        type="text"
                        value={csvConfig.filterValue}
                        onChange={(e) =>
                          setCsvConfig({
                            ...csvConfig,
                            filterValue: e.target.value,
                          })
                        }
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                        placeholder="e.g., completed"
                      />
                    </div>
                  </div>
                  <div className="bg-blue-50 p-3 rounded text-sm text-blue-800">
                    💡 Example: Keep only rows where "status" = "completed"
                  </div>
                </div>
              )}
            </div>

            {/* COLUMN SELECTION */}
            <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
              <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                📋 Select Columns
              </h3>
              <p className="text-sm text-gray-600 mb-4">
                Which columns to keep in output?
              </p>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                {availableColumns.map((col) => (
                  <label key={col} className="flex items-center gap-2 cursor-pointer p-2 rounded hover:bg-gray-100">
                    <input
                      type="checkbox"
                      checked={csvConfig.selectedColumns.includes(col)}
                      onChange={() => toggleColumn(col)}
                      className="rounded"
                    />
                    <span className="text-sm font-medium text-gray-700">{col}</span>
                  </label>
                ))}
              </div>
              <div className="mt-4 p-3 bg-blue-50 rounded text-sm text-blue-800">
                Selected: {csvConfig.selectedColumns.join(', ') || 'None'}
              </div>
            </div>

            {/* SORT CONFIGURATION */}
            <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  📊 Sort
                </h3>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={csvConfig.sortEnabled}
                    onChange={(e) =>
                      setCsvConfig({
                        ...csvConfig,
                        sortEnabled: e.target.checked,
                      })
                    }
                  />
                  <span className="text-sm font-medium">Enable sort</span>
                </label>
              </div>

              {csvConfig.sortEnabled && (
                <div className="space-y-4">
                  <p className="text-sm text-gray-600">
                    Sort by column:
                  </p>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Column
                      </label>
                      <input
                        type="text"
                        value={csvConfig.sortField}
                        onChange={(e) =>
                          setCsvConfig({
                            ...csvConfig,
                            sortField: e.target.value,
                          })
                        }
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                        placeholder="e.g., amount"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Direction
                      </label>
                      <select
                        value={csvConfig.sortDirection}
                        onChange={(e) =>
                          setCsvConfig({
                            ...csvConfig,
                            sortDirection: e.target.value,
                          })
                        }
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                      >
                        <option value="asc">Ascending (Low to High)</option>
                        <option value="desc">Descending (High to Low)</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* OUTPUT CONFIGURATION */}
            <div className="border border-gray-200 rounded-lg p-6 bg-gray-50">
              <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center gap-2">
                💾 Output
              </h3>

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Format
                  </label>
                  <select
                    value={csvConfig.outputFormat}
                    onChange={(e) =>
                      setCsvConfig({
                        ...csvConfig,
                        outputFormat: e.target.value,
                      })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
                  >
                    <option value="csv">CSV (Comma Separated)</option>
                    <option value="json">JSON</option>
                    <option value="excel">Excel (XLSX)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Output path
                  </label>
                  <input
                    type="text"
                    value={csvConfig.outputPath}
                    onChange={(e) =>
                      setCsvConfig({
                        ...csvConfig,
                        outputPath: e.target.value,
                      })
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none font-mono text-sm"
                    placeholder="/reports/sales-processed.csv"
                  />
                </div>
              </div>
            </div>

            {/* PREVIEW */}
            <div className="border border-blue-200 rounded-lg p-6 bg-blue-50">
              <h3 className="text-lg font-bold text-blue-900 mb-3 flex items-center gap-2">
                👀 JSON Preview (generated automatically)
              </h3>
              <pre className="bg-white p-4 rounded border border-blue-200 overflow-x-auto text-xs font-mono text-gray-800">
                {JSON.stringify(buildJsonConfig(), null, 2)}
              </pre>
            </div>
          </div>
        )}

        {/* STEP 3: Review */}
        {step === 3 && (
          <div className="space-y-6">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">
              Step 3: Review & Create
            </h2>

            <div className="space-y-6">
              <div className="border border-gray-200 rounded-lg p-6">
                <h3 className="font-bold text-gray-900 mb-3">Basic Information</h3>
                <div className="space-y-2 text-sm">
                  <p><span className="font-medium text-gray-700">Name:</span> {basicInfo.name}</p>
                  <p><span className="font-medium text-gray-700">Type:</span> {basicInfo.type}</p>
                  <p><span className="font-medium text-gray-700">Schedule:</span> {basicInfo.cronExpression}</p>
                  <p><span className="font-medium text-gray-700">Timeout:</span> {basicInfo.timeout}s</p>
                  <p><span className="font-medium text-gray-700">Retries:</span> {basicInfo.maxRetries}</p>
                </div>
              </div>

              <div className="border border-gray-200 rounded-lg p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-bold text-gray-900">Configuration</h3>
                  <button
                    type="button"
                    onClick={() => {
                      setEditingJson(!editingJson)
                      setJsonError('')
                      if (!editingJson && !customConfig) {
                        setCustomConfig(JSON.stringify(buildJsonConfig(), null, 2))
                      }
                    }}
                    className="text-sm px-3 py-1 rounded border border-gray-300 hover:bg-gray-50 transition-colors"
                  >
                    {editingJson ? '✓ Done Editing' : '✎ Edit JSON'}
                  </button>
                </div>

                {!editingJson ? (
                  <pre className="bg-gray-50 p-4 rounded overflow-x-auto text-xs font-mono">
                    {JSON.stringify(customConfig ? JSON.parse(customConfig) : buildJsonConfig(), null, 2)}
                  </pre>
                ) : (
                  <div className="space-y-2">
                    <textarea
                      value={customConfig || JSON.stringify(buildJsonConfig(), null, 2)}
                      onChange={(e) => {
                        setCustomConfig(e.target.value)
                        try {
                          JSON.parse(e.target.value)
                          setJsonError('')
                        } catch {
                          setJsonError('Invalid JSON format')
                        }
                      }}
                      className={`w-full px-4 py-2 border rounded font-mono text-xs p-4 focus:ring-2 outline-none ${
                        jsonError
                          ? 'border-red-300 focus:ring-red-500'
                          : 'border-gray-300 focus:ring-blue-500'
                      }`}
                      rows={12}
                    />
                    {jsonError && (
                      <p className="text-sm text-red-600">⚠️ {jsonError}</p>
                    )}
                  </div>
                )}
              </div>

              <div className={`border rounded-lg p-4 ${
                jsonError ? 'bg-red-50 border-red-200' : 'bg-green-50 border-green-200'
              }`}>
                <p className={`text-sm ${jsonError ? 'text-red-800' : 'text-green-800'}`}>
                  {jsonError ? `❌ ${jsonError}` : '✅ Configuration looks good! Click "Create Job" to save.'}
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Navigation Buttons */}
        <div className="flex gap-4 justify-between pt-8 border-t border-gray-200">
          <button
            type="button"
            onClick={() => setStep(Math.max(1, step - 1))}
            disabled={step === 1}
            className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 font-medium disabled:opacity-50 transition-colors"
          >
            ← Previous
          </button>

          {step < 3 ? (
            <button
              type="button"
              onClick={() => setStep(step + 1)}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg transition-colors"
            >
              Next →
            </button>
          ) : (
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => navigate('/jobs')}
                className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 font-medium transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading || jsonError !== ''}
                className="px-6 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white font-medium rounded-lg transition-colors"
              >
                {loading ? 'Creating...' : '✓ Create Job'}
              </button>
            </div>
          )}
        </div>
      </form>
    </div>
  )
}
