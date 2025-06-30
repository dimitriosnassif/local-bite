'use client';

import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Star, MapPin, Clock } from "lucide-react";

export default function ChefShowcase() {
  const featuredChefs = [
    {
      id: 1,
      name: "Maria Rodriguez",
      businessName: "Abuela's Kitchen",
      location: "Downtown, Dubai",
      rating: 4.8,
      reviewCount: 127,
      description: "Authentic Mexican cuisine passed down through generations. Fresh tortillas made daily.",
      specialties: ["Mexican", "Vegetarian", "Gluten-Free"],
      minOrder: 25,
      deliveryTime: "45-60 min",
      isAvailable: true
    },
    {
      id: 2,
      name: "James Chen", 
      businessName: "Fusion Bites",
      location: "Marina, Dubai",
      rating: 4.9,
      reviewCount: 203,
      description: "Modern Asian fusion with a creative twist. Bold flavors and contemporary presentation.",
      specialties: ["Asian Fusion", "Vegan Options", "Quick Bites"],
      minOrder: 30,
      deliveryTime: "30-45 min",
      isAvailable: true
    },
    {
      id: 3,
      name: "Sarah Thompson",
      businessName: "Garden to Table", 
      location: "Abu Dhabi",
      rating: 4.7,
      reviewCount: 89,
      description: "Farm-fresh ingredients transformed into healthy, delicious meals with seasonal menus.",
      specialties: ["Farm-to-Table", "Organic", "Seasonal"],
      minOrder: 35,
      deliveryTime: "50-65 min",
      isAvailable: false
    }
  ];

  return (
    <section className="py-20 px-4 bg-slate-50">
      <div className="max-w-7xl mx-auto">
        {/* Section Header */}
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4">
            Meet our{' '}
            <span className="text-slate-800">featured chefs</span>
          </h2>
          <p className="text-xl text-slate-600 max-w-3xl mx-auto">
            Discover amazing local chefs bringing you fresh, delicious meals made with passion.
          </p>
        </div>

        {/* Chef Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
          {featuredChefs.map((chef) => (
            <Card key={chef.id} className="group hover:shadow-2xl transition-all duration-300 overflow-hidden border-0 shadow-lg">
              {/* Cover Image */}
              <div className="relative h-48 overflow-hidden bg-gradient-to-br from-slate-100 to-slate-200">
                <div className="absolute inset-0 bg-gradient-to-t from-black/50 to-transparent z-10" />
                
                {/* Availability Badge */}
                <div className="absolute top-4 right-4 z-20">
                  <Badge variant={chef.isAvailable ? "success" : "secondary"}>
                    {chef.isAvailable ? "Available Now" : "Closed"}
                  </Badge>
                </div>

                {/* Chef Info */}
                <div className="absolute bottom-4 left-4 z-20 flex items-center gap-3">
                  <div className="w-12 h-12 rounded-full bg-white p-0.5">
                    <div className="w-full h-full rounded-full bg-slate-300 flex items-center justify-center">
                      <span className="text-lg font-semibold text-slate-600">
                        {chef.name.charAt(0)}
                      </span>
                    </div>
                  </div>
                  <div>
                    <h3 className="text-white font-semibold">{chef.businessName}</h3>
                    <p className="text-white/80 text-sm">By {chef.name}</p>
                  </div>
                </div>
              </div>

              <CardContent className="p-6">
                {/* Rating and Location */}
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-1">
                    <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                    <span className="text-sm font-medium">{chef.rating}</span>
                    <span className="text-xs text-slate-500">({chef.reviewCount})</span>
                  </div>
                  <div className="flex items-center gap-1 text-slate-500 text-sm">
                    <MapPin className="w-4 h-4" />
                    <span>{chef.location}</span>
                  </div>
                </div>

                {/* Description */}
                <p className="text-slate-600 text-sm mb-4 leading-relaxed">
                  {chef.description}
                </p>

                {/* Specialties */}
                <div className="flex flex-wrap gap-2 mb-4">
                  {chef.specialties.map((specialty, index) => (
                    <Badge key={index} variant="secondary" className="text-xs">
                      {specialty}
                    </Badge>
                  ))}
                </div>

                {/* Order Info */}
                <div className="flex items-center justify-between text-sm text-slate-500 mb-4">
                  <div className="flex items-center gap-1">
                    <Clock className="w-4 h-4" />
                    <span>{chef.deliveryTime}</span>
                  </div>
                  <div>Min ${chef.minOrder}</div>
                </div>

                {/* CTA Button */}
                <Button 
                  className="w-full" 
                  disabled={!chef.isAvailable}
                  variant={chef.isAvailable ? "default" : "secondary"}
                >
                  {chef.isAvailable ? "View Menu" : "Currently Closed"}
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Bottom CTA */}
        <div className="text-center">
          <Button variant="outline" size="lg" className="px-8 py-4 text-lg border-2">
            Browse All Chefs
          </Button>
          <p className="text-slate-500 text-sm mt-4">
            Over 500+ local chefs across UAE
          </p>
        </div>
      </div>
    </section>
  );
} 