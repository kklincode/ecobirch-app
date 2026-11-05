import { LeafPoints, TraitValues, LeafAnalysis, EnvironmentalRating } from "./types";

// Helper: Calculate distance between two coordinate points
export function getDistance(p1: { x: number; y: number }, p2: { x: number; y: number }): number {
  return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
}

// Helper: Point-to-line distance. Line passes through p1 and p2. Distance to p0.
export function getPointToLineDistance(
  p0: { x: number; y: number },
  p1: { x: number; y: number },
  p2: { x: number; y: number }
): number {
  const numerator = Math.abs(
    (p2.y - p1.y) * p0.x - (p2.x - p1.x) * p0.y + p2.x * p1.y - p2.y * p1.x
  );
  const denominator = Math.sqrt(Math.pow(p2.y - p1.y, 2) + Math.pow(p2.x - p1.x, 2));
  return denominator === 0 ? 0 : numerator / denominator;
}

// Helper: Angle in degrees between two vectors sharing a common base point
// Vector 1: from vein2Base to tip (representing main direction)
// Vector 2: from vein2Base to vein2End
export function getAngleBetweenVectors(
  base: { x: number; y: number },
  tip: { x: number; y: number },
  end: { x: number; y: number }
): number {
  // Vector A: base -> tip
  const ax = tip.x - base.x;
  const ay = tip.y - base.y;
  
  // Vector B: base -> end
  const bx = end.x - base.x;
  const by = end.y - base.y;
  
  const dotProduct = ax * bx + ay * by;
  const magA = Math.sqrt(ax * ax + ay * ay);
  const magB = Math.sqrt(bx * bx + by * by);
  
  if (magA === 0 || magB === 0) return 0;
  
  let cosTheta = dotProduct / (magA * magB);
  // Clamp due to float operations
  cosTheta = Math.max(-1, Math.min(1, cosTheta));
  
  return (Math.acos(cosTheta) * 180) / Math.PI;
}

// Convert screen / percentage distance to lifelike mm measurements
const MM_SCALE = 0.65; // Each percent unit maps to 0.65 mm

export function calculateLeafTraits(points: LeafPoints): LeafAnalysis["traits"] {
  // Trait 1: Max width from main vein
  const widthLeftRaw = getPointToLineDistance(points.leftWidthMax, points.base, points.tip);
  const widthRightRaw = getPointToLineDistance(points.rightWidthMax, points.base, points.tip);
  
  const xLeft1 = widthLeftRaw * MM_SCALE;
  const xRight1 = widthRightRaw * MM_SCALE;
  const yDiff1 = Math.abs(xLeft1 - xRight1) / (xLeft1 + xRight1 || 1);

  // Trait 2: Length of the second secondary vein
  const xLeft2 = getDistance(points.leftVein2Base, points.leftVein2End) * MM_SCALE;
  const xRight2 = getDistance(points.rightVein2Base, points.rightVein2End) * MM_SCALE;
  const yDiff2 = Math.abs(xLeft2 - xRight2) / (xLeft2 + xRight2 || 1);

  // Trait 3: Distance between bases of 1st and 2nd secondary veins
  const xLeft3 = getDistance(points.leftVein1Base, points.leftVein2Base) * MM_SCALE;
  const xRight3 = getDistance(points.rightVein1Base, points.rightVein2Base) * MM_SCALE;
  const yDiff3 = Math.abs(xLeft3 - xRight3) / (xLeft3 + xRight3 || 1);

  // Trait 4: Distance between ends of 1st and 2nd secondary veins
  const xLeft4 = getDistance(points.leftVein1End, points.leftVein2End) * MM_SCALE;
  const xRight4 = getDistance(points.rightVein1End, points.rightVein2End) * MM_SCALE;
  const yDiff4 = Math.abs(xLeft4 - xRight4) / (xLeft4 + xRight4 || 1);

  // Trait 5: Angle between main vein and second secondary vein
  // Main direction uses the local base of vein 2 pointing to the tip
  const xLeft5 = getAngleBetweenVectors(points.leftVein2Base, points.tip, points.leftVein2End);
  const xRight5 = getAngleBetweenVectors(points.rightVein2Base, points.tip, points.rightVein2End);
  const yDiff5 = Math.abs(xLeft5 - xRight5) / (xLeft5 + xRight5 || 1);

  return {
    t1Width: { xLeft: xLeft1, xRight: xRight1, yDiff: yDiff1 },
    t2VeinLen: { xLeft: xLeft2, xRight: xRight2, yDiff: yDiff2 },
    t3VeinDist: { xLeft: xLeft3, xRight: xRight3, yDiff: yDiff3 },
    t4EndDist: { xLeft: xLeft4, xRight: xRight4, yDiff: yDiff4 },
    t5Angle: { xLeft: xLeft5, xRight: xRight5, yDiff: yDiff5 },
  };
}

export function calculateZScore(traits: LeafAnalysis["traits"]): number {
  return (
    (traits.t1Width.yDiff +
      traits.t2VeinLen.yDiff +
      traits.t3VeinDist.yDiff +
      traits.t4EndDist.yDiff +
      traits.t5Angle.yDiff) /
    5
  );
}

// Evaluate status according to Section 8 of the methodology
export function getEnvironmentalRating(fa: number): EnvironmentalRating {
  if (fa <= 0.055) {
    return {
      fa,
      score: 1,
      status: "Условно нормальное",
      color: "bg-emerald-500 text-white border-emerald-600",
      hexColor: "#10b981",
      description: "Чистая окружающая среда. Растения развиваются без заметных антропогенных стрессов.",
    };
  } else if (fa <= 0.060) {
    return {
      fa,
      score: 2,
      status: "Начальные отклонения",
      color: "bg-yellow-400 text-slate-900 border-yellow-500",
      hexColor: "#facc15",
      description: "Слабая антропогенная нагрузка. Качество среды незначительно снижено.",
    };
  } else if (fa <= 0.065) {
    return {
      fa,
      score: 3,
      status: "Средний уровень загрязнения",
      color: "bg-amber-500 text-white border-amber-600",
      hexColor: "#f59e0b",
      description: "Умеренная антропогенная нагрузка. Качество среды заметно ухудшено.",
    };
  } else if (fa <= 0.070) {
    return {
      fa,
      score: 4,
      status: "Значительное загрязнение",
      color: "bg-rose-500 text-white border-rose-600",
      hexColor: "#f43f5e",
      description: "Высокая антропогенная нагрузка. Окружающая среда находится в неудовлетворительном состоянии.",
    };
  } else {
    return {
      fa,
      score: 5,
      status: "Критическое состояние",
      color: "bg-red-800 text-white border-red-900",
      hexColor: "#991b1b",
      description: "Чрезвычайная экологическая ситуация. Сильное токсическое воздействие на живые организмы.",
    };
  }
}

// Generate default clean leaf coordinates
export function getDefaultLeafPoints(): LeafPoints {
  return {
    base: { x: 50, y: 88 },
    tip: { x: 50, y: 15 },
    
    leftVein1Base: { x: 50, y: 70 },
    leftVein1End: { x: 22, y: 52 },
    
    leftVein2Base: { x: 50, y: 52 },
    leftVein2End: { x: 26, y: 32 },
    
    leftWidthMax: { x: 14, y: 58 },

    rightVein1Base: { x: 50, y: 68 },
    rightVein1End: { x: 78, y: 50 },
    
    rightVein2Base: { x: 50, y: 50 },
    rightVein2End: { x: 74, y: 31 },
    
    rightWidthMax: { x: 86, y: 57 },
  };
}
