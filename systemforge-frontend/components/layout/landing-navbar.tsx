'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Terminal, Menu, X, ChevronDown, Cpu, Network, Code, Shield, Cloud, ShoppingCart, Landmark, Zap, Layers, ArrowRight, User, Building2, Calculator, Rocket } from 'lucide-react';

const productFeatures = [
  {
    title: 'System Generator',
    description: 'AI-driven backend architecture design',
    icon: Cpu,
    href: '/create'
  },
  {
    title: 'Visualizer',
    description: 'Interactive resource graph mapping',
    icon: Network,
    href: '/architecture'
  },
  {
    title: 'Code Export',
    description: 'Generate boilerplate & schemas',
    icon: Code,
    href: '/dashboard'
  },
  {
    title: 'Cloud IaC',
    description: 'Terraform & Kubernetes manifests',
    icon: Cloud,
    href: '/dashboard'
  },
  {
    title: 'Security Audit',
    description: 'Automated vulnerability scanning',
    icon: Shield,
    href: '/dashboard'
  }
];

const solutionsFeatures = [
  {
    title: 'E-commerce',
    description: 'Scalable retail & inventory backends',
    icon: ShoppingCart,
    href: '/dashboard'
  },
  {
    title: 'Fintech & Banking',
    description: 'Secure, compliant financial infrastructure',
    icon: Landmark,
    href: '/dashboard'
  },
  {
    title: 'Real-time Apps',
    description: 'WebSockets & event-driven systems',
    icon: Zap,
    href: '/dashboard'
  },
  {
    title: 'SaaS Platforms',
    description: 'Multi-tenant architecture design',
    icon: Layers,
    href: '/dashboard'
  }
];

const pricingPlans = [
  {
    title: 'Community',
    description: 'Free forever for individuals',
    icon: User,
    href: '/pricing#community'
  },
  {
    title: 'Pro',
    description: 'For small teams and startups',
    icon: Zap,
    href: '/pricing#pro'
  },
  {
    title: 'Enterprise',
    description: 'Custom limits and dedicated support',
    icon: Building2,
    href: '/pricing#enterprise'
  }
];

const pricingResources = [
  {
    title: 'Compare Plans',
    description: 'See all features side-by-side',
    icon: Layers,
    href: '/pricing#compare'
  },
  {
    title: 'Startup Program',
    description: 'Apply for credits and support',
    icon: Rocket,
    href: '/startups'
  }
];

