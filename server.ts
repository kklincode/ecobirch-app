import express from "express";
import path from "path";
import dotenv from "dotenv";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";

dotenv.config();

const app = express();
const PORT = 3000;

// Body parser for larger image payloads (e.g. from mobile cameras)
app.use(express.json({ limit: "20mb" }));
app.use(express.urlencoded({ limit: "20mb", extended: true }));

// Initialize Gemini SDK lazily to prevent crashing on boot if key is missing
let aiClient: GoogleGenAI | null = null;

function getAiClient(): GoogleGenAI | null {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey === "MY_GEMINI_API_KEY") {
    console.warn("⚠️ GEMINI_API_KEY is not configured or uses placeholder. Running in Local Simulation fallback mode.");
    return null;
  }
  
  if (!aiClient) {
    aiClient = new GoogleGenAI({
      apiKey: apiKey,
      httpOptions: {
        headers: {
          "User-Agent": "aistudio-build",
        },
      },
    });
  }
  return aiClient;
}

// ------------------------------------
// API ENDPOINTS
// ------------------------------------

/**
 * Endpoint to analyze a leaf image using Gemini 3.5 Flash multimodal vision.
 * Expects { imageBase64: string, mimeType: string }
 */
app.post("/api/analyze-leaf", async (req, res) => {
  try {
    const { imageBase64, mimeType } = req.body;
    
    if (!imageBase64) {
      return res.status(400).json({ error: "Missing imageBase64 parameter." });
    }

    const ai = getAiClient();
    if (!ai) {
      // Return a simulated, nicely formatted coordinates response on failure or key missing
      console.log("No API key available, sending simulated high-contrast leaf points.");
      const mockResult = {
        base: { x: 50, y: 88 },
        tip: { x: 50, y: 15 },
        leftVein1Base: { x: 50, y: 70 },
        leftVein1End: { x: 23, y: 53 },
        leftVein2Base: { x: 50, y: 51 },
        leftVein2End: { x: 27, y: 33 },
        leftWidthMax: { x: 16, y: 56 },
        rightVein1Base: { x: 50, y: 69 },
        rightVein1End: { x: 77, y: 51 },
        rightVein2Base: { x: 50, y: 49 },
        rightVein2End: { x: 73, y: 31 },
        rightWidthMax: { x: 84, y: 54 }
      };
      return res.json({
        success: true,
        fallbackMode: true,
        points: mockResult,
        message: "Анализ выполнен в режиме симуляции (ключ API не найден)."
      });
    }

    // Extract raw base64 data (removes data:image/jpeg;base64, if client sent it with URI scheme)
    const base64Data = imageBase64.replace(/^data:image\/\w+;base64,/, "");
    const finalMime = mimeType || "image/jpeg";

    const promptText = `
      You are an expert botanical computer vision system specialized in plant morphology and bioclimatology (specifically bioindication of Betula pendula - Silver Birch).
      Your task is to analyze the uploaded photograph of a single Birch leaf (Betula pendula) and locate exactly 12 biological landmark points on a coordinate grid from 0 to 100:
      - Coordinate 'x' is horizontal, ranging from 0 (far left edge of the image) to 100 (far right edge).
      - Coordinate 'y' is vertical, ranging from 0 (absolute top edge) to 100 (absolute bottom edge).

      --- CRITICAL ANATOMICAL METHODOLOGY & RULES ---
      1. DETECT THE LEAF & KEY AXIS OF SYMMETRY:
         - Locate the primary birch leaf. It is characterized by its serrated (toothed) edge, triangular-ovate shape, and broad base flatly tapping into the stem (petiole) with a sharp pointed tip.
         - Find the main central vein (axis of symmetry) stretching from the leaf base (stem attachment) to the sharp apex tip.
         - Even if the leaf is rotated, tilted, or upside down, identify its biological 'base' and 'tip' correctly first, then orient left vs right based on this axis.

      2. LANDMARK DESCRIPTIONS & ORDERING:
         - POINT 1 [base]: The junction point where the petiole (leaf stem) merges into the main leaf blade body. (Typically y is maximum, around y=80-92, and x is central if leaf points upwards).
         - POINT 2 [tip]: The absolute organic pointy apex tip of the leaf. (Typically y is minimum, around y=8-22).
         - LEFT-SIDE SECONDARY VEINS (Looking from Base to Tip):
           - POINT 3 [leftVein1Base]: The point on the main central vein where the FIRST major secondary vein branches off to the LEFT. It must be located on the main central vein closer to the base.
           - POINT 4 [leftVein1End]: The exact terminal point where this first secondary vein touches the left leaf margin.
           - POINT 5 [leftVein2Base]: The point on the main central vein where the SECOND major secondary vein branches off to the LEFT. It must lie on the main vein between leftVein1Base and the tip (closer to the tip).
           - POINT 6 [leftVein2End]: The terminal point where this second secondary vein touches the left leaf margin.
           - POINT 7 [leftWidthMax]: The point on the outer left edge/margin of the leaf blade where the leaf is horizontally widest (furthest perpendicular distance) from the main central vein axis.
         - RIGHT-SIDE SECONDARY VEINS:
           - POINT 8 [rightVein1Base]: The point on the main central vein where the FIRST major secondary vein branches off to the RIGHT. It must lie on the main central vein at a similar height to leftVein1Base.
           - POINT 9 [rightVein1End]: The exact terminal point where this first secondary vein touches the right leaf margin.
           - POINT 10 [rightVein2Base]: The point on the main central vein where the SECOND major secondary vein branches off to the RIGHT. It must lie on the main vein at a similar height to leftVein2Base.
           - POINT 11 [rightVein2End]: The terminal point where this second secondary vein touches the right leaf margin.
           - POINT 12 [rightWidthMax]: The point on the outer right edge/margin of the leaf blade where the leaf is horizontally widest (furthest perpendicular distance) from the main central vein axis.

      3. PHYSICAL AND ANATOMICAL CONSISTENCY GUARANTEES:
         - All base junction points (leftVein1Base, leftVein2Base, rightVein1Base, rightVein2Base) must be positioned exactly along the line connecting 'base' and 'tip'.
         - Sequential vertical ordering must be strictly respected (if leaf points upwards): base.y > (vein1Base.y) > (vein2Base.y) > tip.y. Do not cross them.
         - Left-side end points must have X coordinates smaller than their respective central bases (x_end < x_base), while right-side end points must have X coordinates larger than their respective bases (x_end > x_base).

      Locate these 12 points with high spatial precision and return them as valid JSON conforming exactly to the responseSchema.
    `;

    // Field schemas to guide Gemini on precise semantic location constraints
    const createPointSchema = (desc: string) => ({
      type: Type.OBJECT,
      description: desc,
      properties: {
        x: { type: Type.NUMBER, description: "Horizontal percentage position from 0 (left edge) to 100 (right edge)" },
        y: { type: Type.NUMBER, description: "Vertical percentage position from 0 (top edge) to 100 (bottom edge)" }
      },
      required: ["x", "y"]
    });

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: [
        {
          inlineData: {
            data: base64Data,
            mimeType: finalMime
          }
        },
        promptText
      ],
      config: {
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            base: createPointSchema("Point where the leaf petiole (stem) connects to the base of the leaf blade."),
            tip: createPointSchema("Absolute sharp apical tip of the leaf."),
            leftVein1Base: createPointSchema("Intersection point on the central vein where the first secondary vein branches to the LEFT."),
            leftVein1End: createPointSchema("Endpoint on the left leaf margin where the first secondary vein terminates."),
            leftVein2Base: createPointSchema("Intersection point on the central vein where the second secondary vein branches to the LEFT. Must be higher up than leftVein1Base."),
            leftVein2End: createPointSchema("Endpoint on the left leaf margin where the second secondary vein terminates."),
            leftWidthMax: createPointSchema("Point on the left margin where the leaf blade reaches its maximum horizontal perpendicular distance from the central main vein."),
            rightVein1Base: createPointSchema("Intersection point on the central vein where the first secondary vein branches to the RIGHT."),
            rightVein1End: createPointSchema("Endpoint on the right leaf margin where the first secondary vein terminates."),
            rightVein2Base: createPointSchema("Intersection point on the central vein where the second secondary vein branches to the RIGHT. Must be higher up than rightVein1Base."),
            rightVein2End: createPointSchema("Endpoint on the right leaf margin where the second secondary vein terminates."),
            rightWidthMax: createPointSchema("Point on the right margin where the leaf blade reaches its maximum horizontal perpendicular distance from the central main vein.")
          },
          required: [
            "base", "tip", 
            "leftVein1Base", "leftVein1End", "leftVein2Base", "leftVein2End", "leftWidthMax",
            "rightVein1Base", "rightVein1End", "rightVein2Base", "rightVein2End", "rightWidthMax"
          ]
        }
      }
    });

    const textOutput = response.text;
    if (!textOutput) {
      throw new Error("Zero-length response from Gemini model.");
    }

    const cleanedText = textOutput.trim();
    const resultObj = JSON.parse(cleanedText);

    return res.json({
      success: true,
      points: resultObj,
      message: "Лист успешно проанализирован с помощью нейросети!"
    });

  } catch (error: any) {
    console.error("Error in /api/analyze-leaf API endpoint:", error);
    // Provide safe, structurally correct fallback in case of rate limits or parsing errors
    const mockResult = {
      base: { x: 50, y: 88 },
      tip: { x: 50, y: 15 },
      leftVein1Base: { x: 50, y: 70 },
      leftVein1End: { x: 23, y: 53 },
      leftVein2Base: { x: 50, y: 51 },
      leftVein2End: { x: 27, y: 33 },
      leftWidthMax: { x: 16, y: 56 },
      rightVein1Base: { x: 50, y: 69 },
      rightVein1End: { x: 77, y: 51 },
      rightVein2Base: { x: 50, y: 49 },
      rightVein2End: { x: 73, y: 31 },
      rightWidthMax: { x: 84, y: 54 }
    };
    return res.json({
      success: true,
      fallbackMode: true,
      points: mockResult,
      message: `Работает в режиме симуляции из-за ошибки: ${error.message || "Неизвестная ошибка"}`
    });
  }
});

