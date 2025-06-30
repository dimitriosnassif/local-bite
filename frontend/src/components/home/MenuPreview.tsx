'use client';

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Plus, Star, Clock } from "lucide-react";

export default function MenuPreview() {
  const menuCategories = [
    {
      name: "Small Plates",
      items: [
        {
          id: 1,
          name: "Truffle Arancini",
          description: "Crispy risotto balls with truffle oil, parmesan cheese, and fresh herbs",
          price: 14.00,
          image: "/images/arancini.jpg",
          isPopular: true,
          preparationTime: "15 min"
        },
        {
          id: 2,
          name: "Burrata Caprese",
          description: "Fresh burrata with heirloom tomatoes, basil, and aged balsamic",
          price: 18.00,
          image: "/images/burrata.jpg",
          isPopular: false,
          preparationTime: "10 min"
        }
      ]
    },
    {
      name: "Mains",
      items: [
        {
          id: 3,
          name: "Braised Short Rib",
          description: "24-hour braised beef short rib with roasted vegetables and red wine jus",
          price: 32.00,
          image: "/images/short-rib.jpg",
          isPopular: true,
          preparationTime: "25 min"
        },
        {
          id: 4,
          name: "Pan-Seared Salmon",
          description: "Atlantic salmon with quinoa pilaf, seasonal vegetables, and lemon herb butter",
          price: 28.00,
          image: "/images/salmon.jpg",
          isPopular: false,
          preparationTime: "20 min"
        }
      ]
    },
    {
      name: "Desserts",
      items: [
        {
          id: 5,
          name: "Chocolate Lava Cake",
          description: "Warm chocolate cake with molten center, vanilla ice cream, and fresh berries",
          price: 12.00,
          image: "/images/lava-cake.jpg",
          isPopular: true,
          preparationTime: "12 min"
        }
      ]
    }
  ];

  return (
    <section className="py-20 px-4 bg-white">
      <div className="max-w-7xl mx-auto">
        {/* Section Header */}
        <div className="text-center mb-16">
          <h2 className="text-4xl md:text-5xl font-bold text-slate-900 mb-4">
            Beautiful{' '}
            <span className="text-slate-800">menu displays</span>
          </h2>
          <p className="text-xl text-slate-600 max-w-3xl mx-auto">
            Showcase your dishes with stunning visuals and detailed descriptions 
            that make customers hungry for more.
          </p>
        </div>

        {/* Mock Menu Interface */}
        <div className="max-w-4xl mx-auto">
          <Card className="shadow-2xl border-0 overflow-hidden">
            {/* Menu Header */}
            <CardHeader className="bg-gradient-to-r from-slate-800 to-slate-700 text-white p-8">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center">
                  <span className="text-2xl">👨‍🍳</span>
                </div>
                <div>
                  <CardTitle className="text-3xl mb-2">Bella Vista Kitchen</CardTitle>
                  <p className="text-slate-200">By Chef Antonio Rossi</p>
                  <div className="flex items-center gap-4 mt-2">
                    <div className="flex items-center gap-1">
                      <Star className="w-4 h-4 fill-white" />
                      <span className="text-sm">4.9</span>
                    </div>
                    <span className="text-sm text-slate-200">Italian • Fine Dining</span>
                  </div>
                </div>
              </div>
            </CardHeader>

            <CardContent className="p-0">
              {/* Menu Categories */}
              <div className="border-b">
                <div className="flex gap-6 px-8 pt-6">
                  {menuCategories.map((category, index) => (
                    <button
                      key={index}
                      className={`pb-4 px-2 text-sm font-medium border-b-2 transition-colors ${
                        index === 0 
                          ? 'border-slate-800 text-slate-800' 
                          : 'border-transparent text-slate-500 hover:text-slate-700'
                      }`}
                    >
                      {category.name}
                    </button>
                  ))}
                </div>
              </div>

              {/* Menu Items */}
              <div className="p-8">
                <div className="space-y-8">
                  {menuCategories[0].items.map((item) => (
                    <div key={item.id} className="flex gap-6 group">
                      {/* Item Image */}
                      <div className="w-24 h-24 bg-gradient-to-br from-slate-100 to-slate-200 rounded-xl flex-shrink-0 flex items-center justify-center">
                        <span className="text-2xl">🍽️</span>
                      </div>

                      {/* Item Details */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between mb-2">
                          <div>
                            <div className="flex items-center gap-2">
                              <h3 className="text-lg font-semibold text-slate-900">{item.name}</h3>
                              {item.isPopular && (
                                <Badge variant="default" className="text-xs bg-amber-100 text-amber-800">
                                  Popular
                                </Badge>
                              )}
                            </div>
                            <div className="flex items-center gap-3 text-sm text-slate-500 mt-1">
                              <div className="flex items-center gap-1">
                                <Clock className="w-3 h-3" />
                                <span>{item.preparationTime}</span>
                              </div>
                            </div>
                          </div>
                          <div className="text-right">
                            <div className="text-xl font-bold text-slate-900">
                              ${item.price.toFixed(2)}
                            </div>
                          </div>
                        </div>
                        
                        <p className="text-slate-600 text-sm leading-relaxed mb-3">
                          {item.description}
                        </p>

                        <Button size="sm" className="opacity-0 group-hover:opacity-100 transition-opacity">
                          <Plus className="w-4 h-4 mr-1" />
                          Add to Cart
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Cart Summary */}
              <div className="bg-slate-50 p-6 border-t">
                <div className="flex items-center justify-between">
                  <div className="text-sm text-slate-600">
                    2 items • $58.32
                  </div>
                  <Button className="px-6">
                    View Cart
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Bottom CTA */}
        <div className="text-center mt-16">
          <Button variant="accent" size="lg" className="px-8 py-4 text-lg shadow-lg hover:shadow-xl transition-all duration-300">
            Create Your Menu
          </Button>
          <p className="text-slate-500 text-sm mt-4">
            Professional menu design tools included
          </p>
        </div>
      </div>
    </section>
  );
} 