'use client';

import { useState } from 'react';

interface DropdownItem {
  label: string;
  href?: string;
  onClick?: () => void;
  icon?: string;
}

interface DropdownMenuProps {
  buttonText: string;
  items: DropdownItem[];
  buttonClassName?: string;
  dropdownClassName?: string;
  itemClassName?: string;
}

/**
 * Reusable DropdownMenu Component
 * 
 * @example
 * // Basic usage with links
 * <DropdownMenu 
 *   buttonText="My Menu"
 *   items={[
 *     { label: 'Home', href: '/home' },
 *     { label: 'About', href: '/about' }
 *   ]}
 * />
 * 
 * @example
 * // With custom click handlers and icons
 * <DropdownMenu 
 *   buttonText="Actions"
 *   items={[
 *     { label: 'Save', icon: '💾', onClick: () => handleSave() },
 *     { label: 'Delete', icon: '🗑️', onClick: () => handleDelete() }
 *   ]}
 * />
 * 
 * @example
 * // With custom styling
 * <DropdownMenu 
 *   buttonText="Styled Menu"
 *   items={[...]}
 *   buttonClassName="bg-blue-500 text-white px-4 py-2 rounded"
 *   dropdownClassName="w-48 bg-gray-100 rounded-md shadow-md"
 * />
 */
export default function DropdownMenu({ 
  buttonText, 
  items, 
  buttonClassName = "text-gray-600 hover:text-orange-500 transition-colors flex items-center space-x-1",
  dropdownClassName = "absolute top-full right-0 mt-2 w-64 bg-white rounded-lg shadow-lg border border-gray-100 py-2 z-50",
  itemClassName = "flex items-center px-4 py-3 text-gray-700 hover:bg-orange-50 hover:text-orange-500 transition-colors"
}: DropdownMenuProps) {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const handleItemClick = (item: DropdownItem) => {
    if (item.onClick) {
      item.onClick();
    }
    setIsDropdownOpen(false);
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsDropdownOpen(!isDropdownOpen)}
        onBlur={() => setTimeout(() => setIsDropdownOpen(false), 150)}
        className={buttonClassName}
      >
        <span>{buttonText}</span>
        <svg 
          className={`w-4 h-4 transition-transform ${isDropdownOpen ? 'rotate-180' : ''}`} 
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      
      {isDropdownOpen && (
        <div className={dropdownClassName}>
          {items.map((item, index) => (
            <a
              key={index}
              href={item.href || '#'}
              onClick={(e) => {
                if (item.onClick) {
                  e.preventDefault();
                  handleItemClick(item);
                }
              }}
              className={itemClassName}
            >
              {item.icon && <span className="mr-3">{item.icon}</span>}
              <span>{item.label}</span>
            </a>
          ))}
        </div>
      )}
    </div>
  );
} 