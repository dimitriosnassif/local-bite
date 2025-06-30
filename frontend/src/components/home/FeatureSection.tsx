'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ShoppingCart, Truck, Zap, CreditCard, MapPin, Clock } from "lucide-react";
import Image from "next/image";

export default function FeatureSection() {
  const features = [
    {
      icon: ShoppingCart,
      title: "Online checkout that converts more orders",
      description: "Beautiful, mobile-optimized checkout experience that makes ordering seamless for your customers.",
      buttonText: "STORE CHECKOUT",
      image: "/images/checkout-demo.jpg", // Placeholder
      bgColor: "bg-emerald-600",
      highlights: [
        { icon: CreditCard, text: "Multiple payment options" },
        { icon: ShoppingCart, text: "Cart optimization" },
        { icon: Zap, text: "Lightning fast checkout" }
      ]
    },
    {
      icon: Truck,
      title: "Expand your customer base with delivery",
      description: "Reach more customers with integrated delivery options and pickup scheduling.",
      buttonText: "INTEGRATED DELIVERY & PICKUP",
      image: "/images/delivery-demo.jpg", // Placeholder
      bgColor: "bg-slate-700",
      highlights: [
        { icon: MapPin, text: "Location-based delivery" },
        { icon: Clock, text: "Flexible scheduling" },
        { icon: Truck, text: "Real-time tracking" }
      ]
    },
    {
      icon: Zap,
      title: "Set up and start selling in minutes",
      description: "Create your online store quickly with our intuitive setup process and beautiful templates.",
      buttonText: "EASY CUSTOM STORE",
      image: "/images/setup-demo.jpg", // Placeholder
      bgColor: "bg-amber-600",
      highlights: [
        { icon: Zap, text: "Quick setup process" },
        { icon: ShoppingCart, text: "Ready-made templates" },
        { icon: CreditCard, text: "Instant payment setup" }
      ]
    }
  ];

  return (
    <section className="py-20 px-4 bg-white">
      <div className="max-w-7xl mx-auto">
        {/* Section Header */}
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4">
            Everything you need to{' '}
            <span className="text-slate-800">sell food online</span>
          </h2>
          <p className="text-xl text-slate-600 max-w-3xl mx-auto">
            From beautiful storefronts to seamless checkout, we've got all the tools 
            to help your food business thrive online.
          </p>
        </div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <Card key={index} className="group hover:shadow-2xl transition-all duration-300 border-0 shadow-lg overflow-hidden">
              {/* Feature Image/Demo */}
              <div className="relative h-64 bg-gradient-to-br from-slate-50 to-slate-100 overflow-hidden">
                <div className={`absolute inset-0 ${feature.bgColor} opacity-10`}></div>
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className={`w-32 h-32 ${feature.bgColor} rounded-2xl flex items-center justify-center shadow-xl`}>
                    <feature.icon className="w-16 h-16 text-white" />
                  </div>
                </div>
                {/* Floating elements for visual interest */}
                <div className="absolute top-4 right-4 w-16 h-16 bg-white rounded-xl shadow-lg flex items-center justify-center opacity-80">
                  <feature.icon className="w-8 h-8 text-slate-600" />
                </div>
              </div>

              <CardHeader className="pb-4">
                <CardTitle className="text-2xl mb-3 group-hover:text-slate-800 transition-colors">
                  {feature.title}
                </CardTitle>
                <CardDescription className="text-base leading-relaxed">
                  {feature.description}
                </CardDescription>
              </CardHeader>

              <CardContent className="space-y-4">
                {/* Feature Highlights */}
                <div className="space-y-2">
                  {feature.highlights.map((highlight, idx) => (
                    <div key={idx} className="flex items-center gap-3 text-sm text-slate-600">
                      <highlight.icon className="w-4 h-4 text-amber-600" />
                      <span>{highlight.text}</span>
                    </div>
                  ))}
                </div>

                {/* CTA Button */}
                <Button 
                  variant="outline" 
                  className="w-full mt-6 border-2 hover:border-slate-800 hover:text-slate-800 font-semibold tracking-wide"
                >
                  {feature.buttonText}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Bottom CTA */}
        <div className="text-center mt-16">
          <Button variant="accent" size="lg" className="px-8 py-4 text-lg shadow-lg hover:shadow-xl transition-all duration-300">
            Start Your Free Store Today
          </Button>
          <p className="text-slate-500 text-sm mt-4">
            Join 10,000+ food entrepreneurs already using LocalBite
          </p>
        </div>
      </div>
    </section>
  );
} 