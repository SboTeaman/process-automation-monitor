import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import {
  Menu,
  Home,
  Zap,
  BarChart3,
  AlertCircle,
  History,
  LogOut,
  ChevronDown,
} from 'lucide-react'
import { useState } from 'react'

export default function Layout() {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [userMenuOpen, setUserMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const menuItems = [
    { path: '/', label: 'Dashboard', icon: Home },
    { path: '/jobs', label: 'Jobs', icon: Zap },
    { path: '/analytics', label: 'Analytics', icon: BarChart3 },
    { path: '/alerts', label: 'Alerts', icon: AlertCircle },
    { path: '/execution-history', label: 'Execution History', icon: History },
  ]

  return (
    <div className="flex h-screen bg-gray-50">
      <aside
        className={`${
          sidebarOpen ? 'w-64' : 'w-20'
        } bg-gray-900 text-white transition-all duration-300 flex flex-col`}
      >
        <div className="p-4 border-b border-gray-700 flex items-center justify-between">
          {sidebarOpen && <h1 className="text-xl font-bold">PAM</h1>}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-1 hover:bg-gray-800 rounded"
          >
            <Menu size={20} />
          </button>
        </div>

        <nav className="flex-1 p-4 space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon
            return (
              <Link
                key={item.path}
                to={item.path}
                className="flex items-center gap-3 px-4 py-2 rounded-lg hover:bg-gray-800 transition-colors group relative"
              >
                <Icon size={20} />
                {sidebarOpen && <span>{item.label}</span>}
                {!sidebarOpen && (
                  <div className="absolute left-20 bg-gray-800 px-2 py-1 rounded whitespace-nowrap opacity-0 group-hover:opacity-100 pointer-events-none">
                    {item.label}
                  </div>
                )}
              </Link>
            )
          })}
        </nav>

        <div className="p-4 border-t border-gray-700">
          <div className="relative">
            <button
              onClick={() => setUserMenuOpen(!userMenuOpen)}
              className="flex items-center gap-2 w-full px-3 py-2 rounded-lg hover:bg-gray-800 transition-colors"
            >
              <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-sm font-bold">
                {user?.username?.[0].toUpperCase()}
              </div>
              {sidebarOpen && (
                <>
                  <div className="flex-1 text-left text-sm">
                    <div className="font-medium">{user?.username}</div>
                    <div className="text-gray-400 text-xs">{user?.role}</div>
                  </div>
                  <ChevronDown size={16} />
                </>
              )}
            </button>

            {userMenuOpen && sidebarOpen && (
              <div className="absolute bottom-full left-0 right-0 bg-gray-800 rounded-lg mb-2 shadow-lg overflow-hidden">
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-2 px-4 py-2 w-full hover:bg-gray-700 transition-colors text-red-400"
                >
                  <LogOut size={16} />
                  <span>Logout</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </aside>

      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
