import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";
import HeroSection from "@/components/home/HeroSection";
import FeatureSection from "@/components/home/FeatureSection";
import ChefShowcase from "@/components/home/ChefShowcase";
import MenuPreview from "@/components/home/MenuPreview";
import CTASection from "@/components/home/CTASection";

export default function HomePage() {
  return (
    <>
      <Header />
      <main>
        <HeroSection />
        <FeatureSection />
        <ChefShowcase />
        <MenuPreview />
        <CTASection />
      </main>
      <Footer />
    </>
  );
}
