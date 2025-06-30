'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';

export default function OAuth2RedirectPage() {
  const router = useRouter();
  const { loginWithToken } = useAuth();

  useEffect(() => {
    const handleOAuth2Redirect = async () => {
      try {
        // Get URL parameters
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        const refreshToken = urlParams.get('refreshToken');
        const error = urlParams.get('error');

        if (error) {
          console.error('OAuth2 error:', error);
          router.push('/auth?error=oauth2_failed');
          return;
        }

        if (token && refreshToken) {
          // Use the login function from AuthContext
          await loginWithToken({
            token,
            refreshToken,
            tokenType: 'Bearer',
            expiresIn: parseInt(urlParams.get('expiresIn') || '3600')
          });

          // Redirect to dashboard on successful login
          router.push('/dashboard');
        } else {
          console.error('Missing tokens in OAuth2 redirect');
          router.push('/auth?error=missing_tokens');
        }
      } catch (error) {
        console.error('Error processing OAuth2 redirect:', error);
        router.push('/auth?error=oauth2_processing_failed');
      }
    };

    handleOAuth2Redirect();
  }, [router, loginWithToken]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <h2 className="mt-6 text-xl font-medium text-gray-900">
            Processing OAuth2 login...
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Please wait while we complete your authentication.
          </p>
        </div>
      </div>
    </div>
  );
} 