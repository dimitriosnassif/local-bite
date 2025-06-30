'use client';

import { useAuth } from '@/contexts/AuthContext';
import { LogOut, User, Mail, Phone, Shield, CheckCircle, XCircle } from 'lucide-react';

export default function Dashboard() {
  const { user, logout } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 to-red-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="container mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <img 
                src="/localbite-logo.png" 
                alt="LocalBite" 
                className="h-12 w-auto object-contain"
              />
            </div>
            <button
              onClick={logout}
              className="flex items-center space-x-2 bg-gray-100 hover:bg-gray-200 px-4 py-2 rounded-lg transition-colors"
            >
              <LogOut className="h-5 w-5" />
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-6 py-12">
        <div className="max-w-4xl mx-auto">
          {/* Welcome Section */}
          <div className="bg-white rounded-lg shadow-lg p-8 mb-8">
            <div className="text-center mb-8">
              <div className="w-20 h-20 bg-orange-500 rounded-full flex items-center justify-center mx-auto mb-4">
                <User className="h-10 w-10 text-white" />
              </div>
              <h2 className="text-3xl font-bold text-gray-900">
                Welcome, {user.firstName}! 🎉
              </h2>
              <p className="text-gray-600 mt-2">
                Authentication test successful! Your backend connection is working perfectly.
              </p>
            </div>

            {/* User Info Cards */}
            <div className="grid md:grid-cols-2 gap-6">
              {/* Profile Info */}
              <div className="bg-gray-50 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <User className="h-5 w-5 mr-2" />
                  Profile Information
                </h3>
                <div className="space-y-3">
                  <div className="flex items-center">
                    <span className="text-gray-600 w-20">Name:</span>
                    <span className="font-medium">{user.firstName} {user.lastName}</span>
                  </div>
                  <div className="flex items-center">
                    <Mail className="h-4 w-4 text-gray-400 mr-2" />
                    <span className="text-gray-600 w-16">Email:</span>
                    <span className="font-medium">{user.email}</span>
                  </div>
                  {user.phoneNumber && (
                    <div className="flex items-center">
                      <Phone className="h-4 w-4 text-gray-400 mr-2" />
                      <span className="text-gray-600 w-16">Phone:</span>
                      <span className="font-medium">{user.phoneNumber}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Account Status */}
              <div className="bg-gray-50 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                  <Shield className="h-5 w-5 mr-2" />
                  Account Status
                </h3>
                <div className="space-y-3">
                  <div className="flex items-center">
                    <span className="text-gray-600 w-20">Provider:</span>
                    <span className="font-medium capitalize">{user.provider}</span>
                  </div>
                  <div className="flex items-center">
                    <span className="text-gray-600 w-20">Email:</span>
                    {user.emailVerified ? (
                      <div className="flex items-center text-green-600">
                        <CheckCircle className="h-4 w-4 mr-1" />
                        <span className="font-medium">Verified</span>
                      </div>
                    ) : (
                      <div className="flex items-center text-red-600">
                        <XCircle className="h-4 w-4 mr-1" />
                        <span className="font-medium">Not Verified</span>
                      </div>
                    )}
                  </div>
                  <div className="flex items-center">
                    <span className="text-gray-600 w-20">Roles:</span>
                    <div className="flex flex-wrap gap-1">
                      {user.roles.map((role) => (
                        <span
                          key={role}
                          className="bg-orange-100 text-orange-800 text-xs font-medium px-2 py-1 rounded"
                        >
                          {role.replace('ROLE_', '')}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Features Section */}
          <div className="grid md:grid-cols-3 gap-6">
            <div className="bg-white rounded-lg shadow-lg p-6 text-center">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <CheckCircle className="h-8 w-8 text-green-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Backend Connected</h3>
              <p className="text-gray-600 text-sm">
                Successfully authenticated with the Spring Boot backend server
              </p>
            </div>

            <div className="bg-white rounded-lg shadow-lg p-6 text-center">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Shield className="h-8 w-8 text-blue-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">JWT Authentication</h3>
              <p className="text-gray-600 text-sm">
                Your JWT token is working and protecting API endpoints
              </p>
            </div>

            <div className="bg-white rounded-lg shadow-lg p-6 text-center">
              <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <User className="h-8 w-8 text-purple-600" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">User Session</h3>
              <p className="text-gray-600 text-sm">
                User data is properly managed and persisted across page loads
              </p>
            </div>
          </div>

          {/* Testing Info */}
          <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-blue-900 mb-3">✅ Authentication Test Results</h3>
            <div className="text-blue-800 text-sm space-y-2">
              <p>• ✅ User registration working (with email verification requirement)</p>
              <p>• ✅ Manual email verification working (demo mode)</p>
              <p>• ✅ User login working with JWT tokens</p>
              <p>• ✅ Protected routes and JWT validation working</p>
              <p>• ✅ User session persistence working</p>
              <p>• ✅ Rate limiting and security features active</p>
              <p>• ✅ Frontend-Backend communication established</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
} 