export function LandingNavbar() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <nav 
      className={`fixed top-0 w-full z-50 transition-all duration-300 ${
        isScrolled 
          ? 'bg-[#0e1322]/80 backdrop-blur-xl border-b border-[#3a494b]/30 shadow-[0_10px_30px_rgba(0,0,0,0.5)] py-3' 
          : 'bg-transparent py-5'
      }`}
    >
      <div className="flex justify-between items-center px-6 md:px-8 max-w-7xl mx-auto font-headline font-medium text-sm tracking-wide">
        <Link href="/" className="text-xl font-bold tracking-tighter text-[#00f2ff] drop-shadow-[0_0_8px_rgba(0,242,255,0.5)] flex items-center gap-2 group">
          <Terminal className="w-6 h-6 group-hover:rotate-12 transition-transform duration-300" />
          SystemForge
        </Link>
        
        <div className="hidden md:flex items-center gap-8">
          <div className="relative group/product">
            <button className="flex items-center gap-1 text-[#dee1f7]/70 hover:text-[#00f2ff] transition-colors duration-300 py-6">
              Product
              <ChevronDown className="w-4 h-4 group-hover/product:rotate-180 transition-transform duration-300" />
            </button>
            
            {/* Mega Menu Dropdown */}
            <div className="absolute top-full left-1/2 -translate-x-1/2 w-[600px] bg-[#090e1c]/95 backdrop-blur-xl border border-outline-variant/20 shadow-[0_20px_40px_rgba(0,0,0,0.5)] rounded-2xl p-6 opacity-0 invisible group-hover/product:opacity-100 group-hover/product:visible transition-all duration-300 translate-y-2 group-hover/product:translate-y-0">
              <div className="grid grid-cols-2 gap-x-8 gap-y-6">
                {productFeatures.map((feature) => {
                  const Icon = feature.icon;
                  return (
                    <Link 
                      key={feature.title} 
                      href={feature.href}
                      className="flex items-start gap-4 group/item hover:bg-white/5 p-3 rounded-xl transition-colors"
                    >
                      <div className="p-2.5 rounded-lg bg-primary-container/10 text-primary-container group-hover/item:bg-primary-container group-hover/item:text-on-primary transition-colors shrink-0">
                        <Icon className="w-5 h-5" />
                      </div>
                      <div>
                        <h4 className="text-[#e1fdff] font-bold text-sm mb-1 group-hover/item:text-[#00f2ff] transition-colors">{feature.title}</h4>
                        <p className="text-[#dee1f7]/60 text-xs leading-relaxed">{feature.description}</p>
                      </div>
                    </Link>
                  );
                })}
              </div>
              <div className="mt-6 pt-6 border-t border-outline-variant/10 flex items-center justify-between">
                <div>
                  <h4 className="text-[#e1fdff] font-bold text-sm mb-1">SystemForge Enterprise</h4>
                  <p className="text-[#dee1f7]/60 text-xs">Advanced security and custom deployment options.</p>
                </div>
                <Link href="/dashboard" className="text-xs font-bold text-[#00f2ff] hover:text-white transition-colors px-4 py-2 rounded-lg bg-[#00f2ff]/10 hover:bg-[#00f2ff]/20">
                  Contact Sales
                </Link>
              </div>
            </div>
          </div>

          <div className="relative group/solutions">
            <button className="flex items-center gap-1 text-[#dee1f7]/70 hover:text-[#00f2ff] transition-colors duration-300 py-6">
              Solutions
              <ChevronDown className="w-4 h-4 group-hover/solutions:rotate-180 transition-transform duration-300" />
            </button>
            
            {/* Mega Menu Dropdown */}
            <div className="absolute top-full left-1/2 -translate-x-1/2 w-[550px] bg-[#090e1c]/95 backdrop-blur-xl border border-outline-variant/20 shadow-[0_20px_40px_rgba(0,0,0,0.5)] rounded-2xl p-6 opacity-0 invisible group-hover/solutions:opacity-100 group-hover/solutions:visible transition-all duration-300 translate-y-2 group-hover/solutions:translate-y-0">
              <div className="grid grid-cols-2 gap-x-8 gap-y-6">
                {solutionsFeatures.map((feature) => {
                  const Icon = feature.icon;
                  return (
                    <Link 
                      key={feature.title} 
                      href={feature.href}
                      className="flex items-start gap-4 group/item hover:bg-white/5 p-3 rounded-xl transition-colors"
                    >
                      <div className="p-2.5 rounded-lg bg-secondary-container/10 text-secondary-container group-hover/item:bg-secondary-container group-hover/item:text-on-secondary-container transition-colors shrink-0">
                        <Icon className="w-5 h-5" />
                      </div>
                      <div>
                        <h4 className="text-[#e1fdff] font-bold text-sm mb-1 group-hover/item:text-[#00f2ff] transition-colors">{feature.title}</h4>
                        <p className="text-[#dee1f7]/60 text-xs leading-relaxed">{feature.description}</p>
                      </div>
                    </Link>
                  );
                })}
              </div>
              <div className="mt-6 pt-6 border-t border-outline-variant/10">
                <Link href="/dashboard" className="flex items-center justify-between group/case-study p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/10 hover:border-primary-container/30 transition-colors">
                  <div>
                    <h4 className="text-[#e1fdff] font-bold text-sm mb-1">Customer Stories</h4>
                    <p className="text-[#dee1f7]/60 text-xs">See how top engineering teams use SystemForge.</p>
                  </div>
                  <div className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center group-hover/case-study:bg-primary-container group-hover/case-study:text-on-primary transition-colors">
                    <ArrowRight className="w-4 h-4" />
                  </div>
                </Link>
              </div>
            </div>
          </div>

          <div className="relative group/pricing">
            <button className="flex items-center gap-1 text-[#dee1f7]/70 hover:text-[#00f2ff] transition-colors duration-300 py-6">
              Pricing
              <ChevronDown className="w-4 h-4 group-hover/pricing:rotate-180 transition-transform duration-300" />
            </button>
            
            {/* Mega Menu Dropdown */}
            <div className="absolute top-full left-1/2 -translate-x-1/2 w-[600px] bg-[#090e1c]/95 backdrop-blur-xl border border-outline-variant/20 shadow-[0_20px_40px_rgba(0,0,0,0.5)] rounded-2xl p-6 opacity-0 invisible group-hover/pricing:opacity-100 group-hover/pricing:visible transition-all duration-300 translate-y-2 group-hover/pricing:translate-y-0">
              <div className="grid grid-cols-2 gap-8">
                {/* Plans Column */}
                <div className="flex flex-col gap-4">
                  <h3 className="text-[#dee1f7]/50 text-xs font-bold uppercase tracking-wider mb-1">Plans</h3>
                  {pricingPlans.map((plan) => {
                    const Icon = plan.icon;
                    return (
                      <Link 
                        key={plan.title} 
                        href={plan.href}
                        className="flex items-start gap-4 group/item hover:bg-white/5 p-3 -mx-3 rounded-xl transition-colors"
                      >
                        <div className="p-2.5 rounded-lg bg-tertiary-container/10 text-tertiary-container group-hover/item:bg-tertiary-container group-hover/item:text-on-tertiary-container transition-colors shrink-0">
                          <Icon className="w-5 h-5" />
                        </div>
                        <div>
                          <h4 className="text-[#e1fdff] font-bold text-sm mb-1 group-hover/item:text-[#00f2ff] transition-colors">{plan.title}</h4>
                          <p className="text-[#dee1f7]/60 text-xs leading-relaxed">{plan.description}</p>
                        </div>
                      </Link>
                    );
                  })}
                </div>

                {/* Resources Column */}
                <div className="flex flex-col gap-4 bg-surface-container-lowest/50 p-5 -my-3 -mr-3 rounded-xl border border-outline-variant/10">
                  <h3 className="text-[#dee1f7]/50 text-xs font-bold uppercase tracking-wider mb-1">Resources</h3>
                  {pricingResources.map((resource) => {
                    const Icon = resource.icon;
                    return (
                      <Link 
                        key={resource.title} 
                        href={resource.href}
                        className="flex items-start gap-4 group/item hover:bg-white/5 p-3 -mx-3 rounded-xl transition-colors"
                      >
                        <div className="p-2.5 rounded-lg bg-surface-container-high text-on-surface-variant group-hover/item:bg-primary-container group-hover/item:text-on-primary transition-colors shrink-0">
                          <Icon className="w-5 h-5" />
                        </div>
                        <div>
                          <h4 className="text-[#e1fdff] font-bold text-sm mb-1 group-hover/item:text-[#00f2ff] transition-colors">{resource.title}</h4>
                          <p className="text-[#dee1f7]/60 text-xs leading-relaxed">{resource.description}</p>
                        </div>
                      </Link>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>

          {['Architecture'].map((item) => (
            <Link 
              key={item} 
              href={item === 'Architecture' ? '/architecture' : '#'} 
              className="relative text-[#dee1f7]/70 hover:text-[#00f2ff] transition-colors duration-300 group py-6"
            >
              {item}
              <span className="absolute bottom-4 left-0 w-0 h-0.5 bg-[#00f2ff] transition-all duration-300 group-hover:w-full shadow-[0_0_8px_#00f2ff]"></span>
            </Link>
          ))}
        </div>

        <div className="hidden md:flex items-center gap-5">
          <Link href="/dashboard" className="text-[#dee1f7]/70 hover:text-[#e1fdff] transition-colors">Sign In</Link>
          <Link 
            href="/dashboard" 
            className="bg-gradient-to-br from-[#00f2ff] to-[#0566d9] text-on-primary px-5 py-2 rounded-lg font-bold text-xs uppercase tracking-wider shadow-[0_0_15px_rgba(0,242,255,0.3)] hover:shadow-[0_0_25px_rgba(0,242,255,0.5)] hover:brightness-110 active:scale-95 transition-all"
          >
            Get Started
          </Link>
        </div>

        <button 
          className="md:hidden text-[#dee1f7]/70 hover:text-[#00f2ff] transition-colors"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        >
          {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {/* Mobile Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden absolute top-full left-0 w-full bg-[#0e1322]/95 backdrop-blur-xl border-b border-[#3a494b]/30 shadow-2xl py-4 px-6 flex flex-col gap-4 max-h-[80vh] overflow-y-auto">
          <div className="flex flex-col gap-2">
            <div className="text-[#e1fdff] font-bold py-2 border-b border-white/10 text-sm">Product</div>
            <div className="pl-4 flex flex-col gap-3 py-2">
              {productFeatures.map((feature) => (
                <Link 
                  key={feature.title} 
                  href={feature.href}
                  className="text-[#dee1f7]/70 hover:text-[#00f2ff] text-sm flex items-center gap-3"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <feature.icon className="w-4 h-4" />
                  {feature.title}
                </Link>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <div className="text-[#e1fdff] font-bold py-2 border-b border-white/10 text-sm">Solutions</div>
            <div className="pl-4 flex flex-col gap-3 py-2">
              {solutionsFeatures.map((feature) => (
                <Link 
                  key={feature.title} 
                  href={feature.href}
                  className="text-[#dee1f7]/70 hover:text-[#00f2ff] text-sm flex items-center gap-3"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <feature.icon className="w-4 h-4" />
                  {feature.title}
                </Link>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <div className="text-[#e1fdff] font-bold py-2 border-b border-white/10 text-sm">Pricing</div>
            <div className="pl-4 flex flex-col gap-3 py-2">
              {pricingPlans.map((plan) => (
                <Link 
                  key={plan.title} 
                  href={plan.href}
                  className="text-[#dee1f7]/70 hover:text-[#00f2ff] text-sm flex items-center gap-3"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <plan.icon className="w-4 h-4" />
                  {plan.title}
                </Link>
              ))}
              <div className="h-px bg-white/5 my-1"></div>
              {pricingResources.map((resource) => (
                <Link 
                  key={resource.title} 
                  href={resource.href}
                  className="text-[#dee1f7]/70 hover:text-[#00f2ff] text-sm flex items-center gap-3"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <resource.icon className="w-4 h-4" />
                  {resource.title}
                </Link>
              ))}
            </div>
          </div>
          
          {['Architecture'].map((item) => (
            <Link 
              key={item} 
              href={item === 'Architecture' ? '/architecture' : '#'} 
              className="text-[#e1fdff] font-bold hover:text-[#00f2ff] py-2 border-b border-white/5 text-sm"
              onClick={() => setMobileMenuOpen(false)}
            >
              {item}
            </Link>
          ))}
          <div className="flex flex-col gap-3 pt-2">
            <Link 
              href="/dashboard" 
              className="text-center text-[#dee1f7]/80 hover:text-[#e1fdff] py-2 border border-white/10 rounded-lg"
              onClick={() => setMobileMenuOpen(false)}
            >
              Sign In
            </Link>
            <Link 
              href="/dashboard" 
              className="text-center bg-gradient-to-br from-[#00f2ff] to-[#0566d9] text-on-primary px-5 py-3 rounded-lg font-bold text-xs uppercase tracking-wider"
              onClick={() => setMobileMenuOpen(false)}
            >
              Get Started
            </Link>
          </div>
        </div>
      )}
    </nav>
  );
}
