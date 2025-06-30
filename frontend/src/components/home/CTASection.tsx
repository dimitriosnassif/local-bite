'use client';

import { Button } from "@/components/ui/button";
import { ArrowRight, CheckCircle } from "lucide-react";

export default function CTASection() {
  const benefits = [
    "No setup fees or monthly costs",
    "Accept payments instantly",
    "Mobile-optimized ordering",
    "24/7 customer support",
    "Built-in marketing tools",
    "Real-time analytics"
  ];

  return (
    <section className="py-20 px-4 bg-gradient-to-br from-slate-800 to-slate-900 text-white relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-white rounded-full opacity-10"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-amber-400 rounded-full opacity-20"></div>
      </div>

      <div className="relative max-w-6xl mx-auto text-center">
        {/* Main CTA */}
        <div className="mb-16">
          <h2 className="text-4xl md:text-6xl font-bold mb-6 tracking-tight">
            Ready to start{' '}
            <span className="text-amber-400">selling?</span>
          </h2>
          <p className="text-xl md:text-2xl text-slate-200 mb-8 max-w-3xl mx-auto leading-relaxed">
            Join thousands of successful food entrepreneurs who chose LocalBite 
            to grow their business online.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-8">
            <Button 
              size="lg" 
              variant="outline"
              className="text-lg px-8 py-4 bg-white text-slate-800 border-white hover:bg-slate-50 shadow-lg hover:shadow-xl transition-all duration-300"
            >
              Get started for free
              <ArrowRight className="ml-2 w-5 h-5" />
            </Button>
            <Button 
              variant="ghost" 
              size="lg" 
              className="text-lg px-8 py-4 text-white hover:bg-white/10"
            >
              Schedule a demo
            </Button>
          </div>

          <p className="text-slate-300 text-sm">
            Start selling in under 10 minutes • No credit card required
          </p>
        </div>

        {/* Benefits Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-4xl mx-auto">
          {benefits.map((benefit, index) => (
            <div key={index} className="flex items-center gap-3 text-left">
              <CheckCircle className="w-5 h-5 text-amber-400 flex-shrink-0" />
              <span className="text-slate-200">{benefit}</span>
            </div>
          ))}
        </div>

        {/* Social Proof */}
        <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl mx-auto">
          <div className="text-center">
            <div className="text-3xl font-bold text-white mb-2">98%</div>
            <div className="text-slate-300 text-sm">Customer Satisfaction</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-white mb-2">$2.5M+</div>
            <div className="text-slate-300 text-sm">Revenue Generated</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-white mb-2">24/7</div>
            <div className="text-slate-300 text-sm">Support Available</div>
          </div>
        </div>
      </div>
    </section>
  );
} 