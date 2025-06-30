'use client';

import { Button } from "@/components/ui/button";
import { Menu, X, ChevronDown } from "lucide-react";
import { useState } from "react";
import Link from "next/link";
import { useAuth } from '@/contexts/AuthContext';
import DropdownMenu from '../DropdownMenu';

export default function Header() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const { isAuthenticated, user, logout } = useAuth();

  const navItems = [
    { name: "Explore", href: "#explore" },
    { name: "Become a Seller", href: "#become-seller" },
    { name: "Features", href: "#features" },
    { name: "How It Works", href: "#how-it-works" },
    { name: "Pricing", href: "#pricing" },
  ];

  return (
    <header className="bg-white border-b border-gray-100 sticky top-0 z-50 backdrop-blur-sm bg-white/95">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-20">
          {/* Logo */}
          <Link href="/" className="flex items-center group">
            <img 
              src="/localbite-logo.png" 
              alt="LocalBite" 
              className="h-12 md:h-14 w-auto object-contain transition-opacity group-hover:opacity-80"
            />
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-6">
            {navItems.map((item) => (
              <Link
                key={item.name}
                href={item.href}
                className="text-slate-600 hover:text-slate-800 transition-colors font-medium"
              >
                {item.name}
              </Link>
            ))}
            
            {/* Inside LocalBite Dropdown */}
            <DropdownMenu 
              buttonText="Inside LocalBite"
              items={[
                { label: 'FAQs', href: '#faqs' },
                { label: 'Support', href: '#support' },
                { label: 'Wall of Love', href: '#wall-of-love' },
                { label: 'Blog', href: '#blog' },
                { label: 'Tutorials', href: '#tutorials' }
              ]}
            />
            
            {/* Auth Navigation */}
            {isAuthenticated ? (
              <div className="flex items-center space-x-4">
                <Link 
                  href="/dashboard"
                  className="text-slate-700 hover:text-slate-900 font-medium"
                >
                  Welcome, {user?.firstName}
                </Link>
                <button
                  onClick={logout}
                  className="bg-slate-100 hover:bg-slate-200 text-slate-700 px-4 py-2 rounded-lg transition-colors"
                >
                  Sign Out
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Button variant="ghost" asChild>
                  <Link href="/auth">Sign In</Link>
                </Button>
                <Button asChild>
                  <Link href="/auth">Get Started</Link>
                </Button>
              </div>
            )}
          </nav>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden p-2"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            {isMenuOpen ? (
              <X className="w-6 h-6 text-slate-600" />
            ) : (
              <Menu className="w-6 h-6 text-slate-600" />
            )}
          </button>
        </div>

        {/* Mobile Menu */}
        {isMenuOpen && (
          <div className="md:hidden border-t border-slate-100 py-4">
            <nav className="flex flex-col gap-4">
              {navItems.map((item) => (
                <Link
                  key={item.name}
                  href={item.href}
                  className="text-slate-600 hover:text-slate-800 transition-colors font-medium py-2"
                  onClick={() => setIsMenuOpen(false)}
                >
                  {item.name}
                </Link>
              ))}
              
              {/* Mobile Inside LocalBite Links */}
              <div className="border-t border-slate-100 pt-4">
                <p className="text-sm font-medium text-slate-900 mb-2">Inside LocalBite</p>
                <div className="pl-4 space-y-2">
                  <Link href="#faqs" className="block text-slate-600 hover:text-slate-800 transition-colors py-1" onClick={() => setIsMenuOpen(false)}>FAQs</Link>
                  <Link href="#support" className="block text-slate-600 hover:text-slate-800 transition-colors py-1" onClick={() => setIsMenuOpen(false)}>Support</Link>
                  <Link href="#wall-of-love" className="block text-slate-600 hover:text-slate-800 transition-colors py-1" onClick={() => setIsMenuOpen(false)}>Wall of Love</Link>
                  <Link href="#blog" className="block text-slate-600 hover:text-slate-800 transition-colors py-1" onClick={() => setIsMenuOpen(false)}>Blog</Link>
                  <Link href="#tutorials" className="block text-slate-600 hover:text-slate-800 transition-colors py-1" onClick={() => setIsMenuOpen(false)}>Tutorials</Link>
                </div>
              </div>
              
              {/* Mobile Auth Buttons */}
              <div className="flex flex-col gap-3 pt-4 border-t border-slate-100">
                {isAuthenticated ? (
                  <>
                    <Link 
                      href="/dashboard"
                      className="text-slate-700 hover:text-slate-900 font-medium py-2"
                      onClick={() => setIsMenuOpen(false)}
                    >
                      Welcome, {user?.firstName}
                    </Link>
                    <button
                      onClick={() => {
                        logout();
                        setIsMenuOpen(false);
                      }}
                      className="bg-slate-100 hover:bg-slate-200 text-slate-700 px-4 py-2 rounded-lg transition-colors justify-start text-left"
                    >
                      Sign Out
                    </button>
                  </>
                ) : (
                  <>
                    <Button variant="ghost" asChild className="justify-start">
                      <Link href="/auth">Sign in</Link>
                    </Button>
                    <Button asChild className="justify-start">
                      <Link href="/auth">Get started</Link>
                    </Button>
                  </>
                )}
              </div>
            </nav>
          </div>
        )}
      </div>
    </header>
  );
} 