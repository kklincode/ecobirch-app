export interface Point {
  x: number; // Percent of width, 0 to 100
  y: number; // Percent of height, 0 to 100
}

export interface LeafPoints {
  base: Point;          // P0: Base of leaf (petiole junction)
  tip: Point;           // P1: Apex of leaf (tip)
  
  leftVein1Base: Point; // L-V1-Base
  leftVein1End: Point;  // L-V1-End
  leftVein2Base: Point; // L-V2-Base
  leftVein2End: Point;  // L-V2-End
  leftWidthMax: Point;  // Leftmost edge max width

  rightVein1Base: Point; // R-V1-Base
  rightVein1End: Point;  // R-V1-End
  rightVein2Base: Point; // R-V2-Base
  rightVein2End: Point;  // R-V2-End
  rightWidthMax: Point;  // Rightmost edge max width
}

export interface TraitValues {
  xLeft: number;
  xRight: number;
  yDiff: number; // Y_i = |X_l - X_r| / (X_l + X_r)
}

export interface LeafAnalysis {
  id: string;
  photoUrl: string;
  photoName: string;
  points: LeafPoints;
  isTemplate?: boolean;
  traits: {
    t1Width: TraitValues;  // Trait 1
    t2VeinLen: TraitValues; // Trait 2
    t3VeinDist: TraitValues; // Trait 3
    t4EndDist: TraitValues;  // Trait 4
    t5Angle: TraitValues;    // Trait 5
  };
  zScore: number; // Z = average of Y1-Y5
  timestamp: string; // ISO String
}

export interface Territory {
  id: string;
  name: string;
  description: string;
  latitude: number;
  longitude: number;
  leaves: LeafAnalysis[];
  timestamp: string;
}

export interface EnvironmentalRating {
  fa: number;
  score: number; // 1 to 5
  status: string; // e.g. "Условно нормальное"
  color: string; // e.g. "bg-emerald-500", "text-emerald-600"
  hexColor: string; // hex for SVG/Canvas
  description: string;
}