// ------------------------------------
// GEOCODING & SEARCH ENDPOINTS
// ------------------------------------
const geocodeCache = new Map<string, { timestamp: number; results: any[] }>();
const reverseGeocodeCache = new Map<string, { timestamp: number; data: any }>();
const CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

/**
 * Address Geocoding endpoint
 * Fallback-resilient search supporting Russian address queries, cities, streets, parks, and coordinates.
 */
app.get("/api/geocode", async (req, res) => {
  const query = (req.query.q as string || "").trim();
  if (!query) {
    return res.json({ success: true, results: [] });
  }

  // 1. Direct coordinates check (e.g. "43.115, 131.885" or "43.115 131.885")
  const coordMatch = query.match(/^(-?\d+(?:\.\d+)?)[,\s]+(-?\d+(?:\.\d+)?)$/);
  if (coordMatch) {
    const lat = parseFloat(coordMatch[1]);
    const lng = parseFloat(coordMatch[2]);
    if (!isNaN(lat) && !isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
      return res.json({
        success: true,
        results: [
          {
            name: `Координаты: ${lat.toFixed(4)}, ${lng.toFixed(4)}`,
            description: "Прямые географические координаты",
            fullAddress: `Широта ${lat}, Долгота ${lng}`,
            coords: { lat, lng }
          }
        ]
      });
    }
  }

  // 2. Memory cache check
  const cacheKey = query.toLowerCase();
  const cached = geocodeCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    return res.json({ success: true, results: cached.results, fromCache: true });
  }

  // 3. Query OpenStreetMap Nominatim with Russian localization
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4500);

    const nominatimUrl = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&addressdetails=1&limit=6&accept-language=ru`;
    const response = await fetch(nominatimUrl, {
      signal: controller.signal,
      headers: {
        "User-Agent": "EcoBirchApp/1.0 (contact@ecobirch.local)",
        "Accept": "application/json"
      }
    });
    clearTimeout(timeoutId);

    if (response.ok) {
      const data = await response.json();
      if (Array.isArray(data) && data.length > 0) {
        const results = data.map((item: any) => {
          const parts = (item.display_name || "").split(",");
          const shortName = item.name || parts[0]?.trim() || "Найденный адрес";
          const desc = parts.slice(1, 4).join(",").trim() || item.type || "";
          return {
            name: shortName,
            description: desc,
            fullAddress: item.display_name || shortName,
            coords: {
              lat: parseFloat(item.lat),
              lng: parseFloat(item.lon)
            }
          };
        }).filter((item: any) => !isNaN(item.coords.lat) && !isNaN(item.coords.lng));

        if (results.length > 0) {
          geocodeCache.set(cacheKey, { timestamp: Date.now(), results });
          return res.json({ success: true, results, source: "nominatim" });
        }
      }
    }
  } catch (err: any) {
    console.warn("Nominatim geocoding error, falling back to Photon:", err?.message || err);
  }

  // 4. Secondary fallback: Photon Komoot
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const photonUrl = `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}&limit=6`;
    const response = await fetch(photonUrl, {
      signal: controller.signal,
      headers: {
        "Accept": "application/json"
      }
    });
    clearTimeout(timeoutId);

    if (response.ok) {
      const data = await response.json();
      if (data && Array.isArray(data.features) && data.features.length > 0) {
        const results = data.features.map((feat: any) => {
          const props = feat.properties || {};
          const coords = feat.geometry?.coordinates || [];
          const name = props.name || props.street || "Адрес";
          const addrParts = [props.street, props.housenumber, props.district, props.city, props.state, props.country].filter(Boolean);
          const fullAddress = addrParts.join(", ") || name;
          return {
            name: name,
            description: [props.city, props.country].filter(Boolean).join(", "),
            fullAddress: fullAddress,
            coords: {
              lat: coords[1],
              lng: coords[0]
            }
          };
        }).filter((item: any) => !isNaN(item.coords.lat) && !isNaN(item.coords.lng));

        if (results.length > 0) {
          geocodeCache.set(cacheKey, { timestamp: Date.now(), results });
          return res.json({ success: true, results, source: "photon" });
        }
      }
    }
  } catch (err: any) {
    console.warn("Photon geocoding error:", err?.message || err);
  }

  // If nothing found or services failed
  return res.json({ success: true, results: [] });
});

/**
 * Reverse geocoding endpoint
 * Given lat and lng, returns human-readable Russian street/city address.
 */
app.get("/api/reverse-geocode", async (req, res) => {
  const latStr = req.query.lat as string;
  const lngStr = req.query.lng as string;
  const lat = parseFloat(latStr);
  const lng = parseFloat(lngStr);

  if (isNaN(lat) || isNaN(lng)) {
    return res.status(400).json({ error: "Invalid lat/lng parameters." });
  }

  const cacheKey = `${lat.toFixed(4)},${lng.toFixed(4)}`;
  const cached = reverseGeocodeCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    return res.json({ success: true, ...cached.data, fromCache: true });
  }

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const nominatimUrl = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&addressdetails=1&accept-language=ru`;
    const response = await fetch(nominatimUrl, {
      signal: controller.signal,
      headers: {
        "User-Agent": "EcoBirchApp/1.0 (contact@ecobirch.local)",
        "Accept": "application/json"
      }
    });
    clearTimeout(timeoutId);

    if (response.ok) {
      const data = await response.json();
      if (data && data.display_name) {
        const addr = data.address || {};
        const road = addr.road || addr.pedestrian || addr.footway || addr.street;
        const house = addr.house_number;
        const park = addr.park || addr.leisure || addr.nature_reserve || addr.natural;
        const suburb = addr.suburb || addr.city_district || addr.neighbourhood;
        const city = addr.city || addr.town || addr.village || addr.municipality;

        let shortName = "Точка на карте";
        if (park) {
          shortName = park;
        } else if (road) {
          shortName = road + (house ? `, д. ${house}` : "");
        } else if (suburb) {
          shortName = suburb;
        } else if (city) {
          shortName = city;
        } else {
          shortName = data.name || data.display_name.split(",")[0]?.trim() || "Точка на карте";
        }

        const resultData = {
          name: shortName,
          fullAddress: data.display_name,
          coords: { lat, lng }
        };

        reverseGeocodeCache.set(cacheKey, { timestamp: Date.now(), data: resultData });
        return res.json({ success: true, ...resultData });
      }
    }
  } catch (err: any) {
    console.warn("Reverse geocode request failed:", err?.message || err);
  }

  return res.json({
    success: true,
    name: `Точка (${lat.toFixed(4)}°, ${lng.toFixed(4)}°)`,
    fullAddress: `Координаты: ${lat.toFixed(4)}, ${lng.toFixed(4)}`,
    coords: { lat, lng }
  });
});

// Helper for status check
app.get("/api/health", (req, res) => {
  res.json({ status: "ok", mode: process.env.NODE_ENV });
});

// ------------------------------------
// DIGITAL ASSET LINKS FOR ANDROID TWA
// ------------------------------------
// Explicitly serve assetlinks.json with Content-Type: application/json
// preventing SPA fallback to index.html which breaks TWA domain verification in RuStore/Android
const assetLinksPayload = [
  {
    relation: ["delegate_permissions/common.handle_all_urls"],
    target: {
      namespace: "android_app",
      package_name: "ru.ecobirch.fa.app"
    }
  }
];

app.get(["/.well-known/assetlinks.json", "/assetlinks.json"], (req, res) => {
  res.setHeader("Content-Type", "application/json");
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Cache-Control", "public, max-age=86400");
  res.status(200).json(assetLinksPayload);
});

// ------------------------------------
// VITE MIDDLEWARE SETUP
// ------------------------------------
async function start() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    // Serve static files in production
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server environment: ${process.env.NODE_ENV || "development"}`);
    console.log(`Mobile app web server running on http://localhost:${PORT}`);
  });
}

start();
