import { Territory, LeafPoints } from "./types";
import { calculateLeafTraits, calculateZScore } from "./utils";

// Perfect, healthy leaf coordinates (highly symmetrical)
export const healthyLeafPoints: LeafPoints = {
  base: { x: 50, y: 90 },
  tip: { x: 50, y: 15 },
  leftVein1Base: { x: 50, y: 70 },
  leftVein1End: { x: 22, y: 52 },
  leftVein2Base: { x: 50, y: 50 },
  leftVein2End: { x: 26, y: 32 },
  leftWidthMax: { x: 18, y: 55 },
  rightVein1Base: { x: 50, y: 70 },
  rightVein1End: { x: 78, y: 52 },
  rightVein2Base: { x: 50, y: 50 },
  rightVein2End: { x: 74, y: 32 },
  rightWidthMax: { x: 82, y: 55 },
};

// Asymmetric leaf coordinates (highly asymmetrical due to environmental stress)
export const damagedLeafPoints: LeafPoints = {
  base: { x: 50, y: 90 },
  tip: { x: 52, y: 12 },
  leftVein1Base: { x: 50, y: 74 },
  leftVein1End: { x: 14, y: 58 }, // much larger width & lower vein
  leftVein2Base: { x: 50, y: 54 },
  leftVein2End: { x: 18, y: 38 },
  leftWidthMax: { x: 10, y: 62 },
  rightVein1Base: { x: 50, y: 64 }, // mismatched vein bases
  rightVein1End: { x: 76, y: 46 }, // shorter/higher right side
  rightVein2Base: { x: 50, y: 44 },
  rightVein2End: { x: 72, y: 26 },
  rightWidthMax: { x: 86, y: 48 }, // skewed right side
};

// Medium asymmetry leaf coordinates
export const mediumLeafPoints: LeafPoints = {
  base: { x: 50, y: 88 },
  tip: { x: 50, y: 16 },
  leftVein1Base: { x: 50, y: 68 },
  leftVein1End: { x: 21, y: 51 },
  leftVein2Base: { x: 50, y: 49 },
  leftVein2End: { x: 24, y: 31 },
  leftWidthMax: { x: 16, y: 54 },
  rightVein1Base: { x: 50, y: 71 },
  rightVein1End: { x: 75, y: 54 },
  rightVein2Base: { x: 50, y: 52 },
  rightVein2End: { x: 71, y: 35 },
  rightWidthMax: { x: 80, y: 58 },
};

function generateLeaf(id: string, name: string, points: LeafPoints, timestamp?: string): any {
  const traits = calculateLeafTraits(points);
  const zScore = calculateZScore(traits);
  return {
    id,
    photoUrl: "template",
    photoName: name,
    points,
    traits,
    zScore,
    isTemplate: true,
    timestamp: timestamp || new Date().toISOString(),
  };
}

// Function to generate realistic birch leaf measurements over the last 7 days
export function createSampleTerritories(): Territory[] {
  const now = Date.now();
  const dayMs = 86400000;

  // Day offsets from 6 days ago (index 0) to today (index 6)
  const days = [6, 5, 4, 3, 2, 1, 0];

  // Helper to clone and slightly vary coordinates for biological variability
  const perturb = (base: LeafPoints, variance: number): LeafPoints => ({
    ...base,
    leftWidthMax: { ...base.leftWidthMax, x: Math.max(8, base.leftWidthMax.x + (Math.sin(variance) * 2)) },
    rightWidthMax: { ...base.rightWidthMax, x: Math.min(92, base.rightWidthMax.x + (Math.cos(variance) * 2)) },
    leftVein2End: { ...base.leftVein2End, y: base.leftVein2End.y + (Math.sin(variance * 2) * 1.5) },
    rightVein2End: { ...base.rightVein2End, y: base.rightVein2End.y + (Math.cos(variance * 2) * 1.5) },
  });

  // Territory 1: Botanical Garden (Clean control, Score 1)
  const t1Leaves = days.flatMap((d, i) => {
    const tStamp1 = new Date(now - d * dayMs - 1000 * 60 * 60 * 2).toISOString();
    const tStamp2 = new Date(now - d * dayMs - 1000 * 60 * 60 * 4).toISOString();
    return [
      generateLeaf(`t1_l_${d}_1`, `Лист берёзы БС-${d + 1}A`, perturb(healthyLeafPoints, i * 1.1), tStamp1),
      generateLeaf(`t1_l_${d}_2`, `Лист берёзы БС-${d + 1}B`, perturb(healthyLeafPoints, (i + 1) * 0.7), tStamp2),
    ];
  });

  // Territory 2: Urban Square (Moderate urban stress, Score 2-3)
  const t2Leaves = days.flatMap((d, i) => {
    const tStamp1 = new Date(now - d * dayMs - 1000 * 60 * 60 * 3).toISOString();
    const tStamp2 = new Date(now - d * dayMs - 1000 * 60 * 60 * 6).toISOString();
    return [
      generateLeaf(`t2_l_${d}_1`, `Лист берёзы ГС-${d + 1}A`, perturb(mediumLeafPoints, i * 0.9), tStamp1),
      generateLeaf(`t2_l_${d}_2`, `Лист берёзы ГС-${d + 1}B`, perturb(mediumLeafPoints, (i + 1) * 1.3), tStamp2),
    ];
  });

  // Territory 3: Highway Avenue (High industrial/traffic stress, Score 4-5)
  const t3Leaves = days.flatMap((d, i) => {
    const tStamp1 = new Date(now - d * dayMs - 1000 * 60 * 60 * 1).toISOString();
    const tStamp2 = new Date(now - d * dayMs - 1000 * 60 * 60 * 5).toISOString();
    return [
      generateLeaf(`t3_l_${d}_1`, `Лист берёзы ПР-${d + 1}A`, perturb(damagedLeafPoints, i * 1.4), tStamp1),
      generateLeaf(`t3_l_${d}_2`, `Лист берёзы ПР-${d + 1}B`, perturb(damagedLeafPoints, (i + 1) * 0.8), tStamp2),
    ];
  });

  return [
    {
      id: "t1",
      name: "Ботанический сад (Контроль)",
      description: "Лесопарковая зона, удалена от магистралей. Фоновый экологический мониторинг.",
      latitude: 43.2185,
      longitude: 131.9862,
      leaves: t1Leaves,
      timestamp: new Date(now - 6 * dayMs).toISOString(),
    },
    {
      id: "t2",
      name: "Покровский парк (Городской сквер)",
      description: "Центральная городская рекреационная зона со средней антропогенной нагрузкой.",
      latitude: 43.1264,
      longitude: 131.8925,
      leaves: t2Leaves,
      timestamp: new Date(now - 6 * dayMs).toISOString(),
    },
    {
      id: "t3",
      name: "Проспект 100-летия (Магистраль)",
      description: "Зона интенсивного движения автотранспорта с высоким уровнем выхлопных газов.",
      latitude: 43.1654,
      longitude: 131.9082,
      leaves: t3Leaves,
      timestamp: new Date(now - 6 * dayMs).toISOString(),
    },
  ];
}

// Clean empty journal by default as requested.
// createSampleTerritories() remains available for demo data loading.
export const mockTerritories: Territory[] = [];

