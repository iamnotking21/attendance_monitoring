"use client";

import { LazyMotion, MotionConfig } from "motion/react";
import type { ReactNode } from "react";

/**
 * Loads the animation engine after first paint, and honours `prefers-reduced-motion`.
 *
 * `LazyMotion` keeps only a tiny proxy in the initial bundle; the feature set — around 25 kB
 * compressed — arrives in its own chunk once the page is interactive, so animation never sits
 * on the critical path. `strict` makes the full `motion.*` components throw, which is the point:
 * importing one anywhere would silently pull the engine back into the main bundle.
 *
 * `reducedMotion="user"` covers what CSS cannot. The media query in `globals.css` only reaches
 * CSS transitions and keyframes; animation computed in JavaScript and written straight to
 * `style` sails past it. This drops transform and layout animation for those users while
 * keeping opacity, so state changes stay visible without anything moving across the screen.
 */
const loadFeatures = () => import("motion/react").then((mod) => mod.domMax);

export function MotionPreferences({ children }: { children: ReactNode }) {
  return (
    <LazyMotion features={loadFeatures} strict>
      <MotionConfig reducedMotion="user">{children}</MotionConfig>
    </LazyMotion>
  );
}
