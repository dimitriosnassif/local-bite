'use client';

import { Button } from "@/components/ui/button";
import { ArrowRight } from "lucide-react";

export default function HeroSection() {
  return (
    <section className="relative bg-gradient-to-br from-slate-50 to-slate-100 py-20 px-4 min-h-[80vh] flex items-center justify-center">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-slate-200 rounded-full mix-blend-multiply filter blur-xl opacity-70 animate-blob"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-amber-200 rounded-full mix-blend-multiply filter blur-xl opacity-70 animate-blob animation-delay-2000"></div>
      </div>

      <div className="relative max-w-6xl mx-auto text-center">
        {/* Logo/Brand Icon */}
        <div className="flex items-center justify-center mx-auto mb-8">
          <img 
            src="/localbite-logo.png" 
            alt="LocalBite" 
            className="h-24 md:h-32 lg:h-40 w-auto object-contain"
          />
        </div>

        {/* Main Heading */}
        <h1 className="text-5xl md:text-6xl lg:text-7xl font-bold text-slate-900 mb-6 tracking-tight">
          Turn Your Kitchen Into a{' '}
          <span className="text-slate-800 relative">
            Business
            <svg className="absolute -bottom-2 left-0 w-full h-3" viewBox="0 0 100 10" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M0 8C20 4, 40 2, 60 4C80 6, 90 8, 100 6" stroke="currentColor" strokeWidth="2" fill="none" className="text-amber-400"/>
            </svg>
          </span>
        </h1>

        {/* Subtitle */}
        <p className="text-xl md:text-2xl text-slate-600 mb-8 max-w-3xl mx-auto leading-relaxed">
          Join thousands of home chefs, caterers, bakers, and local pop ups who 
          are selling their food with LocalBite.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-8">
          <Button variant="accent" size="lg" className="text-lg px-8 py-4 shadow-lg hover:shadow-xl transition-all duration-300">
            Get started
            <ArrowRight className="ml-2 w-5 h-5" />
          </Button>
          <Button variant="outline" size="lg" className="text-lg px-8 py-4 border-2 border-slate-300 text-slate-700 hover:bg-slate-50">
            Watch demo
          </Button>
        </div>

        {/* Social Proof */}
        <p className="text-slate-500 text-sm">
          No monthly fees. No credit card required.
        </p>
      </div>
    </section>
  );
} 